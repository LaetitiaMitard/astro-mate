import { afterEach, describe, expect, it, vi } from "vitest";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import EventLog from "./EventLog";

function eventPage(page: number, totalElements = 21) {
  return {
    items: [
      {
        id: `event-${page}`,
        level: "WARNING",
        category: "DATA",
        systemId: "thermal-control",
        systemName: "Contrôle thermique",
        source: "simulation",
        message: `Événement ${page}`,
        occurredAt: "2026-10-21T12:00:00Z",
      },
    ],
    page,
    pageSize: 20,
    totalElements,
    totalPages: Math.ceil(totalElements / 20),
  };
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe("event log", () => {
  it("combines search filters and sort, and returns to the first page", async () => {
    const fetchMock = vi.fn((input: RequestInfo | URL) => {
      const requestUrl = new URL(
        typeof input === "string" ? input : input.toString(),
        window.location.origin,
      );
      return Promise.resolve(
        new Response(
          JSON.stringify(eventPage(Number(requestUrl.searchParams.get("page")))),
          {
            status: 200,
            headers: { "Content-Type": "application/json" },
          },
        ),
      );
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<EventLog />);
    expect(await screen.findByText("Événement 0")).toBeInTheDocument();
    const initialRequest = fetchMock.mock.calls[0]?.[0];
    expect(initialRequest).toBeDefined();
    const initialUrl = new URL(
      typeof initialRequest === "string"
        ? initialRequest
        : initialRequest?.toString(),
      window.location.origin,
    );
    expect(initialUrl.searchParams.get("pageSize")).toBe("20");
    expect(initialUrl.searchParams.get("sort")).toBe("occurredAt,desc");
    expect(initialUrl.searchParams.has("from")).toBe(false);
    expect(initialUrl.searchParams.has("to")).toBe(false);
    fireEvent.click(screen.getByRole("button", { name: "Suivant" }));
    expect(await screen.findByText("Événement 1")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Période début (incluse)"), {
      target: { value: "2026-10-20T12:00" },
    });
    fireEvent.change(screen.getByLabelText("Période fin (exclue)"), {
      target: { value: "2026-10-21T12:00" },
    });
    fireEvent.change(screen.getByLabelText("Niveau"), {
      target: { value: "WARNING" },
    });
    fireEvent.change(screen.getByLabelText("Catégorie"), {
      target: { value: "DATA" },
    });
    fireEvent.change(screen.getByLabelText("Recherche"), {
      target: { value: "THERMIQUE" },
    });
    fireEvent.change(screen.getByLabelText("Ordre chronologique"), {
      target: { value: "occurredAt,asc" },
    });

    await waitFor(() => {
      const request = fetchMock.mock.calls.at(-1)?.[0];
      expect(request).toBeDefined();
      if (request === undefined) {
        return;
      }
      const requestUrl = new URL(
        typeof request === "string" ? request : request.toString(),
        window.location.origin,
      );
      expect(requestUrl.searchParams.get("page")).toBe("0");
      expect(requestUrl.searchParams.get("pageSize")).toBe("20");
      expect(requestUrl.searchParams.get("from")).toBe(
        new Date("2026-10-20T12:00").toISOString(),
      );
      expect(requestUrl.searchParams.get("to")).toBe(
        new Date("2026-10-21T12:00").toISOString(),
      );
      expect(requestUrl.searchParams.get("level")).toBe("WARNING");
      expect(requestUrl.searchParams.get("category")).toBe("DATA");
      expect(requestUrl.searchParams.get("q")).toBe("THERMIQUE");
      expect(requestUrl.searchParams.get("sort")).toBe("occurredAt,asc");
    });
    expect(await screen.findByText("Événement 0")).toBeInTheDocument();
    expect(
      screen.getByRole("navigation", { name: "Pagination du journal" }),
    ).toHaveTextContent(/Page\s+1\s+sur\s+2/);
  });

  it("shows an explicit empty result after searching", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
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
    vi.stubGlobal("fetch", fetchMock);

    render(<EventLog />);
    fireEvent.change(screen.getByLabelText("Recherche"), {
      target: { value: "introuvable" },
    });

    expect(
      await screen.findByText(
        "Aucun événement technique ne correspond à ces critères.",
      ),
    ).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });
});
