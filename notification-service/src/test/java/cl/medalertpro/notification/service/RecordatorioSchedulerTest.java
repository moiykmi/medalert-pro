package cl.medalertpro.notification.service;

import cl.medalertpro.notification.entity.Cita;
import cl.medalertpro.notification.entity.Paciente;
import cl.medalertpro.notification.repository.CitaRepository;
import cl.medalertpro.notification.repository.NotificacionRepository;
import cl.medalertpro.notification.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordatorioSchedulerTest {

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private NotificacionDispatchService dispatchService;

    @Mock
    private ConfiguracionService configuracionService;

    private RecordatorioScheduler scheduler;
    private final MensajeBuilder mensajeBuilder = new MensajeBuilder();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mensajeBuilder, "portalUrl", "https://portal.test");
        scheduler = new RecordatorioScheduler(citaRepository, pacienteRepository, notificacionRepository, dispatchService, configuracionService, mensajeBuilder);
        lenient().when(configuracionService.isRecordatorio48hHabilitado()).thenReturn(true);
        lenient().when(configuracionService.isRecordatorio24hHabilitado()).thenReturn(true);
        lenient().when(configuracionService.resolverCanalEnvio(anyString()))
                .thenAnswer(inv -> Optional.of(inv.getArgument(0, String.class)));
    }

    private static Cita cita(Long id, Long pacienteId, LocalDateTime fechaHora) {
        Cita c = new Cita();
        ReflectionTestUtils.setField(c, "id", id);
        c.setPacienteId(pacienteId);
        c.setProfesionalId(1L);
        c.setFechaHora(fechaHora);
        c.setEstado("AGENDADA");
        return c;
    }

    private static Paciente paciente(Long id, String canalPreferido) {
        Paciente p = new Paciente();
        ReflectionTestUtils.setField(p, "id", id);
        p.setNombre("Juana Pérez");
        p.setTelefono("+56911111111");
        p.setEmail("juana@example.com");
        p.setCanalPreferido(canalPreferido);
        return p;
    }

    @Test
    void enviaRecordatorio48hYRecordatorio24hParaLosDiasCorrespondientes() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime fechaCita48h = hoy.plusDays(2).atTime(10, 0);
        LocalDateTime fechaCita24h = hoy.plusDays(1).atTime(11, 0);

        Cita citaEn48h = cita(10L, 100L, fechaCita48h);
        Cita citaEn24h = cita(11L, 101L, fechaCita24h);

        when(citaRepository.findByEstadoAndFechaHoraBetween(eq("AGENDADA"),
                eq(hoy.plusDays(2).atStartOfDay()), any())).thenReturn(List.of(citaEn48h));
        when(citaRepository.findByEstadoAndFechaHoraBetween(eq("AGENDADA"),
                eq(hoy.plusDays(1).atStartOfDay()), any())).thenReturn(List.of(citaEn24h));

        when(pacienteRepository.findById(100L)).thenReturn(Optional.of(paciente(100L, "SMS")));
        when(pacienteRepository.findById(101L)).thenReturn(Optional.of(paciente(101L, "WHATSAPP")));

        scheduler.enviarRecordatorios();

        verify(dispatchService).enviarRecordatorioYRegistrar(eq(10L), eq(100L), eq("RECORDATORIO_48H"),
                eq("SMS"), eq("+56911111111"), eq("juana@example.com"), any(), any());
        verify(dispatchService).enviarRecordatorioYRegistrar(eq(11L), eq(101L), eq("RECORDATORIO_24H"),
                eq("WHATSAPP"), eq("+56911111111"), eq("juana@example.com"), any(), any());
    }

    @Test
    void noReenviaUnRecordatorioYaEnviadoParaLaMismaCita() {
        LocalDate hoy = LocalDate.now();
        Cita citaYaAvisada = cita(20L, 200L, hoy.plusDays(2).atTime(9, 0));

        when(citaRepository.findByEstadoAndFechaHoraBetween(eq("AGENDADA"),
                eq(hoy.plusDays(2).atStartOfDay()), any())).thenReturn(List.of(citaYaAvisada));
        when(citaRepository.findByEstadoAndFechaHoraBetween(eq("AGENDADA"),
                eq(hoy.plusDays(1).atStartOfDay()), any())).thenReturn(List.of());
        when(notificacionRepository.existsByCitaIdAndTipo(20L, "RECORDATORIO_48H")).thenReturn(true);

        scheduler.enviarRecordatorios();

        verifyNoInteractions(dispatchService);
        verify(pacienteRepository, never()).findById(anyLong());
    }

    @Test
    void omiteLaCitaSiElPacienteYaNoExiste() {
        LocalDate hoy = LocalDate.now();
        Cita citaHuerfana = cita(30L, 300L, hoy.plusDays(1).atTime(9, 0));

        when(citaRepository.findByEstadoAndFechaHoraBetween(eq("AGENDADA"),
                eq(hoy.plusDays(2).atStartOfDay()), any())).thenReturn(List.of());
        when(citaRepository.findByEstadoAndFechaHoraBetween(eq("AGENDADA"),
                eq(hoy.plusDays(1).atStartOfDay()), any())).thenReturn(List.of(citaHuerfana));
        when(pacienteRepository.findById(300L)).thenReturn(Optional.empty());

        scheduler.enviarRecordatorios();

        verifyNoInteractions(dispatchService);
    }

    @Test
    void noProcesaElRecordatorio48hSiElAdminLoDeshabilito() {
        LocalDate hoy = LocalDate.now();
        when(configuracionService.isRecordatorio48hHabilitado()).thenReturn(false);
        when(citaRepository.findByEstadoAndFechaHoraBetween(eq("AGENDADA"),
                eq(hoy.plusDays(1).atStartOfDay()), any())).thenReturn(List.of());

        scheduler.enviarRecordatorios();

        verify(citaRepository, never()).findByEstadoAndFechaHoraBetween(eq("AGENDADA"), eq(hoy.plusDays(2).atStartOfDay()), any());
        verifyNoInteractions(dispatchService);
    }

    @Test
    void noEnviaNadaSiElAdminDeshabilitoLosTresCanales() {
        LocalDate hoy = LocalDate.now();
        Cita citaSinCanal = cita(40L, 400L, hoy.plusDays(1).atTime(9, 0));

        when(citaRepository.findByEstadoAndFechaHoraBetween(eq("AGENDADA"),
                eq(hoy.plusDays(2).atStartOfDay()), any())).thenReturn(List.of());
        when(citaRepository.findByEstadoAndFechaHoraBetween(eq("AGENDADA"),
                eq(hoy.plusDays(1).atStartOfDay()), any())).thenReturn(List.of(citaSinCanal));
        when(pacienteRepository.findById(400L)).thenReturn(Optional.of(paciente(400L, "SMS")));
        when(configuracionService.resolverCanalEnvio(anyString())).thenReturn(Optional.empty());

        scheduler.enviarRecordatorios();

        verifyNoInteractions(dispatchService);
    }
}
