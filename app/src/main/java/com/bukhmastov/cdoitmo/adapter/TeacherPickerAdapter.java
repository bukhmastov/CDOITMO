package com.bukhmastov.cdoitmo.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.model.schedule.teachers.STeacher;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;

import javax.inject.Inject;

public class TeacherPickerAdapter extends ArrayAdapter<STeacher> {

    private final Context context;
    private ArrayList<STeacher> teachers;

    @Inject
    Log log;

    public TeacherPickerAdapter(Context context, ArrayList<STeacher> teachers) {
        super(context, R.layout.spinner_teacher_picker, teachers);
        AppComponentProvider.getComponent().inject(this);
        this.context = context;
        this.teachers = teachers;
    }

    @Override
    public int getCount() {
        return teachers.size();
    }

    @Override
    public STeacher getItem(int index) {
        return teachers.get(index);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void addTeachers(ArrayList<STeacher> teachers){
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
            STeacher teacher = getItem(position);
            if (teacher != null) {
                String post = teacher.getPost();
                TextView title = convertView.findViewById(R.id.title);
                TextView meta = convertView.findViewById(R.id.meta);
                if (title != null) {
                    title.setText(teacher.getPerson());
                }
                if (meta != null) {
                    if (StringUtils.isBlank(post) || post.equals("null")) {
                        meta.setVisibility(View.GONE);
                    } else {
                        meta.setVisibility(View.VISIBLE);
                        meta.setText(post);
                    }
                }
            }
            return convertView;
        } catch (Exception e) {
            log.exception(e);
            return super.getView(position, convertView, parent);
        }
    }
}
