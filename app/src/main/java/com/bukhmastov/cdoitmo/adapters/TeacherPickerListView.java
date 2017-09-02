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
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        try {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.listview_teacher_picker, parent, false);
            }
            HashMap<String, String> teacherMap = teachersMap.get(position);
            TextView lv_teacher_picker_name = convertView.findViewById(R.id.lv_teacher_picker_name);
            if (lv_teacher_picker_name != null) {
                String text = teacherMap.get("person");
                if (teacherMap.get("post") != null && !Objects.equals(teacherMap.get("post"), "") && !Objects.equals(teacherMap.get("post"), "null")) {
                    text += " (" + teacherMap.get("post") + ")";
                }
                lv_teacher_picker_name.setText(text);
            }
            return convertView;
        } catch (Exception e) {
            Static.error(e);
            return super.getView(position, convertView, parent);
        }
    }
}
