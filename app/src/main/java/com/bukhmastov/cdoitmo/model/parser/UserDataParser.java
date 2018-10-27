package com.bukhmastov.cdoitmo.model.parser;

import androidx.annotation.NonNull;

import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.user.UserData;

import org.htmlcleaner.TagNode;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserDataParser extends Parser<UserData> {

    public UserDataParser(@NonNull String html) {
        super(html);
    }

    @Override
    protected UserData doParse(TagNode root) throws Throwable {
        UserData userData = new UserData();
        userData.setName("");
        userData.setAvatar("");
        userData.setGroup("");
        userData.setWeek(-1);
        // находим имя пользователя
        TagNode fio = root.findElementByAttValue("id", "fio", true, false);
        userData.setName(fio == null ? "" : fio.getText().toString().trim());
        // находим адрес аватарки
        TagNode alink_Avatar = root.findElementByAttValue("class", "alink_Avatar", true, false);
        if (alink_Avatar != null && alink_Avatar.hasChildren()) {
            TagNode img = alink_Avatar.findElementByName("img", false);
            if (img != null) {
                userData.setAvatar("servlet/" + img.getAttributeByName("src").trim().replace("&amp;", "&"));
            }
        }
        // находим группу пользователя
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
                                userData.setGroup(columns.get(1).getText().toString().trim());
                                break;
                            }
                        }
                    }
                }
            }
        }
        // находим номер текущей недели
        TagNode divCalendarIcon = root.findElementByAttValue("id", "divCalendarIcon", true, false);
        if (divCalendarIcon != null) {
            Matcher m = Pattern.compile("^.*\\((.*) нед\\).*$").matcher(divCalendarIcon.getText().toString().trim());
            if (m.find()) {
                try {
                    userData.setWeek(Integer.parseInt(m.group(1)));
                } catch (NumberFormatException ignore) {
                    // ignore
                }
            }
        }
        return userData;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Parse.USER_DATA;
    }
}
