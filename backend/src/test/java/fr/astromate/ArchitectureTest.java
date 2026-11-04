package fr.astromate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureTest {
    private static final Path SOURCES = Path.of("src/main/java/fr/astromate");
    private static final Pattern REFERENCES = Pattern.compile(
            "(?:fr\\.astromate|org|jakarta|javax|java\\.sql)\\.[\\w.]+");

    @Test
    void domainDependsOnlyOnTheJdkAndDomain() throws IOException {
        assertReferences("domain", List.of("fr.astromate.domain."));
    }

    @Test
    void applicationDependsOnlyOnItsContractsTheDomainAndJdk() throws IOException {
        assertReferences("application", List.of("fr.astromate.domain.", "fr.astromate.application."));
    }

    @Test
    void sqlRemainsInsidePersistenceAdapters() throws IOException {
        try (var files = Files.walk(SOURCES)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.startsWith(SOURCES.resolve("infrastructure/persistence")))
                    .toList()) {
                assertThat(Files.readString(file)).as("SQL outside persistence: %s", file)
                        .doesNotContain("SELECT ", "INSERT ", "UPDATE ", "DELETE ");
            }
        }
    }

    private void assertReferences(String layer, List<String> allowedPrefixes) throws IOException {
        try (var files = Files.walk(SOURCES.resolve(layer))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                var matcher = REFERENCES.matcher(Files.readString(file));
                while (matcher.find()) {
                    String reference = matcher.group();
                    assertThat(allowedPrefixes.stream().anyMatch(reference::startsWith))
                            .as("Forbidden dependency %s in %s", reference, file).isTrue();
                }
            }
        }
    }
}
