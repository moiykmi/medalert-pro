package cl.medalertpro.fhirintegration.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Cuerpo esperado en POST /eventos/cancelacion
 * Simula el "registro manual del evento" que hace el personal administrativo,
 * dado que RAS no expone una API pública para disparar esto automáticamente.
 */
public class RegistrarCancelacionRequest {

    @NotNull
    private Long profesionalId;

    @NotNull
    private LocalDate fecha;

    // Rango horario opcional (ej. 09:00 a 12:00). Si ambos quedan en null,
    // se cancela el día completo (comportamiento anterior).
    private LocalTime horaInicio;
    private LocalTime horaFin;

    private String motivo;

    private Long registradoPor; // id del usuario_admin que registra el evento

    public Long getProfesionalId() { return profesionalId; }
    public void setProfesionalId(Long profesionalId) { this.profesionalId = profesionalId; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public Long getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(Long registradoPor) { this.registradoPor = registradoPor; }
}
