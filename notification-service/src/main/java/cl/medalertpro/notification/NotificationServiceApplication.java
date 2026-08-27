package cl.medalertpro.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class NotificationServiceApplication {

    // El servidor (Railway) corre en UTC, pero el consultorio opera en hora de
    // Chile. Sin esto, LocalDate.now()/LocalDateTime.now() (usados en los
    // reportes por mes/semana y en el cálculo de ausentismo) quedan varias
    // horas adelantados respecto al día real del consultorio durante la noche.
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Santiago"));
    }

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
