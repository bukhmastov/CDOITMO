package com.bukhmastov.cdoitmo.parse;

import com.bukhmastov.cdoitmo.data.User;
import com.bukhmastov.cdoitmo.data.Week;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;

import org.htmlcleaner.TagNode;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserDataParse extends Parse<UserDataParse.ProfileData> {
    public class ProfileData {
        public final User user;
        public final Week week;

        ProfileData(User user, Week week) {
            this.user = user;
            this.week = week;
        }
    }

    public UserDataParse(String data, Response<ProfileData> delegate) {
        super(data, delegate);
    }

    @Override
    protected ProfileData parse(TagNode root) {
        // находим имя пользователя
        TagNode fio = root.findElementByAttValue("id", "fio", true, false);
        String name = fio == null ? "" : fio.getText().toString().trim();
        // находим адрес аватарки
        String avatar = "";
        TagNode alink_Avatar = root.findElementByAttValue("class", "alink_Avatar", true, false);
        if (alink_Avatar != null && alink_Avatar.hasChildren()) {
            TagNode img = alink_Avatar.findElementByName("img", false);
            if (img != null) {
                avatar = "servlet/" + img.getAttributeByName("src").trim().replace("&amp;", "&");
            }
        }
        // находим группу пользователя
        String group = "";
        TagNode editForm = root.findElementByAttValue("name", "editForm", true, false);
        if (editForm != null) {
            TagNode div = editForm.findElementByAttValue("class", "d_text", false, false);
            if (div != null) {
                TagNode table = div.findElementByAttValue("class", "d_table", false, false);
                if (table != null) {
                    List<? extends TagNode> rows = table.findElementByName("tbody", false).getAllElementsList(false);
                    if (rows != null) {
                        for (TagNode row : rows) {
                            List<? extends TagNode> columns = row.getAllElementsList(false);
                            if (columns != null && "группа".equals(columns.get(0).getText().toString().toLowerCase().trim())) {
                                group = columns.get(1).getText().toString().trim();
                                break;
                            }
                        }
                    }
                }
            }
        }
        // находим номер текущей недели
        String week = "";
        TagNode divCalendarIcon = root.findElementByAttValue("id", "divCalendarIcon", true, false);
        if (divCalendarIcon != null) {
            Matcher m = Pattern.compile("^.*\\((.*) нед\\).*$").matcher(divCalendarIcon.getText().toString().trim());
            if (m.find()) {
                week = m.group(1);
            }
        }
        return new ProfileData(new User(name, avatar, group, null), new Week(week));
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Parse.USER_DATA;
    }
}
