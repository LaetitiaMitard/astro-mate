import { afterEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen, within } from "@testing-library/react";
import App from "./App";

const dashboard = {
  stationStatus: "NOMINAL",
  offlineSystems: [],
  simulationStatus: "ACTIVE",
  lastMeasurementAt: "2026-10-20T12:00:00Z",
  indicators: [
    {
      key: "oxygen",
      systemId: "life-support",
      sourceMetricIds: ["oxygen-level"],
      name: "O₂",
      value: 21,
      unit: "% vol.",
      status: "NOMINAL",
      nominalRange: { min: 19, max: 23, minInclusive: true, maxInclusive: true },
      measuredAt: "2026-10-20T12:00:00Z",
    },
    {
      key: "carbonDioxide",
      systemId: "life-support",
      sourceMetricIds: ["carbon-dioxide-level"],
      name: "CO₂",
      value: 400,
      unit: "ppm",
      status: "NOMINAL",
      nominalRange: { min: 0, max: 1000, minInclusive: true, maxInclusive: true },
      measuredAt: "2026-10-20T12:00:00Z",
    },
    {
      key: "temperature",
      systemId: "thermal-control",
      sourceMetricIds: ["system-temperature"],
      name: "Température",
      value: 22.3,
      unit: "°C",
      status: "NOMINAL",
      nominalRange: { min: 18, max: 26, minInclusive: true, maxInclusive: true },
      measuredAt: "2026-10-20T12:00:00Z",
    },
    {
      key: "availableEnergy",
      systemId: "power-supply",
      sourceMetricIds: ["available-energy"],
      name: "Énergie disponible",
      value: 78,
      unit: "%",
      status: "NOMINAL",
      nominalRange: { min: 20, max: 100, minInclusive: true, maxInclusive: true },
      measuredAt: "2026-10-20T12:00:00Z",
    },
  ],
  recentOpenAlerts: [],
};

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe("AstroMate dashboard", () => {
  it("shows the four nominal indicators returned by the simulation", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(dashboard), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );

    render(<App />);

    const stationSummary = await screen.findByRole("region", {
      name: "État de la station",
    });
    expect(within(stationSummary).getByText("NOMINAL")).toBeInTheDocument();
    expect(screen.getByText("Simulation active")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "O₂" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "CO₂" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Température" })).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "Énergie disponible" }),
    ).toBeInTheDocument();
    expect(screen.getByText("21")).toBeInTheDocument();
    expect(screen.getByText("400")).toBeInTheDocument();
    expect(screen.getByText("22,3")).toBeInTheDocument();
    expect(screen.getByText("78")).toBeInTheDocument();
    expect(screen.getByText("19–23 % vol.")).toBeInTheDocument();
    expect(screen.getByText(/^0–1.*000 ppm$/)).toBeInTheDocument();
    expect(screen.getByText("18–26 °C")).toBeInTheDocument();
    expect(screen.getByText("20–100 %")).toBeInTheDocument();
    expect(screen.getByText("Aucune alerte ouverte")).toBeInTheDocument();
  });

  it("does not display nominal values when the dashboard API is unavailable", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ title: "Service indisponible" }), {
          status: 503,
          headers: { "Content-Type": "application/problem+json" },
        }),
      ),
    );

    render(<App />);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Tableau de bord indisponible",
    );
    expect(screen.queryByText("21")).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "O₂" })).not.toBeInTheDocument();
  });
});
