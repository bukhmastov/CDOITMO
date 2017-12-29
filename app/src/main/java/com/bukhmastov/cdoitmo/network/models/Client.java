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
import java.io.InputStream;
import java.io.Reader;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

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
                OkHttpClient.Builder builder = new OkHttpClient().newBuilder()
                        .addInterceptor(new LoggingInterceptor())
                        .followRedirects(false)
                        .connectTimeout(timeout.connect, TimeUnit.SECONDS)
                        .readTimeout(timeout.read, TimeUnit.SECONDS);
                trustDefinedCertificates(builder);
                client = builder.build();
            }
            return client;
        }
        private static void trustDefinedCertificates(OkHttpClient.Builder builder) {
            try {
                X509TrustManager trustManager = trustManagerForCertificates(trustedCertificatesInputStream());
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[] {trustManager}, null);
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                builder.sslSocketFactory(sslSocketFactory, trustManager);
            } catch (Exception e) {
                Static.error(e);
            }
        }
        private static InputStream trustedCertificatesInputStream() {
            /*
             * Certificate for: ITMO UNIVERSITY (*.ifmo.ru)
             * Issued by: GeoTrust TLS RSA CA G1 (www.digicert.com)
             * Valid: ‎21 dec ‎2017 - 19 feb ‎2020
             * Certificate at pem format:
             */
            String deifmoRsaCertificationAuthority = "-----BEGIN CERTIFICATE-----\n" +
                    "MIIGaDCCBVCgAwIBAgIQBNf0DeMvYXonJ8XhQ5nwBDANBgkqhkiG9w0BAQsFADBg\n" +
                    "MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3\n" +
                    "d3cuZGlnaWNlcnQuY29tMR8wHQYDVQQDExZHZW9UcnVzdCBUTFMgUlNBIENBIEcx\n" +
                    "MB4XDTE3MTIyMTAwMDAwMFoXDTIwMDIxOTEyMDAwMFowVjELMAkGA1UEBhMCUlUx\n" +
                    "GTAXBgNVBAcTEFNhaW50IFBldGVyc2J1cmcxGDAWBgNVBAoTD0lUTU8gVU5JVkVS\n" +
                    "U0lUWTESMBAGA1UEAwwJKi5pZm1vLnJ1MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A\n" +
                    "MIIBCgKCAQEA1vEpSSKivybXfZgDxBPQksD6z6IdJrMRjD08XiKkZcCrmFfj7rnO\n" +
                    "tNcuQghiI2BpM6G2PgXGaFrhpLIeK11LYRcRES02zVXhftvGvdrzsKU31f2JeDQI\n" +
                    "XDdG8PWgdbDkGcwaoh0tu8wCjH3DsZJfqjqcwPRU5zum1qEUXAeQJe1hy1s/C7nb\n" +
                    "LQOfss/CgwMlHnsktgVGKo/OXkH0Vx+QcsJLLWYksA2oJZng571KLSkWrng8YSNj\n" +
                    "ZaiA0achBVJaV7pVMTZ2exxY2vdPpM9/goJdBjnNydQi4I3Li+TNns6Xh5geMPgh\n" +
                    "dj/Vro+KMWhl5GkVtaWnhp7QUv4NVHNHYQIDAQABo4IDJjCCAyIwHwYDVR0jBBgw\n" +
                    "FoAUlE/UXYvkpOKmgP792PkA76O+AlcwHQYDVR0OBBYEFPdM63+7+oL0VeDSfujZ\n" +
                    "dfhSUEBlMB0GA1UdEQQWMBSCCSouaWZtby5ydYIHaWZtby5ydTAOBgNVHQ8BAf8E\n" +
                    "BAMCBaAwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMD8GA1UdHwQ4MDYw\n" +
                    "NKAyoDCGLmh0dHA6Ly9jZHAuZ2VvdHJ1c3QuY29tL0dlb1RydXN0VExTUlNBQ0FH\n" +
                    "MS5jcmwwTAYDVR0gBEUwQzA3BglghkgBhv1sAQEwKjAoBggrBgEFBQcCARYcaHR0\n" +
                    "cHM6Ly93d3cuZGlnaWNlcnQuY29tL0NQUzAIBgZngQwBAgIwdgYIKwYBBQUHAQEE\n" +
                    "ajBoMCYGCCsGAQUFBzABhhpodHRwOi8vc3RhdHVzLmdlb3RydXN0LmNvbTA+Bggr\n" +
                    "BgEFBQcwAoYyaHR0cDovL2NhY2VydHMuZ2VvdHJ1c3QuY29tL0dlb1RydXN0VExT\n" +
                    "UlNBQ0FHMS5jcnQwCQYDVR0TBAIwADCCAX4GCisGAQQB1nkCBAIEggFuBIIBagFo\n" +
                    "AHcApLkJkLQYWBSHuxOizGdwCjw1mAT5G9+443fNDsgN3BAAAAFgeIc1WgAABAMA\n" +
                    "SDBGAiEAx828SBz2uAVD4Esva7QIzstUbunWgtWKd7R+62yKSGECIQCNC1KgDmkJ\n" +
                    "DQAX3mDl285U89YIg+w6FQGQs18QGlByggB1AId1v+dZfPiMQ5lfvfNu/1aNR1Y2\n" +
                    "/0q1YMG06v9eoIMPAAABYHiHNqcAAAQDAEYwRAIgRixqDDO8p6uObzrDKC1o5u21\n" +
                    "7jUjQ8sG0gMEjv4NScACIFbVNPE1P77C60WwfbxTEz622vWjsg1FkCOzQ/siVNmQ\n" +
                    "AHYAu9nfvB+KcbWTlCOXqpJ7RzhXlQqrUugakJZkNo4e0YUAAAFgeIc2KgAABAMA\n" +
                    "RzBFAiEAuZMvRaGzl2jPNWiehhso1byGO46tjxChGjZk8B683W0CIBOSg+DDeaLN\n" +
                    "s22cpe5VkXolQTwhHww3GV5PTEZxUttoMA0GCSqGSIb3DQEBCwUAA4IBAQAeb0Np\n" +
                    "Y9MahNWXFfQIntW8Q6Cd1unOs8mt3G5G8y5GhuU+DZj5Iw4xAricAB/QUGGbrGtf\n" +
                    "Q3R9BA6S6elAcpUUaA1wuGUjh/yfZYx+VBTVhRhCgjO8J5KdkGzMSc6cArCbrkv6\n" +
                    "hNm8xAFtcp3/zOG+UaBaGd/dn6NCduoGSmE5YvOrf/HIvZB8REnlGhlG1JePd4WN\n" +
                    "lgsLC1rX8l65KiP/66j/S0krnDuFl7d/T8m1uAiUKGmGGPjM+gXnrVlcsnSJtkN9\n" +
                    "Li0qjL7Ei2eH7gHaxyzJ7bA6wodR3S3u/+MA+G/4jprDQjJv/z42hcERdw+d1uku\n" +
                    "VOepcc82nrVd0OOQ\n" +
                    "-----END CERTIFICATE-----";
            return new Buffer()
                    .writeUtf8(deifmoRsaCertificationAuthority)
                    .inputStream();
        }
        private static X509TrustManager trustManagerForCertificates(InputStream in) throws Exception {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(in);
            if (certificates.isEmpty()) {
                throw new IllegalArgumentException("expected non-empty set of trusted certificates");
            }
            // Put the certificates a key store.
            char[] password = "password".toCharArray(); // Any password will work.
            KeyStore keyStore = newEmptyKeyStore(password);
            int index = 0;
            for (Certificate certificate : certificates) {
                String certificateAlias = Integer.toString(index++);
                keyStore.setCertificateEntry(certificateAlias, certificate);
            }
            // Use it to build an X509 trust manager.
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, password);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
            }
            return (X509TrustManager) trustManagers[0];
        }
        private static KeyStore newEmptyKeyStore(char[] password) throws Exception {
            try {
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                InputStream in = null;
                keyStore.load(in, password);
                return keyStore;
            } catch (IOException e) {
                throw new AssertionError(e);
            }
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
                    rawHandler.onError(STATUS_CODE_EMPTY, null, throwable);
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
                    rawHandler.onError(STATUS_CODE_EMPTY, null, throwable);
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
                                                        rawJsonHandler.onError(code, headers, new ParseException("Failed to parse JSONObject", 0));
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
                                                        rawJsonHandler.onError(code, headers, new ParseException("Failed to parse JSONArray", 0));
                                                    }
                                                } else {
                                                    rawJsonHandler.onError(code, headers, new Exception("Response is not recognized as JSONObject or JSONArray"));
                                                }
                                            }
                                        }
                                    } catch (Throwable throwable) {
                                        rawJsonHandler.onError(code, headers, throwable);
                                    }
                                }
                            });
                        }
                        @Override
                        public void onNewRequest(Request request) {
                            rawJsonHandler.onNewRequest(request);
                        }
                        @Override
                        public void onError(int code, okhttp3.Headers headers, Throwable throwable) {
                            rawJsonHandler.onError(code, headers, throwable);
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
                    rawJsonHandler.onError(STATUS_CODE_EMPTY, null, throwable);
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
                    rawHandler.onError(STATUS_CODE_EMPTY, null, throwable);
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
