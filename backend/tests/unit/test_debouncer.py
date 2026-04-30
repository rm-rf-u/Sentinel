"""Debouncer timing correctness."""

import pytest
from sentinel.events.debouncer import Debouncer


def test_fires_once_after_threshold():
    d = Debouncer(2.0)
    assert not d.update(True, 0.0)
    assert not d.update(True, 1.0)
    assert d.update(True, 2.0)       # fires at threshold
    assert not d.update(True, 3.0)   # does NOT re-fire while still active


def test_rearms_after_reset():
    d = Debouncer(1.0)
    assert not d.update(True, 0.0)
    assert d.update(True, 1.0)
    d.update(False, 1.5)             # condition clears → rearm
    assert not d.update(True, 1.6)
    assert d.update(True, 2.6)       # fires again


def test_no_fire_if_condition_drops_early():
    d = Debouncer(3.0)
    d.update(True, 0.0)
    d.update(True, 1.0)
    d.update(False, 1.5)             # dropped before threshold
    assert not d.update(True, 4.0)  # new window starts; not yet at threshold
    assert d.update(True, 7.0)       # 3s elapsed in new window → fires


def test_elapsed_while_active():
    d = Debouncer(5.0)
    d.update(True, 10.0)
    assert d.elapsed(12.0) == pytest.approx(2.0)
    d.update(False, 13.0)
    assert d.elapsed(15.0) == 0.0


def test_reset():
    d = Debouncer(1.0)
    d.update(True, 0.0)
    d.update(True, 1.0)  # fires
    d.reset()
    assert not d.update(True, 1.5)
    assert d.update(True, 2.5)
