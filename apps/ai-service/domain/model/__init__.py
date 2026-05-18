"""Domain entities and value objects — pure Python, zero framework imports."""
from .completion import (
    CompletionRequest,
    CompletionResponse,
    ProviderName,
    Citation,
    Confidence,
)
from .rag import RagDocument, RagQuery, RagResult
from .nlq import NlqQuestion, NlqAnswer, GeneratedSql, ChartSpec
from .tenant import TenantContext, UserContext
from .errors import (
    DomainError,
    PiiViolationError,
    PromptInjectionError,
    UnsafeSqlError,
    ProviderUnavailableError,
)

__all__ = [
    "CompletionRequest",
    "CompletionResponse",
    "ProviderName",
    "Citation",
    "Confidence",
    "RagDocument",
    "RagQuery",
    "RagResult",
    "NlqQuestion",
    "NlqAnswer",
    "GeneratedSql",
    "ChartSpec",
    "TenantContext",
    "UserContext",
    "DomainError",
    "PiiViolationError",
    "PromptInjectionError",
    "UnsafeSqlError",
    "ProviderUnavailableError",
]
