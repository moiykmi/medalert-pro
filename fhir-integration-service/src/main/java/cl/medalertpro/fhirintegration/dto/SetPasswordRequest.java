package cl.medalertpro.fhirintegration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SetPasswordRequest {

    @NotBlank
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
