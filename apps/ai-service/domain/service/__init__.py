"""Pure domain services — no I/O."""
from .narrative_builder import NarrativeBuilder
from .chart_inferrer import ChartInferrer

__all__ = ["NarrativeBuilder", "ChartInferrer"]
