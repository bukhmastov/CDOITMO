package com.bukhmastov.cdoitmo.activity;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.view.OnSwipeTouchListener;

import java.util.ArrayList;

public class IntroducingActivity extends ConnectedActivity {

    private static final String TAG = "IntroducingActivity";
    private final Activity activity = this;
    private final ArrayList<Screen> screens = new ArrayList<>();
    private int position = -1;
    private static final String colorIndicatorActive = "#FFFFFFFF";
    private static final String colorIndicatorInActive = "#88FFFFFF";

    private class Screen {
        public final String title;
        public final String desc;
        public @DrawableRes
        final int image;
        public final String colorBackground;
        public final String colorAccent;
        public Screen(@StringRes int title, @StringRes int desc, @DrawableRes int image, String colorBackground, String colorAccent) {
            this.title = activity.getString(title);
            this.desc = activity.getString(desc);
            this.image = image;
            this.colorBackground = colorBackground;
            this.colorAccent = colorAccent;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        switch (Static.getAppTheme(activity)) {
            case "light":
            default: setTheme(R.style.AppTheme_TransparentStatusBar); break;
            case "dark": setTheme(R.style.AppTheme_Dark_TransparentStatusBar); break;
            case "white": setTheme(R.style.AppTheme_White_TransparentStatusBar); break;
            case "black": setTheme(R.style.AppTheme_Black_TransparentStatusBar); break;
        }
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");
        FirebaseAnalyticsProvider.logCurrentScreen(this);
        setContentView(R.layout.activity_introducing);
        findViewById(R.id.content).setPadding(0, getStatusBarHeight(), 0, 0);
        initScreens();
        initInterface();
        initControls();
        next();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Fragment destroyed");
    }

    @Override
    protected int getRootViewId() {
        return R.id.content;
    }

    private void initScreens() {
        try {
            position = -1;
            screens.add(new Screen(R.string.intro_title_1, R.string.intro_desc_1, R.drawable.image_intro_1, "#9c27b0", "#cddc39"));
            screens.add(new Screen(R.string.intro_title_2, R.string.intro_desc_2, R.drawable.image_intro_2, "#2196f3", "#ff9800"));
            screens.add(new Screen(R.string.intro_title_3, R.string.intro_desc_3, R.drawable.image_intro_3, "#cddc39", "#673ab7"));
            screens.add(new Screen(R.string.intro_title_4, R.string.intro_desc_4, R.drawable.image_intro_4, "#00bcd4", "#cddc39"));
            screens.add(new Screen(R.string.intro_title_5, R.string.intro_desc_5, R.drawable.image_intro_5, "#1946ba", "#ff5722"));
        } catch (Exception e) {
            Static.error(e);
            close();
        }
    }
    private void initInterface() {
        try {
            ViewGroup indicators = findViewById(R.id.indicators);
            int color = getColor(colorIndicatorInActive);
            for (int i = 0; i < screens.size(); i++) {
                TextView indicator = (TextView) inflate(R.layout.layout_introducing_indicator);
                indicator.setTextColor(color);
                indicators.addView(indicator);
            }
        } catch (Exception e) {
            Static.error(e);
            close();
        }
    }
    private void initControls() {
        try {
            findViewById(R.id.skip).setOnClickListener(view -> close());
            findViewById(R.id.next).setOnClickListener(view -> next());
            findViewById(R.id.container).setOnTouchListener(new OnSwipeTouchListener(this) {
                @Override
                public void onSwipeLeft2Right() {
                    next();
                }
            });
        } catch (Exception e) {
            Static.error(e);
            close();
        }
    }

    private void next() {
        try {
            if (++position < screens.size()) {
                Screen previous_screen = screens.get(position > 0 ? position - 1 : position);
                Screen screen = screens.get(position);
                final View container = findViewById(R.id.container);
                final ViewGroup indicators = findViewById(R.id.indicators);
                final ImageView image_view = findViewById(R.id.image_view);
                final TextView title_view = findViewById(R.id.title_view);
                final TextView desc_view = findViewById(R.id.desc_view);
                title_view.setText(screen.title);
                desc_view.setText(screen.desc);
                image_view.setImageResource(screen.image);
                animateColor(getColor(previous_screen.colorBackground), getColor(screen.colorBackground), valueAnimator -> container.setBackgroundTintList(ColorStateList.valueOf((int) valueAnimator.getAnimatedValue())));
                animateColor(getColor(previous_screen.colorAccent), getColor(screen.colorAccent), valueAnimator -> title_view.setTextColor(ColorStateList.valueOf((int) valueAnimator.getAnimatedValue())));
                for (int i = 0; i < indicators.getChildCount(); i++) {
                    final TextView indicator = (TextView) indicators.getChildAt(i);
                    animateColor(indicator.getTextColors().getDefaultColor(), getColor(i == position ? colorIndicatorActive : colorIndicatorInActive), valueAnimator -> indicator.setTextColor(ColorStateList.valueOf((int) valueAnimator.getAnimatedValue())));
                }
            } else {
                close();
            }
        } catch (Exception e) {
            Static.error(e);
            close();
        }
    }
    private void close() {
        finish();
    }

    private void animateColor(final int from, final int to, final ValueAnimator.AnimatorUpdateListener animatorUpdateListener) {
        try {
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), from, to);
            colorAnimation.setDuration(300);
            colorAnimation.addUpdateListener(animatorUpdateListener);
            colorAnimation.start();
        } catch (Exception e) {
            Static.error(e);
            close();
        }
    }
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
    private int getColor(String color) {
        return Color.parseColor(color);
    }
    private View inflate(int layout) throws InflateException {
        return ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layout, null);
    }
}
