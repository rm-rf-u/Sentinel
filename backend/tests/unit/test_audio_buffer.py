"""AudioRingBuffer correctness tests."""

import numpy as np
import pytest

from sentinel.capture.audio_buffer import AudioRingBuffer


def test_returns_none_before_enough_data():
    buf = AudioRingBuffer(max_seconds=1.0, sample_rate=100)
    buf.put(np.ones(50, dtype=np.float32))
    assert buf.get_latest(80) is None


def test_get_latest_correct_values():
    buf = AudioRingBuffer(max_seconds=2.0, sample_rate=100)
    buf.put(np.zeros(50, dtype=np.float32))
    buf.put(np.ones(50, dtype=np.float32))
    result = buf.get_latest(50)
    assert result is not None
    np.testing.assert_array_equal(result, np.ones(50, dtype=np.float32))


def test_wrap_around():
    """Buffer smaller than total written — wrap-around must be seamless."""
    buf = AudioRingBuffer(max_seconds=1.0, sample_rate=10)  # cap = 10 samples
    for i in range(3):
        buf.put(np.full(5, float(i), dtype=np.float32))
    # After 15 samples written into a 10-sample buffer, latest 5 should be 2.0
    result = buf.get_latest(5)
    assert result is not None
    np.testing.assert_array_almost_equal(result, np.full(5, 2.0))


def test_seconds_buffered():
    buf = AudioRingBuffer(max_seconds=5.0, sample_rate=100)
    buf.put(np.zeros(200, dtype=np.float32))
    assert buf.seconds_buffered == pytest.approx(2.0)


def test_put_and_get_larger_than_window():
    buf = AudioRingBuffer(max_seconds=5.0, sample_rate=100)
    data = np.arange(300, dtype=np.float32)
    buf.put(data)
    result = buf.get_latest(100)
    assert result is not None
    np.testing.assert_array_equal(result, data[-100:])
