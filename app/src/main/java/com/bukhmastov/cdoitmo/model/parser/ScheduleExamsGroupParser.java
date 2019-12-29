package com.bukhmastov.cdoitmo.model.parser;

import androidx.annotation.NonNull;

import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.schedule.exams.SExam;
import com.bukhmastov.cdoitmo.model.schedule.exams.SExams;
import com.bukhmastov.cdoitmo.model.schedule.exams.SSubject;

import org.htmlcleaner.TagNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleExamsGroupParser extends Parser<SExams> {

    private final Pattern groupPattern = Pattern.compile("[a-zа-яё]\\d{4,}[a-zа-яё]?", Pattern.CASE_INSENSITIVE);
    private final Pattern advicePattern = Pattern.compile("^.*конс.*$", Pattern.CASE_INSENSITIVE);
    private final Pattern creditDiffPattern = Pattern.compile("^.*диф.*зач.*$", Pattern.CASE_INSENSITIVE);
    private final Pattern creditPattern = Pattern.compile("^.*зач.*$", Pattern.CASE_INSENSITIVE);
    private final String query;

    public ScheduleExamsGroupParser(@NonNull String html, @NonNull String query) {
        super(html);
        this.query = query;
    }

    @Override
    protected SExams doParse(TagNode root) {
        TagNode[] titles = root.getElementsByAttValue("class", "page-header", true, false);
        TagNode[] containers = root.getElementsByAttValue("class", "rasp_tabl_day", true, false);
        String title = query;
        if (titles != null && titles.length > 0) {
            title = titles[0].getText().toString().trim();
            Matcher m = groupPattern.matcher(title);
            if (m.find()) {
                title = m.group().trim();
            }
        }
        List<SSubject> subjects = new ArrayList<>();
        if (containers != null) {
            for (TagNode container : containers) {
                if (container == null) {
                    continue;
                }
                TagNode[] fields = container.getAllElements(false)[0].getAllElements(false)[0].getAllElements(false)[0].getAllElements(false);
                if (fields == null || fields.length < 4) {
                    continue;
                }
                SSubject subject = new SSubject();
                SExam entry = new SExam();
                entry.setDate(getTextFromNode(fields[0]));
                entry.setTime(getTextFromNode(fields[1].getAllElements(false)[0]));
                entry.setRoom(getTextFromNode(fields[2].getAllElements(false)[0].getAllElements(false)[0]).replace("\n", " ").trim());
                TagNode meta = fields[3].getAllElements(false)[0];
                if (meta != null) {
                    subject.setSubject(getTextFromNode(meta.getAllElements(false)[0]));
                    subject.setTeacherName(getTextFromNode(meta.getAllElements(false)[1]));
                }
                String type = getTextFromNode(fields[4]);
                if (advicePattern.matcher(type).find()) {
                    subject.setAdvice(entry);
                } else {
                    subject.setType(getType(type));
                    subject.setExam(entry);
                }
                subjects.add(subject);
            }
        }
        SExams exams = new SExams();
        exams.setTitle(title);
        exams.setSchedule(new ArrayList<>(mergeAdvices(subjects)));
        return exams;
    }

    private String getTextFromNode(TagNode tagNode) {
        if (tagNode == null) {
            return "";
        }
        CharSequence text = tagNode.getText();
        if (text == null) {
            return "";
        }
        return text.toString().trim();
    }

    private String getType(String type) {
        if (creditDiffPattern.matcher(type).find()) {
            return "diffcredit";
        }
        if (creditPattern.matcher(type).find()) {
            return "credit";
        }
        return "exam";
    }

    private List<SSubject> mergeAdvices(List<SSubject> subjects) {
        List<SSubject> advices = new ArrayList<>();
        Iterator<SSubject> it = subjects.iterator();
        while (it.hasNext()) {
            SSubject subject = it.next();
            if (subject.getAdvice() != null && subject.getExam() == null) {
                advices.add(subject);
                it.remove();
            }
        }
        for (SSubject advice : advices) {
            for (SSubject subject : subjects) {
                if (subject.getAdvice() != null) {
                    continue;
                }
                if (!isSubjectsMatch(subject, advice)) {
                    continue;
                }
                subject.setAdvice(advice.getAdvice());
                break;
            }
        }
        return subjects;
    }

    private boolean isSubjectsMatch(SSubject first, SSubject second) {
        if (!Objects.equals(first.getSubject(), second.getSubject())) {
            return false;
        }
        if (!Objects.equals(first.getGroup(), second.getGroup())) {
            return false;
        }
        if (!Objects.equals(first.getTeacherName(), second.getTeacherName())) {
            return false;
        }
        return true;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Parse.SCHEDULE_EXAMS_GROUP;
    }
}
