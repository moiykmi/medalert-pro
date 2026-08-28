package cl.medalertpro.fhirintegration.controller;

import cl.medalertpro.fhirintegration.dto.CrearProfesionalRequest;
import cl.medalertpro.fhirintegration.entity.ProfesionalSalud;
import cl.medalertpro.fhirintegration.repository.CitaRepository;
import cl.medalertpro.fhirintegration.repository.ProfesionalRepository;
import cl.medalertpro.fhirintegration.service.AdminAuthGuard;
import cl.medalertpro.fhirintegration.service.MedicoAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfesionalControllerTest {

    @Mock
    private ProfesionalRepository profesionalRepository;
    @Mock
    private CitaRepository citaRepository;
    @Mock
    private AdminAuthGuard authGuard;
    @Mock
    private MedicoAuthService medicoAuthService;
    @Mock
    private HttpServletRequest httpRequest;

    private ProfesionalController controller;

    @BeforeEach
    void setUp() {
        controller = new ProfesionalController(profesionalRepository, citaRepository, authGuard, medicoAuthService);
    }

    @Test
    void creaElProfesionalConLosDatosRecibidos() {
        CrearProfesionalRequest request = new CrearProfesionalRequest();
        request.setNombre("Klga. Antonia Muñoz");
        request.setEspecialidad("Kinesiología");
        request.setEstablecimientoId(1L);

        when(profesionalRepository.save(any(ProfesionalSalud.class))).thenAnswer(inv -> {
            ProfesionalSalud p = inv.getArgument(0);
            p.setId(21L);
            return p;
        });

        ProfesionalSalud resultado = controller.crear(request, httpRequest);

        assertThat(resultado.getId()).isEqualTo(21L);
        assertThat(resultado.getNombre()).isEqualTo("Klga. Antonia Muñoz");
        assertThat(resultado.getEspecialidad()).isEqualTo("Kinesiología");
        assertThat(resultado.getEstablecimientoId()).isEqualTo(1L);
    }
}
