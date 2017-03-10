package com.bukhmastov.cdoitmo.parse;

import android.content.Context;
import android.os.AsyncTask;

import com.bukhmastov.cdoitmo.utils.Static;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Room101ViewRequestParse extends AsyncTask<String, Void, JSONObject> {
    public interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    private Context context = null;
    public Room101ViewRequestParse(response delegate, Context context){
        this.delegate = delegate;
        this.context = context;
    }
    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            Matcher m;
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(params[0].replace("&nbsp;", " "));
            JSONObject response = new JSONObject();
            TagNode multi_table = root.getElementsByAttValue("class", "multi_table", true, false)[0];
            TagNode[] d_table = multi_table.getElementsByName("table", true);
            TagNode[] tds = d_table[0].getAllElements(false)[0].getAllElements(false)[1].getAllElements(false);
            response.put("timestamp", Calendar.getInstance().getTimeInMillis());
            response.put("date", tds[0].getText().toString().trim());
            response.put("limit", tds[1].getText().toString().trim());
            response.put("left", tds[2].getText().toString().trim());
            response.put("penalty", tds[3].getText().toString().trim());
            TagNode[] trs = d_table[1].getAllElements(false)[0].getAllElements(false);
            JSONArray sessions = new JSONArray();
            for(int i = 1; i < trs.length; i++){
                tds = trs[i].getAllElements(false);
                JSONObject session = new JSONObject();
                String date = tds[1].getText().toString().trim();
                m = Pattern.compile("(\\d)(\\d).(\\d{2}).(\\d{2,4})").matcher(date);
                if (m.find()) {
                    date = (Objects.equals(m.group(1), "0") ? "" : m.group(1)) + m.group(2) + " " + Static.getGenitiveMonth(context, m.group(3)) + " " + m.group(4);
                }
                String time = tds[2].getText().toString().trim();
                String[] times = time.split("-");
                session.put("number", tds[0].getText().toString().trim());
                session.put("date", date);
                session.put("time", time);
                session.put("timeStart", times[0].trim());
                session.put("timeEnd", times[1].trim());
                TagNode[] inputs = tds[3].getElementsByName("input", false);
                if(inputs.length > 0){
                    session.put("status", inputs[0].getAttributeByName("value").trim());
                    m = Pattern.compile(".*document\\.fn\\.reid\\.value=(\\d+).*").matcher(inputs[0].getAttributeByName("onclick").trim());
                    if(m.find()){
                        int reid = 0;
                        try {
                            reid = Integer.parseInt(m.group(1));
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                        session.put("reid", reid);
                    } else {
                        session.put("reid", 0);
                    }
                } else {
                    session.put("status", tds[3].getText().toString().trim());
                    session.put("reid", 0);
                }
                session.put("requested", tds[4].getText().toString().trim());
                sessions.put(session);
            }
            response.put("sessions", sessions);
            return response;
        } catch (Exception e) {
            Static.error(e);
            return null;
        }
    }
    @Override
    protected void onPostExecute(JSONObject json) {
        delegate.finish(json);
    }
}