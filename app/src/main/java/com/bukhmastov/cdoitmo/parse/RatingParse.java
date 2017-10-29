package com.bukhmastov.cdoitmo.parse;

import com.bukhmastov.cdoitmo.exceptions.SilentException;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class RatingParse implements Runnable {

    private static final String TAG = "RatingParse";
    public interface response {
        void finish(JSONObject json);
    }
    private final response delegate;
    private final String data;

    public RatingParse(String data, response delegate) {
        this.data = data;
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
            JSONObject json = new JSONObject();
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
            json.put("courses", courses);
            json.put("max_course", max_course);
            delegate.finish(json);
        } catch (SilentException silent) {
            delegate.finish(null);
        } catch (Exception e) {
            Static.error(e);
            delegate.finish(null);
        }
    }
}
