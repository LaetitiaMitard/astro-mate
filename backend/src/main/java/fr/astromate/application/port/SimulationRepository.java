package fr.astromate.application.port;

public interface SimulationRepository {
    long elapsedMillis();

    void saveElapsedMillis(long elapsedMillis);
}
