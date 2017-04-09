package com.bukhmastov.cdoitmo.converters;

import android.os.AsyncTask;

import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ERegisterConverter extends AsyncTask<JSONObject, Void, JSONObject> {
    public interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    public ERegisterConverter(response delegate){
        this.delegate = delegate;
    }
    @Override
    protected JSONObject doInBackground(JSONObject... params) {
        JSONObject response = new JSONObject();
        try {
            JSONObject eregister = params[0];
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
                JSONObject firstTermOut = new JSONObject();
                JSONObject secondTermOut = new JSONObject();
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
                firstTermOut.put("subjects", firstTermSubjectsOut);
                secondTermOut.put("subjects", secondTermSubjectsOut);
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
        return response;
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
    @Override
    protected void onPostExecute(JSONObject json) {
        delegate.finish(json);
    }
}
