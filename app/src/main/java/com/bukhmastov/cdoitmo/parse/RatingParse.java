package com.bukhmastov.cdoitmo.parse;

import android.os.AsyncTask;

import com.bukhmastov.cdoitmo.utils.Static;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class RatingParse extends AsyncTask<String, Void, JSONObject> {
    public interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    public RatingParse(response delegate){
        this.delegate = delegate;
    }
    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            String response = params[0].replace("&nbsp;", " ");
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(response);
            JSONObject json = new JSONObject();
            TagNode div = root.findElementByAttValue("class", "d_text", true, false);
            TagNode table = div.getElementListByAttValue("class", "d_table", true, false).get(1);
            List<? extends TagNode> rows = table.getAllElementsList(false).get(0).getAllElementsList(false);
            int max_course = 1;
            JSONArray courses = new JSONArray();
            for(TagNode row : rows){
                if(row.getText().toString().contains("Позиция")) continue;
                List<? extends TagNode> columns = row.getAllElementsList(false);
                JSONObject course = new JSONObject();
                course.put("faculty", columns.get(0).getText().toString().trim());
                int course_value = Integer.parseInt(columns.get(1).getText().toString().trim());
                if(course_value > max_course) max_course = course_value;
                course.put("course", course_value);
                course.put("position", columns.get(2).getText().toString().trim());
                courses.put(course);
            }
            json.put("courses", courses);
            json.put("max_course", max_course);
            return json;
        } catch (Exception e) {
            Static.error(e);
            return null;
        }
    }
    @Override
    protected void onPostExecute(JSONObject json) {
        delegate.finish(json);
    }
}
