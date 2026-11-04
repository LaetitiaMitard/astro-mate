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
  items: TechnicalEvent[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
}

export interface EventLogFilters {
  from?: string;
  to?: string;
  level?: TechnicalEvent["level"];
  category?: TechnicalEvent["category"];
  query?: string;
  sort: "occurredAt,asc" | "occurredAt,desc";
}

export type AlertStatus = "OPEN" | "RESOLVED";
export type AlertLevel = "INFO" | "WARNING" | "CRITICAL";

export interface AlertEpisode {
  id: string;
  level: AlertLevel;
  systemId: string;
  systemName: string;
  metricId: string | null;
  message: string;
  status: AlertStatus;
  detectedAt: string;
  resolvedAt: string | null;
}

export interface AlertPage {
  items: AlertEpisode[];
  page: number;
  pageSize: number;
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
  recentOpenAlerts: AlertEpisode[];
}
