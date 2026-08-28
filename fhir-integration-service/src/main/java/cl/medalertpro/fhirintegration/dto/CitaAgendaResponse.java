package cl.medalertpro.fhirintegration.dto;

import java.time.LocalDateTime;

public class CitaAgendaResponse {

    private Long id;
    private LocalDateTime fechaHora;
    private String estado;
    private Long pacienteId;
    private String pacienteNombre;
    private Long profesionalId;
    private String profesionalNombre;
    private String profesionalEspecialidad;

    public CitaAgendaResponse(Long id, LocalDateTime fechaHora, String estado, Long pacienteId, String pacienteNombre,
                               Long profesionalId, String profesionalNombre, String profesionalEspecialidad) {
        this.id = id;
        this.fechaHora = fechaHora;
        this.estado = estado;
        this.pacienteId = pacienteId;
        this.pacienteNombre = pacienteNombre;
        this.profesionalId = profesionalId;
        this.profesionalNombre = profesionalNombre;
        this.profesionalEspecialidad = profesionalEspecialidad;
    }

    public Long getId() { return id; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getEstado() { return estado; }
    public Long getPacienteId() { return pacienteId; }
    public String getPacienteNombre() { return pacienteNombre; }
    public Long getProfesionalId() { return profesionalId; }
    public String getProfesionalNombre() { return profesionalNombre; }
    public String getProfesionalEspecialidad() { return profesionalEspecialidad; }
}
