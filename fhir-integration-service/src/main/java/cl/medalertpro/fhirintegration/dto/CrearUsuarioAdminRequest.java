package cl.medalertpro.fhirintegration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CrearUsuarioAdminRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "ADMIN|PERSONAL_ADMINISTRATIVO|AUDITORIA", message = "rol debe ser ADMIN, PERSONAL_ADMINISTRATIVO o AUDITORIA")
    private String rol;

    @NotNull
    private Long establecimientoId;

    @NotBlank
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Long getEstablecimientoId() { return establecimientoId; }
    public void setEstablecimientoId(Long establecimientoId) { this.establecimientoId = establecimientoId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
