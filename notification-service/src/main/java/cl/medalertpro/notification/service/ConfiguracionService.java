package cl.medalertpro.notification.service;

import cl.medalertpro.notification.dto.ActualizarConfiguracionRequest;
import cl.medalertpro.notification.entity.ConfiguracionSistema;
import cl.medalertpro.notification.repository.ConfiguracionSistemaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Configuración global editable desde el admin: qué canales de notificación
 * están habilitados y si los recordatorios preventivos (48h/24h) están
 * activos. Consultada por NotificacionListener, EscalacionScheduler y
 * RecordatorioScheduler antes de enviar cualquier mensaje — no son toggles
 * decorativos.
 */
@Service
public class ConfiguracionService {

    private static final List<String> ORDEN_CANALES = List.of("SMS", "WHATSAPP", "EMAIL");

    private final ConfiguracionSistemaRepository repository;

    public ConfiguracionService(ConfiguracionSistemaRepository repository) {
        this.repository = repository;
    }

    public ConfiguracionSistema obtener() {
        return repository.findById(ConfiguracionSistema.ID_UNICO).orElseGet(() -> repository.save(new ConfiguracionSistema()));
    }

    public ConfiguracionSistema actualizar(ActualizarConfiguracionRequest request) {
        ConfiguracionSistema config = obtener();
        config.setCanalSmsHabilitado(request.getCanalSmsHabilitado());
        config.setCanalWhatsappHabilitado(request.getCanalWhatsappHabilitado());
        config.setCanalEmailHabilitado(request.getCanalEmailHabilitado());
        config.setRecordatorio48hHabilitado(request.getRecordatorio48hHabilitado());
        config.setRecordatorio24hHabilitado(request.getRecordatorio24hHabilitado());
        config.setEscalacionMinutosEspera(request.getEscalacionMinutosEspera());
        config.setEscalacionMaxIntentos(request.getEscalacionMaxIntentos());
        config.setActualizadoEn(LocalDateTime.now());
        return repository.save(config);
    }

    public int escalacionMinutosEspera() {
        return obtener().getEscalacionMinutosEspera();
    }

    public int escalacionMaxIntentos() {
        return obtener().getEscalacionMaxIntentos();
    }

    public boolean isCanalHabilitado(String canal) {
        ConfiguracionSistema c = obtener();
        return switch (canal) {
            case "SMS" -> c.isCanalSmsHabilitado();
            case "WHATSAPP" -> c.isCanalWhatsappHabilitado();
            case "EMAIL" -> c.isCanalEmailHabilitado();
            default -> false;
        };
    }

    /** Canales habilitados, en el orden fijo de escalamiento SMS → WHATSAPP → EMAIL. */
    public List<String> canalesHabilitadosEnOrden() {
        return ORDEN_CANALES.stream().filter(this::isCanalHabilitado).toList();
    }

    /**
     * Resuelve el canal por el que realmente se debe enviar: el preferido del
     * paciente si está habilitado, o el primer canal habilitado disponible.
     * Vacío si el admin deshabilitó los 3 canales.
     */
    public Optional<String> resolverCanalEnvio(String canalPreferido) {
        String normalizado = normalizarCanal(canalPreferido);
        if (isCanalHabilitado(normalizado)) return Optional.of(normalizado);
        return canalesHabilitadosEnOrden().stream().findFirst();
    }

    public boolean isRecordatorio48hHabilitado() {
        return obtener().isRecordatorio48hHabilitado();
    }

    public boolean isRecordatorio24hHabilitado() {
        return obtener().isRecordatorio24hHabilitado();
    }

    private String normalizarCanal(String canalPreferido) {
        if (canalPreferido == null) return "SMS";
        return switch (canalPreferido.toUpperCase()) {
            case "WHATSAPP" -> "WHATSAPP";
            case "EMAIL" -> "EMAIL";
            default -> "SMS";
        };
    }
}
