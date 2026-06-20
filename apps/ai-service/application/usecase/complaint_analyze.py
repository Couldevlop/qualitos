"""Use case : analyse NLP d'un lot de réclamations clients (§4.9).

Même couture que les autres use cases : le tenant est porté pour l'auditabilité,
le calcul (sentiment + classification) est pur et sans état.
"""
from __future__ import annotations

import logging

from domain.model.complaint import ComplaintAnalysis
from domain.model.tenant import TenantContext
from domain.service import complaint_nlp

logger = logging.getLogger(__name__)


class ComplaintAnalyzeUseCase:
    """Textes de réclamations -> sentiment + catégorie + criticité par item."""

    def execute(
        self,
        texts: list[str],
        tenant: TenantContext,
        *,
        categories: dict[str, list[str]] | None = None,
        backend: str = "lexical",
    ) -> ComplaintAnalysis:
        result = complaint_nlp.analyze(texts, categories=categories, backend=backend)
        logger.info(
            "Complaint NLP tenant=%s n=%d critical=%d backend=%s",
            tenant.tenant_id, result.n, result.critical_count, backend,
        )
        return result
