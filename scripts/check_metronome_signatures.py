#!/usr/bin/env python3
"""Validate the metronome's default time-signature choices."""

from __future__ import annotations

import pathlib
import re
import sys


ROOT = pathlib.Path(__file__).resolve().parents[1]
ACTIVITY = ROOT / "apps" / "metronome" / "src" / "main" / "java" / "com" / "personalapps" / "metronome" / "MetronomeActivity.java"

EXPECTED_LABELS = ["2/4", "3/4", "4/4", "2/2", "3/8", "6/8"]
EXPECTED_BEATS = [2, 3, 4, 2, 3, 6]
IRREGULAR_DEFAULTS = {"5/4", "7/8"}
ADVANCED_COMPOUND_DEFAULTS = {"9/8", "12/8"}


def extract_string_array(source: str, name: str) -> list[str]:
    match = re.search(rf"private static final String\[\] {name} = \{{(.*?)\n    \}};", source, re.S)
    if not match:
        raise AssertionError(f"{name} is missing")
    return re.findall(r'"([^"]+)"', match.group(1))


def extract_int_array(source: str, name: str) -> list[int]:
    match = re.search(rf"private static final int\[\] {name} = \{{(.*?)\n    \}};", source, re.S)
    if not match:
        raise AssertionError(f"{name} is missing")
    return [int(value) for value in re.findall(r"\d+", match.group(1))]


def main() -> int:
    source = ACTIVITY.read_text(encoding="utf-8")
    failures: list[str] = []

    labels = extract_string_array(source, "SIGNATURE_LABELS")
    beats = extract_int_array(source, "SIGNATURE_BEATS")

    if labels != EXPECTED_LABELS:
        failures.append(f"SIGNATURE_LABELS must be {EXPECTED_LABELS}, got {labels}")
    if beats != EXPECTED_BEATS:
        failures.append(f"SIGNATURE_BEATS must be {EXPECTED_BEATS}, got {beats}")
    blocked = sorted((IRREGULAR_DEFAULTS | ADVANCED_COMPOUND_DEFAULTS).intersection(labels))
    if blocked:
        failures.append(f"default signatures should not include {blocked}")
    if len(labels) != len(beats):
        failures.append("SIGNATURE_LABELS and SIGNATURE_BEATS must have the same length")

    if failures:
        print("Signature check failed:")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print("Signature check passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
