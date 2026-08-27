package cl.medalertpro.notification.dto;

import java.util.List;

public class BitacoraResponse {

    private String fecha; // "2026-08-27"
    private List<BitacoraEntryDto> entradas;
    private long erroresHoy;

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public List<BitacoraEntryDto> getEntradas() { return entradas; }
    public void setEntradas(List<BitacoraEntryDto> entradas) { this.entradas = entradas; }

    public long getErroresHoy() { return erroresHoy; }
    public void setErroresHoy(long erroresHoy) { this.erroresHoy = erroresHoy; }
}
