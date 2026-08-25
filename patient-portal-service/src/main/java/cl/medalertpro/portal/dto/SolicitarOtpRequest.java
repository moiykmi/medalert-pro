package cl.medalertpro.portal.dto;

import jakarta.validation.constraints.NotBlank;

public class SolicitarOtpRequest {
    @NotBlank
    private String rut;

    public String getRut() { return rut; }
    public void setRut(String rut) { this.rut = rut; }
}
