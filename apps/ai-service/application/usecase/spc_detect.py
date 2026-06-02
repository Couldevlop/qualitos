"""SPC detection use case: run control-chart analysis over a numeric series.

Thin application layer over the pure domain service ``spc_rules`` — kept for
symmetry with the other use cases and as the seam where cross-cutting concerns
(logging, future persistence of findings) attach. The tenant is carried for
auditability/multi-tenancy even though the computation itself is stateless.
"""
from __future__ import annotations

import logging

from domain.model.spc import SpcAnalysis
from domain.model.tenant import TenantContext
from domain.service import spc_rules

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
