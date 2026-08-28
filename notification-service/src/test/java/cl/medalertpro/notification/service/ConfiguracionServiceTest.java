package cl.medalertpro.notification.service;

import cl.medalertpro.notification.dto.ActualizarConfiguracionRequest;
import cl.medalertpro.notification.entity.ConfiguracionSistema;
import cl.medalertpro.notification.repository.ConfiguracionSistemaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguracionServiceTest {

    @Mock
    private ConfiguracionSistemaRepository repository;

    private ConfiguracionService service;

    @BeforeEach
    void setUp() {
        service = new ConfiguracionService(repository);
    }

    private ConfiguracionSistema configConTodoHabilitado() {
        ConfiguracionSistema c = new ConfiguracionSistema();
        c.setCanalSmsHabilitado(true);
        c.setCanalWhatsappHabilitado(true);
        c.setCanalEmailHabilitado(true);
        return c;
    }

    @Test
    void resolverCanalEnvio_conElPreferidoHabilitado_devuelveElPreferido() {
        when(repository.findById(ConfiguracionSistema.ID_UNICO)).thenReturn(Optional.of(configConTodoHabilitado()));

        assertThat(service.resolverCanalEnvio("WHATSAPP")).contains("WHATSAPP");
    }

    @Test
    void resolverCanalEnvio_conElPreferidoDeshabilitado_caeAlSiguienteHabilitadoEnOrden() {
        ConfiguracionSistema c = configConTodoHabilitado();
        c.setCanalSmsHabilitado(false);
        when(repository.findById(ConfiguracionSistema.ID_UNICO)).thenReturn(Optional.of(c));

        assertThat(service.resolverCanalEnvio("SMS")).contains("WHATSAPP");
    }

    @Test
    void resolverCanalEnvio_conLosTresCanalesDeshabilitados_devuelveVacio() {
        ConfiguracionSistema c = configConTodoHabilitado();
        c.setCanalSmsHabilitado(false);
        c.setCanalWhatsappHabilitado(false);
        c.setCanalEmailHabilitado(false);
        when(repository.findById(ConfiguracionSistema.ID_UNICO)).thenReturn(Optional.of(c));

        assertThat(service.resolverCanalEnvio("SMS")).isEmpty();
    }

    @Test
    void resolverCanalEnvio_sinCanalPreferido_usaSmsComoDefecto() {
        when(repository.findById(ConfiguracionSistema.ID_UNICO)).thenReturn(Optional.of(configConTodoHabilitado()));

        assertThat(service.resolverCanalEnvio(null)).contains("SMS");
    }

    @Test
    void obtener_sinFilaEnBaseDeDatos_creaUnaPorDefectoConTodoHabilitado() {
        when(repository.findById(ConfiguracionSistema.ID_UNICO)).thenReturn(Optional.empty());
        when(repository.save(any(ConfiguracionSistema.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfiguracionSistema resultado = service.obtener();

        assertThat(resultado.isCanalSmsHabilitado()).isTrue();
        assertThat(resultado.isRecordatorio48hHabilitado()).isTrue();
    }

    @Test
    void actualizar_guardaLosNuevosValoresYActualizaLaMarcaDeTiempo() {
        ConfiguracionSistema existente = configConTodoHabilitado();
        when(repository.findById(ConfiguracionSistema.ID_UNICO)).thenReturn(Optional.of(existente));
        when(repository.save(any(ConfiguracionSistema.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarConfiguracionRequest request = new ActualizarConfiguracionRequest();
        request.setCanalSmsHabilitado(true);
        request.setCanalWhatsappHabilitado(false);
        request.setCanalEmailHabilitado(true);
        request.setRecordatorio48hHabilitado(false);
        request.setRecordatorio24hHabilitado(true);
        request.setEscalacionMinutosEspera(5);
        request.setEscalacionMaxIntentos(2);

        ConfiguracionSistema actualizado = service.actualizar(request);

        assertThat(actualizado.isCanalWhatsappHabilitado()).isFalse();
        assertThat(actualizado.isRecordatorio48hHabilitado()).isFalse();
        assertThat(actualizado.getEscalacionMinutosEspera()).isEqualTo(5);
        assertThat(actualizado.getEscalacionMaxIntentos()).isEqualTo(2);
        assertThat(actualizado.getActualizadoEn()).isNotNull();
    }

    @Test
    void escalacionMinutosEspera_devuelveElValorConfigurado() {
        ConfiguracionSistema c = configConTodoHabilitado();
        c.setEscalacionMinutosEspera(15);
        c.setEscalacionMaxIntentos(4);
        when(repository.findById(ConfiguracionSistema.ID_UNICO)).thenReturn(Optional.of(c));

        assertThat(service.escalacionMinutosEspera()).isEqualTo(15);
        assertThat(service.escalacionMaxIntentos()).isEqualTo(4);
    }
}
