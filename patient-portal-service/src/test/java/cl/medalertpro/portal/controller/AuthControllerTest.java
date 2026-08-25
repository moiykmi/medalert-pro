package cl.medalertpro.portal.controller;

import cl.medalertpro.portal.dto.SesionResponse;
import cl.medalertpro.portal.dto.SolicitarOtpRequest;
import cl.medalertpro.portal.dto.VerificarOtpRequest;
import cl.medalertpro.portal.entity.Paciente;
import cl.medalertpro.portal.repository.PacienteRepository;
import cl.medalertpro.portal.service.EmailService;
import cl.medalertpro.portal.service.OtpService;
import cl.medalertpro.portal.service.SesionService;
import cl.medalertpro.portal.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private OtpService otpService;
    @Mock
    private SesionService sesionService;
    @Mock
    private SmsService smsService;
    @Mock
    private EmailService emailService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(pacienteRepository, otpService, sesionService, smsService, emailService);
    }

    private Paciente pacienteSms() {
        Paciente paciente = new Paciente();
        paciente.setId(1L);
        paciente.setRut("11111111-1");
        paciente.setNombre("Juan Perez");
        paciente.setTelefono("+56912345678");
        paciente.setEmail("juan@test.cl");
        paciente.setCanalPreferido("SMS");
        return paciente;
    }

    @Test
    void solicitarOtpConRutNoRegistradoLanza404() {
        SolicitarOtpRequest request = new SolicitarOtpRequest();
        request.setRut("99999999-9");
        when(pacienteRepository.findByRut("99999999-9")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authController.solicitarOtp(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void solicitarOtpConCanalSmsEnviaPorSms() {
        Paciente paciente = pacienteSms();
        SolicitarOtpRequest request = new SolicitarOtpRequest();
        request.setRut(paciente.getRut());
        when(pacienteRepository.findByRut(paciente.getRut())).thenReturn(Optional.of(paciente));
        when(otpService.generar(paciente.getRut())).thenReturn("123456");

        authController.solicitarOtp(request);

        verify(smsService).enviarSms(eq(paciente.getTelefono()), anyString());
        verify(emailService, never()).enviarEmail(anyString(), anyString(), anyString());
    }

    @Test
    void solicitarOtpConCanalEmailEnviaPorEmail() {
        Paciente paciente = pacienteSms();
        paciente.setCanalPreferido("EMAIL");
        SolicitarOtpRequest request = new SolicitarOtpRequest();
        request.setRut(paciente.getRut());
        when(pacienteRepository.findByRut(paciente.getRut())).thenReturn(Optional.of(paciente));
        when(otpService.generar(paciente.getRut())).thenReturn("123456");

        authController.solicitarOtp(request);

        verify(emailService).enviarEmail(eq(paciente.getEmail()), anyString(), anyString());
        verify(smsService, never()).enviarSms(anyString(), anyString());
    }

    @Test
    void solicitarOtpNoPropagaExcepcionSiFallaElEnvio() {
        Paciente paciente = pacienteSms();
        SolicitarOtpRequest request = new SolicitarOtpRequest();
        request.setRut(paciente.getRut());
        when(pacienteRepository.findByRut(paciente.getRut())).thenReturn(Optional.of(paciente));
        when(otpService.generar(paciente.getRut())).thenReturn("123456");
        doThrow(new RuntimeException("proveedor caido")).when(smsService).enviarSms(anyString(), anyString());

        authController.solicitarOtp(request);
    }

    @Test
    void verificarOtpConRutNoRegistradoLanza404() {
        VerificarOtpRequest request = new VerificarOtpRequest();
        request.setRut("99999999-9");
        request.setCodigo("123456");
        when(pacienteRepository.findByRut("99999999-9")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authController.verificarOtp(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void verificarOtpConCodigoInvalidoLanza401() {
        Paciente paciente = pacienteSms();
        VerificarOtpRequest request = new VerificarOtpRequest();
        request.setRut(paciente.getRut());
        request.setCodigo("000000");
        when(pacienteRepository.findByRut(paciente.getRut())).thenReturn(Optional.of(paciente));
        when(otpService.validar(paciente.getRut(), "000000")).thenReturn(false);

        assertThatThrownBy(() -> authController.verificarOtp(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void verificarOtpConCodigoValidoRetornaSesion() {
        Paciente paciente = pacienteSms();
        VerificarOtpRequest request = new VerificarOtpRequest();
        request.setRut(paciente.getRut());
        request.setCodigo("123456");
        when(pacienteRepository.findByRut(paciente.getRut())).thenReturn(Optional.of(paciente));
        when(otpService.validar(paciente.getRut(), "123456")).thenReturn(true);
        when(sesionService.crearSesion(paciente.getId())).thenReturn("token-abc");

        SesionResponse response = authController.verificarOtp(request);

        assertThat(response.getToken()).isEqualTo("token-abc");
        assertThat(response.getPacienteId()).isEqualTo(paciente.getId());
        assertThat(response.getNombre()).isEqualTo(paciente.getNombre());
    }
}
