package fr.astromate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AstroMateApplication {
    public static void main(String[] args) {
        SpringApplication.run(AstroMateApplication.class, args);
    }
}
