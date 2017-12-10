package com.bukhmastov.cdoitmo.parse;

import android.content.Context;

import com.bukhmastov.cdoitmo.exceptions.SilentException;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Room101ViewRequestParse implements Runnable {

    private static final String TAG = "R101ViewRequestParse";
    public interface response {
        void finish(JSONObject json);
    }
    private final response delegate;
    private final Context context;
    private final String data;

    public Room101ViewRequestParse(Context context, String data, response delegate) {
        this.context = context;
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
            Matcher m;
            JSONObject response = new JSONObject();
            TagNode multi_table = root.getElementsByAttValue("class", "multi_table", true, false)[0];
            if (multi_table == null) {
                throw new SilentException();
            }
            TagNode[] d_table = multi_table.getElementsByName("table", true);
            if (d_table == null) {
                throw new SilentException();
            }
            TagNode[] tds = d_table[0].getAllElements(false)[0].getAllElements(false)[1].getAllElements(false);
            TagNode[] trs = d_table[1].getAllElements(false)[0].getAllElements(false);
            if (tds == null || trs == null) {
                throw new SilentException();
            }
            response.put("timestamp", Static.getCalendar().getTimeInMillis());
            response.put("date", tds[0].getText().toString().trim());
            response.put("limit", tds[1].getText().toString().trim());
            response.put("left", tds[2].getText().toString().trim());
            response.put("penalty", tds[3].getText().toString().trim());
            JSONArray sessions = new JSONArray();
            for (int i = 1; i < trs.length; i++) {
                tds = trs[i].getAllElements(false);
                if (tds != null && tds.length >= 5) {
                    JSONObject session = new JSONObject();
                    String date = tds[1].getText().toString().trim();
                    m = Pattern.compile("(\\d)(\\d).(\\d{2}).(\\d{2,4})").matcher(date);
                    if (m.find()) {
                        date = ("0".equals(m.group(1)) ? "" : m.group(1)) + m.group(2) + " " + Static.getGenitiveMonth(context, m.group(3)) + " " + m.group(4);
                    }
                    String time = tds[2].getText().toString().trim();
                    String[] times = time.split("-");
                    session.put("number", tds[0].getText().toString().trim());
                    session.put("date", date);
                    session.put("time", time);
                    session.put("timeStart", times[0].trim());
                    session.put("timeEnd", times[1].trim());
                    TagNode[] inputs = tds[3].getElementsByName("input", false);
                    if (inputs != null && inputs.length > 0) {
                        session.put("status", inputs[0].getAttributeByName("value").trim());
                        m = Pattern.compile(".*document\\.fn\\.reid\\.value=(\\d+).*").matcher(inputs[0].getAttributeByName("onclick").trim());
                        if (m.find()) {
                            int reid;
                            try {
                                reid = Integer.parseInt(m.group(1));
                            } catch (Exception ignore){
                                reid = 0;
                            }
                            session.put("reid", reid);
                        } else {
                            session.put("reid", 0);
                        }
                    } else {
                        session.put("status", tds[3].getText().toString().trim());
                        session.put("reid", 0);
                    }
                    session.put("requested", tds[4].getText().toString().trim());
                    sessions.put(session);
                }
            }
            response.put("sessions", sessions);
            delegate.finish(response);
        } catch (SilentException silent) {
            delegate.finish(null);
        } catch (Exception e) {
            Static.error(e);
            delegate.finish(null);
        }
    }
}
