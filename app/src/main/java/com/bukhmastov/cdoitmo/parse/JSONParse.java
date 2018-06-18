package com.bukhmastov.cdoitmo.parse;

import org.json.JSONObject;

public abstract class JSONParse extends Parse<JSONObject> {
    public JSONParse(String data, Response<JSONObject> delegate) {
        super(data, delegate);
    }
}
