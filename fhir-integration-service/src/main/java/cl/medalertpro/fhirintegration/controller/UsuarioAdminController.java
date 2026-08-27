package cl.medalertpro.fhirintegration.controller;

import cl.medalertpro.fhirintegration.dto.CrearUsuarioAdminRequest;
import cl.medalertpro.fhirintegration.dto.SetPasswordRequest;
import cl.medalertpro.fhirintegration.dto.UsuarioAdminResponse;
import cl.medalertpro.fhirintegration.entity.UsuarioAdmin;
import cl.medalertpro.fhirintegration.repository.UsuarioAdminRepository;
import cl.medalertpro.fhirintegration.service.AdminSesionService;
import cl.medalertpro.fhirintegration.service.AdminAuthGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Gestión del personal administrativo con login individual. Solo el acceso
 * maestro (X-Admin-Token estático) puede crear cuentas o asignar/cambiar
 * contraseñas — un usuario_admin con sesión propia no puede escalar
 * privilegios ni crear otras cuentas.
 */
@RestController
@RequestMapping("/admin/usuarios")
public class UsuarioAdminController {

    private final UsuarioAdminRepository repository;
    private final AdminSesionService adminSesionService;
    private final AdminAuthGuard authGuard;

    public UsuarioAdminController(UsuarioAdminRepository repository, AdminSesionService adminSesionService,
                                   AdminAuthGuard authGuard) {
        this.repository = repository;
        this.adminSesionService = adminSesionService;
        this.authGuard = authGuard;
    }

    @GetMapping
    public List<UsuarioAdminResponse> listar(HttpServletRequest request) {
        authGuard.requerirSuperuser(request);
        return repository.findAll().stream().map(UsuarioAdminResponse::desde).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioAdminResponse crear(@Valid @RequestBody CrearUsuarioAdminRequest request, HttpServletRequest httpRequest) {
        authGuard.requerirSuperuser(httpRequest);

        repository.findByEmail(request.getEmail()).ifPresent(u -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese email ya está en uso");
        });

        UsuarioAdmin usuario = new UsuarioAdmin();
        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setRol(request.getRol());
        usuario.setEstablecimientoId(request.getEstablecimientoId());
        usuario.setActivo(true);
        usuario.setPasswordHash(adminSesionService.hashear(request.getPassword()));

        return UsuarioAdminResponse.desde(repository.save(usuario));
    }

    @PutMapping("/{id}/password")
    public void cambiarPassword(@PathVariable Long id, @Valid @RequestBody SetPasswordRequest request,
                                 HttpServletRequest httpRequest) {
        authGuard.requerirSuperuser(httpRequest);

        UsuarioAdmin usuario = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        usuario.setPasswordHash(adminSesionService.hashear(request.getPassword()));
        repository.save(usuario);
    }
}
