package cl.medalertpro.fhirintegration.controller;

import cl.medalertpro.fhirintegration.dto.AdminLoginRequest;
import cl.medalertpro.fhirintegration.dto.AdminSesionResponse;
import cl.medalertpro.fhirintegration.service.AdminSesionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    private final AdminSesionService adminSesionService;

    public AdminAuthController(AdminSesionService adminSesionService) {
        this.adminSesionService = adminSesionService;
    }

    @PostMapping("/login")
    public AdminSesionResponse login(@Valid @RequestBody AdminLoginRequest request) {
        return adminSesionService.login(request);
    }
}
