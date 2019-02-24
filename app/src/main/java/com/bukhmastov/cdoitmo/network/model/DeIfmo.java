package com.bukhmastov.cdoitmo.network.model;

import android.content.Context;
import android.text.TextUtils;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.provider.NetworkUserAgentProvider;
import com.bukhmastov.cdoitmo.util.Storage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class DeIfmo extends Client {

    private static final long JSESSIONID_EXPIRATION_TIME_MS = TimeUnit.MINUTES.toMillis(20);

    @Inject
    NetworkUserAgentProvider networkUserAgentProvider;

    public DeIfmo() {
        super();
        AppComponentProvider.getComponent().inject(this);
    }

    /**
     * Performs GET request
     * @param context context, cannot be null
     * @param url to be requested, cannot be null
     * @param query of request
     * @param handler of request, cannot be null
     * @see ResponseHandler
     */
    protected void doGet(@NonNull Context context, @NonNull String url,
                         @Nullable Map<String, String> query, @NonNull ResponseHandler handler) {
        try {
            doGet(url, getHeaders(context), query, handler);
        } catch (Exception exception) {
            handler.onFailure(STATUS_CODE_EMPTY, null, getFailedStatus(exception));
        }
    }

    /**
     * Performs POST request
     * @param context context, cannot be null
     * @param url to be requested, cannot be null
     * @param params of request
     * @param handler of request, cannot be null
     * @see ResponseHandler
     */
    protected void doPost(@NonNull Context context, @NonNull String url,
                          @Nullable Map<String, String> params, @NonNull ResponseHandler handler) {
        try {
            doPost(url, getHeaders(context), null, params, handler);
        } catch (Exception exception) {
            handler.onFailure(STATUS_CODE_EMPTY, null, getFailedStatus(exception));
        }
    }

    /**
     * Performs GET request and parse result as {@link JsonEntity}
     * @param context context, cannot be null
     * @param url to be requested, cannot be null
     * @param query of request
     * @param restHandler of request, cannot be null
     * @see RestResponseHandler
     */
    protected <T extends JsonEntity> void doGetJson(@NonNull Context context, @NonNull String url,
                       @Nullable Map<String, String> query, @NonNull RestResponseHandler<T> restHandler) {
        try {
            doGetJson(url, getHeaders(context), query, restHandler);
        } catch (Exception exception) {
            restHandler.onFailure(STATUS_CODE_EMPTY, null, getFailedStatus(exception));
        }
    }

    /**
     * Checks if jsessionid cookie is expired
     * @param context context
     * @return true if jsessionid is expired, false otherwise
     */
    public boolean isAuthExpiredByJsessionId(@NonNull Context context) {
        thread.assertNotUI();
        boolean isExpired = true;
        JSONArray storedCookies;
        try {
            storedCookies = new JSONArray(storage.get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#cookies", ""));
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
                            isExpired = Long.parseLong(attr.getString("value")) <= System.currentTimeMillis();
                            break;
                        }
                    }
                }
                if (!isExpired) {
                    break;
                }
            }
        } catch (Exception ignore) {
            // ignore
        }
        return isExpired;
    }

    protected okhttp3.Headers getHeaders(@NonNull final Context context) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", networkUserAgentProvider.get(context));
        JSONArray storedCookies;
        try {
            storedCookies = new JSONArray(storage.get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#cookies", ""));
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

    @Override
    protected JSONArray parseCookies(@Nullable okhttp3.Headers headers) {
        try {
            JSONArray parsed = super.parseCookies(headers);
            if (parsed == null) {
                return null;
            }
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
                        expires = String.valueOf(System.currentTimeMillis() + JSESSIONID_EXPIRATION_TIME_MS);
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
            log.exception(e);
            return new JSONArray();
        }
    }

    protected void storeCookies(@NonNull Context context, Headers headers, boolean refreshJsessionid) {
        storeCookies(context, headers.get(), refreshJsessionid);
    }

    protected void storeCookies(@NonNull Context context, okhttp3.Headers headers, boolean refreshJsessionid) {
        String cookiesStr = storage.get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#cookies", "");
        JSONArray cookies = mergeCookies(headers, cookiesStr, refreshJsessionid);
        storage.put(context, Storage.PERMANENT, Storage.USER, "user#deifmo#cookies", cookies.toString());
    }

    private JSONArray mergeCookies(@NonNull okhttp3.Headers headers, String stored, boolean refreshJsessionid) {
        JSONArray newCookies;
        JSONArray storedCookies;
        try {
            newCookies = parseCookies(headers);
        } catch (Exception e) {
            newCookies = new JSONArray();
        }
        if (newCookies == null) {
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
                                .put("value", String.valueOf(System.currentTimeMillis() + JSESSIONID_EXPIRATION_TIME_MS))
                        );
                        storedCookie.put("attrs", attrs);
                        storedCookies.put(i, storedCookie);
                    }
                }
            }
            return storedCookies;
        } catch (Exception e) {
            log.exception(e);
            return newCookies;
        }
    }

    @Override
    protected String getFailedMessage(@NonNull Context context, int code) {
        if (code == 591) {
            return context.getString(R.string.server_maintenance);
        }
        return null;
    }
}
