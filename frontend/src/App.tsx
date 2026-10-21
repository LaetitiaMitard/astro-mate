import { useEffect, useState, type MouseEvent } from "react";
import "./App.css";
import {
  getDashboard,
  getSystemDetail,
  type Dashboard,
  type SystemDetail,
} from "./dashboardApi";
import {
  formatMeasurementDate,
  formatRange,
  formatValue,
} from "./dashboardFormat";
import ThermalSystemDetail from "./ThermalSystemDetail";

const statusLabels: Record<Dashboard["stationStatus"], string> = {
  NOMINAL: "NOMINAL",
  WARNING: "AVERTISSEMENT",
  CRITICAL: "CRITIQUE",
  OFFLINE: "HORS LIGNE",
  UNKNOWN: "INCONNU",
};

const systems = [
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

const systemStatusPriority = {
  NOMINAL: 0,
  UNKNOWN: 1,
  OFFLINE: 2,
  WARNING: 3,
  CRITICAL: 4,
} satisfies Record<Dashboard["stationStatus"], number>;

function getSystemStatus(
  systemId: string,
  dashboard: Dashboard,
): Dashboard["stationStatus"] {
  const reportedStatus = dashboard.systemStatuses?.find(
    (system) => system.systemId === systemId,
  )?.status;
  if (reportedStatus !== undefined) {
    return reportedStatus;
  }

  const statuses: Dashboard["stationStatus"][] = dashboard.indicators
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

function getLastValidMeasurementAt(systemId: string, dashboard: Dashboard) {
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

export default function App() {
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [loadState, setLoadState] = useState<"loading" | "loaded" | "error">(
    "loading",
  );
  const [pathname, setPathname] = useState(window.location.pathname);
  const [refreshVersion, setRefreshVersion] = useState(0);
  const [systemDetail, setSystemDetail] = useState<SystemDetail | null>(null);
  const [detailLoadState, setDetailLoadState] = useState<
    "loading" | "loaded" | "error"
  >("loading");
  const selectedSystemId = pathname.match(/^\/systems\/([^/]+)\/?$/)?.[1];
  const selectedSystem = systems.find(
    (system) => system.id === selectedSystemId,
  );

  useEffect(() => {
    if (typeof EventSource === "undefined") {
      return;
    }

    const eventSource = new EventSource("/api/v1/telemetry/changes");
    let refreshTimeout: number | null = null;
    eventSource.onmessage = () => {
      if (refreshTimeout !== null) {
        window.clearTimeout(refreshTimeout);
      }
      refreshTimeout = window.setTimeout(
        () => setRefreshVersion((version) => version + 1),
        50,
      );
    };

    return () => {
      if (refreshTimeout !== null) {
        window.clearTimeout(refreshTimeout);
      }
      eventSource.close();
    };
  }, []);

  useEffect(() => {
    const controller = new AbortController();

    getDashboard(controller.signal)
      .then((result) => {
        setDashboard(result);
        setLoadState("loaded");
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setLoadState("error");
        }
      });

    return () => controller.abort();
  }, [refreshVersion]);

  useEffect(() => {
    if (selectedSystemId !== "thermal-control") {
      return;
    }

    const controller = new AbortController();
    setDetailLoadState((state) =>
      state === "loaded" ? state : "loading",
    );

    getSystemDetail(selectedSystemId, controller.signal)
      .then((result) => {
        setSystemDetail(result);
        setDetailLoadState("loaded");
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setDetailLoadState("error");
        }
      });

    return () => controller.abort();
  }, [selectedSystemId, refreshVersion]);

  useEffect(() => {
    const onPopState = () => setPathname(window.location.pathname);
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function navigate(event: MouseEvent<HTMLAnchorElement>) {
    event.preventDefault();
    const nextPath = event.currentTarget.pathname;
    window.history.pushState(null, "", nextPath);
    setPathname(nextPath);
  }

  const isSystemListRoute = pathname === "/systems";
  const isSystemRoute = isSystemListRoute || selectedSystemId !== undefined;
  const selectedSystemIndicators =
    selectedSystem && dashboard
      ? dashboard.indicators.filter(
          (indicator) => indicator.systemId === selectedSystem.id,
        )
      : [];
  const selectedSystemStatus: Dashboard["stationStatus"] =
    selectedSystem && dashboard
      ? getSystemStatus(selectedSystem.id, dashboard)
      : "UNKNOWN";
  const selectedSystemMeasurementAt =
    selectedSystem && dashboard
      ? getLastValidMeasurementAt(selectedSystem.id, dashboard)
      : null;

  return (
    <main className="app">
      <header className="page-header">
        <div>
          <p className="eyebrow">CENTRE DE CONTRÔLE · STATION UNIQUE</p>
          <h1>AstroMate</h1>
        </div>
        <p className="notice">Simulation — données fictives, non opérationnelles</p>
      </header>
      <nav className="main-navigation" aria-label="Navigation principale">
        <a href="/" onClick={navigate}>
          Tableau de bord
        </a>
        <a href="/systems" onClick={navigate}>
          Systèmes
        </a>
      </nav>

      {loadState === "loading" && (
        <p className="loading" role="status">
          Lecture de la simulation…
        </p>
      )}

      {loadState === "error" && (
        <section className="error-panel" role="alert">
          <p className="eyebrow">LECTURE IMPOSSIBLE</p>
          <h2>Tableau de bord indisponible</h2>
          <p>
            Impossible de lire les données de la simulation. Aucune valeur
            nominale n’est affichée.
          </p>
        </section>
      )}

      {loadState === "loaded" && dashboard && (
        isSystemRoute ? (
          selectedSystem ? (
            selectedSystem.id === "thermal-control" ? (
              detailLoadState === "loading" ? (
                <p className="loading" role="status">
                  Lecture du contrôle thermique…
                </p>
              ) : detailLoadState === "error" || systemDetail === null ? (
                <section className="error-panel" role="alert">
                  <h2>Détail du système indisponible</h2>
                  <p>
                    Impossible de lire les métriques et composants thermiques.
                  </p>
                </section>
              ) : (
                <ThermalSystemDetail
                  detail={systemDetail}
                  onNavigate={navigate}
                  refreshVersion={refreshVersion}
                />
              )
            ) : (
            <section
              className="system-detail"
              aria-labelledby="system-detail-heading"
            >
              <a className="back-link" href="/systems" onClick={navigate}>
                Retour aux systèmes
              </a>
              <div className="section-heading">
                <div>
                  <p className="eyebrow">SYSTÈME DE LA STATION</p>
                  <h2 id="system-detail-heading">{selectedSystem.name}</h2>
                  <p className="system-description">
                    {selectedSystem.description}
                  </p>
                </div>
                <span
                  className={`system-status status status-${selectedSystemStatus.toLowerCase()}`}
                >
                  {statusLabels[selectedSystemStatus]}
                </span>
              </div>
              <div className="system-measurement">
                <span>Dernière mesure valide</span>
                <strong>
                  {formatMeasurementDate(selectedSystemMeasurementAt)}
                </strong>
              </div>
              <section className="system-metrics" aria-label="Mesures du système">
                <h3>Mesures suivies</h3>
                {selectedSystemIndicators.length === 0 ? (
                  <p>Aucune mesure associée à ce système.</p>
                ) : (
                  <ul className="system-metric-list">
                    {selectedSystemIndicators.map((indicator) => (
                      <li key={indicator.key}>
                        <span>{indicator.name}</span>
                        <strong>
                          {formatValue(indicator.value)} {indicator.unit}
                        </strong>
                        <span
                          className={`metric-status status-${indicator.status.toLowerCase()}`}
                        >
                          {statusLabels[indicator.status]}
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </section>
            </section>
            )
          ) : (
            isSystemListRoute ? (
              <section
                className="systems-section"
                aria-label="Systèmes de la station"
              >
                <div className="section-heading">
                  <div>
                    <p className="eyebrow">SURVEILLANCE DE LA STATION</p>
                    <h2>Systèmes</h2>
                  </div>
                  <span className="section-count">04 SYSTÈMES</span>
                </div>
                <ul className="system-grid">
                  {systems.map((system) => {
                    const status = getSystemStatus(system.id, dashboard);
                    return (
                      <li key={system.id}>
                        <a
                          className="system-card"
                          href={`/systems/${system.id}`}
                          onClick={navigate}
                        >
                          <span className="system-card-heading">
                            <strong>{system.name}</strong>
                            <span
                              className={`metric-status status-${status.toLowerCase()}`}
                            >
                              {statusLabels[status]}
                            </span>
                          </span>
                          <span className="system-card-description">
                            {system.description}
                          </span>
                          <span className="system-card-measurement">
                            Dernière mesure valide :{" "}
                            {formatMeasurementDate(
                              getLastValidMeasurementAt(system.id, dashboard),
                            )}
                          </span>
                        </a>
                      </li>
                    );
                  })}
                </ul>
              </section>
            ) : (
              <section className="error-panel" role="alert">
                <h2>Système introuvable</h2>
                <a href="/systems" onClick={navigate}>
                  Retour aux systèmes
                </a>
              </section>
            )
          )
        ) : (
          <>
          <section className="station-summary" aria-label="État de la station">
            <div>
              <p className="eyebrow">ÉTAT GLOBAL</p>
              <strong
                className={`status status-${dashboard.stationStatus.toLowerCase()}`}
              >
                {statusLabels[dashboard.stationStatus]}
              </strong>
            </div>
            {dashboard.offlineSystems.length > 0 && (
              <p className="offline-system-note">
                Systèmes hors ligne :{" "}
                {dashboard.offlineSystems
                  .map(
                    (systemId) =>
                      systems.find((system) => system.id === systemId)?.name ??
                      systemId,
                  )
                  .join(", ")}
              </p>
            )}
            <div className="simulation-status">
              <span
                className={`status-dot simulation-dot-${dashboard.simulationStatus.toLowerCase()}`}
                aria-hidden="true"
              />
              {dashboard.simulationStatus === "ACTIVE"
                ? "Simulation active"
                : dashboard.simulationStatus === "PAUSED"
                  ? "Simulation en pause"
                  : "Simulation indisponible"}
            </div>
          </section>

          <section className="indicators-section">
            <div className="section-heading">
              <div>
                <p className="eyebrow">SURVEILLANCE DE LA STATION</p>
                <h2>Indicateurs principaux</h2>
              </div>
              <span className="section-count">04 MESURES</span>
            </div>
            <div className="indicator-grid">
              {dashboard.indicators.map((indicator) => (
                <article className="indicator-card" key={indicator.key}>
                  <div className="indicator-card-heading">
                    <h3>{indicator.name}</h3>
                    <span
                      className={`metric-status status-${indicator.status.toLowerCase()}`}
                    >
                      {statusLabels[indicator.status]}
                    </span>
                  </div>
                  <p className="indicator-value">
                    <strong>{formatValue(indicator.value)}</strong>
                    <span>{indicator.unit}</span>
                  </p>
                  <div className="indicator-range">
                    <span>Plage nominale</span>
                    <strong>
                      {formatRange(indicator.nominalRange, indicator.unit)}
                    </strong>
                  </div>
                </article>
              ))}
            </div>
          </section>

          <section className="alerts-panel" aria-labelledby="alerts-heading">
            <div className="alerts-icon" aria-hidden="true">
              !
            </div>
            <div>
              <h2 id="alerts-heading">Alertes ouvertes</h2>
              {dashboard.recentOpenAlerts.length === 0 ? (
                <p>Aucune alerte ouverte</p>
              ) : (
                <ul>
                  {dashboard.recentOpenAlerts.map((alert) => (
                    <li key={alert.id}>{alert.message}</li>
                  ))}
                </ul>
              )}
            </div>
            <span className="alert-count">
              {dashboard.recentOpenAlerts.length.toString().padStart(2, "0")}
            </span>
          </section>
          </>
        )
      )}
    </main>
  );
}
