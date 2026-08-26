package cl.medalertpro.fhirintegration.dto;

public class MedicoSesionResponse {

    private String token;
    private Long profesionalId;
    private String nombre;
    private String especialidad;

    public MedicoSesionResponse(String token, Long profesionalId, String nombre, String especialidad) {
        this.token = token;
        this.profesionalId = profesionalId;
        this.nombre = nombre;
        this.especialidad = especialidad;
    }

    public String getToken() { return token; }
    public Long getProfesionalId() { return profesionalId; }
    public String getNombre() { return nombre; }
    public String getEspecialidad() { return especialidad; }
}
