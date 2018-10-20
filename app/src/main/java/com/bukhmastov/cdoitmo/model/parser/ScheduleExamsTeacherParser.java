package com.bukhmastov.cdoitmo.model.parser;

import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.schedule.exams.SExam;
import com.bukhmastov.cdoitmo.model.schedule.exams.SExams;
import com.bukhmastov.cdoitmo.model.schedule.exams.SSubject;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import org.htmlcleaner.TagNode;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleExamsTeacherParser extends Parser<SExams> {

    private final Pattern teacherPattern = Pattern.compile("^(([a-zа-яА-ЯёЁ]|\\s)*).*$", Pattern.CASE_INSENSITIVE);
    private final Pattern teacherReplacePattern = Pattern.compile("Ассистент|Доцент|Профессор|Ст\\.|препод\\.|Зав\\.|кафедрой|Преподаватель|Расписание\\sэкзаменов|Расписание\\sсессии", Pattern.CASE_INSENSITIVE);
    private final Pattern advicePattern = Pattern.compile("^Консультация (.{1,10}) в (\\d{1,2}:\\d{1,2}) Место:(.*)$", Pattern.CASE_INSENSITIVE);
    private final String query;

    public ScheduleExamsTeacherParser(@NonNull String html, @NonNull String query) {
        super(html);
        this.query = query;
    }

    @Override
    protected SExams doParse(TagNode root) throws Throwable {
        TagNode[] titles = root.getElementsByAttValue("class", "page-header", true, false);
        TagNode[] containers = root.getElementsByAttValue("class", "rasp_tabl_day", true, false);
        String title = "";
        SExams exams = new SExams();
        exams.setSchedule(new ArrayList<>());
        if (containers != null) {
            for (TagNode container : containers) {
                if (container == null) {
                    continue;
                }
                final TagNode[] fields = container.getAllElements(false)[0].getAllElements(false)[0].getAllElements(false)[0].getAllElements(false);
                if (fields == null || fields.length < 5) {
                    continue;
                }
                SSubject subject = new SSubject();
                SExam exam = new SExam();
                SExam advice = new SExam();
                exam.setDate(fields[0].getAllElements(false)[0].getText().toString().trim());
                exam.setTime(fields[1].getAllElements(false)[0].getText().toString().trim());
                exam.setRoom(fields[2].getAllElements(false)[0].getAllElements(false)[0].getText().toString().trim().replace(".", "").trim());
                subject.setGroup(fields[3].getAllElements(false)[0].getText().toString().trim());
                TagNode meta = fields[4].getAllElements(false)[0];
                if (meta != null) {
                    subject.setSubject(meta.getAllElements(false)[0].getText().toString().trim());
                    if (StringUtils.isBlank(title)) {
                        title = meta.getAllElements(false)[1].getText().toString().trim();
                    }
                    Matcher m = advicePattern.matcher(meta.getAllElements(false)[2].getText().toString().trim());
                    if (m.find()) {
                        advice.setDate(m.group(1));
                        advice.setTime(m.group(2));
                        advice.setRoom(m.group(3).replace(".", "").trim());
                    }
                }
                String type = fields[5].getText().toString().trim().toLowerCase();
                if (type.startsWith("зач")) {
                    subject.setType("credit");
                } else {
                    subject.setType("exam");
                }
                subject.setExam(exam);
                subject.setAdvice(advice);
                exams.getSchedule().add(subject);
            }
        }
        if (StringUtils.isBlank(title)) {
            title = query;
            if (titles != null && titles.length > 0) {
                title = titles[0].getText().toString().trim();
                title = teacherReplacePattern.matcher(title).replaceAll("");
                title = title.trim();
                Matcher m = teacherPattern.matcher(title);
                if (m.find()) {
                    title = m.group(1).trim();
                }
            }
        }
        exams.setTitle(title);
        return exams;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Parse.SCHEDULE_EXAMS_TEACHER;
    }
}
