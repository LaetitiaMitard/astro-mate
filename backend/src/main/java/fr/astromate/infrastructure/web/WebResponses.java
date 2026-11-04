package fr.astromate.infrastructure.web;

import fr.astromate.application.model.AlertPageView;
import fr.astromate.application.model.DashboardView;
import fr.astromate.application.model.EventLogView;
import fr.astromate.application.model.MeasurementSeriesView;
import fr.astromate.application.model.SystemDetailView;
import fr.astromate.application.model.SystemSummaryView;

public final class WebResponses {
    private WebResponses() {
    }

    public static EventLogResponse from(EventLogView value) {
        return new EventLogResponse(
                value.items().stream().map(WebResponses::from).toList(),
                value.page(),
                value.pageSize(),
                value.totalElements(),
                value.totalPages()
        );
    }

    public static DashboardResponse from(DashboardView value) {
        return new DashboardResponse(
                value.stationStatus(),
                value.offlineSystems(),
                value.systemStatuses().stream().map(WebResponses::from).toList(),
                value.simulationStatus(),
                value.lastMeasurementAt(),
                value.indicators().stream().map(WebResponses::from).toList(),
                value.recentOpenAlerts().stream().map(WebResponses::from).toList()
        );
    }

    public static DashboardResponse.SystemState from(DashboardView.SystemState value) {
        return new DashboardResponse.SystemState(
                value.systemId(),
                value.status()
        );
    }

    public static DashboardResponse.Indicator from(DashboardView.Indicator value) {
        return new DashboardResponse.Indicator(
                value.key(),
                value.systemId(),
                value.sourceMetricIds(),
                value.name(),
                value.value(),
                value.unit(),
                value.status(),
                from(value.nominalRange()),
                value.measuredAt()
        );
    }

    public static DashboardResponse.Range from(DashboardView.Range value) {
        return new DashboardResponse.Range(
                value.min(),
                value.max(),
                value.minInclusive(),
                value.maxInclusive()
        );
    }

    public static DashboardResponse.Alert from(DashboardView.Alert value) {
        return new DashboardResponse.Alert(
                value.id(),
                value.level(),
                value.systemId(),
                value.systemName(),
                value.metricId(),
                value.message(),
                value.status(),
                value.detectedAt(),
                value.resolvedAt()
        );
    }

    public static SystemSummaryResponse from(SystemSummaryView value) {
        return new SystemSummaryResponse(
                value.id(),
                value.name(),
                value.description(),
                value.status(),
                value.availability(),
                value.updatedAt()
        );
    }

    public static SystemDetailResponse from(SystemDetailView value) {
        return new SystemDetailResponse(
                value.id(),
                value.name(),
                value.description(),
                value.status(),
                value.availability(),
                value.updatedAt(),
                value.indicators().stream().map(WebResponses::from).toList(),
                value.metrics().stream().map(WebResponses::from).toList(),
                value.components().stream().map(WebResponses::from).toList(),
                value.recentEvents().stream().map(WebResponses::from).toList()
        );
    }

    public static SystemDetailResponse.Metric from(SystemDetailView.Metric value) {
        return new SystemDetailResponse.Metric(
                value.id(),
                value.name(),
                value.unit(),
                value.value(),
                value.status(),
                from(value.nominalRange()),
                from(value.criticalLimits()),
                value.measuredAt(),
                value.componentId()
        );
    }

    public static SystemDetailResponse.CriticalLimits from(SystemDetailView.CriticalLimits value) {
        return new SystemDetailResponse.CriticalLimits(
                value.minExclusive(),
                value.maxExclusive()
        );
    }

    public static SystemDetailResponse.Component from(SystemDetailView.Component value) {
        return new SystemDetailResponse.Component(
                value.id(),
                value.name(),
                value.status(),
                value.value(),
                value.unit(),
                value.updatedAt(),
                value.simulated()
        );
    }

    public static SystemDetailResponse.Event from(SystemDetailView.Event value) {
        return new SystemDetailResponse.Event(
                value.id(),
                value.level(),
                value.category(),
                value.systemId(),
                value.systemName(),
                value.source(),
                value.message(),
                value.occurredAt()
        );
    }

    public static MeasurementSeriesResponse from(MeasurementSeriesView value) {
        return new MeasurementSeriesResponse(
                value.indicatorKey(),
                value.sourceMetricIds(),
                value.name(),
                value.unit(),
                value.from(),
                value.to(),
                value.measurements().stream().map(WebResponses::from).toList()
        );
    }

    public static MeasurementSeriesResponse.Measurement from(MeasurementSeriesView.Measurement value) {
        return new MeasurementSeriesResponse.Measurement(
                value.value(),
                value.measuredAt()
        );
    }

    public static AlertPageResponse from(AlertPageView value) {
        return new AlertPageResponse(
                value.items().stream().map(WebResponses::from).toList(),
                value.page(),
                value.pageSize(),
                value.totalElements(),
                value.totalPages()
        );
    }

}
