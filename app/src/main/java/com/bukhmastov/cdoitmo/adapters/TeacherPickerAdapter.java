package com.bukhmastov.cdoitmo.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

public class TeacherPickerAdapter extends ArrayAdapter<JSONObject> {

    private final Context context;
    private ArrayList<JSONObject> teachers;

    public TeacherPickerAdapter(Context context, ArrayList<JSONObject> teachers) {
        super(context, R.layout.listview_teacher_picker, teachers);
        this.context = context;
        this.teachers = teachers;
    }

    @Override
    public int getCount() {
        return teachers.size();
    }

    @Override
    public JSONObject getItem(int index) {
        return teachers.get(index);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void addTeachers(ArrayList<JSONObject> teachers){
        this.teachers = teachers;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        try {
            JSONObject teacher = getItem(position);
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.layout_teachers_auto_complete_list, parent, false);
            }
            if (teacher != null) {
                ((TextView) convertView.findViewById(R.id.title)).setText(teacher.getString("person"));
                String post = teacher.getString("post");
                if (post == null || Objects.equals(post, "null")) {
                    convertView.findViewById(R.id.meta).setLayoutParams(new LinearLayout.LayoutParams(0, 0));
                } else {
                    ((TextView) convertView.findViewById(R.id.meta)).setText(post);
                }
            }
            return convertView;
        } catch (JSONException e) {
            Static.error(e);
            return super.getView(position, convertView, parent);
        }
    }
}
