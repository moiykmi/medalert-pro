package cl.medalertpro.portal.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class ReagendarRequest {
    @NotNull
    @Future(message = "La nueva fecha debe ser en el futuro")
    private LocalDateTime nuevaFechaHora;

    public LocalDateTime getNuevaFechaHora() { return nuevaFechaHora; }
    public void setNuevaFechaHora(LocalDateTime nuevaFechaHora) { this.nuevaFechaHora = nuevaFechaHora; }
}
