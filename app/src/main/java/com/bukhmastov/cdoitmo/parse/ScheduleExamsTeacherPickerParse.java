package com.bukhmastov.cdoitmo.parse;

import android.os.AsyncTask;

import com.bukhmastov.cdoitmo.utils.Static;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleExamsTeacherPickerParse extends AsyncTask<String, Void, JSONObject> {
    public interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    public ScheduleExamsTeacherPickerParse(response delegate){
        this.delegate = delegate;
    }
    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            Matcher m;
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(params[0].replace("&nbsp;", " "));
            TagNode content = root.getElementsByAttValue("class", "content_block", true, false)[0];
            JSONArray teachers = new JSONArray();
            TagNode[] p = content.getElementsByName("p", false);
            for(TagNode item : p){
                try {
                    TagNode[] elements = item.getAllElements(false);
                    if(elements.length == 0) continue;
                    m = Pattern.compile("^/ru/exam/3/(.+)/.*$").matcher(elements[0].getAttributeByName("href"));
                    if(m.find()){
                        JSONObject teacher = new JSONObject();
                        teacher.put("name", elements[0].getText().toString().trim());
                        teacher.put("scope", "teacher" + m.group(1));
                        teacher.put("id", m.group(1));
                        teachers.put(teacher);
                    }
                } catch (Exception e){
                    Static.error(e);
                }
            }
            JSONObject response = new JSONObject();
            response.put("type", "teacher_picker");
            response.put("timestamp", Calendar.getInstance().getTimeInMillis());
            response.put("teachers", teachers);
            response.put("cache_token", params[1]);
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