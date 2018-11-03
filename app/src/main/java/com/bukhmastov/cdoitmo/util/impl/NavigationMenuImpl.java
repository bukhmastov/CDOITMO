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
import com.bukhmastov.cdoitmo.util.TextUtils;
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
    @Inject
    Lazy<TextUtils> textUtils;

    public NavigationMenuImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void displayEnableDisableOfflineButton(final NavigationView navigationView) {
        thread.runOnUI(() -> {
            if (navigationView != null) {
                try {
                    Menu menu = navigationView.getMenu();
                    MenuItem nav_enable_offline_mode = menu.findItem(R.id.nav_enable_offline_mode);
                    MenuItem nav_disable_offline_mode = menu.findItem(R.id.nav_disable_offline_mode);
                    if (App.OFFLINE_MODE) {
                        nav_enable_offline_mode.setVisible(false);
                        nav_disable_offline_mode.setVisible(true);
                    } else {
                        nav_enable_offline_mode.setVisible(true);
                        nav_disable_offline_mode.setVisible(false);
                    }
                } catch (Exception ignore) {}
            }
        });
    }

    @Override
    public void displayUserData(final Context context, final Storage storage, final NavigationView navigationView) {
        thread.run(() -> {
            final String name = storage.get(context, Storage.PERMANENT, Storage.USER, "user#name");
            final List<String> groups = getGroups(context, storage);
            final String group = android.text.TextUtils.join(", ", groups);
            thread.runOnUI(() -> {
                displayUserData(navigationView, R.id.user_name, name);
                displayUserData(navigationView, R.id.user_group, group);
                displayUserDataExpand(context, storage, navigationView, groups);
            });
        });
    }

    @Override
    public void toggleOfflineIcon(final Menu menu) {
        thread.runOnUI(() -> {
            if (menu != null) {
                MenuItem menuItem = menu.findItem(R.id.offline_mode);
                if (menuItem != null) {
                    menuItem.setVisible(App.OFFLINE_MODE);
                }
            }
        });
    }

    @Override
    public void displayRemoteMessage(final Activity activity, final FirebaseConfigProvider firebaseConfigProvider, final Storage storage) {
        thread.run(() -> firebaseConfigProvider.getMessage(FirebaseConfigProvider.MESSAGE_MENU, value -> {
            thread.run(() -> {
                if (value == null) {
                    return;
                }
                int type = value.getType();
                String message = value.getMessage();
                if (StringUtils.isBlank(message)) {
                    return;
                }
                String hash = textUtils.get().crypt(message);
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
                        notificationMessage.get().snackBar(activity, activity.getString(R.string.notification_dismissed), activity.getString(R.string.undo), v -> thread.run(() -> {
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
                        }));
                    });
                    if (layout != null && messageView != null) {
                        messageView.removeAllViews();
                        messageView.addView(layout);
                        if (messageSeparator != null) {
                            messageSeparator.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }, throwable -> {});
        }));
    }

    @Override
    public void hideIfUnauthorizedMode(final NavigationView navigationView) {
        thread.runOnUI(() -> {
            try {
                if (navigationView != null) {
                    final Menu menu = navigationView.getMenu();
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
                }
            } catch (Exception ignore) {}
        });
    }

    private List<String> getGroups(final Context context, final Storage storage) {
        final String g = storage.get(context, Storage.PERMANENT, Storage.USER, "user#group").trim();
        final String[] gs = storage.get(context, Storage.PERMANENT, Storage.USER, "user#groups").split(",");
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

    private void displayUserData(final NavigationView navigationView, final int id, final String text) {
        thread.runOnUI(() -> {
            if (navigationView == null) return;
            View activity_main_nav_header = navigationView.getHeaderView(0);
            if (activity_main_nav_header == null) return;
            TextView textView = activity_main_nav_header.findViewById(id);
            if (textView != null) {
                if (!text.isEmpty()) {
                    textView.setText(text);
                    textView.setVisibility(View.VISIBLE);
                } else {
                    textView.setVisibility(View.GONE);
                }
            }
        });
    }

    private void displayUserDataExpand(final Context context, final Storage storage, final NavigationView navigationView, final List<String> groups) {
        thread.runOnUI(() -> {
            if (navigationView == null) return;
            View activity_main_nav_header = navigationView.getHeaderView(0);
            if (activity_main_nav_header == null) return;
            ViewGroup user_info_expand = activity_main_nav_header.findViewById(R.id.user_info_expand);
            if (user_info_expand != null) {
                if (groups.size() > 1) {
                    user_info_expand.setOnClickListener(v -> thread.runOnUI(() -> {
                        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, R.layout.spinner_center);
                        arrayAdapter.addAll(groups);
                        new AlertDialog.Builder(context)
                                .setTitle(R.string.set_main_group)
                                .setAdapter(arrayAdapter, (dialogInterface, position) -> thread.run(() -> {
                                    try {
                                        storage.put(context, Storage.PERMANENT, Storage.USER, "user#group", groups.get(position));
                                        thread.runOnUI(() -> displayUserData(context, storage, navigationView));
                                    } catch (Exception ignore) {}
                                }))
                                .setNegativeButton(R.string.do_cancel, null)
                                .create().show();
                    }));
                    user_info_expand.setVisibility(View.VISIBLE);
                } else {
                    user_info_expand.setVisibility(View.GONE);
                }
            }
        });
    }
}
