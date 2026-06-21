package com.personalapps.metronome;

final class MetronomePlaybackSession {
    interface Clock {
        void setListener(MetronomeEngine.Listener listener);

        void start(MetronomeEngine.Settings settings);

        void update(MetronomeEngine.Settings settings);

        void stop();

        void release();
    }

    interface WakeLockHandle {
        boolean isHeld();

        void acquire();

        void release();
    }

    private final Clock clock;
    private final WakeLockHandle wakeLock;

    private MetronomeEngine.Settings settings;
    private volatile MetronomeEngine.Listener listener;
    private boolean playing = false;
    private int currentBeat = -1;

    MetronomePlaybackSession(
            Clock clock,
            WakeLockHandle wakeLock,
            MetronomeEngine.Settings initialSettings
    ) {
        this.clock = clock;
        this.wakeLock = wakeLock;
        settings = initialSettings;
        clock.setListener(beat -> {
            synchronized (MetronomePlaybackSession.this) {
                currentBeat = beat;
            }
            MetronomeEngine.Listener currentListener = listener;
            if (currentListener != null) {
                currentListener.onBeat(beat);
            }
        });
    }

    synchronized void setListener(MetronomeEngine.Listener listener) {
        this.listener = listener;
    }

    synchronized void start(MetronomeEngine.Settings nextSettings) {
        settings = nextSettings;
        currentBeat = -1;
        acquireWakeLockIfNeeded();
        try {
            clock.start(nextSettings);
            playing = true;
        } catch (RuntimeException exception) {
            playing = false;
            releaseWakeLockIfHeld();
            throw exception;
        }
    }

    synchronized void update(MetronomeEngine.Settings nextSettings) {
        settings = nextSettings;
        if (playing) {
            clock.update(nextSettings);
        }
    }

    synchronized void stop() {
        playing = false;
        currentBeat = -1;
        try {
            clock.stop();
        } finally {
            releaseWakeLockIfHeld();
        }
    }

    synchronized void release() {
        playing = false;
        currentBeat = -1;
        try {
            clock.release();
        } finally {
            releaseWakeLockIfHeld();
        }
    }

    synchronized boolean isPlaying() {
        return playing;
    }

    synchronized int getCurrentBeat() {
        return currentBeat;
    }

    synchronized MetronomeEngine.Settings getSettings() {
        return settings;
    }

    private void acquireWakeLockIfNeeded() {
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }
    }

    private void releaseWakeLockIfHeld() {
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
}
