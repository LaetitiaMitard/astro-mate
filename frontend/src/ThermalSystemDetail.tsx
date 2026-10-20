import { useState, type MouseEvent } from "react";
import type { SystemDetail } from "./dashboardApi";
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
                    {formatMeasurementDate(component.updatedAt)}
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
            <p>
              L’API fournit actuellement la dernière mesure valide de chaque
              métrique, sans historique de mesures.
            </p>
          </div>
        )}
      </section>
    </section>
  );
}
