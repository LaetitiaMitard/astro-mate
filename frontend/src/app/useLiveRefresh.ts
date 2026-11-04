import { useEffect, useState } from "react";

export default function useLiveRefresh() {
  const [refreshVersion, setRefreshVersion] = useState(0);

  useEffect(() => {
    let lastRefreshAt: number | null = null;
    function refresh() {
      const now = Date.now();
      if (lastRefreshAt !== null && now - lastRefreshAt < 6_000) {
        return;
      }
      lastRefreshAt = now;
      setRefreshVersion((version) => version + 1);
    }

    const interval = window.setInterval(refresh, 10_000);
    if (typeof EventSource === "undefined") {
      return () => window.clearInterval(interval);
    }

    const eventSource = new EventSource("/api/v1/telemetry/changes");
    let refreshTimeout: number | null = null;
    eventSource.onerror = eventSource.onopen = refresh;
    eventSource.onmessage = () => {
      if (refreshTimeout !== null) {
        window.clearTimeout(refreshTimeout);
      }
      refreshTimeout = window.setTimeout(refresh, 50);
    };

    return () => {
      window.clearInterval(interval);
      if (refreshTimeout !== null) {
        window.clearTimeout(refreshTimeout);
      }
      eventSource.close();
    };
  }, []);

  return refreshVersion;
}
