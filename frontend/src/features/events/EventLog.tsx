import { useEffect, useState } from "react";
import {
  getEvents,
} from "../../shared/api/client";
import type {
  EventLogFilters,
  EventLogPage,
  TechnicalEvent,
} from "../../shared/api/models";
import { formatMeasurementDate } from "../../shared/lib/dashboardFormat";

interface EventLogForm {
  from: string;
  to: string;
  level: "" | TechnicalEvent["level"];
  category: "" | TechnicalEvent["category"];
  query: string;
  sort: EventLogFilters["sort"];
}

const initialFilters: EventLogForm = {
  from: "",
  to: "",
  level: "",
  category: "",
  query: "",
  sort: "occurredAt,desc",
};

function toUtcIso(value: string): string | undefined {
  if (!value) {
    return undefined;
  }
  const date = new Date(value);
  return Number.isFinite(date.getTime()) ? date.toISOString() : value;
}

function getPeriodError(filters: Pick<EventLogForm, "from" | "to">): string | null {
  const from = filters.from ? new Date(filters.from).getTime() : null;
  const to = filters.to ? new Date(filters.to).getTime() : null;
  if (
    (from !== null && !Number.isFinite(from)) ||
    (to !== null && !Number.isFinite(to))
  ) {
    return "Les dates de la période doivent être valides.";
  }
  if (from !== null && to !== null && from >= to) {
    return "La date de début doit précéder la date de fin.";
  }
  return null;
}

export default function EventLog({ refreshVersion = 0 }: { refreshVersion?: number }) {
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState<EventLogForm>(initialFilters);
  const [result, setResult] = useState<EventLogPage | null>(null);
  const [loadState, setLoadState] = useState<"loading" | "loaded" | "error">(
    "loading",
  );
  const periodError = getPeriodError(filters);
  const canDisplayResults =
    !periodError && loadState === "loaded" && result !== null;

  useEffect(() => {
    const controller = new AbortController();
    if (periodError) {
      setLoadState("loaded");
      return;
    }
    setLoadState("loading");

    getEvents(
      page,
      {
        from: toUtcIso(filters.from),
        to: toUtcIso(filters.to),
        level: filters.level || undefined,
        category: filters.category || undefined,
        query: filters.query || undefined,
        sort: filters.sort,
      },
      controller.signal,
    )
      .then((events) => {
        setResult(events);
        setLoadState("loaded");
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setLoadState("error");
        }
      });

    return () => controller.abort();
  }, [filters, page, periodError, refreshVersion]);

  function updateFilter<K extends keyof EventLogForm>(
    key: K,
    value: EventLogForm[K],
  ) {
    setFilters((currentFilters) => ({ ...currentFilters, [key]: value }));
    setPage(0);
  }

  const hasFilters = Object.entries(filters).some(
    ([key, value]) => key !== "sort" && value !== "",
  );

  return (
    <section
      className="event-log-section"
      aria-labelledby="event-log-heading"
    >
      <div className="section-heading">
        <div>
          <p className="eyebrow">HISTORIQUE DE LA STATION</p>
          <h2 id="event-log-heading">Journal chronologique</h2>
        </div>
      </div>
      <p className="event-log-window">
        Période par défaut : les sept derniers jours · 20 événements maximum
        par page
      </p>

      <div className="event-log-filters" aria-label="Filtres et recherche du journal">
        <label>
          Période début (incluse)
          <input
            type="datetime-local"
            value={filters.from}
            onChange={(event) => updateFilter("from", event.currentTarget.value)}
          />
        </label>
        <label>
          Période fin (exclue)
          <input
            type="datetime-local"
            value={filters.to}
            onChange={(event) => updateFilter("to", event.currentTarget.value)}
          />
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
          Catégorie
          <select
            value={filters.category}
            onChange={(event) => {
              const value = event.currentTarget.value;
              updateFilter(
                "category",
                value === "SYSTEM" ||
                  value === "ALERT" ||
                  value === "MAINTENANCE" ||
                  value === "OPERATION" ||
                  value === "DATA"
                  ? value
                  : "",
              );
            }}
          >
            <option value="">Toutes</option>
            <option value="SYSTEM">SYSTEM</option>
            <option value="ALERT">ALERT</option>
            <option value="MAINTENANCE">MAINTENANCE</option>
            <option value="OPERATION">OPERATION</option>
            <option value="DATA">DATA</option>
          </select>
        </label>
        <label>
          Recherche
          <input
            type="search"
            value={filters.query}
            maxLength={200}
            onChange={(event) => updateFilter("query", event.currentTarget.value)}
            placeholder="Message, source ou système"
          />
        </label>
        <label>
          Ordre chronologique
          <select
            value={filters.sort}
            onChange={(event) =>
              updateFilter(
                "sort",
                event.currentTarget.value === "occurredAt,asc"
                  ? "occurredAt,asc"
                  : "occurredAt,desc",
              )
            }
          >
            <option value="occurredAt,desc">Plus récents d’abord</option>
            <option value="occurredAt,asc">Plus anciens d’abord</option>
          </select>
        </label>
      </div>

      {periodError && (
        <p className="event-log-validation" role="alert">
          {periodError}
        </p>
      )}

      {loadState === "loading" && (
        <p className="loading" role="status">
          Lecture du journal technique…
        </p>
      )}

      {loadState === "error" && (
        <section className="error-panel" role="alert">
          <h3>Journal technique indisponible</h3>
          <p>
            Simulation indisponible. Impossible de lire les événements de la station.
          </p>
        </section>
      )}

      {canDisplayResults && result.items.length === 0 && (
        <p className="event-log-empty">
          {hasFilters
            ? "Aucun événement technique ne correspond à ces critères."
            : "Aucun événement technique au cours des sept derniers jours."}
        </p>
      )}

      {canDisplayResults && result.items.length > 0 && (
        <>
          <div className="event-log-table-scroll">
            <table
              className="event-log-table"
              aria-label="Événements techniques filtrés"
            >
              <thead>
                <tr>
                  <th scope="col">Date et heure</th>
                  <th scope="col">Niveau</th>
                  <th scope="col">Catégorie</th>
                  <th scope="col">Système</th>
                  <th scope="col">Événement</th>
                </tr>
              </thead>
              <tbody>
                {result.items.map((event) => (
                  <tr key={event.id}>
                    <td>{formatMeasurementDate(event.occurredAt)}</td>
                    <td>{event.level}</td>
                    <td>{event.category}</td>
                    <td>{event.systemName ?? "Station"}</td>
                    <td>{event.message}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <nav className="event-log-pagination" aria-label="Pagination du journal">
            <span>
              Page {result.page + 1} sur {result.totalPages} ·{" "}
              {result.totalElements} événement
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
