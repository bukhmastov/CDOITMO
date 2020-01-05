package com.bukhmastov.cdoitmo.activity.presenter.impl;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
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
        screens.add(new Screen(R.string.intro_title_6, R.string.intro_desc_6, R.drawable.image_intro_6, "#ffee58", "#311b92"));
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
        activity.findViewById(R.id.container).setOnTouchListener(new OnSwipeTouchListener(activity, true) {
            @Override
            public boolean onSwipeRight2Left() {
                next();
                return true;
            }
            @Override
            public boolean onSwipeLeft2Right() {
                previous();
                return true;
            }
        });
    }

    private void next() {
        try {
            if (++position >= screens.size()) {
                close();
                return;
            }
            Screen screenPrevious = screens.get(position > 0 ? position - 1 : position);
            Screen screenCurrent = screens.get(position);
            showScreen(screenPrevious, screenCurrent);
        } catch (Exception e) {
            log.exception(e);
            close();
        }
    }

    private void previous() {
        try {
            if (position < 1) {
                return;
            }
            position--;
            Screen screenPrevious = screens.get(position < screens.size() - 1 ? position + 1 : position);
            Screen screenCurrent = screens.get(position);
            showScreen(screenPrevious, screenCurrent);
        } catch (Exception e) {
            log.exception(e);
            close();
        }
    }

    private void showScreen(Screen screenPrevious, Screen screenCurrent) {
        View container = activity.findViewById(R.id.container);
        ViewGroup indicators = activity.findViewById(R.id.indicators);
        ImageView imageView = activity.findViewById(R.id.image_view);
        TextView titleView = activity.findViewById(R.id.title_view);
        TextView descView = activity.findViewById(R.id.desc_view);
        titleView.setText(screenCurrent.title);
        descView.setText(screenCurrent.desc);
        imageView.setImageResource(screenCurrent.image);
        animateColor(getColor(screenPrevious.colorBackground), getColor(screenCurrent.colorBackground), valueAnimator -> container.setBackgroundTintList(ColorStateList.valueOf((int) valueAnimator.getAnimatedValue())));
        animateColor(getColor(screenPrevious.colorAccent), getColor(screenCurrent.colorAccent), valueAnimator -> titleView.setTextColor(ColorStateList.valueOf((int) valueAnimator.getAnimatedValue())));
        for (int i = 0; i < indicators.getChildCount(); i++) {
            final TextView indicator = (TextView) indicators.getChildAt(i);
            animateColor(indicator.getTextColors().getDefaultColor(), getColor(i == position ? colorIndicatorActive : colorIndicatorInActive), valueAnimator -> indicator.setTextColor(ColorStateList.valueOf((int) valueAnimator.getAnimatedValue())));
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
