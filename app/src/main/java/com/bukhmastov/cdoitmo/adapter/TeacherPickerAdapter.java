package com.bukhmastov.cdoitmo.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class TeacherPickerAdapter extends ArrayAdapter<JSONObject> {

    private final Context context;
    private ArrayList<JSONObject> teachers;

    public TeacherPickerAdapter(Context context, ArrayList<JSONObject> teachers) {
        super(context, R.layout.spinner_teacher_picker, teachers);
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
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.spinner_teacher_picker, parent, false);
            }
            final JSONObject teacher = getItem(position);
            if (teacher != null) {
                final String post = teacher.getString("post");
                TextView title = convertView.findViewById(R.id.title);
                TextView meta = convertView.findViewById(R.id.meta);
                if (title != null) {
                    title.setText(teacher.getString("person"));
                }
                if (meta != null) {
                    if (post == null || post.equals("null") || post.trim().isEmpty()) {
                        meta.setVisibility(View.GONE);
                    } else {
                        meta.setVisibility(View.VISIBLE);
                        meta.setText(post);
                    }
                }
            }
            return convertView;
        } catch (JSONException e) {
            Log.exception(e);
            return super.getView(position, convertView, parent);
        }
    }
}
