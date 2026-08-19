#!/usr/bin/env python3
"""Validate metronome tempo marking ranges and tap-to-cycle wiring."""

from __future__ import annotations

import pathlib
import re
import sys


ROOT = pathlib.Path(__file__).resolve().parents[1]
ACTIVITY = ROOT / "apps" / "metronome" / "src" / "main" / "java" / "com" / "personalapps" / "metronome" / "MetronomeActivity.java"

EXPECTED_MARKINGS = [
    (40, 44, 42, "Grave", "庄板"),
    (46, 50, 48, "Largo", "广板"),
    (52, 54, 52, "Lento", "慢板"),
    (56, 58, 56, "Adagio", "柔板"),
    (60, 63, 60, "Larghetto", "小广板"),
    (66, 66, 66, "Andante", "行板"),
    (69, 84, 76, "Andantino", "小行板"),
    (88, 104, 96, "Moderato", "中板"),
    (108, 126, 116, "Allegretto", "小快板"),
    (132, 152, 144, "Allegro", "快板"),
    (160, 176, 168, "Vivace", "活板"),
    (184, 220, 200, "Presto", "急板"),
    (228, 228, 228, "Prestissimo", "最急板"),
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
        failures.append("TEMPO_MARKINGS must match the supplied textbook ranges and mechanical-scale targets")

    for min_bpm, max_bpm, target_bpm, name, _ in actual:
        if not min_bpm <= target_bpm <= max_bpm:
            failures.append(f"{name} target BPM must stay inside its reference range")

    required_markers = [
        "tempoMarkingText.setOnClickListener",
        "cycleTempoMarking",
        "nextTempoMarkingIndexFor",
        "setTempoBpm",
        "targetBpm",
        "教材参考范围",
        "低于教材术语范围",
        "高于教材术语范围",
        "术语档位间隔",
        "该 BPM 未落在教材术语范围",
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
