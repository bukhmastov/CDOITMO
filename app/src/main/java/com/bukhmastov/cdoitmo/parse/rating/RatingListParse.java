package com.bukhmastov.cdoitmo.parse.rating;

import com.bukhmastov.cdoitmo.exception.SilentException;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.parse.JSONParse;
import com.bukhmastov.cdoitmo.parse.Parse;

import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RatingListParse extends JSONParse {

    public RatingListParse(String data, Response<JSONObject> delegate) {
        super(data, delegate);
    }

    @Override
    protected JSONObject parse(TagNode root) throws Throwable {
        TagNode page = root.findElementByAttValue("class", "c-page", true, false);
        if (page == null) {
            throw new SilentException();
        }
        TagNode div = page.findElementByAttValue("class", "p-inner nobt", false, false);
        if (div == null) {
            throw new SilentException();
        }
        TagNode[] spans = div.getElementsByName("span", false);
        JSONArray faculties = new JSONArray();
        if (spans != null) {
            for (TagNode span : spans) {
                if (span != null) {
                    JSONObject faculty = new JSONObject();
                    String name = span.getText().toString().trim();
                    Matcher matcher = Pattern.compile("^(.*) \\((.{1,10})\\)$").matcher(name);
                    if (matcher.find()) name = matcher.group(2) + " (" + matcher.group(1) + ")";
                    faculty.put("name", name);
                    faculties.put(faculty);
                }
            }
        }
        TagNode[] links = div.getElementsByAttValue("class", "big-links left", false, false);
        if (links != null) {
            for (int i = 0; i < faculties.length(); i++) {
                TagNode link = links[i];
                if (link != null) {
                    TagNode[] as = link.getElementsByName("a", false);
                    if (as != null && as.length > 0) {
                        String[] attrs = as[0].getAttributeByName("href").replace("&amp;", "&").split("&");
                        for (String attr : attrs) {
                            String[] pair = attr.split("=");
                            if ("depId".equals(pair[0])) faculties.getJSONObject(i).put("depId", pair[1]);
                        }
                    }
                }
            }
        }
        return new JSONObject().put("faculties", faculties);
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Parse.RATING_LIST;
    }
}
