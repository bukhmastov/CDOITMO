package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.content.Intent;
import android.net.Uri;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.adapter.rva.GroupRVA;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.event.events.OpenIntentEvent;
import com.bukhmastov.cdoitmo.event.events.ShareTextEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.LinkedAccountsFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.IsuGroupInfoFragmentPresenter;
import com.bukhmastov.cdoitmo.model.group.GGroup;
import com.bukhmastov.cdoitmo.model.group.GList;
import com.bukhmastov.cdoitmo.model.group.GPerson;
import com.bukhmastov.cdoitmo.network.IsuPrivateRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static com.bukhmastov.cdoitmo.util.Thread.IG;

public class IsuGroupInfoFragmentPresenterImpl extends ConnectedFragmentWithDataPresenterImpl<GList>
        implements IsuGroupInfoFragmentPresenter, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "IsuGroupInfoFragment";

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
    IsuPrivateRestClient isuPrivateRestClient;
    @Inject
    Time time;
    @Inject
    NotificationMessage notificationMessage;

    public IsuGroupInfoFragmentPresenterImpl() {
        super(GList.class);
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onClearCacheEvent(ClearCacheEvent event) {
        if (event.isNot(ClearCacheEvent.GROUPS)) {
            return;
        }
        clearData();
    }

    @Override
    public void onToolbarSetup(Menu menu) {
        try {
            thread.assertUI();
            if (menu == null) {
                return;
            }
            MenuItem share = menu.findItem(R.id.action_share);
            thread.run(IG, () -> {
                if (share == null) {
                    return;
                }
                GList data = getData();
                if (data == null || CollectionUtils.isEmpty(data.getList())) {
                    thread.runOnUI(IG, () -> share.setVisible(false));
                    return;
                }
                thread.runOnUI(IG, () -> {
                    share.setVisible(true);
                    share.setOnMenuItemClickListener(menuItem -> {
                        View view = activity.findViewById(R.id.action_share);
                        if (view == null) {
                            view = activity.findViewById(android.R.id.content);
                        }
                        if (view != null) {
                            share(view);
                        }
                        return true;
                    });
                });
            });
        } catch (Throwable throwable) {
            log.exception(throwable);
        }
    }

    @Override
    public void onRefresh() {
        thread.run(IG, () -> {
            log.v(TAG, "refreshing");
            load(true);
        });
    }

    protected void load() {
        thread.run(IG, () -> {
            load(storagePref.get(activity, "pref_use_cache", true) ?
                    Integer.parseInt(storagePref.get(activity, "pref_static_refresh", "168"))
                    : 0);
        });
    }

    private void load(int refresh_rate) {
        thread.run(IG, () -> {
            log.v(TAG, "load | refresh_rate=", refresh_rate);
            if (!storagePref.get(activity, "pref_use_cache", true)) {
                load(false);
                return;
            }
            GList cache = getFromCache();
            if (cache == null) {
                load(true, null);
                return;
            }
            setData(cache);
            if (cache.getTimestamp() + refresh_rate * 3600000L < time.getTimeInMillis()) {
                load(true, cache);
            } else {
                load(false, cache);
            }
        });
    }

    private void load(boolean force) {
        thread.run(IG, () -> load(force, null));
    }

    private void load(boolean force, GList cached) {
        thread.run(IG, () -> {
            log.v(TAG, "load | force=", force);
            if ((!force || Client.isOffline(activity)) && storagePref.get(activity, "pref_use_cache", true)) {
                try {
                    GList cache = cached == null ? getFromCache() : cached;
                    if (cache != null) {
                        log.v(TAG, "load | from cache");
                        setData(cache);
                        display();
                        return;
                    }
                } catch (Exception e) {
                    log.v(TAG, "load | failed to load from cache");
                }
            }
            if (App.OFFLINE_MODE) {
                if (getData() != null) {
                    display();
                    return;
                }
                thread.runOnUI(IG, () -> {
                    fragment.draw(R.layout.state_offline_text);
                    View reload = fragment.container().findViewById(R.id.offline_reload);
                    if (reload != null) {
                        reload.setOnClickListener(v -> load());
                    }
                });
                return;
            }
            if (StringUtils.isBlank(storage.get(activity, Storage.PERMANENT, Storage.USER, "user#isu#access_token", ""))) {
                thread.runOnUI(IG, () -> {
                    fragment.draw(R.layout.layout_isu_required);
                    View openIsuAuth = fragment.container().findViewById(R.id.open_isu_auth);
                    if (openIsuAuth != null) {
                        openIsuAuth.setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, LinkedAccountsFragment.class, null));
                    }
                });
                return;
            }
            isuPrivateRestClient.get(activity, "groups/user/%apikey%/%isutoken%", null, new RestResponseHandler<GList>() {
                @Override
                public void onSuccess(int code, Client.Headers headers, GList response) throws Exception {
                    log.v(TAG, "load | success | code=", code, " | response=", response);
                    if (code == 200 && response != null) {
                        response.setTimestamp(time.getTimeInMillis());
                        if (storagePref.get(activity, "pref_use_cache", true)) {
                            storage.put(activity, Storage.CACHE, Storage.USER, "group#core", response.toJsonString());
                        }
                        setData(response);
                        display();
                        return;
                    }
                    if (getData() != null) {
                        display();
                        return;
                    }
                    loadFailed();
                }
                @Override
                public void onFailure(int code, Client.Headers headers, int state) {
                    thread.run(IG, () -> {
                        log.v(TAG, "load | failure ", state);
                        if (state == Client.FAILED_OFFLINE) {
                            if (getData() != null) {
                                display();
                                return;
                            }
                            thread.runOnUI(IG, () -> {
                                fragment.draw(R.layout.state_offline_text);
                                View reload = fragment.container().findViewById(R.id.offline_reload);
                                if (reload != null) {
                                    reload.setOnClickListener(v -> load());
                                }
                            }, throwable -> {
                                loadFailed();
                            });
                            return;
                        }
                        if (isuPrivateRestClient.isFailedAuth(state)) {
                            thread.runOnUI(IG, () -> {
                                fragment.draw(R.layout.layout_isu_required);
                                View openIsuAuth = fragment.container().findViewById(R.id.open_isu_auth);
                                if (openIsuAuth != null) {
                                    openIsuAuth.setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, LinkedAccountsFragment.class, null));
                                }
                                if (isuPrivateRestClient.isFailedAuthCredentials(state)) {
                                    TextView isuAuthMessage = fragment.container().findViewById(R.id.isu_auth_message);
                                    if (isuAuthMessage != null) {
                                        isuAuthMessage.setText(isuPrivateRestClient.getFailedMessage(activity, code, state));
                                    }
                                }
                            }, throwable -> {
                                loadFailed();
                            });
                            return;
                        }
                        thread.runOnUI(IG, () -> {
                            fragment.draw(R.layout.state_failed_button);
                            TextView message = fragment.container().findViewById(R.id.try_again_message);
                            if (message != null) {
                                message.setText(isuPrivateRestClient.getFailedMessage(activity, code, state));
                            }
                            View reload = fragment.container().findViewById(R.id.try_again_reload);
                            if (reload != null) {
                                reload.setOnClickListener(v -> load());
                            }
                        }, throwable -> {
                            loadFailed();
                        });
                    }, throwable -> {
                        loadFailed();
                    });
                }
                @Override
                public void onProgress(int state) {
                    thread.runOnUI(IG, () -> {
                        log.v(TAG, "load | progress ", state);
                        fragment.draw(R.layout.state_loading_text);
                        TextView message = fragment.container().findViewById(R.id.loading_message);
                        if (message != null) {
                            message.setText(isuPrivateRestClient.getProgressMessage(activity, state));
                        }
                    });
                }
                @Override
                public void onNewRequest(Client.Request request) {
                    requestHandle = request;
                }
                @Override
                public GList newInstance() {
                    return new GList();
                }
            });
        }, throwable -> {
            loadFailed();
        });
    }

    private void loadFailed() {
        thread.runOnUI(IG, () -> {
            log.v(TAG, "loadFailed");
            fragment.draw(R.layout.state_failed_button);
            TextView message = fragment.container().findViewById(R.id.try_again_message);
            if (message != null) {
                message.setText(R.string.load_failed);
            }
            View reload = fragment.container().findViewById(R.id.try_again_reload);
            if (reload != null) {
                reload.setOnClickListener(v -> load());
            }
        }, throwable -> {});
    }

    protected void display() {
        thread.run(IG, () -> {
            log.v(TAG, "display");
            GList data = getData();
            if (data == null) {
                loadFailed();
                return;
            }
            GroupRVA adapter = new GroupRVA(activity, data);
            adapter.setClickListener(R.id.person, (v, person) -> {
                if (person == null || StringUtils.isBlank(person.getUrl())) {
                    return;
                }
                thread.standalone(() -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(person.getUrl()));
                    eventBus.fire(new OpenIntentEvent(intent));
                }, throwable -> {
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                });
            });
            thread.runOnUI(IG, () -> {
                onToolbarSetup(fragment.toolbar());
                fragment.draw(R.layout.layout_rva);
                // set adapter to recycler view
                LinearLayoutManager layoutManager = new LinearLayoutManager(activity, RecyclerView.VERTICAL, false);
                RecyclerView recyclerView = fragment.container().findViewById(R.id.recyclerView);
                if (recyclerView != null) {
                    recyclerView.setLayoutManager(layoutManager);
                    recyclerView.setAdapter(adapter);
                    recyclerView.setHasFixedSize(true);
                }
                // setup swipe
                SwipeRefreshLayout swipe = fragment.container().findViewById(R.id.swipe_container);
                if (swipe != null) {
                    swipe.setColorSchemeColors(Color.resolve(activity, R.attr.colorAccent));
                    swipe.setProgressBackgroundColorSchemeColor(Color.resolve(activity, R.attr.colorBackgroundRefresh));
                    swipe.setOnRefreshListener(this);
                }
            }, throwable -> {
                log.exception(throwable);
                loadFailed();
            });
        }, throwable -> {
            log.exception(throwable);
            loadFailed();
        });
    }

    private void share(View view) {
        thread.run(IG, () -> {
            GList data = getData();
            if (data == null || CollectionUtils.isEmpty(data.getList())) {
                return;
            }
            Map<String, List<GGroup>> groups = new HashMap<>();
            for (GGroup gGroup : data.getList()) {
                if (gGroup == null) {
                    continue;
                }
                String group = gGroup.getGroup();
                if (!groups.containsKey(group)) {
                    groups.put(group, new ArrayList<>());
                }
                groups.get(group).add(gGroup);
            }
            thread.runOnUI(IG, () -> {
                SparseArray<Map<String, List<GGroup>>> menuGroupsMap = new SparseArray<>();
                PopupMenu popup = new PopupMenu(activity, view);
                popup.inflate(R.menu.group_share);
                if (groups.size() == 0) {
                    popup.getMenu().findItem(R.id.share_group).setVisible(false);
                } else {
                    Menu subMenu = popup.getMenu().findItem(R.id.share_group).getSubMenu();
                    for (Map.Entry<String, List<GGroup>> entry : groups.entrySet()) {
                        String group = entry.getKey();
                        List<GGroup> items = entry.getValue();
                        int id = View.generateViewId();
                        subMenu.add(Menu.NONE, id, Menu.NONE, group);
                        Map<String, List<GGroup>> currentGroups = new HashMap<>();
                        currentGroups.put(group, items);
                        menuGroupsMap.put(id, currentGroups);
                    }
                }
                popup.setOnMenuItemClickListener(menuItem -> {
                    thread.run(IG, () -> {
                        switch (menuItem.getItemId()) {
                            case R.id.share_all_groups: share(groups); break;
                            default:
                                Map<String, List<GGroup>> grp = menuGroupsMap.get(menuItem.getItemId());
                                if (grp != null) {
                                    share(grp);
                                }
                                break;
                        }
                    }, throwable -> {
                        log.exception(throwable);
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    });
                    popup.dismiss();
                    return true;
                });
                popup.show();
            }, throwable -> {
                log.exception(throwable);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            });
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }

    private void share(Map<String, List<GGroup>> groups) {
        thread.run(IG, () -> {
            if (groups == null) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Мои учебные группы:");
            if (groups.size() == 0) {
                sb.append("\n");
                sb.append("Нет групп");
            } else {
                for (Map.Entry<String, List<GGroup>> entry : groups.entrySet()) {
                    for (GGroup group : entry.getValue()) {
                        sb.append("\n");
                        sb.append(group.getGroup());
                        for (GPerson person : CollectionUtils.emptyIfNull(group.getPersons())) {
                            sb.append("\n");
                            sb.append(person.getNumber()).append(". ");
                            sb.append(person.getFio());
                            sb.append(" (").append(person.getPersonId()).append(")");
                        }
                        sb.append("\n");
                    }
                }
            }
            eventBus.fire(new ShareTextEvent(sb.toString().trim(), "txt_groups"));
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected String getCacheType() {
        return Storage.USER;
    }

    @Override
    protected String getCachePath() {
        return "group#core";
    }

    @Override
    protected String getThreadToken() {
        return IG;
    }
}
