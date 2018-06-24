package com.bukhmastov.cdoitmo.converter;

import android.content.Context;

import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Time;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProtocolConverter extends Converter {

    private final Context context;
    private final JSONArray protocol;
    private final int number_of_weeks;

    //@Inject
    private Storage storage = Storage.instance();
    //@Inject
    private StoragePref storagePref = StoragePref.instance();

    public ProtocolConverter(Context context, JSONArray protocol, int number_of_weeks, Response delegate) {
        super(delegate);
        this.context = context;
        this.protocol = protocol;
        this.number_of_weeks = number_of_weeks;
    }

    @Override
    protected JSONObject convert() throws Throwable {
        for (int i = 0; i < protocol.length(); i++) {
            JSONObject item = markConvert(protocol.getJSONObject(i));
            String hash = TextUtils.crypt(getCast(item));
            Double value, oldValue, delta, oldDelta;
            if (storagePref.get(context, "pref_protocol_changes_track_title", true)) {
                String changeLogItemString = storage.get(context, Storage.CACHE, Storage.USER, "protocol#log#" + hash, null);
                if (changeLogItemString == null) {
                    oldValue = null;
                    oldDelta = null;
                } else {
                    try {
                        JSONObject changeLogItem = new JSONObject(changeLogItemString);
                        oldValue = changeLogItem.getDouble("value");
                        oldDelta = changeLogItem.getDouble("delta");
                    } catch (Exception e) {
                        oldValue = null;
                        oldDelta = null;
                    }
                }
                value = string2double(item.getString("value"));
                if (oldValue == null) {
                    delta = 0.0;
                } else {
                    delta = value - oldValue;
                    if (delta == 0.0) {
                        delta = oldDelta;
                    }
                }
                JSONObject log = new JSONObject();
                log.put("value", value);
                log.put("delta", delta);
                storage.put(context, Storage.CACHE, Storage.USER, "protocol#log#" + hash, log.toString());
            } else {
                delta = 0.0;
            }
            delta = round(delta);
            item.put("cdoitmo_hash", hash);
            item.put("cdoitmo_delta", markConverter(String.valueOf(delta), true));
            item.put("cdoitmo_delta_double", delta);
            protocol.put(i, item);
        }
        return new JSONObject()
                .put("timestamp", Time.getCalendar().getTimeInMillis())
                .put("number_of_weeks", number_of_weeks)
                .put("protocol", protocol);
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Convert.PROTOCOL;
    }

    private String getCast(JSONObject item) throws JSONException {
        final String separator = "#";
        JSONObject var = item.getJSONObject("var");
        return (new StringBuilder())
                .append(item.getString("subject")).append(separator)
                .append(var.getString("name")).append(separator)
                .append(var.getString("min")).append(separator)
                .append(var.getString("max")).append(separator)
                .append(var.getString("threshold")).append(separator)
                .toString();
    }
    private JSONObject markConvert(JSONObject item) throws JSONException {
        item.put("value", markConverter(item.getString("value")));
        JSONObject var = item.getJSONObject("var");
        var.put("min", markConverter(var.getString("min")));
        var.put("max", markConverter(var.getString("max")));
        var.put("threshold", markConverter(var.getString("threshold")));
        item.put("var", var);
        return item;
    }
    private String markConverter(String value){
        return markConverter(value, false);
    }
    private String markConverter(String value, boolean withSign){
        Matcher m;
        value = value.replace(",", ".").trim();
        m = Pattern.compile("^\\.(.+)").matcher(value);
        if (m.find()) value = "0." + m.group(1);
        m = Pattern.compile("(.+)\\.0$").matcher(value);
        if (m.find()) value = m.group(1);
        if (withSign) {
            m = Pattern.compile("^(\\D?)(\\d*\\.?\\d*)$").matcher(value);
            if (m.find() && m.group(1).isEmpty()) {
                value = "+" + m.group(2);
            }
        }
        return value;
    }
    private double string2double(String string){
        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    private double round(double d) {
        int precise = 100;
        d = d * precise;
        int i = (int) Math.round(d);
        return (double) i / precise;
    }
}
