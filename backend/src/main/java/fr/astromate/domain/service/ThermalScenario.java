package fr.astromate.domain.service;

public final class ThermalScenario {
    public static final long CYCLE_MILLIS = 240_000;

    private ThermalScenario() {
    }

    public static Temperatures temperatures(long elapsedMillis) {
        long phase = elapsedMillis / 60_000;
        return new Temperatures(
                phase == 1 ? 27 : phase == 2 ? 31 : 22.1,
                phase == 1 ? 27 : phase == 2 ? 31 : 22.4
        );
    }

    public record Temperatures(double radiatorA, double radiatorB) {
    }
}
