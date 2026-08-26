package cl.medalertpro.fhirintegration.service;

import cl.medalertpro.fhirintegration.dto.CancelacionEventoMessage;
import cl.medalertpro.fhirintegration.dto.RegistrarCancelacionRequest;
import cl.medalertpro.fhirintegration.entity.Cita;
import cl.medalertpro.fhirintegration.entity.EventoCancelacion;
import cl.medalertpro.fhirintegration.entity.Paciente;
import cl.medalertpro.fhirintegration.repository.CitaRepository;
import cl.medalertpro.fhirintegration.repository.EventoCancelacionRepository;
import cl.medalertpro.fhirintegration.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoCancelacionServiceTest {

    private static final String EXCHANGE = "medalert.eventos";
    private static final String ROUTING_KEY = "cancelacion.registrada";

    @Mock
    private EventoCancelacionRepository eventoRepository;

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private EventoCancelacionService service;

    @BeforeEach
    void setUp() {
        service = new EventoCancelacionService(eventoRepository, citaRepository, pacienteRepository, rabbitTemplate);
        ReflectionTestUtils.setField(service, "exchange", EXCHANGE);
        ReflectionTestUtils.setField(service, "routingKey", ROUTING_KEY);

        lenient().when(eventoRepository.save(any(EventoCancelacion.class))).thenAnswer(invocation -> {
            EventoCancelacion evento = invocation.getArgument(0);
            if (evento.getId() == null) {
                ReflectionTestUtils.setField(evento, "id", 100L);
            }
            return evento;
        });
    }

    @Test
    void registrarYPublicar_conCitasAgendadas_lasCancelaYPublicaMensaje() {
        Long profesionalId = 1L;
        LocalDate fecha = LocalDate.of(2026, 8, 1);

        RegistrarCancelacionRequest request = new RegistrarCancelacionRequest();
        request.setProfesionalId(profesionalId);
        request.setFecha(fecha);
        request.setMotivo("Licencia médica");
        request.setRegistradoPor(9L);

        Cita cita1 = new Cita();
        ReflectionTestUtils.setField(cita1, "id", 10L);
        cita1.setPacienteId(20L);
        cita1.setProfesionalId(profesionalId);
        cita1.setFechaHora(fecha.atTime(9, 0));
        cita1.setEstado("AGENDADA");

        Cita cita2 = new Cita();
        ReflectionTestUtils.setField(cita2, "id", 11L);
        cita2.setPacienteId(21L);
        cita2.setProfesionalId(profesionalId);
        cita2.setFechaHora(fecha.atTime(10, 30));
        cita2.setEstado("AGENDADA");

        when(citaRepository.findByProfesionalIdAndEstadoAndFechaHoraBetween(
                eq(profesionalId), eq("AGENDADA"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(cita1, cita2));

        Paciente paciente1 = new Paciente();
        ReflectionTestUtils.setField(paciente1, "id", 20L);
        paciente1.setNombre("Juana Pérez");
        paciente1.setTelefono("+56911111111");
        paciente1.setEmail("juana@example.com");
        paciente1.setCanalPreferido("SMS");

        Paciente paciente2 = new Paciente();
        ReflectionTestUtils.setField(paciente2, "id", 21L);
        paciente2.setNombre("Pedro Soto");
        paciente2.setTelefono("+56922222222");
        paciente2.setEmail("pedro@example.com");
        paciente2.setCanalPreferido("WHATSAPP");

        when(pacienteRepository.findById(20L)).thenReturn(Optional.of(paciente1));
        when(pacienteRepository.findById(21L)).thenReturn(Optional.of(paciente2));

        EventoCancelacion resultado = service.registrarYPublicar(request);

        assertThat(resultado.getEstado()).isEqualTo("COMPLETADO");
        assertThat(resultado.getProfesionalId()).isEqualTo(profesionalId);

        ArgumentCaptor<List<Cita>> citasCaptor = ArgumentCaptor.forClass(List.class);
        verify(citaRepository).saveAll(citasCaptor.capture());
        List<Cita> citasGuardadas = citasCaptor.getValue();
        assertThat(citasGuardadas).hasSize(2);
        assertThat(citasGuardadas).allMatch(c -> "CANCELADA".equals(c.getEstado()));

        ArgumentCaptor<CancelacionEventoMessage> mensajeCaptor = ArgumentCaptor.forClass(CancelacionEventoMessage.class);
        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), mensajeCaptor.capture());

        CancelacionEventoMessage mensaje = mensajeCaptor.getValue();
        assertThat(mensaje.getEventoId()).isEqualTo(100L);
        assertThat(mensaje.getProfesionalId()).isEqualTo(profesionalId);
        assertThat(mensaje.getMotivo()).isEqualTo("Licencia médica");
        assertThat(mensaje.getPacientesAfectados()).hasSize(2);
        assertThat(mensaje.getPacientesAfectados())
                .extracting(CancelacionEventoMessage.PacienteAfectado::getPacienteId)
                .containsExactlyInAnyOrder(20L, 21L);
        assertThat(mensaje.getPacientesAfectados())
                .extracting(CancelacionEventoMessage.PacienteAfectado::getCanalPreferido)
                .containsExactlyInAnyOrder("SMS", "WHATSAPP");

        verify(eventoRepository, times(2)).save(any(EventoCancelacion.class));
    }

    @Test
    void registrarYPublicar_sinCitasAgendadas_publicaMensajeSinPacientesAfectados() {
        Long profesionalId = 2L;
        LocalDate fecha = LocalDate.of(2026, 8, 1);

        RegistrarCancelacionRequest request = new RegistrarCancelacionRequest();
        request.setProfesionalId(profesionalId);
        request.setFecha(fecha);
        request.setMotivo("Sin pacientes agendados");
        request.setRegistradoPor(9L);

        when(citaRepository.findByProfesionalIdAndEstadoAndFechaHoraBetween(
                eq(profesionalId), eq("AGENDADA"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        EventoCancelacion resultado = service.registrarYPublicar(request);

        assertThat(resultado.getEstado()).isEqualTo("COMPLETADO");

        verify(pacienteRepository, never()).findById(anyLong());

        ArgumentCaptor<List<Cita>> citasCaptor = ArgumentCaptor.forClass(List.class);
        verify(citaRepository).saveAll(citasCaptor.capture());
        assertThat(citasCaptor.getValue()).isEmpty();

        ArgumentCaptor<CancelacionEventoMessage> mensajeCaptor = ArgumentCaptor.forClass(CancelacionEventoMessage.class);
        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), mensajeCaptor.capture());
        assertThat(mensajeCaptor.getValue().getPacientesAfectados()).isEmpty();
    }

    @Test
    void registrarYPublicar_conRangoHorario_consultaSoloEseRangoYLoGuardaEnElEvento() {
        Long profesionalId = 1L;
        LocalDate fecha = LocalDate.of(2026, 8, 1);

        RegistrarCancelacionRequest request = new RegistrarCancelacionRequest();
        request.setProfesionalId(profesionalId);
        request.setFecha(fecha);
        request.setHoraInicio(LocalTime.of(9, 0));
        request.setHoraFin(LocalTime.of(12, 0));
        request.setMotivo("Reunión clínica");
        request.setRegistradoPor(9L);

        when(citaRepository.findByProfesionalIdAndEstadoAndFechaHoraBetween(
                eq(profesionalId), eq("AGENDADA"),
                eq(fecha.atTime(9, 0)), eq(fecha.atTime(12, 0))))
                .thenReturn(List.of());

        EventoCancelacion resultado = service.registrarYPublicar(request);

        assertThat(resultado.getHoraInicio()).isEqualTo(LocalTime.of(9, 0));
        assertThat(resultado.getHoraFin()).isEqualTo(LocalTime.of(12, 0));

        verify(citaRepository).findByProfesionalIdAndEstadoAndFechaHoraBetween(
                eq(profesionalId), eq("AGENDADA"), eq(fecha.atTime(9, 0)), eq(fecha.atTime(12, 0)));
    }

    @Test
    void registrarYPublicar_conHoraFinAntesQueHoraInicio_lanzaExcepcionSinTocarNada() {
        RegistrarCancelacionRequest request = new RegistrarCancelacionRequest();
        request.setProfesionalId(1L);
        request.setFecha(LocalDate.of(2026, 8, 1));
        request.setHoraInicio(LocalTime.of(12, 0));
        request.setHoraFin(LocalTime.of(9, 0));

        assertThatThrownBy(() -> service.registrarYPublicar(request))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(eventoRepository, citaRepository, pacienteRepository, rabbitTemplate);
    }
}
