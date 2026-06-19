"""Durable JSONL event store for the store-and-forward buffer.

Implements the :class:`edge_inference.domain.buffer.EventStore` port with a
simple append-only JSON-lines file so the backlog survives a process/appliance
restart (the gateway may reboot during a long WAN outage).

Design notes:
- **Append on enqueue** is O(1) (a single line write + flush).
- **pop_left / replace** rewrite the file. They are O(n); on a constrained edge
  node the backlog is bounded, and these are called on drain (link up) where the
  cost is acceptable. Keeping it dead simple beats a fragile index file.
- Lines that fail to parse on load are skipped (defensive against a torn final
  write after a power loss) rather than crashing the gateway at boot.

Pure stdlib — no third-party dependency.
"""

from __future__ import annotations

import json
import logging
import os
import tempfile
from pathlib import Path

from edge_inference.domain.buffer import StoreAndForwardBuffer
from edge_inference.domain.models import HubEvent

_LOG = logging.getLogger(__name__)


class JsonlFileEventStore:
    """File-backed, order-preserving event store."""

    def __init__(self, path: str | os.PathLike[str]) -> None:
        self._path = Path(path)
        self._path.parent.mkdir(parents=True, exist_ok=True)
        if not self._path.exists():
            self._path.touch()

    @property
    def path(self) -> Path:
        return self._path

    def load(self) -> list[HubEvent]:
        events: list[HubEvent] = []
        with self._path.open("r", encoding="utf-8") as fh:
            for lineno, raw in enumerate(fh, start=1):
                line = raw.strip()
                if not line:
                    continue
                try:
                    events.append(
                        StoreAndForwardBuffer.event_from_dict(json.loads(line))
                    )
                except (json.JSONDecodeError, KeyError, ValueError) as exc:
                    # Tolerate a torn last line (power loss mid-write).
                    _LOG.warning(
                        "file_store skipping corrupt line %d in %s: %s",
                        lineno, self._path, exc,
                    )
        return events

    def append(self, event: HubEvent) -> None:
        line = json.dumps(
            StoreAndForwardBuffer.event_to_dict(event),
            separators=(",", ":"),
            ensure_ascii=False,
        )
        with self._path.open("a", encoding="utf-8") as fh:
            fh.write(line + "\n")
            fh.flush()
            os.fsync(fh.fileno())

    def pop_left(self) -> None:
        events = self.load()
        if not events:
            return
        self.replace(events[1:])

    def replace(self, events) -> None:
        # Atomic rewrite: write to a temp file in the same dir, then replace.
        events = list(events)
        fd, tmp = tempfile.mkstemp(
            dir=str(self._path.parent), prefix=self._path.name, suffix=".tmp"
        )
        try:
            with os.fdopen(fd, "w", encoding="utf-8") as fh:
                for event in events:
                    line = json.dumps(
                        StoreAndForwardBuffer.event_to_dict(event),
                        separators=(",", ":"),
                        ensure_ascii=False,
                    )
                    fh.write(line + "\n")
                fh.flush()
                os.fsync(fh.fileno())
            os.replace(tmp, self._path)
        except Exception:
            # Best-effort cleanup of the temp file on failure.
            try:
                os.unlink(tmp)
            except OSError:  # pragma: no cover - cleanup guard
                pass
            raise
