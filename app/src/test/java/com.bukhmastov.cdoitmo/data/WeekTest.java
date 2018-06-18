package com.bukhmastov.cdoitmo.data;

import com.bukhmastov.cdoitmo.MockStorageProxy;
import com.bukhmastov.cdoitmo.util.Storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.Calendar;

class WeekTest {
    @Test
    void testStorage() {
        MockStorageProxy storage = new MockStorageProxy();

        Calendar cal = Calendar.getInstance();
        cal.set(2017, 9, 6);
        long fetchedOn = cal.getTimeInMillis();
        new Week("2", fetchedOn).store(storage);

        cal.set(2017, 9, 21);
        assertEquals(4, Week.getCurrent(storage, cal));
    }

    @Test
    void testWeekOverride() {
        MockStorageProxy storage = new MockStorageProxy();

        Calendar cal = Calendar.getInstance();
        cal.set(2017, 9, 6);
        long overriddenOn = cal.getTimeInMillis();
        String overridenAs = "1";

        storage.put(Storage.StorageType.PREFS, "pref_week_force_override",
                overridenAs + "#" + Long.toString(overriddenOn));

        new Week("2", overriddenOn).store(storage);

        cal.set(2017, 9, 21);
        assertEquals(3, Week.getCurrent(storage, cal));
    }
}
