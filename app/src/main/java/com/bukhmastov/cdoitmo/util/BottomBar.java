package com.bukhmastov.cdoitmo.util;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.util.singleton.Color;

//TODO interface - impl
public class BottomBar {

    private static final String TAG = "BottomBar";

    public static final int LENGTH_MOMENTUM = 600;
    public static final int LENGTH_SHORT = 1500;
    public static final int LENGTH_LONG = 2750;

    //@Inject
    //TODO interface - impl: remove static
    private static Thread thread = Thread.instance();

    public static void showUpdateTime(Activity activity, long time) {
        showUpdateTime(activity, time, LENGTH_MOMENTUM, false);
    }

    public static void showUpdateTime(Activity activity, long time, int duration) {
        showUpdateTime(activity, time, LENGTH_MOMENTUM, false);
    }

    public static void showUpdateTime(Activity activity, long time, int duration, boolean force) {
        showUpdateTime(activity, android.R.id.content, time, duration, force);
    }

    public static void showUpdateTime(Activity activity, @IdRes int layout, long time) {
        showUpdateTime(activity, layout, time, LENGTH_MOMENTUM, false);
    }

    public static void showUpdateTime(Activity activity, @IdRes int layout, long time, int duration) {
        showUpdateTime(activity, layout, time, duration, false);
    }

    public static void showUpdateTime(Activity activity, @IdRes int layout, long time, int duration, boolean force) {
        String message = Time.getUpdateTime(activity, time);
        int shift = (int) ((Time.getCalendar().getTimeInMillis() - time) / 1000L);
        if (force || shift > 4) {
            snackBar(activity, layout, activity.getString(R.string.update_date) + " " + message, duration);
        }
    }

    public static void toast(final Context context, @StringRes final int resId) {
        toast(context, context.getString(resId));
    }

    public static void toast(final Context context, final String text) {
        thread.runOnUI(() -> {
            if (context == null) {
                return;
            }
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        });
    }

    public static void snackBar(Activity activity, String text) {
        snackBar(activity, text, LENGTH_LONG);
    }

    public static void snackBar(Activity activity, String text, int duration) {
        snackBar(activity, android.R.id.content, text, duration);
    }

    public static void snackBar(Activity activity, @IdRes int layout, String text) {
        snackBar(activity, layout, text, LENGTH_LONG);
    }

    public static void snackBar(Activity activity, @IdRes int layout, String text, int duration) {
        if (activity == null) {
            return;
        }
        snackBar(activity, activity.findViewById(layout), text, duration);
    }

    public static void snackBar(Activity activity, String text, String action, View.OnClickListener onClickListener) {
        snackBar(activity, text, action, LENGTH_LONG, onClickListener);
    }

    public static void snackBar(Activity activity, String text, String action, int duration, View.OnClickListener onClickListener) {
        if (activity == null) {
            return;
        }
        snackBar(activity, activity.findViewById(android.R.id.content), text, action, duration, onClickListener);
    }

    public static void snackBar(Activity activity, @IdRes int layout, String text, String action, int duration, View.OnClickListener onClickListener) {
        if (activity == null) {
            return;
        }
        snackBar(activity, activity.findViewById(layout), text, action, duration, onClickListener);
    }

    public static void snackBar(final Context context, final View layout, final String text, final int duration) {
        snackBar(context, layout, text, null, duration, null);
    }

    public static void snackBar(final Context context, final View layout, final String text, final String action, final View.OnClickListener onClickListener) {
        snackBar(context, layout, text, action, LENGTH_LONG, onClickListener);
    }

    public static void snackBar(final Context context, final View layout, final String text, final String action, final int duration, final View.OnClickListener onClickListener) {
        thread.runOnUI(() -> {
            if (layout != null) {
                Snackbar snackbar = Snackbar.make(layout, text, duration);
                snackbar.getView().setBackgroundColor(Color.resolve(context, R.attr.colorBackgroundSnackBar));
                if (action != null) snackbar.setAction(action, onClickListener);
                snackbar.show();
            }
        });
    }

    public static void snackBarOffline(final Activity activity) {
        if (App.OFFLINE_MODE) {
            snackBar(activity, activity.getString(R.string.offline_mode_on));
        }
    }
}
