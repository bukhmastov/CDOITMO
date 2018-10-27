package com.bukhmastov.cdoitmo.adapter.rva;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.bukhmastov.cdoitmo.R;

public class RecyclerViewOnScrollListener extends RecyclerView.OnScrollListener {

    private static final int HIDE_MINIMUM = 200;
    private static final int HIDE_THRESHOLD = 20;
    private int scrolled = 0;
    private int scrolledDistance = 0;
    private boolean controlsVisible = true;
    private View container;

    public RecyclerViewOnScrollListener(View container) {
        this.container = container;
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        scrolled += dy;
        if (scrolled < HIDE_MINIMUM) {
            if (!controlsVisible) {
                show();
                controlsVisible = true;
            }
        } else {
            if (scrolledDistance > HIDE_THRESHOLD && controlsVisible) {
                hide();
                controlsVisible = false;
                scrolledDistance = 0;
            } else if (scrolledDistance < -HIDE_THRESHOLD && !controlsVisible) {
                show();
                controlsVisible = true;
                scrolledDistance = 0;
            }
            if ((controlsVisible && dy > 0) || (!controlsVisible && dy < 0)) {
                scrolledDistance += dy;
            }
        }
    }

    private void show() {
        if (container != null) {
            ViewGroup panel = container.findViewById(R.id.top_panel);
            if (panel != null) {
                panel.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1)).start();
            }
        }
    }
    private void hide() {
        if (container != null) {
            ViewGroup panel = container.findViewById(R.id.top_panel);
            if (panel != null) {
                panel.animate().translationY(-panel.getHeight()).setInterpolator(new AccelerateInterpolator(1)).start();
            }
        }
    }
}
