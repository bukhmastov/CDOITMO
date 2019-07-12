package com.bukhmastov.cdoitmo.util.impl;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseConfigProvider;
import com.bukhmastov.cdoitmo.util.NavigationMenu;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;
import com.bukhmastov.cdoitmo.view.Message;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.Lazy;

public class NavigationMenuImpl implements NavigationMenu {

    @Inject
    Thread thread;
    @Inject
    Lazy<NotificationMessage> notificationMessage;

    public NavigationMenuImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void displayEnableDisableOfflineButton(NavigationView navigationView) {
        thread.assertUI();
        if (navigationView == null) {
            return;
        }
        try {
            Menu menu = navigationView.getMenu();
            MenuItem navEnableOfflineMode = menu.findItem(R.id.nav_enable_offline_mode);
            MenuItem navDisableOfflineMode = menu.findItem(R.id.nav_disable_offline_mode);
            if (App.OFFLINE_MODE) {
                navEnableOfflineMode.setVisible(false);
                navDisableOfflineMode.setVisible(true);
            } else {
                navEnableOfflineMode.setVisible(true);
                navDisableOfflineMode.setVisible(false);
            }
        } catch (Exception ignore) {}
    }

    @Override
    public void displayUserData(Context context, Storage storage, NavigationView navigationView) {
        thread.assertNotUI();
        String name = storage.get(context, Storage.PERMANENT, Storage.USER, "user#name");
        List<String> groups = getGroups(context, storage);
        String group = android.text.TextUtils.join(", ", groups);
        thread.runOnUI(() -> {
            displayUserData(navigationView, R.id.user_name, name);
            displayUserData(navigationView, R.id.user_group, group);
            displayUserDataExpand(context, storage, navigationView, groups);
        });
    }

    @Override
    public void toggleOfflineIcon(Menu menu) {
        thread.assertUI();
        if (menu == null) {
            return;
        }
        MenuItem menuItem = menu.findItem(R.id.offline_mode);
        if (menuItem == null) {
            return;
        }
        menuItem.setVisible(App.OFFLINE_MODE);
    }

    @Override
    public void displayRemoteMessage(Activity activity, FirebaseConfigProvider firebaseConfigProvider, Storage storage) {
        firebaseConfigProvider.getMessage(activity, FirebaseConfigProvider.MESSAGE_MENU, value -> {
            if (value == null) {
                return;
            }
            int type = value.getType();
            String message = value.getMessage();
            if (StringUtils.isBlank(message)) {
                return;
            }
            String hash = StringUtils.crypt(message);
            if (hash != null && hash.equals(storage.get(activity, Storage.PERMANENT, Storage.GLOBAL, "firebase#remote_message#menu", ""))) {
                return;
            }
            thread.runOnUI(() -> {
                ViewGroup messageView = activity.findViewById(R.id.message_menu);
                View messageSeparator = activity.findViewById(R.id.message_menu_separator);
                View layout = Message.getRemoteMessage(activity, type, message, (context, view) -> {
                    if (hash == null) {
                        return;
                    }
                    if (!storage.put(activity, Storage.PERMANENT, Storage.GLOBAL, "firebase#remote_message#menu", hash)) {
                        return;
                    }
                    if (messageView != null && view != null) {
                        messageView.removeView(view);
                        if (messageSeparator != null) {
                            messageSeparator.setVisibility(View.GONE);
                        }
                    }
                    notificationMessage.get().snackBar(activity, activity.getString(R.string.notification_dismissed), activity.getString(R.string.undo), v -> {
                        thread.standalone(() -> {
                            if (!storage.delete(activity, Storage.PERMANENT, Storage.GLOBAL, "firebase#remote_message#menu")) {
                                return;
                            }
                            thread.runOnUI(() -> {
                                if (messageView != null && view != null) {
                                    messageView.addView(view);
                                    if (messageSeparator != null) {
                                        messageSeparator.setVisibility(View.VISIBLE);
                                    }
                                }
                            });
                        });
                    });
                });
                if (layout != null && messageView != null) {
                    messageView.removeAllViews();
                    messageView.addView(layout);
                    if (messageSeparator != null) {
                        messageSeparator.setVisibility(View.VISIBLE);
                    }
                }
            });
        });
    }

    @Override
    public void hideIfUnauthorizedMode(NavigationView navigationView) {
        thread.assertUI();
        if (navigationView == null) {
            return;
        }
        try {
            Menu menu = navigationView.getMenu();
            if (menu.findItem(R.id.nav_e_register).isVisible()) {
                if (App.UNAUTHORIZED_MODE) {
                    menu.findItem(R.id.nav_e_register).setVisible(false);
                    menu.findItem(R.id.nav_protocol_changes).setVisible(false);
                    menu.findItem(R.id.nav_room101).setVisible(false);
                    menu.findItem(R.id.nav_do_clean_auth).setVisible(false);
                    menu.findItem(R.id.nav_logout).setVisible(false);
                }
            } else {
                if (!App.UNAUTHORIZED_MODE) {
                    menu.findItem(R.id.nav_e_register).setVisible(true);
                    menu.findItem(R.id.nav_protocol_changes).setVisible(true);
                    menu.findItem(R.id.nav_room101).setVisible(true);
                    menu.findItem(R.id.nav_do_clean_auth).setVisible(true);
                    menu.findItem(R.id.nav_logout).setVisible(true);
                }
            }
        } catch (Exception ignore) {}
    }

    private List<String> getGroups(Context context, Storage storage) {
        String g = storage.get(context, Storage.PERMANENT, Storage.USER, "user#group").trim();
        String[] gs = storage.get(context, Storage.PERMANENT, Storage.USER, "user#groups").split(",");
        List<String> groups = new ArrayList<>();
        if (!g.isEmpty()) {
            groups.add(g);
        }
        for (String g1 : gs) {
            g1 = g1.trim();
            if (g1.equals(g)) {
                continue;
            }
            groups.add(g1);
        }
        return groups;
    }

    private void displayUserData(NavigationView navigationView, int id, String text) {
        if (navigationView == null) {
            return;
        }
        View activityMainNavHeader = navigationView.getHeaderView(0);
        if (activityMainNavHeader == null) {
            return;
        }
        TextView textView = activityMainNavHeader.findViewById(id);
        if (textView == null) {
            return;
        }
        if (!text.isEmpty()) {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }
    }

    private void displayUserDataExpand(Context context, Storage storage, NavigationView navigationView, List<String> groups) {
        if (navigationView == null) {
            return;
        }
        View activityMainNavHeader = navigationView.getHeaderView(0);
        if (activityMainNavHeader == null) {
            return;
        }
        ViewGroup userInfoExpand = activityMainNavHeader.findViewById(R.id.user_info_expand);
        if (userInfoExpand == null) {
            return;
        }
        if (groups.size() < 2) {
            userInfoExpand.setVisibility(View.GONE);
            return;
        }
        userInfoExpand.setOnClickListener(v -> {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.spinner_center);
            adapter.addAll(groups);
            new AlertDialog.Builder(context)
                    .setTitle(R.string.set_main_group)
                    .setAdapter(adapter, (dialogInterface, position) -> thread.standalone(() -> {
                        storage.put(context, Storage.PERMANENT, Storage.USER, "user#group", groups.get(position));
                        displayUserData(context, storage, navigationView);
                    }))
                    .setNegativeButton(R.string.do_cancel, null)
                    .create().show();
        });
        userInfoExpand.setVisibility(View.VISIBLE);
    }
}
