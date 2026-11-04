import { afterEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import StationOverview from "./StationOverview";
import type { SystemStatus } from "../../shared/api/models";

const labels: Record<SystemStatus, string> = {
  NOMINAL: "NOMINAL",
  WARNING: "AVERTISSEMENT",
  CRITICAL: "CRITIQUE",
  OFFLINE: "HORS LIGNE",
  UNKNOWN: "INCONNU",
};

const systems = [
  { id: "life-support", name: "Support de vie", status: "NOMINAL" },
  { id: "thermal-control", name: "Contrôle thermique", status: "WARNING" },
  { id: "power-supply", name: "Alimentation électrique", status: "UNKNOWN" },
  { id: "communications", name: "Communications", status: "OFFLINE" },
] satisfies { id: string; name: string; status: SystemStatus }[];

afterEach(cleanup);

describe("Station overview", () => {
  it("shows the replacement image, four connector arrows and keyboard-accessible system links", () => {
    const onNavigate = vi.fn((event: { preventDefault: () => void }) => event.preventDefault());
    render(
      <StationOverview
        systems={systems}
        stationStatus="WARNING"
        statusLabels={labels}
        onNavigate={onNavigate}
      />,
    );

    const image = screen.getByRole("img", { name: /Station spatiale/ });
    expect(image.getAttribute("src")).toContain("/assets/station.jpeg");
    expect(image).toHaveAttribute("width", "1024");
    expect(image).toHaveAttribute("height", "683");
    expect(screen.queryByText(/NASA/)).not.toBeInTheDocument();
    const scene = image.closest(".station-scene")!;
    const connectors = scene.querySelector(".station-connectors")!;
    expect(connectors).toHaveAttribute("viewBox", "0 0 1024 683");
    expect(connectors).toHaveAttribute("aria-hidden", "true");
    expect(connectors.querySelectorAll("g > path")).toHaveLength(4);
    for (const system of systems) {
      expect(connectors.querySelector(`[data-system="${system.id}"]`)).not.toBeNull();
    }
    const navigation = screen.getByRole("navigation", { name: "Systèmes sur la station" });
    expect(within(navigation).getAllByRole("link")).toHaveLength(4);
    for (const system of systems) {
      const link = within(navigation).getByRole("link", {
        name: `${system.name} ${labels[system.status]}`,
      });
      expect(link).toHaveAttribute("href", `/systems/${system.id}`);
      link.focus();
      expect(link).toHaveFocus();
      fireEvent.click(link);
    }
    expect(onNavigate).toHaveBeenCalledTimes(4);
  });

  it.each<SystemStatus>(["NOMINAL", "WARNING", "CRITICAL", "OFFLINE", "UNKNOWN"])(
    "updates the global and system states to %s without relying on color",
    (status) => {
      const props = {
        systems,
        stationStatus: "NOMINAL" as SystemStatus,
        statusLabels: labels,
        onNavigate: vi.fn(),
      };
      const { rerender } = render(<StationOverview {...props} />);
      rerender(
        <StationOverview
          {...props}
          stationStatus={status}
          systems={systems.map((system) => ({ ...system, status }))}
        />,
      );
      const station = screen.getByRole("region", { name: "Vue de la station" });
      expect(within(station).getAllByText(labels[status])).toHaveLength(5);
      for (const link of within(station).getAllByRole("link")) {
        expect(within(link).getByText(labels[status]))
          .toHaveClass(`status-${status.toLowerCase()}`);
      }
    },
  );
});
