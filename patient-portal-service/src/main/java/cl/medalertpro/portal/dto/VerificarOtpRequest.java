package cl.medalertpro.portal.dto;

import jakarta.validation.constraints.NotBlank;

public class VerificarOtpRequest {
    @NotBlank
    private String rut;

    @NotBlank
    private String codigo;

    public String getRut() { return rut; }
    public void setRut(String rut) { this.rut = rut; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
}
