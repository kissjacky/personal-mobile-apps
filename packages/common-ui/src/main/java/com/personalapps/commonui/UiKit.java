package com.personalapps.commonui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public final class UiKit {
    public static final int INK = Color.rgb(20, 33, 61);
    public static final int MUTED = Color.rgb(100, 116, 139);
    public static final int PAPER = Color.rgb(255, 255, 255);
    public static final int PANEL = Color.rgb(248, 250, 252);
    public static final int AMBER = Color.rgb(245, 185, 66);
    public static final int GREEN = Color.rgb(29, 127, 99);
    public static final int RED = Color.rgb(184, 51, 58);

    private UiKit() {
    }

    public static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable rounded(int color, float radiusDp, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    public static GradientDrawable bordered(int color, int strokeColor, float radiusDp, Context context) {
        GradientDrawable drawable = rounded(color, radiusDp, context);
        drawable.setStroke(dp(context, 1), strokeColor);
        return drawable;
    }

    public static GradientDrawable oval(int color, int strokeColor, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setStroke(dp(context, 1), strokeColor);
        return drawable;
    }

    public static GradientDrawable pageBackground() {
        return new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        Color.rgb(247, 251, 255),
                        Color.rgb(241, 245, 249),
                        Color.rgb(255, 250, 240)
                }
        );
    }

    public static TextView text(Context context, String value, float sp, int color, int style) {
        TextView textView = new TextView(context);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setIncludeFontPadding(true);
        if (style != Typeface.NORMAL) {
            textView.setTypeface(Typeface.DEFAULT, style);
        }
        return textView;
    }

    public static Button button(Context context, String value) {
        Button button = new Button(context);
        button.setText(value);
        button.setTextSize(14);
        button.setTextColor(INK);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setMinHeight(dp(context, 42));
        button.setMinimumHeight(dp(context, 42));
        button.setPadding(dp(context, 8), 0, dp(context, 8), 0);
        styleButton(button, false);
        return button;
    }

    public static void styleButton(Button button, boolean selected) {
        Context context = button.getContext();
        button.setTextColor(selected ? Color.WHITE : INK);
        button.setBackground(bordered(
                selected ? INK : PAPER,
                selected ? INK : Color.argb(34, 20, 33, 61),
                8,
                context
        ));
        if (Build.VERSION.SDK_INT >= 21) {
            button.setStateListAnimator(null);
        }
    }

    public static void pressFeedback(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(50).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                    break;
                default:
                    break;
            }
            return false;
        });
    }
}
