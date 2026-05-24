package com.personalapps.metronome;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public final class MetronomeEngine {
    public interface Listener {
        void onBeat(int beatIndex);
    }

    public static final class Settings {
        public final int bpm;
        public final int beatsPerBar;

        public Settings(int bpm, int beatsPerBar) {
            this.bpm = Math.max(30, Math.min(240, bpm));
            this.beatsPerBar = Math.max(1, Math.min(12, beatsPerBar));
        }
    }

    private static final long FIRST_TICK_DELAY_NANOS = 40_000_000L;

    private final ClickSoundPool clickSoundPool;
    private final AtomicReference<Settings> settingsRef;
    private volatile boolean running = false;
    private volatile Listener listener;
    private Thread clockThread;

    public MetronomeEngine(ClickSoundPool clickSoundPool, Settings initialSettings) {
        this.clickSoundPool = clickSoundPool;
        settingsRef = new AtomicReference<>(initialSettings);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public synchronized void start(Settings settings) {
        stop();
        settingsRef.set(settings);
        running = true;
        clockThread = new Thread(this::runClock, "MetronomeClock");
        clockThread.setPriority(Thread.MAX_PRIORITY);
        clockThread.start();
    }

    public synchronized void stop() {
        running = false;
        Thread thread = clockThread;
        clockThread = null;
        if (thread != null) {
            thread.interrupt();
            if (Thread.currentThread() != thread) {
                try {
                    thread.join(180);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void update(Settings settings) {
        settingsRef.set(settings);
    }

    public void release() {
        stop();
        clickSoundPool.release();
    }

    private void runClock() {
        int beat = 0;
        long nextTick = System.nanoTime() + FIRST_TICK_DELAY_NANOS;

        while (running) {
            long waitNanos = nextTick - System.nanoTime();
            if (waitNanos > 0) {
                LockSupport.parkNanos(waitNanos);
            }
            if (!running) {
                break;
            }

            Settings settings = settingsRef.get();
            if (beat >= settings.beatsPerBar) {
                beat = 0;
            }

            clickSoundPool.play(beat == 0, beat);
            Listener currentListener = listener;
            if (currentListener != null) {
                currentListener.onBeat(beat);
            }

            beat = (beat + 1) % settings.beatsPerBar;
            long periodNanos = Math.round(60_000_000_000.0 / settings.bpm);
            nextTick += periodNanos;
            long now = System.nanoTime();
            if (nextTick < now - periodNanos) {
                nextTick = now + periodNanos;
            }
        }
    }
}
