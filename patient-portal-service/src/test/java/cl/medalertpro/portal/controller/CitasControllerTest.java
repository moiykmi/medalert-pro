package cl.medalertpro.portal.controller;

import cl.medalertpro.portal.dto.ReagendarRequest;
import cl.medalertpro.portal.entity.Cita;
import cl.medalertpro.portal.entity.Reagendamiento;
import cl.medalertpro.portal.repository.CitaRepository;
import cl.medalertpro.portal.repository.ReagendamientoRepository;
import cl.medalertpro.portal.service.AuthGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitasControllerTest {

    // Fecha fija dentro del horario laboral (jueves 10:00) — usar LocalDateTime.now()
    // haría los tests intermitentes según la hora real en que corran.
    private static final LocalDateTime FECHA_VALIDA = LocalDateTime.of(2026, 9, 17, 10, 0);

    @Mock
    private CitaRepository citaRepository;
    @Mock
    private ReagendamientoRepository reagendamientoRepository;
    @Mock
    private AuthGuard authGuard;
    @Mock
    private HttpServletRequest httpRequest;

    private CitasController citasController;

    @BeforeEach
    void setUp() {
        citasController = new CitasController(citaRepository, reagendamientoRepository, authGuard);
    }

    @Test
    void misCitasRetornaLasCitasDelPacienteAutenticado() {
        when(authGuard.pacienteAutenticado(httpRequest)).thenReturn(1L);
        Cita cita = new Cita();
        cita.setId(10L);
        cita.setPacienteId(1L);
        when(citaRepository.findByPacienteIdOrderByFechaHoraDesc(1L)).thenReturn(List.of(cita));

        List<Cita> resultado = citasController.misCitas(httpRequest);

        assertThat(resultado).containsExactly(cita);
    }

    private Cita citaCancelada() {
        Cita cita = new Cita();
        cita.setId(10L);
        cita.setPacienteId(1L);
        cita.setProfesionalId(5L);
        cita.setFechaHora(LocalDateTime.of(2026, 9, 10, 10, 0));
        cita.setEstado("CANCELADA");
        return cita;
    }

    private ReagendarRequest request(LocalDateTime nuevaFechaHora) {
        ReagendarRequest r = new ReagendarRequest();
        r.setNuevaFechaHora(nuevaFechaHora);
        return r;
    }

    @Test
    void reagendarConCitaInexistenteLanza404() {
        when(authGuard.pacienteAutenticado(httpRequest)).thenReturn(1L);
        when(citaRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> citasController.reagendar(10L, request(FECHA_VALIDA), httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void reagendarConCitaDeOtroPacienteLanza403() {
        when(authGuard.pacienteAutenticado(httpRequest)).thenReturn(999L);
        Cita cita = citaCancelada();
        when(citaRepository.findById(10L)).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> citasController.reagendar(10L, request(FECHA_VALIDA), httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void reagendarConCitaNoCanceladaLanza409() {
        when(authGuard.pacienteAutenticado(httpRequest)).thenReturn(1L);
        Cita cita = citaCancelada();
        cita.setEstado("AGENDADA");
        when(citaRepository.findById(10L)).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> citasController.reagendar(10L, request(FECHA_VALIDA), httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void reagendarAntesDeLas8amLanza400() {
        when(authGuard.pacienteAutenticado(httpRequest)).thenReturn(1L);
        when(citaRepository.findById(10L)).thenReturn(Optional.of(citaCancelada()));

        assertThatThrownBy(() -> citasController.reagendar(10L, request(LocalDateTime.of(2026, 9, 17, 7, 30)), httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void reagendarDespuesDeLas1730Lanza400() {
        when(authGuard.pacienteAutenticado(httpRequest)).thenReturn(1L);
        when(citaRepository.findById(10L)).thenReturn(Optional.of(citaCancelada()));

        assertThatThrownBy(() -> citasController.reagendar(10L, request(LocalDateTime.of(2026, 9, 17, 17, 31)), httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void reagendarEnHorarioDeColacionLanza400() {
        when(authGuard.pacienteAutenticado(httpRequest)).thenReturn(1L);
        when(citaRepository.findById(10L)).thenReturn(Optional.of(citaCancelada()));

        assertThatThrownBy(() -> citasController.reagendar(10L, request(LocalDateTime.of(2026, 9, 17, 13, 30)), httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void reagendarAlUltimoBloqueDeLasSeisEsValido() {
        when(authGuard.pacienteAutenticado(httpRequest)).thenReturn(1L);
        Cita citaOriginal = citaCancelada();
        when(citaRepository.findById(10L)).thenReturn(Optional.of(citaOriginal));
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime nuevaFecha = LocalDateTime.of(2026, 9, 17, 17, 30);

        Cita resultado = citasController.reagendar(10L, request(nuevaFecha), httpRequest);

        assertThat(resultado.getFechaHora()).isEqualTo(nuevaFecha);
    }

    @Test
    void reagendarConHorarioYaOcupadoPorOtraCitaDelMismoProfesionalLanza409() {
        when(authGuard.pacienteAutenticado(httpRequest)).thenReturn(1L);
        Cita citaOriginal = citaCancelada();
        when(citaRepository.findById(10L)).thenReturn(Optional.of(citaOriginal));
        when(citaRepository.existsByProfesionalIdAndEstadoAndFechaHoraBetween(eq(5L), eq("AGENDADA"), any(), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> citasController.reagendar(10L, request(FECHA_VALIDA), httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
        verify(citaRepository, never()).save(any());
    }

    @Test
    void reagendarConCitaCanceladaCreaNuevaCitaYRegistraReagendamiento() {
        when(authGuard.pacienteAutenticado(httpRequest)).thenReturn(1L);
        Cita citaOriginal = citaCancelada();
        when(citaRepository.findById(10L)).thenReturn(Optional.of(citaOriginal));
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cita resultado = citasController.reagendar(10L, request(FECHA_VALIDA), httpRequest);

        assertThat(resultado.getEstado()).isEqualTo("AGENDADA");
        assertThat(resultado.getFechaHora()).isEqualTo(FECHA_VALIDA);
        assertThat(resultado.getPacienteId()).isEqualTo(1L);
        assertThat(resultado.getProfesionalId()).isEqualTo(5L);
        assertThat(citaOriginal.getEstado()).isEqualTo("REAGENDADA");

        ArgumentCaptor<Reagendamiento> captor = ArgumentCaptor.forClass(Reagendamiento.class);
        verify(reagendamientoRepository).save(captor.capture());
        Reagendamiento reagendamiento = captor.getValue();
        assertThat(reagendamiento.getCitaOriginalId()).isEqualTo(10L);
        assertThat(reagendamiento.getPacienteId()).isEqualTo(1L);
        assertThat(reagendamiento.getEstado()).isEqualTo("CONFIRMADO");
    }
}
