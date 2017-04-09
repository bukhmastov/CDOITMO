package com.bukhmastov.cdoitmo.objects;

import android.content.Context;
import android.os.Handler;

import com.bukhmastov.cdoitmo.converters.ERegisterConverter;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONObject;

public class ERegister {

    private static final String TAG = "ERegister";
    private Context context;
    private JSONObject eregister = null;

    public ERegister(Context context){
        this.context = context;
        String eRegister = Storage.file.cache.get(context, "eregister#core");
        if (!eRegister.isEmpty()) {
            try {
                this.eregister = new JSONObject(eRegister);
            } catch (Exception e) {
                Static.error(e);
            }
        }
    }
    public void put(JSONObject data, final Handler handler){
        new ERegisterConverter(new ERegisterConverter.response() {
            @Override
            public void finish(JSONObject json) {
                eregister = json;
                Storage.file.cache.put(context, "eregister#core", eregister.toString());
                handler.sendEmptyMessage(0);
            }
        }).execute(data);
    }
    public JSONObject get(){
        return eregister;
    }
    public boolean is(){
        return eregister != null;
    }

}