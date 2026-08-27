package cl.medalertpro.fhirintegration.dto;

public class ProfesionalConCitasResponse {

    private Long id;
    private String nombre;
    private String especialidad;
    private int citasAgendadas;
    private String email; // null si el profesional aún no tiene acceso al portal médico configurado

    public ProfesionalConCitasResponse(Long id, String nombre, String especialidad, int citasAgendadas, String email) {
        this.id = id;
        this.nombre = nombre;
        this.especialidad = especialidad;
        this.citasAgendadas = citasAgendadas;
        this.email = email;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEspecialidad() { return especialidad; }
    public int getCitasAgendadas() { return citasAgendadas; }
    public String getEmail() { return email; }
}
