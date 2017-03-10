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
import java.util.Objects;

public class TeacherPickerListView extends ArrayAdapter<HashMap<String, String>> {

    private final Activity context;
    private final ArrayList<HashMap<String, String>> teachersMap;

    public TeacherPickerListView(Activity context, ArrayList<HashMap<String, String>> teachersMap) {
        super(context, R.layout.listview_teacher_picker, teachersMap);
        this.context = context;
        this.teachersMap = teachersMap;
    }

    @NonNull
    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        HashMap<String, String> teacherMap = teachersMap.get(position);
        View rowView = inflater.inflate(R.layout.listview_teacher_picker, null, true);
        TextView lv_teacher_picker_name = (TextView) rowView.findViewById(R.id.lv_teacher_picker_name);
        if (lv_teacher_picker_name != null) {
            String text = teacherMap.get("person");
            if (teacherMap.get("post") != null && !Objects.equals(teacherMap.get("post"), "") && !Objects.equals(teacherMap.get("post"), "null")) {
                text += " (" + teacherMap.get("post") + ")";
            }
            lv_teacher_picker_name.setText(text);
        }
        return rowView;
    }
}
