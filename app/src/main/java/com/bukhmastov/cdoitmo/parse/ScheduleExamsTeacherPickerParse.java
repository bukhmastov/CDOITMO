package com.bukhmastov.cdoitmo.parse;

import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleExamsTeacherPickerParse implements Runnable {

    private static final String TAG = "SETeacherPickerParse";
    public interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    private String data;
    private String cache_token;

    public ScheduleExamsTeacherPickerParse(String data, String cache_token, response delegate) {
        this.data = data;
        this.cache_token = cache_token;
        this.delegate = delegate;
    }

    @Override
    public void run() {
        Log.v(TAG, "parsing");
        try {
            Matcher m;
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(data.replace("&nbsp;", " "));
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
            response.put("cache_token", cache_token);
            delegate.finish(response);
        } catch (Exception e) {
            Static.error(e);
            delegate.finish(null);
        }
    }
}
