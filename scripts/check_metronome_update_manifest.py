#!/usr/bin/env python3
"""Validate the metronome update manifest and in-app update-check wiring."""

from __future__ import annotations

import json
import pathlib
import re
import sys


ROOT = pathlib.Path(__file__).resolve().parents[1]
APP_DIR = ROOT / "apps" / "metronome"
UPDATE_MANIFEST = APP_DIR / "update.json"
ENV_EXAMPLE = ROOT / ".env.example"
ANDROID_MANIFEST = APP_DIR / "src" / "main" / "AndroidManifest.xml"
ACTIVITY = APP_DIR / "src" / "main" / "java" / "com" / "personalapps" / "metronome" / "MetronomeActivity.java"


def env_value(name: str) -> str:
    pattern = re.compile(rf"^{re.escape(name)}=(.+)$")
    for line in ENV_EXAMPLE.read_text(encoding="utf-8").splitlines():
        match = pattern.match(line.strip())
        if match:
            return match.group(1).strip()
    raise AssertionError(f"{name} is missing in .env.example")


def fail(message: str, failures: list[str]) -> None:
    failures.append(message)


def main() -> int:
    failures: list[str] = []
    version_code = int(env_value("METRONOME_VERSION_CODE"))
    version_name = env_value("METRONOME_VERSION_NAME")
    expected_tag = f"metronome-v{version_name}"
    expected_apk = f"metronome-{version_name}.apk"

    if not UPDATE_MANIFEST.exists():
        fail(f"{UPDATE_MANIFEST.relative_to(ROOT)} is missing", failures)
    else:
        manifest = json.loads(UPDATE_MANIFEST.read_text(encoding="utf-8"))
        if manifest.get("app") != "metronome":
            fail("update manifest app must be metronome", failures)
        if manifest.get("versionCode") != version_code:
            fail(f"versionCode must be {version_code}", failures)
        if manifest.get("versionName") != version_name:
            fail(f"versionName must be {version_name}", failures)
        apk_url = manifest.get("apkUrl", "")
        checksum_url = manifest.get("checksumUrl", "")
        release_url = manifest.get("releaseUrl", "")
        if expected_tag not in apk_url or expected_apk not in apk_url:
            fail("apkUrl must point at the current Gitee APK asset", failures)
        if expected_tag not in checksum_url or not checksum_url.endswith(f"{expected_apk}.sha256"):
            fail("checksumUrl must point at the current Gitee checksum asset", failures)
        if expected_tag not in release_url:
            fail("releaseUrl must point at the current Gitee release tag", failures)
        notes = manifest.get("notes")
        if not isinstance(notes, list) or not notes or not all(isinstance(item, str) and item.strip() for item in notes):
            fail("notes must be a non-empty list of strings", failures)

    android_manifest = ANDROID_MANIFEST.read_text(encoding="utf-8")
    if 'android.permission.INTERNET' not in android_manifest:
        fail("AndroidManifest.xml must declare INTERNET permission", failures)

    activity = ACTIVITY.read_text(encoding="utf-8")
    expected_update_url = "https://gitee.com/jackyyu/personal-mobile-apps/raw/main/apps/metronome/update.json"
    if expected_update_url not in activity:
        fail("MetronomeActivity must fetch the stable Gitee update manifest URL", failures)
    for marker in ["checkForUpdate", "fetchUpdateInfo", "openUpdateUrl"]:
        if marker not in activity:
            fail(f"MetronomeActivity is missing {marker}", failures)

    if failures:
        print("Update manifest check failed:")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print(f"Update manifest check passed for metronome {version_name} ({version_code})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
