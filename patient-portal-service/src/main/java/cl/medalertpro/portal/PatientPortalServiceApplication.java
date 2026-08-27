package cl.medalertpro.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class PatientPortalServiceApplication {

    // El servidor (Railway) corre en UTC, pero el consultorio opera en hora de
    // Chile. Sin esto, LocalDate.now()/LocalDateTime.now() (usados para "hoy"
    // en citas, OTP y sesiones) quedan varias horas adelantados respecto al
    // día real del consultorio durante la noche en Chile.
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Santiago"));
    }

    public static void main(String[] args) {
        SpringApplication.run(PatientPortalServiceApplication.class, args);
    }
}
