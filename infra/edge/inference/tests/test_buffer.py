"""Tests for the store-and-forward buffer (FIFO, bound, replay, disconnected)."""

from __future__ import annotations

import pytest

from edge_inference.domain.buffer import (
    BufferFull,
    InMemoryEventStore,
    OverflowPolicy,
    StoreAndForwardBuffer,
)
from edge_inference.domain.models import HubEvent, Severity


def _event(i: int) -> HubEvent:
    return HubEvent(
        event_type="iot.anomaly.detected",
        device_id=f"DEV-{i}",
        metric="temperature",
        score=0.9,
        severity=Severity.CRITICAL,
        backend="fallback",
        reason="above-high",
        timestamp_ms=1_000 + i,
    )


def test_fifo_order_on_drain():
    buf = StoreAndForwardBuffer(capacity=10)
    for i in range(5):
        buf.enqueue(_event(i))

    sent: list[HubEvent] = []
    n = buf.drain(lambda e: (sent.append(e), True)[1])

    assert n == 5
    assert buf.is_empty()
    assert [e.device_id for e in sent] == [f"DEV-{i}" for i in range(5)]


def test_capacity_must_be_positive():
    with pytest.raises(ValueError):
        StoreAndForwardBuffer(capacity=0)


def test_bound_drop_oldest():
    buf = StoreAndForwardBuffer(capacity=3, overflow_policy=OverflowPolicy.DROP_OLDEST)
    for i in range(5):
        buf.enqueue(_event(i))

    assert len(buf) == 3
    assert buf.dropped_count == 2
    sent: list[HubEvent] = []
    buf.drain(lambda e: (sent.append(e), True)[1])
    # Oldest two (0,1) evicted, freshest kept in order.
    assert [e.device_id for e in sent] == ["DEV-2", "DEV-3", "DEV-4"]


def test_bound_reject_new_raises_and_keeps_existing():
    buf = StoreAndForwardBuffer(capacity=2, overflow_policy=OverflowPolicy.REJECT_NEW)
    buf.enqueue(_event(0))
    buf.enqueue(_event(1))
    assert buf.is_full()
    with pytest.raises(BufferFull):
        buf.enqueue(_event(2))
    assert len(buf) == 2  # existing untouched


def test_disconnected_accumulates_then_syncs_on_reconnect():
    buf = StoreAndForwardBuffer(capacity=10, connected=False)
    for i in range(3):
        buf.enqueue(_event(i))

    # While offline, drain is a no-op even if a sender is provided.
    assert buf.drain(lambda e: True) == 0
    assert len(buf) == 3

    # Link returns -> flush in order.
    buf.set_connected(True)
    sent: list[HubEvent] = []
    assert buf.drain(lambda e: (sent.append(e), True)[1]) == 3
    assert [e.device_id for e in sent] == ["DEV-0", "DEV-1", "DEV-2"]


def test_drain_stops_and_keeps_head_when_sender_returns_false():
    buf = StoreAndForwardBuffer(capacity=10)
    for i in range(4):
        buf.enqueue(_event(i))

    sent: list[HubEvent] = []

    def sender(e: HubEvent) -> bool:
        if len(sent) >= 2:
            return False  # simulate link dropping mid-flush
        sent.append(e)
        return True

    n = buf.drain(sender)
    assert n == 2
    assert len(buf) == 2  # head preserved, order intact
    assert buf.peek().device_id == "DEV-2"


def test_drain_keeps_event_on_sender_exception():
    buf = StoreAndForwardBuffer(capacity=10)
    buf.enqueue(_event(0))

    def boom(_e: HubEvent) -> bool:
        raise ConnectionError("WAN down")

    assert buf.drain(boom) == 0
    assert len(buf) == 1  # event NOT lost


def test_drain_max_events_limits_batch():
    buf = StoreAndForwardBuffer(capacity=10)
    for i in range(5):
        buf.enqueue(_event(i))
    sent: list[HubEvent] = []
    assert buf.drain(lambda e: (sent.append(e), True)[1], max_events=2) == 2
    assert len(buf) == 3
    assert [e.device_id for e in sent] == ["DEV-0", "DEV-1"]


def test_persistence_restores_backlog_in_order():
    store = InMemoryEventStore()
    buf = StoreAndForwardBuffer(capacity=10, store=store)
    for i in range(3):
        buf.enqueue(_event(i))

    # New buffer over the SAME store = simulated restart.
    buf2 = StoreAndForwardBuffer(capacity=10, store=store)
    assert len(buf2) == 3
    sent: list[HubEvent] = []
    buf2.drain(lambda e: (sent.append(e), True)[1])
    assert [e.device_id for e in sent] == ["DEV-0", "DEV-1", "DEV-2"]


def test_persistence_store_stays_in_sync_after_drain():
    store = InMemoryEventStore()
    buf = StoreAndForwardBuffer(capacity=10, store=store)
    for i in range(3):
        buf.enqueue(_event(i))
    buf.drain(lambda e: True)
    # After a full successful drain the durable store is empty too.
    assert store.load() == []


def test_restore_oversized_backlog_is_truncated_to_capacity():
    store = InMemoryEventStore([_event(i) for i in range(5)])
    buf = StoreAndForwardBuffer(capacity=3, store=store)
    assert len(buf) == 3
    assert buf.dropped_count == 2
    sent: list[HubEvent] = []
    buf.drain(lambda e: (sent.append(e), True)[1])
    # Freshest kept.
    assert [e.device_id for e in sent] == ["DEV-2", "DEV-3", "DEV-4"]


def test_connection_and_capacity_properties():
    buf = StoreAndForwardBuffer(capacity=7, connected=False)
    assert buf.capacity == 7
    assert buf.connected is False
    buf.set_connected(True)
    assert buf.connected is True


def test_event_roundtrip_dict():
    e = _event(7)
    d = StoreAndForwardBuffer.event_to_dict(e)
    assert d["severity"] == "critical"
    assert StoreAndForwardBuffer.event_from_dict(d) == e
