package cl.medalertpro.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Sin este bean, Spring usa un único hilo compartido por defecto para TODOS
 * los @Scheduled (EscalacionScheduler cada 1 min, RecordatorioScheduler a
 * las 09:00). Con un solo hilo, si el envío de un canal se demora o se
 * cuelga (ver el timeout de SMTP en application.yml), ninguna otra
 * escalación ni el recordatorio diario se ejecuta hasta que termine.
 */
@Configuration
public class TaskSchedulerConfig {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("medalert-scheduler-");
        return scheduler;
    }
}
