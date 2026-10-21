import { afterEach, describe, expect, it, vi } from "vitest";
import { act, cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import App from "./App";

class TestEventSource {
  static instance: TestEventSource | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  close = vi.fn();

  constructor(readonly url: string) {
    TestEventSource.instance = this;
  }
}

const dashboard = {
  stationStatus: "NOMINAL",
  offlineSystems: [],
  systemStatuses: [
    { systemId: "life-support", status: "NOMINAL" },
    { systemId: "thermal-control", status: "NOMINAL" },
    { systemId: "power-supply", status: "NOMINAL" },
    { systemId: "communications", status: "NOMINAL" },
  ],
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

const thermalSystemDetail = {
  id: "thermal-control",
  name: "Contrôle thermique",
  description: "Température et circulation thermique",
  status: "CRITICAL",
  availability: "ONLINE",
  updatedAt: "2026-10-20T12:05:00Z",
  indicators: [
    {
      key: "temperature",
      systemId: "thermal-control",
      sourceMetricIds: ["radiator-a-temperature", "radiator-b-temperature"],
      name: "Température",
      value: 26.7,
      unit: "°C",
      status: "CRITICAL",
      nominalRange: {
        min: 18,
        max: 26,
        minInclusive: true,
        maxInclusive: true,
      },
      measuredAt: "2026-10-20T12:05:00Z",
    },
  ],
  metrics: [
    {
      id: "radiator-a-temperature",
      name: "Température Radiateur A",
      value: 31,
      unit: "°C",
      status: "CRITICAL",
      nominalRange: {
        min: 18,
        max: 26,
        minInclusive: true,
        maxInclusive: true,
      },
      criticalLimits: { minExclusive: 15, maxExclusive: 30 },
      measuredAt: "2026-10-20T12:05:00Z",
      componentId: "radiator-a",
    },
    {
      id: "radiator-b-temperature",
      name: "Température Radiateur B",
      value: 22.4,
      unit: "°C",
      status: "NOMINAL",
      nominalRange: {
        min: 18,
        max: 26,
        minInclusive: true,
        maxInclusive: true,
      },
      criticalLimits: { minExclusive: 15, maxExclusive: 30 },
      measuredAt: "2026-10-20T12:05:00Z",
      componentId: "radiator-b",
    },
    {
      id: "fluid-flow",
      name: "Débit de fluide",
      value: 120,
      unit: "L/min",
      status: "NOMINAL",
      nominalRange: {
        min: 80,
        max: 200,
        minInclusive: true,
        maxInclusive: true,
      },
      criticalLimits: { minExclusive: null, maxExclusive: null },
      measuredAt: "2026-10-20T12:05:00Z",
      componentId: null,
    },
    {
      id: "pressure",
      name: "Pression",
      value: 2.1,
      unit: "bar",
      status: "NOMINAL",
      nominalRange: {
        min: 1.5,
        max: 3,
        minInclusive: true,
        maxInclusive: true,
      },
      criticalLimits: { minExclusive: null, maxExclusive: null },
      measuredAt: "2026-10-20T12:05:00Z",
      componentId: null,
    },
    {
      id: "thermal-consumption",
      name: "Consommation thermique",
      value: 1.2,
      unit: "kW",
      status: "NOMINAL",
      nominalRange: {
        min: null,
        max: 2,
        minInclusive: true,
        maxInclusive: false,
      },
      criticalLimits: { minExclusive: null, maxExclusive: null },
      measuredAt: "2026-10-20T12:05:00Z",
      componentId: null,
    },
  ],
  components: [
    {
      id: "radiator-a",
      name: "Radiateur A",
      status: "CRITICAL",
      value: 31,
      unit: "°C",
      updatedAt: "2026-10-20T12:05:00Z",
      simulated: false,
    },
    {
      id: "radiator-b",
      name: "Radiateur B",
      status: "NOMINAL",
      value: 22.4,
      unit: "°C",
      updatedAt: "2026-10-20T12:05:00Z",
      simulated: false,
    },
    {
      id: "main-pump",
      name: "Pompe principale",
      status: "NOMINAL",
      value: null,
      unit: null,
      updatedAt: "2026-10-20T12:00:00Z",
      simulated: true,
    },
    {
      id: "heat-exchanger",
      name: "Échangeur thermique",
      status: "NOMINAL",
      value: null,
      unit: null,
      updatedAt: "2026-10-20T12:00:00Z",
      simulated: true,
    },
  ],
  recentEvents: [],
};

const thermalTemperatureHistory = {
  indicatorKey: "temperature",
  sourceMetricIds: ["radiator-a-temperature", "radiator-b-temperature"],
  name: "Température",
  unit: "°C",
  from: "2026-10-20T06:00:00Z",
  to: "2026-10-20T12:00:00Z",
  measurements: [
    { value: 22.3, measuredAt: "2026-10-20T11:59:50Z" },
    { value: 28.6, measuredAt: "2026-10-20T12:00:00Z" },
  ],
};

afterEach(() => {
  cleanup();
  window.history.replaceState(null, "", "/");
  vi.unstubAllGlobals();
  TestEventSource.instance = null;
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

  it("shows recent technical events and navigates between pages", async () => {
    const eventLogPage = (page: number) => ({
      items: [
        {
          id: `event-${page}`,
          level: "WARNING",
          category: "DATA",
          systemId: "thermal-control",
          systemName: "Contrôle thermique",
          source: "simulation",
          message: `Événement page ${page + 1}`,
          occurredAt: "2026-10-21T12:00:00Z",
        },
      ],
      page,
      pageSize: 20,
      totalElements: 21,
      totalPages: 2,
    });
    const fetchMock = vi.fn((input: RequestInfo | URL) => {
      const url = typeof input === "string" ? input : input.toString();
      const payload = url.startsWith("/api/v1/events")
        ? eventLogPage(Number(new URL(url, window.location.origin).searchParams.get("page")))
        : dashboard;
      return Promise.resolve(
        new Response(JSON.stringify(payload), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      );
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<App />);
    fireEvent.click(await screen.findByRole("link", { name: "Journal" }));

    expect(
      await screen.findByRole("heading", { name: "Journal chronologique" }),
    ).toBeInTheDocument();
    expect(await screen.findByText("Événement page 1")).toBeInTheDocument();
    expect(screen.getByText("Contrôle thermique")).toBeInTheDocument();
    expect(
      screen.getByRole("navigation", { name: "Pagination du journal" }),
    ).toHaveTextContent(/Page\s+1\s+sur\s+2/);
    expect(screen.getByRole("button", { name: "Précédent" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Suivant" })).toBeEnabled();
    expect(
      screen.queryByRole("button", { name: /export/i }),
    ).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Suivant" }));

    expect(await screen.findByText("Événement page 2")).toBeInTheDocument();
    expect(
      screen.getByRole("navigation", { name: "Pagination du journal" }),
    ).toHaveTextContent(/Page\s+2\s+sur\s+2/);
    expect(screen.getByRole("button", { name: "Précédent" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Suivant" })).toBeDisabled();
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/events?page=1",
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    );
  });

  it("announces event loading and an empty journal", async () => {
    window.history.replaceState(null, "", "/events");
    let resolveEvents: ((response: Response) => void) | undefined;
    const pendingResponse = new Promise<Response>((resolve) => {
      resolveEvents = resolve;
    });
    vi.stubGlobal("fetch", vi.fn().mockReturnValue(pendingResponse));

    render(<App />);

    expect(screen.getByRole("status")).toHaveTextContent(
      "Lecture du journal technique",
    );
    await act(async () => {
      resolveEvents?.(
        new Response(
          JSON.stringify({
            items: [],
            page: 0,
            pageSize: 20,
            totalElements: 0,
            totalPages: 0,
          }),
          {
            status: 200,
            headers: { "Content-Type": "application/json" },
          },
        ),
      );
    });

    expect(
      await screen.findByText(
        "Aucun événement technique au cours des sept derniers jours.",
      ),
    ).toBeInTheDocument();
    expect(screen.queryByRole("navigation", { name: "Pagination du journal" }))
      .not.toBeInTheDocument();
  });

  it("shows an explicit message when the event API fails", async () => {
    window.history.replaceState(null, "", "/events");
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
      "Journal technique indisponible",
    );
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  it("refreshes the dashboard when the server reports a measurement change", async () => {
    vi.stubGlobal("EventSource", TestEventSource);
    const changedDashboard = {
      ...dashboard,
      indicators: dashboard.indicators.map((indicator) =>
        indicator.key === "temperature"
          ? { ...indicator, value: 27, status: "WARNING" }
          : indicator,
      ),
    };
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(JSON.stringify(dashboard), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify(changedDashboard), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      );
    vi.stubGlobal("fetch", fetchMock);

    render(<App />);

    expect(await screen.findByText("22,3")).toBeInTheDocument();
    expect(TestEventSource.instance?.url).toBe("/api/v1/telemetry/changes");
    act(() => {
      TestEventSource.instance?.onmessage?.(new MessageEvent("message"));
    });

    expect(await screen.findByText("27")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it("refreshes thermal details and history from the same server event", async () => {
    vi.stubGlobal("EventSource", TestEventSource);
    const changedDetail = {
      ...thermalSystemDetail,
      metrics: thermalSystemDetail.metrics.map((metric) =>
        metric.id === "radiator-a-temperature"
          ? { ...metric, value: 32 }
          : metric,
      ),
      components: thermalSystemDetail.components.map((component) =>
        component.id === "radiator-a" ? { ...component, value: 32 } : component,
      ),
    };
    const changedHistory = {
      ...thermalTemperatureHistory,
      measurements: [
        ...thermalTemperatureHistory.measurements,
        { value: 30.2, measuredAt: "2026-10-20T12:00:10Z" },
      ],
    };
    let detailRequests = 0;
    let historyRequests = 0;
    vi.stubGlobal(
      "fetch",
      vi.fn((input: RequestInfo | URL) => {
        const url = typeof input === "string" ? input : input.toString();
        const payload = url.endsWith("/measurements")
          ? (++historyRequests > 1 ? changedHistory : thermalTemperatureHistory)
          : url.endsWith("/systems/thermal-control")
            ? (++detailRequests > 1 ? changedDetail : thermalSystemDetail)
            : dashboard;
        return Promise.resolve(
          new Response(JSON.stringify(payload), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          }),
        );
      }),
    );

    render(<App />);
    fireEvent.click(screen.getByRole("link", { name: "Systèmes" }));
    const systemsSection = await screen.findByRole("region", {
      name: "Systèmes de la station",
    });
    fireEvent.click(
      within(systemsSection).getByRole("link", { name: /Contrôle thermique/ }),
    );
    expect(await screen.findByText("31", { exact: true })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("tab", { name: "Historique" }));
    expect(await screen.findByText("28,6 °C")).toBeInTheDocument();

    act(() => {
      TestEventSource.instance?.onmessage?.(new MessageEvent("message"));
    });

    expect(await screen.findByText("30,2 °C")).toBeInTheDocument();
    expect(detailRequests).toBe(2);
    expect(historyRequests).toBe(2);
  });

  it("makes the radiator average and its most severe status clear", async () => {
    const criticalDashboard = {
      ...dashboard,
      stationStatus: "CRITICAL",
      systemStatuses: dashboard.systemStatuses.map((system) =>
        system.systemId === "thermal-control"
          ? { ...system, status: "CRITICAL" }
          : system,
      ),
      indicators: dashboard.indicators.map((indicator) =>
        indicator.key === "temperature"
          ? { ...indicator, value: 26.7, status: "CRITICAL" }
          : indicator,
      ),
    };
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(criticalDashboard), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );

    render(<App />);

    expect(
      await screen.findByRole("region", { name: "État de la station" }),
    ).toBeInTheDocument();
    expect(screen.getByText("26,7")).toBeInTheDocument();
    expect(screen.getAllByText("CRITIQUE")).toHaveLength(2);
  });

  it("names offline systems alongside the station status", async () => {
    const offlineDashboard = {
      ...dashboard,
      stationStatus: "OFFLINE",
      offlineSystems: ["thermal-control"],
      systemStatuses: dashboard.systemStatuses.map((system) =>
        system.systemId === "thermal-control"
          ? { ...system, status: "OFFLINE" }
          : system,
      ),
    };
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(offlineDashboard), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );

    render(<App />);

    expect(await screen.findByText("HORS LIGNE")).toBeInTheDocument();
    expect(
      screen.getByText("Systèmes hors ligne : Contrôle thermique"),
    ).toBeInTheDocument();
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

  it("lists the canonical systems and opens their details", async () => {
    const systemsDashboard = {
      ...dashboard,
      systemStatuses: dashboard.systemStatuses.map((system) =>
        system.systemId === "thermal-control"
          ? { ...system, status: "WARNING" }
          : system,
      ),
    };
    vi.stubGlobal(
      "fetch",
      vi.fn((input: RequestInfo | URL) => {
        const url = typeof input === "string" ? input : input.toString();
        const payload = url.endsWith("/measurements")
          ? thermalTemperatureHistory
          : url.endsWith("/systems/thermal-control")
            ? thermalSystemDetail
            : systemsDashboard;
        return Promise.resolve(
          new Response(JSON.stringify(payload), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          }),
        );
      }),
    );

    render(<App />);
    fireEvent.click(screen.getByRole("link", { name: "Systèmes" }));

    const systemsSection = await screen.findByRole("region", {
      name: "Systèmes de la station",
    });
    const lifeSupport = within(systemsSection).getByRole("link", {
      name: /Support de vie/,
    });
    expect(lifeSupport).toBeInTheDocument();
    expect(
      within(systemsSection).getByRole("link", { name: /Contrôle thermique/ }),
    ).toBeInTheDocument();
    expect(
      within(systemsSection).getByRole("link", {
        name: /Alimentation électrique/,
      }),
    ).toBeInTheDocument();
    expect(
      within(systemsSection).getByRole("link", { name: /Communications/ }),
    ).toBeInTheDocument();
    const communications = within(systemsSection).getByRole("link", {
      name: /Communications/,
    });
    expect(within(communications).getByText("NOMINAL")).toBeInTheDocument();
    expect(
      within(communications).getByText(/Aucune mesure valide/),
    ).toBeInTheDocument();

    const thermalSystem = within(systemsSection).getByRole("link", {
      name: /Contrôle thermique/,
    });
    expect(within(thermalSystem).getByText("AVERTISSEMENT")).toBeInTheDocument();
    expect(within(thermalSystem).getByText(/20\/10\/2026/)).toBeInTheDocument();

    fireEvent.click(thermalSystem);

    expect(
      await screen.findByRole("heading", { name: "Contrôle thermique" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Retour aux systèmes" })).toHaveAttribute(
      "href",
      "/systems",
    );
    expect(screen.getByText("31", { exact: true })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("link", { name: "Retour aux systèmes" }));
    await screen.findByRole("heading", { name: "Systèmes" });
    for (const systemName of [
      "Support de vie",
      "Alimentation électrique",
      "Communications",
    ]) {
      fireEvent.click(
        screen.getByRole("link", { name: new RegExp(systemName) }),
      );
      expect(
        await screen.findByRole("heading", { name: systemName }),
      ).toBeInTheDocument();
      fireEvent.click(screen.getByRole("link", { name: "Retour aux systèmes" }));
      await screen.findByRole("heading", { name: "Systèmes" });
    }
  });

  it("shows thermal references and components with distinct tabs and disabled maintenance", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn((input: RequestInfo | URL) => {
        const url = typeof input === "string" ? input : input.toString();
        const payload = url.endsWith("/measurements")
          ? thermalTemperatureHistory
          : url.endsWith("/systems/thermal-control")
            ? thermalSystemDetail
            : dashboard;
        return Promise.resolve(
          new Response(JSON.stringify(payload), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          }),
        );
      }),
    );

    render(<App />);
    fireEvent.click(screen.getByRole("link", { name: "Systèmes" }));
    const systemsSection = await screen.findByRole("region", {
      name: "Systèmes de la station",
    });
    fireEvent.click(
      within(systemsSection).getByRole("link", { name: /Contrôle thermique/ }),
    );

    expect(
      await screen.findByRole("heading", { name: "Contrôle thermique" }),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByRole("tab", { name: "Composants" }));

    expect(screen.getByText("Radiateur A")).toBeInTheDocument();
    expect(screen.getByText("Radiateur B")).toBeInTheDocument();
    expect(screen.getByText("Pompe principale")).toBeInTheDocument();
    expect(screen.getByText("Échangeur thermique")).toBeInTheDocument();
    expect(screen.getByText("31 °C")).toBeInTheDocument();
    expect(screen.getAllByText("CRITIQUE")).toHaveLength(2);
    expect(screen.getAllByText("État simulé")).toHaveLength(2);
    expect(
      screen.getAllByText(/État simulé depuis le 20\/10\/2026/),
    ).toHaveLength(2);
    expect(screen.getAllByText(/20\/10\/2026/)).toHaveLength(5);

    fireEvent.click(screen.getByRole("tab", { name: "Vue d’ensemble" }));
    expect(screen.getByText("Température moyenne du système")).toBeInTheDocument();
    expect(screen.getByText("26,7")).toBeInTheDocument();
    expect(
      screen.getByText("Aucun événement récent"),
    ).toBeInTheDocument();
    expect(screen.getAllByText("18–26 °C")).toHaveLength(3);
    expect(screen.getAllByText(/<15 °C.*>30 °C/)).toHaveLength(3);
    expect(screen.getByText("80–200 L/min")).toBeInTheDocument();
    expect(screen.getByText("1,5–3 bar")).toBeInTheDocument();
    expect(screen.getByText(/Jusqu’à <2 kW/)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("tab", { name: "Historique" }));
    expect(screen.getByRole("heading", { name: "Historique thermique" }))
      .toBeInTheDocument();
    expect(
      await screen.findByRole("img", {
        name: /Courbe de température en °C sur les six dernières heures/,
      }),
    ).toBeInTheDocument();
    const historyTable = screen.getByRole("table", {
      name: "Mesures thermiques des six dernières heures",
    });
    expect(
      within(historyTable).getByText("2026-10-20T11:59:50Z"),
    ).toBeInTheDocument();
    expect(within(historyTable).getByText("28,6 °C")).toBeInTheDocument();
    expect(
      screen.getByRole("tab", { name: "Maintenance" }),
    ).toBeDisabled();
    expect(
      screen.queryByRole("button", { name: "Planifier une intervention" }),
    ).not.toBeInTheDocument();
  });
});
