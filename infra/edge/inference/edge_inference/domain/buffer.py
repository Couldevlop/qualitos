"""Store-and-forward buffer — the resilience core of the edge component.

Behaviour (CLAUDE.md §9.5, mirrors the mosquitto bridge queue but for
application-level hub events):

- **FIFO** ordered replay — events leave in the exact order they were enqueued.
- **Bounded** — a maximum capacity protects the constrained edge appliance
  (512 Mo RAM mini). The eviction policy on overflow is configurable:
  ``DROP_OLDEST`` (default, keep the freshest telemetry) or ``REJECT_NEW``.
- **Disconnected mode** — while ``connected`` is false, events accumulate; when
  connectivity returns they are replayed in order via :meth:`drain`.
- **Persistence** — an optional :class:`EventStore` port lets the queue survive
  process restarts (the file adapter lives in ``infrastructure``). The default
  is a pure in-memory store, so the buffer is fully testable with zero I/O.

This module is stdlib-only and has no knowledge of MQTT, ONNX or the network.
"""

from __future__ import annotations

from collections import deque
from collections.abc import Callable, Iterable
from dataclasses import asdict
from enum import Enum
from typing import Protocol

from .models import HubEvent, Severity


class OverflowPolicy(str, Enum):
    DROP_OLDEST = "drop_oldest"
    REJECT_NEW = "reject_new"


class BufferFull(RuntimeError):
    """Raised on enqueue when the buffer is full and policy is REJECT_NEW."""


class EventStore(Protocol):
    """Persistence port for the buffer (hexagonal — adapter in infrastructure).

    Implementations must preserve insertion order. ``load`` is called once at
    construction to restore a previous session; the mutating methods keep the
    durable store in sync with the in-memory deque.
    """

    def load(self) -> list[HubEvent]: ...

    def append(self, event: HubEvent) -> None: ...

    def pop_left(self) -> None: ...

    def replace(self, events: Iterable[HubEvent]) -> None: ...


class InMemoryEventStore:
    """Default, non-durable store. Pure data structure, no I/O."""

    def __init__(self, initial: Iterable[HubEvent] | None = None) -> None:
        self._events: list[HubEvent] = list(initial or [])

    def load(self) -> list[HubEvent]:
        return list(self._events)

    def append(self, event: HubEvent) -> None:
        self._events.append(event)

    def pop_left(self) -> None:
        if self._events:
            self._events.pop(0)

    def replace(self, events: Iterable[HubEvent]) -> None:
        self._events = list(events)


class StoreAndForwardBuffer:
    """Bounded, ordered, persistable queue of hub-bound events."""

    def __init__(
        self,
        capacity: int = 100_000,
        *,
        store: EventStore | None = None,
        overflow_policy: OverflowPolicy = OverflowPolicy.DROP_OLDEST,
        connected: bool = True,
    ) -> None:
        if capacity <= 0:
            raise ValueError("capacity must be positive")
        self._capacity = capacity
        self._policy = overflow_policy
        self._store = store if store is not None else InMemoryEventStore()
        self._connected = connected
        self._dropped = 0
        # Restore any persisted backlog, oldest first, honouring the bound.
        restored = self._store.load()
        if len(restored) > capacity:
            # Keep the freshest `capacity` events; record the rest as dropped.
            self._dropped += len(restored) - capacity
            restored = restored[-capacity:]
            self._store.replace(restored)
        self._queue: deque[HubEvent] = deque(restored)

    # -- properties ---------------------------------------------------------

    @property
    def connected(self) -> bool:
        return self._connected

    @property
    def capacity(self) -> int:
        return self._capacity

    @property
    def dropped_count(self) -> int:
        """Total events evicted by the overflow policy since construction."""
        return self._dropped

    def __len__(self) -> int:
        return len(self._queue)

    def is_empty(self) -> bool:
        return not self._queue

    def is_full(self) -> bool:
        return len(self._queue) >= self._capacity

    # -- connection state ---------------------------------------------------

    def set_connected(self, connected: bool) -> None:
        """Flip the link state. Going offline only accumulates; going online
        does NOT auto-flush — callers decide when to :meth:`drain`."""
        self._connected = connected

    # -- enqueue ------------------------------------------------------------

    def enqueue(self, event: HubEvent) -> bool:
        """Append an event. Returns True if stored, False if dropped.

        On overflow: DROP_OLDEST evicts the head (and the new event is kept);
        REJECT_NEW raises :class:`BufferFull`.
        """
        if self.is_full():
            if self._policy is OverflowPolicy.REJECT_NEW:
                raise BufferFull(
                    f"buffer full (capacity={self._capacity}), event rejected"
                )
            # DROP_OLDEST: evict head to make room.
            self._queue.popleft()
            self._store.pop_left()
            self._dropped += 1
        self._queue.append(event)
        self._store.append(event)
        return True

    # -- drain (ordered replay) --------------------------------------------

    def peek(self) -> HubEvent | None:
        return self._queue[0] if self._queue else None

    def drain(
        self,
        sender: Callable[[HubEvent], bool],
        *,
        max_events: int | None = None,
    ) -> int:
        """Replay queued events in FIFO order through ``sender``.

        ``sender`` returns True on successful delivery (event removed) or False
        to stop draining and KEEP the event at the head (e.g. the link dropped
        mid-flush). Returns the number of events successfully sent.

        Does nothing while disconnected — store-and-forward only flushes when
        the link is up.
        """
        if not self._connected:
            return 0
        sent = 0
        while self._queue and (max_events is None or sent < max_events):
            head = self._queue[0]
            try:
                ok = sender(head)
            except Exception:
                # Delivery failure must not lose the event — stop and keep it.
                break
            if not ok:
                break
            self._queue.popleft()
            self._store.pop_left()
            sent += 1
        return sent

    # -- serialisation helpers (used by the file store adapter) ------------

    @staticmethod
    def event_to_dict(event: HubEvent) -> dict:
        data = asdict(event)
        # Enum -> str for JSON friendliness.
        data["severity"] = event.severity.value
        return data

    @staticmethod
    def event_from_dict(data: dict) -> HubEvent:
        return HubEvent(
            event_type=data["event_type"],
            device_id=data["device_id"],
            metric=data["metric"],
            score=float(data["score"]),
            severity=Severity(data["severity"]),
            backend=data["backend"],
            reason=data["reason"],
            timestamp_ms=int(data["timestamp_ms"]),
        )
