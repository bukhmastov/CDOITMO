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

public class RatingListView extends ArrayAdapter<HashMap<String, String>> {

    private final Activity context;
    private final ArrayList<HashMap<String, String>> courses;

    public RatingListView(Activity context, ArrayList<HashMap<String, String>> courses) {
        super(context, R.layout.listview_rating, courses);
        this.context = context;
        this.courses = courses;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        try {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.listview_rating, parent, false);
            }
            HashMap<String, String> change = courses.get(position);
            TextView lv_rating_name = ((TextView) convertView.findViewById(R.id.lv_rating_name));
            TextView lv_rating_position = ((TextView) convertView.findViewById(R.id.lv_rating_position));
            if (lv_rating_name != null) lv_rating_name.setText(change.get("name"));
            if (lv_rating_position != null) lv_rating_position.setText(change.get("position"));
            return convertView;
        } catch (Exception e) {
            Static.error(e);
            return super.getView(position, convertView, parent);
        }
    }
}
