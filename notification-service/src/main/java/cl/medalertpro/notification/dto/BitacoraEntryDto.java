package cl.medalertpro.notification.dto;

import java.time.LocalDateTime;

public class BitacoraEntryDto {

    private LocalDateTime fecha;
    private String tipo; // EVENTO_REGISTRADO | NOTIFICACIONES_ENVIADAS | ESCALAMIENTO | RECORDATORIO | REAGENDAMIENTO
    private String titulo;
    private String detalle;

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
}
