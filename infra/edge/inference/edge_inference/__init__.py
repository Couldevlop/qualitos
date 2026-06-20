"""QualitOS Edge Inference component (CLAUDE.md §9.5).

Local, on-site inference at the Edge Gateway:

- a **store-and-forward** buffer (FIFO, bounded, persistent, ordered replay,
  disconnected mode) that holds telemetry-derived events until the IoT Hub is
  reachable again;
- an **inference orchestrator** that loads an ONNX model lazily (opt-in,
  `onnxruntime`) and degrades to a **deterministic threshold fallback** when no
  model is supplied — exactly the "real ONNX or deterministic fallback" pattern
  of the Vision 5S service.

The package is intentionally split into a framework-free ``domain`` layer (100%
unit-testable without heavy deps) and an ``infrastructure`` layer holding the
optional ONNX adapter and the on-disk buffer. No network I/O lives here: when an
anomaly is detected, the orchestrator merely *enqueues* a hub-bound event onto
the store-and-forward buffer. The actual mTLS push is the gateway's MQTT bridge
(see ``infra/edge/k3s``).
"""

from __future__ import annotations

__all__ = ["__version__"]

__version__ = "0.1.0"
