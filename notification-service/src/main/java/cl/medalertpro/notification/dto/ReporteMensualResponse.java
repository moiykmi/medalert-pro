package cl.medalertpro.notification.dto;

import java.util.List;

public class ReporteMensualResponse {

    private String periodo; // "2026-05"
    private long totalNotificaciones;
    private double porcentajeEntrega;
    private long reagendamientos;

    // Estimación, no una medición real: ver horasAhorradasNotaMetodologica.
    private double horasAhorradasEstimadas;
    private String horasAhorradasNotaMetodologica;

    private double tasaAusentismo;
    private double tasaAusentismoMesAnterior;

    private List<CanalStat> notificacionesPorCanal;
    private List<AusentismoMensualPoint> ausentismoEvolucion;
    private EscalamientoStats escalamientos;

    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }

    public long getTotalNotificaciones() { return totalNotificaciones; }
    public void setTotalNotificaciones(long totalNotificaciones) { this.totalNotificaciones = totalNotificaciones; }

    public double getPorcentajeEntrega() { return porcentajeEntrega; }
    public void setPorcentajeEntrega(double porcentajeEntrega) { this.porcentajeEntrega = porcentajeEntrega; }

    public long getReagendamientos() { return reagendamientos; }
    public void setReagendamientos(long reagendamientos) { this.reagendamientos = reagendamientos; }

    public double getHorasAhorradasEstimadas() { return horasAhorradasEstimadas; }
    public void setHorasAhorradasEstimadas(double horasAhorradasEstimadas) { this.horasAhorradasEstimadas = horasAhorradasEstimadas; }

    public String getHorasAhorradasNotaMetodologica() { return horasAhorradasNotaMetodologica; }
    public void setHorasAhorradasNotaMetodologica(String horasAhorradasNotaMetodologica) { this.horasAhorradasNotaMetodologica = horasAhorradasNotaMetodologica; }

    public double getTasaAusentismo() { return tasaAusentismo; }
    public void setTasaAusentismo(double tasaAusentismo) { this.tasaAusentismo = tasaAusentismo; }

    public double getTasaAusentismoMesAnterior() { return tasaAusentismoMesAnterior; }
    public void setTasaAusentismoMesAnterior(double tasaAusentismoMesAnterior) { this.tasaAusentismoMesAnterior = tasaAusentismoMesAnterior; }

    public List<CanalStat> getNotificacionesPorCanal() { return notificacionesPorCanal; }
    public void setNotificacionesPorCanal(List<CanalStat> notificacionesPorCanal) { this.notificacionesPorCanal = notificacionesPorCanal; }

    public List<AusentismoMensualPoint> getAusentismoEvolucion() { return ausentismoEvolucion; }
    public void setAusentismoEvolucion(List<AusentismoMensualPoint> ausentismoEvolucion) { this.ausentismoEvolucion = ausentismoEvolucion; }

    public EscalamientoStats getEscalamientos() { return escalamientos; }
    public void setEscalamientos(EscalamientoStats escalamientos) { this.escalamientos = escalamientos; }

    public static class CanalStat {
        private String canal;
        private long enviados;
        private double porcentajeEntregado;

        public String getCanal() { return canal; }
        public void setCanal(String canal) { this.canal = canal; }

        public long getEnviados() { return enviados; }
        public void setEnviados(long enviados) { this.enviados = enviados; }

        public double getPorcentajeEntregado() { return porcentajeEntregado; }
        public void setPorcentajeEntregado(double porcentajeEntregado) { this.porcentajeEntregado = porcentajeEntregado; }
    }

    public static class AusentismoMensualPoint {
        private String periodo; // "2026-05"
        private double tasa;

        public String getPeriodo() { return periodo; }
        public void setPeriodo(String periodo) { this.periodo = periodo; }

        public double getTasa() { return tasa; }
        public void setTasa(double tasa) { this.tasa = tasa; }
    }

    public static class EscalamientoStats {
        private long smsAWhatsapp;
        private long whatsappAEmail;
        private long sinContactoDefinitivo;
        private long totalContactados; // pares (evento, paciente) notificados en el período — base para los porcentajes

        public long getSmsAWhatsapp() { return smsAWhatsapp; }
        public void setSmsAWhatsapp(long smsAWhatsapp) { this.smsAWhatsapp = smsAWhatsapp; }

        public long getWhatsappAEmail() { return whatsappAEmail; }
        public void setWhatsappAEmail(long whatsappAEmail) { this.whatsappAEmail = whatsappAEmail; }

        public long getSinContactoDefinitivo() { return sinContactoDefinitivo; }
        public void setSinContactoDefinitivo(long sinContactoDefinitivo) { this.sinContactoDefinitivo = sinContactoDefinitivo; }

        public long getTotalContactados() { return totalContactados; }
        public void setTotalContactados(long totalContactados) { this.totalContactados = totalContactados; }
    }
}
