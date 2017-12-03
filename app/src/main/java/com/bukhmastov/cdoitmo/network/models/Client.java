package com.bukhmastov.cdoitmo.network.models;

import android.content.Context;
import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.network.interfaces.RawHandler;
import com.bukhmastov.cdoitmo.network.interfaces.RawJsonHandler;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

public abstract class Client {

    private static final String TAG = "Client";
    private static class client {
        private static OkHttpClient client = null;
        private static class timeout {
            private static final int connect = 10;
            private static final int read = 30;
        }
        private static OkHttpClient get() {
            if (client == null) {
                client = new OkHttpClient().newBuilder()
                        .addInterceptor(new LoggingInterceptor())
                        .followRedirects(false)
                        .connectTimeout(timeout.connect, TimeUnit.SECONDS)
                        .readTimeout(timeout.read, TimeUnit.SECONDS)
                        .build();
            }
            return client;
        }
    }
    public enum Protocol {HTTP, HTTPS}

    public static final int STATUS_CODE_EMPTY = -1;

    public static final int STATE_HANDLING = 0;
    public static final int FAILED_OFFLINE = 0;
    public static final int FAILED_TRY_AGAIN = 1;
    public static final int FAILED_SERVER_ERROR = 2;
    public static final int FAILED_INTERRUPTED = 3;

    protected static void _g(final String url, final okhttp3.Headers headers, final Map<String, String> query, final RawHandler rawHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
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
                        execute(builder.build(), headers, null, rawHandler);
                    }
                    execute(httpUrl, headers, null, rawHandler);
                } catch (Throwable throwable) {
                    rawHandler.onError(throwable);
                }
            }
        });
    }
    protected static void _p(final String url, final okhttp3.Headers headers, final Map<String, String> query, final Map<String, String> params, final RawHandler rawHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
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
                        execute(builder.build(), headers, formBody.build(), rawHandler);
                    } else {
                        execute(builder.build(), headers, null, rawHandler);
                    }
                } catch (Throwable throwable) {
                    rawHandler.onError(throwable);
                }
            }
        });
    }
    protected static void _gJson(final String url, final okhttp3.Headers headers, final Map<String, String> query, final RawJsonHandler rawJsonHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                try {
                    HttpUrl httpUrl = HttpUrl.parse(url);
                    if (httpUrl == null) {
                        throw new NullPointerException("httpUrl is null");
                    }
                    RawHandler rawHandler = new RawHandler() {
                        @Override
                        public void onDone(final int code, final okhttp3.Headers responseHeaders, final String response) {
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if (response.isEmpty()) {
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
                                                            jsonObject = new JSONObject(fixInvalidResponse(response));
                                                        }
                                                        rawJsonHandler.onDone(code, headers, response, jsonObject, null);
                                                    } catch (Throwable throwable) {
                                                        rawJsonHandler.onError(new ParseException("Failed to parse JSONObject", 0));
                                                    }
                                                } else if (response.startsWith("[") && response.endsWith("]")) {
                                                    try {
                                                        JSONArray jsonArray;
                                                        try {
                                                            jsonArray = new JSONArray(response);
                                                        } catch (Throwable throwable) {
                                                            jsonArray = new JSONArray(fixInvalidResponse(response));
                                                        }
                                                        rawJsonHandler.onDone(code, headers, response, null, jsonArray);
                                                    } catch (Throwable throwable) {
                                                        rawJsonHandler.onError(new ParseException("Failed to parse JSONArray", 0));
                                                    }
                                                } else {
                                                    rawJsonHandler.onError(new Exception("Response is not recognized as JSONObject or JSONArray"));
                                                }
                                            }
                                        }
                                    } catch (Throwable throwable) {
                                        rawJsonHandler.onError(throwable);
                                    }
                                }
                            });
                        }
                        @Override
                        public void onNewRequest(Request request) {
                            rawJsonHandler.onNewRequest(request);
                        }
                        @Override
                        public void onError(Throwable throwable) {
                            rawJsonHandler.onError(throwable);
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
                } catch (Throwable throwable) {
                    rawJsonHandler.onError(throwable);
                }
            }
        });
    }
    private static void execute(final HttpUrl url, final okhttp3.Headers headers, final RequestBody requestBody, final RawHandler rawHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                try {
                    Log.v(TAG,
                            "execute | load | " +
                            "url=" + (url == null ? "<null>" : url.toString()) + " | " +
                            "headers=" + getLogHeaders(headers) + " | " +
                            "requestBody=" + getLogRequestBody(requestBody)
                    );
                    okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
                    if (url != null) {
                        builder.url(url);
                    }
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
                    Call call = client.get().newCall(request);
                    rawHandler.onNewRequest(new Request(call));
                    okhttp3.Response response = call.execute();
                    ResponseBody responseBody = response.body();
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
                    final int code = response.code();
                    final okhttp3.Headers headers = response.headers();
                    Log.v(TAG,
                            "execute | done | " +
                            "url=" + (url == null ? "<null>" : url.toString()) + " | " +
                            "code=" + code + " | " +
                            "headers=" + getLogHeaders(headers) + " | " +
                            "response=" + (responseString.isEmpty() ? "<empty>" : "<string>")
                    );
                    rawHandler.onDone(code, headers, responseString);
                } catch (Throwable throwable) {
                    rawHandler.onError(throwable);
                }
            }
        });
    }

    protected static String getProtocol(Protocol protocol) {
        switch (protocol) {
            case HTTP: return "http://";
            case HTTPS: return "https://";
            default:
                Protocol p = Protocol.HTTP;
                Log.wtf(TAG, "getProtocol | undefined protocol, going to use " + p.toString());
                return getProtocol(p);
        }
    }
    private static String fixInvalidResponse(String response) {
        Matcher m = Pattern.compile("(\\\\u)([0-9a-f]{3})[^0-9a-f]").matcher(response);
        if (m.find()) {
            response = m.replaceAll(m.group(1) + "0" + m.group(2));
        }
        return response;
    }

    protected static JSONArray parseCookies(final okhttp3.Headers headers) {
        try {
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
                    //Log.v(TAG, "parseCookies | cookie: " + cookieName + "=" + cookieValue);
                    JSONArray attrs = new JSONArray();
                    for (int i = 1; i < attributes.length; i++) {
                        String[] attribute = attributes[i].split("=");
                        if (attribute.length != 2) continue;
                        String attrName = attribute[0].trim().toLowerCase();
                        String attrValue = attribute[1].trim();
                        //Log.v(TAG, "parseCookies |    attr: " + attrName + "=" + attrValue);
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
            Static.error(e);
            return new JSONArray();
        }
    }
    protected static boolean isInterrupted(final Throwable throwable) {
        return throwable != null && throwable.getMessage() != null && "socket closed".equals(throwable.getMessage().toLowerCase());
    }

    public static boolean isAuthorized(final Context context) {
        return true;
    }

    private static final class Secured {
        private static final String[] request = new String[] {"passwd", "pass", "password"};
        private static final String[] headers = new String[] {"route", "JSESSIONID", "PHPSESSID"};
    }
    private static String getLogRequestBody(final RequestBody requestBody) {
        try {
            if (requestBody == null) {
                return "<null>";
            }
            final Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);
            String log = buffer.readUtf8().trim();
            for (String secured : Secured.request) {
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
    private static String getLogHeaders(final okhttp3.Headers headers) {
        try {
            if (headers == null) {
                return "<null>";
            }
            String log = headers.toString().replaceAll("\n", " ").trim();
            for (String secured : Secured.headers) {
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
    public static String getFailureMessage(final Context context, final int statusCode) {
        return context.getString(R.string.server_error) + (statusCode > 0 ? " [status code: " + statusCode + "]" : "");
    }

    public static class Request {
        private Call call = null;
        public Request(Call call) {
            this.call = call;
        }
        public boolean cancel() {
            if (call != null && !call.isCanceled()) {
                Log.v(TAG, "request cancelled | url=" + call.request().url());
                call.cancel();
                return true;
            } else {
                return false;
            }
        }
    }
    public static class Headers {
        private okhttp3.Headers headers = null;
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

    private static class LoggingInterceptor implements Interceptor {
        @Override
        public Response intercept(@NonNull Interceptor.Chain chain) throws IOException {
            okhttp3.Request request = chain.request();
            long t1 = System.nanoTime();
            Log.v(TAG,
                    "execute | interceptor | request | " +
                    request.method() + " | " +
                    request.url()
            );
            Response response = chain.proceed(request);
            long t2 = System.nanoTime();
            Log.v(TAG,
                    "execute | interceptor | response | " +
                    response.request().method() + " | " +
                    response.request().url() + " | " +
                    ((t2 - t1) / 1e6d) + "ms" + " | " +
                    response.code() + " | " +
                    response.message()
            );
            return response;
        }
    }
}
