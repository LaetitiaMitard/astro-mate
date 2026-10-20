import type { Dashboard } from "./dashboardApi";

export function formatValue(value: number | null) {
  if (value === null) {
    return "Inconnue";
  }

  return new Intl.NumberFormat("fr-FR", { maximumFractionDigits: 1 }).format(
    value,
  );
}

export function formatRange(
  range: Dashboard["indicators"][number]["nominalRange"],
  unit: string,
) {
  const min =
    range.min === null
      ? null
      : `${range.minInclusive ? "" : ">"}${formatValue(range.min)}`;
  const max =
    range.max === null
      ? null
      : `${range.maxInclusive ? "" : "<"}${formatValue(range.max)}`;

  if (min === null) {
    return `Jusqu’à ${max} ${unit}`;
  }
  if (max === null) {
    return `À partir de ${min} ${unit}`;
  }

  return `${min}–${max} ${unit}`;
}

export function formatMeasurementDate(value: string | null) {
  if (value === null || !Number.isFinite(Date.parse(value))) {
    return "Aucune mesure valide";
  }

  return new Intl.DateTimeFormat("fr-FR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}
