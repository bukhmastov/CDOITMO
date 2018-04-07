package com.bukhmastov.cdoitmo.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.ArrayMap;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.LoginActivity;
import com.bukhmastov.cdoitmo.adapter.rva.RatingRVA;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.interfaces.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.parse.rating.RatingListParse;
import com.bukhmastov.cdoitmo.parse.rating.RatingParse;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class RatingFragment extends ConnectedFragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "RatingFragment";
    private boolean loaded = false;
    private Client.Request requestHandle = null;
    private final ArrayMap<String, Info> data = new ArrayMap<>();

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({COMMON, OWN})
    public @interface TYPE {}
    public static final String COMMON = "common";
    public static final String OWN = "own";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({EMPTY, LOADED, FAILED, OFFLINE, SERVER_ERROR})
    public @interface STATUS {}
    public static final String EMPTY = "empty";
    public static final String LOADED = "loaded";
    public static final String FAILED = "failed";
    public static final String OFFLINE = "offline";
    public static final String SERVER_ERROR = "server_error";

    public class Info {
        public @STATUS String status;
        public JSONObject data;
        public Info(@STATUS String status, JSONObject data) {
            this.status = status;
            this.data = data;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
        data.put(COMMON, new Info(EMPTY, null));
        data.put(OWN,    new Info(EMPTY, null));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Fragment destroyed");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rating, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "resumed");
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
        if (!loaded) {
            loaded = true;
            try {
                String storedData = restoreData(this);
                String storedExtra = restoreDataExtra(this);
                JSONObject storedCommon = storedData != null && !storedData.isEmpty() ? Static.string2json(storedData) : null;
                JSONObject storedOwn = storedExtra != null && !storedExtra.isEmpty() ? Static.string2json(storedExtra) : null;
                if (storedCommon != null) {
                    data.put(COMMON, new Info(LOADED, storedCommon));
                }
                if (storedOwn != null) {
                    data.put(OWN, new Info(LOADED, storedOwn));
                }
                if (storedCommon == null || storedOwn == null) {
                    load();
                } else {
                    display();
                }
            } catch (Exception e) {
                Static.error(e);
                load();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "paused");
        if (requestHandle != null && requestHandle.cancel()) {
            loaded = false;
        }
    }

    @Override
    public void onRefresh() {
        Log.v(TAG, "refreshing");
        load(COMMON, true);
    }

    private void load() {
        Static.T.runThread(() -> load(COMMON));
    }
    private void load(final @TYPE String type) {
        Static.T.runThread(() -> {
            switch (type) {
                case COMMON: {
                    load(type, Storage.pref.get(activity, "pref_use_cache", true) ? Integer.parseInt(Storage.pref.get(activity, "pref_static_refresh", "168")) : 0);
                    break;
                }
                case OWN: {
                    load(type, Storage.pref.get(activity, "pref_use_cache", true) ? Integer.parseInt(Storage.pref.get(activity, "pref_dynamic_refresh", "0")) : 0);
                    break;
                }
            }
        });
    }
    private void load(final @TYPE String type, final int refresh_rate) {
        Static.T.runThread(() -> {
            Log.v(TAG, "load | type=" + type + " | refresh_rate=" + refresh_rate);
            if (Storage.pref.get(activity, "pref_use_cache", true)) {
                String cache = "";
                switch (type) {
                    case COMMON: {
                        cache = Storage.file.cache.get(activity, "rating#list").trim();
                        break;
                    }
                    case OWN: {
                        cache = Storage.file.cache.get(activity, "rating#core").trim();
                        break;
                    }
                }
                if (!cache.isEmpty()) {
                    try {
                        data.get(type).data = new JSONObject(cache);
                        if (data.get(type).data.getLong("timestamp") + refresh_rate * 3600000L < Static.getCalendar().getTimeInMillis()) {
                            load(type, true, cache);
                        } else {
                            load(type, false, cache);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "load | type=", type, " | exception=", e);
                        load(type, true, cache);
                    }
                } else {
                    load(type, false);
                }
            } else {
                load(type, false);
            }
        });
    }
    private void load(final @TYPE String type, final boolean force) {
        Static.T.runThread(() -> load(type, force, ""));
    }
    private void load(final @TYPE String type, final boolean force, final String cache) {
        Static.T.runOnUiThread(() -> {
            draw(R.layout.state_loading);
            if (activity != null) {
                TextView loading_message = activity.findViewById(R.id.loading_message);
                if (loading_message != null) {
                    loading_message.setText(R.string.loading);
                }
            }
        });
        Static.T.runThread(() -> {
            if (Static.UNAUTHORIZED_MODE && type.equals(OWN)) {
                loaded(type);
                return;
            }
            Log.v(TAG, "load | type=" + type + " | force=" + (force ? "true" : "false"));
            if ((!force || !Static.isOnline(activity)) && Storage.pref.get(activity, "pref_use_cache", true)) {
                try {
                    String c = "";
                    if (cache.isEmpty()) {
                        switch (type) {
                            case COMMON: {
                                c = Storage.file.cache.get(activity, "rating#list").trim();
                                break;
                            }
                            case OWN: {
                                c = Storage.file.cache.get(activity, "rating#core").trim();
                                break;
                            }
                        }
                    } else {
                        c = cache;
                    }
                    if (!c.isEmpty()) {
                        Log.v(TAG, "load | type=" + type + " | from cache");
                        data.put(type, new Info(LOADED, new JSONObject(c)));
                        loaded(type);
                        return;
                    }
                } catch (Exception e) {
                    Log.v(TAG, "load | type=" + type + " | failed to load from cache");
                    switch (type) {
                        case COMMON: {
                            Storage.file.cache.delete(activity, "rating#list");
                            break;
                        }
                        case OWN: {
                            Storage.file.cache.delete(activity, "rating#core");
                            break;
                        }
                    }
                }
            }
            if (!Static.OFFLINE_MODE) {
                String url = "";
                switch (type) {
                    case COMMON: {
                        url = "index.php?node=rating";
                        break;
                    }
                    case OWN: {
                        url = "servlet/distributedCDE?Rule=REP_EXECUTE_PRINT&REP_ID=1441";
                        break;
                    }
                }
                DeIfmoClient.get(activity, url, null, new ResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                        Static.T.runThread(() -> {
                            Log.v(TAG, "load | type=" + type + " | success | statusCode=" + statusCode + " | response=" + (response == null ? "null" : "notnull"));
                            if (statusCode == 200) {
                                switch (type) {
                                    case COMMON: {
                                        new RatingListParse(response, json -> {
                                            if (json != null) {
                                                try {
                                                    json = new JSONObject()
                                                            .put("timestamp", Static.getCalendar().getTimeInMillis())
                                                            .put("rating", json);
                                                    if (Storage.pref.get(activity, "pref_use_cache", true)) {
                                                        Storage.file.cache.put(activity, "rating#list", json.toString());
                                                    }
                                                    data.put(type, new Info(LOADED, json));
                                                } catch (JSONException e) {
                                                    Static.error(e);
                                                    if (data.get(type).data != null) {
                                                        data.get(type).status = LOADED;
                                                        loaded(type);
                                                    } else {
                                                        data.put(type, new Info(FAILED, null));
                                                    }
                                                }
                                            } else {
                                                if (data.get(type).data != null) {
                                                    data.get(type).status = LOADED;
                                                    loaded(type);
                                                } else {
                                                    data.put(type, new Info(FAILED, null));
                                                }
                                            }
                                            loaded(type);
                                        }).run();
                                        break;
                                    }
                                    case OWN: {
                                        new RatingParse(response, json -> {
                                            if (json != null) {
                                                try {
                                                    json = new JSONObject()
                                                            .put("timestamp", Static.getCalendar().getTimeInMillis())
                                                            .put("rating", json);
                                                    if (Storage.pref.get(activity, "pref_use_cache", true)) {
                                                        Storage.file.cache.put(activity, "rating#core", json.toString());
                                                    }
                                                    data.put(type, new Info(LOADED, json));
                                                } catch (JSONException e) {
                                                    Static.error(e);
                                                    if (data.get(type).data != null) {
                                                        data.get(type).status = LOADED;
                                                        loaded(type);
                                                    } else {
                                                        data.put(type, new Info(FAILED, null));
                                                    }
                                                }
                                            } else {
                                                if (data.get(type).data != null) {
                                                    data.get(type).status = LOADED;
                                                    loaded(type);
                                                } else {
                                                    data.put(type, new Info(FAILED, null));
                                                }
                                            }
                                            loaded(type);
                                        }).run();
                                        break;
                                    }
                                }
                            } else {
                                if (data.get(type).data != null) {
                                    data.get(type).status = LOADED;
                                    loaded(type);
                                } else {
                                    data.put(type, new Info(FAILED, null));
                                    loaded(type);
                                }
                            }
                        });
                    }
                    @Override
                    public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                        Static.T.runThread(() -> {
                            Log.v(TAG, "load | type=" + type + " | failure " + state);
                            switch (state) {
                                case DeIfmoClient.FAILED_AUTH_CREDENTIALS_REQUIRED: {
                                    loaded = false;
                                    gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_REQUIRED);
                                    break;
                                }
                                case DeIfmoClient.FAILED_AUTH_CREDENTIALS_FAILED: {
                                    loaded = false;
                                    gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_FAILED);
                                    break;
                                }
                                case DeIfmoClient.FAILED_SERVER_ERROR: {
                                    data.put(type, new Info(SERVER_ERROR, null));
                                    loaded(type);
                                    break;
                                }
                                default: {
                                    if (data.get(type).data != null) {
                                        data.get(type).status = LOADED;
                                        loaded(type);
                                    } else {
                                        data.put(type, new Info(FAILED, null));
                                        loaded(type);
                                    }
                                    break;
                                }
                            }
                        });
                    }
                    @Override
                    public void onProgress(final int state) {
                        Log.v(TAG, "load | type=" + type + " | progress " + state);
                    }
                    @Override
                    public void onNewRequest(Client.Request request) {
                        requestHandle = request;
                    }
                });
            } else {
                if (data.get(type).data != null) {
                    data.get(type).status = LOADED;
                    loaded(type);
                } else {
                    data.put(type, new Info(OFFLINE, null));
                    loaded(type);
                }
            }
        });
    }
    private void loaded(final @TYPE String type) {
        Static.T.runThread(() -> {
            switch (type) {
                case COMMON: {
                    if (!Static.UNAUTHORIZED_MODE) {
                        load(OWN);
                    } else {
                        display();
                    }
                    break;
                }
                case OWN: {
                    display();
                    break;
                }
            }
        });
    }
    private void loadFailed() {
        Static.T.runOnUiThread(() -> {
            Log.v(TAG, "loadFailed");
            try {
                draw(R.layout.state_try_again);
                View try_again_reload = activity.findViewById(R.id.try_again_reload);
                if (try_again_reload != null) {
                    try_again_reload.setOnClickListener(v -> load());
                }
            } catch (Exception e) {
                Static.error(e);
            }
        });
    }
    private void display() {
        Static.T.runThread(() -> {
            try {
                Log.v(TAG, "display");
                storeData(this,
                        data.containsKey(COMMON) ? (data.get(COMMON).data != null ? data.get(COMMON).data.toString() : null) : null,
                        data.containsKey(OWN) ? (data.get(OWN).data != null ? data.get(OWN).data.toString() : null) : null
                );
                final RatingRVA adapter = new RatingRVA(activity, data);
                adapter.setOnElementClickListener(R.id.common_apply, (v, data) -> Static.T.runThread(() -> {
                    try {
                        FirebaseAnalyticsProvider.logBasicEvent(activity, "Detailed rating used");
                        final JSONObject d = (JSONObject) data.get("data");
                        final String faculty = d.getString("faculty");
                        final String course = d.getString("course");
                        Log.v(TAG, "detailed rating used | faculty=" + faculty + " | course=" + course);
                        Static.T.runOnUiThread(() -> {
                            try {
                                Bundle extras = new Bundle();
                                extras.putString("faculty", faculty);
                                extras.putString("course", course);
                                activity.openActivityOrFragment(RatingListFragment.class, extras);
                            } catch (Exception e) {
                                Static.error(e);
                                Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                            }
                        });
                    } catch (Exception e) {
                        Static.error(e);
                        Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    }
                }));
                adapter.setOnElementClickListener(R.id.own_apply, (v, data) -> Static.T.runThread(() -> {
                    try {
                        final JSONObject d = (JSONObject) data.get("data");
                        if (d != null) {
                            FirebaseAnalyticsProvider.logBasicEvent(activity, "Own rating used");
                            final String faculty = d.getString("faculty");
                            final String course = d.getString("course");
                            final String years = d.getString("years");
                            Log.v(TAG, "own rating used | faculty=" + faculty + " | course=" + course + " | years=" + years);
                            Static.T.runOnUiThread(() -> {
                                try {
                                    Bundle extras = new Bundle();
                                    extras.putString("faculty", faculty);
                                    extras.putString("course", course);
                                    extras.putString("years", years);
                                    activity.openActivityOrFragment(RatingListFragment.class, extras);
                                } catch (Exception e) {
                                    Static.error(e);
                                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                }
                            });
                        } else {
                            Log.v(TAG, "own rating used | not found");
                        }
                    } catch (Exception e) {
                        Static.error(e);
                        Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    }
                }));
                Static.T.runOnUiThread(() -> {
                    try {
                        draw(R.layout.layout_rating);
                        // set adapter to recycler view
                        final LinearLayoutManager layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
                        final RecyclerView rating_list = activity.findViewById(R.id.rating_list);
                        if (rating_list != null) {
                            rating_list.setLayoutManager(layoutManager);
                            rating_list.setAdapter(adapter);
                            rating_list.setHasFixedSize(true);
                        }
                        // setup swipe
                        final SwipeRefreshLayout swipe_container = activity.findViewById(R.id.swipe_container);
                        if (swipe_container != null) {
                            swipe_container.setColorSchemeColors(Static.colorAccent);
                            swipe_container.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                            swipe_container.setOnRefreshListener(this);
                        }
                    } catch (Exception e) {
                        Static.error(e);
                        loadFailed();
                    }
                });
            } catch (Exception e) {
                Static.error(e);
                loadFailed();
            }
        });
    }

    private void gotoLogin(final int state) {
        Static.T.runThread(() -> {
            try {
                Log.v(TAG, "gotoLogin | state=" + state);
                Intent intent = new Intent(activity, LoginActivity.class);
                intent.putExtra("state", state);
                activity.startActivity(intent);
            } catch (Exception e) {
                Static.error(e);
                loadFailed();
            }
        });
    }

    private void draw(int layoutId) {
        try {
            ViewGroup vg = activity.findViewById(R.id.container_rating);
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(inflate(layoutId), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            Static.error(e);
        }
    }
    private View inflate(int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}
