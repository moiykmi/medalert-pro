package cl.medalertpro.portal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public class ActualizarDatosRequest {

    @Pattern(regexp = "^\\+\\d{8,15}$", message = "Teléfono debe incluir código de país, ej: +56912345678")
    private String telefono;

    @Email
    private String email;

    @Pattern(regexp = "SMS|WHATSAPP|EMAIL", message = "Canal debe ser SMS, WHATSAPP o EMAIL")
    private String canalPreferido;

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCanalPreferido() { return canalPreferido; }
    public void setCanalPreferido(String canalPreferido) { this.canalPreferido = canalPreferido; }
}
