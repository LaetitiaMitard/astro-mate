import { useEffect, useState } from "react";
import "./App.css";
import { getDashboard, type Dashboard } from "./dashboardApi";

const statusLabels: Record<string, string> = {
  NOMINAL: "NOMINAL",
  WARNING: "AVERTISSEMENT",
  CRITICAL: "CRITIQUE",
  OFFLINE: "HORS LIGNE",
  UNKNOWN: "INCONNU",
};

function formatValue(value: number | null) {
  if (value === null) {
    return "Inconnue";
  }

  return new Intl.NumberFormat("fr-FR", { maximumFractionDigits: 1 }).format(
    value,
  );
}

function formatRange(
  range: Dashboard["indicators"][number]["nominalRange"],
  unit: string,
) {
  const min =
    range.min === null
      ? null
      : `${range.minInclusive ? "" : ">"}${formatValue(range.min)}`;
  const max =
    range.max === null
      ? null
      : `${range.maxInclusive ? "" : "<"}${formatValue(range.max)}`;

  if (min === null) {
    return `Jusqu’à ${max} ${unit}`;
  }
  if (max === null) {
    return `À partir de ${min} ${unit}`;
  }

  return `${min}–${max} ${unit}`;
}

export default function App() {
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [loadState, setLoadState] = useState<"loading" | "loaded" | "error">(
    "loading",
  );

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
  }, []);

  return (
    <main className="app">
      <header className="page-header">
        <div>
          <p className="eyebrow">CENTRE DE CONTRÔLE · STATION UNIQUE</p>
          <h1>AstroMate</h1>
        </div>
        <p className="notice">Simulation — données fictives, non opérationnelles</p>
      </header>

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
      )}
    </main>
  );
}
