package com.bukhmastov.cdoitmo.parse;

import com.bukhmastov.cdoitmo.exceptions.SilentException;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

public class ScheduleAttestationsParse implements Runnable {

    private static final String TAG = "SAParse";
    public interface response {
        void finish(JSONObject json);
    }
    private final response delegate;
    private final String data;
    private final int term;

    public ScheduleAttestationsParse(String data, int term, response delegate) {
        this.data = data;
        this.term = term;
        this.delegate = delegate;
    }

    @Override
    public void run() {
        Log.v(TAG, "parsing");
        try {
            final TagNode root = new HtmlCleaner().clean(data.replace("&nbsp;", " "));
            if (root == null) {
                throw new SilentException();
            }
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
            delegate.finish(new JSONObject().put("schedule", schedule));
        } catch (SilentException silent) {
            delegate.finish(null);
        } catch (Exception e) {
            Static.error(e);
            delegate.finish(null);
        }
    }
}
