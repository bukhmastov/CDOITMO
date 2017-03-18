package com.bukhmastov.cdoitmo.objects;

import android.content.Context;

import com.bukhmastov.cdoitmo.objects.entities.Group;
import com.bukhmastov.cdoitmo.objects.entities.ParsedERegister;
import com.bukhmastov.cdoitmo.objects.entities.Point;
import com.bukhmastov.cdoitmo.objects.entities.Subject;
import com.bukhmastov.cdoitmo.objects.entities.Term;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class ERegister {

    private static final String TAG = "ERegister";
    private Context context;
    private ParsedERegister parsedERegister = null;

    public ERegister(Context context){
        this.context = context;
        String eRegister = Storage.file.cache.get(context, "eregister#core");
        if (!eRegister.isEmpty()) {
            try {
                parse(new JSONObject(eRegister));
            } catch (Exception e) {
                Static.error(e);
            }
        }
    }
    public void put(JSONObject data){
        try {
            JSONObject json = new JSONObject();
            json.put("timestamp", Calendar.getInstance().getTimeInMillis());
            json.put("eregister", data);
            parse(json);
            Storage.file.cache.put(context, "eregister#core", json.toString());
        } catch (Exception e) {
            Static.error(e);
        }
    }
    public ParsedERegister get(){
        return parsedERegister;
    }
    public boolean is(){
        return this.parsedERegister != null;
    }
    private void parse(JSONObject json){
        if(json == null){
            parsedERegister = null;
        } else {
            try {
                parsedERegister = new ParsedERegister();
                parsedERegister.timestamp = json.getLong("timestamp");
                parsedERegister.date = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT).format(new Date(parsedERegister.timestamp));
                JSONArray years = json.getJSONObject("eregister").getJSONArray("years");
                for(int i = 0; i < years.length(); i++){
                    JSONObject groupData = years.getJSONObject(i);
                    Group group = new Group();
                    group.name = groupData.getString("group");
                    String studyyear = groupData.getString("studyyear");
                    int year1 = Integer.parseInt(studyyear.split("/")[0]);
                    int year2 = Integer.parseInt(studyyear.split("/")[1]);
                    if(year1 < year2){
                        group.year[0] = year1;
                        group.year[1] = year2;
                    } else {
                        group.year[0] = year2;
                        group.year[1] = year1;
                    }
                    Term firstTerm = new Term();
                    Term secondTerm = new Term();
                    int t1 = 0, t2 = 0;
                    JSONArray subjects = groupData.getJSONArray("subjects");
                    for(int j = 0; j < subjects.length(); j++){
                        JSONObject subjectData = subjects.getJSONObject(j);
                        int term = Integer.parseInt(subjectData.getString("semester"));
                        if(t1 == 0){
                            t1 = term;
                        }
                        if(t1 != term){
                            t2 = term;
                        }
                        if(t1 != 0 && t2 != 0){
                            firstTerm.number = Math.min(t1, t2);
                            secondTerm.number = Math.max(t1, t2);
                            break;
                        }
                    }
                    for(int j = 0; j < subjects.length(); j++){
                        JSONObject subjectData = subjects.getJSONObject(j);
                        Subject subject = new Subject();
                        subject.name = subjectData.getString("name");
                        subject.currentPoints = -1.0;
                        if(subjectData.has("points")){
                            JSONArray pointsARR = subjectData.getJSONArray("points");
                            for (int k = 0; k < pointsARR.length(); k++) {
                                JSONObject point = pointsARR.getJSONObject(k);
                                if(Objects.equals(point.getString("max"), "100")){
                                    subject.currentPoints = Double.parseDouble(point.getString("value").replace(',', '.'));
                                    break;
                                }
                            }
                        }
                        if(subjectData.has("marks")){
                            JSONArray marks = subjectData.getJSONArray("marks");
                            if(marks.length() > 0) {
                                JSONObject marksData = marks.getJSONObject(0);
                                subject.type = marksData.getString("worktype");
                                subject.mark = marksData.getString("mark");
                                subject.markDate = marksData.getString("markdate");
                            }
                        }
                        if(subjectData.has("points")){
                            JSONArray pointsARR = subjectData.getJSONArray("points");
                            for(int k = 0; k < pointsARR.length(); k++){
                                JSONObject pointData = pointsARR.getJSONObject(k);
                                if(pointData.getString("variable").contains("Семестр")) continue;
                                Point point = new Point();
                                point.name = pointData.getString("variable");
                                String value = pointData.getString("value").replace(",", ".");
                                point.value = Objects.equals(value, "") ? 0.0 : Double.parseDouble(value);
                                String limit = pointData.getString("limit").replace(",", ".");
                                point.limit = Objects.equals(limit, "") ? 0.0 : Double.parseDouble(limit);
                                String max = pointData.getString("max").replace(",", ".");
                                point.max = Objects.equals(max, "") ? 0.0 : Double.parseDouble(max);
                                subject.points.add(point);
                            }
                        }
                        int term = Integer.parseInt(subjectData.getString("semester"));
                        if(term == firstTerm.number) firstTerm.subjects.add(subject);
                        if(term == secondTerm.number) secondTerm.subjects.add(subject);
                    }
                    group.terms.add(firstTerm);
                    group.terms.add(secondTerm);
                    parsedERegister.groups.add(group);
                }
            } catch (Exception e) {
                Static.error(e);
                parsedERegister = null;
            }
        }
    }
}