"""Dependency-injection container.

Pure Python DI â€” no framework. Builds use cases from infra adapters.
Tests substitute fakes/stubs.
"""
from __future__ import annotations

import os
from dataclasses import dataclass
from typing import Any

from application.usecase import (
    CompleteTextUseCase,
    FederatedTrainRoundUseCase,
    NlqAskUseCase,
    RagQueryUseCase,
)
from domain.model.completion import ProviderName
from domain.port.ai_provider import AIProvider
from domain.port.embedder import Embedder
from domain.port.federated_client import FederatedClient
from domain.port.pii_filter import PiiFilter
from domain.port.prompt_audit_logger import PromptAuditLogger
from domain.port.prompt_injection_filter import PromptInjectionFilter
from domain.port.sql_executor import ReadOnlySqlExecutor
from domain.port.sql_validator import SqlValidator
from domain.port.vector_store import VectorStore
from federated.opt_in_federated_client import OptInFederatedClient
from infrastructure.guardrails import (
    HeuristicInjectionFilter,
    PresidioPiiFilter,
)
from infrastructure.nlq import (
    InMemoryReadOnlyExecutor,
    JdbcReadOnlyExecutor,
    SqlglotValidator,
)
from infrastructure.persistence import JsonPromptAuditLogger
from infrastructure.providers import (
    AnthropicProvider,
    MistralProvider,
    OllamaProvider,
)
from infrastructure.vector import (
    BgeM3Embedder,
    InMemoryVectorStore,
    QdrantVectorStore,
)


@dataclass(slots=True)
class Container:
    providers: dict[ProviderName, AIProvider]
    pii_filter: PiiFilter
    injection_filter: PromptInjectionFilter
    audit_logger: PromptAuditLogger
    vector_store: VectorStore
    embedder: Embedder
    sql_validator: SqlValidator
    sql_executor: ReadOnlySqlExecutor
    federated_client: FederatedClient

    @classmethod
    def build_default(cls, overrides: dict[str, Any] | None = None) -> "Container":
        o = overrides or {}
        providers: dict[ProviderName, AIProvider] = o.get("providers") or {
            # Modèle/endpoint Ollama configurables par env (cf. ADR 0014) — permet un
            # petit modèle en dev (llama3.2:1b) tenant dans le timeout CPU.
            ProviderName.OLLAMA: OllamaProvider(
                base_url=os.environ.get("OLLAMA_BASE_URL", "http://ollama:11434"),
                model=os.environ.get("OLLAMA_MODEL", "llama3.1:8b"),
                # Timeout large : l'inférence CPU + chargement à froid du modèle dépasse 30 s.
                timeout_s=float(os.environ.get("OLLAMA_TIMEOUT_S", "120")),
            ),
            ProviderName.ANTHROPIC: AnthropicProvider(),
            ProviderName.MISTRAL: MistralProvider(),
        }
        pii_filter = o.get("pii_filter") or PresidioPiiFilter()
        injection_filter = o.get("injection_filter") or HeuristicInjectionFilter()
        audit_logger = o.get("audit_logger") or JsonPromptAuditLogger()

        vector_store: VectorStore = o.get("vector_store") or (
            QdrantVectorStore() if os.environ.get("QDRANT_URL") else InMemoryVectorStore()
        )
        embedder = o.get("embedder") or BgeM3Embedder()
        sql_validator: SqlValidator = o.get("sql_validator") or SqlglotValidator()
        sql_executor: ReadOnlySqlExecutor = o.get("sql_executor") or (
            JdbcReadOnlyExecutor()
            if os.environ.get("NLQ_READONLY_DSN")
            else InMemoryReadOnlyExecutor()
        )
        federated_client = o.get("federated_client") or OptInFederatedClient()

        return cls(
            providers=providers,
            pii_filter=pii_filter,
            injection_filter=injection_filter,
            audit_logger=audit_logger,
            vector_store=vector_store,
            embedder=embedder,
            sql_validator=sql_validator,
            sql_executor=sql_executor,
            federated_client=federated_client,
        )

    def complete_text(self) -> CompleteTextUseCase:
        return CompleteTextUseCase(
            self.providers, self.pii_filter, self.injection_filter, self.audit_logger
        )

    def rag_query(self) -> RagQueryUseCase:
        return RagQueryUseCase(
            self.vector_store,
            self.embedder,
            self.providers,
            self.pii_filter,
            self.injection_filter,
            self.audit_logger,
        )

    def nlq_ask(self) -> NlqAskUseCase:
        return NlqAskUseCase(
            self.providers,
            self.sql_validator,
            self.sql_executor,
            self.pii_filter,
            self.injection_filter,
            self.audit_logger,
        )

    def federated_round(self) -> FederatedTrainRoundUseCase:
        return FederatedTrainRoundUseCase(self.federated_client)
