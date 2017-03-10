package com.bukhmastov.cdoitmo.objects;

import android.content.Context;
import android.preference.PreferenceManager;

import com.bukhmastov.cdoitmo.utils.Cache;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class Protocol {

    private static final String TAG = "Protocol";
    private Context context;
    private JSONObject protocol = null;

    public Protocol(Context context){
        this.context = context;
        String protocol = Cache.get(context, "Protocol");
        if(!Objects.equals(protocol, "")){
            try {
                this.protocol = new JSONObject(protocol);
            } catch (Exception e) {
                Static.error(e);
            }
        }
    }
    public void put(JSONArray data, int number_of_weeks){
        try {
            JSONObject json = new JSONObject();
            json.put("timestamp", Calendar.getInstance().getTimeInMillis());
            json.put("date", new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT).format(new Date(Calendar.getInstance().getTimeInMillis())));
            json.put("number_of_weeks", number_of_weeks);
            json.put("protocol", data);
            protocol = json;
            Cache.put(context, "Protocol", protocol.toString());
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString("ProtocolTrackerHISTORY", data.toString()).apply();
        } catch (Exception e) {
            Static.error(e);
        }
    }
    public JSONObject get(){
        return protocol;
    }
    public boolean is(){
        return protocol != null;
    }
    public boolean is(int number_of_weeks){
        if (protocol == null) {
            return false;
        } else {
            try {
                return protocol.getInt("number_of_weeks") == number_of_weeks;
            } catch (JSONException e) {
                Static.error(e);
                return false;
            }
        }
    }

}
