package com.bukhmastov.cdoitmo.network.model;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.exception.CorruptedException;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.joiner.RestStringResponseHandlerJoiner;
import com.bukhmastov.cdoitmo.network.provider.NetworkClientProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.Reader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

public abstract class Client {

    private static final String TAG = "Client";
    private static final String[] LOG_SECURED_HEADERS = new String[] {"route", "JSESSIONID", "PHPSESSID", "access_token", "refresh_token"};
    private static final String[] LOG_SECURED_REQUEST_BODY = new String[] {"passwd", "pass", "password"};

    /**
     * Empty http status code
     * Used when no info available about response's http status code
     */
    public static final int STATUS_CODE_EMPTY = -1;

    /**
     * Progress states of request
     * Value of each state should be less that 100
     */
    public static final int STATE_HANDLING = 0;
    public static final int STATE_AUTHORIZATION = 1;
    public static final int STATE_AUTHORIZED = 2;

    /**
     * Failure states of request
     * Value of each state should be less that 100
     */
    public static final int FAILED = 0;
    public static final int FAILED_DENIED = 1;
    public static final int FAILED_INTERRUPTED = 2;
    public static final int FAILED_CORRUPTED = 3;
    public static final int FAILED_ERROR_4XX = 4;
    public static final int FAILED_ERROR_5XX = 5;
    public static final int FAILED_OFFLINE = 6;
    public static final int FAILED_EMPTY_RESPONSE = 7;
    public static final int FAILED_EXPECTED_REDIRECTION = 8;
    public static final int FAILED_AUTH = 9;
    public static final int FAILED_AUTH_REQUIRED = 10;
    public static final int FAILED_AUTH_CREDENTIALS_REQUIRED = 11;
    public static final int FAILED_AUTH_CREDENTIALS_FAILED = 12;
    public static final int FAILED_AUTH_ISU_RESTORE_CREDENTIALS_FAILED = 13;

    /**
     * Protocol of request
     */
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({HTTP, HTTPS})
    public @interface Protocol {}
    public static final String HTTP = "http";
    public static final String HTTPS = "https";

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Storage storage;
    @Inject
    StoragePref storagePref;
    @Inject
    NetworkClientProvider networkClientProvider;

    public Client() {
        AppComponentProvider.getComponent().inject(this);
    }

    /**
     * Performs GET request
     * @param url to be requested, cannot be null
     * @param headers of request
     * @param query of request
     * @param handler of request, cannot be null
     * @see ResponseHandler
     */
    protected void doGet(@NonNull String url, @Nullable okhttp3.Headers headers,
                         @Nullable Map<String, String> query, @NonNull ResponseHandler handler) {
        try {
            HttpUrl httpUrl = HttpUrl.parse(url);
            if (httpUrl == null) {
                throw new NullPointerException("httpUrl is null");
            }
            if (query != null) {
                HttpUrl.Builder builder = httpUrl.newBuilder();
                for (Map.Entry<String, String> entry : query.entrySet()) {
                    builder.addQueryParameter(entry.getKey(), entry.getValue());
                }
                execute(builder.build(), headers, null, handler);
            } else {
                execute(httpUrl, headers, null, handler);
            }
        } catch (Exception exception) {
            handler.onFailure(STATUS_CODE_EMPTY, null, getFailedStatus(exception));
        }
    }

    /**
     * Performs POST request
     * @param url to be requested, cannot be null
     * @param headers of request
     * @param query of request
     * @param params of request
     * @param handler of request, cannot be null
     * @see ResponseHandler
     */
    protected void doPost(@NonNull String url, @Nullable okhttp3.Headers headers,
                          @Nullable Map<String, String> query, @Nullable Map<String, String> params,
                          @NonNull ResponseHandler handler) {
        try {
            HttpUrl httpUrl = HttpUrl.parse(url);
            if (httpUrl == null) {
                throw new NullPointerException("httpUrl is null");
            }
            HttpUrl.Builder builder = httpUrl.newBuilder();
            if (query != null) {
                for (Map.Entry<String, String> entry : query.entrySet()) {
                    builder.addQueryParameter(entry.getKey(), entry.getValue());
                }
            }
            if (params != null) {
                FormBody.Builder formBody = new FormBody.Builder();
                for (Map.Entry<String, String> param : params.entrySet()) {
                    formBody.add(param.getKey(), param.getValue());
                }
                execute(builder.build(), headers, formBody.build(), handler);
            } else {
                execute(builder.build(), headers, null, handler);
            }
        } catch (Exception exception) {
            handler.onFailure(STATUS_CODE_EMPTY, null, getFailedStatus(exception));
        }
    }

    /**
     * Performs GET request and parse result as {@link JsonEntity}
     * @param url to be requested, cannot be null
     * @param headers of request
     * @param query of request
     * @param restHandler of request, cannot be null
     * @see RestResponseHandler
     */
    protected <T extends JsonEntity> void doGetJson(@NonNull String url, @Nullable okhttp3.Headers headers,
                            @Nullable Map<String, String> query, @NonNull RestResponseHandler<T> restHandler) {
        doGet(url, headers, query, new RestStringResponseHandlerJoiner(restHandler) {
            @Override
            public void onSuccess(int code, Headers headers, String response) throws Exception {
                try {
                    if (StringUtils.isBlank(response)) {
                        restHandler.onFailure(code, headers, FAILED_EMPTY_RESPONSE);
                        return;
                    }
                    Object object;
                    try {
                        object = new JSONTokener(response).nextValue();
                    } catch (JSONException e) {
                        try {
                            // Sometimes de.ifmo server provides corrupted json response
                            // Let's try to fix it
                            object = new JSONTokener(tryFixInvalidJsonResponse(response)).nextValue();
                        } catch (JSONException e1) {
                            throw new CorruptedException(e1);
                        }
                    }
                    if (object == null || object == JSONObject.NULL) {
                        throw new CorruptedException();
                    }
                    if (object instanceof JSONObject) {
                        JSONObject obj = (JSONObject) object;
                        T entity = restHandler.newInstance().fromJson(obj);
                        restHandler.onSuccess(code, headers, entity);
                        return;
                    }
                    if (object instanceof JSONArray) {
                        JSONArray arr = (JSONArray) object;
                        JSONObject obj = restHandler.convertArray(arr);
                        if (obj == null) {
                            throw new CorruptedException();
                        }
                        T entity = restHandler.newInstance().fromJson(obj);
                        restHandler.onSuccess(code, headers, entity);
                        return;
                    }
                    throw new CorruptedException();
                } catch (Exception exception) {
                    restHandler.onFailure(code, headers, getFailedStatus(exception));
                }
            }
        });
    }

    /**
     * Core network method that performs all network requests
     * @param url to be requested, cannot be null
     * @param headers of request
     * @param requestBody of request
     * @param handler of request, cannot be null
     * @see HttpUrl
     * @see okhttp3.Headers
     * @see RequestBody
     * @see ResponseHandler
     */
    private void execute(@NonNull HttpUrl url, @Nullable okhttp3.Headers headers,
                         @Nullable RequestBody requestBody, @NonNull ResponseHandler handler) {
        try {
            if (!thread.assertNotUI()) {
                throw new IllegalStateException("Network request was not executed. Currently on main thread.");
            }
            log.v(TAG,
                    "execute | load | ",
                    "url=", getUrl(url), " | ",
                    "headers=", getLogHeaders(headers), " | ",
                    "requestBody=", getLogRequestBody(requestBody)
            );
            // build request
            okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
            builder.url(url);
            if (headers != null) {
                builder.headers(headers);
            }
            if (requestBody != null) {
                if (!(requestBody instanceof FormBody) || ((FormBody) requestBody).size() > 0) {
                    MediaType contentType = requestBody.contentType();
                    builder.addHeader("Content-Type", contentType == null ? "application/x-www-form-urlencoded" : contentType.toString());
                    builder.addHeader("Content-Length", String.valueOf(requestBody.contentLength()));
                }
                builder.post(requestBody);
            }
            okhttp3.Request request = builder.build();
            // perform request
            Call call = networkClientProvider.get().newCall(request);
            handler.onNewRequest(new Request(call));
            Response response = call.execute();
            // fetch response as string
            String responseString = "";
            if (response.body() != null) {
                int bufferSize = 1024;
                char[] buffer = new char[bufferSize];
                StringBuilder out = new StringBuilder();
                try (Reader reader = response.body().charStream()) {
                    int length;
                    while ((length = reader.read(buffer, 0, buffer.length)) != -1) {
                        out.append(buffer, 0, length);
                    }
                }
                responseString = out.toString();
            }
            call.cancel();
            // it's all over..
            int code = response.code();
            okhttp3.Headers responseHeaders = response.headers();
            log.v(TAG,
                    "execute | done | ",
                    "url=", getUrl(url), " | ",
                    "code=", code, " | ",
                    "headers=", getLogHeaders(responseHeaders), " | ",
                    "response=", (responseString.isEmpty() ? "<empty>" : "<string>")
            );
            response.close();
            if (code >= 500) {
                handler.onFailure(code, new Headers(responseHeaders), FAILED_ERROR_5XX);
                return;
            }
            if (code >= 400) {
                handler.onFailure(code, new Headers(responseHeaders), FAILED_ERROR_4XX);
                return;
            }
            handler.onSuccess(code, new Headers(responseHeaders), responseString);
        } catch (Exception exception) {
            handler.onFailure(STATUS_CODE_EMPTY, null, getFailedStatus(exception));
        }
    }

    public boolean isAuthorized(@NonNull Context context) {
        return true;
    }

    public String getProgressMessage(@NonNull Context context, int state) {
        switch (state) {
            case STATE_HANDLING: return context.getString(R.string.network_state_loading);
            case STATE_AUTHORIZATION: return context.getString(R.string.network_state_authorization);
            case STATE_AUTHORIZED: return context.getString(R.string.network_state_authorized);
        }
        return context.getString(R.string.network_state_loading);
    }

    public String getFailedMessage(@NonNull Context context, int code, int failed) {
        if (code != STATUS_CODE_EMPTY) {
            String message = getFailedMessage(context, code);
            if (StringUtils.isNotBlank(message)) {
                return message;
            }
        }
        switch (failed) {
            case FAILED: return context.getString(R.string.network_failed);
            case FAILED_DENIED: return context.getString(R.string.network_failed_denied);
            case FAILED_INTERRUPTED: return context.getString(R.string.network_failed_interrupted);
            case FAILED_CORRUPTED: return context.getString(R.string.network_failed_corrupted);
            case FAILED_ERROR_4XX: return String.format(context.getString(R.string.network_failed_error_4xx), code);
            case FAILED_ERROR_5XX: return String.format(context.getString(R.string.network_failed_error_5xx), code);
            case FAILED_OFFLINE: return context.getString(R.string.network_failed_offline);
            case FAILED_EMPTY_RESPONSE: return context.getString(R.string.network_failed_empty_response);
            case FAILED_EXPECTED_REDIRECTION: return context.getString(R.string.network_failed_expected_redirection);
            case FAILED_AUTH: return context.getString(R.string.network_failed_auth);
            case FAILED_AUTH_REQUIRED: return context.getString(R.string.network_failed_auth_required);
            case FAILED_AUTH_CREDENTIALS_REQUIRED: return context.getString(R.string.network_failed_credentials_required);
            case FAILED_AUTH_CREDENTIALS_FAILED: return context.getString(R.string.network_failed_credentials_failed);
            case FAILED_AUTH_ISU_RESTORE_CREDENTIALS_FAILED: return context.getString(R.string.network_failed_restore_isu_credentials_failed);
        }
        return context.getString(R.string.network_failed);
    }

    public boolean isFailedAuth(int failed) {
        return failed == FAILED_AUTH ||
                failed == FAILED_AUTH_REQUIRED ||
                failed == FAILED_AUTH_CREDENTIALS_REQUIRED ||
                failed == FAILED_AUTH_CREDENTIALS_FAILED ||
                failed == FAILED_AUTH_ISU_RESTORE_CREDENTIALS_FAILED;
    }

    public boolean isFailedAuthCredentials(int failed) {
        return failed == FAILED_AUTH_CREDENTIALS_REQUIRED ||
                failed == FAILED_AUTH_CREDENTIALS_FAILED ||
                failed == FAILED_AUTH_ISU_RESTORE_CREDENTIALS_FAILED;
    }

    protected String getFailedMessage(@NonNull Context context, int code) {
        return null;
    }

    protected int getFailedStatus(Exception exception) {
        return getFailedStatus(exception, FAILED);
    }

    protected int getFailedStatus(Exception exception, int def) {
        if (exception instanceof IllegalStateException) {
            return FAILED_DENIED;
        }
        if (exception instanceof IOException) {
            return FAILED_INTERRUPTED;
        }
        if (exception instanceof CorruptedException) {
            return FAILED_CORRUPTED;
        }
        return def;
    }

    private String tryFixInvalidJsonResponse(@Nullable String response) {
        if (response == null) {
            return null;
        }
        Matcher m = Pattern.compile("(\\\\u)([0-9a-f]{3})[^0-9a-f]").matcher(response);
        if (m.find()) {
            log.v(TAG, "Found and fixed invalid json response");
            response = m.replaceAll(m.group(1) + "0" + m.group(2));
        }
        return response;
    }

    protected String getProtocol(@NonNull @Protocol String protocol) {
        switch (protocol) {
            case HTTP: return "http://";
            case HTTPS: return "https://";
            default:
                @Protocol String p = HTTP;
                log.wtf(TAG, "getProtocol | undefined protocol, going to use " + p);
                return getProtocol(p);
        }
    }

    protected JSONArray parseCookies(@Nullable okhttp3.Headers headers) {
        try {
            if (headers == null) {
                return null;
            }
            Map<String, List<String>> headersMap = headers.toMultimap();
            if (!headersMap.containsKey("set-cookie")) {
                return new JSONArray();
            }
            JSONArray parsed = new JSONArray();
            List<String> set_cookie = headersMap.get("set-cookie");
            for (String cookieAndAttributes : CollectionUtils.emptyIfNull(set_cookie)) {
                String[] attributes = cookieAndAttributes.split(";");
                if (attributes.length == 0) continue;
                String[] cookie = attributes[0].split("=");
                if (cookie.length != 2) continue;
                String cookieName = cookie[0].trim();
                String cookieValue = cookie[1].trim();
                //log.v(TAG, "parseCookies | cookie: " + cookieName + "=" + cookieValue);
                JSONArray attrs = new JSONArray();
                for (int i = 1; i < attributes.length; i++) {
                    String[] attribute = attributes[i].split("=");
                    if (attribute.length != 2) continue;
                    String attrName = attribute[0].trim().toLowerCase();
                    String attrValue = attribute[1].trim();
                    //log.v(TAG, "parseCookies |    attr: " + attrName + "=" + attrValue);
                    attrs.put(new JSONObject()
                            .put("name", attrName)
                            .put("value", attrValue)
                    );
                }
                parsed.put(new JSONObject()
                        .put("name", cookieName)
                        .put("value", cookieValue)
                        .put("attrs", attrs)
                );
            }
            return parsed;
        } catch (Exception e) {
            log.exception(e);
            return new JSONArray();
        }
    }

    private String getUrl(@Nullable HttpUrl httpUrl) {
        try {
            if (httpUrl == null) {
                return "<null>";
            }
            String url = httpUrl.toString();
            if (url.contains("isu.ifmo.ru") && url.contains("api/core")) {
                url = url.replaceAll("[^/]{64}", "<apikey>");
                url = url.replaceAll("AT-[^/]*", "<auth-token>");
            }
            if (url.contains("services.ifmo.ru")) {
                Matcher m = Pattern.compile("^(.*oauth2\\.0/)(.*)$", Pattern.CASE_INSENSITIVE).matcher(url);
                if (m.find()) {
                    url = m.replaceAll("$1<hidden>");
                }
            }
            return url;
        } catch (Exception e) {
            return "<error>";
        }
    }

    private String getLogRequestBody(@Nullable RequestBody requestBody) {
        try {
            if (requestBody == null) {
                return "<null>";
            }
            String log = "";
            try (Buffer buffer = new Buffer()) {
                requestBody.writeTo(buffer);
                log = buffer.readUtf8().trim();
            }
            for (String secured : LOG_SECURED_REQUEST_BODY) {
                Matcher m = Pattern.compile("(" + secured + "=)([^&]*)", Pattern.CASE_INSENSITIVE).matcher(log);
                if (m.find()) {
                    log = m.replaceAll("$1<hidden>");
                }
            }
            return log;
        } catch (Exception e) {
            return "<error>";
        }
    }

    private String getLogHeaders(@Nullable okhttp3.Headers headers) {
        try {
            if (headers == null) {
                return "<null>";
            }
            String log = headers.toString().replaceAll("\n", " ").trim();
            for (String secured : LOG_SECURED_HEADERS) {
                Matcher m = Pattern.compile("(" + secured + "=)([^\\s;]*)", Pattern.CASE_INSENSITIVE).matcher(log);
                if (m.find()) {
                    log = m.replaceAll("$1<hidden>");
                }
            }
            return log;
        } catch (Exception e) {
            return "<error>";
        }
    }

    public static boolean isOffline(@NonNull Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager == null) {
                return false;
            }
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return (networkInfo == null || !networkInfo.isConnected());
        } catch (Exception e) {
            return false;
        }
    }

    public class Request {
        private Call call;
        public Request(Call call) {
            this.call = call;
        }
        public boolean cancel() {
            if (call != null && !call.isCanceled()) {
                log.v(TAG, "request cancelled | url=", getUrl(call.request().url()));
                call.cancel();
                return true;
            } else {
                return false;
            }
        }
    }

    public class Headers {
        private okhttp3.Headers headers;
        public Headers(okhttp3.Headers headers) {
            if (headers == null) {
                this.headers = okhttp3.Headers.of();
            } else {
                this.headers = headers;
            }
        }
        public okhttp3.Headers get() {
            return headers;
        }
        public String getValue(String key) {
            return headers == null ? null : headers.get(key);
        }
    }
}
