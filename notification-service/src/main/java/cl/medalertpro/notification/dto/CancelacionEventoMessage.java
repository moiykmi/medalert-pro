package cl.medalertpro.notification.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Debe coincidir exactamente con CancelacionEventoMessage de fhir-integration-service,
 * ya que es el mismo mensaje serializado en JSON que viaja por la cola.
 */
public class CancelacionEventoMessage {

    private Long eventoId;
    private Long profesionalId;
    private String motivo;
    private LocalDateTime fechaEvento;
    private List<PacienteAfectado> pacientesAfectados;

    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }

    public Long getProfesionalId() { return profesionalId; }
    public void setProfesionalId(Long profesionalId) { this.profesionalId = profesionalId; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public LocalDateTime getFechaEvento() { return fechaEvento; }
    public void setFechaEvento(LocalDateTime fechaEvento) { this.fechaEvento = fechaEvento; }

    public List<PacienteAfectado> getPacientesAfectados() { return pacientesAfectados; }
    public void setPacientesAfectados(List<PacienteAfectado> pacientesAfectados) { this.pacientesAfectados = pacientesAfectados; }

    public static class PacienteAfectado {
        private Long pacienteId;
        private Long citaId;
        private String nombre;
        private String telefono;
        private String email;
        private String canalPreferido;

        public Long getPacienteId() { return pacienteId; }
        public void setPacienteId(Long pacienteId) { this.pacienteId = pacienteId; }

        public Long getCitaId() { return citaId; }
        public void setCitaId(Long citaId) { this.citaId = citaId; }

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }

        public String getTelefono() { return telefono; }
        public void setTelefono(String telefono) { this.telefono = telefono; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getCanalPreferido() { return canalPreferido; }
        public void setCanalPreferido(String canalPreferido) { this.canalPreferido = canalPreferido; }
    }
}
