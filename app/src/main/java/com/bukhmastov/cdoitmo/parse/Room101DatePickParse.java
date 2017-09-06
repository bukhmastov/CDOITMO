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

public class Room101DatePickParse implements Runnable {

    private static final String TAG = "R101DatePickParse";
    public interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    private String data;

    public Room101DatePickParse(String data, response delegate) {
        this.data = data;
        this.delegate = delegate;
    }

    @Override
    public void run() {
        Log.v(TAG, "parsing");
        try {
            JSONObject response = new JSONObject();
            response.put("type", "date_pick");
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(data.replace("&nbsp;", " "));
            TagNode[] tables = root.getElementsByAttValue("class", "d_table2 calendar_1", true, false);
            if (tables == null || tables.length == 0) {
                response.put("data", new JSONArray());
                delegate.finish(response);
                return;
            }
            TagNode table = tables[0];
            TagNode[] trs = table.getElementsByName("tbody", false)[0].getElementsByName("tr", false);
            int counter = 0;
            JSONArray dates = new JSONArray();
            for (TagNode tr : trs) {
                counter++;
                if (counter == 1 || counter == 2) continue;
                TagNode[] tds = tr.getElementsByName("td", false);
                for(TagNode td : tds){
                    if (!td.hasChildren()) continue;
                    TagNode[] inputs = td.getElementsByName("input", false);
                    if (inputs == null || inputs.length == 0) continue;
                    TagNode input = inputs[0];
                    if (!input.hasAttribute("disabled")) {
                        String onclick = input.getAttributeByName("onclick");
                        if (onclick != null && !Objects.equals(onclick, "")) {
                            Matcher m = Pattern.compile(".*dateRequest\\.value='(.*)';.*").matcher(onclick);
                            if (m.find()) {
                                JSONObject session = new JSONObject();
                                session.put("time", m.group(1));
                                session.put("available", "");
                                dates.put(session);
                            }
                        }
                    }
                }
            }
            response.put("data", dates);
            delegate.finish(response);
        } catch (Exception e) {
            Static.error(e);
            delegate.finish(null);
        }
    }
}
