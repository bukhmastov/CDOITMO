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
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

public class SubjectActivity extends AppCompatActivity {

    private JSONObject subject = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Static.darkTheme) setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_subject));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.subject_review);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // получаем реквизиты предмета
        String groupSub = getIntent().getStringExtra("group");
        int termSub = Integer.parseInt(getIntent().getStringExtra("term"));
        String nameSub = getIntent().getStringExtra("name");
        // проверяем целостность данных
        try {
            if (ERegisterFragment.eRegister == null) throw new Exception("ERegisterFragment.eRegister is null");
            JSONObject data = ERegisterFragment.eRegister.get();
            boolean groupFound = false;
            JSONArray groups = data.getJSONArray("groups");
            for (int i = 0; i < groups.length(); i++) {
                JSONObject group = groups.getJSONObject(i);
                if(Objects.equals(group.getString("name"), groupSub)){
                    boolean termFound = false;
                    JSONArray terms = group.getJSONArray("terms");
                    for (int j = 0; j < terms.length(); j++) {
                        JSONObject term = terms.getJSONObject(j);
                        if(term.getInt("number") == termSub){
                            boolean subjectFound = false;
                            JSONArray subjects = term.getJSONArray("subjects");
                            for (int k = 0; k < subjects.length(); k++) {
                                JSONObject subject = subjects.getJSONObject(k);
                                if (Objects.equals(subject.getString("name"), nameSub)) {
                                    this.subject = subject;
                                    subjectFound = true;
                                    break;
                                }
                            }
                            if (!subjectFound) throw new Exception("Subject not found");
                            termFound = true;
                            break;
                        }
                    }
                    if (!termFound) throw new Exception("Term not found");
                    groupFound = true;
                    break;
                }
            }
            if (!groupFound) throw new Exception("Group not found");
            if (actionBar != null) {
                actionBar.setTitle(subject.getString("name"));
            }
            // отображаем шапку
            TextView as_current_points = (TextView) findViewById(R.id.as_current_points);
            TextView as_desc = (TextView) findViewById(R.id.as_desc);
            TextView as_result = (TextView) findViewById(R.id.as_result);
            Double currentPoints = subject.getDouble("currentPoints");
            String pointsStr = String.valueOf(currentPoints);
            if (currentPoints != -1.0) {
                if (currentPoints == Double.parseDouble(currentPoints.intValue() + ".0")) {
                    pointsStr = currentPoints.intValue() + "";
                }
            } else {
                pointsStr = "";
            }
            if (as_current_points != null) as_current_points.setText(pointsStr);
            if (as_desc != null) as_desc.setText(termSub + " " + getString(R.string.semester) + (Objects.equals(subject.getString("type"), "") ? "" : " | " + subject.getString("type")));
            if (as_result != null) {
                if (Objects.equals(subject.getString("mark"), "")) {
                    ((ViewGroup) as_result.getParent()).removeView(as_result);
                } else {
                    as_result.setText(subject.getString("mark"));
                }
            }
            // отображаем список оценок
            ListView as_list_view = (ListView) findViewById(R.id.as_list_view);
            if (as_list_view != null) {
                ArrayList<JSONObject> pointsArray = new ArrayList<>();
                JSONArray points = subject.getJSONArray("points");
                for (int i = 0; i < points.length(); i++) {
                    pointsArray.add(points.getJSONObject(i));
                }
                as_list_view.setAdapter(new PointsListView(this, pointsArray));
            }
        } catch (Exception e) {
            Static.error(e);
            finish();
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