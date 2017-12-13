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

public class SubjectListView extends ArrayAdapter<HashMap<String, String>> {

    private final Activity context;
    private final ArrayList<HashMap<String, String>> subj;
    private int colorOnGoing;
    private int colorPassed;

    public SubjectListView(Activity context, ArrayList<HashMap<String, String>> subj) {
        super(context, R.layout.listview_subject, subj);
        this.context = context;
        this.subj = subj;
        try {
            this.colorOnGoing = Static.resolveColor(context, android.R.attr.textColorPrimary);
        } catch (Exception e) {
            this.colorOnGoing = -1;
        }
        try {
            this.colorPassed = Static.resolveColor(context, R.attr.colorPositiveTrend);
        } catch (Exception e) {
            this.colorPassed = -1;
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        try {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.listview_subject, parent, false);
            }
            final HashMap<String, String> sub = subj.get(position);
            final Double points = Double.parseDouble(sub.get("value"));
            final String name = sub.get("name");
            final String term = sub.get("semester");
            final String type = sub.get("type");
            TextView lv_subject_name = convertView.findViewById(R.id.lv_subject_name);
            TextView lv_subject_sem = convertView.findViewById(R.id.lv_subject_sem);
            TextView lv_subject_points = convertView.findViewById(R.id.lv_point_value);
            if (lv_subject_name != null) lv_subject_name.setText(name);
            if (lv_subject_sem != null) lv_subject_sem.setText(term + " " + context.getString(R.string.semester) + (type.isEmpty() ? "" : " | " + type));
            if (lv_subject_points != null) lv_subject_points.setText(double2string(points));
            if (points >= 60.0 && colorPassed != -1) {
                if (lv_subject_name != null) lv_subject_name.setTextColor(colorPassed);
                if (lv_subject_sem != null) lv_subject_sem.setTextColor(colorPassed);
                if (lv_subject_points != null) lv_subject_points.setTextColor(colorPassed);
            } else if (colorOnGoing != -1) {
                if (lv_subject_name != null) lv_subject_name.setTextColor(colorOnGoing);
                if (lv_subject_sem != null) lv_subject_sem.setTextColor(colorOnGoing);
                if (lv_subject_points != null) lv_subject_points.setTextColor(colorOnGoing);
            }
            return convertView;
        } catch (Exception e) {
            Static.error(e);
            return super.getView(position, convertView, parent);
        }
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
