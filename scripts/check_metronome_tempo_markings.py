#!/usr/bin/env python3
"""Validate metronome tempo marking ranges and tap-to-cycle wiring."""

from __future__ import annotations

import pathlib
import re
import sys


ROOT = pathlib.Path(__file__).resolve().parents[1]
ACTIVITY = ROOT / "apps" / "metronome" / "src" / "main" / "java" / "com" / "personalapps" / "metronome" / "MetronomeActivity.java"

EXPECTED_MARKINGS = [
    (40, 60, 50, "Largo", "广板"),
    (60, 66, 63, "Larghetto", "小广板"),
    (66, 76, 72, "Adagio", "柔板"),
    (76, 108, 92, "Andante", "行板"),
    (108, 120, 112, "Moderato", "中板"),
    (120, 168, 144, "Allegro", "快板"),
    (168, 200, 184, "Presto", "急板"),
    (200, 208, 208, "Prestissimo", "最急板"),
]


def main() -> int:
    source = ACTIVITY.read_text(encoding="utf-8")
    failures: list[str] = []

    pattern = re.compile(
        r'new TempoMarking\((\d+),\s*(\d+),\s*(\d+),\s*"([^"]+)",\s*"([^"]+)"\)'
    )
    actual = [
        (int(min_bpm), int(max_bpm), int(target_bpm), name, chinese_name)
        for min_bpm, max_bpm, target_bpm, name, chinese_name in pattern.findall(source)
    ]
    if actual != EXPECTED_MARKINGS:
        failures.append("TEMPO_MARKINGS must use the Wittner reference order, ranges, and product target BPMs")

    for index, (min_bpm, max_bpm, target_bpm, name, _) in enumerate(actual):
        if not min_bpm <= target_bpm <= max_bpm:
            failures.append(f"{name} target BPM must stay inside its reference range")
        if index + 1 < len(actual) and max_bpm != actual[index + 1][0]:
            failures.append(f"{name} range must meet the following range at one shared endpoint")

    required_markers = [
        "tempoMarkingText.setOnClickListener",
        "cycleTempoMarking",
        "setTempoBpm",
        "targetBpm",
        "Wittner 参考范围",
        "低于常用术语范围",
        "高于常用术语范围",
    ]
    for marker in required_markers:
        if marker not in source:
            failures.append(f"MetronomeActivity is missing {marker}")

    if failures:
        print("Tempo marking check failed:")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print("Tempo marking check passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
