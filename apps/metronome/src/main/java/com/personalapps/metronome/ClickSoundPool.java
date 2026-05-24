package com.personalapps.metronome;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.ToneGenerator;
import android.os.Build;

public final class ClickSoundPool {
    private final SoundPool soundPool;
    private final ToneGenerator fallback;
    private final int accentId;
    private final int normalId;
    private volatile int loadedCount = 0;

    public ClickSoundPool(Context context) {
        if (Build.VERSION.SDK_INT >= 21) {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setAudioAttributes(attributes)
                    .setMaxStreams(4)
                    .build();
        } else {
            soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
        }
        fallback = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
            if (status == 0) {
                loadedCount += 1;
            }
        });
        Context appContext = context.getApplicationContext();
        accentId = soundPool.load(appContext, R.raw.metronome_accent, 1);
        normalId = soundPool.load(appContext, R.raw.metronome_tick, 1);
    }

    public void play(boolean accent, int beatIndex) {
        int sampleId = accent ? accentId : normalId;
        if (sampleId != 0 && loadedCount >= 2) {
            soundPool.play(sampleId, 1f, 1f, accent ? 2 : 1, 0, 1f);
            return;
        }
        fallback.startTone(accent ? ToneGenerator.TONE_PROP_ACK : ToneGenerator.TONE_PROP_BEEP, 60);
    }

    public void release() {
        soundPool.release();
        fallback.release();
    }
}
