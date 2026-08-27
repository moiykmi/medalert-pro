package cl.medalertpro.fhirintegration.dto;

import cl.medalertpro.fhirintegration.entity.UsuarioAdmin;

public class UsuarioAdminResponse {

    private Long id;
    private String nombre;
    private String email;
    private String rol;
    private boolean activo;
    private boolean tieneAcceso; // true si ya tiene password_hash configurado

    public static UsuarioAdminResponse desde(UsuarioAdmin u) {
        UsuarioAdminResponse dto = new UsuarioAdminResponse();
        dto.id = u.getId();
        dto.nombre = u.getNombre();
        dto.email = u.getEmail();
        dto.rol = u.getRol();
        dto.activo = u.isActivo();
        dto.tieneAcceso = u.getPasswordHash() != null;
        return dto;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getRol() { return rol; }
    public boolean isActivo() { return activo; }
    public boolean isTieneAcceso() { return tieneAcceso; }
}
