package com.bukhmastov.cdoitmo.adapters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.objects.entities.Point;

import java.util.ArrayList;

public class PointsListView extends ArrayAdapter<Point> {
    private final Activity context;
    private final ArrayList<Point> points;

    public PointsListView(Activity context, ArrayList<Point> points) {
        super(context, R.layout.listview_point, points);
        this.context = context;
        this.points = points;
    }

    @NonNull
    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        Point point = points.get(position);
        View rowView = inflater.inflate(R.layout.listview_point, null, true);
        TextView lv_point_name = ((TextView) rowView.findViewById(R.id.lv_point_name));
        TextView lv_point_limits = ((TextView) rowView.findViewById(R.id.lv_point_limits));
        TextView lv_point_value = ((TextView) rowView.findViewById(R.id.lv_point_value));
        if (lv_point_name != null) lv_point_name.setText(point.name);
        if (lv_point_limits != null) lv_point_limits.setText("0 / " + double2string(point.limit) + " / " + double2string(point.max));
        if (lv_point_value != null) lv_point_value.setText(double2string(point.value));
        return rowView;
    }

    private String double2string(Double value){
        String valueStr = String.valueOf(value);
        if(value != -1.0){
            if(value == Double.parseDouble(value.intValue() + ".0")){
                valueStr = value.intValue() + "";
            }
        } else {
            valueStr = "";
        }
        return valueStr;
    }
}