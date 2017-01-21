package com.bukhmastov.cdoitmo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;

class DeIfmoRestClient {

    private static final String TAG = "DeIfmoRestClient";
    private static final String BASE_URL = "http://de.ifmo.ru/";
    private static final String BASE_URL_IFMO = "http://www.ifmo.ru/";
    private static final String USER_AGENT = "Android Application";
    private static AsyncHttpClient httpclient = new AsyncHttpClient();
    private static boolean initialized = false;
    private static Context context;

    static final int STATE_CHECKING = 0;
    static final int STATE_AUTHORIZATION = 1;
    static final int STATE_AUTHORIZED = 2;
    static final int STATE_HANDLING = 3;

    static final int FAILED_OFFLINE = 0;
    static final int FAILED_TRY_AGAIN = 1;
    static final int FAILED_AUTH_TRY_AGAIN = 2;
    static final int FAILED_AUTH_CREDENTIALS_REQUIRED = 3;
    static final int FAILED_AUTH_CREDENTIALS_FAILED = 4;

    static void init(Context ctx){
        if(!initialized){
            context = ctx;
            httpclient.setLoggingLevel(Log.WARN);
            httpclient.addHeader("User-Agent", USER_AGENT);
            httpclient.addHeader("Cookie", "JSESSIONID=" + Storage.get(context, "session_cookie") + "; Path=/;");
            initialized = true;
        }
    }
    static void check(final DeIfmoRestClientResponseHandler responseHandler){
        if(isOnline()){
            responseHandler.onProgress(STATE_CHECKING);
            if (Objects.equals(Storage.get(context, "session_cookie"), "")){
                authorize(new DeIfmoRestClientResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, String response) {
                        check(responseHandler);
                    }
                    @Override
                    public void onProgress(int state) {
                        responseHandler.onProgress(state);
                    }
                    @Override
                    public void onFailure(int state) {
                        responseHandler.onFailure(state);
                    }
                    @Override
                    public void onNewHandle(RequestHandle requestHandle) {
                        responseHandler.onNewHandle(requestHandle);
                    }
                });
            } else {
                DeIfmoRestClient.get("servlet/distributedCDE?Rule=editPersonProfile", null, new DeIfmoRestClientResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, String response) {
                        new UserDataParse(new UserDataParse.response() {
                            @Override
                            public void finish(HashMap<String, String> result) {
                                if(result != null){
                                    Storage.put(context, "name", result.get("name"));
                                    Storage.put(context, "group", result.get("group"));
                                    try {
                                        JSONObject jsonObject = new JSONObject();
                                        jsonObject.put("timestamp", Calendar.getInstance().getTimeInMillis());
                                        jsonObject.put("week", Integer.parseInt(result.get("week")));
                                        Storage.put(context, "week", jsonObject.toString());
                                    } catch (Exception e) {
                                        LoginActivity.errorTracker.add(e);
                                        Storage.delete(context, "week");
                                    }
                                    responseHandler.onSuccess(200, result.get("name"));
                                } else {
                                    responseHandler.onSuccess(200, "");
                                }
                            }
                        }).execute(response);
                    }
                    @Override
                    public void onProgress(int state) {}
                    @Override
                    public void onFailure(int state) {
                        responseHandler.onFailure(FAILED_TRY_AGAIN);
                    }
                    @Override
                    public void onNewHandle(RequestHandle requestHandle) {
                        responseHandler.onNewHandle(requestHandle);
                    }
                });
            }
        } else {
            responseHandler.onFailure(FAILED_OFFLINE);
        }
    }
    static void authorize(final DeIfmoRestClientResponseHandler responseHandler){
        responseHandler.onProgress(STATE_AUTHORIZATION);
        String login = Storage.get(context, "login");
        String password = Storage.get(context, "password");
        if (Objects.equals(login, "") || Objects.equals(password, "")) {
            responseHandler.onFailure(FAILED_AUTH_CREDENTIALS_REQUIRED);
        } else {
            RequestParams params = new RequestParams();
            params.put("Rule", "LOGON");
            params.put("LOGIN", login);
            params.put("PASSWD", password);
            renewSessionCookie();
            responseHandler.onNewHandle(httpclient.post(getAbsoluteUrl("servlet", false), params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    responseHandler.onNewHandle(null);
                    for (Header header : headers) {
                        if (Objects.equals(header.getName(), "Set-Cookie")) {
                            String[] pairs = header.getValue().split(";");
                            for (String pair : pairs) {
                                String[] cookie = pair.split("=");
                                if (Objects.equals(cookie[0], "JSESSIONID")) {
                                    if (!Objects.equals(cookie[1], "") && cookie[1] != null) {
                                        Storage.put(context, "session_cookie", cookie[1]);
                                    } else {
                                        Log.w(TAG, "Got 'Set-Cookie' with empty 'JSESSIONID'");
                                        responseHandler.onFailure(FAILED_AUTH_TRY_AGAIN);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    try {
                        String data = new String((new String(responseBody, "windows-1251")).getBytes("UTF-8"));
                        if (data.contains("Access is forbidden") && data.contains("Invalid login/password")) {
                            responseHandler.onFailure(FAILED_AUTH_CREDENTIALS_FAILED);
                        } else if (data.contains("Выбор группы безопасности") && data.contains("OPTION VALUE=8")) {
                            httpclient.get(getAbsoluteUrl("servlet/distributedCDE?Rule=APPLYSECURITYGROUP&PERSON=" + Storage.get(context, "login") + "&SECURITYGROUP=8&COMPNAME=", false), null, new AsyncHttpResponseHandler(){
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                                    responseHandler.onProgress(STATE_AUTHORIZED);
                                    responseHandler.onSuccess(statusCode, "authorized");
                                }
                                @Override
                                public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                                    responseHandler.onFailure(FAILED_AUTH_TRY_AGAIN);
                                }
                            });
                        } else if(data.contains("Обучение и аттестация")){
                            responseHandler.onProgress(STATE_AUTHORIZED);
                            responseHandler.onSuccess(statusCode, "authorized");
                        } else {
                            responseHandler.onFailure(FAILED_AUTH_TRY_AGAIN);
                        }
                    } catch (UnsupportedEncodingException e) {
                        LoginActivity.errorTracker.add(e);
                        responseHandler.onFailure(FAILED_AUTH_TRY_AGAIN);
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    responseHandler.onNewHandle(null);
                    responseHandler.onFailure(FAILED_AUTH_TRY_AGAIN);
                }
            }));
        }
    }
    static void get(String url, final RequestParams params, DeIfmoRestClientResponseHandler responseHandler){
        get(url, params, false, responseHandler);
    }
    static void get(final String url, final RequestParams params, final boolean is_ifmo, final DeIfmoRestClientResponseHandler responseHandler){
        if(isOnline()) {
            responseHandler.onProgress(STATE_HANDLING);
            renewSessionCookie();
            responseHandler.onNewHandle(httpclient.get(getAbsoluteUrl(url, is_ifmo), params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    responseHandler.onNewHandle(null);
                    try {
                        String data = "";
                        if(is_ifmo){
                            data = new String(responseBody, "UTF-8");
                        } else {
                            if (responseBody != null) data = new String((new String(responseBody, "windows-1251")).getBytes("UTF-8"));
                        }
                        if (data.contains("Закончился интервал неактивности") || data.contains("Доступ запрещен")) {
                            authorize(new DeIfmoRestClientResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, String response) {
                                    get(url, params, responseHandler);
                                }
                                @Override
                                public void onProgress(int state) {
                                    responseHandler.onProgress(state);
                                }
                                @Override
                                public void onFailure(int state) {
                                    responseHandler.onFailure(state);
                                }
                                @Override
                                public void onNewHandle(RequestHandle requestHandle) {
                                    responseHandler.onNewHandle(requestHandle);
                                }
                            });
                        } else {
                            responseHandler.onSuccess(statusCode, data);
                        }
                    } catch (UnsupportedEncodingException e) {
                        LoginActivity.errorTracker.add(e);
                        responseHandler.onFailure(FAILED_TRY_AGAIN);
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    responseHandler.onNewHandle(null);
                    responseHandler.onFailure(FAILED_TRY_AGAIN);
                }
            }));
        } else {
            responseHandler.onFailure(FAILED_OFFLINE);
        }
    }
    static void post(final String url, final RequestParams params, final DeIfmoRestClientResponseHandler responseHandler){
        if(isOnline()) {
            responseHandler.onProgress(STATE_HANDLING);
            renewSessionCookie();
            responseHandler.onNewHandle(httpclient.post(getAbsoluteUrl(url, false), params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    responseHandler.onNewHandle(null);
                    try {
                        String data = "";
                        if (responseBody != null) data = new String((new String(responseBody, "windows-1251")).getBytes("UTF-8"));
                        if (data.contains("Закончился интервал неактивности") || data.contains("Доступ запрещен")) {
                            authorize(new DeIfmoRestClientResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, String response) {
                                    post(url, params, responseHandler);
                                }
                                @Override
                                public void onProgress(int state) {
                                    responseHandler.onProgress(state);
                                }
                                @Override
                                public void onFailure(int state) {
                                    responseHandler.onFailure(state);
                                }
                                @Override
                                public void onNewHandle(RequestHandle requestHandle) {
                                    responseHandler.onNewHandle(requestHandle);
                                }
                            });
                        } else {
                            responseHandler.onSuccess(statusCode, data);
                        }
                    } catch (UnsupportedEncodingException e) {
                        LoginActivity.errorTracker.add(e);
                        responseHandler.onFailure(FAILED_TRY_AGAIN);
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    responseHandler.onNewHandle(null);
                    responseHandler.onFailure(FAILED_TRY_AGAIN);
                }
            }));
        } else {
            responseHandler.onFailure(FAILED_OFFLINE);
        }
    }
    static void getJSON(final String url, final RequestParams params, final DeIfmoRestClientJsonResponseHandler responseHandler){
        if(isOnline()) {
            responseHandler.onProgress(STATE_HANDLING);
            renewSessionCookie();
            responseHandler.onNewHandle(httpclient.get(getAbsoluteUrl(url, false), params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    responseHandler.onNewHandle(null);
                    responseHandler.onSuccess(statusCode, response);
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    responseHandler.onNewHandle(null);
                    responseHandler.onFailure(FAILED_TRY_AGAIN);
                }
            }));
        } else {
            responseHandler.onFailure(FAILED_OFFLINE);
        }
    }

    private static String getAbsoluteUrl(String relativeUrl, boolean is_ifmo) {
        return is_ifmo ? BASE_URL_IFMO + relativeUrl : BASE_URL + relativeUrl;
    }
    private static void renewSessionCookie(){
        httpclient.removeHeader("Cookie");
        httpclient.addHeader("Cookie", "JSESSIONID=" + Storage.get(context, "session_cookie") + "; Path=/;");
    }
    static boolean isOnline() {
        NetworkInfo networkInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }
}

class UserDataParse extends AsyncTask<String, Void, HashMap<String, String>> {
    interface response {
        void finish(HashMap<String, String> result);
    }
    private response delegate = null;
    UserDataParse(response delegate){
        this.delegate = delegate;
    }
    @Override
    protected HashMap<String, String> doInBackground(String... params) {
        try {
            HashMap<String, String> response = new HashMap<>();
            TagNode root = new HtmlCleaner().clean(params[0].replace("&nbsp;", " "));
            // находим имя пользователя
            TagNode fio = root.findElementByAttValue("id", "fio", true, false);
            response.put("name", fio.getText().toString().trim());
            // находим группу пользователя
            TagNode editForm = root.findElementByAttValue("name", "editForm", true, false);
            TagNode div = editForm.findElementByAttValue("class", "d_text", false, false);
            TagNode table = div.findElementByAttValue("class", "d_table", false, false);
            List<? extends TagNode> rows = table.findElementByName("tbody", false).getAllElementsList(false);
            for (TagNode row : rows) {
                List<? extends TagNode> columns = row.getAllElementsList(false);
                if (Objects.equals(columns.get(0).getText().toString().trim(), "Группа")) {
                    response.put("group", columns.get(1).getText().toString().trim());
                    break;
                }
            }
            // находим номер текущей недели
            TagNode divCalendarIcon = root.findElementByAttValue("id", "divCalendarIcon", true, false);
            Matcher m = Pattern.compile("^.*\\((.*) нед\\).*$").matcher(divCalendarIcon.getText().toString().trim());
            if(m.find()){
                response.put("week", m.group(1));
            } else {
                response.put("week", "-1");
            }
            return response;
        } catch (Exception e){
            LoginActivity.errorTracker.add(e);
            return null;
        }
    }
    @Override
    protected void onPostExecute(HashMap<String, String> result) {
        delegate.finish(result);
    }
}

interface DeIfmoRestClientResponseHandler {
    void onSuccess(int statusCode, String response);
    void onProgress(int state);
    void onFailure(int state);
    void onNewHandle(RequestHandle requestHandle);
}
interface DeIfmoRestClientJsonResponseHandler {
    void onSuccess(int statusCode, JSONObject response);
    void onProgress(int state);
    void onFailure(int state);
    void onNewHandle(RequestHandle requestHandle);
}