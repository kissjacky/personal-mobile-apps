# Release Guide

GitHub is the primary repository and the source of truth for releases. Gitee is a post-publish mirror for users in China.

## One-time Repository Setup

Create the primary GitHub repository and an empty Gitee mirror repository:

- Primary: `git@github.com:kissjacky/personal-mobile-apps.git`
- Mirror: `git@gitee.com:jackyyu/personal-mobile-apps.git`

Push to GitHub first:

```bash
git push -u origin main
git push origin --tags
```

The Gitee repository should be updated by the `Sync to Gitee` workflow or by `./scripts/mirror_to_gitee.sh` as a fallback.

## Required Secrets

GitHub Actions needs these repository secrets to build signed APKs:

| Secret | Description |
| --- | --- |
| `ANDROID_SIGNING_KEY_BASE64` | Base64 encoded release `.jks` file. |
| `ANDROID_SIGNING_STORE_PASSWORD` | Keystore password. |
| `ANDROID_SIGNING_KEY_ALIAS` | Key alias. |
| `ANDROID_SIGNING_KEY_PASSWORD` | Key password. |

Optional Gitee post-sync secrets:

| Secret | Description |
| --- | --- |
| `GITEE_ACCESS_TOKEN` | Gitee personal access token with repository release permissions. |
| `GITEE_OWNER` | Gitee namespace, currently `jackyyu`. |
| `GITEE_REPO` | Gitee repository path, currently `personal-mobile-apps`. |
| `GITEE_SSH_PRIVATE_KEY` | Private key allowed to mirror Git refs to the Gitee repository. |

## Create a Signing Key

Keep the keystore outside Git. A local example:

```bash
mkdir -p .tmp-signing
keytool -genkeypair \
  -v \
  -keystore .tmp-signing/metronome-release.jks \
  -storetype JKS \
  -alias metronome \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Put the local signing values in `.env`:

```dotenv
ANDROID_SIGNING_STORE_FILE=.tmp-signing/metronome-release.jks
ANDROID_SIGNING_STORE_PASSWORD=replace-me
ANDROID_SIGNING_KEY_ALIAS=metronome
ANDROID_SIGNING_KEY_PASSWORD=replace-me
```

Generate the GitHub secret value for the keystore:

```bash
base64 -i .tmp-signing/metronome-release.jks | pbcopy
```

Paste that value into `ANDROID_SIGNING_KEY_BASE64`.

## Release from GitHub Actions

Update `.env.example`, `CHANGELOG.md`, and the app version in `.env` locally. Commit the source change, then tag:

```bash
git tag -a metronome-v1.0.20 -m "metronome 1.0.20"
git push origin main --tags
```

The `android-release.yml` workflow will:

1. Build a signed release APK.
2. Upload the APK and SHA-256 checksum to GitHub Releases.
3. Run a best-effort post-release Gitee asset sync when Gitee secrets are configured.

The `sync-gitee.yml` workflow also keeps the Gitee code mirror updated and can be run manually to resync release assets:

1. Mirror GitHub refs to Gitee.
2. Download the GitHub Release assets.
3. Upload the same APK and checksum to the Gitee Release when Gitee secrets are configured.

## Release from Local Machine

Build a signed APK:

```bash
./scripts/build_release.sh metronome
```

Create a GitHub Release:

```bash
./scripts/create_github_release.sh metronome
```

Gitee is only a post-publish mirror. Use these only as fallbacks if the workflow cannot run:

```bash
./scripts/mirror_to_gitee.sh
./scripts/publish_gitee_release.sh metronome-v1.0.20 dist/releases/metronome-v1.0.20.apk
```
