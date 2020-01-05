package com.bukhmastov.cdoitmo.activity;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.MotionEvent;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bukhmastov.cdoitmo.view.OnSwipeTouchListener;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    @CallSuper
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initSwipeListener();
    }

    @Override
    @CallSuper
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        initSwipeListener();
    }

    @Override
    @CallSuper
    protected void onDestroy() {
        destroySwipeListener();
        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean isDrawerExists = getDrawerLayout() != null;
        boolean isShouldListenForDrawerSwipe = shouldListenForDrawerSwipe();
        boolean isListeningForSwipe = swipeListener != null;
        if (
            isDrawerExists &&
            isShouldListenForDrawerSwipe &&
            isListeningForSwipe &&
            swipeListener.onTouch(null, event)
        ) {
            return true;
        }
        return super.dispatchTouchEvent(event);
    }

    private void initSwipeListener() {
        swipeListener = new OnSwipeTouchListener(this) {
            @Override
            public boolean onSwipeLeft2Right() {
                DrawerLayout drawerLayout = getDrawerLayout();
                if (drawerLayout == null) {
                    return false;
                }
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            }
        };
    }

    private void destroySwipeListener() {
        swipeListener = null;
    }

    public DrawerLayout getDrawerLayout() { return null; }

    public boolean shouldListenForDrawerSwipe() { return getDrawerLayout() != null; }

    private OnSwipeTouchListener swipeListener;
}
