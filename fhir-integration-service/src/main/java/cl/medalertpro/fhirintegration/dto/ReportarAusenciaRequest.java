package cl.medalertpro.fhirintegration.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Cuerpo esperado en POST /medico/ausencia — el médico autenticado reporta su
 * propia ausencia (sin elegir profesionalId: lo determina su sesión).
 */
public class ReportarAusenciaRequest {

    @NotNull
    private LocalDate fecha;

    private LocalTime horaInicio;
    private LocalTime horaFin;

    private String motivo;

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
