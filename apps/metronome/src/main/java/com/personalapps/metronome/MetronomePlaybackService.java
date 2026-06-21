package com.personalapps.metronome;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

public final class MetronomePlaybackService extends Service {
    public interface PlaybackListener {
        void onBeat(int beat);

        void onPlayingChanged(boolean isPlaying, int beat);
    }

    private static final String ACTION_START = "com.personalapps.metronome.action.START";
    private static final String ACTION_STOP = "com.personalapps.metronome.action.STOP";
    private static final String EXTRA_BPM = "bpm";
    private static final String EXTRA_BEATS_PER_BAR = "beatsPerBar";
    private static final String CHANNEL_ID = "metronome_playback";
    private static final int NOTIFICATION_ID = 20_260_621;

    private final IBinder binder = new LocalBinder();

    private MetronomePlaybackSession session;
    private NotificationManager notificationManager;
    private volatile PlaybackListener listener;

    public static Intent startIntent(Context context, MetronomeEngine.Settings settings) {
        Intent intent = new Intent(context, MetronomePlaybackService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_BPM, settings.bpm);
        intent.putExtra(EXTRA_BEATS_PER_BAR, settings.beatsPerBar);
        return intent;
    }

    public static Intent stopIntent(Context context) {
        Intent intent = new Intent(context, MetronomePlaybackService.class);
        intent.setAction(ACTION_STOP);
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();
        MetronomeEngine.Settings initialSettings = new MetronomeEngine.Settings(60, 4);
        session = new MetronomePlaybackSession(
                new MetronomeEngine(new ClickSoundPool(this), initialSettings),
                createWakeLockHandle(),
                initialSettings
        );
        session.setListener(beat -> {
            PlaybackListener currentListener = listener;
            if (currentListener != null) {
                currentListener.onBeat(beat);
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopPlayback();
            return START_NOT_STICKY;
        }
        startPlayback(settingsFromIntent(intent));
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        if (session != null) {
            session.release();
        }
        super.onDestroy();
    }

    void setListener(PlaybackListener listener) {
        this.listener = listener;
        if (listener != null) {
            listener.onPlayingChanged(isPlaying(), getCurrentBeat());
        }
    }

    void startPlayback(MetronomeEngine.Settings settings) {
        if (session == null) {
            return;
        }
        startForeground(NOTIFICATION_ID, buildNotification(settings));
        session.start(settings);
        notifyPlaybackChanged();
    }

    void updateSettings(MetronomeEngine.Settings settings) {
        if (session == null) {
            return;
        }
        session.update(settings);
        if (session.isPlaying() && notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, buildNotification(settings));
        }
    }

    void stopPlayback() {
        if (session != null) {
            session.stop();
        }
        notifyPlaybackChanged();
        stopForegroundCompat();
        stopSelf();
    }

    boolean isPlaying() {
        return session != null && session.isPlaying();
    }

    int getCurrentBeat() {
        return session == null ? -1 : session.getCurrentBeat();
    }

    MetronomeEngine.Settings getSettings() {
        return session == null ? new MetronomeEngine.Settings(60, 4) : session.getSettings();
    }

    private MetronomeEngine.Settings settingsFromIntent(Intent intent) {
        if (intent == null) {
            return getSettings();
        }
        return new MetronomeEngine.Settings(
                intent.getIntExtra(EXTRA_BPM, getSettings().bpm),
                intent.getIntExtra(EXTRA_BEATS_PER_BAR, getSettings().beatsPerBar)
        );
    }

    private MetronomePlaybackSession.WakeLockHandle createWakeLockHandle() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager == null) {
            return new NoOpWakeLockHandle();
        }
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "PersonalMobileApps:MetronomePlayback"
        );
        wakeLock.setReferenceCounted(false);
        return new AndroidWakeLockHandle(wakeLock);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26 || notificationManager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "节拍器播放",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("保持节拍器息屏播放");
        channel.setShowBadge(false);
        notificationManager.createNotificationChannel(channel);
    }

    private Notification buildNotification(MetronomeEngine.Settings settings) {
        Intent contentIntent = new Intent(this, MetronomeActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this,
                0,
                contentIntent,
                pendingIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT)
        );
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                1,
                stopIntent(this),
                pendingIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT)
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("钢琴节拍器正在播放")
                .setContentText(settings.bpm + " BPM · 每小节 " + settings.beatsPerBar + " 拍")
                .setContentIntent(contentPendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(R.mipmap.ic_launcher, "停止", stopPendingIntent);
        return builder.build();
    }

    private int pendingIntentFlags(int baseFlags) {
        if (Build.VERSION.SDK_INT >= 23) {
            return baseFlags | PendingIntent.FLAG_IMMUTABLE;
        }
        return baseFlags;
    }

    @SuppressWarnings("deprecation")
    private void stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
    }

    private void notifyPlaybackChanged() {
        PlaybackListener currentListener = listener;
        if (currentListener != null) {
            currentListener.onPlayingChanged(isPlaying(), getCurrentBeat());
        }
    }

    public final class LocalBinder extends Binder {
        MetronomePlaybackService getService() {
            return MetronomePlaybackService.this;
        }
    }

    private static final class AndroidWakeLockHandle implements MetronomePlaybackSession.WakeLockHandle {
        private final PowerManager.WakeLock wakeLock;

        private AndroidWakeLockHandle(PowerManager.WakeLock wakeLock) {
            this.wakeLock = wakeLock;
        }

        @Override
        public boolean isHeld() {
            return wakeLock.isHeld();
        }

        @Override
        public void acquire() {
            wakeLock.acquire();
        }

        @Override
        public void release() {
            wakeLock.release();
        }
    }

    private static final class NoOpWakeLockHandle implements MetronomePlaybackSession.WakeLockHandle {
        @Override
        public boolean isHeld() {
            return false;
        }

        @Override
        public void acquire() {
        }

        @Override
        public void release() {
        }
    }
}
