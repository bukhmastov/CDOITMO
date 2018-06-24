package com.bukhmastov.cdoitmo.object.preference;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.util.Log;

public class PreferenceHeader {

    protected static final String TAG = "PreferenceHeader";
    public @DrawableRes final int icon;
    public @StringRes final int title;
    public final Class fragment;

    public PreferenceHeader(@StringRes int title, @DrawableRes int icon, Class fragment) {
        this.title = title;
        this.icon = icon;
        this.fragment = fragment;
    }

    @Nullable
    public static View getView(final ConnectedActivity activity, final PreferenceHeader preference) {
        final View header = inflate(activity, R.layout.preference_header);
        if (header == null) {
            return null;
        }
        ((ImageView) header.findViewById(R.id.preference_header_icon)).setImageResource(preference.icon);
        ((TextView) header.findViewById(R.id.preference_header_title)).setText(preference.title);
        header.findViewById(R.id.preference_header).setOnClickListener(v -> activity.openFragment(ConnectedActivity.TYPE.STACKABLE, preference.fragment, null));
        return header;
    }

    @Nullable
    protected static View inflate(final Context context, @LayoutRes final int layout) throws InflateException {
        if (context == null) {
            Log.e(TAG, "Failed to inflate layout, context is null");
            return null;
        }
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            Log.e(TAG, "Failed to inflate layout, inflater is null");
            return null;
        }
        return inflater.inflate(layout, null);
    }
}
