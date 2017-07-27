package com.bukhmastov.cdoitmo.parse;

import android.os.AsyncTask;

import com.bukhmastov.cdoitmo.utils.Static;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RatingListParse extends AsyncTask<String, Void, JSONObject> {
    public interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    public RatingListParse(response delegate){
        this.delegate = delegate;
    }
    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            String response = params[0].replace("&nbsp;", " ");
            TagNode root = new HtmlCleaner().clean(response);
            TagNode div = root.findElementByAttValue("class", "c-page", true, false).findElementByAttValue("class", "p-inner nobt", false, false);
            TagNode[] spans = div.getElementsByName("span", false);
            JSONArray faculties = new JSONArray();
            for (TagNode span : spans) {
                JSONObject faculty = new JSONObject();
                String name = span.getText().toString().trim();
                Matcher matcher = Pattern.compile("^(.*) \\((.{1,10})\\)$").matcher(name);
                if(matcher.find()) name = matcher.group(2) + " (" + matcher.group(1) + ")";
                faculty.put("name", name);
                faculties.put(faculty);
            }
            TagNode[] links = div.getElementsByAttValue("class", "big-links left", false, false);
            for (int i = 0; i < faculties.length(); i++) {
                TagNode link = links[i];
                TagNode[] as = link.getElementsByName("a", false);
                if (as != null && as.length > 0) {
                    String[] attrs = as[0].getAttributeByName("href").replace("&amp;", "&").split("&");
                    for (String attr : attrs) {
                        String[] pair = attr.split("=");
                        if (Objects.equals(pair[0], "depId")) faculties.getJSONObject(i).put("depId", pair[1]);
                    }
                }
            }
            return new JSONObject().put("faculties", faculties);
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