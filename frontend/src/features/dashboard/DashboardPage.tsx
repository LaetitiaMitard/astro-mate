import type { MouseEvent } from "react";
import type { Dashboard } from "../../shared/api/models";
import { navigationItems } from "../../shared/navigation";
import {
  getSystemStatus,
  statusLabels,
  systems,
} from "../../shared/domain/systems";
import {
  formatMeasurementDate,
  formatRange,
  formatValue,
} from "../../shared/lib/dashboardFormat";
import StationOverview from "./StationOverview";

const indicatorIcons: Record<string, string> = {
  oxygen: "M8 5a7 7 0 1 0 0 14A7 7 0 0 0 8 5Zm9 11c0-4 5-4 5-1 0 2-5 3-5 6h5",
  carbonDioxide: "M6 18a4 4 0 0 1-1-8 7 7 0 0 1 13-1 4.5 4.5 0 0 1 0 9Z",
  temperature: "M10 14.5V5a2 2 0 0 1 4 0v9.5a4 4 0 1 1-4 0ZM12 10v8M17 5h3M17 9h2",
  availableEnergy: "m14 2-9 12h6l-1 8 9-12h-6Z",
};

interface DashboardPageProps {
  dashboard: Dashboard;
  onNavigate: (event: MouseEvent<HTMLAnchorElement>) => void;
}

export default function DashboardPage({
  dashboard,
  onNavigate,
}: DashboardPageProps) {
  return (
    <>
      <section className="station-summary" aria-label="État de la station">
        <div>
          <p className="eyebrow">ÉTAT GLOBAL</p>
          <strong className={`status status-${dashboard.stationStatus.toLowerCase()}`}>
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
        <p className="system-card-measurement">
          Dernière mesure valide :{" "}
          {dashboard.lastMeasurementAt === null ? (
            formatMeasurementDate(null)
          ) : (
            <time dateTime={dashboard.lastMeasurementAt}>
              {formatMeasurementDate(dashboard.lastMeasurementAt)}
            </time>
          )}
        </p>
      </section>

      <section className="indicators-section">
        <h2 className="visually-hidden">Indicateurs principaux</h2>
        <div className="indicator-grid">
          {dashboard.indicators.map((indicator) => (
            <article
              className={`indicator-card indicator-${indicator.key}`}
              key={indicator.key}
            >
              <div className="indicator-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none">
                  <path d={indicatorIcons[indicator.key]} />
                </svg>
              </div>
              <div className="indicator-card-heading">
                <h3>{indicator.name}</h3>
              </div>
              <p className="indicator-value">
                <strong>{formatValue(indicator.value)}</strong>
                <span>{indicator.unit}</span>
              </p>
              <div className="indicator-range">
                <span
                  className={`indicator-state status-${indicator.status.toLowerCase()}`}
                >
                  {statusLabels[indicator.status]}
                </span>
                <strong>
                  <span className="visually-hidden">Plage nominale : </span>
                  {formatRange(indicator.nominalRange, indicator.unit)}
                </strong>
              </div>
              <p className="system-card-measurement">
                Dernière mesure valide :{" "}
                <time dateTime={indicator.measuredAt ?? undefined}>
                  {formatMeasurementDate(indicator.measuredAt)}
                </time>
              </p>
              {dashboard.offlineSystems.includes(indicator.systemId) ? (
                <p className="offline-system-note">
                  Télémétrie suspendue — système simulé hors ligne.
                </p>
              ) : (
                indicator.status === "UNKNOWN" && (
                  <p className="offline-system-note">
                    Mesure absente ou périmée — état courant inconnu.
                  </p>
                )
              )}
            </article>
          ))}
        </div>
      </section>

      <div className="dashboard-main-grid">
        <StationOverview
          systems={systems.map((system) => ({
            ...system,
            status: getSystemStatus(system.id, dashboard),
          }))}
          stationStatus={dashboard.stationStatus}
          statusLabels={statusLabels}
          onNavigate={onNavigate}
        />
        <section className="alerts-panel" aria-labelledby="alerts-heading">
          <div className="section-heading">
            <h2 id="alerts-heading">
              Alertes ouvertes{" "}
              <span className="alert-count">
                ({dashboard.recentOpenAlerts.length})
              </span>
            </h2>
            <a className="back-link" href="/alerts" onClick={onNavigate}>
              Consulter toutes les alertes
            </a>
          </div>
          {dashboard.recentOpenAlerts.length === 0 ? (
            <div className="empty-alerts">
              <span className="empty-alerts-icon" aria-hidden="true">
                ✓
              </span>
              <p>Aucune alerte ouverte</p>
              <span>Les alertes de la simulation apparaîtront ici.</span>
            </div>
          ) : (
            <ul className="recent-alert-list">
              {dashboard.recentOpenAlerts.map((alert) => (
                <li key={alert.id}>
                  <span
                    className={`indicator-state status-${alert.level.toLowerCase()}`}
                  >
                    {alert.level === "INFO"
                      ? "INFO"
                      : statusLabels[alert.level]}
                  </span>
                  <strong>{alert.message}</strong>
                  <span>{alert.systemName}</span>
                  <time dateTime={alert.detectedAt}>
                    {formatMeasurementDate(alert.detectedAt)}
                  </time>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
      <section className="quick-actions" aria-labelledby="quick-actions-heading">
        <h2 id="quick-actions-heading">Actions rapides</h2>
        <div className="quick-action-grid">
          {navigationItems
            .filter((item) => item.href !== "/")
            .map((item) => (
              <a key={item.href} href={item.href} onClick={onNavigate}>
                <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d={item.icon} />
                </svg>
                <span>
                  {item.href === "/systems"
                    ? "Voir les systèmes"
                    : item.href === "/alerts"
                      ? "Voir les alertes"
                      : "Consulter le journal"}
                </span>
                <span aria-hidden="true">→</span>
              </a>
            ))}
        </div>
      </section>
      <footer className="dashboard-footer">
        <span>AstroMate · Centre de contrôle</span>
        <span>Explorer aujourd’hui. Un avenir plus sûr demain.</span>
      </footer>
    </>
  );
}
