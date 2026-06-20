"""Infrastructure adapters for the edge inference component.

Holds the *optional* and *I/O-bound* pieces, kept out of the pure domain:

- :mod:`file_store` — a durable, append-only JSONL event store for the
  store-and-forward buffer (survives restarts);
- :mod:`onnx_backend` — the real ONNX Runtime inference adapter (lazy import);
- :mod:`backend_factory` — composition root that selects ONNX-or-fallback.
"""
