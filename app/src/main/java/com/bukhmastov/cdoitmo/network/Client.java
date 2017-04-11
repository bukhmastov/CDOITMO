package com.bukhmastov.cdoitmo.network;

import android.content.Context;
import android.util.Log;

import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.AsyncHttpClient;

abstract class Client {

    static AsyncHttpClient httpclient = new AsyncHttpClient();
    private static boolean initialized = false;
    private static final long jsessionid_ts_limit = 1200000L; // 20min // 20 * 60 * 1000

    static void init(){
        if (!initialized) {
            initialized = true;
            httpclient.setLoggingLevel(Log.WARN);
        }
    }
    static void renewCookie(Context context){
        httpclient.removeHeader("User-Agent");
        httpclient.removeHeader("Cookie");
        httpclient.addHeader("User-Agent", Static.getUserAgent(context));
        httpclient.addHeader("Cookie", "JSESSIONID=" + Storage.file.perm.get(context, "user#jsessionid") + "; Path=/;");
    }
    static boolean checkJsessionId(Context context){
        return Long.parseLong(Storage.file.perm.get(context, "user#jsessionid_ts", "0")) + jsessionid_ts_limit < System.currentTimeMillis();
    }

}
