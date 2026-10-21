package fr.astromate;

import java.time.Clock;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class AstroMateApplication {
    public static void main(String[] args) {
        SpringApplication.run(AstroMateApplication.class, args);
    }

    @Bean
    Clock simulationClock() {
        return Clock.systemUTC();
    }

    @Bean
    ApplicationRunner verifyLocalDatabaseConnection(JdbcTemplate jdbcTemplate) {
        return args -> jdbcTemplate.queryForObject("SELECT sqlite_version()", String.class);
    }
}
