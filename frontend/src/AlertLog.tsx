import { useEffect, useState } from "react";
import {
  getAlerts,
  type AlertLevel,
  type AlertPage,
  type AlertStatus,
} from "./dashboardApi";
import { formatMeasurementDate } from "./dashboardFormat";

const systems = [
  { id: "life-support", name: "Support de vie" },
  { id: "thermal-control", name: "Contrôle thermique" },
  { id: "power-supply", name: "Alimentation électrique" },
  { id: "communications", name: "Communications" },
];

const alertStatusLabels: Record<AlertStatus, string> = {
  OPEN: "Ouverte",
  RESOLVED: "Résolue",
};

interface AlertFilters {
  status: AlertStatus | "";
  level: AlertLevel | "";
  systemId: string;
}

export default function AlertLog() {
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState<AlertFilters>({
    status: "",
    level: "",
    systemId: "",
  });
  const [result, setResult] = useState<AlertPage | null>(null);
  const [loadState, setLoadState] = useState<"loading" | "loaded" | "error">(
    "loading",
  );

  useEffect(() => {
    const controller = new AbortController();
    setLoadState("loading");

    getAlerts(
      page,
      {
        status: filters.status || undefined,
        level: filters.level || undefined,
        systemId: filters.systemId || undefined,
      },
      controller.signal,
    )
      .then((alerts) => {
        setResult(alerts);
        setLoadState("loaded");
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setLoadState("error");
        }
      });

    return () => controller.abort();
  }, [filters, page]);

  function updateFilter<K extends keyof AlertFilters>(
    key: K,
    value: AlertFilters[K],
  ) {
    setFilters((currentFilters) => ({ ...currentFilters, [key]: value }));
    setPage(0);
  }

  return (
    <section className="alert-log-section" aria-labelledby="alert-log-heading">
      <div className="section-heading">
        <div>
          <p className="eyebrow">SURVEILLANCE DE LA STATION</p>
          <h2 id="alert-log-heading">Alertes de la station</h2>
        </div>
      </div>

      <div className="alert-filters" aria-label="Filtres des alertes">
        <label>
          État
          <select
            value={filters.status}
            onChange={(event) => {
              const value = event.currentTarget.value;
              updateFilter(
                "status",
                value === "OPEN" || value === "RESOLVED" ? value : "",
              );
            }}
          >
            <option value="">Tous</option>
            <option value="OPEN">Ouvertes</option>
            <option value="RESOLVED">Résolues</option>
          </select>
        </label>
        <label>
          Niveau
          <select
            value={filters.level}
            onChange={(event) => {
              const value = event.currentTarget.value;
              updateFilter(
                "level",
                value === "INFO" || value === "WARNING" || value === "CRITICAL"
                  ? value
                  : "",
              );
            }}
          >
            <option value="">Tous</option>
            <option value="CRITICAL">CRITICAL</option>
            <option value="WARNING">WARNING</option>
            <option value="INFO">INFO</option>
          </select>
        </label>
        <label>
          Système
          <select
            value={filters.systemId}
            onChange={(event) => updateFilter("systemId", event.currentTarget.value)}
          >
            <option value="">Tous</option>
            {systems.map((system) => (
              <option key={system.id} value={system.id}>
                {system.name}
              </option>
            ))}
          </select>
        </label>
      </div>

      {loadState === "loading" && (
        <p className="loading" role="status">
          Lecture des alertes…
        </p>
      )}

      {loadState === "error" && (
        <section className="error-panel" role="alert">
          <h3>Alertes indisponibles</h3>
          <p>Impossible de lire les alertes de la station.</p>
        </section>
      )}

      {loadState === "loaded" && result && result.items.length === 0 && (
        <p className="alert-log-empty">Aucune alerte pour ces filtres.</p>
      )}

      {loadState === "loaded" && result && result.items.length > 0 && (
        <>
          <div className="alert-log-table-scroll">
            <table className="alert-log-table" aria-label="Épisodes d’alerte">
              <thead>
                <tr>
                  <th scope="col">Détection</th>
                  <th scope="col">Niveau</th>
                  <th scope="col">Système</th>
                  <th scope="col">État</th>
                  <th scope="col">Résolution</th>
                  <th scope="col">Épisode</th>
                </tr>
              </thead>
              <tbody>
                {result.items.map((alert) => (
                  <tr key={alert.id}>
                    <td>{formatMeasurementDate(alert.detectedAt)}</td>
                    <td>{alert.level}</td>
                    <td>{alert.systemName}</td>
                    <td>{alertStatusLabels[alert.status]}</td>
                    <td>
                      {alert.resolvedAt === null
                        ? "—"
                        : formatMeasurementDate(alert.resolvedAt)}
                    </td>
                    <td>{alert.message}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <nav
            className="event-log-pagination"
            aria-label="Pagination des alertes"
          >
            <span>
              Page {result.page + 1} sur {result.totalPages} ·{" "}
              {result.totalElements} alerte
              {result.totalElements === 1 ? "" : "s"}
            </span>
            <div>
              <button
                type="button"
                onClick={() => setPage((currentPage) => currentPage - 1)}
                disabled={result.page === 0}
              >
                Précédent
              </button>
              <button
                type="button"
                onClick={() => setPage((currentPage) => currentPage + 1)}
                disabled={result.page + 1 >= result.totalPages}
              >
                Suivant
              </button>
            </div>
          </nav>
        </>
      )}
    </section>
  );
}
