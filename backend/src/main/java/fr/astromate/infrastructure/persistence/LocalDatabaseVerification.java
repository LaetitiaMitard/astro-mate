package fr.astromate.infrastructure.persistence;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class LocalDatabaseVerification implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public LocalDatabaseVerification(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        jdbc.queryForObject("SELECT sqlite_version()", String.class);
    }
}
