package cl.medalertpro.notification.controller;

import cl.medalertpro.notification.entity.Notificacion;
import cl.medalertpro.notification.repository.NotificacionRepository;
import com.twilio.security.RequestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwilioWebhookControllerTest {

    private static final String CALLBACK_URL = "https://notif.test/webhooks/twilio/estado-mensaje";

    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private RequestValidator requestValidator;

    private TwilioWebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new TwilioWebhookController(notificacionRepository, requestValidator);
        ReflectionTestUtils.setField(controller, "callbackUrl", CALLBACK_URL);
    }

    private MultiValueMap<String, String> parametros(String sid, String estado) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("MessageSid", sid);
        map.add("MessageStatus", estado);
        return map;
    }

    @Test
    void actualizaEstadoEntregaYMarcaEntregadoEnParaEstadosFinales() {
        var params = parametros("SM123", "delivered");
        when(requestValidator.validate(eq(CALLBACK_URL), ArgumentMatchers.<Map<String, String>>any(), eq("firma-valida"))).thenReturn(true);
        Notificacion notificacion = new Notificacion();
        when(notificacionRepository.findByProveedorMessageId("SM123")).thenReturn(Optional.of(notificacion));

        ResponseEntity<Void> resultado = controller.recibirEstadoMensaje(params, "firma-valida");

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(notificacion.getEstadoEntrega()).isEqualTo("delivered");
        assertThat(notificacion.getEntregadoEn()).isNotNull();
        verify(notificacionRepository).save(notificacion);
    }

    @Test
    void noMarcaEntregadoEnParaEstadosIntermedios() {
        var params = parametros("SM456", "sent");
        when(requestValidator.validate(eq(CALLBACK_URL), ArgumentMatchers.<Map<String, String>>any(), eq("firma-valida"))).thenReturn(true);
        Notificacion notificacion = new Notificacion();
        when(notificacionRepository.findByProveedorMessageId("SM456")).thenReturn(Optional.of(notificacion));

        controller.recibirEstadoMensaje(params, "firma-valida");

        assertThat(notificacion.getEstadoEntrega()).isEqualTo("sent");
        assertThat(notificacion.getEntregadoEn()).isNull();
    }

    @Test
    void rechazaConFirmaInvalida() {
        var params = parametros("SM123", "delivered");
        when(requestValidator.validate(eq(CALLBACK_URL), ArgumentMatchers.<Map<String, String>>any(), eq("firma-mala"))).thenReturn(false);

        ResponseEntity<Void> resultado = controller.recibirEstadoMensaje(params, "firma-mala");

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(notificacionRepository);
    }

    @Test
    void rechazaSinHeaderDeFirma() {
        var params = parametros("SM123", "delivered");

        ResponseEntity<Void> resultado = controller.recibirEstadoMensaje(params, null);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(notificacionRepository);
    }

    @Test
    void ignoraSidDesconocidoSinFallar() {
        var params = parametros("SM999", "delivered");
        when(requestValidator.validate(eq(CALLBACK_URL), ArgumentMatchers.<Map<String, String>>any(), eq("firma-valida"))).thenReturn(true);
        when(notificacionRepository.findByProveedorMessageId("SM999")).thenReturn(Optional.empty());

        ResponseEntity<Void> resultado = controller.recibirEstadoMensaje(params, "firma-valida");

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificacionRepository, never()).save(any());
    }
}
