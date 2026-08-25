package cl.medalertpro.portal.dto;

public class SesionResponse {
    private String token;
    private Long pacienteId;
    private String nombre;

    public SesionResponse(String token, Long pacienteId, String nombre) {
        this.token = token;
        this.pacienteId = pacienteId;
        this.nombre = nombre;
    }

    public String getToken() { return token; }
    public Long getPacienteId() { return pacienteId; }
    public String getNombre() { return nombre; }
}
