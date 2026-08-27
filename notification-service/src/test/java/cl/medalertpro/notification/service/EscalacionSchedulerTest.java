package cl.medalertpro.notification.service;

import cl.medalertpro.notification.entity.EventoCancelacion;
import cl.medalertpro.notification.entity.Notificacion;
import cl.medalertpro.notification.entity.Paciente;
import cl.medalertpro.notification.repository.EventoCancelacionRepository;
import cl.medalertpro.notification.repository.NotificacionRepository;
import cl.medalertpro.notification.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscalacionSchedulerTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private EventoCancelacionRepository eventoRepository;

    @Mock
    private NotificacionDispatchService dispatchService;

    private EscalacionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new EscalacionScheduler(notificacionRepository, pacienteRepository, eventoRepository, dispatchService);
        ReflectionTestUtils.setField(scheduler, "minutosEspera", 60);
    }

    private static Notificacion notificacion(Long id, Long eventoId, Long pacienteId, String canal, short intento, LocalDateTime enviadoEn) {
        Notificacion n = new Notificacion();
        n.setId(id);
        n.setEventoId(eventoId);
        n.setPacienteId(pacienteId);
        n.setCitaId(99L);
        n.setCanal(canal);
        n.setIntentoNumero(intento);
        n.setEstadoEnvio("ENVIADO");
        n.setEnviadoEn(enviadoEn);
        return n;
    }

    @Test
    void escalaUnaNotificacionEnviadaVencidaAlSiguienteCanal() {
        Notificacion stale = notificacion(1L, 100L, 200L, "SMS", (short) 1, LocalDateTime.now().minusMinutes(90));
        when(notificacionRepository.findByEstadoEnvioAndTipoAndConfirmadoEnIsNullAndEnviadoEnBefore(eq("ENVIADO"), eq("CANCELACION"), any(LocalDateTime.class)))
                .thenReturn(List.of(stale));
        when(notificacionRepository.findByEventoIdAndPacienteIdOrderByIntentoNumeroAsc(100L, 200L))
                .thenReturn(List.of(stale));

        Paciente paciente = new Paciente();
        paciente.setId(200L);
        paciente.setNombre("Ana Rios");
        paciente.setTelefono("+56922222222");
        paciente.setEmail("ana@test.cl");
        when(pacienteRepository.findById(200L)).thenReturn(Optional.of(paciente));

        EventoCancelacion evento = new EventoCancelacion();
        evento.setId(100L);
        evento.setMotivo("falta de insumos");
        when(eventoRepository.findById(100L)).thenReturn(Optional.of(evento));

        scheduler.revisarYEscalar();

        ArgumentCaptor<String> canalCaptor = ArgumentCaptor.forClass(String.class);
        verify(dispatchService).enviarYRegistrar(eq(100L), eq(200L), eq(99L), canalCaptor.capture(),
                eq("+56922222222"), eq("ana@test.cl"), anyString(), eq((short) 2));
        assertThat(canalCaptor.getValue()).isEqualTo("WHATSAPP");
    }

    @Test
    void noEscalaCuandoNoHayNotificacionesVencidas() {
        when(notificacionRepository.findByEstadoEnvioAndTipoAndConfirmadoEnIsNullAndEnviadoEnBefore(eq("ENVIADO"), eq("CANCELACION"), any(LocalDateTime.class)))
                .thenReturn(List.of());

        scheduler.revisarYEscalar();

        verifyNoInteractions(dispatchService);
        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void noEscalaYMarcaSinRespuestaAlAlcanzarMaximoDeIntentos() {
        Notificacion agotada = notificacion(2L, 101L, 201L, "EMAIL", (short) 3, LocalDateTime.now().minusMinutes(90));
        when(notificacionRepository.findByEstadoEnvioAndTipoAndConfirmadoEnIsNullAndEnviadoEnBefore(eq("ENVIADO"), eq("CANCELACION"), any(LocalDateTime.class)))
                .thenReturn(List.of(agotada));

        scheduler.revisarYEscalar();

        verifyNoInteractions(dispatchService);
        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        assertThat(captor.getValue().getEstadoEnvio()).isEqualTo("SIN_RESPUESTA");
    }

    @Test
    void noEscalaYMarcaSinRespuestaCuandoYaSeUsaronTodosLosCanales() {
        Notificacion ultima = notificacion(3L, 102L, 202L, "WHATSAPP", (short) 2, LocalDateTime.now().minusMinutes(90));
        Notificacion sms = notificacion(4L, 102L, 202L, "SMS", (short) 1, LocalDateTime.now().minusMinutes(150));
        Notificacion email = notificacion(5L, 102L, 202L, "EMAIL", (short) 2, LocalDateTime.now().minusMinutes(90));

        when(notificacionRepository.findByEstadoEnvioAndTipoAndConfirmadoEnIsNullAndEnviadoEnBefore(eq("ENVIADO"), eq("CANCELACION"), any(LocalDateTime.class)))
                .thenReturn(List.of(ultima));
        when(notificacionRepository.findByEventoIdAndPacienteIdOrderByIntentoNumeroAsc(102L, 202L))
                .thenReturn(List.of(sms, ultima, email));

        scheduler.revisarYEscalar();

        verifyNoInteractions(dispatchService);
        verify(notificacionRepository).save(argThat(n -> "SIN_RESPUESTA".equals(n.getEstadoEnvio())));
    }

    @Test
    void procesaSoloLaNotificacionMasRecientePorParEventoPaciente() {
        Notificacion antigua = notificacion(6L, 103L, 203L, "SMS", (short) 1, LocalDateTime.now().minusMinutes(150));
        Notificacion reciente = notificacion(7L, 103L, 203L, "WHATSAPP", (short) 2, LocalDateTime.now().minusMinutes(90));

        when(notificacionRepository.findByEstadoEnvioAndTipoAndConfirmadoEnIsNullAndEnviadoEnBefore(eq("ENVIADO"), eq("CANCELACION"), any(LocalDateTime.class)))
                .thenReturn(List.of(antigua, reciente));
        when(notificacionRepository.findByEventoIdAndPacienteIdOrderByIntentoNumeroAsc(103L, 203L))
                .thenReturn(List.of(antigua, reciente));

        Paciente paciente = new Paciente();
        paciente.setId(203L);
        paciente.setNombre("Luis Diaz");
        paciente.setTelefono("+56933333333");
        paciente.setEmail("luis@test.cl");
        when(pacienteRepository.findById(203L)).thenReturn(Optional.of(paciente));

        EventoCancelacion evento = new EventoCancelacion();
        evento.setId(103L);
        evento.setMotivo("motivo x");
        when(eventoRepository.findById(103L)).thenReturn(Optional.of(evento));

        scheduler.revisarYEscalar();

        verify(dispatchService).enviarYRegistrar(eq(103L), eq(203L), anyLong(), eq("EMAIL"),
                anyString(), anyString(), anyString(), eq((short) 3));
        verify(notificacionRepository, times(1)).findByEventoIdAndPacienteIdOrderByIntentoNumeroAsc(103L, 203L);
    }
}
