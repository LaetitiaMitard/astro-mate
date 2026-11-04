import { useId, type MouseEvent } from "react";
import type { SystemStatus } from "../../shared/api/models";

const stationImage = new URL("../../assets/station.jpeg", import.meta.url).href;

interface StationOverviewProps {
  systems: { id: string; name: string; status: SystemStatus }[];
  stationStatus: SystemStatus;
  statusLabels: Record<SystemStatus, string>;
  onNavigate: (event: MouseEvent<HTMLAnchorElement>) => void;
}

export default function StationOverview({
  systems,
  stationStatus,
  statusLabels,
  onNavigate,
}: StationOverviewProps) {
  const arrowId = useId();

  return (
    <section className="station-overview" aria-labelledby="station-heading">
      <div className="station-overview-heading">
        <h2 id="station-heading">Vue de la station</h2>
        <span className={`metric-status status-${stationStatus.toLowerCase()}`}>
          {statusLabels[stationStatus]}
        </span>
      </div>
      <div className="station-scene">
      <img
        className="station-illustration"
        src={stationImage}
        alt="Station spatiale et ses panneaux solaires en orbite au-dessus de la Terre"
        width="1024"
        height="683"
      />
      <svg className="station-connectors" viewBox="0 0 1024 683" fill="none" aria-hidden="true">
        <defs>
          <marker id={arrowId} viewBox="0 0 10 10" refX="8" refY="5" markerWidth="7" markerHeight="7" orient="auto">
            <path d="m1 1 7 4-7 4" />
          </marker>
        </defs>
        <g markerEnd={`url(#${arrowId})`}>
          <path data-system="life-support" d="M317 164H400L447 269" />
          <path data-system="communications" d="M676 157V202L617 251" />
          <path data-system="power-supply" d="M870 458V420L825 355" />
          <path data-system="thermal-control" d="M543 492V436L565 380" />
        </g>
      </svg>
      <nav className="station-links" aria-label="Systèmes sur la station">
        {systems.map((system) => (
          <a
            key={system.id}
            className={`station-link station-link-${system.id}`}
            href={`/systems/${system.id}`}
            onClick={onNavigate}
          >
            <span>{system.name}</span>
            <span className={`station-link-status status-${system.status.toLowerCase()}`}>
              {statusLabels[system.status]}
            </span>
          </a>
        ))}
      </nav>
      </div>
      <p className="station-caption">
        <span>Explorez les systèmes de la station</span>
      </p>
    </section>
  );
}
