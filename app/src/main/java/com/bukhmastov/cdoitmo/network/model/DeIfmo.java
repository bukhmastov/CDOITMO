package com.bukhmastov.cdoitmo.network.model;

import android.content.Context;
import android.text.TextUtils;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.network.handlers.RawHandler;
import com.bukhmastov.cdoitmo.network.handlers.RawJsonHandler;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHasFailed;
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

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public abstract class DeIfmo extends Client {

    private static final long jsessionid_expiration_time_ms = 1200000L; // 20min // 20 * 60 * 1000

    public static final int STATE_CHECKING = 10;
    public static final int STATE_AUTHORIZATION = 11;
    public static final int STATE_AUTHORIZED = 12;
    public static final int FAILED_AUTH_TRY_AGAIN = 10;
    public static final int FAILED_AUTH_CREDENTIALS_REQUIRED = 11;
    public static final int FAILED_AUTH_CREDENTIALS_FAILED = 12;
    public static final int FAILED_UNAUTHORIZED_MODE = 13;

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
     * @param rawHandler of request, cannot be null
     * @see RawHandler
     */
    protected void doGet(@NonNull Context context, @NonNull String url,
                         @Nullable Map<String, String> query, @NonNull RawHandler rawHandler) {
        try {
            doGet(url, getHeaders(context), query, rawHandler);
        } catch (Throwable throwable) {
            rawHandler.onError(STATUS_CODE_EMPTY, null, throwable);
        }
    }

    /**
     * Performs POST request
     * @param context context, cannot be null
     * @param url to be requested, cannot be null
     * @param params of request
     * @param rawHandler of request, cannot be null
     * @see RawHandler
     */
    protected void doPost(@NonNull Context context, @NonNull String url,
                          @Nullable Map<String, String> params, @NonNull RawHandler rawHandler) {
        try {
            doPost(url, getHeaders(context), null, params, rawHandler);
        } catch (Throwable throwable) {
            rawHandler.onError(STATUS_CODE_EMPTY, null, throwable);
        }
    }

    /**
     * Performs GET request and parse result as json
     * @param context context, cannot be null
     * @param url to be requested, cannot be null
     * @param query of request
     * @param rawJsonHandler of request, cannot be null
     * @see RawJsonHandler
     */
    protected void doGetJson(@NonNull Context context, @NonNull String url,
                             @Nullable Map<String, String> query, @NonNull RawJsonHandler rawJsonHandler) {
        try {
            doGetJson(url, getHeaders(context), query, rawJsonHandler);
        } catch (Throwable throwable) {
            rawJsonHandler.onError(STATUS_CODE_EMPTY, null, throwable);
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

    @Override
    @Nullable
    protected JSONArray parseCookies(@Nullable okhttp3.Headers headers) {
        try {
            final JSONArray parsed = super.parseCookies(headers);
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
                        expires = String.valueOf(System.currentTimeMillis() + jsessionid_expiration_time_ms);
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

    protected void storeCookies(@NonNull Context context, okhttp3.Headers headers) {
        storeCookies(context, headers, true);
    }

    protected void storeCookies(@NonNull Context context, okhttp3.Headers headers, boolean refreshJsessionid) {
        String cookiesStr = storage.get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#cookies", "");
        JSONArray cookies = mergeCookies(headers, cookiesStr, refreshJsessionid);
        storage.put(context, Storage.PERMANENT, Storage.USER, "user#deifmo#cookies", cookies.toString());
    }

    @NonNull
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
                                .put("value", String.valueOf(System.currentTimeMillis() + jsessionid_expiration_time_ms))
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

    @NonNull
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

    @NonNull
    public static String getFailureMessage(@NonNull Context context, int statusCode) {
        if (statusCode == 591) {
            return context.getString(R.string.server_maintenance);
        } else {
            return Client.getFailureMessage(context, statusCode);
        }
    }

    public static @StringRes int getFailureMessage(int statusCode) {
        if (statusCode == 591) {
            return R.string.server_maintenance;
        } else {
            return Client.getFailureMessage();
        }
    }
}
