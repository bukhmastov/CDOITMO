package com.bukhmastov.cdoitmo.model.parser;

import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.exception.SilentException;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.rating.top.RStudent;
import com.bukhmastov.cdoitmo.model.rating.top.RatingTopList;

import org.htmlcleaner.TagNode;

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RatingTopListParser extends Parser<RatingTopList> {

    private final String username;

    public RatingTopListParser(@NonNull String html, String username) {
        super(html);
        this.username = username;
    }

    @Override
    protected RatingTopList doParse(TagNode root) throws Throwable {
        TagNode page = root.findElementByAttValue("class", "c-page", true, false);
        if (page == null) {
            throw new SilentException();
        }
        TagNode div = page.findElementByAttValue("class", "p-inner nobt", false, false);
        if (div == null) {
            throw new SilentException();
        }
        RatingTopList ratingTopList = new RatingTopList();
        ratingTopList.setStudents(new ArrayList<>());
        String header = "";
        Matcher m;
        m = Pattern.compile("Рейтинг студентов (.*)").matcher(div.findElementByAttValue("class", "notop", false, false).getText().toString().trim());
        if (m.find()) {
            header = m.group(1).trim();
        }
        m = Pattern.compile("^(.*) учебный год, (.*)$").matcher(div.findElementByAttValue("class", "info", false, false).getText().toString().trim());
        if (m.find()) {
            if (!header.isEmpty()) {
                header += " - ";
            }
            header += m.group(2) + " (" + m.group(1).replace(" ", "") + ")";
        }
        ratingTopList.setHeader(header);
        TagNode[] trs = div.findElementByAttValue("class", "table-rating", false, false).getElementsByName("tbody", false)[0].getElementsByName("tr", false);
        if (trs != null) {
            int counter = 0;
            for (TagNode tr : trs) {
                if (counter++ == 0 || tr == null) continue;
                TagNode[] tds = tr.getElementsByName("td", false);
                if (tds == null || tds.length == 0) continue;
                String number = tds[0].getText().toString().trim();
                String fio = tds[1].getText().toString().trim();
                String meta = tds[3].getText().toString().trim();
                RStudent student = new RStudent();
                student.setNumber(Integer.parseInt(number));
                student.setFio(fio);
                m = Pattern.compile("гр. (.*), каф. (.*)").matcher(meta);
                if (m.find()) {
                    student.setGroup(m.group(1).trim());
                    student.setDepartment(m.group(2).trim());
                } else {
                    student.setGroup("");
                    student.setDepartment("");
                }
                student.setMe(Objects.equals(fio, username));
                TagNode[] is = tds[2].getAllElements(false);
                if (is != null && is.length > 0) {
                    m = Pattern.compile("^icon-expand_.* (.*)$").matcher(is[0].getAttributeByName("class"));
                    if (m.find()) {
                        student.setChange(m.group(1).trim());
                    } else {
                        student.setChange("none");
                    }
                    student.setDelta(is[0].getAttributeByName("title"));
                } else {
                    student.setChange("none");
                    student.setDelta("0");
                }
                ratingTopList.getStudents().add(student);
            }
        }
        return ratingTopList;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Parse.RATING_TOP_LIST;
    }
}
