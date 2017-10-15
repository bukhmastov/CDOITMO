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
            HashMap<String, String> sub = subj.get(position);
            TextView lv_subject_name = convertView.findViewById(R.id.lv_subject_name);
            TextView lv_subject_sem = convertView.findViewById(R.id.lv_subject_sem);
            TextView lv_subject_points = convertView.findViewById(R.id.lv_point_value);
            if (lv_subject_name != null) lv_subject_name.setText(sub.get("name"));
            if (lv_subject_sem != null) lv_subject_sem.setText(sub.get("semester") + " " + context.getString(R.string.semester) + (Objects.equals(sub.get("type"), "") ? "" : " | " + sub.get("type")));
            if (lv_subject_points != null) lv_subject_points.setText(double2string(Double.parseDouble(sub.get("value"))));
            if (Double.parseDouble(sub.get("value")) >= 60.0) {
                if (colorPassed != -1) {
                    if (lv_subject_name != null) lv_subject_name.setTextColor(colorPassed);
                    if (lv_subject_sem != null) lv_subject_sem.setTextColor(colorPassed);
                    if (lv_subject_points != null) lv_subject_points.setTextColor(colorPassed);
                }
            } else {
                if (colorOnGoing != -1) {
                    if (lv_subject_name != null) lv_subject_name.setTextColor(colorOnGoing);
                    if (lv_subject_sem != null) lv_subject_sem.setTextColor(colorOnGoing);
                    if (lv_subject_points != null) lv_subject_points.setTextColor(colorOnGoing);
                }
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
