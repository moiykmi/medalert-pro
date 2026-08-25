package cl.medalertpro.notification.service;

import cl.medalertpro.notification.entity.Notificacion;
import cl.medalertpro.notification.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionDispatchServiceTest {

    @Mock
    private SmsService smsService;

    @Mock
    private WhatsAppService whatsAppService;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificacionRepository notificacionRepository;

    private NotificacionDispatchService dispatchService;

    @BeforeEach
    void setUp() {
        dispatchService = new NotificacionDispatchService(smsService, whatsAppService, emailService, notificacionRepository);
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void enviaPorSmsYRegistraEnviado() {
        when(smsService.enviarSms("+56911111111", "texto")).thenReturn("SID-SMS-1");

        Notificacion resultado = dispatchService.enviarYRegistrar(1L, 2L, 3L, "SMS", "+56911111111", "a@b.com", "texto", (short) 1);

        assertThat(resultado.getCanal()).isEqualTo("SMS");
        assertThat(resultado.getEstadoEnvio()).isEqualTo("ENVIADO");
        assertThat(resultado.getProveedorMessageId()).isEqualTo("SID-SMS-1");
        assertThat(resultado.getEnviadoEn()).isNotNull();
        verify(smsService).enviarSms("+56911111111", "texto");
        verifyNoInteractions(whatsAppService, emailService);
    }

    @Test
    void enviaPorWhatsAppYRegistraEnviado() {
        when(whatsAppService.enviarWhatsApp(anyString(), anyString())).thenReturn("SID-WA-1");

        Notificacion resultado = dispatchService.enviarYRegistrar(1L, 2L, 3L, "WHATSAPP", "+56911111111", "a@b.com", "texto", (short) 2);

        assertThat(resultado.getCanal()).isEqualTo("WHATSAPP");
        assertThat(resultado.getEstadoEnvio()).isEqualTo("ENVIADO");
        assertThat(resultado.getProveedorMessageId()).isEqualTo("SID-WA-1");
        verify(whatsAppService).enviarWhatsApp("+56911111111", "texto");
        verifyNoInteractions(smsService, emailService);
    }

    @Test
    void enviaPorEmailYRegistraEnviado() {
        when(emailService.enviarEmail(anyString(), anyString(), anyString())).thenReturn("mailtrap-123");

        Notificacion resultado = dispatchService.enviarYRegistrar(1L, 2L, 3L, "EMAIL", "+56911111111", "a@b.com", "texto", (short) 3);

        assertThat(resultado.getCanal()).isEqualTo("EMAIL");
        assertThat(resultado.getEstadoEnvio()).isEqualTo("ENVIADO");
        assertThat(resultado.getProveedorMessageId()).isEqualTo("mailtrap-123");
        verify(emailService).enviarEmail(eq("a@b.com"), eq("Cita médica cancelada — MedAlert Pro"), eq("texto"));
        verifyNoInteractions(smsService, whatsAppService);
    }

    @Test
    void marcaFallidoCuandoElProveedorLanzaExcepcion() {
        when(smsService.enviarSms(anyString(), anyString())).thenThrow(new RuntimeException("Twilio down"));

        Notificacion resultado = dispatchService.enviarYRegistrar(1L, 2L, 3L, "SMS", "+56911111111", "a@b.com", "texto", (short) 1);

        assertThat(resultado.getEstadoEnvio()).isEqualTo("FALLIDO");
        assertThat(resultado.getProveedorMessageId()).isNull();
        assertThat(resultado.getEnviadoEn()).isNull();
    }

    @Test
    void marcaFallidoParaCanalDesconocidoSinLanzarAlLlamador() {
        Notificacion resultado = dispatchService.enviarYRegistrar(1L, 2L, 3L, "FAX", "+56911111111", "a@b.com", "texto", (short) 1);

        assertThat(resultado.getEstadoEnvio()).isEqualTo("FALLIDO");
        verifyNoInteractions(smsService, whatsAppService, emailService);
    }

    @Test
    void persisteSiemprePorRepositorioConLosDatosDelEnvio() {
        when(smsService.enviarSms(anyString(), anyString())).thenReturn("sid");

        dispatchService.enviarYRegistrar(10L, 20L, 30L, "SMS", "t", "e", "texto", (short) 1);

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        assertThat(captor.getValue().getEventoId()).isEqualTo(10L);
        assertThat(captor.getValue().getPacienteId()).isEqualTo(20L);
        assertThat(captor.getValue().getCitaId()).isEqualTo(30L);
        assertThat(captor.getValue().getIntentoNumero()).isEqualTo((short) 1);
    }
}
