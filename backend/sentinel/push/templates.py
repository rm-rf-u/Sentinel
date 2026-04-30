"""Korean push notification copy, keyed by EventType."""

from sentinel.types import EventType

# (title, body)
NOTIFICATION: dict[str, tuple[str, str]] = {
    EventType.ZONE_VIOLATION: (
        "안전 구역 이탈",
        "아이가 안전 구역을 벗어났습니다. 확인해 주세요.",
    ),
    EventType.PRONE_POSITION: (
        "엎드린 자세 감지",
        "엎드린 자세가 감지되었습니다. 아기를 확인해 주세요.",
    ),
    EventType.CRY_DETECTED: (
        "울음 감지",
        "아기가 울고 있습니다.",
    ),
}
