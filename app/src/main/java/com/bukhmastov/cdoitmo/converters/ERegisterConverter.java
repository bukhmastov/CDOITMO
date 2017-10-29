package com.bukhmastov.cdoitmo.converters;

import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ERegisterConverter implements Runnable {

    private static final String TAG = "ERegisterConverter";
    public interface response {
        void finish(JSONObject json);
    }
    private final response delegate;
    private final JSONObject eregister;

    public ERegisterConverter(JSONObject eregister, response delegate) {
        this.eregister = eregister;
        this.delegate = delegate;
    }

    @Override
    public void run() {
        Log.v(TAG, "converting");
        JSONObject response = new JSONObject();
        try {
            JSONArray years = eregister.getJSONArray("years");
            JSONArray yearsOut = new JSONArray();
            for (int i = 0; i < years.length(); i++) {
                JSONObject year = years.getJSONObject(i);
                JSONObject yearOut = new JSONObject();
                int year1 = Integer.parseInt(year.getString("studyyear").split("/")[0]);
                int year2 = Integer.parseInt(year.getString("studyyear").split("/")[1]);
                JSONArray yearsDataOut = new JSONArray();
                yearsDataOut.put(0, year1 < year2 ? year1 : year2);
                yearsDataOut.put(1, year1 < year2 ? year2 : year1);
                JSONArray termsOut = new JSONArray();
                HashMap<String, Integer> hashMap = new HashMap<>();
                hashMap.put("number", -1);
                JSONObject firstTermOut = new JSONObject(hashMap);
                JSONObject secondTermOut = new JSONObject(hashMap);
                int t1 = 0, t2 = 0;
                JSONArray subjects = year.getJSONArray("subjects");
                for (int j = 0; j < subjects.length(); j++) {
                    int term = Integer.parseInt(subjects.getJSONObject(j).getString("semester"));
                    if (t1 == 0) {
                        t1 = term;
                    }
                    if (t1 != term) {
                        t2 = term;
                    }
                    if (t1 != 0 && t2 != 0) {
                        firstTermOut.put("number", Math.min(t1, t2));
                        secondTermOut.put("number", Math.max(t1, t2));
                        break;
                    }
                }
                fixTerm(firstTermOut, secondTermOut);
                fixTerm(secondTermOut, firstTermOut);
                JSONArray firstTermSubjectsOut = new JSONArray();
                JSONArray secondTermSubjectsOut = new JSONArray();
                for (int j = 0; j < subjects.length(); j++) {
                    JSONObject subjectData = subjects.getJSONObject(j);
                    JSONObject subjectOut = new JSONObject();
                    subjectOut.put("name", subjectData.getString("name"));
                    subjectOut.put("currentPoints", -1.0);
                    subjectOut.put("type", "");
                    subjectOut.put("mark", "");
                    subjectOut.put("markDate", "");
                    subjectOut.put("points", new JSONArray());
                    if (subjectData.has("points")) {
                        JSONArray pointsARR = subjectData.getJSONArray("points");
                        for (int k = 0; k < pointsARR.length(); k++) {
                            JSONObject point = pointsARR.getJSONObject(k);
                            if (Objects.equals(point.getString("max"), "100")){
                                subjectOut.put("currentPoints", mark2double(point.getString("value")));
                                break;
                            }
                        }
                    }
                    if (subjectData.has("marks")) {
                        JSONArray marks = subjectData.getJSONArray("marks");
                        if (marks.length() > 0) {
                            JSONObject marksData = marks.getJSONObject(0);
                            subjectOut.put("type", marksData.getString("worktype"));
                            subjectOut.put("mark", marksData.getString("mark"));
                            subjectOut.put("markDate", marksData.getString("markdate"));
                        }
                    }
                    if (subjectData.has("points")) {
                        JSONArray pointsARR = subjectData.getJSONArray("points");
                        JSONArray pointsOut = new JSONArray();
                        for (int k = 0; k < pointsARR.length(); k++) {
                            JSONObject pointData = pointsARR.getJSONObject(k);
                            if (pointData.getString("variable").toLowerCase().contains("семестр")) continue;
                            JSONObject pointOut = new JSONObject();
                            pointOut.put("name", pointData.getString("variable"));
                            String value = pointData.getString("value").trim();
                            pointOut.put("value", value.isEmpty() ? 0.0 : mark2double(value));
                            String limit = pointData.getString("limit").trim();
                            pointOut.put("limit", limit.isEmpty() ? 0.0 : mark2double(limit));
                            String max = pointData.getString("max").trim();
                            pointOut.put("max", max.isEmpty() ? 0.0 : mark2double(max));
                            pointsOut.put(pointOut);
                        }
                        subjectOut.put("points", pointsOut);
                    }
                    int term = Integer.parseInt(subjectData.getString("semester"));
                    if (term == firstTermOut.getInt("number")) firstTermSubjectsOut.put(subjectOut);
                    if (term == secondTermOut.getInt("number")) secondTermSubjectsOut.put(subjectOut);
                }
                firstTermOut.put("subjects", sortSubjects(firstTermSubjectsOut));
                secondTermOut.put("subjects", sortSubjects(secondTermSubjectsOut));
                termsOut.put(firstTermOut);
                termsOut.put(secondTermOut);
                yearOut.put("name", year.getString("group"));
                yearOut.put("years", yearsDataOut);
                yearOut.put("terms", termsOut);
                yearsOut.put(yearOut);
            }
            response.put("groups", yearsOut);
            response.put("timestamp", Calendar.getInstance().getTimeInMillis());
        } catch (Exception e) {
            Static.error(e);
        }
        delegate.finish(response);
    }

    private double mark2double(String string){
        return string2double(markConverter(string));
    }
    private double string2double(String string){
        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    private String markConverter(String value){
        Matcher m;
        value = value.replace(",", ".").trim();
        m = Pattern.compile("^\\.(.+)").matcher(value);
        if (m.find()) value = "0." + m.group(1);
        m = Pattern.compile("(.+)\\.0$").matcher(value);
        if (m.find()) value = m.group(1);
        return value;
    }
    private void fixTerm(JSONObject firstTerm, JSONObject secondTerm) throws JSONException {
        if (firstTerm.getInt("number") == -1 && secondTerm.getInt("number") != -1) {
            if (secondTerm.getInt("number") % 2 == 0) {
                firstTerm.put("number", secondTerm.getInt("number") - 1);
            } else {
                firstTerm.put("number", secondTerm.getInt("number") + 1);
            }
        }
    }
    private JSONArray sortSubjects(JSONArray subjects) throws JSONException {
        ArrayList<JSONObject> sort = new ArrayList<>();
        for (int i = 0; i < subjects.length(); i++) {
            sort.add(subjects.getJSONObject(i));
        }
        Collections.sort(sort, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject o1, JSONObject o2) {
                try {
                    return o1.getString("name").compareTo(o2.getString("name"));
                } catch (JSONException e) {
                    return 0;
                }
            }
        });
        return new JSONArray(sort);
    }
}
