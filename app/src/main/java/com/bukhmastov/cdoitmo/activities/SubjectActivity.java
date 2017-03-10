package com.bukhmastov.cdoitmo.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapters.PointsListView;
import com.bukhmastov.cdoitmo.fragments.ERegisterFragment;
import com.bukhmastov.cdoitmo.objects.entities.Group;
import com.bukhmastov.cdoitmo.objects.entities.ParsedERegister;
import com.bukhmastov.cdoitmo.objects.entities.Subject;
import com.bukhmastov.cdoitmo.objects.entities.Term;
import com.bukhmastov.cdoitmo.utils.Static;

import java.util.Objects;

public class SubjectActivity extends AppCompatActivity {

    private Subject subject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Static.darkTheme) setTheme(R.style.AppTheme_Dark);
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
        if (as_current_points != null) as_current_points.setText(pointsStr);
        if (as_desc != null) as_desc.setText(termSub + " " + getString(R.string.semester) + (Objects.equals(subject.type, "") ? "" : " | " + subject.type));
        if (as_result != null) {
            if (Objects.equals(subject.mark, "")) {
                ((ViewGroup) as_result.getParent()).removeView(as_result);
            } else {
                as_result.setText(subject.mark);
            }
        }
        // отображаем список оценок
        ListView as_list_view = (ListView) findViewById(R.id.as_list_view);
        if (as_list_view != null) {
            as_list_view.setAdapter(new PointsListView(this, subject.points));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: finish(); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

}