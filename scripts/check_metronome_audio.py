#!/usr/bin/env python3
"""Check metronome click samples for basic loudness and harshness bounds."""

from __future__ import annotations

import math
import pathlib
import struct
import sys
import wave


ROOT = pathlib.Path(__file__).resolve().parents[1]
RAW_DIR = ROOT / "apps" / "metronome" / "src" / "main" / "res" / "raw"


def read_pcm(path: pathlib.Path) -> tuple[int, list[int]]:
    with wave.open(str(path), "rb") as wav:
        if wav.getnchannels() != 1:
            raise AssertionError(f"{path.name}: expected mono wav")
        if wav.getsampwidth() != 2:
            raise AssertionError(f"{path.name}: expected 16-bit PCM wav")
        if wav.getframerate() != 44100:
            raise AssertionError(f"{path.name}: expected 44100 Hz wav")
        frames = wav.readframes(wav.getnframes())
        sample_count = len(frames) // 2
    return 44100, list(struct.unpack(f"<{sample_count}h", frames))


def metrics(path: pathlib.Path) -> dict[str, float]:
    sample_rate, samples = read_pcm(path)
    abs_samples = [abs(sample) for sample in samples]
    duration = len(samples) / sample_rate
    peak = max(abs_samples) / 32768.0
    rms = math.sqrt(sum(sample * sample for sample in samples) / len(samples)) / 32768.0
    attack_count = max(1, int(sample_rate * 0.005))
    attack_5ms = max(abs(sample) for sample in samples[:attack_count]) / 32768.0
    zero_crossings = sum(
        1
        for left, right in zip(samples, samples[1:])
        if (left < 0 <= right) or (right < 0 <= left)
    )
    return {
        "duration": duration,
        "peak": peak,
        "rms": rms,
        "attack_5ms": attack_5ms,
        "zero_crossing_rate": zero_crossings / duration,
    }


def main() -> int:
    accent = metrics(RAW_DIR / "metronome_accent.wav")
    normal = metrics(RAW_DIR / "metronome_tick.wav")
    failures: list[str] = []

    if not 0.12 <= accent["duration"] <= 0.24:
        failures.append(f"accent duration out of range: {accent['duration']:.3f}s")
    if not 0.86 <= accent["peak"] <= 0.93:
        failures.append(f"accent peak should stay prominent: {accent['peak']:.3f}")
    if not 0.069 <= accent["rms"] <= 0.080:
        failures.append(f"accent body should be clearly audible: {accent['rms']:.3f}")
    if not 0.40 <= accent["attack_5ms"] <= 0.62:
        failures.append(f"accent attack should be present but rounded: {accent['attack_5ms']:.3f}")
    if accent["zero_crossing_rate"] < normal["zero_crossing_rate"] * 3.0:
        failures.append(
            "accent brightness is too muted: "
            f"{accent['zero_crossing_rate']:.1f}/s vs normal "
            f"{normal['zero_crossing_rate']:.1f}/s"
        )
    if accent["zero_crossing_rate"] > normal["zero_crossing_rate"] * 4.1:
        failures.append(
            "accent high-frequency content is too strong: "
            f"{accent['zero_crossing_rate']:.1f}/s vs normal "
            f"{normal['zero_crossing_rate']:.1f}/s"
        )

    print("accent", {key: round(value, 4) for key, value in accent.items()})
    print("normal", {key: round(value, 4) for key, value in normal.items()})
    if failures:
        print("Audio check failed:")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print("Audio check passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
