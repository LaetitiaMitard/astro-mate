package fr.astromate.domain.service;

import fr.astromate.domain.model.MetricStatus;

public final class MeasurementTransitions {
    private MeasurementTransitions() {
    }

    public static boolean isAlert(MetricStatus status) {
        return status == MetricStatus.WARNING || status == MetricStatus.CRITICAL;
    }

    public static boolean closesInvalidData(MetricStatus status) {
        return status == MetricStatus.NOMINAL;
    }

    public static String statusLabel(MetricStatus status) {
        return switch (status) {
            case NOMINAL -> "nominal";
            case WARNING -> "avertissement";
            case CRITICAL -> "critique";
            case UNKNOWN -> "inconnu";
            case OFFLINE -> "hors ligne";
        };
    }

    public static String alertLevelLabel(String level) {
        return switch (level) {
            case "WARNING" -> "d’avertissement";
            case "CRITICAL" -> "critique";
            default -> throw new IllegalArgumentException(
                    "Niveau d’alerte non pris en charge : " + level
            );
        };
    }
}
