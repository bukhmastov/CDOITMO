package com.bukhmastov.cdoitmo.data;

import android.text.TextUtils;

import static com.bukhmastov.cdoitmo.util.Static.contains;
import static com.bukhmastov.cdoitmo.util.Storage.StorageType.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class User {
    private final String name;
    private final String avatar;
    private final String group;
    private final List<String> groups;

    public User(String name, String avatar, String group, List<String> groups) {
        this.name = name;
        this.avatar = avatar;
        this.group = group;
        this.groups = groups != null ? groups : Arrays.asList(group);
    }

    public String getName() { return name; }

    public String getAvatar() { return avatar; }

    public String getGroup() { return group; }

    public List<String> getGroups() { return groups; }

    public void store(StorageProxy proxy) {
        proxy.put(PER_USER,"user#name", name);
        proxy.put(PER_USER, "user#avatar", avatar);

        final String groupOverride = proxy.get(PREFS, "pref_group_force_override").trim();
        final String[] groups = (groupOverride.isEmpty() ? group : groupOverride).split(",\\s|\\s|,");
        final String currentGroup = proxy.get(PER_USER, "user#group");

        if (!contains(groups, currentGroup)) {
            proxy.put(PER_USER,"user#group", groups[0]);
        }
        proxy.put(PER_USER,"user#groups", TextUtils.join(", ", groups));
    }

    public static String getName(StorageProxy proxy) {
        return proxy.get(PER_USER, "user#name");
    }

    public static User load(StorageProxy proxy) {
        final List<String> groups = loadGroups(proxy);
        final String group = TextUtils.join(", ", groups);
        final String name = proxy.get(PER_USER, "user#name");
        final String avatar = proxy.get(PER_USER, "user#avatar");

        return new User(name, avatar, group, groups);
    }

    private static List<String> loadGroups(StorageProxy proxy) {
        final String currentGroup = proxy.get(PER_USER, "user#group").trim();
        final String[] storedGroups = proxy.get(PER_USER, "user#groups").split(",");

        List<String> groups = new ArrayList<>();
        if (!currentGroup.isEmpty()) {
            groups.add(currentGroup);
        }
        for (String group : storedGroups) {
            group = group.trim();
            if (group.equals(currentGroup)) continue;
            groups.add(group);
        }
        return groups;
    }
}
