package com.personalapps.metronome;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.personalapps.commonui.UiKit;

public final class MetronomeActivity extends Activity {
    private static final int MIN_BPM = 30;
    private static final int MAX_BPM = 240;
    private static final String[] SIGNATURE_LABELS = {
            "2/4", "3/4", "4/4", "5/4", "6/8", "7/8", "9/8", "12/8"
    };
    private static final int[] SIGNATURE_BEATS = {
            2, 3, 4, 5, 6, 7, 9, 12
    };
    private static final TempoMarking[] TEMPO_MARKINGS = {
            new TempoMarking(45, "Grave", "庄板"),
            new TempoMarking(60, "Largo", "广板"),
            new TempoMarking(66, "Larghetto", "小广板"),
            new TempoMarking(75, "Adagio", "柔板"),
            new TempoMarking(107, "Andante", "行板"),
            new TempoMarking(119, "Moderato", "中板"),
            new TempoMarking(155, "Allegro", "快板"),
            new TempoMarking(167, "Vivace", "活板"),
            new TempoMarking(199, "Presto", "急板"),
            new TempoMarking(MAX_BPM, "Prestissimo", "最急板")
    };

    private final Handler handler = new Handler(Looper.getMainLooper());

    private SharedPreferences preferences;
    private MetronomeEngine engine;
    private PowerManager.WakeLock playbackWakeLock;

    private int bpm = 60;
    private int signatureIndex = 2;
    private int currentBeat = -1;
    private boolean playing = false;
    private boolean updatingBpmInput = false;

    private EditText bpmInput;
    private TextView tempoMarkingText;
    private Spinner signatureSpinner;
    private TextView statusText;
    private TextView beatText;
    private LinearLayout beatLights;
    private TextView playButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        preferences = getSharedPreferences("metronome_clean_v1", MODE_PRIVATE);
        loadPreferences();

        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(250, 252, 255));
        window.setNavigationBarColor(Color.rgb(250, 252, 255));

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            playbackWakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "PersonalMobileApps:Metronome"
            );
            playbackWakeLock.setReferenceCounted(false);
        }

        engine = new MetronomeEngine(new ClickSoundPool(this), currentSettings());
        engine.setListener(beat -> handler.post(() -> {
            currentBeat = beat;
            updateBeatViews();
        }));

        setContentView(buildContent());
        updateAllViews();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (engine != null) {
            engine.release();
        }
        releasePlaybackWakeLock();
        super.onDestroy();
    }

    private ScrollView buildContent() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setFocusable(true);
        scrollView.setFocusableInTouchMode(true);
        scrollView.setBackgroundColor(Color.rgb(250, 252, 255));
        scrollView.setOnClickListener(view -> clearTempoFocus());

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(24), dp(20), dp(28));
        page.setOnClickListener(view -> clearTempoFocus());
        scrollView.addView(page, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        TextView title = text("钢琴节拍器", 30, Color.rgb(20, 33, 61), Typeface.BOLD);
        page.addView(title);
        TextView subtitle = text("输入速度，选择拍号，第一拍自动重音。", 15, Color.rgb(91, 104, 129), Typeface.NORMAL);
        subtitle.setPadding(0, dp(5), 0, dp(18));
        page.addView(subtitle);

        page.addView(buildTempoCard());
        page.addView(space(14));
        page.addView(buildSignatureCard());
        page.addView(space(14));
        page.addView(buildBeatCard());
        page.addView(space(18));
        page.addView(buildPlayButton());
        return scrollView;
    }

    private LinearLayout buildTempoCard() {
        LinearLayout card = card();
        FrameLayout header = new FrameLayout(this);
        header.setPadding(0, 0, 0, dp(10));

        TextView bpmLabel = text("速度 BPM", 13, Color.rgb(91, 104, 129), Typeface.BOLD);
        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.CENTER_VERTICAL
        );
        header.addView(bpmLabel, labelParams);

        tempoMarkingText = text("", 13, Color.rgb(22, 101, 52), Typeface.BOLD);
        tempoMarkingText.setGravity(Gravity.CENTER);
        tempoMarkingText.setPadding(dp(10), dp(4), dp(10), dp(4));
        tempoMarkingText.setBackground(bordered(Color.rgb(240, 253, 244), Color.rgb(187, 247, 208), 14));
        FrameLayout.LayoutParams markingParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        header.addView(tempoMarkingText, markingParams);

        card.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(36)
        ));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView decreaseButton = tempoAdjustButton("-", -1);
        row.addView(decreaseButton, new LinearLayout.LayoutParams(
                dp(56),
                dp(84)
        ));

        bpmInput = new EditText(this);
        bpmInput.setSingleLine(true);
        bpmInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        bpmInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        bpmInput.setText(String.valueOf(bpm));
        bpmInput.setTextSize(48);
        bpmInput.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        bpmInput.setGravity(Gravity.CENTER);
        bpmInput.setMinHeight(0);
        bpmInput.setMinimumHeight(0);
        bpmInput.setPadding(0, 0, 0, 0);
        bpmInput.setSelectAllOnFocus(true);
        bpmInput.setTextColor(Color.rgb(20, 33, 61));
        bpmInput.setBackground(bordered(Color.WHITE, Color.rgb(203, 213, 225), 12));
        bpmInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                normalizeTempoInput();
                clearTempoFocus();
                return true;
            }
            return false;
        });
        bpmInput.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                normalizeTempoInput();
            }
        });
        bpmInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                applyTempoLive(editable.toString());
            }
        });
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                0,
                dp(84),
                1f
        );
        inputParams.setMargins(dp(8), 0, dp(8), 0);
        row.addView(bpmInput, inputParams);

        TextView increaseButton = tempoAdjustButton("+", 1);
        row.addView(increaseButton, new LinearLayout.LayoutParams(
                dp(56),
                dp(84)
        ));
        card.addView(row);
        return card;
    }

    private LinearLayout buildSignatureCard() {
        LinearLayout card = card();
        card.setOnClickListener(view -> clearTempoFocus());
        card.addView(label("拍号"));

        signatureSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                SIGNATURE_LABELS
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        signatureSpinner.setAdapter(adapter);
        signatureSpinner.setSelection(signatureIndex);
        signatureSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                signatureIndex = position;
                currentBeat = -1;
                updateEngineSettings();
                savePreferences();
                updateAllViews();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        card.addView(signatureSpinner, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
        ));
        return card;
    }

    private LinearLayout buildBeatCard() {
        LinearLayout card = card();
        card.setOnClickListener(view -> clearTempoFocus());
        card.addView(label("拍数"));

        beatText = text("", 22, Color.rgb(20, 33, 61), Typeface.BOLD);
        beatText.setGravity(Gravity.CENTER);
        beatText.setPadding(0, dp(8), 0, dp(10));
        card.addView(beatText);

        beatLights = new LinearLayout(this);
        beatLights.setGravity(Gravity.CENTER);
        beatLights.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(beatLights, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(64)
        ));

        statusText = text("", 14, Color.rgb(91, 104, 129), Typeface.BOLD);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, dp(10), 0, 0);
        card.addView(statusText);
        return card;
    }

    private TextView buildPlayButton() {
        playButton = text("", 22, Color.WHITE, Typeface.BOLD);
        playButton.setGravity(Gravity.CENTER);
        playButton.setTextSize(22);
        playButton.setBackground(rounded(Color.rgb(29, 127, 99), 14));
        playButton.setOnClickListener(view -> {
            clearTempoFocus();
            if (playing) {
                stopPlayback();
            } else if (applyTempoFromInput()) {
                startPlayback();
            }
        });
        UiKit.pressFeedback(playButton);
        playButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(68)
        ));
        return playButton;
    }

    private void applyTempoLive(String rawValue) {
        if (updatingBpmInput) {
            return;
        }
        String raw = rawValue == null ? "" : rawValue.trim();
        if (raw.length() == 0) {
            return;
        }
        int next;
        try {
            next = Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            return;
        }
        if (next < MIN_BPM || next > MAX_BPM) {
            return;
        }
        if (bpm != next) {
            bpm = next;
            updateEngineSettings();
            savePreferences();
            updateTempoMarking();
            updateBeatViews();
        }
    }

    private void changeTempoBy(int delta) {
        int next = clampBpm(bpm + delta);
        if (bpm == next) {
            return;
        }
        bpm = next;
        setBpmInputText(String.valueOf(bpm));
        updateEngineSettings();
        savePreferences();
        updateAllViews();
    }

    private void startPlayback() {
        playing = true;
        currentBeat = -1;
        engine.start(currentSettings());
        acquirePlaybackWakeLock();
        updateAllViews();
    }

    private void stopPlayback() {
        playing = false;
        currentBeat = -1;
        engine.stop();
        releasePlaybackWakeLock();
        updateAllViews();
    }

    private boolean applyTempoFromInput() {
        String raw = bpmInput == null ? String.valueOf(bpm) : bpmInput.getText().toString().trim();
        int next;
        try {
            next = Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            showTempoError();
            return false;
        }
        if (next < MIN_BPM || next > MAX_BPM) {
            showTempoError();
            return false;
        }
        bpm = next;
        updateEngineSettings();
        savePreferences();
        updateAllViews();
        return true;
    }

    private void normalizeTempoInput() {
        if (bpmInput == null) {
            return;
        }
        String raw = bpmInput.getText().toString().trim();
        if (raw.length() == 0) {
            setBpmInputText(String.valueOf(bpm));
            return;
        }
        int next;
        try {
            next = Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            showTempoError();
            return;
        }
        if (next < MIN_BPM || next > MAX_BPM) {
            showTempoError();
            return;
        }
        if (bpm != next) {
            bpm = next;
            updateEngineSettings();
            savePreferences();
            updateAllViews();
        } else if (!raw.equals(String.valueOf(bpm))) {
            setBpmInputText(String.valueOf(bpm));
        }
    }

    private void showTempoError() {
        Toast.makeText(this, "请输入 30 到 240 之间的 BPM", Toast.LENGTH_SHORT).show();
        if (bpmInput != null) {
            setBpmInputText(String.valueOf(bpm));
            bpmInput.selectAll();
        }
    }

    private void clearTempoFocus() {
        View current = getCurrentFocus();
        if (bpmInput != null) {
            bpmInput.clearFocus();
        }
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null && current != null) {
            manager.hideSoftInputFromWindow(current.getWindowToken(), 0);
        }
    }

    private void updateEngineSettings() {
        if (engine != null) {
            engine.update(currentSettings());
        }
    }

    private MetronomeEngine.Settings currentSettings() {
        return new MetronomeEngine.Settings(bpm, currentBeatsPerBar());
    }

    private int currentBeatsPerBar() {
        return SIGNATURE_BEATS[Math.max(0, Math.min(signatureIndex, SIGNATURE_BEATS.length - 1))];
    }

    private String currentSignature() {
        return SIGNATURE_LABELS[Math.max(0, Math.min(signatureIndex, SIGNATURE_LABELS.length - 1))];
    }

    private void updateAllViews() {
        if (bpmInput != null && !bpmInput.hasFocus()) {
            setBpmInputText(String.valueOf(bpm));
        }
        if (playButton != null) {
            playButton.setText(playing ? "停止" : "开始");
            playButton.setBackground(rounded(playing ? Color.rgb(184, 51, 58) : Color.rgb(29, 127, 99), 14));
        }
        updateTempoMarking();
        updateBeatViews();
    }

    private void updateTempoMarking() {
        if (tempoMarkingText != null) {
            tempoMarkingText.setText(tempoMarkingFor(bpm));
        }
    }

    private String tempoMarkingFor(int value) {
        int normalizedBpm = clampBpm(value);
        for (TempoMarking marking : TEMPO_MARKINGS) {
            if (normalizedBpm <= marking.maxBpm) {
                return marking.label();
            }
        }
        return TEMPO_MARKINGS[TEMPO_MARKINGS.length - 1].label();
    }

    private int clampBpm(int value) {
        return Math.max(MIN_BPM, Math.min(MAX_BPM, value));
    }

    private void updateBeatViews() {
        int beats = currentBeatsPerBar();
        if (beatLights != null && beatLights.getChildCount() != beats) {
            beatLights.removeAllViews();
            for (int i = 0; i < beats; i++) {
                TextView light = text(String.valueOf(i + 1), 15, Color.rgb(20, 33, 61), Typeface.BOLD);
                light.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(42), dp(42));
                params.setMargins(dp(4), 0, dp(4), 0);
                beatLights.addView(light, params);
            }
        }
        if (beatLights != null) {
            for (int i = 0; i < beatLights.getChildCount(); i++) {
                TextView light = (TextView) beatLights.getChildAt(i);
                boolean active = playing && currentBeat == i;
                int fill = active ? (i == 0 ? Color.rgb(255, 111, 34) : Color.rgb(37, 99, 235)) : Color.WHITE;
                int stroke = i == 0 ? Color.rgb(255, 111, 34) : Color.rgb(203, 213, 225);
                light.setTextColor(active ? Color.WHITE : Color.rgb(20, 33, 61));
                light.setBackground(oval(fill, stroke));
            }
        }
        if (beatText != null) {
            beatText.setText(playing && currentBeat >= 0
                    ? "第 " + (currentBeat + 1) + " 拍 / " + currentSignature()
                    : currentSignature() + " · 第一拍重音");
        }
        if (statusText != null) {
            statusText.setText(playing ? "息屏后会继续播放，音量跟随系统媒体音量。" : "默认 60 BPM，点击开始后第一拍声音更重。");
        }
    }

    private void acquirePlaybackWakeLock() {
        if (playbackWakeLock != null && !playbackWakeLock.isHeld()) {
            playbackWakeLock.acquire();
        }
    }

    private void releasePlaybackWakeLock() {
        if (playbackWakeLock != null && playbackWakeLock.isHeld()) {
            playbackWakeLock.release();
        }
    }

    private void loadPreferences() {
        bpm = preferences.getInt("bpm", 60);
        signatureIndex = preferences.getInt("signatureIndex", 2);
        if (signatureIndex < 0 || signatureIndex >= SIGNATURE_LABELS.length) {
            signatureIndex = 2;
        }
    }

    private void savePreferences() {
        preferences.edit()
                .putInt("bpm", bpm)
                .putInt("signatureIndex", signatureIndex)
                .apply();
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(bordered(Color.WHITE, Color.rgb(226, 232, 240), 14));
        return card;
    }

    private void setBpmInputText(String value) {
        if (bpmInput == null) {
            return;
        }
        updatingBpmInput = true;
        bpmInput.setText(value);
        updatingBpmInput = false;
    }

    private TextView label(String value) {
        TextView label = text(value, 13, Color.rgb(91, 104, 129), Typeface.BOLD);
        label.setPadding(0, 0, 0, dp(8));
        return label;
    }

    private TextView tempoAdjustButton(String value, int delta) {
        TextView button = text(value, 30, Color.rgb(20, 33, 61), Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(delta < 0 ? "降低速度" : "提高速度");
        button.setBackground(bordered(Color.rgb(248, 250, 252), Color.rgb(203, 213, 225), 12));
        button.setOnClickListener(view -> {
            clearTempoFocus();
            changeTempoBy(delta);
        });
        UiKit.pressFeedback(button);
        return button;
    }

    private TextView text(String value, float sp, int color, int style) {
        TextView textView = UiKit.text(this, value, sp, color, style);
        textView.setIncludeFontPadding(true);
        return textView;
    }

    private View space(int heightDp) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(1, dp(heightDp)));
        return view;
    }

    private GradientDrawable rounded(int color, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable bordered(int color, int strokeColor, float radiusDp) {
        GradientDrawable drawable = rounded(color, radiusDp);
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private GradientDrawable oval(int color, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private int dp(float value) {
        return UiKit.dp(this, value);
    }

    private static final class TempoMarking {
        private final int maxBpm;
        private final String name;
        private final String chineseName;

        private TempoMarking(int maxBpm, String name, String chineseName) {
            this.maxBpm = maxBpm;
            this.name = name;
            this.chineseName = chineseName;
        }

        private String label() {
            return name + " · " + chineseName;
        }
    }
}
