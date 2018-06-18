package com.bukhmastov.cdoitmo.parse.rating;

import com.bukhmastov.cdoitmo.exception.SilentException;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.parse.JSONParse;
import com.bukhmastov.cdoitmo.parse.Parse;

import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class RatingParse extends JSONParse {

    public RatingParse(String data, Response<JSONObject> delegate) {
        super(data, delegate);
    }

    @Override
    protected JSONObject parse(TagNode root) throws Throwable {
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
        int max_course = 1;
        JSONArray courses = new JSONArray();
        for (TagNode row : rows) {
            if (row == null || row.getText().toString().toLowerCase().contains("позиция")) {
                continue;
            }
            List<? extends TagNode> columns = row.getAllElementsList(false);
            if (columns != null) {
                JSONObject course = new JSONObject();
                course.put("faculty", columns.get(0).getText().toString().trim());
                int course_value = Integer.parseInt(columns.get(1).getText().toString().trim());
                if (course_value > max_course) max_course = course_value;
                course.put("course", course_value);
                course.put("position", columns.get(2).getText().toString().trim());
                courses.put(course);
            }
        }
        return new JSONObject()
                .put("courses", courses)
                .put("max_course", max_course);
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Parse.RATING;
    }
}
