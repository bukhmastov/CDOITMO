package com.bukhmastov.cdoitmo.objects.preferences;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;

public class PreferenceHeader {
    public @DrawableRes int icon;
    public @StringRes int title;
    public Class fragment;
    public PreferenceHeader(@StringRes int title, @DrawableRes int icon, Class fragment) {
        this.title = title;
        this.icon = icon;
        this.fragment = fragment;
    }
    public static View getView(final ConnectedActivity activity, final PreferenceHeader preference) {
        View header = inflate(activity, R.layout.layout_preference_header);
        ((ImageView) header.findViewById(R.id.preference_header_icon)).setImageResource(preference.icon);
        ((TextView) header.findViewById(R.id.preference_header_title)).setText(preference.title);
        header.findViewById(R.id.preference_header).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.openFragment(ConnectedActivity.TYPE.stackable, preference.fragment, null);
            }
        });
        return header;
    }
    private static View inflate(final Context context, final int layoutId) throws InflateException {
        return ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}
