package cl.medalertpro.fhirintegration.dto;

public class ProfesionalConCitasResponse {

    private Long id;
    private String nombre;
    private String especialidad;
    private int citasAgendadas;

    public ProfesionalConCitasResponse(Long id, String nombre, String especialidad, int citasAgendadas) {
        this.id = id;
        this.nombre = nombre;
        this.especialidad = especialidad;
        this.citasAgendadas = citasAgendadas;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEspecialidad() { return especialidad; }
    public int getCitasAgendadas() { return citasAgendadas; }
}
