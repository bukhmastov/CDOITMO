package com.bukhmastov.cdoitmo.converters;

import android.os.AsyncTask;

import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class ProtocolConverter extends AsyncTask<JSONArray, Void, JSONObject> {
    public interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    public ProtocolConverter(response delegate){
        this.delegate = delegate;
    }
    @Override
    protected JSONObject doInBackground(JSONArray... params) {
        JSONObject response = new JSONObject();
        try {
            JSONArray protocol = params[0];
            for (int i = 0; i < protocol.length(); i++) {
                JSONObject item = protocol.getJSONObject(i);
                //String hash = Static.crypt(getCast(item));
                //item.put("cdoitmo_hash", hash);
                protocol.put(i, item);
            }
            response.put("timestamp", Calendar.getInstance().getTimeInMillis());
            response.put("number_of_weeks", params[1].getInt(0));
            response.put("protocol", protocol);
        } catch (Exception e) {
            Static.error(e);
        }
        return response;
    }
    private String getCast(JSONObject item) throws JSONException {
        final String separator = "#";
        JSONObject var = item.getJSONObject("var");
        return (new StringBuilder())
                .append(item.getString("subject")).append(separator)
                .append(item.getString("value")).append(separator)
                .append(item.getString("date")).append(separator)
                .append(item.getString("sign")).append(separator)
                .append(var.getString("name")).append(separator)
                .append(var.getString("min")).append(separator)
                .append(var.getString("max")).append(separator)
                .append(var.getString("threshold"))
                .toString();
    }
    @Override
    protected void onPostExecute(JSONObject json) {
        delegate.finish(json);
    }
}
