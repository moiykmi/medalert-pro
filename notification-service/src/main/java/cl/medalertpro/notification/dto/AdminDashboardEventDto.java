package cl.medalertpro.notification.dto;

import java.time.LocalDateTime;

public class AdminDashboardEventDto {

    private Long eventoId;
    private LocalDateTime fechaEvento;
    private String motivo;
    private long pacientesNotificados;
    private long notificacionesConfirmadas;
    private Double minutosTotalesNotificacion;
    private LocalDateTime ultimoHito;

    public Long getEventoId() {
        return eventoId;
    }

    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }

    public LocalDateTime getFechaEvento() {
        return fechaEvento;
    }

    public void setFechaEvento(LocalDateTime fechaEvento) {
        this.fechaEvento = fechaEvento;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public long getPacientesNotificados() {
        return pacientesNotificados;
    }

    public void setPacientesNotificados(long pacientesNotificados) {
        this.pacientesNotificados = pacientesNotificados;
    }

    public long getNotificacionesConfirmadas() {
        return notificacionesConfirmadas;
    }

    public void setNotificacionesConfirmadas(long notificacionesConfirmadas) {
        this.notificacionesConfirmadas = notificacionesConfirmadas;
    }

    public Double getMinutosTotalesNotificacion() {
        return minutosTotalesNotificacion;
    }

    public void setMinutosTotalesNotificacion(Double minutosTotalesNotificacion) {
        this.minutosTotalesNotificacion = minutosTotalesNotificacion;
    }

    public LocalDateTime getUltimoHito() {
        return ultimoHito;
    }

    public void setUltimoHito(LocalDateTime ultimoHito) {
        this.ultimoHito = ultimoHito;
    }
}
