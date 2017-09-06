package com.bukhmastov.cdoitmo.parse;

import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RatingListParse implements Runnable {

    private static final String TAG = "RatingListParse";
    public interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    private String data;

    public RatingListParse(String data, response delegate) {
        this.data = data;
        this.delegate = delegate;
    }

    @Override
    public void run() {
        Log.v(TAG, "parsing");
        try {
            String response = data.replace("&nbsp;", " ");
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
            delegate.finish(new JSONObject().put("faculties", faculties));
        } catch (Exception e) {
            Static.error(e);
            delegate.finish(null);
        }
    }
}
