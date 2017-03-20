package com.bukhmastov.cdoitmo.objects;

import android.content.Context;
import android.os.Handler;

import com.bukhmastov.cdoitmo.converters.ProtocolConverter;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Protocol {

    private static final String TAG = "Protocol";
    private Context context;
    private JSONObject protocol = null;

    public Protocol(Context context){
        this.context = context;
        String protocol = Storage.file.cache.get(context, "protocol#core");
        if (!protocol.isEmpty()) {
            try {
                this.protocol = new JSONObject(protocol);
            } catch (Exception e) {
                Static.error(e);
            }
        }
    }
    public void put(JSONArray data, int number_of_weeks, final Handler handler){
        try {
            JSONArray array = new JSONArray();
            array.put(number_of_weeks);
            new ProtocolConverter(context, new ProtocolConverter.response() {
                @Override
                public void finish(JSONObject json) {
                    try {
                        protocol = json;
                        Storage.file.cache.put(context, "protocol#core", protocol.toString());
                        Storage.file.perm.put(context, "protocol_tracker#protocol", protocol.getJSONArray("protocol").toString());
                        handler.sendEmptyMessage(0);
                    } catch (JSONException e) {
                        Static.error(e);
                    }
                }
            }).execute(data, array);
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
