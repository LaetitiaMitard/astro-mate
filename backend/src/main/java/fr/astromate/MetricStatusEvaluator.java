package fr.astromate;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

enum MetricStatus {
    NOMINAL,
    WARNING,
    CRITICAL,
    UNKNOWN,
    OFFLINE
}

final class MetricStatusEvaluator {
    private static final Map<String, Rule> RULES = Map.of(
            "oxygen", new Rule("% vol.", 19.0, 23.0, true, true, 17.0, 25.0, 0.0, 100.0),
            "carbonDioxide", new Rule("ppm", 0.0, 1000.0, true, true, null, 2000.0, 0.0, null),
            "temperature", new Rule("°C", 18.0, 26.0, true, true, 15.0, 30.0, -273.15, null),
            "radiatorATemperature", new Rule("°C", 18.0, 26.0, true, true, 15.0, 30.0, -273.15, null),
            "radiatorBTemperature", new Rule("°C", 18.0, 26.0, true, true, 15.0, 30.0, -273.15, null),
            "availableEnergy", new Rule("%", 20.0, 100.0, true, true, 10.0, null, 0.0, 100.0),
            "fluidFlow", new Rule("L/min", 80.0, 200.0, true, true, null, null, 0.0, null),
            "pressure", new Rule("bar", 1.5, 3.0, true, true, null, null, 0.0, null),
            "thermalConsumption", new Rule("kW", null, 2.0, true, false, null, null, 0.0, null)
    );

    private MetricStatusEvaluator() {
    }

    static Rule ruleFor(String metricKey) {
        Rule rule = metricKey == null ? null : RULES.get(metricKey);
        if (rule == null) {
            throw new IllegalArgumentException("Métrique inconnue : " + metricKey);
        }
        return rule;
    }

    static MetricStatus evaluate(String metricKey, Double value) {
        if (value == null || !isPhysicallyValid(metricKey, value)) {
            return MetricStatus.UNKNOWN;
        }

        Rule rule = ruleFor(metricKey);
        if ((rule.criticalMin() != null && value < rule.criticalMin())
                || (rule.criticalMax() != null && value > rule.criticalMax())) {
            return MetricStatus.CRITICAL;
        }
        if (!withinNominalRange(rule, value)) {
            return MetricStatus.WARNING;
        }
        return MetricStatus.NOMINAL;
    }

    static boolean isPhysicallyValid(String metricKey, double value) {
        Rule rule = metricKey == null ? null : RULES.get(metricKey);
        return rule != null
                && Double.isFinite(value)
                && (rule.physicalMin() == null || value >= rule.physicalMin())
                && (rule.physicalMax() == null || value <= rule.physicalMax());
    }

    static MetricStatus mostSevere(Collection<MetricStatus> statuses) {
        return statuses.stream()
                .max(Comparator.comparingInt(MetricStatusEvaluator::priority))
                .orElse(MetricStatus.NOMINAL);
    }

    private static boolean withinNominalRange(Rule rule, double value) {
        boolean aboveMin = rule.nominalMin() == null
                || (rule.minInclusive()
                        ? value >= rule.nominalMin()
                        : value > rule.nominalMin());
        boolean belowMax = rule.nominalMax() == null
                || (rule.maxInclusive()
                        ? value <= rule.nominalMax()
                        : value < rule.nominalMax());
        return aboveMin && belowMax;
    }

    private static int priority(MetricStatus status) {
        return switch (status) {
            case NOMINAL -> 0;
            case UNKNOWN -> 1;
            case OFFLINE -> 2;
            case WARNING -> 3;
            case CRITICAL -> 4;
        };
    }

    record Rule(
            String unit,
            Double nominalMin,
            Double nominalMax,
            boolean minInclusive,
            boolean maxInclusive,
            Double criticalMin,
            Double criticalMax,
            Double physicalMin,
            Double physicalMax
    ) {
    }
}
