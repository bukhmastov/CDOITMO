package com.bukhmastov.cdoitmo.parse;

import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserDataParse implements Runnable {

    private static final String TAG = "UserDataParse";
    public interface response {
        void finish(HashMap<String, String> result);
    }
    private response delegate = null;
    private String data;

    public UserDataParse(String data, response delegate) {
        this.data = data;
        this.delegate = delegate;
    }

    @Override
    public void run() {
        Log.v(TAG, "parsing");
        try {
            HashMap<String, String> response = new HashMap<>();
            TagNode root = new HtmlCleaner().clean(data.replace("&nbsp;", " "));
            // находим имя пользователя
            TagNode fio = root.findElementByAttValue("id", "fio", true, false);
            response.put("name", fio.getText().toString().trim());
            // находим адрес аватарки
            response.put("avatar", "");
            TagNode alink_Avatar = root.findElementByAttValue("class", "alink_Avatar", true, false);
            if (alink_Avatar.hasChildren()) {
                TagNode img = alink_Avatar.findElementByName("img", false);
                if (img != null) {
                    response.put("avatar", "servlet/" + img.getAttributeByName("src").trim().replace("&amp;", "&"));
                }
            }
            // находим группу пользователя
            response.put("group", "");
            TagNode editForm = root.findElementByAttValue("name", "editForm", true, false);
            TagNode div = editForm.findElementByAttValue("class", "d_text", false, false);
            TagNode table = div.findElementByAttValue("class", "d_table", false, false);
            List<? extends TagNode> rows = table.findElementByName("tbody", false).getAllElementsList(false);
            for (TagNode row : rows) {
                List<? extends TagNode> columns = row.getAllElementsList(false);
                if (Objects.equals(columns.get(0).getText().toString().trim(), "Группа")) {
                    response.put("group", columns.get(1).getText().toString().trim());
                    break;
                }
            }
            // находим номер текущей недели
            response.put("week", "-1");
            TagNode divCalendarIcon = root.findElementByAttValue("id", "divCalendarIcon", true, false);
            Matcher m = Pattern.compile("^.*\\((.*) нед\\).*$").matcher(divCalendarIcon.getText().toString().trim());
            if (m.find()) {
                response.put("week", m.group(1));
            }
            delegate.finish(response);
        } catch (Exception e){
            Static.error(e);
            delegate.finish(null);
        }
    }
}
