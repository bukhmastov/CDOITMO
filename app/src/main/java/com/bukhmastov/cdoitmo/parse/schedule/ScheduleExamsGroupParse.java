package com.bukhmastov.cdoitmo.parse.schedule;

import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.parse.JSONParse;
import com.bukhmastov.cdoitmo.parse.Parse;

import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleExamsGroupParse extends JSONParse {

    private final String query;
    private final Pattern groupPattern = Pattern.compile("[a-zа-яё]\\d{4}[a-zа-яё]?", Pattern.CASE_INSENSITIVE);
    private final Pattern advicePattern = Pattern.compile("^Консультация (.{1,10}) в (\\d{1,2}:\\d{1,2}) Место:(.*)$", Pattern.CASE_INSENSITIVE);

    public ScheduleExamsGroupParse(String data, String query, Response<JSONObject> delegate) {
        super(data, delegate);
        this.query = query;
    }

    @Override
    protected JSONObject parse(TagNode root) throws Throwable {
        final TagNode[] titles = root.getElementsByAttValue("class", "page-header", true, false);
        final TagNode[] containers = root.getElementsByAttValue("class", "rasp_tabl_day", true, false);
        final JSONArray exams = new JSONArray();
        if (containers != null) {
            for (TagNode container : containers) {
                if (container == null) continue;
                final TagNode[] fields = container.getAllElements(false)[0].getAllElements(false)[0].getAllElements(false)[0].getAllElements(false);
                if (fields == null || fields.length < 4) continue;
                JSONObject examInfo = new JSONObject();
                JSONObject exam = new JSONObject();
                JSONObject advice = new JSONObject();
                exam.put("date", fields[0].getText().toString().trim());
                exam.put("time", fields[1].getAllElements(false)[0].getText().toString().trim());
                exam.put("room", fields[2].getAllElements(false)[0].getAllElements(false)[0].getText().toString().trim().replace(".", "").trim());
                TagNode meta = fields[3].getAllElements(false)[0];
                if (meta != null) {
                    examInfo.put("subject", meta.getAllElements(false)[0].getText().toString().trim());
                    examInfo.put("teacher", meta.getAllElements(false)[1].getText().toString().trim());
                    Matcher m = advicePattern.matcher(meta.getAllElements(false)[2].getText().toString().trim());
                    if (m.find()) {
                        advice.put("date", m.group(1));
                        advice.put("time", m.group(2));
                        advice.put("room", m.group(3).replace(".", "").trim());
                    }
                }
                String type = fields[4].getText().toString().trim().toLowerCase();
                if (type.startsWith("зач")) {
                    type = "credit";
                } else {
                    type = "exam";
                }
                examInfo.put("type", type);
                examInfo.put("exam", exam);
                examInfo.put("advice", advice);
                exams.put(examInfo);
            }
        }
        String label = query;
        if (titles != null && titles.length > 0) {
            label = titles[0].getText().toString().trim();
            Matcher m = groupPattern.matcher(label);
            if (m.find()) {
                label = m.group().trim();
            }
        }
        return new JSONObject()
                .put("label", label)
                .put("query", query)
                .put("exams", exams);
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Parse.SCHEDULE_EXAMS_GROUP;
    }
}
