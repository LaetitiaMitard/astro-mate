import type { Dashboard, SystemStatus } from "../api/models";

export const systems = [
  {
    id: "life-support",
    name: "Support de vie",
    description: "Atmosphère et qualité de l’air",
  },
  {
    id: "thermal-control",
    name: "Contrôle thermique",
    description: "Température et circulation thermique",
  },
  {
    id: "power-supply",
    name: "Alimentation électrique",
    description: "Énergie disponible à bord",
  },
  {
    id: "communications",
    name: "Communications",
    description: "Système de communication de la station",
  },
] as const;

export const statusLabels: Record<SystemStatus, string> = {
  NOMINAL: "NOMINAL",
  WARNING: "AVERTISSEMENT",
  CRITICAL: "CRITIQUE",
  OFFLINE: "HORS LIGNE",
  UNKNOWN: "INCONNU",
};

const systemStatusPriority: Record<SystemStatus, number> = {
  NOMINAL: 0,
  UNKNOWN: 1,
  OFFLINE: 2,
  WARNING: 3,
  CRITICAL: 4,
};

export function getSystemStatus(systemId: string, dashboard: Dashboard) {
  const reportedStatus = dashboard.systemStatuses?.find(
    (system) => system.systemId === systemId,
  )?.status;
  if (reportedStatus !== undefined) {
    return reportedStatus;
  }

  const statuses: SystemStatus[] = dashboard.indicators
    .filter((indicator) => indicator.systemId === systemId)
    .map((indicator) => indicator.status);

  if (dashboard.offlineSystems.includes(systemId)) {
    statuses.push("OFFLINE");
  }

  if (statuses.length === 0) {
    return "NOMINAL";
  }

  return statuses.reduce((mostSevere, status) =>
    systemStatusPriority[status] > systemStatusPriority[mostSevere]
      ? status
      : mostSevere,
  );
}

export function getLastValidMeasurementAt(systemId: string, dashboard: Dashboard) {
  return dashboard.indicators
    .filter(
      (indicator) =>
        indicator.systemId === systemId &&
        indicator.value !== null &&
        indicator.measuredAt !== null &&
        Number.isFinite(Date.parse(indicator.measuredAt)),
    )
    .reduce<string | null>((latest, indicator) => {
      const measuredAt = indicator.measuredAt;
      if (
        measuredAt !== null &&
        (latest === null || Date.parse(measuredAt) > Date.parse(latest))
      ) {
        return measuredAt;
      }

      return latest;
    }, null);
}
