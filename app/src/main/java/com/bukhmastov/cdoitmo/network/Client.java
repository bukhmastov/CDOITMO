package com.bukhmastov.cdoitmo.network;

import android.content.Context;
import android.util.Log;

import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.AsyncHttpClient;

abstract class Client {

    private static final String USER_AGENT = "Android Application";
    static AsyncHttpClient httpclient = new AsyncHttpClient();
    private static boolean initialized = false;

    static void init(){
        if (!initialized) {
            initialized = true;
            httpclient.setLoggingLevel(Log.WARN);
        }
    }
    static void renewCookie(Context context){
        httpclient.removeHeader("User-Agent");
        httpclient.removeHeader("Cookie");
        httpclient.addHeader("User-Agent", USER_AGENT);
        httpclient.addHeader("Cookie", "JSESSIONID=" + Storage.file.perm.get(context, "user#jsessionid") + "; Path=/;");
    }

}
