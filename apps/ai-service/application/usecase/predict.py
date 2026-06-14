"""Predictive use cases — thin application layer over the pure domain services.

Même couture que ``spc_detect`` : le tenant est porté pour l'auditabilité,
le calcul lui-même est sans état (CLAUDE.md §6.5, §12.3 — explicable).
"""
from __future__ import annotations

import logging

from domain.model.predict import KpiForecast, NcClusteringResult, SupplierRiskScore
from domain.model.tenant import TenantContext
from domain.service import forecasting, nc_clustering, supplier_scoring

logger = logging.getLogger(__name__)


class KpiForecastUseCase:
    """Série de mesures KPI → prévision + probabilité d'atteinte de la cible."""

    def execute(
        self,
        values: list[float],
        target: float,
        tenant: TenantContext,
        *,
        horizon: int = 6,
        direction: str = "at_least",
        seasonal_period: int | None = None,
    ) -> KpiForecast:
        result = forecasting.forecast(
            values, target, horizon=horizon, direction=direction,
            seasonal_period=seasonal_period,
        )
        logger.info(
            "KPI forecast tenant=%s n=%d horizon=%d model=%s p=%.2f confidence=%s",
            tenant.tenant_id, result.n, result.horizon, result.model,
            result.probability, result.confidence,
        )
        return result


class SupplierRiskUseCase:
    """Features fournisseur → score de risque 0-100 + drivers explicables."""

    def execute(self, features: dict[str, float], tenant: TenantContext) -> SupplierRiskScore:
        result = supplier_scoring.score(features)
        logger.info(
            "Supplier risk tenant=%s score=%.1f level=%s drivers=%d",
            tenant.tenant_id, result.score, result.level, len(result.drivers),
        )
        return result


class NcClusterUseCase:
    """Textes de NC → clusters de similarité (patterns récurrents)."""

    def execute(
        self, texts: list[str], tenant: TenantContext, *, threshold: float = 0.35
    ) -> NcClusteringResult:
        result = nc_clustering.cluster(texts, threshold=threshold)
        logger.info(
            "NC clustering tenant=%s n=%d clusters=%d noise=%d",
            tenant.tenant_id, result.n, len(result.clusters), len(result.noise_indices),
        )
        return result
