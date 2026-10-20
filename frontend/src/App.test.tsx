import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import App from "./App";

describe("AstroMate startup screen", () => {
  it("identifies the local simulation as fictional and non-operational", () => {
    render(<App />);

    expect(screen.getByRole("heading", { name: "AstroMate" })).toBeInTheDocument();
    expect(
      screen.getByText("Simulation — données fictives, non opérationnelles"),
    ).toBeInTheDocument();
  });
});
