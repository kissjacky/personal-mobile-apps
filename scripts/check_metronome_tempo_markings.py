#!/usr/bin/env python3
"""Validate metronome tempo marking ranges and tap-to-cycle wiring."""

from __future__ import annotations

import pathlib
import re
import sys


ROOT = pathlib.Path(__file__).resolve().parents[1]
ACTIVITY = ROOT / "apps" / "metronome" / "src" / "main" / "java" / "com" / "personalapps" / "metronome" / "MetronomeActivity.java"

EXPECTED_MARKINGS = [
    (45, 40, "Grave", "庄板"),
    (60, 50, "Largo", "广板"),
    (66, 63, "Larghetto", "小广板"),
    (76, 72, "Adagio", "柔板"),
    (108, 88, "Andante", "行板"),
    (120, 112, "Moderato", "中板"),
    (168, 132, "Allegro", "快板"),
    (176, 172, "Vivace", "活板"),
    (200, 184, "Presto", "急板"),
    (240, 208, "Prestissimo", "最急板"),
]


def main() -> int:
    source = ACTIVITY.read_text(encoding="utf-8")
    failures: list[str] = []

    max_bpm_match = re.search(r"private static final int MAX_BPM = (\d+);", source)
    max_bpm = int(max_bpm_match.group(1)) if max_bpm_match else 0
    pattern = re.compile(
        r'new TempoMarking\((\d+|MAX_BPM),\s*(\d+),\s*"([^"]+)",\s*"([^"]+)"\)'
    )
    actual = [
        (max_bpm if max_bpm_value == "MAX_BPM" else int(max_bpm_value), int(target_bpm), name, chinese_name)
        for max_bpm_value, target_bpm, name, chinese_name in pattern.findall(source)
    ]
    if actual != EXPECTED_MARKINGS:
        failures.append("TEMPO_MARKINGS must use standard piano/metronome order, ranges, and target BPMs")

    required_markers = [
        "tempoMarkingText.setOnClickListener",
        "cycleTempoMarking",
        "setTempoBpm",
        "targetBpm",
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
