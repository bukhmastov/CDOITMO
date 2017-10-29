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

public class ScheduleExamsTeacherParse implements Runnable {

    private static final String TAG = "SETeacherParse";
    public interface response {
        void finish(JSONObject json);
    }
    private final response delegate;
    private final String data;
    private final String cache_token;

    public ScheduleExamsTeacherParse(String data, String cache_token, response delegate) {
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
            JSONArray schedule = new JSONArray();
            TagNode[] exams = root.getElementsByAttValue("class", "rasp_tabl_day", true, false);
            String teacher = "";
            if (exams != null) {
                for (TagNode exam : exams) {
                    if (exam == null) continue;
                    TagNode[] fields = exam.getAllElements(false)[0].getAllElements(false)[0].getAllElements(false)[0].getAllElements(false);
                    if (fields == null) continue;
                    JSONObject examContainerObj = new JSONObject();
                    JSONObject examObj = new JSONObject();
                    JSONObject consultObj = new JSONObject();
                    examObj.put("date", fields[0].getAllElements(false)[0].getText().toString().trim());
                    examObj.put("time", fields[1].getAllElements(false)[0].getText().toString().trim());
                    examObj.put("room", fields[2].getAllElements(false)[0].getAllElements(false)[0].getText().toString().trim().replace(".", "").trim());
                    examContainerObj.put("group", fields[3].getAllElements(false)[0].getText().toString().trim());
                    TagNode meta = fields[4].getAllElements(false)[0];
                    if (meta != null) {
                        examContainerObj.put("subject", meta.getAllElements(false)[0].getText().toString().trim());
                        teacher = meta.getAllElements(false)[1].getText().toString().trim();
                        Matcher m = Pattern.compile("^Консультация (.{1,10}) в (\\d{1,2}:\\d{1,2}) Место:(.*)$").matcher(meta.getAllElements(false)[2].getText().toString().trim());
                        if (m.find()) {
                            consultObj.put("date", m.group(1));
                            consultObj.put("time", m.group(2));
                            consultObj.put("room", m.group(3).replace(".", "").trim());
                        }
                    }
                    examContainerObj.put("exam", examObj);
                    examContainerObj.put("consult", consultObj);
                    schedule.put(examContainerObj);
                }
            }
            JSONObject response = new JSONObject();
            response.put("type", "teacher");
            response.put("timestamp", Calendar.getInstance().getTimeInMillis());
            response.put("scope", teacher);
            response.put("cache_token", cache_token);
            response.put("schedule", schedule);
            delegate.finish(response);
        } catch (SilentException silent) {
            delegate.finish(null);
        } catch (Exception e) {
            Static.error(e);
            delegate.finish(null);
        }
    }
}
