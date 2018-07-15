package com.bukhmastov.cdoitmo.util;

import android.app.Activity;
import android.content.Context;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseConfigProvider;
import com.bukhmastov.cdoitmo.view.Message;

import java.util.ArrayList;
import java.util.List;

public class NavigationMenu {

    public static void displayEnableDisableOfflineButton(final NavigationView navigationView) {
        Thread.runOnUI(() -> {
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

    public static void displayUserData(final Context context, final Storage storage, final NavigationView navigationView) {
        Thread.run(() -> {
            final String name = storage.get(context, Storage.PERMANENT, Storage.USER, "user#name");
            final List<String> groups = getGroups(context, storage);
            final String group = TextUtils.join(", ", groups);
            Thread.runOnUI(() -> {
                displayUserData(navigationView, R.id.user_name, name);
                displayUserData(navigationView, R.id.user_group, group);
                displayUserDataExpand(context, storage, navigationView, groups);
            });
        });
    }

    private static List<String> getGroups(final Context context, final Storage storage) {
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

    private static void displayUserData(final NavigationView navigationView, final int id, final String text) {
        Thread.runOnUI(() -> {
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

    private static void displayUserDataExpand(final Context context, final Storage storage, final NavigationView navigationView, final List<String> groups) {
        Thread.runOnUI(() -> {
            if (navigationView == null) return;
            View activity_main_nav_header = navigationView.getHeaderView(0);
            if (activity_main_nav_header == null) return;
            ViewGroup user_info_expand = activity_main_nav_header.findViewById(R.id.user_info_expand);
            if (user_info_expand != null) {
                if (groups.size() > 1) {
                    user_info_expand.setOnClickListener(v -> Thread.runOnUI(() -> {
                        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, R.layout.spinner_center);
                        arrayAdapter.addAll(groups);
                        new AlertDialog.Builder(context)
                                .setTitle(R.string.set_main_group)
                                .setAdapter(arrayAdapter, (dialogInterface, position) -> Thread.run(() -> {
                                    try {
                                        storage.put(context, Storage.PERMANENT, Storage.USER, "user#group", groups.get(position));
                                        Thread.runOnUI(() -> displayUserData(context, storage, navigationView));
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

    public static void toggleOfflineIcon(final Menu menu) {
        Thread.runOnUI(() -> {
            if (menu != null) {
                MenuItem menuItem = menu.findItem(R.id.offline_mode);
                if (menuItem != null) {
                    menuItem.setVisible(App.OFFLINE_MODE);
                }
            }
        });
    }

    public static void displayRemoteMessage(final Activity activity, final FirebaseConfigProvider firebaseConfigProvider, final Storage storage) {
        Thread.run(() -> firebaseConfigProvider.getJson(FirebaseConfigProvider.MESSAGE_MENU, value -> Thread.run(() -> {
            try {
                if (value == null) return;
                final int type = value.getInt("type");
                final String message = value.getString("message");
                if (message == null || message.trim().isEmpty()) return;
                final String hash = com.bukhmastov.cdoitmo.util.TextUtils.crypt(message);
                if (hash != null && hash.equals(storage.get(activity, Storage.PERMANENT, Storage.GLOBAL, "firebase#remote_message#menu", ""))) {
                    return;
                }
                Thread.runOnUI(() -> {
                    final ViewGroup message_menu = activity.findViewById(R.id.message_menu);
                    final View message_menu_separator = activity.findViewById(R.id.message_menu_separator);
                    final View layout = Message.getRemoteMessage(activity, type, message, (context, view) -> {
                        if (hash != null) {
                            if (storage.put(activity, Storage.PERMANENT, Storage.GLOBAL, "firebase#remote_message#menu", hash)) {
                                if (message_menu != null && view != null) {
                                    message_menu.removeView(view);
                                    if (message_menu_separator != null) {
                                        message_menu_separator.setVisibility(View.GONE);
                                    }
                                }
                                BottomBar.snackBar(activity, activity.getString(R.string.notification_dismissed), activity.getString(R.string.undo), v -> Thread.run(() -> {
                                    if (storage.delete(activity, Storage.PERMANENT, Storage.GLOBAL, "firebase#remote_message#menu")) {
                                        Thread.runOnUI(() -> {
                                            if (message_menu != null && view != null) {
                                                message_menu.addView(view);
                                                if (message_menu_separator != null) {
                                                    message_menu_separator.setVisibility(View.VISIBLE);
                                                }
                                            }
                                        });
                                    }
                                }));
                            }
                        }
                    });
                    if (layout != null && message_menu != null) {
                        message_menu.removeAllViews();
                        message_menu.addView(layout);
                        if (message_menu_separator != null) {
                            message_menu_separator.setVisibility(View.VISIBLE);
                        }
                    }
                });
            } catch (Exception ignore) {
                // ignore
            }
        })));
    }

    public static void hideIfUnauthorizedMode(final NavigationView navigationView) {
        Thread.runOnUI(() -> {
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
}
