package com.bukhmastov.cdoitmo.parse.schedule;

import com.bukhmastov.cdoitmo.exception.SilentException;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.parse.Parse;
import com.bukhmastov.cdoitmo.util.Static;

import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

public class ScheduleAttestationsParse extends Parse {

    private final int term;

    public ScheduleAttestationsParse(String data, int term, Response delegate) {
        super(data, delegate);
        this.term = term;
    }

    @Override
    protected JSONObject parse(TagNode root) throws Throwable {
        final TagNode[] content = root.getElementsByAttValue("class", "c-page", true, false);
        if (content == null || content.length == 0) {
            throw new SilentException();
        }
        final TagNode[] titles = content[0].getElementsByName("h4", true);
        final TagNode[] tables = content[0].getElementsByName("table", true);
        if (titles == null || tables == null) {
            throw new SilentException();
        }
        final int length = Math.min(titles.length, tables.length);
        final JSONArray schedule = new JSONArray();
        for (int i = 0; i < length; i++) {
            try {
                final String subject = titles[i].getText().toString().trim();
                final TagNode[] trs = tables[i].getElementsByName("tr", true);
                final JSONArray attestations = new JSONArray();
                for (TagNode tr : trs) {
                    final TagNode[] tds = tr.getElementsByName("td", true);
                    if (tds.length < 2) continue;
                    attestations.put(new JSONObject()
                            .put("name", tds[0].getText().toString().trim())
                            .put("week", tds[1].getText().toString().trim())
                    );
                }
                if (attestations.length() > 0) {
                    schedule.put(new JSONObject()
                            .put("name", subject)
                            .put("term", term)
                            .put("attestations", attestations)
                    );
                }
            } catch (Exception e) {
                Static.error(e);
            }
        }
        return new JSONObject().put("schedule", schedule);
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Parse.SCHEDULE_ATTESTATIONS;
    }
}
