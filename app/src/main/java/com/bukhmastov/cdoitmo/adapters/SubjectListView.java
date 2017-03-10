package com.bukhmastov.cdoitmo.adapters;

import android.app.Activity;
import android.support.annotation.NonNull;
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

    public SubjectListView(Activity context, ArrayList<HashMap<String, String>> subj) {
        super(context, R.layout.listview_subject, subj);
        this.context = context;
        this.subj = subj;
    }

    @NonNull
    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        HashMap<String, String> sub = subj.get(position);
        View rowView = inflater.inflate(R.layout.listview_subject, null, true);
        TextView lv_subject_name = ((TextView) rowView.findViewById(R.id.lv_subject_name));
        TextView lv_subject_sem = ((TextView) rowView.findViewById(R.id.lv_subject_sem));
        TextView lv_subject_points = ((TextView) rowView.findViewById(R.id.lv_point_value));
        if (lv_subject_name != null) lv_subject_name.setText(sub.get("name"));
        if (lv_subject_sem != null) lv_subject_sem.setText(sub.get("semester") + " " + context.getString(R.string.semester) + (Objects.equals(sub.get("type"), "") ? "" : " | " + sub.get("type")));
        if (lv_subject_points != null) lv_subject_points.setText(double2string(Double.parseDouble(sub.get("value"))));
        if(Double.parseDouble(sub.get("value")) >= 60.0){
            context.getTheme().resolveAttribute(R.attr.textColorPassed, Static.typedValue, true);
            if (lv_subject_name != null) lv_subject_name.setTextColor(Static.typedValue.data);
            if (lv_subject_sem != null) lv_subject_sem.setTextColor(Static.typedValue.data);
            if (lv_subject_points != null) lv_subject_points.setTextColor(Static.typedValue.data);
        }
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