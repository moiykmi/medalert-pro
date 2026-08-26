package cl.medalertpro.fhirintegration.dto;

public class MedicoAgendaHoyResponse {

    private String nombre;
    private String especialidad;
    private int citasHoy;

    public MedicoAgendaHoyResponse(String nombre, String especialidad, int citasHoy) {
        this.nombre = nombre;
        this.especialidad = especialidad;
        this.citasHoy = citasHoy;
    }

    public String getNombre() { return nombre; }
    public String getEspecialidad() { return especialidad; }
    public int getCitasHoy() { return citasHoy; }
}
