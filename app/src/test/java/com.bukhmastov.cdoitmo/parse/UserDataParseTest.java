package com.bukhmastov.cdoitmo.parse;

import com.bukhmastov.cdoitmo.TestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UserDataParseTest {
    @Test
    void testProfileFetch() {
        String page = TestHelper.readResource("parse/editPersonProfile.html");
        new UserDataParse(page, response -> {
            assertEquals("Фамилия Имя Отчество", response.user.getName());
            assertEquals("servlet/distributedCDE?Rule=GETATTACH&ATT_ID=AVATAR_ID", response.user.getAvatar());
            assertEquals("G0000", response.user.getGroup());
            assertEquals(19, response.week.getWeek());
        }).run();
    }
}