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
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * HU09: autenticación de pacientes mediante RUT + OTP (código de un solo uso
 * enviado por SMS o email, según el canal_preferido del paciente).
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final PacienteRepository pacienteRepository;
    private final OtpService otpService;
    private final SesionService sesionService;
    private final SmsService smsService;
    private final EmailService emailService;

    public AuthController(PacienteRepository pacienteRepository, OtpService otpService,
                           SesionService sesionService, SmsService smsService, EmailService emailService) {
        this.pacienteRepository = pacienteRepository;
        this.otpService = otpService;
        this.sesionService = sesionService;
        this.smsService = smsService;
        this.emailService = emailService;
    }

    @PostMapping("/solicitar-otp")
    @ResponseStatus(HttpStatus.OK)
    public void solicitarOtp(@Valid @RequestBody SolicitarOtpRequest request) {
        Paciente paciente = pacienteRepository.findByRut(request.getRut())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "RUT no registrado"));

        String codigo = otpService.generar(paciente.getRut());
        String texto = "MedAlert Pro: su codigo de verificacion es " + codigo + ". Valido por 5 minutos.";

        // Nota de desarrollo: se deja el código también en el log del servidor
        // para poder probar sin depender de que llegue el SMS/email real.
        log.info("OTP generado para RUT {} ({}): {}", paciente.getRut(), paciente.getNombre(), codigo);

        try {
            if ("EMAIL".equalsIgnoreCase(paciente.getCanalPreferido())) {
                emailService.enviarEmail(paciente.getEmail(), "Tu código de acceso — MedAlert Pro", texto);
            } else {
                smsService.enviarSms(paciente.getTelefono(), texto);
            }
        } catch (Exception e) {
            log.error("No se pudo enviar el OTP a {}: {}", paciente.getRut(), e.getMessage());
            // No se lanza excepción al cliente: el código igual quedó generado y
            // válido (queda en el log de desarrollo), para no bloquear la prueba
            // por una falla puntual del proveedor de SMS/email.
        }
    }

    @PostMapping("/verificar-otp")
    public SesionResponse verificarOtp(@Valid @RequestBody VerificarOtpRequest request) {
        Paciente paciente = pacienteRepository.findByRut(request.getRut())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "RUT no registrado"));

        boolean valido = otpService.validar(paciente.getRut(), request.getCodigo());
        if (!valido) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código inválido o expirado");
        }

        String token = sesionService.crearSesion(paciente.getId());
        return new SesionResponse(token, paciente.getId(), paciente.getNombre());
    }
}
