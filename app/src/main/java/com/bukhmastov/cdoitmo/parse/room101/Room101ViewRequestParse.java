package com.bukhmastov.cdoitmo.parse.room101;

import android.content.Context;

import com.bukhmastov.cdoitmo.exception.SilentException;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.parse.Parse;
import com.bukhmastov.cdoitmo.util.Time;

import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class Room101ViewRequestParse extends Parse {

    private final Context context;

    @Inject
    Time time;

    public Room101ViewRequestParse(Context context, String data, Response delegate) {
        super(data, delegate);
        AppComponentProvider.getComponent().inject(this);
        this.context = context;
    }

    @Override
    protected JSONObject parse(TagNode root) throws Throwable {
        JSONObject response = new JSONObject();
        Matcher m;
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
        response.put("timestamp", time.getCalendar().getTimeInMillis());
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
                    date = ("0".equals(m.group(1)) ? "" : m.group(1)) + m.group(2) + " " + time.getGenitiveMonth(context, m.group(3)) + " " + m.group(4);
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
        return response;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Parse.ROOM_101_VIEW_REQUEST;
    }
}
