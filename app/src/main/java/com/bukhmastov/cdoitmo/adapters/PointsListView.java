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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PointsListView extends ArrayAdapter<JSONObject> {
    private final Activity context;
    private final ArrayList<JSONObject> points;

    public PointsListView(Activity context, ArrayList<JSONObject> points) {
        super(context, R.layout.listview_point, points);
        this.context = context;
        this.points = points;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        try {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.listview_point, parent, false);
            }
            JSONObject point = points.get(position);
            TextView lv_point_name = ((TextView) convertView.findViewById(R.id.lv_point_name));
            TextView lv_point_limits = ((TextView) convertView.findViewById(R.id.lv_point_limits));
            TextView lv_point_value = ((TextView) convertView.findViewById(R.id.lv_point_value));
            try {
                if (lv_point_name != null) lv_point_name.setText(point.getString("name"));
                if (lv_point_limits != null) lv_point_limits.setText("0 / " + markConverter(String.valueOf(point.getDouble("limit"))) + " / " + markConverter(String.valueOf(point.getDouble("max"))));
                if (lv_point_value != null) lv_point_value.setText(markConverter(String.valueOf(point.getDouble("value"))));
            } catch (Exception e) {
                Static.error(e);
            }
            return convertView;
        } catch (Exception e) {
            Static.error(e);
            return super.getView(position, convertView, parent);
        }
    }

    private String markConverter(String value){
        Matcher m;
        value = value.replace(",", ".").trim();
        m = Pattern.compile("^\\.(.+)").matcher(value);
        if (m.find()) value = "0." + m.group(1);
        m = Pattern.compile("(.+)\\.0$").matcher(value);
        if (m.find()) value = m.group(1);
        return value;
    }
}