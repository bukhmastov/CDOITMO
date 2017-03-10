package com.bukhmastov.cdoitmo.adapters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;

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
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        HashMap<String, String> change = courses.get(position);
        View rowView = inflater.inflate(R.layout.listview_rating, null, true);
        TextView lv_rating_name = ((TextView) rowView.findViewById(R.id.lv_rating_name));
        TextView lv_rating_position = ((TextView) rowView.findViewById(R.id.lv_rating_position));
        if (lv_rating_name != null) lv_rating_name.setText(change.get("name"));
        if (lv_rating_position != null) lv_rating_position.setText(change.get("position"));
        return rowView;
    }
}
