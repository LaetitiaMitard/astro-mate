import { useEffect, useRef, useState, type MouseEvent } from "react";
import "../App.css";
import {
  getDashboard,
  getSystemDetail,
} from "../shared/api/client";
import type { Dashboard, SystemDetail } from "../shared/api/models";
import { statusLabels } from "../shared/domain/systems";
import { navigationItems } from "../shared/navigation";
import AlertLog from "../features/alerts/AlertLog";
import DashboardPage from "../features/dashboard/DashboardPage";
import EventLog from "../features/events/EventLog";
import SystemsPage from "../features/systems/SystemsPage";
import useLiveRefresh from "./useLiveRefresh";

function DashboardClock() {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const interval = window.setInterval(() => setNow(new Date()), 30_000);
    return () => window.clearInterval(interval);
  }, []);

  return (
    <time className="dashboard-clock" dateTime={now.toISOString()}>
      <span>
        {new Intl.DateTimeFormat("fr-FR", { dateStyle: "full" }).format(now)}
      </span>
      <strong>
        {new Intl.DateTimeFormat("fr-FR", {
          hour: "2-digit",
          minute: "2-digit",
          timeZoneName: "short",
        }).format(now)}
      </strong>
    </time>
  );
}

export default function App() {
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [loadState, setLoadState] = useState<"loading" | "loaded" | "error">(
    "loading",
  );
  const [pathname, setPathname] = useState(window.location.pathname);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const menuButtonRef = useRef<HTMLButtonElement>(null);
  const navigationRef = useRef<HTMLElement>(null);
  const contentRef = useRef<HTMLElement>(null);
  const refreshVersion = useLiveRefresh();
  const [systemDetail, setSystemDetail] = useState<SystemDetail | null>(null);
  const [detailLoadState, setDetailLoadState] = useState<
    "loading" | "loaded" | "error"
  >("loading");

  const isEventLogRoute = pathname === "/events";
  const isAlertLogRoute = pathname === "/alerts";
  const isHistoryRoute = isEventLogRoute || isAlertLogRoute;
  const selectedSystemId = pathname.match(/^\/systems\/([^/]+)\/?$/)?.[1];
  const isSystemRoute = pathname === "/systems" || selectedSystemId !== undefined;

  useEffect(() => {
    if (isHistoryRoute) {
      return;
    }

    const controller = new AbortController();
    getDashboard(controller.signal)
      .then((result) => {
        setDashboard(result);
        setLoadState("loaded");
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setLoadState("error");
        }
      });

    return () => controller.abort();
  }, [isHistoryRoute, refreshVersion]);

  useEffect(() => {
    if (selectedSystemId !== "thermal-control") {
      return;
    }

    const controller = new AbortController();
    setDetailLoadState((state) => (state === "loaded" ? state : "loading"));
    getSystemDetail(selectedSystemId, controller.signal)
      .then((result) => {
        setSystemDetail(result);
        setDetailLoadState("loaded");
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setDetailLoadState("error");
        }
      });

    return () => controller.abort();
  }, [selectedSystemId, refreshVersion]);

  useEffect(() => {
    const onPopState = () => {
      if (navigationRef.current?.contains(document.activeElement)) {
        contentRef.current?.focus();
      }
      setIsMenuOpen(false);
      setPathname(window.location.pathname);
    };
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function navigate(event: MouseEvent<HTMLAnchorElement>) {
    if (
      event.button !== 0 ||
      event.metaKey ||
      event.ctrlKey ||
      event.shiftKey ||
      event.altKey
    ) {
      return;
    }
    event.preventDefault();
    const nextPath = event.currentTarget.pathname;
    window.history.pushState(null, "", nextPath);
    setPathname(nextPath);
    if (isMenuOpen) {
      contentRef.current?.focus();
    }
    setIsMenuOpen(false);
  }

  return (
    <div className="app">
      <aside
        className="sidebar"
        onKeyDown={(event) => {
          if (event.key === "Escape" && isMenuOpen) {
            setIsMenuOpen(false);
            menuButtonRef.current?.focus();
          }
        }}
      >
        <div className="sidebar-header">
          <div className="sidebar-brand">
            <svg
              className="brand-icon"
              viewBox="0 0 48 48"
              fill="none"
              aria-hidden="true"
            >
              <circle cx="24" cy="24" r="15" />
              <ellipse
                cx="24"
                cy="24"
                rx="26"
                ry="8"
                transform="rotate(-40 24 24)"
              />
              <path d="m18 26 9-10-2 12-9 5z" />
            </svg>
            <div>
              <h1>
                Astro<span>Mate</span>
              </h1>
              <p>
                Une station plus sûre,
                <br />
                ensemble
              </p>
            </div>
          </div>
          <button
            ref={menuButtonRef}
            className="menu-toggle"
            type="button"
            aria-expanded={isMenuOpen}
            aria-controls="main-navigation"
            onClick={() => setIsMenuOpen((open) => !open)}
          >
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path
                d={
                  isMenuOpen
                    ? "m6 6 12 12M6 18 18 6"
                    : "M4 6h16M4 12h16M4 18h16"
                }
              />
            </svg>
            Menu
          </button>
        </div>
        <nav
          ref={navigationRef}
          id="main-navigation"
          className={`main-navigation${isMenuOpen ? " is-open" : ""}`}
          aria-label="Navigation principale"
        >
          {navigationItems.map((item) => (
            <a
              key={item.href}
              href={item.href}
              onClick={navigate}
              aria-current={
                (item.href === "/systems"
                  ? isSystemRoute
                  : pathname === item.href)
                  ? "page"
                  : undefined
              }
            >
              <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d={item.icon} />
              </svg>
              {item.label}
            </a>
          ))}
        </nav>
      </aside>
      <main className="app-content" ref={contentRef} tabIndex={-1}>
        <header className="page-header">
          {!isHistoryRoute && !isSystemRoute ? (
            <div className="dashboard-greeting">
              <h2>Bonjour !</h2>
              <p>Voici l’état actuel de la station spatiale.</p>
            </div>
          ) : (
            <p className="eyebrow">CENTRE DE CONTRÔLE · STATION UNIQUE</p>
          )}
          <DashboardClock />
        </header>
        <p className="notice">
          Simulation — données fictives, non opérationnelles
        </p>

        {isEventLogRoute && <EventLog refreshVersion={refreshVersion} />}
        {isAlertLogRoute && <AlertLog refreshVersion={refreshVersion} />}

        {!isHistoryRoute && loadState === "loading" && (
          <p className="loading" role="status">
            Lecture de la simulation…
          </p>
        )}

        {!isHistoryRoute && loadState === "error" && (
          <section className="error-panel" role="alert">
            <p className="eyebrow">LECTURE IMPOSSIBLE</p>
            <h2>Tableau de bord indisponible</h2>
            <p>
              Simulation indisponible. Impossible de lire l’API ou la base
              locale. Aucune mesure conservée n’est présentée comme courante.
            </p>
          </section>
        )}

        {!isHistoryRoute && loadState === "loaded" && dashboard && (
          isSystemRoute ? (
            <SystemsPage
              pathname={pathname}
              dashboard={dashboard}
              systemDetail={systemDetail}
              detailLoadState={detailLoadState}
              refreshVersion={refreshVersion}
              onNavigate={navigate}
            />
          ) : (
            <DashboardPage dashboard={dashboard} onNavigate={navigate} />
          )
        )}
      </main>
    </div>
  );
}
