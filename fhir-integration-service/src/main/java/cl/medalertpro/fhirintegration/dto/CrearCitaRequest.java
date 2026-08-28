package cl.medalertpro.fhirintegration.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class CrearCitaRequest {

    @NotNull
    private Long pacienteId;

    @NotNull
    private Long profesionalId;

    @NotNull
    private LocalDateTime fechaHora;

    public Long getPacienteId() { return pacienteId; }
    public void setPacienteId(Long pacienteId) { this.pacienteId = pacienteId; }

    public Long getProfesionalId() { return profesionalId; }
    public void setProfesionalId(Long profesionalId) { this.profesionalId = profesionalId; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
}
