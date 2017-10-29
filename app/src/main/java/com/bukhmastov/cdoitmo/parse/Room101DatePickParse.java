package com.bukhmastov.cdoitmo.parse;

import com.bukhmastov.cdoitmo.exceptions.SilentException;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Room101DatePickParse implements Runnable {

    private static final String TAG = "R101DatePickParse";
    public interface response {
        void finish(JSONObject json);
    }
    private final response delegate;
    private final String data;

    public Room101DatePickParse(String data, response delegate) {
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
            JSONObject response = new JSONObject();
            response.put("type", "date_pick");
            TagNode[] tables = root.getElementsByAttValue("class", "d_table2 calendar_1", true, false);
            if (tables == null || tables.length == 0) {
                response.put("data", new JSONArray());
                delegate.finish(response);
                return;
            }
            JSONArray dates = new JSONArray();
            TagNode table = tables[0];
            if (table != null) {
                TagNode[] trs = table.getElementsByName("tbody", false)[0].getElementsByName("tr", false);
                if (trs != null) {
                    int counter = 0;
                    for (TagNode tr : trs) {
                        counter++;
                        if (tr == null || counter == 1 || counter == 2) continue;
                        TagNode[] tds = tr.getElementsByName("td", false);
                        if (tds == null) continue;
                        for (TagNode td : tds) {
                            if (td == null || !td.hasChildren()) continue;
                            TagNode[] inputs = td.getElementsByName("input", false);
                            if (inputs == null || inputs.length == 0) continue;
                            TagNode input = inputs[0];
                            if (input != null && !input.hasAttribute("disabled")) {
                                String onclick = input.getAttributeByName("onclick");
                                if (onclick != null && !onclick.isEmpty()) {
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
                }
            }
            response.put("data", dates);
            delegate.finish(response);
        } catch (SilentException silent) {
            delegate.finish(null);
        } catch (Exception e) {
            Static.error(e);
            delegate.finish(null);
        }
    }
}
