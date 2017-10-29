package com.bukhmastov.cdoitmo.parse;

import com.bukhmastov.cdoitmo.exceptions.SilentException;
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
    private final response delegate;
    private final String data;
    private final String cache_token;

    public ScheduleExamsTeacherPickerParse(String data, String cache_token, response delegate) {
        this.data = data;
        this.cache_token = cache_token;
        this.delegate = delegate;
    }

    @Override
    public void run() {
        Log.v(TAG, "parsing");
        try {
            TagNode root = new HtmlCleaner().clean(data.replace("&nbsp;", " "));
            if (root == null) {
                throw new SilentException();
            }
            JSONArray teachers = new JSONArray();
            TagNode content = root.getElementsByAttValue("class", "content_block", true, false)[0];
            if (content != null) {
                TagNode[] p = content.getElementsByName("p", false);
                if (p != null) {
                    for (TagNode item : p) {
                        try {
                            if (item == null) continue;
                            TagNode[] elements = item.getAllElements(false);
                            if (elements == null || elements.length == 0) continue;
                            Matcher m = Pattern.compile("^/ru/exam/3/(.+)/.*$").matcher(elements[0].getAttributeByName("href"));
                            if (m.find()) {
                                JSONObject teacher = new JSONObject();
                                teacher.put("name", elements[0].getText().toString().trim());
                                teacher.put("scope", "teacher" + m.group(1));
                                teacher.put("id", m.group(1));
                                teachers.put(teacher);
                            }
                        } catch (Exception e) {
                            Static.error(e);
                        }
                    }
                }
            }
            JSONObject response = new JSONObject();
            response.put("type", "teacher_picker");
            response.put("timestamp", Calendar.getInstance().getTimeInMillis());
            response.put("teachers", teachers);
            response.put("cache_token", cache_token);
            delegate.finish(response);
        } catch (SilentException silent) {
            delegate.finish(null);
        } catch (Exception e) {
            Static.error(e);
            delegate.finish(null);
        }
    }
}
