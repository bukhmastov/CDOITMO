package com.bukhmastov.cdoitmo.view;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Log;

import javax.inject.Inject;

public class OnSwipeTouchListener implements View.OnTouchListener {

    private final GestureDetector gestureDetector;
    @Inject Log log;

    public OnSwipeTouchListener(Context context) {
        this(context, false);
    }

    public OnSwipeTouchListener(Context context, boolean interceptDownEvent) {
        this(context, 100, 100, interceptDownEvent);
    }

    public OnSwipeTouchListener(Context context, int swipeThreshold, int swipeVelocityThreshold, boolean interceptDownEvent) {
        AppComponentProvider.getComponent().inject(this);
        gestureDetector = new GestureDetector(context, new SwipeListener(swipeThreshold, swipeVelocityThreshold, interceptDownEvent));
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (event != null && view != null && event.getAction() == MotionEvent.ACTION_UP) {
            view.performClick();
        }
        return gestureDetector.onTouchEvent(event);
    }

    private final class SwipeListener extends GestureDetector.SimpleOnGestureListener {

        private final int swipeThreshold;
        private final int swipeVelocityThreshold;
        private final boolean interceptDownEvent;

        public SwipeListener(int swipeThreshold, int swipeVelocityThreshold, boolean interceptDownEvent) {
            this.swipeThreshold = swipeThreshold;
            this.swipeVelocityThreshold = swipeVelocityThreshold;
            this.interceptDownEvent = interceptDownEvent;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return interceptDownEvent;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();
                float diffXabs = Math.abs(diffX);
                float diffYabs = Math.abs(diffY);
                float velocityXabs = Math.abs(velocityX);
                float velocityYabs = Math.abs(velocityY);
                if (diffXabs > diffYabs) {
                    if (diffXabs > swipeThreshold && velocityXabs > swipeVelocityThreshold) {
                        if (diffX > 0) {
                            return onSwipeLeft2Right();
                        } else {
                            return onSwipeRight2Left();
                        }
                    }
                } else if (diffYabs > swipeThreshold && velocityYabs > swipeVelocityThreshold) {
                    if (diffY > 0) {
                        return onSwipeBottom2Top();
                    } else {
                        return onSwipeTop2Bottom();
                    }
                }
            } catch (Exception e) {
                log.exception(e);
            }
            return false;
        }
    }

    public boolean onSwipeLeft2Right() { return false; }
    public boolean onSwipeRight2Left() { return false; }
    public boolean onSwipeTop2Bottom() { return false; }
    public boolean onSwipeBottom2Top() { return false; }
}
