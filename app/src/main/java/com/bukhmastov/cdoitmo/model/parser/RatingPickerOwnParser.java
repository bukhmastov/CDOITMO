package com.bukhmastov.cdoitmo.model.parser;

import androidx.annotation.NonNull;

import com.bukhmastov.cdoitmo.exception.SilentException;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.rating.pickerown.RCourse;
import com.bukhmastov.cdoitmo.model.rating.pickerown.RatingPickerOwn;

import org.htmlcleaner.TagNode;

import java.util.ArrayList;
import java.util.List;

public class RatingPickerOwnParser extends Parser<RatingPickerOwn> {

    public RatingPickerOwnParser(@NonNull String html) {
        super(html);
    }

    @Override
    protected RatingPickerOwn doParse(TagNode root) throws Throwable {
        TagNode div = root.findElementByAttValue("class", "d_text", true, false);
        if (div == null) {
            throw new SilentException();
        }
        TagNode table = div.getElementListByAttValue("class", "d_table", true, false).get(1);
        if (table == null) {
            throw new SilentException();
        }
        List<? extends TagNode> rows = table.getAllElementsList(false).get(0).getAllElementsList(false);
        if (rows == null) {
            throw new SilentException();
        }
        int maxCourse = 1;
        RatingPickerOwn ratingPickerOwn = new RatingPickerOwn();
        ratingPickerOwn.setCourses(new ArrayList<>());
        for (TagNode row : rows) {
            if (row == null || row.getText().toString().toLowerCase().contains("позиция")) {
                continue;
            }
            List<? extends TagNode> columns = row.getAllElementsList(false);
            if (columns != null) {
                int courseNumber = Integer.parseInt(columns.get(1).getText().toString().trim());
                if (courseNumber > maxCourse) {
                    maxCourse = courseNumber;
                }
                RCourse course = new RCourse();
                course.setFaculty(columns.get(0).getText().toString().trim());
                course.setCourse(courseNumber);
                course.setPosition(columns.get(2).getText().toString().trim());
                ratingPickerOwn.getCourses().add(course);
            }
        }
        ratingPickerOwn.setMaxCourse(maxCourse);
        return ratingPickerOwn;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Parse.RATING;
    }
}
