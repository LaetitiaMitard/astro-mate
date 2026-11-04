import type { MouseEvent } from "react";
import type { Dashboard, SystemDetail } from "../../shared/api/models";
import {
  getLastValidMeasurementAt,
  getSystemStatus,
  statusLabels,
  systems,
} from "../../shared/domain/systems";
import { formatMeasurementDate, formatValue } from "../../shared/lib/dashboardFormat";
import ThermalSystemDetail from "./ThermalSystemDetail";

interface SystemsPageProps {
  pathname: string;
  dashboard: Dashboard;
  systemDetail: SystemDetail | null;
  detailLoadState: "loading" | "loaded" | "error";
  refreshVersion: number;
  onNavigate: (event: MouseEvent<HTMLAnchorElement>) => void;
}

export default function SystemsPage({
  pathname,
  dashboard,
  systemDetail,
  detailLoadState,
  refreshVersion,
  onNavigate,
}: SystemsPageProps) {
  const selectedSystemId = pathname.match(/^\/systems\/([^/]+)\/?$/)?.[1];
  const selectedSystem = systems.find(
    (system) => system.id === selectedSystemId,
  );

  if (selectedSystem) {
    if (selectedSystem.id === "thermal-control") {
      if (detailLoadState === "loading") {
        return (
          <p className="loading" role="status">
            Lecture du contrôle thermique…
          </p>
        );
      }

      if (detailLoadState === "error" || systemDetail === null) {
        return (
          <section className="error-panel" role="alert">
            <h2>Détail du système indisponible</h2>
            <p>
              Simulation indisponible. Impossible de lire les métriques et
              composants thermiques.
            </p>
          </section>
        );
      }

      return (
        <ThermalSystemDetail
          detail={systemDetail}
          onNavigate={onNavigate}
          refreshVersion={refreshVersion}
        />
      );
    }

    const selectedSystemIndicators = dashboard.indicators.filter(
      (indicator) => indicator.systemId === selectedSystem.id,
    );
    const selectedSystemStatus = getSystemStatus(selectedSystem.id, dashboard);
    const selectedSystemMeasurementAt = getLastValidMeasurementAt(
      selectedSystem.id,
      dashboard,
    );

    return (
      <section
        className="system-detail"
        aria-labelledby="system-detail-heading"
      >
        <a className="back-link" href="/systems" onClick={onNavigate}>
          Retour aux systèmes
        </a>
        <div className="section-heading">
          <div>
            <p className="eyebrow">SYSTÈME DE LA STATION</p>
            <h2 id="system-detail-heading">{selectedSystem.name}</h2>
            <p className="system-description">{selectedSystem.description}</p>
          </div>
          <span
            className={`system-status status status-${selectedSystemStatus.toLowerCase()}`}
          >
            {statusLabels[selectedSystemStatus]}
          </span>
        </div>
        {dashboard.offlineSystems.includes(selectedSystem.id) && (
          <p className="offline-system-note">
            Télémétrie suspendue — système simulé hors ligne.
          </p>
        )}
        <div className="system-measurement">
          <span>Dernière mesure valide</span>
          <strong>{formatMeasurementDate(selectedSystemMeasurementAt)}</strong>
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
    );
  }

  if (pathname !== "/systems") {
    return (
      <section className="error-panel" role="alert">
        <h2>Système introuvable</h2>
        <a href="/systems" onClick={onNavigate}>
          Retour aux systèmes
        </a>
      </section>
    );
  }

  return (
    <section className="systems-section" aria-label="Systèmes de la station">
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
                onClick={onNavigate}
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
                {dashboard.offlineSystems.includes(system.id) && (
                  <span className="offline-system-note">
                    Télémétrie suspendue — système simulé hors ligne.
                  </span>
                )}
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
  );
}
