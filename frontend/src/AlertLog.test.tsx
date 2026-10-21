import { afterEach, describe, expect, it, vi } from "vitest";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import AlertLog from "./AlertLog";

function alert(
  id: string,
  status: "OPEN" | "RESOLVED",
  detectedAt: string,
) {
  return {
    id,
    level: "WARNING",
    systemId: "thermal-control",
    systemName: "Contrôle thermique",
    metricId: "radiator-a-temperature",
    message: id,
    status,
    detectedAt,
    resolvedAt: status === "OPEN" ? null : detectedAt,
  };
}

function alertPage(
  items: ReturnType<typeof alert>[],
  page: number,
  totalElements: number,
  totalPages: number,
) {
  return { items, page, pageSize: 2, totalElements, totalPages };
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe("alert log", () => {
  it("submits level and system filters independently", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(alertPage([], 0, 0, 0)), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    render(<AlertLog />);
    expect(
      await screen.findByText("Aucune alerte pour ces filtres."),
    ).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Niveau"), {
      target: { value: "WARNING" },
    });
    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/alerts?page=0&level=WARNING",
        expect.objectContaining({ signal: expect.any(AbortSignal) }),
      ),
    );

    fireEvent.change(screen.getByLabelText("Niveau"), {
      target: { value: "" },
    });
    fireEvent.change(screen.getByLabelText("Système"), {
      target: { value: "thermal-control" },
    });
    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/alerts?page=0&systemId=thermal-control",
        expect.objectContaining({ signal: expect.any(AbortSignal) }),
      ),
    );
  });

  it("combines level and system filters and preserves filtered pagination", async () => {
    const fetchMock = vi.fn((input: RequestInfo | URL) => {
      const requestUrl = new URL(
        typeof input === "string" ? input : input.toString(),
        window.location.origin,
      );
      const page = Number(requestUrl.searchParams.get("page"));
      const filtered =
        requestUrl.searchParams.get("level") === "WARNING" &&
        requestUrl.searchParams.get("systemId") === "thermal-control";
      const payload = filtered
        ? page === 0
          ? alertPage(
              [
                alert("open-warning", "OPEN", "2026-10-21T12:00:00Z"),
                alert("resolved-warning-new", "RESOLVED", "2026-10-20T12:00:00Z"),
              ],
              0,
              3,
              2,
            )
          : alertPage(
              [alert("resolved-warning-old", "RESOLVED", "2026-10-19T12:00:00Z")],
              1,
              3,
              2,
            )
        : alertPage([alert(`unfiltered-${page}`, "OPEN", "2026-10-21T12:00:00Z")], page, 21, 11);

      return Promise.resolve(
        new Response(JSON.stringify(payload), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      );
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<AlertLog />);
    expect(await screen.findByText("unfiltered-0")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Suivant" }));
    expect(await screen.findByText("unfiltered-1")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Niveau"), {
      target: { value: "WARNING" },
    });
    fireEvent.change(screen.getByLabelText("Système"), {
      target: { value: "thermal-control" },
    });

    expect(await screen.findByText("resolved-warning-new")).toBeInTheDocument();
    const table = screen.getByRole("table", { name: "Épisodes d’alerte" });
    const rows = within(table).getAllByRole("row");
    expect(within(rows[1]).getByText("open-warning")).toBeInTheDocument();
    expect(within(rows[2]).getByText("resolved-warning-new")).toBeInTheDocument();
    expect(
      screen.getByRole("navigation", { name: "Pagination des alertes" }),
    ).toHaveTextContent(/Page\s+1\s+sur\s+2/);
    expect(screen.getByRole("navigation", { name: "Pagination des alertes" }))
      .toHaveTextContent("3 alertes");

    fireEvent.click(screen.getByRole("button", { name: "Suivant" }));
    expect(await screen.findByText("resolved-warning-old")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/alerts?page=1&level=WARNING&systemId=thermal-control",
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    );
    expect(
      screen.getByRole("navigation", { name: "Pagination des alertes" }),
    ).toHaveTextContent(/Page\s+2\s+sur\s+2/);
  });

  it("explains when filters return no alert episodes", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(alertPage([], 0, 0, 0)), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );

    render(<AlertLog />);

    expect(
      await screen.findByText("Aucune alerte pour ces filtres."),
    ).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
    expect(
      screen.queryByRole("navigation", { name: "Pagination des alertes" }),
    ).not.toBeInTheDocument();
  });

  it("shows an explicit message when the alert API fails", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ title: "Service indisponible" }), {
          status: 503,
          headers: { "Content-Type": "application/problem+json" },
        }),
      ),
    );

    render(<AlertLog />);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Alertes indisponibles",
    );
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });
});
