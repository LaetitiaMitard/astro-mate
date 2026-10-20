export type IndicatorStatus = "NOMINAL" | "WARNING" | "CRITICAL" | "UNKNOWN";
export type SystemStatus = IndicatorStatus | "OFFLINE";

export interface DashboardIndicator {
  key: string;
  systemId: string;
  sourceMetricIds: string[];
  name: string;
  value: number | null;
  unit: string;
  status: IndicatorStatus;
  nominalRange: {
    min: number | null;
    max: number | null;
    minInclusive: boolean;
    maxInclusive: boolean;
  };
  measuredAt: string | null;
}

export interface Dashboard {
  stationStatus: SystemStatus;
  offlineSystems: string[];
  simulationStatus: "ACTIVE" | "PAUSED" | "UNAVAILABLE";
  lastMeasurementAt: string | null;
  indicators: DashboardIndicator[];
  recentOpenAlerts: {
    id: string;
    level: string;
    systemId: string;
    systemName: string;
    metricId: string | null;
    message: string;
    status: string;
    detectedAt: string;
    resolvedAt: string | null;
  }[];
}

const indicatorStatuses = ["NOMINAL", "WARNING", "CRITICAL", "UNKNOWN"];
const systemStatuses = [...indicatorStatuses, "OFFLINE"];
const indicatorKeys = new Set([
  "oxygen",
  "carbonDioxide",
  "temperature",
  "availableEnergy",
]);

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isOneOf(value: unknown, values: string[]): value is string {
  return typeof value === "string" && values.includes(value);
}

function isRange(value: unknown): value is DashboardIndicator["nominalRange"] {
  return (
    isRecord(value) &&
    (typeof value.min === "number" || value.min === null) &&
    (typeof value.max === "number" || value.max === null) &&
    typeof value.minInclusive === "boolean" &&
    typeof value.maxInclusive === "boolean"
  );
}

function isIndicator(value: unknown): value is DashboardIndicator {
  return (
    isRecord(value) &&
    typeof value.key === "string" &&
    typeof value.systemId === "string" &&
    Array.isArray(value.sourceMetricIds) &&
    value.sourceMetricIds.every((metricId) => typeof metricId === "string") &&
    typeof value.name === "string" &&
    (typeof value.value === "number" && Number.isFinite(value.value) ||
      value.value === null) &&
    typeof value.unit === "string" &&
    isOneOf(value.status, indicatorStatuses) &&
    isRange(value.nominalRange) &&
    (typeof value.measuredAt === "string" || value.measuredAt === null)
  );
}

function isAlert(value: unknown): value is Dashboard["recentOpenAlerts"][number] {
  return (
    isRecord(value) &&
    typeof value.id === "string" &&
    typeof value.level === "string" &&
    typeof value.systemId === "string" &&
    typeof value.systemName === "string" &&
    (typeof value.metricId === "string" || value.metricId === null) &&
    typeof value.message === "string" &&
    typeof value.status === "string" &&
    typeof value.detectedAt === "string" &&
    (typeof value.resolvedAt === "string" || value.resolvedAt === null)
  );
}

function isDashboard(value: unknown): value is Dashboard {
  return (
    isRecord(value) &&
    isOneOf(value.stationStatus, systemStatuses) &&
    Array.isArray(value.offlineSystems) &&
    value.offlineSystems.every((system) => typeof system === "string") &&
    isOneOf(value.simulationStatus, ["ACTIVE", "PAUSED", "UNAVAILABLE"]) &&
    (typeof value.lastMeasurementAt === "string" ||
      value.lastMeasurementAt === null) &&
    Array.isArray(value.indicators) &&
    value.indicators.length === 4 &&
    value.indicators.every(isIndicator) &&
    value.indicators.every((indicator) => indicatorKeys.has(indicator.key)) &&
    new Set(value.indicators.map((indicator) => indicator.key)).size === 4 &&
    Array.isArray(value.recentOpenAlerts) &&
    value.recentOpenAlerts.every(isAlert)
  );
}

export async function getDashboard(signal: AbortSignal): Promise<Dashboard> {
  const response = await fetch("/api/v1/dashboard", { signal });

  if (!response.ok) {
    throw new Error(`Dashboard request failed with status ${response.status}`);
  }

  const payload: unknown = await response.json();
  if (!isDashboard(payload)) {
    throw new Error("Dashboard response is invalid");
  }

  return payload;
}
