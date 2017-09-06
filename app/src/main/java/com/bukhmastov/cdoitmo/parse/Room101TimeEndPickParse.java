package com.bukhmastov.cdoitmo.parse;

import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

public class Room101TimeEndPickParse implements Runnable {

    private static final String TAG = "R101TimeEndPickParse";
    public interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    private String data;

    public Room101TimeEndPickParse(String data, response delegate) {
        this.data = data;
        this.delegate = delegate;
    }

    @Override
    public void run() {
        Log.v(TAG, "parsing");
        try {
            JSONObject response = new JSONObject();
            response.put("type", "time_end_pick");
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(data.replace("&nbsp;", " "));
            TagNode[] tables = root.getElementsByAttValue("class", "d_table min_lmargin_table", true, false);
            if (tables == null || tables.length == 0) {
                response.put("data", new JSONArray());
                delegate.finish(response);
                return;
            }
            TagNode table = tables[0];
            TagNode[] trs = table.getElementsByName("tbody", false)[0].getElementsByName("tr", false);
            int counter = 0;
            JSONArray times = new JSONArray();
            for (TagNode tr : trs) {
                counter++;
                if (counter == 1) continue;
                TagNode td = tr.getElementsByName("td", false)[1];
                TagNode[] inputs = td.getElementsByName("input", false);
                if (inputs == null || inputs.length == 0) continue;
                TagNode input = inputs[0];
                if (!input.hasAttribute("disabled")) {
                    String value = input.getAttributeByName("value");
                    if (value != null) {
                        JSONObject session = new JSONObject();
                        session.put("time", value);
                        try {
                            session.put("available", tr.getElementsByName("td", false)[2].getText().toString().trim());
                        } catch (Exception e) {
                            session.put("available", "");
                        }
                        times.put(session);
                    }
                }
            }
            response.put("data", times);
            delegate.finish(response);
        } catch (Exception e) {
            Static.error(e);
            delegate.finish(null);
        }
    }
}
