package com.bukhmastov.cdoitmo.util;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.view.View;

public interface NotificationMessage {

    int LENGTH_MOMENTUM = 600;
    int LENGTH_SHORT = 1500;
    int LENGTH_LONG = 2750;

    void showUpdateTime(Activity activity, long time);

    void showUpdateTime(Activity activity, long time, int duration);

    void showUpdateTime(Activity activity, long time, int duration, boolean force);

    void showUpdateTime(Activity activity, @IdRes int layout, long time);

    void showUpdateTime(Activity activity, @IdRes int layout, long time, int duration);

    void showUpdateTime(Activity activity, @IdRes int layout, long time, int duration, boolean force);

    void toast(final Context context, @StringRes final int resId);

    void toast(final Context context, final String text);

    void snackBar(Activity activity, String text);

    void snackBar(Activity activity, String text, int duration);

    void snackBar(Activity activity, @IdRes int layout, String text);

    void snackBar(Activity activity, @IdRes int layout, String text, int duration);

    void snackBar(Activity activity, String text, String action, View.OnClickListener onClickListener);

    void snackBar(Activity activity, String text, String action, int duration, View.OnClickListener onClickListener);

    void snackBar(Activity activity, @IdRes int layout, String text, String action, int duration, View.OnClickListener onClickListener);

    void snackBar(final Context context, final View layout, final String text, final int duration);

    void snackBar(final Context context, final View layout, final String text, final String action, final View.OnClickListener onClickListener);

    void snackBar(final Context context, final View layout, final String text, final String action, final int duration, final View.OnClickListener onClickListener);

    void snackBarOffline(final Activity activity);
}
