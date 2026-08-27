package cl.medalertpro.notification.controller;

import cl.medalertpro.notification.dto.ActualizarConfiguracionRequest;
import cl.medalertpro.notification.dto.ConfiguracionResponse;
import cl.medalertpro.notification.service.AdminAuthGuard;
import cl.medalertpro.notification.service.ConfiguracionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/configuracion")
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;
    private final AdminAuthGuard authGuard;

    public ConfiguracionController(ConfiguracionService configuracionService, AdminAuthGuard authGuard) {
        this.configuracionService = configuracionService;
        this.authGuard = authGuard;
    }

    @GetMapping
    public ConfiguracionResponse obtener(HttpServletRequest request) {
        authGuard.validar(request);
        return ConfiguracionResponse.desde(configuracionService.obtener());
    }

    @PutMapping
    public ConfiguracionResponse actualizar(@Valid @RequestBody ActualizarConfiguracionRequest body, HttpServletRequest request) {
        authGuard.validar(request);
        return ConfiguracionResponse.desde(configuracionService.actualizar(body));
    }
}
