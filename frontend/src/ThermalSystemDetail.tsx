import { useEffect, useState, type MouseEvent } from "react";
import {
  getIndicatorMeasurements,
  type MeasurementSeries,
  type SystemDetail,
} from "./dashboardApi";
import {
  formatMeasurementDate,
  formatRange,
  formatValue,
} from "./dashboardFormat";

const statusLabels: Record<SystemDetail["status"], string> = {
  NOMINAL: "NOMINAL",
  WARNING: "AVERTISSEMENT",
  CRITICAL: "CRITIQUE",
  OFFLINE: "HORS LIGNE",
  UNKNOWN: "INCONNU",
};

type ThermalTab = "overview" | "components" | "history";

function chartPoints(measurements: MeasurementSeries["measurements"]) {
  const values = measurements.map((measurement) => measurement.value);
  const minimum = Math.min(...values);
  const maximum = Math.max(...values);
  const valueRange = maximum - minimum;
  const firstTime = Date.parse(measurements[0].measuredAt);
  const timeRange =
    Date.parse(measurements[measurements.length - 1].measuredAt) - firstTime;

  return measurements
    .map((measurement, index) => {
      const x = timeRange === 0
        ? 360
        : 24 + ((Date.parse(measurement.measuredAt) - firstTime) / timeRange) * 672;
      const y = valueRange === 0
        ? 111
        : 198 - ((measurement.value - minimum) / valueRange) * 174;
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    })
    .join(" ");
}

interface ThermalSystemDetailProps {
  detail: SystemDetail;
  onNavigate: (event: MouseEvent<HTMLAnchorElement>) => void;
}

function criticalThresholds(
  limits: SystemDetail["metrics"][number]["criticalLimits"],
  unit: string,
) {
  const thresholds = [
    limits.minExclusive === null
      ? null
      : `<${formatValue(limits.minExclusive)} ${unit}`,
    limits.maxExclusive === null
      ? null
      : `>${formatValue(limits.maxExclusive)} ${unit}`,
  ].filter((value) => value !== null);

  return thresholds.length === 0
    ? "Aucun seuil critique de référence"
    : thresholds.join(" ou ");
}

export default function ThermalSystemDetail({
  detail,
  onNavigate,
}: ThermalSystemDetailProps) {
  const [activeTab, setActiveTab] = useState<ThermalTab>("overview");
  const [history, setHistory] = useState<MeasurementSeries | null>(null);
  const [historyLoadState, setHistoryLoadState] = useState<
    "loading" | "loaded" | "error"
  >("loading");

  useEffect(() => {
    const controller = new AbortController();
    getIndicatorMeasurements(
      "thermal-control",
      "temperature",
      controller.signal,
    )
      .then((result) => {
        setHistory(result);
        setHistoryLoadState("loaded");
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setHistoryLoadState("error");
        }
      });

    return () => controller.abort();
  }, []);

  return (
    <section className="system-detail thermal-detail" aria-labelledby="system-detail-heading">
      <a className="back-link" href="/systems" onClick={onNavigate}>
        Retour aux systèmes
      </a>
      <div className="section-heading">
        <div>
          <p className="eyebrow">SYSTÈME DE LA STATION</p>
          <h2 id="system-detail-heading">{detail.name}</h2>
          <p className="system-description">{detail.description}</p>
        </div>
        <span className={`system-status status status-${detail.status.toLowerCase()}`}>
          {statusLabels[detail.status]}
        </span>
      </div>
      <div className="system-measurement">
        <span>Dernière mesure valide</span>
        <strong>{formatMeasurementDate(detail.updatedAt)}</strong>
      </div>

      <div className="thermal-tabs" role="tablist" aria-label="Contrôle thermique">
        <button
          id="thermal-tab-overview"
          type="button"
          role="tab"
          aria-selected={activeTab === "overview"}
          aria-controls="thermal-tab-panel"
          onClick={() => setActiveTab("overview")}
        >
          Vue d’ensemble
        </button>
        <button
          id="thermal-tab-components"
          type="button"
          role="tab"
          aria-selected={activeTab === "components"}
          aria-controls="thermal-tab-panel"
          onClick={() => setActiveTab("components")}
        >
          Composants
        </button>
        <button
          id="thermal-tab-history"
          type="button"
          role="tab"
          aria-selected={activeTab === "history"}
          aria-controls="thermal-tab-panel"
          onClick={() => setActiveTab("history")}
        >
          Historique
        </button>
        <button type="button" role="tab" disabled aria-disabled="true">
          Maintenance
        </button>
      </div>

      <section
        id="thermal-tab-panel"
        className="thermal-tab-panel"
        role="tabpanel"
        aria-labelledby={`thermal-tab-${activeTab}`}
        tabIndex={0}
      >
        {activeTab === "overview" && (
          <>
            {detail.indicators
              .filter((indicator) => indicator.key === "temperature")
              .map((indicator) => {
                const sourceMetric = detail.metrics.find((metric) =>
                  indicator.sourceMetricIds.includes(metric.id),
                );
                return (
                  <article
                    className="thermal-metric-card thermal-aggregate-card"
                    key={indicator.key}
                  >
                    <div className="indicator-card-heading">
                      <h4>Température moyenne du système</h4>
                      <span
                        className={`metric-status status-${indicator.status.toLowerCase()}`}
                      >
                        {statusLabels[indicator.status]}
                      </span>
                    </div>
                    <p className="thermal-metric-value">
                      <strong>{formatValue(indicator.value)}</strong>
                      <span>{indicator.unit}</span>
                    </p>
                    <dl className="thermal-reference-list">
                      <div>
                        <dt>Plage nominale</dt>
                        <dd>{formatRange(indicator.nominalRange, indicator.unit)}</dd>
                      </div>
                      <div>
                        <dt>Seuil critique</dt>
                        <dd>
                          {sourceMetric
                            ? criticalThresholds(
                                sourceMetric.criticalLimits,
                                indicator.unit,
                              )
                            : "Aucun seuil critique de référence"}
                        </dd>
                      </div>
                    </dl>
                    <p className="thermal-aggregate-note">
                      État déterminé par la mesure de radiateur la plus sévère.
                    </p>
                  </article>
                );
              })}
            <div className="section-heading">
              <div>
                <p className="eyebrow">VALEURS ET RÉFÉRENCES</p>
                <h3>Métriques thermiques</h3>
              </div>
              <span className="section-count">
                {detail.metrics.length.toString().padStart(2, "0")} MÉTRIQUES
              </span>
            </div>
            <div className="thermal-metric-grid">
              {detail.metrics.map((metric) => (
                <article className="thermal-metric-card" key={metric.id}>
                  <div className="indicator-card-heading">
                    <h4>{metric.name}</h4>
                    <span
                      className={`metric-status status-${metric.status.toLowerCase()}`}
                    >
                      {statusLabels[metric.status]}
                    </span>
                  </div>
                  <p className="thermal-metric-value">
                    <strong>{formatValue(metric.value)}</strong>
                    <span>{metric.unit}</span>
                  </p>
                  <dl className="thermal-reference-list">
                    <div>
                      <dt>Plage nominale</dt>
                      <dd>{formatRange(metric.nominalRange, metric.unit)}</dd>
                    </div>
                    <div>
                      <dt>Seuil critique</dt>
                      <dd>{criticalThresholds(metric.criticalLimits, metric.unit)}</dd>
                    </div>
                    <div>
                      <dt>Dernière mise à jour</dt>
                      <dd>{formatMeasurementDate(metric.measuredAt)}</dd>
                    </div>
                  </dl>
                </article>
              ))}
            </div>
            <section
              className="thermal-events"
              aria-labelledby="thermal-events-heading"
            >
              <h3 id="thermal-events-heading">Événements récents</h3>
              {detail.recentEvents.length === 0 ? (
                <p>Aucun événement récent</p>
              ) : (
                <ul>
                  {detail.recentEvents.map((event) => (
                    <li key={event.id}>
                      <span>{formatMeasurementDate(event.occurredAt)}</span>
                      <strong>{event.level}</strong>
                      <span>{event.message}</span>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </>
        )}

        {activeTab === "components" && (
          <>
            <div className="section-heading">
              <div>
                <p className="eyebrow">ÉQUIPEMENTS</p>
                <h3>Composants thermiques</h3>
              </div>
              <span className="section-count">
                {detail.components.length.toString().padStart(2, "0")} COMPOSANTS
              </span>
            </div>
            <ul className="thermal-component-list">
              {detail.components.map((component) => (
                <li className="thermal-component" key={component.id}>
                  <div>
                    <h4>{component.name}</h4>
                    {component.simulated && (
                      <span className="simulated-component">État simulé</span>
                    )}
                  </div>
                  <strong className="thermal-component-value">
                    {component.value === null || component.unit === null
                      ? "Aucune mesure associée"
                      : `${formatValue(component.value)} ${component.unit}`}
                  </strong>
                  <span
                    className={`metric-status status-${component.status.toLowerCase()}`}
                  >
                    {statusLabels[component.status]}
                  </span>
                  <span className="thermal-component-updated">
                    {component.simulated
                      ? `État simulé depuis le ${formatMeasurementDate(component.updatedAt)}`
                      : `Dernière mesure : ${formatMeasurementDate(component.updatedAt)}`}
                  </span>
                </li>
              ))}
            </ul>
          </>
        )}

        {activeTab === "history" && (
          <div className="thermal-history">
            <p className="eyebrow">MESURES HORODATÉES</p>
            <h3>Historique thermique</h3>
            {historyLoadState === "loading" && (
              <p role="status">Chargement de l’historique thermique…</p>
            )}
            {historyLoadState === "error" && (
              <section className="error-panel" role="alert">
                <p className="eyebrow">LECTURE IMPOSSIBLE</p>
                <p>L’historique thermique est indisponible.</p>
              </section>
            )}
            {historyLoadState === "loaded" && history && (
              history.measurements.length === 0 ? (
                <p>Aucune mesure disponible sur les six dernières heures.</p>
              ) : (
                <>
                  <p>
                    Température moyenne calculée à partir des mesures horodatées
                    de Radiateur A et Radiateur B.
                  </p>
                  <figure className="thermal-history-chart">
                    <svg
                      viewBox="0 0 720 220"
                      role="img"
                      aria-label={`Courbe de ${history.name.toLowerCase()} en ${history.unit} sur les six dernières heures`}
                    >
                      <line x1="24" y1="198" x2="696" y2="198" />
                      <line x1="24" y1="24" x2="24" y2="198" />
                      <polyline points={chartPoints(history.measurements)} />
                      {new Set(
                        history.measurements.map((measurement) => measurement.measuredAt),
                      ).size === 1 && (
                        <circle
                          cx="360"
                          cy="111"
                          r="4"
                          aria-hidden="true"
                        />
                      )}
                    </svg>
                    <figcaption>
                      Valeurs en {history.unit}; les horodatages et valeurs
                      exacts sont disponibles dans le tableau.
                    </figcaption>
                  </figure>
                  <div className="thermal-history-table-scroll">
                    <table className="thermal-history-table">
                      <caption>
                        Mesures thermiques des six dernières heures
                      </caption>
                      <thead>
                        <tr>
                          <th scope="col">Horodatage UTC</th>
                          <th scope="col">{history.name} ({history.unit})</th>
                        </tr>
                      </thead>
                      <tbody>
                        {history.measurements.map((measurement) => (
                          <tr key={measurement.measuredAt}>
                            <td>
                              <time dateTime={measurement.measuredAt}>
                                {measurement.measuredAt}
                              </time>
                            </td>
                            <td>
                              {formatValue(measurement.value)} {history.unit}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </>
              )
            )}
          </div>
        )}
      </section>
    </section>
  );
}
