package com.bukhmastov.cdoitmo.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;

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
    public View getView(int position, View view, @NonNull ViewGroup parent) { //?attr/textColorPassed
        LayoutInflater inflater = context.getLayoutInflater();
        HashMap<String, String> user = users.get(position);
        View rowView = inflater.inflate(R.layout.listview_rating_list, null, true);
        TypedValue typedValue = new TypedValue();
        if(Objects.equals(user.get("is_me"), "1")){
            ViewGroup vg = ((ViewGroup) rowView.findViewById(R.id.lvrl_number_layout));
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.triangle_mark_layout, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
            this.context.getTheme().resolveAttribute(R.attr.colorPrimaryOpacity, typedValue, true);
            rowView.setBackgroundColor(typedValue.data);
            rowView.findViewById(R.id.lvrl_layout).setPadding(32, 0, 16, 0);
        }
        ((TextView) rowView.findViewById(R.id.lvrl_number)).setText(user.get("number"));
        ((TextView) rowView.findViewById(R.id.lvrl_fio)).setText(user.get("fio"));
        ((TextView) rowView.findViewById(R.id.lvrl_meta)).setText(user.get("meta"));
        if(!Objects.equals(user.get("change"), "none")){
            TextView lvrl_delta = (TextView) rowView.findViewById(R.id.lvrl_delta);
            lvrl_delta.setText(user.get("delta"));
            switch (user.get("change")){
                case "up":
                    this.context.getTheme().resolveAttribute(R.attr.textColorPassed, typedValue, true);
                    lvrl_delta.setTextColor(typedValue.data);
                    break;
                case "down":
                    this.context.getTheme().resolveAttribute(R.attr.textColorDegrade, typedValue, true);
                    lvrl_delta.setTextColor(typedValue.data);
                    break;
            }
        }
        return rowView;
    }
}
