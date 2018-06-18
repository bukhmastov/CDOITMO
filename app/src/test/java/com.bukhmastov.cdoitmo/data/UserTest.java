package com.bukhmastov.cdoitmo.data;

import com.bukhmastov.cdoitmo.MockStorageProxy;
import com.bukhmastov.cdoitmo.util.Storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

class UserTest {
    @Test
    void testStorage() {
        MockStorageProxy storage = new MockStorageProxy();

        new User("FN LN", "avatarUrl", "G1000,G2000", null).store(storage);

        User stored = User.load(storage);
        assertEquals("FN LN", stored.getName());
        assertEquals("avatarUrl", stored.getAvatar());
        assertEquals("G1000, G2000", stored.getGroup());
        assertEquals(Arrays.asList("G1000", "G2000"), stored.getGroups());
    }

    @Test
    void testGroupOverride() {
        MockStorageProxy storage = new MockStorageProxy();
        storage.put(Storage.StorageType.PREFS, "pref_group_force_override", "G3000");

        new User("FN LN", "avatarUrl", "G1000,G2000", null).store(storage);

        User stored = User.load(storage);
        assertEquals("G3000", stored.getGroup());
        assertEquals(Arrays.asList("G3000"), stored.getGroups());
    }
}
