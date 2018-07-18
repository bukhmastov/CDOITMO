package com.bukhmastov.cdoitmo.util.impl;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.Color;

public class NotificationMessageImpl implements NotificationMessage {

    //@Inject
    private Thread thread = Thread.instance();
    //@Inject
    private Time time = Time.instance();

    @Override
    public void showUpdateTime(Activity activity, long time) {
        showUpdateTime(activity, time, LENGTH_MOMENTUM, false);
    }

    @Override
    public void showUpdateTime(Activity activity, long time, int duration) {
        showUpdateTime(activity, time, LENGTH_MOMENTUM, false);
    }

    @Override
    public void showUpdateTime(Activity activity, long time, int duration, boolean force) {
        showUpdateTime(activity, android.R.id.content, time, duration, force);
    }

    @Override
    public void showUpdateTime(Activity activity, @IdRes int layout, long time) {
        showUpdateTime(activity, layout, time, LENGTH_MOMENTUM, false);
    }

    @Override
    public void showUpdateTime(Activity activity, @IdRes int layout, long time, int duration) {
        showUpdateTime(activity, layout, time, duration, false);
    }

    @Override
    public void showUpdateTime(Activity activity, @IdRes int layout, long t, int duration, boolean force) {
        String message = time.getUpdateTime(activity, t);
        int shift = (int) ((time.getCalendar().getTimeInMillis() - t) / 1000L);
        if (force || shift > 4) {
            snackBar(activity, layout, activity.getString(R.string.update_date) + " " + message, duration);
        }
    }

    @Override
    public void toast(final Context context, @StringRes final int resId) {
        toast(context, context.getString(resId));
    }

    @Override
    public void toast(final Context context, final String text) {
        thread.runOnUI(() -> {
            if (context == null) {
                return;
            }
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void snackBar(Activity activity, String text) {
        snackBar(activity, text, LENGTH_LONG);
    }

    @Override
    public void snackBar(Activity activity, String text, int duration) {
        snackBar(activity, android.R.id.content, text, duration);
    }

    @Override
    public void snackBar(Activity activity, @IdRes int layout, String text) {
        snackBar(activity, layout, text, LENGTH_LONG);
    }

    @Override
    public void snackBar(Activity activity, @IdRes int layout, String text, int duration) {
        if (activity == null) {
            return;
        }
        snackBar(activity, activity.findViewById(layout), text, duration);
    }

    @Override
    public void snackBar(Activity activity, String text, String action, View.OnClickListener onClickListener) {
        snackBar(activity, text, action, LENGTH_LONG, onClickListener);
    }

    @Override
    public void snackBar(Activity activity, String text, String action, int duration, View.OnClickListener onClickListener) {
        if (activity == null) {
            return;
        }
        snackBar(activity, activity.findViewById(android.R.id.content), text, action, duration, onClickListener);
    }

    @Override
    public void snackBar(Activity activity, @IdRes int layout, String text, String action, int duration, View.OnClickListener onClickListener) {
        if (activity == null) {
            return;
        }
        snackBar(activity, activity.findViewById(layout), text, action, duration, onClickListener);
    }

    @Override
    public void snackBar(final Context context, final View layout, final String text, final int duration) {
        snackBar(context, layout, text, null, duration, null);
    }

    @Override
    public void snackBar(final Context context, final View layout, final String text, final String action, final View.OnClickListener onClickListener) {
        snackBar(context, layout, text, action, LENGTH_LONG, onClickListener);
    }

    @Override
    public void snackBar(final Context context, final View layout, final String text, final String action, final int duration, final View.OnClickListener onClickListener) {
        thread.runOnUI(() -> {
            if (layout != null) {
                Snackbar snackbar = Snackbar.make(layout, text, duration);
                snackbar.getView().setBackgroundColor(Color.resolve(context, R.attr.colorBackgroundSnackBar));
                if (action != null) snackbar.setAction(action, onClickListener);
                snackbar.show();
            }
        });
    }

    @Override
    public void snackBarOffline(final Activity activity) {
        if (App.OFFLINE_MODE) {
            snackBar(activity, activity.getString(R.string.offline_mode_on));
        }
    }
}
