"""Routers."""
from .completion_router import router as completion_router
from .rag_router import router as rag_router
from .nlq_router import router as nlq_router
from .federated_router import router as federated_router
from .health_router import router as health_router
from .spc_router import router as spc_router

__all__ = [
    "completion_router",
    "rag_router",
    "nlq_router",
    "federated_router",
    "health_router",
    "spc_router",
]
