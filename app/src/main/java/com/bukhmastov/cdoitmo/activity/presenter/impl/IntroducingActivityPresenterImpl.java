package com.bukhmastov.cdoitmo.activity.presenter.impl;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.IntroducingActivity;
import com.bukhmastov.cdoitmo.activity.presenter.IntroducingActivityPresenter;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.view.OnSwipeTouchListener;

import java.util.ArrayList;

import javax.inject.Inject;

public class IntroducingActivityPresenterImpl implements IntroducingActivityPresenter {

    private static final String TAG = "IntroducingActivity";
    private static final String colorIndicatorActive = "#FFFFFFFF";
    private static final String colorIndicatorInActive = "#88FFFFFF";
    private IntroducingActivity activity = null;
    private ArrayList<Screen> screens = new ArrayList<>();
    private int position = -1;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public IntroducingActivityPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void setActivity(@NonNull IntroducingActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.runOnUI(() -> {
            try {
                log.i(TAG, "Activity created");
                firebaseAnalyticsProvider.logCurrentScreen(activity);
                activity.findViewById(R.id.content).setPadding(0, getStatusBarHeight(), 0, 0);
                initScreens();
                initInterface();
                initControls();
                next();
            } catch (Exception e) {
                log.exception(e);
                close();
            }
        });
    }

    @Override
    public void onDestroy() {
        log.v(TAG, "Fragment destroyed");
    }

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

    private void initScreens() {
        position = -1;
        screens.add(new Screen(R.string.intro_title_1, R.string.intro_desc_1, R.drawable.image_intro_1, "#9c27b0", "#cddc39"));
        screens.add(new Screen(R.string.intro_title_2, R.string.intro_desc_2, R.drawable.image_intro_2, "#2196f3", "#ff9800"));
        screens.add(new Screen(R.string.intro_title_3, R.string.intro_desc_3, R.drawable.image_intro_3, "#cddc39", "#673ab7"));
        screens.add(new Screen(R.string.intro_title_4, R.string.intro_desc_4, R.drawable.image_intro_4, "#00bcd4", "#cddc39"));
        screens.add(new Screen(R.string.intro_title_5, R.string.intro_desc_5, R.drawable.image_intro_5, "#1946ba", "#ff5722"));
    }

    private void initInterface() {
        ViewGroup indicators = activity.findViewById(R.id.indicators);
        int color = getColor(colorIndicatorInActive);
        for (int i = 0; i < screens.size(); i++) {
            TextView indicator = (TextView) activity.inflate(R.layout.layout_introducing_indicator);
            indicator.setTextColor(color);
            indicators.addView(indicator);
        }
    }

    private void initControls() {
        activity.findViewById(R.id.skip).setOnClickListener(view -> close());
        activity.findViewById(R.id.next).setOnClickListener(view -> next());
        activity.findViewById(R.id.container).setOnTouchListener(new OnSwipeTouchListener(activity) {
            @Override
            public void onSwipeLeft2Right() {
                next();
            }
        });
    }

    private void next() {
        try {
            if (++position < screens.size()) {
                Screen previous_screen = screens.get(position > 0 ? position - 1 : position);
                Screen screen = screens.get(position);
                final View container = activity.findViewById(R.id.container);
                final ViewGroup indicators = activity.findViewById(R.id.indicators);
                final ImageView image_view = activity.findViewById(R.id.image_view);
                final TextView title_view = activity.findViewById(R.id.title_view);
                final TextView desc_view = activity.findViewById(R.id.desc_view);
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
            log.exception(e);
            close();
        }
    }

    private void close() {
        activity.finish();
    }

    private void animateColor(final int from, final int to, final ValueAnimator.AnimatorUpdateListener animatorUpdateListener) {
        try {
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), from, to);
            colorAnimation.setDuration(300);
            colorAnimation.addUpdateListener(animatorUpdateListener);
            colorAnimation.start();
        } catch (Exception e) {
            log.exception(e);
            close();
        }
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = activity.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private int getColor(String color) {
        return Color.parseColor(color);
    }
}
