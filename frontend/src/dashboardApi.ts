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

export interface SystemDetail {
  id: string;
  name: string;
  description: string;
  status: SystemStatus;
  availability: "ONLINE" | "OFFLINE";
  updatedAt: string | null;
  indicators: DashboardIndicator[];
  metrics: {
    id: string;
    name: string;
    unit: string;
    value: number | null;
    status: IndicatorStatus;
    nominalRange: DashboardIndicator["nominalRange"];
    criticalLimits: {
      minExclusive: number | null;
      maxExclusive: number | null;
    };
    measuredAt: string | null;
    componentId: string | null;
  }[];
  components: {
    id: string;
    name: string;
    status: IndicatorStatus;
    value: number | null;
    unit: string | null;
    updatedAt: string | null;
    simulated: boolean;
  }[];
  recentEvents: {
    id: string;
    level: string;
    category: string;
    systemId: string | null;
    systemName: string | null;
    source: string | null;
    message: string;
    occurredAt: string;
  }[];
}

export interface MeasurementSeries {
  indicatorKey: string;
  sourceMetricIds: string[];
  name: string;
  unit: string;
  from: string;
  to: string;
  measurements: {
    value: number;
    measuredAt: string;
  }[];
}

export interface TechnicalEvent {
  id: string;
  level: "INFO" | "WARNING" | "CRITICAL";
  category: "SYSTEM" | "ALERT" | "MAINTENANCE" | "OPERATION" | "DATA";
  systemId: string | null;
  systemName: string | null;
  source: string | null;
  message: string;
  occurredAt: string;
}

export interface EventLogPage {
  content: TechnicalEvent[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Dashboard {
  stationStatus: SystemStatus;
  offlineSystems: string[];
  systemStatuses?: { systemId: string; status: SystemStatus }[];
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
const systemIds = new Set([
  "life-support",
  "thermal-control",
  "power-supply",
  "communications",
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

function isSystemState(
  value: unknown,
): value is NonNullable<Dashboard["systemStatuses"]>[number] {
  return (
    isRecord(value) &&
    typeof value.systemId === "string" &&
    systemIds.has(value.systemId) &&
    isOneOf(value.status, systemStatuses)
  );
}

function isCriticalLimits(value: unknown): value is SystemDetail["metrics"][number]["criticalLimits"] {
  return (
    isRecord(value) &&
    (typeof value.minExclusive === "number" || value.minExclusive === null) &&
    (typeof value.maxExclusive === "number" || value.maxExclusive === null)
  );
}

function isMetricDetail(value: unknown): value is SystemDetail["metrics"][number] {
  return (
    isRecord(value) &&
    typeof value.id === "string" &&
    typeof value.name === "string" &&
    typeof value.unit === "string" &&
    (typeof value.value === "number" && Number.isFinite(value.value) ||
      value.value === null) &&
    isOneOf(value.status, indicatorStatuses) &&
    isRange(value.nominalRange) &&
    isCriticalLimits(value.criticalLimits) &&
    (typeof value.measuredAt === "string" || value.measuredAt === null) &&
    (typeof value.componentId === "string" || value.componentId === null)
  );
}

function isComponent(value: unknown): value is SystemDetail["components"][number] {
  return (
    isRecord(value) &&
    typeof value.id === "string" &&
    typeof value.name === "string" &&
    isOneOf(value.status, indicatorStatuses) &&
    (typeof value.value === "number" && Number.isFinite(value.value) ||
      value.value === null) &&
    (typeof value.unit === "string" || value.unit === null) &&
    (typeof value.updatedAt === "string" || value.updatedAt === null) &&
    typeof value.simulated === "boolean"
  );
}

function isEvent(value: unknown): value is SystemDetail["recentEvents"][number] {
  return (
    isRecord(value) &&
    typeof value.id === "string" &&
    typeof value.level === "string" &&
    typeof value.category === "string" &&
    (typeof value.systemId === "string" || value.systemId === null) &&
    (typeof value.systemName === "string" || value.systemName === null) &&
    (typeof value.source === "string" || value.source === null) &&
    typeof value.message === "string" &&
    typeof value.occurredAt === "string"
  );
}

function isTechnicalEvent(value: unknown): value is TechnicalEvent {
  return (
    isRecord(value) &&
    typeof value.id === "string" &&
    isOneOf(value.level, ["INFO", "WARNING", "CRITICAL"]) &&
    isOneOf(value.category, [
      "SYSTEM",
      "ALERT",
      "MAINTENANCE",
      "OPERATION",
      "DATA",
    ]) &&
    (typeof value.systemId === "string" || value.systemId === null) &&
    (typeof value.systemName === "string" || value.systemName === null) &&
    (typeof value.source === "string" || value.source === null) &&
    typeof value.message === "string" &&
    typeof value.occurredAt === "string" &&
    Number.isFinite(Date.parse(value.occurredAt))
  );
}

function isEventLogPage(value: unknown): value is EventLogPage {
  return (
    isRecord(value) &&
    Array.isArray(value.content) &&
    value.content.length <= 20 &&
    value.content.every(isTechnicalEvent) &&
    typeof value.page === "number" &&
    Number.isInteger(value.page) &&
    value.page >= 0 &&
    typeof value.size === "number" &&
    value.size === 20 &&
    typeof value.totalElements === "number" &&
    Number.isInteger(value.totalElements) &&
    value.totalElements >= 0 &&
    typeof value.totalPages === "number" &&
    Number.isInteger(value.totalPages) &&
    value.totalPages === Math.ceil(value.totalElements / 20)
  );
}

function isSystemDetail(value: unknown): value is SystemDetail {
  return (
    isRecord(value) &&
    typeof value.id === "string" &&
    typeof value.name === "string" &&
    typeof value.description === "string" &&
    isOneOf(value.status, systemStatuses) &&
    isOneOf(value.availability, ["ONLINE", "OFFLINE"]) &&
    (typeof value.updatedAt === "string" || value.updatedAt === null) &&
    Array.isArray(value.indicators) &&
    value.indicators.every(isIndicator) &&
    Array.isArray(value.metrics) &&
    value.metrics.every(isMetricDetail) &&
    Array.isArray(value.components) &&
    value.components.every(isComponent) &&
    Array.isArray(value.recentEvents) &&
    value.recentEvents.every(isEvent)
  );
}

function isMeasurementSeries(
  value: unknown,
  indicatorKey: string,
): value is MeasurementSeries {
  return (
    isRecord(value) &&
    value.indicatorKey === indicatorKey &&
    Array.isArray(value.sourceMetricIds) &&
    value.sourceMetricIds.length > 0 &&
    value.sourceMetricIds.every((metricId) => typeof metricId === "string") &&
    typeof value.name === "string" &&
    typeof value.unit === "string" &&
    typeof value.from === "string" &&
    Number.isFinite(Date.parse(value.from)) &&
    typeof value.to === "string" &&
    Number.isFinite(Date.parse(value.to)) &&
    Array.isArray(value.measurements) &&
    value.measurements.every(
      (measurement) =>
        isRecord(measurement) &&
        typeof measurement.value === "number" &&
        Number.isFinite(measurement.value) &&
        typeof measurement.measuredAt === "string" &&
        Number.isFinite(Date.parse(measurement.measuredAt)),
    )
  );
}

function isDashboard(value: unknown): value is Dashboard {
  return (
    isRecord(value) &&
    isOneOf(value.stationStatus, systemStatuses) &&
    Array.isArray(value.offlineSystems) &&
    value.offlineSystems.every((system) => typeof system === "string") &&
    (value.systemStatuses === undefined ||
      (Array.isArray(value.systemStatuses) &&
        value.systemStatuses.length === systemIds.size &&
        value.systemStatuses.every(isSystemState) &&
        new Set(value.systemStatuses.map((system) => system.systemId)).size ===
          systemIds.size)) &&
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

export async function getEvents(
  page: number,
  signal: AbortSignal,
): Promise<EventLogPage> {
  const response = await fetch(`/api/v1/events?page=${page}`, { signal });

  if (!response.ok) {
    throw new Error(`Event log request failed with status ${response.status}`);
  }

  const payload: unknown = await response.json();
  if (!isEventLogPage(payload) || payload.page !== page) {
    throw new Error("Event log response is invalid");
  }

  return payload;
}

export async function getSystemDetail(
  systemId: string,
  signal: AbortSignal,
): Promise<SystemDetail> {
  const response = await fetch(
    `/api/v1/systems/${encodeURIComponent(systemId)}`,
    { signal },
  );

  if (!response.ok) {
    throw new Error(`System detail request failed with status ${response.status}`);
  }

  const payload: unknown = await response.json();
  if (!isSystemDetail(payload) || payload.id !== systemId) {
    throw new Error("System detail response is invalid");
  }

  return payload;
}

export async function getIndicatorMeasurements(
  systemId: string,
  indicatorKey: string,
  signal: AbortSignal,
): Promise<MeasurementSeries> {
  const response = await fetch(
    `/api/v1/systems/${encodeURIComponent(systemId)}/indicators/${encodeURIComponent(indicatorKey)}/measurements`,
    { signal },
  );

  if (!response.ok) {
    throw new Error(
      `Measurement history request failed with status ${response.status}`,
    );
  }

  const payload: unknown = await response.json();
  if (!isMeasurementSeries(payload, indicatorKey)) {
    throw new Error("Measurement history response is invalid");
  }

  return payload;
}
