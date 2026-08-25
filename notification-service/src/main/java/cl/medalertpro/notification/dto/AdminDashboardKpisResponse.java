package cl.medalertpro.notification.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AdminDashboardKpisResponse {

    private LocalDateTime generadoEn;
    private DeliveryRate deliveryRate;
    private ContactEffectiveness contactEffectiveness;
    private NotificationTime notificationTime;
    private PortalReschedule portalReschedule;
    private ContactUpdate contactUpdate;
    private List<ChannelStatusPoint> channelStatusDistribution;
    private List<WeeklyCancellationPoint> weeklyCancellationHistory;

    public LocalDateTime getGeneradoEn() {
        return generadoEn;
    }

    public void setGeneradoEn(LocalDateTime generadoEn) {
        this.generadoEn = generadoEn;
    }

    public DeliveryRate getDeliveryRate() {
        return deliveryRate;
    }

    public void setDeliveryRate(DeliveryRate deliveryRate) {
        this.deliveryRate = deliveryRate;
    }

    public ContactEffectiveness getContactEffectiveness() {
        return contactEffectiveness;
    }

    public void setContactEffectiveness(ContactEffectiveness contactEffectiveness) {
        this.contactEffectiveness = contactEffectiveness;
    }

    public NotificationTime getNotificationTime() {
        return notificationTime;
    }

    public void setNotificationTime(NotificationTime notificationTime) {
        this.notificationTime = notificationTime;
    }

    public PortalReschedule getPortalReschedule() {
        return portalReschedule;
    }

    public void setPortalReschedule(PortalReschedule portalReschedule) {
        this.portalReschedule = portalReschedule;
    }

    public ContactUpdate getContactUpdate() {
        return contactUpdate;
    }

    public void setContactUpdate(ContactUpdate contactUpdate) {
        this.contactUpdate = contactUpdate;
    }

    public List<ChannelStatusPoint> getChannelStatusDistribution() {
        return channelStatusDistribution;
    }

    public void setChannelStatusDistribution(List<ChannelStatusPoint> channelStatusDistribution) {
        this.channelStatusDistribution = channelStatusDistribution;
    }

    public List<WeeklyCancellationPoint> getWeeklyCancellationHistory() {
        return weeklyCancellationHistory;
    }

    public void setWeeklyCancellationHistory(List<WeeklyCancellationPoint> weeklyCancellationHistory) {
        this.weeklyCancellationHistory = weeklyCancellationHistory;
    }

    public static class DeliveryRate {
        private long totalNotificaciones;
        private long entregasExitosas;
        private double porcentajeExito;

        public long getTotalNotificaciones() {
            return totalNotificaciones;
        }

        public void setTotalNotificaciones(long totalNotificaciones) {
            this.totalNotificaciones = totalNotificaciones;
        }

        public long getEntregasExitosas() {
            return entregasExitosas;
        }

        public void setEntregasExitosas(long entregasExitosas) {
            this.entregasExitosas = entregasExitosas;
        }

        public double getPorcentajeExito() {
            return porcentajeExito;
        }

        public void setPorcentajeExito(double porcentajeExito) {
            this.porcentajeExito = porcentajeExito;
        }
    }

    public static class ContactEffectiveness {
        private long totalContactosEfectivos;
        private long primerIntento;
        private long trasEscalamiento;
        private double porcentajePrimerIntento;
        private double porcentajeTrasEscalamiento;

        public long getTotalContactosEfectivos() {
            return totalContactosEfectivos;
        }

        public void setTotalContactosEfectivos(long totalContactosEfectivos) {
            this.totalContactosEfectivos = totalContactosEfectivos;
        }

        public long getPrimerIntento() {
            return primerIntento;
        }

        public void setPrimerIntento(long primerIntento) {
            this.primerIntento = primerIntento;
        }

        public long getTrasEscalamiento() {
            return trasEscalamiento;
        }

        public void setTrasEscalamiento(long trasEscalamiento) {
            this.trasEscalamiento = trasEscalamiento;
        }

        public double getPorcentajePrimerIntento() {
            return porcentajePrimerIntento;
        }

        public void setPorcentajePrimerIntento(double porcentajePrimerIntento) {
            this.porcentajePrimerIntento = porcentajePrimerIntento;
        }

        public double getPorcentajeTrasEscalamiento() {
            return porcentajeTrasEscalamiento;
        }

        public void setPorcentajeTrasEscalamiento(double porcentajeTrasEscalamiento) {
            this.porcentajeTrasEscalamiento = porcentajeTrasEscalamiento;
        }
    }

    public static class NotificationTime {
        private long totalEventos;
        private double promedioMinutos;
        private double maximoMinutos;

        public long getTotalEventos() {
            return totalEventos;
        }

        public void setTotalEventos(long totalEventos) {
            this.totalEventos = totalEventos;
        }

        public double getPromedioMinutos() {
            return promedioMinutos;
        }

        public void setPromedioMinutos(double promedioMinutos) {
            this.promedioMinutos = promedioMinutos;
        }

        public double getMaximoMinutos() {
            return maximoMinutos;
        }

        public void setMaximoMinutos(double maximoMinutos) {
            this.maximoMinutos = maximoMinutos;
        }
    }

    public static class PortalReschedule {
        private long totalCitasCanceladas;
        private long citasReagendadas;
        private double porcentajeReagendamiento;

        public long getTotalCitasCanceladas() {
            return totalCitasCanceladas;
        }

        public void setTotalCitasCanceladas(long totalCitasCanceladas) {
            this.totalCitasCanceladas = totalCitasCanceladas;
        }

        public long getCitasReagendadas() {
            return citasReagendadas;
        }

        public void setCitasReagendadas(long citasReagendadas) {
            this.citasReagendadas = citasReagendadas;
        }

        public double getPorcentajeReagendamiento() {
            return porcentajeReagendamiento;
        }

        public void setPorcentajeReagendamiento(double porcentajeReagendamiento) {
            this.porcentajeReagendamiento = porcentajeReagendamiento;
        }
    }

    public static class ContactUpdate {
        private long totalPacientes;
        private long pacientesActualizados;
        private double porcentajeActualizados;

        public long getTotalPacientes() {
            return totalPacientes;
        }

        public void setTotalPacientes(long totalPacientes) {
            this.totalPacientes = totalPacientes;
        }

        public long getPacientesActualizados() {
            return pacientesActualizados;
        }

        public void setPacientesActualizados(long pacientesActualizados) {
            this.pacientesActualizados = pacientesActualizados;
        }

        public double getPorcentajeActualizados() {
            return porcentajeActualizados;
        }

        public void setPorcentajeActualizados(double porcentajeActualizados) {
            this.porcentajeActualizados = porcentajeActualizados;
        }
    }

    public static class ChannelStatusPoint {
        private String canal;
        private String estado;
        private long total;

        public String getCanal() {
            return canal;
        }

        public void setCanal(String canal) {
            this.canal = canal;
        }

        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }
    }

    public static class WeeklyCancellationPoint {
        private LocalDate semanaInicio;
        private long totalEventos;

        public LocalDate getSemanaInicio() {
            return semanaInicio;
        }

        public void setSemanaInicio(LocalDate semanaInicio) {
            this.semanaInicio = semanaInicio;
        }

        public long getTotalEventos() {
            return totalEventos;
        }

        public void setTotalEventos(long totalEventos) {
            this.totalEventos = totalEventos;
        }
    }
}
