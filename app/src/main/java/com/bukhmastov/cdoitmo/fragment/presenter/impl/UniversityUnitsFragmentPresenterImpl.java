package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.ArrayMap;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.UniversityPersonCardActivity;
import com.bukhmastov.cdoitmo.adapter.rva.RVA;
import com.bukhmastov.cdoitmo.adapter.rva.RecyclerViewOnScrollListener;
import com.bukhmastov.cdoitmo.adapter.rva.university.UniversityFacultiesRVA;
import com.bukhmastov.cdoitmo.adapter.rva.university.UniversityRVA;
import com.bukhmastov.cdoitmo.adapter.rva.university.UniversityUnitsRVA;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.event.events.OpenIntentEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.UniversityUnitsFragmentPresenter;
import com.bukhmastov.cdoitmo.model.rva.RVASingleValue;
import com.bukhmastov.cdoitmo.model.university.units.UDivision;
import com.bukhmastov.cdoitmo.model.university.units.UUnit;
import com.bukhmastov.cdoitmo.model.university.units.UUnits;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import dagger.Lazy;

public class UniversityUnitsFragmentPresenterImpl implements UniversityUnitsFragmentPresenter, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "UniversityUnitsFragment";
    private FragmentActivity activity = null;
    private Fragment fragment = null;
    private View container;
    private Client.Request requestHandle = null;
    private boolean loaded = false;
    private final ArrayList<String> stack = new ArrayList<>();
    private final ArrayMap<String, UUnits> history = new ArrayMap<>();
    private UniversityUnitsRVA unitsRecyclerViewAdapter = null;
    private long timestamp = 0;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    Storage storage;
    @Inject
    StoragePref storagePref;
    @Inject
    IfmoRestClient ifmoRestClient;
    @Inject
    Static staticUtil;
    @Inject
    Time time;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;
    @Inject
    Lazy<NotificationMessage> notificationMessage;

    public UniversityUnitsFragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onClearCacheEvent(ClearCacheEvent event) {
        if (event.isNot(ClearCacheEvent.UNIVERSITY)) {
            return;
        }
        history.clear();
    }

    @Override
    public void setFragment(Fragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.getActivity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        log.v(TAG, "Fragment created");
        firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
    }

    @Override
    public void onDestroy() {
        log.v(TAG, "Fragment destroyed");
        loaded = false;
    }

    @Override
    public void onResume() {
        thread.run(() -> {
            log.v(TAG, "Fragment resumed");
            firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
            if (!loaded) {
                loaded = true;
                load();
            }
        });
    }

    @Override
    public void onPause() {
        thread.run(() -> {
            log.v(TAG, "Fragment paused");
            if (requestHandle != null && requestHandle.cancel()) {
                loaded = false;
            }
        });
    }

    @Override
    public void onCreateView(View container) {
        this.container = container;
    }

    @Override
    public void onRefresh() {
        log.v(TAG, "refreshing");
        load(true);
    }

    private void load() {
        thread.run(() -> load(storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)
                ? Integer.parseInt(storagePref.get(activity, "pref_static_refresh", "168"))
                : 0));
    }

    private void load(final int refresh_rate) {
        thread.run(() -> {
            log.v(TAG, "load | refresh_rate=", refresh_rate);
            String fid = stack.size() == 0 ? "0" : stack.get(stack.size() - 1);
            if (!(storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false))) {
                load(false);
                return;
            }
            UUnits cache = getFromCache(fid);
            if (cache == null) {
                load(true);
                return;
            }
            timestamp = cache.getTimestamp();
            if (cache.getTimestamp() + refresh_rate * 3600000L < time.getTimeInMillis()) {
                load(true, cache);
            } else {
                load(false, cache);
            }
        }, throwable -> {
            loadFailed();
        });
    }

    private void load(final boolean force) {
        thread.run(() -> load(force, null));
    }

    private void load(final boolean force, final UUnits cached) {
        thread.run(() -> {
            log.v(TAG, "load | force=" + (force ? "true" : "false"));
            final String uid = stack.size() == 0 ? "0" : stack.get(stack.size() - 1);
            if (!force && history.containsKey(uid)) {
                log.v(TAG, "load | from local cache");
                timestamp = time.getTimeInMillis();
                display(history.get(uid));
                return;
            }
            if ((!force || !Client.isOnline(activity)) && storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)) {
                UUnits cache = cached == null ? getFromCache(uid) : cached;
                if (cache != null) {
                    log.v(TAG, "load | from cache");
                    display(cache);
                    return;
                }
            }
            if (App.OFFLINE_MODE) {
                thread.runOnUI(() -> {
                    draw(R.layout.state_offline_text);
                    View reload = container.findViewById(R.id.offline_reload);
                    if (reload != null) {
                        reload.setOnClickListener(v -> load());
                    }
                }, throwable -> {
                    loadFailed();
                });
                return;
            }
            loadProvider(new RestResponseHandler() {
                @Override
                public void onSuccess(final int statusCode, final Client.Headers headers, final JSONObject obj, final JSONArray arr) {
                    thread.run(() -> {
                        if (statusCode == 200 && obj != null) {
                            UUnits data = new UUnits().fromJson(obj);
                            data.setTimestamp(time.getTimeInMillis());
                            if (storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)) {
                                storage.put(activity, Storage.CACHE, Storage.GLOBAL, "university#units#" + uid, data.toJsonString());
                            }
                            timestamp = data.getTimestamp();
                            history.put(uid, data);
                            display(data);
                            return;
                        }
                        loadFailed();
                    }, throwable -> {
                        loadFailed();
                    });
                }
                @Override
                public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                    thread.runOnUI(() -> {
                        log.v(TAG, "forceLoad | failure ", state);
                        switch (state) {
                            case IfmoRestClient.FAILED_OFFLINE: {
                                draw(R.layout.state_offline_text);
                                View reload = container.findViewById(R.id.offline_reload);
                                if (reload != null) {
                                    reload.setOnClickListener(v -> load());
                                }
                                break;
                            }
                            case IfmoRestClient.FAILED_CORRUPTED_JSON:
                            case IfmoRestClient.FAILED_SERVER_ERROR:
                            case IfmoRestClient.FAILED_TRY_AGAIN: {
                                draw(R.layout.state_failed_button);
                                TextView message = container.findViewById(R.id.try_again_message);
                                if (message != null) {
                                    switch (state) {
                                        case IfmoRestClient.FAILED_SERVER_ERROR:   message.setText(IfmoRestClient.getFailureMessage(activity, statusCode)); break;
                                        case IfmoRestClient.FAILED_CORRUPTED_JSON: message.setText(R.string.server_provided_corrupted_json); break;
                                    }
                                }
                                View reload = container.findViewById(R.id.try_again_reload);
                                if (reload != null) {
                                    reload.setOnClickListener(v -> load());
                                }
                                break;
                            }
                        }
                    }, throwable -> {
                        loadFailed();
                    });
                }
                @Override
                public void onProgress(final int state) {
                    thread.runOnUI(() -> {
                        log.v(TAG, "forceLoad | progress ", state);
                        draw(R.layout.state_loading_text);
                        TextView message = container.findViewById(R.id.loading_message);
                        if (message != null) {
                            switch (state) {
                                case IfmoRestClient.STATE_HANDLING: message.setText(R.string.loading); break;
                            }
                        }
                    });
                }
                @Override
                public void onNewRequest(Client.Request request) {
                    requestHandle = request;
                }
            });
        }, throwable -> {
            loadFailed();
        });
    }

    private void loadProvider(RestResponseHandler handler) {
        log.v(TAG, "loadProvider");
        String unit_id = "";
        if (stack.size() > 0) {
            unit_id = stack.get(stack.size() - 1);
        }
        ifmoRestClient.get(activity, "unit" + (unit_id.isEmpty() ? "" : "/" + unit_id), null, handler);
    }

    private void loadFailed() {
        thread.runOnUI(() -> {
            log.v(TAG, "loadFailed");
            draw(R.layout.state_failed_button);
            TextView message = container.findViewById(R.id.try_again_message);
            if (message != null) {
                message.setText(R.string.load_failed);
            }
            View reload = container.findViewById(R.id.try_again_reload);
            if (reload != null) {
                reload.setOnClickListener(v -> load());
            }
        }, throwable -> {
            log.exception(throwable);
        });
    }

    private void display(UUnits units) {
        thread.runOnUI(() -> {
            log.v(TAG, "display");
            if (units == null) {
                loadFailed();
                return;
            }
            draw(R.layout.layout_university_list_finite);
            // заголовок
            if (stack.size() == 0 || units.getUnit() == null) {
                ((ImageView) ((ViewGroup) container.findViewById(R.id.back)).getChildAt(0)).setImageResource(R.drawable.ic_refresh);
                container.findViewById(R.id.back).setOnClickListener(v -> load(true));
                ((TextView) container.findViewById(R.id.title)).setText(R.string.unit_general);
                staticUtil.removeView(container.findViewById(R.id.web));
            } else {
                container.findViewById(R.id.back).setOnClickListener(v -> {
                    stack.remove(stack.size() - 1);
                    load();
                });
                UUnit unit = units.getUnit();
                if (StringUtils.isNotBlank(unit.getTitle())) {
                    ((TextView) container.findViewById(R.id.title)).setText(unit.getTitle());
                } else {
                    staticUtil.removeView(container.findViewById(R.id.title));
                }
                String link;
                if (unit.getId() > 0) {
                    link = "http://www.ifmo.ru/ru/viewunit/" + unit.getId() + "/";
                } else {
                    link = null;
                }
                if (StringUtils.isNotBlank(link)) {
                    container.findViewById(R.id.web).setOnClickListener(view -> thread.run(() -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link.trim()));
                        eventBus.fire(new OpenIntentEvent(intent));
                    }));
                } else {
                    staticUtil.removeView(container.findViewById(R.id.web));
                }
            }
            // список
            unitsRecyclerViewAdapter = new UniversityUnitsRVA(activity);
            final RecyclerView recyclerView = container.findViewById(R.id.finite_list);
            if (recyclerView != null) {
                recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                recyclerView.setAdapter(unitsRecyclerViewAdapter);
                recyclerView.addOnScrollListener(new RecyclerViewOnScrollListener(container));
            }
            registerListeners();
            displayUnits(units);
            if (timestamp > 0 && timestamp + 5000 < time.getTimeInMillis()) {
                unitsRecyclerViewAdapter.addItem(new UniversityRVA.Item<>(
                        UniversityRVA.TYPE_INFO_ABOUT_UPDATE_TIME,
                        new RVASingleValue(activity.getString(R.string.update_date) + " " + time.getUpdateTime(activity, timestamp))
                ));
            }
            // добавляем отступ
            container.findViewById(R.id.top_panel).post(() -> {
                try {
                    int height = container.findViewById(R.id.top_panel).getHeight();
                    if (recyclerView != null) {
                        recyclerView.setPadding(0, height, 0, 0);
                        recyclerView.scrollToPosition(0);
                    }
                    LinearLayout finite_list_info = container.findViewById(R.id.finite_list_info);
                    if (finite_list_info != null && finite_list_info.getChildCount() > 0) {
                        finite_list_info.setPadding(0, height, 0, 0);
                    }
                } catch (Exception ignore) {
                    // ignore
                }
            });
            // работаем со свайпом
            SwipeRefreshLayout swipe = container.findViewById(R.id.finite_list_swipe);
            if (swipe != null) {
                swipe.setColorSchemeColors(Color.resolve(activity, R.attr.colorAccent));
                swipe.setProgressBackgroundColorSchemeColor(Color.resolve(activity, R.attr.colorBackgroundRefresh));
                swipe.setOnRefreshListener(this);
            }
        }, throwable -> {
            log.exception(throwable);
            loadFailed();
        });
    }

    private void displayUnits(UUnits units) {
        thread.run(() -> {
            Collection<RVA.Item> items = new ArrayList<>();
            if (units != null && units.getUnit() != null) {
                UUnit unit = units.getUnit();
                if (StringUtils.isNotBlank(unit.getAddress()) ||
                    StringUtils.isNotBlank(unit.getPhone()) ||
                    StringUtils.isNotBlank(unit.getEmail()) ||
                    StringUtils.isNotBlank(unit.getSite())
                ) {
                    items.add(new RVA.Item<>(UniversityFacultiesRVA.TYPE_UNIT_STRUCTURE_COMMON, unit));
                }
                if (StringUtils.isNotBlank(unit.getPost()) ||
                    StringUtils.isNotBlank(unit.getLastName())
                ) {
                    items.add(new RVA.Item<>(UniversityFacultiesRVA.TYPE_UNIT_STRUCTURE_HEAD, unit));
                }
            }
            if (units != null && CollectionUtils.isNotEmpty(units.getDivisions())) {
                items.add(new RVA.Item<>(UniversityFacultiesRVA.TYPE_UNIT_DIVISIONS, units));
            }
            if (items.size() == 0) {
                items.add(new UniversityRVA.Item(UniversityFacultiesRVA.TYPE_NO_DATA));
            }
            thread.runOnUI(() -> {
                if (unitsRecyclerViewAdapter != null) {
                    unitsRecyclerViewAdapter.addItems(items);
                }
            });
        }, throwable -> {
            log.exception(throwable);
            loadFailed();
        });
    }

    private void registerListeners() {
        if (unitsRecyclerViewAdapter == null) {
            return;
        }
        unitsRecyclerViewAdapter.clearClickListeners();
        unitsRecyclerViewAdapter.setClickListener(R.id.division, (v, entity) -> {
            thread.run(() -> {
                if (!(entity.getEntity() instanceof UDivision)) {
                    return;
                }
                int cisDepId = ((UDivision) entity.getEntity()).getId();
                stack.add(String.valueOf(cisDepId));
                load();
            }, throwable -> {
                showSnackBar(R.string.something_went_wrong);
            });
        });
        unitsRecyclerViewAdapter.setClickListener(R.id.university_tile_map, (v, entity) -> {
            thread.run(() -> {
                Uri uri = Uri.parse("geo:0,0?q=" + entity.getValueString());
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                eventBus.fire(new OpenIntentEvent(intent));
            }, throwable -> {
                showSnackBar(R.string.failed_to_start_geo_activity);
            });
        });
        unitsRecyclerViewAdapter.setClickListener(R.id.university_tile_phone, (v, entity) -> {
            thread.run(() -> {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + entity.getValueString()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                eventBus.fire(new OpenIntentEvent(intent));
            }, throwable -> {
                showSnackBar(R.string.something_went_wrong);
            });
        });
        unitsRecyclerViewAdapter.setClickListener(R.id.university_tile_web, (v, entity) -> {
            thread.run(() -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(entity.getValueString()));
                eventBus.fire(new OpenIntentEvent(intent));
            }, throwable -> {
                showSnackBar(R.string.something_went_wrong);
            });
        });
        unitsRecyclerViewAdapter.setClickListener(R.id.university_tile_mail, (v, entity) -> {
            thread.run(() -> {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("message/rfc822");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{entity.getValueString()});
                Intent chooser = Intent.createChooser(intent, activity.getString(R.string.send_mail) + "...");
                eventBus.fire(new OpenIntentEvent(chooser));
            }, throwable -> {
                showSnackBar(R.string.something_went_wrong);
            });
        });
        unitsRecyclerViewAdapter.setClickListener(R.id.university_tile_person, (v, entity) -> {
            thread.run(() -> {
                Bundle extras = new Bundle();
                extras.putInt("pid", entity.getValueInteger());
                eventBus.fire(new OpenActivityEvent(UniversityPersonCardActivity.class, extras));
            }, throwable -> {
                showSnackBar(R.string.something_went_wrong);
            });
        });
    }

    private void showSnackBar(@StringRes int res) {
        if (activity == null) {
            return;
        }
        notificationMessage.get().snackBar(activity, activity.getString(res));
    }

    private UUnits getFromCache(String uid) {
        thread.assertNotUI();
        String cache = storage.get(activity, Storage.CACHE, Storage.GLOBAL, "university#units#" + uid).trim();
        if (StringUtils.isBlank(cache)) {
            return null;
        }
        try {
            return new UUnits().fromJsonString(cache);
        } catch (Exception e) {
            storage.delete(activity, Storage.CACHE, Storage.GLOBAL, "university#units#" + uid);
            return null;
        }
    }

    private void draw(int layoutId) {
        try {
            ViewGroup vg = ((ViewGroup) container);
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(inflate(layoutId), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            log.exception(e);
        }
    }

    private View inflate(@LayoutRes int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}
