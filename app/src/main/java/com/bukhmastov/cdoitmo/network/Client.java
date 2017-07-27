package com.bukhmastov.cdoitmo.network;

import android.content.Context;

import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.SyncHttpClient;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;

public abstract class Client {

    private static final String TAG = "Client";
    static AsyncHttpClient httpclient = new AsyncHttpClient();
    static SyncHttpClient httpclientsync = new SyncHttpClient();
    private static boolean initialized = false;
    private static final long jsessionid_ts_limit = 1200000L; // 20min // 20 * 60 * 1000
    public static final int STATUS_CODE_EMPTY = -1;
    public enum Protocol {HTTP, HTTPS}

    static void init() {
        if (!initialized) {
            initialized = true;
            httpclient.setLoggingLevel(android.util.Log.WARN);
            httpclientsync.setLoggingLevel(android.util.Log.WARN);
        }
    }
    static void renewCookie(Context context) {
        httpclient.removeHeader("User-Agent");
        httpclient.removeHeader("Cookie");
        httpclient.addHeader("User-Agent", Static.getUserAgent(context));
        httpclient.addHeader("Cookie", "JSESSIONID=" + Storage.file.perm.get(context, "user#jsessionid") + "; Path=/;");
        httpclientsync.removeHeader("User-Agent");
        httpclientsync.removeHeader("Cookie");
        httpclientsync.addHeader("User-Agent", Static.getUserAgent(context));
        httpclientsync.addHeader("Cookie", "JSESSIONID=" + Storage.file.perm.get(context, "user#jsessionid") + "; Path=/;");
    }
    static boolean checkJsessionId(Context context) {
        return Long.parseLong(Storage.file.perm.get(context, "user#jsessionid_ts", "0")) + jsessionid_ts_limit < System.currentTimeMillis();
    }
    static String convert2UTF8(Header[] headers, byte[] content) {
        try {
            if (content == null) throw new NullPointerException("content cannot be null");
            String charset = "windows-1251";
            boolean foundAtHeaders = false;
            for (Header header : headers) {
                if (Objects.equals(header.getName().toLowerCase(), "content-type")) {
                    String[] entities = header.getValue().split(";");
                    for (String entity : entities) {
                        String[] pair = entity.trim().split("=");
                        if (pair.length >= 2) {
                            if (Objects.equals(pair[0].trim().toLowerCase(), "charset")) {
                                charset = pair[1].trim().toUpperCase();
                                foundAtHeaders = true;
                            }
                        }
                        if (foundAtHeaders) break;
                    }
                    if (foundAtHeaders) break;
                }
            }
            if (!foundAtHeaders) {
                Matcher m = Pattern.compile("<meta.*charset=\"?(.*)\".*>").matcher(new String(content, "UTF-8"));
                if (m.find()) {
                    charset = m.group(1).trim().toUpperCase();
                }
            }
            if (Objects.equals(charset, "UTF-8")) {
                return new String(content, charset);
            } else {
                return new String((new String(content, charset)).getBytes("UTF-8"));
            }
        } catch (Exception e) {
            Static.error(e);
            return null;
        }
    }
    static String getProtocol(Protocol protocol) {
        switch (protocol) {
            case HTTP: return "http://";
            case HTTPS: return "https://";
            default:
                Log.wtf(TAG, "getProtocol | undefined protocol, going to use HTTP");
                return "http://";
        }
    }
    static AsyncHttpClient getHttpClient() {
        return Static.T.isLooperThread() ? httpclient : httpclientsync;
    }
    static RequestHandle checkHandle(RequestHandle requestHandle) {
        return requestHandle.isFinished() || requestHandle.isCancelled() ? null : requestHandle;
    }
}
