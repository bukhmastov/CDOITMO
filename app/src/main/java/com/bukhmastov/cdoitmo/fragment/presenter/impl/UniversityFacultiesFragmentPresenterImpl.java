package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import com.bukhmastov.cdoitmo.view.RecyclerViewOnScrollListener;
import com.bukhmastov.cdoitmo.adapter.rva.UniversityFacultiesRVA;
import com.bukhmastov.cdoitmo.adapter.rva.UniversityRVA;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.event.events.OpenIntentEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.UniversityFacultiesFragmentPresenter;
import com.bukhmastov.cdoitmo.model.rva.RVASingleValue;
import com.bukhmastov.cdoitmo.model.university.faculties.UDivision;
import com.bukhmastov.cdoitmo.model.university.faculties.UFaculties;
import com.bukhmastov.cdoitmo.model.university.faculties.UStructure;
import com.bukhmastov.cdoitmo.model.university.faculties.UStructureInfo;
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

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import dagger.Lazy;

import static com.bukhmastov.cdoitmo.util.Thread.UF;

public class UniversityFacultiesFragmentPresenterImpl implements UniversityFacultiesFragmentPresenter, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "UniversityFacultiesFragment";
    private FragmentActivity activity = null;
    private Fragment fragment = null;
    private View container;
    private Client.Request requestHandle = null;
    private boolean loaded = false;
    private final ArrayList<String> stack = new ArrayList<>();
    private final ArrayMap<String, UFaculties> history = new ArrayMap<>();
    private UniversityFacultiesRVA facultiesRecyclerViewAdapter = null;
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

    public UniversityFacultiesFragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
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
        thread.initialize(UF);
        log.v(TAG, "onCreate");
        firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
    }

    @Override
    public void onDestroy() {
        log.v(TAG, "onDestroy");
        loaded = false;
        thread.interrupt(UF);
    }

    @Override
    public void onResume() {
        thread.run(UF, () -> {
            log.v(TAG, "onResume");
            firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
            if (!loaded) {
                loaded = true;
                load();
            }
        });
    }

    @Override
    public void onPause() {
        log.v(TAG, "onPause");
        thread.standalone(() -> {
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
        thread.run(UF, () -> {
            load(storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)
                    ? Integer.parseInt(storagePref.get(activity, "pref_static_refresh", "168"))
                    : 0);
        });
    }

    private void load(int refresh_rate) {
        thread.run(UF, () -> {
            log.v(TAG, "load | refresh_rate=", refresh_rate);
            String fid = stack.size() == 0 ? "0" : stack.get(stack.size() - 1);
            if (!(storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false))) {
                load(false);
                return;
            }
            UFaculties cache = getFromCache(fid);
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

    private void load(boolean force) {
        thread.run(UF, () -> load(force, null));
    }

    private void load(boolean force, UFaculties cached) {
        thread.run(UF, () -> {
            log.v(TAG, "load | force=", force);
            String fid = stack.size() == 0 ? "0" : stack.get(stack.size() - 1);
            if (!force && history.containsKey(fid)) {
                log.v(TAG, "load | from local cache");
                timestamp = time.getTimeInMillis();
                display(history.get(fid));
                return;
            }
            if ((!force || Client.isOffline(activity)) && storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)) {
                UFaculties cache = cached == null ? getFromCache(fid) : cached;
                if (cache != null) {
                    log.v(TAG, "load | from cache");
                    display(cache);
                    return;
                }
            }
            if (App.OFFLINE_MODE) {
                thread.runOnUI(UF, () -> {
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
            loadProvider(new RestResponseHandler<UFaculties>() {
                @Override
                public void onSuccess(int code, Client.Headers headers, UFaculties response) throws Exception {
                    if (code == 200 && response != null) {
                        response.setTimestamp(time.getTimeInMillis());
                        if (storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)) {
                            storage.put(activity, Storage.CACHE, Storage.GLOBAL, "university#faculties#" + fid, response.toJsonString());
                        }
                        timestamp = response.getTimestamp();
                        history.put(fid, response);
                        display(response);
                        return;
                    }
                    loadFailed();
                }
                @Override
                public void onFailure(int code, Client.Headers headers, int state) {
                    thread.runOnUI(UF, () -> {
                        log.v(TAG, "load | failure ", state);
                        if (state == Client.FAILED_OFFLINE) {
                            draw(R.layout.state_offline_text);
                            View reload = container.findViewById(R.id.offline_reload);
                            if (reload != null) {
                                reload.setOnClickListener(v -> load());
                            }
                            return;
                        }
                        draw(R.layout.state_failed_button);
                        TextView message = activity.findViewById(R.id.try_again_message);
                        if (message != null) {
                            message.setText(ifmoRestClient.getFailedMessage(activity, code, state));
                        }
                        View reload = container.findViewById(R.id.try_again_reload);
                        if (reload != null) {
                            reload.setOnClickListener(v -> load());
                        }
                    }, throwable -> {
                        loadFailed();
                    });
                }
                @Override
                public void onProgress(int state) {
                    thread.runOnUI(UF, () -> {
                        log.v(TAG, "load | progress ", state);
                        draw(R.layout.state_loading_text);
                        TextView message = container.findViewById(R.id.loading_message);
                        if (message != null) {
                            message.setText(ifmoRestClient.getProgressMessage(activity, state));
                        }
                    });
                }
                @Override
                public void onNewRequest(Client.Request request) {
                    requestHandle = request;
                }
                @Override
                public UFaculties newInstance() {
                    return new UFaculties();
                }
            });
        }, throwable -> {
            loadFailed();
        });
    }

    private void loadProvider(RestResponseHandler<UFaculties> handler) {
        log.v(TAG, "loadProvider");
        String dep_id = "";
        if (stack.size() > 0) {
            dep_id = stack.get(stack.size() - 1);
        }
        ifmoRestClient.get(activity, "study_structure" + (dep_id.isEmpty() ? "" : "/" + dep_id), null, handler);
    }

    private void loadFailed() {
        thread.runOnUI(UF, () -> {
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

    private void display(UFaculties faculties) {
        thread.runOnUI(UF, () -> {
            log.v(TAG, "display");
            if (faculties == null) {
                loadFailed();
                return;
            }
            draw(R.layout.layout_university_list_finite);
            // заголовок
            if (stack.size() == 0 || faculties.getStructure() == null) {
                ((ImageView) ((ViewGroup) container.findViewById(R.id.back)).getChildAt(0)).setImageResource(R.drawable.ic_refresh);
                container.findViewById(R.id.back).setOnClickListener(v -> load(true));
                ((TextView) container.findViewById(R.id.title)).setText(R.string.division_general);
                staticUtil.removeView(container.findViewById(R.id.web));
            } else {
                container.findViewById(R.id.back).setOnClickListener(v -> {
                    stack.remove(stack.size() - 1);
                    load();
                });
                UStructure structure = faculties.getStructure();
                if (StringUtils.isNotBlank(structure.getName())) {
                    ((TextView) container.findViewById(R.id.title)).setText(structure.getName().trim());
                } else {
                    staticUtil.removeView(container.findViewById(R.id.title));
                }
                String link;
                if (StringUtils.isNotBlank(structure.getLink())) {
                    link = structure.getLink();
                } else if (StringUtils.isNotBlank(structure.getType()) && structure.getTypeId() > 0) {
                    link = "http://www.ifmo.ru/ru/" + ("faculty".equals(structure.getType()) ? "viewfaculty" : "viewdepartment") + "/" + structure.getTypeId() + "/";
                } else {
                    link = null;
                }
                if (StringUtils.isNotBlank(link)) {
                    container.findViewById(R.id.web).setOnClickListener(view -> thread.run(UF, () -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link.trim()));
                        eventBus.fire(new OpenIntentEvent(intent));
                    }));
                } else {
                    staticUtil.removeView(container.findViewById(R.id.web));
                }
            }
            // список
            facultiesRecyclerViewAdapter = new UniversityFacultiesRVA(activity);
            RecyclerView recyclerView = container.findViewById(R.id.finite_list);
            if (recyclerView != null) {
                recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                recyclerView.setAdapter(facultiesRecyclerViewAdapter);
                recyclerView.addOnScrollListener(new RecyclerViewOnScrollListener(container));
            }
            registerListeners();
            displayFaculties(faculties);
            if (timestamp > 0 && timestamp + 5000 < time.getTimeInMillis()) {
                facultiesRecyclerViewAdapter.addItem(new UniversityRVA.Item<>(
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

    private void displayFaculties(UFaculties faculties) {
        thread.run(UF, () -> {
            Collection<RVA.Item> items = new ArrayList<>();
            if (faculties != null && faculties.getStructure() != null && faculties.getStructure().getStructureInfo() != null) {
                UStructureInfo structureInfo = faculties.getStructure().getStructureInfo();
                if (StringUtils.isNotBlank(structureInfo.getAddress()) ||
                    StringUtils.isNotBlank(structureInfo.getPhone()) ||
                    StringUtils.isNotBlank(structureInfo.getSite())
                ) {
                    items.add(new RVA.Item<>(UniversityFacultiesRVA.TYPE_UNIT_STRUCTURE_COMMON, faculties.getStructure()));
                }
                if (StringUtils.isNotBlank(structureInfo.getDeaneryAddress()) ||
                    StringUtils.isNotBlank(structureInfo.getDeaneryPhone()) ||
                    StringUtils.isNotBlank(structureInfo.getDeaneryEmail())
                ) {
                    items.add(new RVA.Item<>(UniversityFacultiesRVA.TYPE_UNIT_STRUCTURE_DEANERY, faculties.getStructure()));
                }
                if (StringUtils.isNotBlank(structureInfo.getPersonPost()) && (
                    StringUtils.isNotBlank(structureInfo.getLastName()) ||
                    StringUtils.isNotBlank(structureInfo.getEmail())
                )) {
                    items.add(new RVA.Item<>(UniversityFacultiesRVA.TYPE_UNIT_STRUCTURE_HEAD, faculties.getStructure()));
                }
            }
            if (faculties != null && CollectionUtils.isNotEmpty(faculties.getDivisions())) {
                items.add(new RVA.Item<>(UniversityFacultiesRVA.TYPE_UNIT_DIVISIONS, faculties));
            }
            if (items.size() == 0) {
                items.add(new UniversityFacultiesRVA.Item(UniversityFacultiesRVA.TYPE_NO_DATA));
            }
            thread.runOnUI(UF, () -> {
                if (facultiesRecyclerViewAdapter != null) {
                    facultiesRecyclerViewAdapter.addItems(items);
                }
            });
        }, throwable -> {
            log.exception(throwable);
            loadFailed();
        });
    }

    private void registerListeners() {
        if (facultiesRecyclerViewAdapter == null) {
            return;
        }
        facultiesRecyclerViewAdapter.clearClickListeners();
        facultiesRecyclerViewAdapter.setClickListener(R.id.division, (v, entity) -> {
            thread.run(UF, () -> {
                if (!(entity.getEntity() instanceof UDivision)) {
                    return;
                }
                int cisDepId = ((UDivision) entity.getEntity()).getCisDepId();
                stack.add(String.valueOf(cisDepId));
                load();
            }, throwable -> {
                showSnackBar(R.string.something_went_wrong);
            });
        });
        facultiesRecyclerViewAdapter.setClickListener(R.id.university_tile_map, (v, entity) -> {
            thread.run(UF, () -> {
                Uri uri = Uri.parse("geo:0,0?q=" + entity.getValueString());
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                eventBus.fire(new OpenIntentEvent(intent));
            }, throwable -> {
                showSnackBar(R.string.failed_to_start_geo_activity);
            });
        });
        facultiesRecyclerViewAdapter.setClickListener(R.id.university_tile_phone, (v, entity) -> {
            thread.run(UF, () -> {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + entity.getValueString()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                eventBus.fire(new OpenIntentEvent(intent));
            }, throwable -> {
                showSnackBar(R.string.something_went_wrong);
            });
        });
        facultiesRecyclerViewAdapter.setClickListener(R.id.university_tile_web, (v, entity) -> {
            thread.run(UF, () -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(entity.getValueString()));
                eventBus.fire(new OpenIntentEvent(intent));
            }, throwable -> {
                showSnackBar(R.string.something_went_wrong);
            });
        });
        facultiesRecyclerViewAdapter.setClickListener(R.id.university_tile_mail, (v, entity) -> {
            thread.run(UF, () -> {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("message/rfc822");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{entity.getValueString()});
                Intent chooser = Intent.createChooser(intent, activity.getString(R.string.send_mail) + "...");
                eventBus.fire(new OpenIntentEvent(chooser));
            }, throwable -> {
                showSnackBar(R.string.something_went_wrong);
            });
        });
        facultiesRecyclerViewAdapter.setClickListener(R.id.university_tile_person, (v, entity) -> {
            thread.run(UF, () -> {
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

    private UFaculties getFromCache(String fid) {
        thread.assertNotUI();
        String cache = storage.get(activity, Storage.CACHE, Storage.GLOBAL, "university#faculties#" + fid).trim();
        if (StringUtils.isBlank(cache)) {
            return null;
        }
        try {
            return new UFaculties().fromJsonString(cache);
        } catch (Exception e) {
            storage.delete(activity, Storage.CACHE, Storage.GLOBAL, "university#faculties#" + fid);
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

    private View inflate(@LayoutRes int layout) throws InflateException {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            log.e(TAG, "Failed to inflate layout, inflater is null");
            return null;
        }
        return inflater.inflate(layout, null);
    }
}
