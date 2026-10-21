import { useEffect, useState } from "react";
import { getEvents, type EventLogPage } from "./dashboardApi";
import { formatMeasurementDate } from "./dashboardFormat";

export default function EventLog() {
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<EventLogPage | null>(null);
  const [loadState, setLoadState] = useState<"loading" | "loaded" | "error">(
    "loading",
  );

  useEffect(() => {
    const controller = new AbortController();
    setLoadState("loading");

    getEvents(page, controller.signal)
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
  }, [page]);

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
        Événements techniques des sept derniers jours
      </p>

      {loadState === "loading" && (
        <p className="loading" role="status">
          Lecture du journal technique…
        </p>
      )}

      {loadState === "error" && (
        <section className="error-panel" role="alert">
          <h3>Journal technique indisponible</h3>
          <p>
            Impossible de lire les événements de la station.
          </p>
        </section>
      )}

      {loadState === "loaded" && result && result.content.length === 0 && (
        <p className="event-log-empty">
          Aucun événement technique au cours des sept derniers jours.
        </p>
      )}

      {loadState === "loaded" && result && result.content.length > 0 && (
        <>
          <div className="event-log-table-scroll">
            <table
              className="event-log-table"
              aria-label="Événements techniques des sept derniers jours"
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
                {result.content.map((event) => (
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
