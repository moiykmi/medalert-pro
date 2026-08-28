package cl.medalertpro.fhirintegration.controller;

import cl.medalertpro.fhirintegration.dto.CrearCitaRequest;
import cl.medalertpro.fhirintegration.entity.Cita;
import cl.medalertpro.fhirintegration.repository.CitaRepository;
import cl.medalertpro.fhirintegration.repository.NotificacionRepository;
import cl.medalertpro.fhirintegration.repository.PacienteRepository;
import cl.medalertpro.fhirintegration.repository.ProfesionalRepository;
import cl.medalertpro.fhirintegration.repository.ReagendamientoRepository;
import cl.medalertpro.fhirintegration.service.AdminAuthGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCitaControllerTest {

    @Mock
    private CitaRepository citaRepository;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private ProfesionalRepository profesionalRepository;
    @Mock
    private ReagendamientoRepository reagendamientoRepository;
    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private AdminAuthGuard authGuard;
    @Mock
    private HttpServletRequest httpRequest;

    private AdminCitaController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminCitaController(citaRepository, pacienteRepository, profesionalRepository, reagendamientoRepository, notificacionRepository, authGuard);
    }

    private CrearCitaRequest request(Long pacienteId, Long profesionalId) {
        return request(pacienteId, profesionalId, LocalDateTime.of(2026, 9, 1, 10, 0));
    }

    private CrearCitaRequest request(Long pacienteId, Long profesionalId, LocalDateTime fechaHora) {
        CrearCitaRequest r = new CrearCitaRequest();
        r.setPacienteId(pacienteId);
        r.setProfesionalId(profesionalId);
        r.setFechaHora(fechaHora);
        return r;
    }

    @Test
    void creaLaCitaComoAgendada() {
        when(pacienteRepository.existsById(1L)).thenReturn(true);
        when(profesionalRepository.existsById(2L)).thenReturn(true);
        when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

        Cita resultado = controller.crear(request(1L, 2L), httpRequest);

        assertThat(resultado.getPacienteId()).isEqualTo(1L);
        assertThat(resultado.getProfesionalId()).isEqualTo(2L);
        assertThat(resultado.getEstado()).isEqualTo("AGENDADA");
    }

    @Test
    void conHorarioFueraDeJornadaLanza400() {
        when(pacienteRepository.existsById(1L)).thenReturn(true);
        when(profesionalRepository.existsById(2L)).thenReturn(true);

        assertThatThrownBy(() -> controller.crear(request(1L, 2L, LocalDateTime.of(2026, 9, 1, 18, 0)), httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void conHorarioDeColacionLanza400() {
        when(pacienteRepository.existsById(1L)).thenReturn(true);
        when(profesionalRepository.existsById(2L)).thenReturn(true);

        assertThatThrownBy(() -> controller.crear(request(1L, 2L, LocalDateTime.of(2026, 9, 1, 13, 30)), httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void conHorarioSolapadoConOtraCitaDelMismoProfesionalLanza409() {
        when(pacienteRepository.existsById(1L)).thenReturn(true);
        when(profesionalRepository.existsById(2L)).thenReturn(true);
        when(citaRepository.findByProfesionalIdAndEstadoAndFechaHoraBetween(eq(2L), eq("AGENDADA"), any(), any()))
                .thenReturn(List.of(new Cita()));

        assertThatThrownBy(() -> controller.crear(request(1L, 2L), httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
        verify(citaRepository, never()).save(any());
    }

    @Test
    void conPacienteInexistenteLanza404() {
        when(pacienteRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> controller.crear(request(999L, 2L), httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void conProfesionalInexistenteLanza404() {
        when(pacienteRepository.existsById(1L)).thenReturn(true);
        when(profesionalRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> controller.crear(request(1L, 999L), httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void eliminaLaCitaExistenteYSusReagendamientosAsociados() {
        when(citaRepository.existsById(27L)).thenReturn(true);

        controller.eliminar(27L, httpRequest);

        verify(reagendamientoRepository).deleteByCitaOriginalIdOrCitaNuevaId(27L, 27L);
        verify(notificacionRepository).deleteByCitaId(27L);
        verify(citaRepository).deleteById(27L);
    }

    @Test
    void eliminarConCitaInexistenteLanza404() {
        when(citaRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> controller.eliminar(999L, httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        verify(citaRepository, never()).deleteById(any());
    }
}
