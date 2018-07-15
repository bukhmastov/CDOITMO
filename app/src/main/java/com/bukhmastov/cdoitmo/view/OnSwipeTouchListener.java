package com.bukhmastov.cdoitmo.view;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.bukhmastov.cdoitmo.util.Log;

@SuppressWarnings("EmptyMethod")
public class OnSwipeTouchListener implements View.OnTouchListener {

    private final GestureDetector gestureDetector;

    //@Inject
    private Log log = Log.instance();

    public OnSwipeTouchListener (Context context) {
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event != null && v != null && event.getAction() == MotionEvent.ACTION_UP) {
            v.performClick();
        }
        return gestureDetector.onTouchEvent(event);
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight2Left();
                        } else {
                            onSwipeLeft2Right();
                        }
                        result = true;
                    }
                } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom2Top();
                    } else {
                        onSwipeTop2Bottom();
                    }
                    result = true;
                }
            } catch (Exception e) {
                log.exception(e);
            }
            return result;
        }
    }

    public void onSwipeRight2Left() {}
    public void onSwipeLeft2Right() {}
    public void onSwipeTop2Bottom() {}
    public void onSwipeBottom2Top() {}
}
