package com.bukhmastov.cdoitmo.network.model;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.handlers.RawHandler;
import com.bukhmastov.cdoitmo.network.handlers.RawJsonHandler;
import com.bukhmastov.cdoitmo.util.singleton.PropertiesUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class Isu extends Client {

    public Isu() {
        super();
    }

    /**
     * Performs GET request and parse result as json
     * @param context context, cannot be null
     * @param url to be requested, cannot be null
     * @param query of request
     * @param rawJsonHandler of request, cannot be null
     * @see RawJsonHandler
     */
    protected void gJson(@NonNull final Context context, @NonNull final String url, @Nullable final Map<String, String> query, @NonNull final RawJsonHandler rawJsonHandler) {
        thread.run(thread.BACKGROUND, () -> {
            doGetJson(getUrl(context, url), getHeaders(context), query, rawJsonHandler);
        }, throwable -> {
            rawJsonHandler.onError(STATUS_CODE_EMPTY, null, throwable);
        });
    }

    @NonNull
    private okhttp3.Headers getHeaders(@NonNull final Context context) throws IOException {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", PropertiesUtils.getIsuProperty(context, "isu.api.user.agent"));
        return okhttp3.Headers.of(headers);
    }

    private String getUrl(@NonNull Context context, @NonNull String url) throws IOException {
        return url.replace("%apikey%", PropertiesUtils.getIsuProperty(context, "isu.api.key"));
    }
}
