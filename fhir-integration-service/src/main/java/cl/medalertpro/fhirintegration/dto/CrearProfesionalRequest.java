package cl.medalertpro.fhirintegration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CrearProfesionalRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    private String especialidad;

    @NotNull
    private Long establecimientoId;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    public Long getEstablecimientoId() { return establecimientoId; }
    public void setEstablecimientoId(Long establecimientoId) { this.establecimientoId = establecimientoId; }
}
