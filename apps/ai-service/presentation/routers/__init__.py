"""Routers."""
from .completion_router import router as completion_router
from .rag_router import router as rag_router
from .nlq_router import router as nlq_router
from .federated_router import router as federated_router
from .health_router import router as health_router
from .predict_router import router as predict_router
from .spc_router import router as spc_router
from .anomaly_router import router as anomaly_router

__all__ = [
    "anomaly_router",
    "predict_router",
    "completion_router",
    "rag_router",
    "nlq_router",
    "federated_router",
    "health_router",
    "spc_router",
    "anomaly_router",
]
