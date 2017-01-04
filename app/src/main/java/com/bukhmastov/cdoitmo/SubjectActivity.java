package com.bukhmastov.cdoitmo;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class SubjectActivity extends AppCompatActivity {

    private Subject subject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_subject));
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setTitle(R.string.subject_review);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // получаем реквизиты предмета
        String groupSub = getIntent().getStringExtra("group");
        int termSub = Integer.parseInt(getIntent().getStringExtra("term"));
        String nameSub = getIntent().getStringExtra("name");
        // проверяем целостность данных
        if(ERegisterFragment.eRegister != null){
            ParsedERegister eRegister = ERegisterFragment.eRegister.get();
            boolean groupFound = false;
            for(Group group : eRegister.groups) {
                if(Objects.equals(group.name, groupSub)){
                    boolean termFound = false;
                    for (Term term : group.terms) {
                        if(term.number == termSub){
                            boolean subjectFound = false;
                            for (Subject subject : term.subjects) {
                                if(Objects.equals(subject.name, nameSub)){
                                    this.subject = subject;
                                    subjectFound = true;
                                    break;
                                }
                            }
                            if(!subjectFound) finish();
                            termFound = true;
                            break;
                        }
                    }
                    if(!termFound) finish();
                    groupFound = true;
                    break;
                }
            }
            if(!groupFound) finish();
        } else {
            finish();
        }
        if(actionBar != null){
            actionBar.setTitle(subject.name);
        }
        // отображаем шапку
        TextView as_current_points = (TextView) findViewById(R.id.as_current_points);
        TextView as_desc = (TextView) findViewById(R.id.as_desc);
        TextView as_result = (TextView) findViewById(R.id.as_result);
        Double points = subject.currentPoints;
        String pointsStr = String.valueOf(points);
        if(points != -1.0){
            if(points == Double.parseDouble(points.intValue() + ".0")){
                pointsStr = points.intValue() + "";
            }
        } else {
            pointsStr = "";
        }
        as_current_points.setText(pointsStr);
        as_desc.setText(termSub + " " + getString(R.string.semester) + (Objects.equals(subject.type, "") ? "" : " | " + subject.type));
        if(Objects.equals(subject.mark, "")){
            ((ViewGroup) as_result.getParent()).removeView(as_result);
        } else {
            as_result.setText(subject.mark);
        }
        // отображаем список оценок
        ListView as_list_view = (ListView) findViewById(R.id.as_list_view);
        as_list_view.setAdapter(new PointsListView(this, subject.points));

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: finish(); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

}

class PointsListView extends ArrayAdapter<Point> {
    private final Activity context;
    private final ArrayList<Point> points;

    PointsListView(Activity context, ArrayList<Point> points) {
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
        lv_point_name.setText(point.name);
        lv_point_limits.setText("0 / " + double2string(point.limit) + " / " + double2string(point.max));
        lv_point_value.setText(double2string(point.value));
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