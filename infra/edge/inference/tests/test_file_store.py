"""Tests for the durable JSONL event store (persistence across restarts)."""

from __future__ import annotations

from edge_inference.domain.buffer import StoreAndForwardBuffer
from edge_inference.domain.models import HubEvent, Severity
from edge_inference.infrastructure.file_store import JsonlFileEventStore


def _event(i: int) -> HubEvent:
    return HubEvent(
        event_type="iot.anomaly.detected",
        device_id=f"DEV-{i}",
        metric="temperature",
        score=0.9,
        severity=Severity.WARNING,
        backend="fallback",
        reason="above-high",
        timestamp_ms=1_000 + i,
    )


def test_append_and_load_roundtrip(tmp_path):
    store = JsonlFileEventStore(tmp_path / "queue.jsonl")
    store.append(_event(0))
    store.append(_event(1))
    loaded = store.load()
    assert [e.device_id for e in loaded] == ["DEV-0", "DEV-1"]
    assert loaded[0] == _event(0)


def test_buffer_survives_restart_via_file_store(tmp_path):
    path = tmp_path / "queue.jsonl"
    buf = StoreAndForwardBuffer(capacity=10, store=JsonlFileEventStore(path))
    for i in range(3):
        buf.enqueue(_event(i))

    # New process: fresh buffer over a fresh store pointing at the same file.
    buf2 = StoreAndForwardBuffer(capacity=10, store=JsonlFileEventStore(path))
    assert len(buf2) == 3
    sent: list[HubEvent] = []
    buf2.drain(lambda e: (sent.append(e), True)[1])
    assert [e.device_id for e in sent] == ["DEV-0", "DEV-1", "DEV-2"]


def test_pop_left_persists(tmp_path):
    path = tmp_path / "queue.jsonl"
    store = JsonlFileEventStore(path)
    for i in range(3):
        store.append(_event(i))
    store.pop_left()
    assert [e.device_id for e in store.load()] == ["DEV-1", "DEV-2"]
    # And it is durable.
    assert [e.device_id for e in JsonlFileEventStore(path).load()] == ["DEV-1", "DEV-2"]


def test_replace_rewrites_file_atomically(tmp_path):
    path = tmp_path / "queue.jsonl"
    store = JsonlFileEventStore(path)
    for i in range(3):
        store.append(_event(i))
    store.replace([_event(9)])
    assert [e.device_id for e in store.load()] == ["DEV-9"]


def test_drain_clears_durable_store(tmp_path):
    path = tmp_path / "queue.jsonl"
    buf = StoreAndForwardBuffer(capacity=10, store=JsonlFileEventStore(path))
    for i in range(3):
        buf.enqueue(_event(i))
    buf.drain(lambda e: True)
    assert JsonlFileEventStore(path).load() == []


def test_corrupt_line_is_skipped_on_load(tmp_path):
    path = tmp_path / "queue.jsonl"
    store = JsonlFileEventStore(path)
    store.append(_event(0))
    # Simulate a torn final write after a power loss.
    with path.open("a", encoding="utf-8") as fh:
        fh.write('{"event_type": "broken"\n')  # invalid JSON / missing fields
    store.append(_event(1))
    loaded = store.load()
    assert [e.device_id for e in loaded] == ["DEV-0", "DEV-1"]


def test_pop_left_on_empty_is_noop(tmp_path):
    store = JsonlFileEventStore(tmp_path / "queue.jsonl")
    store.pop_left()  # must not raise
    assert store.load() == []


def test_blank_lines_ignored(tmp_path):
    path = tmp_path / "queue.jsonl"
    store = JsonlFileEventStore(path)
    store.append(_event(0))
    with path.open("a", encoding="utf-8") as fh:
        fh.write("\n   \n")
    assert [e.device_id for e in store.load()] == ["DEV-0"]
