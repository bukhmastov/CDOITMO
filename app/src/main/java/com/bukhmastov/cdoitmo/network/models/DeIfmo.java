package com.bukhmastov.cdoitmo.network.models;

import android.content.Context;
import android.text.TextUtils;

import com.bukhmastov.cdoitmo.network.interfaces.RawHandler;
import com.bukhmastov.cdoitmo.network.interfaces.RawJsonHandler;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public abstract class DeIfmo extends Client {

    private static final long jsessionid_ts_limit = 1200000L; // 20min // 20 * 60 * 1000

    public static final int STATE_CHECKING = 10;
    public static final int STATE_AUTHORIZATION = 11;
    public static final int STATE_AUTHORIZED = 12;
    public static final int FAILED_AUTH_TRY_AGAIN = 10;
    public static final int FAILED_AUTH_CREDENTIALS_REQUIRED = 11;
    public static final int FAILED_AUTH_CREDENTIALS_FAILED = 12;

    protected static boolean checkJsessionId(final Context context) {
        boolean legit = false;
        JSONArray storedCookies;
        try {
            storedCookies = new JSONArray(Storage.file.perm.get(context, "user#deifmo#cookies", ""));
        } catch (Exception ignore) {
            storedCookies = new JSONArray();
        }
        try {
            for (int i = 0; i < storedCookies.length(); i++) {
                JSONObject storedCookie = storedCookies.getJSONObject(i);
                if ("jsessionid".equals(storedCookie.getString("name").toLowerCase())) {
                    JSONArray attrs = storedCookie.getJSONArray("attrs");
                    for (int j = 0; j < attrs.length(); j++) {
                        JSONObject attr = attrs.getJSONObject(j);
                        if ("expires".equals(attr.getString("name"))) {
                            legit = Long.parseLong(attr.getString("value")) > System.currentTimeMillis();
                            break;
                        }
                    }
                }
                if (legit) {
                    break;
                }
            }
        } catch (Exception ignore) {
            // ignore
        }
        return legit;
    }
    protected static void g(final Context context, final String url, final Map<String, String> query, final RawHandler rawHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                try {
                    _g(url, getHeaders(context), query, rawHandler);
                } catch (Throwable throwable) {
                    rawHandler.onError(throwable);
                }
            }
        });
    }
    protected static void p(final Context context, final String url, final Map<String, String> params, final RawHandler rawHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                try {
                    _p(url, getHeaders(context), params, rawHandler);
                } catch (Throwable throwable) {
                    rawHandler.onError(throwable);
                }
            }
        });
    }
    protected static void gJson(final Context context, final String url, final Map<String, String> query, final RawJsonHandler rawJsonHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                try {
                    _gJson(url, getHeaders(context), query, rawJsonHandler);
                } catch (Throwable throwable) {
                    rawJsonHandler.onError(throwable);
                }
            }
        });
    }

    protected static void storeCookies(final Context context, final okhttp3.Headers headers) {
        storeCookies(context, headers, true);
    }
    protected static void storeCookies(final Context context, final okhttp3.Headers headers, final boolean refreshJsessionid) {
        JSONArray cookies = mergeCookies(headers, Storage.file.perm.get(context, "user#deifmo#cookies", ""), refreshJsessionid);
        Storage.file.perm.put(context, "user#deifmo#cookies", cookies.toString());
    }
    protected static JSONArray parseCookies(final okhttp3.Headers headers) {
        try {
            final JSONArray parsed = Client.parseCookies(headers);
            for (int i = 0; i < parsed.length(); i++) {
                JSONObject cookie = parsed.getJSONObject(i);
                String cookieName = cookie.getString("name");
                JSONArray cookieAttrs = cookie.getJSONArray("attrs");
                if ("jsessionid".equals(cookieName.toLowerCase())) {
                    String expires = "";
                    for (int j = 0; j < cookieAttrs.length(); j++) {
                        JSONObject attr = cookieAttrs.getJSONObject(j);
                        String attrName = attr.getString("name");
                        String attrValue = attr.getString("value");
                        if ("expires".equals(attrName)) {
                            expires = attrValue;
                        }
                    }
                    if (!expires.isEmpty()) {
                        try {
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.getDefault());
                            Date date = simpleDateFormat.parse(expires);
                            expires = String.valueOf(date.getTime());
                        } catch (Exception e) {
                            expires = "";
                        }
                    }
                    if (expires.isEmpty()) {
                        expires = String.valueOf(System.currentTimeMillis() + jsessionid_ts_limit);
                    }
                    for (int j = 0; j < cookieAttrs.length(); j++) {
                        JSONObject attr = cookieAttrs.getJSONObject(j);
                        if ("expires".equals(attr.getString("name"))) {
                            cookieAttrs.remove(j);
                            break;
                        }
                    }
                    cookieAttrs.put(new JSONObject()
                            .put("name", "expires")
                            .put("value", expires)
                    );
                    cookie.put("attrs", cookieAttrs);
                }
            }
            return parsed;
        } catch (Exception e) {
            Static.error(e);
            return new JSONArray();
        }
    }
    protected static JSONArray mergeCookies(final okhttp3.Headers headers, final String stored, final boolean refreshJsessionid) {
        JSONArray newCookies;
        JSONArray storedCookies;
        try {
            newCookies = parseCookies(headers);
        } catch (Exception e) {
            newCookies = new JSONArray();
        }
        try {
            storedCookies = new JSONArray(stored);
        } catch (Exception e) {
            storedCookies = new JSONArray();
        }
        try {
            boolean jsessionidNeedRefresh = true;
            for (int i = 0; i < newCookies.length(); i++) {
                JSONObject newCookie = newCookies.getJSONObject(i);
                String name = newCookie.getString("name");
                if ("jsessionid".equals(name.toLowerCase())) {
                    jsessionidNeedRefresh = false;
                }
                for (int j = 0; j < storedCookies.length(); j++) {
                    JSONObject storedCookie = storedCookies.getJSONObject(j);
                    if (name.equals(storedCookie.getString("name"))) {
                        storedCookies.remove(j);
                        break;
                    }
                }
                storedCookies.put(newCookie);
            }
            if (refreshJsessionid && jsessionidNeedRefresh) {
                for (int i = 0; i < storedCookies.length(); i++) {
                    JSONObject storedCookie = storedCookies.getJSONObject(i);
                    String name = storedCookie.getString("name");
                    if ("jsessionid".equals(name.toLowerCase())) {
                        JSONArray attrs = storedCookie.getJSONArray("attrs");
                        for (int j = 0; j < attrs.length(); j++) {
                            JSONObject attr = attrs.getJSONObject(j);
                            if ("expires".equals(attr.getString("name"))) {
                                attrs.remove(j);
                                break;
                            }
                        }
                        attrs.put(new JSONObject()
                                .put("name", "expires")
                                .put("value", String.valueOf(System.currentTimeMillis() + jsessionid_ts_limit))
                        );
                        storedCookie.put("attrs", attrs);
                        storedCookies.put(i, storedCookie);
                    }
                }
            }
            return storedCookies;
        } catch (Exception e) {
            Static.error(e);
            return newCookies;
        }
    }

    protected static okhttp3.Headers getHeaders(final Context context) throws Throwable {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Static.getUserAgent(context));
        JSONArray storedCookies;
        try {
            storedCookies = new JSONArray(Storage.file.perm.get(context, "user#deifmo#cookies", ""));
        } catch (Exception ignore) {
            storedCookies = new JSONArray();
        }
        ArrayList<String> cookies = new ArrayList<>();
        for (int i = 0; i < storedCookies.length(); i++) {
            try {
                JSONObject storedCookie = storedCookies.getJSONObject(i);
                cookies.add(storedCookie.getString("name") + "=" + storedCookie.getString("value"));
            } catch (Exception ignore) {
                // ignore
            }
        }
        if (cookies.size() > 0) {
            headers.put("Cookie", TextUtils.join("; ", cookies).trim());
        }
        return okhttp3.Headers.of(headers);
    }
}
