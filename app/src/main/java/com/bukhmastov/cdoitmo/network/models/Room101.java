package com.bukhmastov.cdoitmo.network.models;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.RawHandler;
import com.bukhmastov.cdoitmo.utils.Static;

import java.util.Map;

public abstract class Room101 extends DeIfmo {

    public static final int FAILED_AUTH = 10;
    public static final int FAILED_EXPECTED_REDIRECTION = 11;

    protected static void g(final Context context, final String url, final Map<String, String> query, final RawHandler rawHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                try {
                    _g(url, getHeaders(context), query, new RawHandler() {
                        @Override
                        public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
                                    storeCookies(context, headers, false);
                                    rawHandler.onDone(code, headers, response);
                                }
                            });
                        }
                        @Override
                        public void onError(final Throwable throwable) {
                            rawHandler.onError(throwable);
                        }
                        @Override
                        public void onNewRequest(final Request request) {
                            rawHandler.onNewRequest(request);
                        }
                    });
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
                    _p(url, getHeaders(context), null, params, new RawHandler() {
                        @Override
                        public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
                                    storeCookies(context, headers, false);
                                    rawHandler.onDone(code, headers, response);
                                }
                            });
                        }
                        @Override
                        public void onError(final Throwable throwable) {
                            rawHandler.onError(throwable);
                        }
                        @Override
                        public void onNewRequest(final Request request) {
                            rawHandler.onNewRequest(request);
                        }
                    });
                } catch (Throwable throwable) {
                    rawHandler.onError(throwable);
                }
            }
        });
    }
}
