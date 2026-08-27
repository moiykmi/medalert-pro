package cl.medalertpro.notification.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AdminDashboardEventDto {

    private Long eventoId;
    private LocalDateTime fechaEvento;
    private String motivo;
    private String estado;
    private Long profesionalId;
    private String profesionalNombre;
    private String profesionalEspecialidad;
    private Long registradoPor;
    private List<String> canales;
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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Long getProfesionalId() {
        return profesionalId;
    }

    public void setProfesionalId(Long profesionalId) {
        this.profesionalId = profesionalId;
    }

    public String getProfesionalNombre() {
        return profesionalNombre;
    }

    public void setProfesionalNombre(String profesionalNombre) {
        this.profesionalNombre = profesionalNombre;
    }

    public String getProfesionalEspecialidad() {
        return profesionalEspecialidad;
    }

    public void setProfesionalEspecialidad(String profesionalEspecialidad) {
        this.profesionalEspecialidad = profesionalEspecialidad;
    }

    public Long getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(Long registradoPor) {
        this.registradoPor = registradoPor;
    }

    public List<String> getCanales() {
        return canales;
    }

    public void setCanales(List<String> canales) {
        this.canales = canales;
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
