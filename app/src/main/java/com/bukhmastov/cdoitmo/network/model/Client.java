package com.bukhmastov.cdoitmo.network.model;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.annotation.StringRes;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.network.handlers.RawHandler;
import com.bukhmastov.cdoitmo.network.handlers.RawJsonHandler;
import com.bukhmastov.cdoitmo.network.provider.NetworkClientProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.Reader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

public abstract class Client {

    private static final String TAG = "Client";
    private static final String[] LOG_SECURED_HEADERS = new String[] {"route", "JSESSIONID", "PHPSESSID"};
    private static final String[] LOG_SECURED_REQUEST_BODY = new String[] {"passwd", "pass", "password"};

    public static final int STATUS_CODE_EMPTY = -1;
    public static final int STATE_HANDLING = 0;
    public static final int FAILED_OFFLINE = 0;
    public static final int FAILED_TRY_AGAIN = 1;
    public static final int FAILED_SERVER_ERROR = 2;
    public static final int FAILED_INTERRUPTED = 3;
    public static final int FAILED_CORRUPTED_JSON = 4;

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
     * @param rawHandler of request, cannot be null
     * @see RawHandler
     */
    protected void doGet(@NonNull String url, @Nullable okhttp3.Headers headers, @Nullable Map<String, String> query, @NonNull RawHandler rawHandler) {
        thread.run(thread.BACKGROUND, () -> {
            HttpUrl httpUrl = HttpUrl.parse(url);
            if (httpUrl == null) {
                throw new NullPointerException("httpUrl is null");
            }
            if (query != null) {
                HttpUrl.Builder builder = httpUrl.newBuilder();
                for (Map.Entry<String, String> entry : query.entrySet()) {
                    builder.addQueryParameter(entry.getKey(), entry.getValue());
                }
                execute(builder.build(), headers, null, rawHandler);
            }
            execute(httpUrl, headers, null, rawHandler);
        }, throwable -> {
            rawHandler.onError(STATUS_CODE_EMPTY, null, throwable);
        });
    }

    /**
     * Performs POST request
     * @param url to be requested, cannot be null
     * @param headers of request
     * @param query of request
     * @param params of request
     * @param rawHandler of request, cannot be null
     * @see RawHandler
     */
    protected void doPost(@NonNull String url, @Nullable okhttp3.Headers headers, @Nullable Map<String, String> query, @Nullable Map<String, String> params, @NonNull RawHandler rawHandler) {
        thread.run(thread.BACKGROUND, () -> {
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
                execute(builder.build(), headers, formBody.build(), rawHandler);
            } else {
                execute(builder.build(), headers, null, rawHandler);
            }
        }, throwable -> {
            rawHandler.onError(STATUS_CODE_EMPTY, null, throwable);
        });
    }

    /**
     * Performs GET request and parse result as json
     * @param url to be requested, cannot be null
     * @param headers of request
     * @param query of request
     * @param rawJsonHandler of request, cannot be null
     * @see RawJsonHandler
     */
    protected void doGetJson(@NonNull String url, @Nullable okhttp3.Headers headers, @Nullable Map<String, String> query, @NonNull RawJsonHandler rawJsonHandler) {
        thread.run(thread.BACKGROUND, () -> {
            HttpUrl httpUrl = HttpUrl.parse(url);
            if (httpUrl == null) {
                throw new NullPointerException("httpUrl is null");
            }
            RawHandler rawHandler = new RawHandler() {
                @Override
                public void onDone(final int code, final okhttp3.Headers responseHeaders, final String response) {
                    thread.run(thread.BACKGROUND, () -> {
                        if (code >= 400) {
                            rawJsonHandler.onDone(code, headers, response, null, null);
                            return;
                        }
                        if (StringUtils.isBlank(response)) {
                            rawJsonHandler.onDone(code, headers, response, new JSONObject(), new JSONArray());
                        } else {
                            try {
                                Object object = new JSONTokener(response).nextValue();
                                if (object instanceof JSONObject) {
                                    rawJsonHandler.onDone(code, headers, response, (JSONObject) object, null);
                                } else if (object instanceof JSONArray) {
                                    rawJsonHandler.onDone(code, headers, response, null, (JSONArray) object);
                                } else {
                                    throw new Exception("Failed to use JSONTokener");
                                }
                            } catch (Exception e) {
                                if (response.startsWith("{") && response.endsWith("}")) {
                                    try {
                                        JSONObject jsonObject;
                                        try {
                                            jsonObject = new JSONObject(response);
                                        } catch (Throwable throwable) {
                                            jsonObject = new JSONObject(tryFixInvalidJsonResponse(response));
                                        }
                                        rawJsonHandler.onDone(code, headers, response, jsonObject, null);
                                    } catch (Throwable throwable) {
                                        rawJsonHandler.onError(code, headers, new ParseException("Failed to parse JSONObject", 0));
                                    }
                                } else if (response.startsWith("[") && response.endsWith("]")) {
                                    try {
                                        JSONArray jsonArray;
                                        try {
                                            jsonArray = new JSONArray(response);
                                        } catch (Throwable throwable) {
                                            jsonArray = new JSONArray(tryFixInvalidJsonResponse(response));
                                        }
                                        rawJsonHandler.onDone(code, headers, response, null, jsonArray);
                                    } catch (Throwable throwable) {
                                        rawJsonHandler.onError(code, headers, new ParseException("Failed to parse JSONArray", 0));
                                    }
                                } else {
                                    rawJsonHandler.onError(code, headers, new Exception("Response is not recognized as JSONObject or JSONArray"));
                                }
                            }
                        }
                    }, throwable -> {
                        rawJsonHandler.onError(code, headers, throwable);
                    });
                }
                @Override
                public void onNewRequest(Request request) {
                    rawJsonHandler.onNewRequest(request);
                }
                @Override
                public void onError(int code, okhttp3.Headers headers1, Throwable throwable) {
                    rawJsonHandler.onError(code, headers1, throwable);
                }
            };
            if (query != null) {
                HttpUrl.Builder builder = httpUrl.newBuilder();
                for (Map.Entry<String, String> entry : query.entrySet()) {
                    builder.addQueryParameter(entry.getKey(), entry.getValue());
                }
                execute(builder.build(), headers, null, rawHandler);
            }
            execute(httpUrl, headers, null, rawHandler);
        }, throwable -> {
            rawJsonHandler.onError(STATUS_CODE_EMPTY, null, throwable);
        });
    }

    /**
     * Core network method that performs all network requests
     * @param url to be requested, cannot be null
     * @param headers of request
     * @param requestBody of request
     * @param rawHandler of request, cannot be null
     * @see HttpUrl
     * @see okhttp3.Headers
     * @see RequestBody
     * @see RawHandler
     */
    private void execute(@NonNull HttpUrl url, @Nullable okhttp3.Headers headers, @Nullable RequestBody requestBody, @NonNull RawHandler rawHandler) {
        thread.run(thread.BACKGROUND, () -> {
            try {
                log.v(TAG,
                        "execute | load | " +
                        "url=" + url.toString() + " | " +
                        "headers=" + getLogHeaders(headers) + " | " +
                        "requestBody=" + getLogRequestBody(requestBody)
                );
                // build request
                okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
                builder.url(url);
                if (headers != null) {
                    builder.headers(headers);
                }
                if (requestBody != null) {
                    MediaType contentType = requestBody.contentType();
                    builder.addHeader("Content-Type", contentType == null ? "application/x-www-form-urlencoded" : contentType.toString());
                    builder.addHeader("Content-Length", String.valueOf(requestBody.contentLength()));
                    builder.post(requestBody);
                }
                okhttp3.Request request = builder.build();
                // perform request
                Call call = networkClientProvider.get().newCall(request);
                rawHandler.onNewRequest(new Request(call));
                Response response = call.execute();
                ResponseBody responseBody = response.body();
                // fetch response as string
                String responseString = "";
                if (responseBody != null) {
                    final int bufferSize = 1024;
                    final char[] buffer = new char[bufferSize];
                    final StringBuilder out = new StringBuilder();
                    final Reader reader = responseBody.charStream();
                    int length;
                    while ((length = reader.read(buffer, 0, buffer.length)) != -1) {
                        out.append(buffer, 0, length);
                    }
                    responseString = out.toString();
                }
                call.cancel();
                // it's all over..
                final int code = response.code();
                final okhttp3.Headers responseHeaders = response.headers();
                log.v(TAG,
                        "execute | done | " +
                        "url=" + url.toString() + " | " +
                        "code=" + code + " | " +
                        "headers=" + getLogHeaders(responseHeaders) + " | " +
                        "response=" + (responseString.isEmpty() ? "<empty>" : "<string>")
                );
                rawHandler.onDone(code, responseHeaders, responseString);
            } catch (Throwable throwable) {
                rawHandler.onError(STATUS_CODE_EMPTY, null, throwable);
            }
        });
    }

    @Nullable
    private String tryFixInvalidJsonResponse(@Nullable String response) {
        // _sometimes_ deifmo server provides corrupted json response
        // let's try to fix it
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
    @NonNull
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
    @Nullable
    protected JSONArray parseCookies(@Nullable final okhttp3.Headers headers) {
        try {
            if (headers == null) {
                return null;
            }
            final JSONArray parsed = new JSONArray();
            Map<String, List<String>> headersMap = headers.toMultimap();
            if (headersMap.containsKey("set-cookie")) {
                List<String> set_cookie = headersMap.get("set-cookie");
                for (String cookieAndAttributes : set_cookie) {
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
            }
            return parsed;
        } catch (Exception e) {
            log.exception(e);
            return new JSONArray();
        }
    }

    public boolean isAuthorized(@NonNull final Context context) {
        return true;
    }
    protected boolean isInterrupted(@Nullable final Throwable throwable) {
        return throwable != null && throwable.getMessage() != null && "socket closed".equalsIgnoreCase(throwable.getMessage());
    }
    protected boolean isCorruptedJson(@Nullable final Throwable throwable) {
        return throwable != null && throwable.getMessage() != null && (
                "Response is not recognized as JSONObject or JSONArray".equalsIgnoreCase(throwable.getMessage()) ||
                "Failed to parse JSONArray".equalsIgnoreCase(throwable.getMessage()) ||
                "Failed to parse JSONObject".equalsIgnoreCase(throwable.getMessage())
        );
    }

    @NonNull
    private String getLogRequestBody(@Nullable final RequestBody requestBody) {
        try {
            if (requestBody == null) {
                return "<null>";
            }
            final Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);
            String log = buffer.readUtf8().trim();
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
    @NonNull
    private String getLogHeaders(@Nullable final okhttp3.Headers headers) {
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

    @NonNull
    public static String getFailureMessage(@NonNull final Context context, final int statusCode) {
        return context.getString(R.string.server_error) + (statusCode > 0 ? "\n[status code: " + statusCode + "]" : "");
    }
    public static @StringRes int getFailureMessage() {
        return R.string.server_error;
    }
    public static boolean isOnline(@NonNull final Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager == null) {
                return true;
            }
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return (networkInfo != null && networkInfo.isConnected());
        } catch (Exception e) {
            return true;
        }
    }

    public class Request {
        private Call call;
        public Request(Call call) {
            this.call = call;
        }
        public boolean cancel() {
            if (call != null && !call.isCanceled()) {
                log.v(TAG, "request cancelled | url=" + call.request().url());
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
