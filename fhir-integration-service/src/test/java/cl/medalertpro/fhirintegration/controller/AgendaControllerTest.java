package cl.medalertpro.fhirintegration.controller;

import cl.medalertpro.fhirintegration.entity.Cita;
import cl.medalertpro.fhirintegration.entity.Paciente;
import cl.medalertpro.fhirintegration.entity.ProfesionalSalud;
import cl.medalertpro.fhirintegration.repository.CitaRepository;
import cl.medalertpro.fhirintegration.repository.PacienteRepository;
import cl.medalertpro.fhirintegration.repository.ProfesionalRepository;
import cl.medalertpro.fhirintegration.service.AdminAuthGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendaControllerTest {

    @Mock
    private CitaRepository citaRepository;
    @Mock
    private ProfesionalRepository profesionalRepository;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private AdminAuthGuard authGuard;
    @Mock
    private HttpServletRequest httpRequest;

    private AgendaController controller;

    @BeforeEach
    void setUp() {
        controller = new AgendaController(citaRepository, profesionalRepository, pacienteRepository, authGuard);
    }

    private Cita cita(Long id, Long pacienteId, Long profesionalId, String estado, LocalDateTime fechaHora) {
        Cita c = new Cita();
        ReflectionTestUtils.setField(c, "id", id);
        c.setPacienteId(pacienteId);
        c.setProfesionalId(profesionalId);
        c.setEstado(estado);
        c.setFechaHora(fechaHora);
        return c;
    }

    private Paciente paciente(Long id, String nombre) {
        Paciente p = new Paciente();
        ReflectionTestUtils.setField(p, "id", id);
        p.setNombre(nombre);
        return p;
    }

    private ProfesionalSalud profesional(Long id, String nombre, String especialidad) {
        ProfesionalSalud p = new ProfesionalSalud();
        ReflectionTestUtils.setField(p, "id", id);
        p.setNombre(nombre);
        p.setEspecialidad(especialidad);
        return p;
    }

    @Test
    void agendaDelDiaResuelveNombresDePacienteYProfesional() {
        LocalDateTime fechaHora = LocalDate.of(2026, 8, 27).atTime(9, 0);
        Cita c = cita(1L, 10L, 20L, "AGENDADA", fechaHora);
        when(citaRepository.findByFechaHoraBetweenOrderByFechaHoraAsc(any(), any())).thenReturn(List.of(c));
        when(pacienteRepository.findAllById(List.of(10L))).thenReturn(List.of(paciente(10L, "María González")));
        when(profesionalRepository.findAllById(List.of(20L))).thenReturn(List.of(profesional(20L, "Dr. Fuentes", "Medicina General")));

        var resultado = controller.agendaDelDia(LocalDate.of(2026, 8, 27), httpRequest);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getPacienteNombre()).isEqualTo("María González");
        assertThat(resultado.get(0).getProfesionalNombre()).isEqualTo("Dr. Fuentes");
        assertThat(resultado.get(0).getProfesionalEspecialidad()).isEqualTo("Medicina General");
        assertThat(resultado.get(0).getEstado()).isEqualTo("AGENDADA");
    }

    @Test
    void agendaDelDiaConPacienteOProfesionalInexistenteUsaValorDeReemplazo() {
        LocalDateTime fechaHora = LocalDate.of(2026, 8, 27).atTime(9, 0);
        Cita c = cita(1L, 10L, 20L, "CANCELADA", fechaHora);
        when(citaRepository.findByFechaHoraBetweenOrderByFechaHoraAsc(any(), any())).thenReturn(List.of(c));
        when(pacienteRepository.findAllById(List.of(10L))).thenReturn(List.of());
        when(profesionalRepository.findAllById(List.of(20L))).thenReturn(List.of());

        var resultado = controller.agendaDelDia(LocalDate.of(2026, 8, 27), httpRequest);

        assertThat(resultado.get(0).getPacienteNombre()).isEqualTo("Paciente no encontrado");
        assertThat(resultado.get(0).getProfesionalNombre()).isEqualTo("Profesional no encontrado");
        assertThat(resultado.get(0).getProfesionalEspecialidad()).isNull();
    }

    @Test
    void agendaDelDiaSinCitasDevuelveListaVacia() {
        when(citaRepository.findByFechaHoraBetweenOrderByFechaHoraAsc(any(), any())).thenReturn(List.of());

        var resultado = controller.agendaDelDia(LocalDate.of(2026, 8, 27), httpRequest);

        assertThat(resultado).isEmpty();
    }
}
