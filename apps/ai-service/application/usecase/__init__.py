"""Use cases."""
from .complete_text import CompleteTextUseCase, CompleteTextRequest, CompleteTextResult
from .rag_query import RagQueryUseCase, RagQueryRequest, RagQueryResult
from .nlq_ask import NlqAskUseCase, NlqAskRequest
from .federated_train_round import FederatedTrainRoundUseCase
from .spc_detect import SpcDetectUseCase

__all__ = [
    "CompleteTextUseCase",
    "CompleteTextRequest",
    "CompleteTextResult",
    "RagQueryUseCase",
    "RagQueryRequest",
    "RagQueryResult",
    "NlqAskUseCase",
    "NlqAskRequest",
    "FederatedTrainRoundUseCase",
    "SpcDetectUseCase",
]
