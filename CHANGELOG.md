# Changelog

## metronome 1.0.21

- Moved metronome playback into a foreground service so audio keeps running after the screen turns off.
- Kept the playback wakelock inside the service lifecycle and added coverage for start, update, stop, and release behavior.

## metronome 1.0.20

- Updated the default time-signature order to 2/2, 2/4, 3/4, 4/4, 3/8, 6/8, and 12/8, with 4/4 as the default.

## metronome 1.0.19

- Refined the default time-signature list for piano practice: 2/4, 3/4, 4/4, 2/2, 3/8, and 6/8.

## metronome 1.0.18

- Added tap-to-cycle behavior for the tempo marking label, using standard piano/metronome ranges and representative BPM targets.

## metronome 1.0.17

- Switched the update checker to Gitee's contents API so the app does not depend on raw-file cache freshness.

## metronome 1.0.16

- Added an in-app update check that opens the Gitee APK download when a newer version is available.

## metronome 1.0.15

- Raised the first-beat accent between the 1.0.13 and 1.0.14 samples, keeping it clearer without restoring the sharp transient.

## metronome 1.0.14

- Softened the first-beat accent sample to reduce harsh high-frequency transients.

## metronome 1.0.13

- Added press-and-hold tempo adjustment for the BPM step buttons.
- Simplified the beat status copy when playback is stopped.

## metronome 1.0.12

- Added BPM step controls around the tempo input.
- Added automatic Italian tempo marking labels for BPM changes.
- Refined tempo control spacing for a more balanced layout.

## metronome 1.0.11

- First open-source release setup.
- Added `.env` based project configuration.
- Added GitHub-first release workflow with Gitee post-sync support.
- Added README and MIT license.
