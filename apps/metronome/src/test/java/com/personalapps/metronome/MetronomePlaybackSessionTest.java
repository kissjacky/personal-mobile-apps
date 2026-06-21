package com.personalapps.metronome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class MetronomePlaybackSessionTest {
    @Test
    public void startHoldsWakeLockUntilStop() {
        FakeClock clock = new FakeClock();
        FakeWakeLock wakeLock = new FakeWakeLock();
        MetronomePlaybackSession session = new MetronomePlaybackSession(
                clock,
                wakeLock,
                new MetronomeEngine.Settings(60, 4)
        );

        session.start(new MetronomeEngine.Settings(72, 3));

        assertTrue(session.isPlaying());
        assertTrue(wakeLock.held);
        assertEquals(1, wakeLock.acquireCount);
        assertEquals(72, clock.startedSettings.bpm);
        assertEquals(3, clock.startedSettings.beatsPerBar);

        session.stop();

        assertFalse(session.isPlaying());
        assertFalse(wakeLock.held);
        assertEquals(1, clock.stopCount);
        assertEquals(1, wakeLock.releaseCount);
    }

    @Test
    public void updateWhilePlayingChangesClockSettingsWithoutRestarting() {
        FakeClock clock = new FakeClock();
        MetronomePlaybackSession session = new MetronomePlaybackSession(
                clock,
                new FakeWakeLock(),
                new MetronomeEngine.Settings(60, 4)
        );

        session.start(new MetronomeEngine.Settings(80, 4));
        session.update(new MetronomeEngine.Settings(100, 6));

        assertEquals(1, clock.startCount);
        assertEquals(1, clock.updateCount);
        assertEquals(100, clock.updatedSettings.bpm);
        assertEquals(6, clock.updatedSettings.beatsPerBar);
    }

    @Test
    public void releaseStopsClockAndReleasesWakeLock() {
        FakeClock clock = new FakeClock();
        FakeWakeLock wakeLock = new FakeWakeLock();
        MetronomePlaybackSession session = new MetronomePlaybackSession(
                clock,
                wakeLock,
                new MetronomeEngine.Settings(60, 4)
        );

        session.start(new MetronomeEngine.Settings(90, 4));
        session.release();

        assertFalse(session.isPlaying());
        assertFalse(wakeLock.held);
        assertEquals(1, clock.releaseCount);
        assertEquals(1, wakeLock.releaseCount);
    }

    private static final class FakeClock implements MetronomePlaybackSession.Clock {
        private int startCount;
        private int updateCount;
        private int stopCount;
        private int releaseCount;
        private MetronomeEngine.Settings startedSettings;
        private MetronomeEngine.Settings updatedSettings;

        @Override
        public void setListener(MetronomeEngine.Listener listener) {
        }

        @Override
        public void start(MetronomeEngine.Settings settings) {
            startCount += 1;
            startedSettings = settings;
        }

        @Override
        public void update(MetronomeEngine.Settings settings) {
            updateCount += 1;
            updatedSettings = settings;
        }

        @Override
        public void stop() {
            stopCount += 1;
        }

        @Override
        public void release() {
            releaseCount += 1;
        }
    }

    private static final class FakeWakeLock implements MetronomePlaybackSession.WakeLockHandle {
        private boolean held;
        private int acquireCount;
        private int releaseCount;

        @Override
        public boolean isHeld() {
            return held;
        }

        @Override
        public void acquire() {
            acquireCount += 1;
            held = true;
        }

        @Override
        public void release() {
            releaseCount += 1;
            held = false;
        }
    }
}
