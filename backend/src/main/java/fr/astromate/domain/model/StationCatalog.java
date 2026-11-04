package fr.astromate.domain.model;

import java.util.List;
import java.util.Map;

public final class StationCatalog {
    private StationCatalog() {
    }

    public static final List<SystemMetadata> SYSTEMS = List.of(
            new SystemMetadata(
                    "life-support",
                    "Support de vie",
                    "Atmosphère et qualité de l’air",
                    List.of("oxygen", "carbonDioxide"),
                    List.of()
            ),
            new SystemMetadata(
                    "thermal-control",
                    "Contrôle thermique",
                    "Température et circulation thermique",
                    List.of(
                            "radiatorATemperature",
                            "radiatorBTemperature",
                            "fluidFlow",
                            "pressure",
                            "thermalConsumption"
                    ),
                    List.of(
                            new ComponentMetadata(
                                    "radiator-a",
                                    "Radiateur A",
                                    List.of("radiatorATemperature")
                            ),
                            new ComponentMetadata(
                                    "radiator-b",
                                    "Radiateur B",
                                    List.of("radiatorBTemperature")
                            ),
                            new ComponentMetadata(
                                    "main-pump",
                                    "Pompe principale",
                                    List.of()
                            ),
                            new ComponentMetadata(
                                    "heat-exchanger",
                                    "Échangeur thermique",
                                    List.of()
                            )
                    )
            ),
            new SystemMetadata(
                    "power-supply",
                    "Alimentation électrique",
                    "Énergie disponible à bord",
                    List.of("availableEnergy"),
                    List.of()
            ),
            new SystemMetadata(
                    "communications",
                    "Communications",
                    "Système de communication de la station",
                    List.of(),
                    List.of()
            )
    );
    public static final Map<String, MetricMetadata> METRIC_METADATA = Map.ofEntries(
            Map.entry(
                    "oxygen",
                    new MetricMetadata("oxygen-level", "O₂", null)
            ),
            Map.entry(
                    "carbonDioxide",
                    new MetricMetadata("carbon-dioxide-level", "CO₂", null)
            ),
            Map.entry(
                    "radiatorATemperature",
                    new MetricMetadata(
                            "radiator-a-temperature",
                            "Température Radiateur A",
                            "radiator-a"
                    )
            ),
            Map.entry(
                    "radiatorBTemperature",
                    new MetricMetadata(
                            "radiator-b-temperature",
                            "Température Radiateur B",
                            "radiator-b"
                    )
            ),
            Map.entry(
                    "fluidFlow",
                    new MetricMetadata("fluid-flow", "Débit de fluide", null)
            ),
            Map.entry(
                    "pressure",
                    new MetricMetadata("pressure", "Pression", null)
            ),
            Map.entry(
                    "thermalConsumption",
                    new MetricMetadata(
                            "thermal-consumption",
                            "Consommation thermique",
                            null
                    )
            ),
            Map.entry(
                    "availableEnergy",
                    new MetricMetadata(
                            "available-energy",
                            "Énergie disponible",
                            null
                    )
            )
    );

    public record SystemMetadata(
            String id,
            String name,
            String description,
            List<String> metricKeys,
            List<ComponentMetadata> components
    ) {
    }

    public record ComponentMetadata(
            String id,
            String name,
            List<String> metricKeys
    ) {
    }

    public static String systemName(String systemId) {
        return SYSTEMS.stream()
                .filter(system -> system.id().equals(systemId))
                .map(SystemMetadata::name)
                .findFirst()
                .orElse(null);
    }

    public record MetricMetadata(String id, String name, String componentId) {
    }
}
