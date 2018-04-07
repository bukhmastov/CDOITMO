package com.bukhmastov.cdoitmo.parse.room101;

import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.parse.Parse;

import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

public class Room101TimeStartPickParse extends Parse {

    public Room101TimeStartPickParse(String data, Response delegate) {
        super(data, delegate);
    }

    @Override
    protected JSONObject parse(TagNode root) throws Throwable {
        JSONObject response = new JSONObject();
        response.put("type", "time_start_pick");
        TagNode[] tables = root.getElementsByAttValue("class", "d_table min_lmargin_table", true, false);
        if (tables == null || tables.length == 0) {
            response.put("data", new JSONArray());
            return response;
        }
        JSONArray times = new JSONArray();
        TagNode table = tables[0];
        if (table != null) {
            TagNode[] trs = table.getElementsByName("tbody", false)[0].getElementsByName("tr", false);
            if (trs != null) {
                int counter = 0;
                for (TagNode tr : trs) {
                    counter++;
                    if (tr == null || counter == 1) continue;
                    TagNode td = tr.getElementsByName("td", false)[0];
                    if (td == null) continue;
                    TagNode[] inputs = td.getElementsByName("input", false);
                    if (inputs == null || inputs.length == 0) continue;
                    TagNode input = inputs[0];
                    if (input != null && !input.hasAttribute("disabled")) {
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
            }
        }
        response.put("data", times);
        return response;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Parse.ROOM_101_TIME_START_PICK;
    }
}
