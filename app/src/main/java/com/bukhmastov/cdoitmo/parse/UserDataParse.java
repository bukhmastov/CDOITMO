package com.bukhmastov.cdoitmo.parse;

import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;

import org.htmlcleaner.TagNode;
import org.json.JSONObject;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserDataParse extends Parse {

    public UserDataParse(String data, Response delegate) {
        super(data, delegate);
    }

    @Override
    protected JSONObject parse(TagNode root) throws Throwable {
        JSONObject response = new JSONObject();
        // находим имя пользователя
        TagNode fio = root.findElementByAttValue("id", "fio", true, false);
        response.put("name", fio == null ? "" : fio.getText().toString().trim());
        // находим адрес аватарки
        response.put("avatar", "");
        TagNode alink_Avatar = root.findElementByAttValue("class", "alink_Avatar", true, false);
        if (alink_Avatar != null && alink_Avatar.hasChildren()) {
            TagNode img = alink_Avatar.findElementByName("img", false);
            if (img != null) {
                response.put("avatar", "servlet/" + img.getAttributeByName("src").trim().replace("&amp;", "&"));
            }
        }
        // находим группу пользователя
        response.put("group", "");
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
                                response.put("group", columns.get(1).getText().toString().trim());
                                break;
                            }
                        }
                    }
                }
            }
        }
        // находим номер текущей недели
        response.put("week", "-1");
        TagNode divCalendarIcon = root.findElementByAttValue("id", "divCalendarIcon", true, false);
        if (divCalendarIcon != null) {
            Matcher m = Pattern.compile("^.*\\((.*) нед\\).*$").matcher(divCalendarIcon.getText().toString().trim());
            if (m.find()) {
                response.put("week", m.group(1));
            }
        }
        return response;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Parse.USER_DATA;
    }
}
