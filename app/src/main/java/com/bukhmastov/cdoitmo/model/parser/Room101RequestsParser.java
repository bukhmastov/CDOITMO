package com.bukhmastov.cdoitmo.model.parser;

import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.exception.SilentException;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.room101.requests.RSession;
import com.bukhmastov.cdoitmo.model.room101.requests.Room101Requests;

import org.htmlcleaner.TagNode;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Room101RequestsParser extends Parser<Room101Requests> {

    public Room101RequestsParser(@NonNull String html) {
        super(html);
    }

    @Override
    protected Room101Requests doParse(TagNode root) throws Throwable {
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
        Room101Requests room101Requests = new Room101Requests();
        room101Requests.setDate(tds[0].getText().toString().trim());
        room101Requests.setLimit(tds[1].getText().toString().trim());
        room101Requests.setLeft(tds[2].getText().toString().trim());
        room101Requests.setPenalty(tds[3].getText().toString().trim());
        room101Requests.setSessions(new ArrayList<>());
        for (int i = 1; i < trs.length; i++) {
            tds = trs[i].getAllElements(false);
            if (tds != null && tds.length >= 5) {
                RSession session = new RSession();
                String date = tds[1].getText().toString().trim();
                m = Pattern.compile("(\\d)(\\d).(\\d{2}).(\\d{2,4})").matcher(date);
                if (m.find()) {
                    date = ("0".equals(m.group(1)) ? "" : m.group(1)) + m.group(2) + " " + time.get().getGenitiveMonth(context, m.group(3)) + " " + m.group(4);
                }
                String time = tds[2].getText().toString().trim();
                String[] times = time.split("-");
                session.setNumber(tds[0].getText().toString().trim());
                session.setDate(date);
                session.setTime(time);
                session.setTimeStart(times[0].trim());
                session.setTimeEnd(times[1].trim());
                TagNode[] inputs = tds[3].getElementsByName("input", false);
                if (inputs != null && inputs.length > 0) {
                    session.setStatus(inputs[0].getAttributeByName("value").trim());
                    m = Pattern.compile(".*document\\.fn\\.reid\\.value=(\\d+).*").matcher(inputs[0].getAttributeByName("onclick").trim());
                    if (m.find()) {
                        int reid;
                        try {
                            reid = Integer.parseInt(m.group(1));
                        } catch (Exception ignore){
                            reid = 0;
                        }
                        session.setReid(reid);
                    } else {
                        session.setReid(0);
                    }
                } else {
                    session.setStatus(tds[3].getText().toString().trim());
                    session.setReid(0);
                }
                session.setRequested(tds[4].getText().toString().trim());
                room101Requests.getSessions().add(session);
            }
        }
        return room101Requests;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Parse.ROOM_101_VIEW_REQUEST;
    }
}
