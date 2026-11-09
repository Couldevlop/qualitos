"""SPC detection use case: run control-chart analysis over a numeric series.

Thin application layer over the pure domain service ``spc_rules`` — kept for
symmetry with the other use cases and as the seam where cross-cutting concerns
(logging, future persistence of findings) attach. The tenant is carried for
auditability/multi-tenancy even though the computation itself is stateless.
"""
from __future__ import annotations

import logging

from domain.model.spc import SpcAnalysis, SpcBreachForecast
from domain.model.tenant import TenantContext
from domain.service import spc_forecast, spc_rules

logger = logging.getLogger(__name__)


class SpcDetectUseCase:
    """Application use case: numeric series -> SPC analysis (limits + Nelson rules)."""

    def execute(
        self,
        values: list[float],
        tenant: TenantContext,
        *,
        center: float | None = None,
        sigma: float | None = None,
    ) -> SpcAnalysis:
        analysis = spc_rules.analyze(values, center=center, sigma=sigma)
        logger.info(
            "SPC analysis tenant=%s n=%d violations=%d out_of_control=%s",
            tenant.tenant_id, analysis.n, len(analysis.violations), analysis.out_of_control,
        )
        return analysis


class SpcForecastUseCase:
    """Série de mesures → quand le procédé sortira de ses limites de contrôle.

    Même couche mince que la détection : le calcul vit dans ``spc_forecast``, et
    le client est porté pour la traçabilité même si le calcul est sans état.
    """

    def execute(
        self,
        values: list[float],
        tenant: TenantContext,
        *,
        horizon: int = 6,
        center: float | None = None,
        sigma: float | None = None,
        seasonal_period: int | None = None,
        threshold: float = 0.5,
    ) -> SpcBreachForecast:
        result = spc_forecast.forecast_breach(
            values, horizon=horizon, center=center, sigma=sigma,
            seasonal_period=seasonal_period, threshold=threshold,
        )
        logger.info(
            "SPC breach forecast tenant=%s n=%d horizon=%d first=%s side=%s confidence=%s",
            tenant.tenant_id, result.n, result.horizon,
            result.first_breach_step, result.side, result.confidence,
        )
        return result
