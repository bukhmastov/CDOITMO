package com.bukhmastov.cdoitmo.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.utils.Static;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class RatingTopListView extends ArrayAdapter<HashMap<String, String>> {

    private final Activity context;
    private final ArrayList<HashMap<String, String>> users;

    public RatingTopListView(Activity context, ArrayList<HashMap<String, String>> users) {
        super(context, R.layout.listview_rating_list, users);
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) { //?attr/textColorPassed
        try {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.listview_rating_list, parent, false);
            HashMap<String, String> user = users.get(position);
            if (Objects.equals(user.get("is_me"), "1")) {
                ViewGroup vg = convertView.findViewById(R.id.lvrl_number_layout);
                if (vg != null) {
                    vg.removeAllViews();
                    vg.addView(((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.triangle_mark_layout, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                }
                convertView.setBackgroundColor(Static.resolveColor(context, R.attr.colorPrimaryOpacity));
                convertView.findViewById(R.id.lvrl_layout).setPaddingRelative(32, 0, 16, 0);
            }
            ((TextView) convertView.findViewById(R.id.lvrl_number)).setText(user.get("number"));
            ((TextView) convertView.findViewById(R.id.lvrl_fio)).setText(user.get("fio"));
            ((TextView) convertView.findViewById(R.id.lvrl_meta)).setText(user.get("meta"));
            if (!Objects.equals(user.get("change"), "none")) {
                TextView lvrl_delta = convertView.findViewById(R.id.lvrl_delta);
                if (lvrl_delta != null) {
                    lvrl_delta.setText(user.get("delta"));
                    switch (user.get("change")) {
                        case "up": lvrl_delta.setTextColor(Static.resolveColor(context, R.attr.textColorPassed)); break;
                        case "down": lvrl_delta.setTextColor(Static.resolveColor(context, R.attr.textColorDegrade)); break;
                    }
                }
            }
            return convertView;
        } catch (Exception e) {
            Static.error(e);
            return super.getView(position, convertView, parent);
        }
    }
}
