package com.bukhmastov.cdoitmo.data;

import com.bukhmastov.cdoitmo.MockStorageProxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

class AccountListTest {
    @Test
    void testAddRemove() throws JSONException {
        MockStorageProxy storage = new MockStorageProxy();

        AccountList accounts = new AccountList(storage);
        accounts.add("login1");
        accounts.add("login2");
        assertEquals(2, accounts.length());

        accounts = new AccountList(storage);
        assertEquals(2, accounts.length());

        JSONArray logins = accounts.get();
        assertEquals("login2", logins.getString(0));
        assertEquals("login1", logins.getString(1));

        accounts.remove("login2");
        assertEquals(1, accounts.length());

        accounts = new AccountList(storage);
        assertEquals(1, accounts.length());

        logins = accounts.get();
        assertEquals("login1", logins.getString(0));
    }

    @Test
    void testIterator() {
        MockStorageProxy storage = new MockStorageProxy();

        AccountList accounts = new AccountList(storage);
        accounts.add("login1");
        accounts.add("login2");

        UserCredentials.setCurrentLogin(storage, "login2");
        new UserCredentials("login2", "pass2", "role2").store(storage);
        new User("name2", "", "", null).store(storage);

        UserCredentials.setCurrentLogin(storage, "login1");
        new UserCredentials("login1", "pass1", "role1").store(storage);
        new User("name1", "", "", null).store(storage);

        Iterator<AccountList.AccountEntry> iter = new AccountList(storage).iterator();
        assertTrue(iter.hasNext());
        AccountList.AccountEntry entry = iter.next();
        assertEquals("name2", entry.name);
        assertEquals("login2", entry.creds.getLogin());
        assertEquals("pass2", entry.creds.getPassword());
        assertEquals("role2", entry.creds.getRole());
        assertTrue(iter.hasNext());
        entry = iter.next();
        assertEquals("name1", entry.name);
        assertEquals("login1", entry.creds.getLogin());
        assertEquals("pass1", entry.creds.getPassword());
        assertEquals("role1", entry.creds.getRole());
        assertFalse(iter.hasNext());
    }
}
