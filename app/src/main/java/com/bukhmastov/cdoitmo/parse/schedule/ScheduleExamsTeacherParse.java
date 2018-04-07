package com.bukhmastov.cdoitmo.parse.schedule;

import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.parse.Parse;

import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleExamsTeacherParse extends Parse {

    private final String query;

    public ScheduleExamsTeacherParse(String data, String query, Response delegate) {
        super(data, delegate);
        this.query = query;
    }

    @Override
    protected JSONObject parse(TagNode root) throws Throwable {
        final TagNode[] titles = root.getElementsByAttValue("class", "page-header", true, false);
        final TagNode[] containers = root.getElementsByAttValue("class", "rasp_tabl_day", true, false);
        final JSONArray exams = new JSONArray();
        String label = "";
        if (containers != null) {
            for (TagNode container : containers) {
                if (container == null) continue;
                final TagNode[] fields = container.getAllElements(false)[0].getAllElements(false)[0].getAllElements(false)[0].getAllElements(false);
                if (fields == null) continue;
                JSONObject examInfo = new JSONObject();
                JSONObject exam = new JSONObject();
                JSONObject advice = new JSONObject();
                exam.put("date", fields[0].getAllElements(false)[0].getText().toString().trim());
                exam.put("time", fields[1].getAllElements(false)[0].getText().toString().trim());
                exam.put("room", fields[2].getAllElements(false)[0].getAllElements(false)[0].getText().toString().trim().replace(".", "").trim());
                examInfo.put("group", fields[3].getAllElements(false)[0].getText().toString().trim());
                TagNode meta = fields[4].getAllElements(false)[0];
                if (meta != null) {
                    examInfo.put("subject", meta.getAllElements(false)[0].getText().toString().trim());
                    label = meta.getAllElements(false)[1].getText().toString().trim();
                    Matcher m = Pattern.compile("^Консультация (.{1,10}) в (\\d{1,2}:\\d{1,2}) Место:(.*)$").matcher(meta.getAllElements(false)[2].getText().toString().trim());
                    if (m.find()) {
                        advice.put("date", m.group(1));
                        advice.put("time", m.group(2));
                        advice.put("room", m.group(3).replace(".", "").trim());
                    }
                }
                examInfo.put("exam", exam);
                examInfo.put("advice", advice);
                exams.put(examInfo);
            }
        }
        if (label.isEmpty()) {
            label = titles != null && titles.length > 0 ? titles[0].getText().toString() : query;
            Matcher m = Pattern.compile("^(.*)Расписание экзаменов\\.?$").matcher(label);
            if (m.find()) {
                label = m.group(1).trim();
            }
        }
        return new JSONObject()
                .put("label", label)
                .put("query", query)
                .put("exams", exams);
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Parse.SCHEDULE_EXAMS_TEACHER;
    }
}
