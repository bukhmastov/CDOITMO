package com.bukhmastov.cdoitmo.parse;

import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RatingTopListParse implements Runnable {

    private static final String TAG = "RatingTopListParse";
    public interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    private String data;
    private String username;

    public RatingTopListParse(String data, String username, response delegate) {
        this.data = data;
        this.username = username;
        this.delegate = delegate;
    }

    @Override
    public void run() {
        Log.v(TAG, "parsing");
        try {
            String response = data.replace("&nbsp;", " ");
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(response);
            JSONObject json = new JSONObject();
            JSONArray list = new JSONArray();
            TagNode div = root.findElementByAttValue("class", "c-page", true, false).findElementByAttValue("class", "p-inner nobt", false, false);
            String header = "";
            Matcher m;
            m = Pattern.compile("Рейтинг студентов (.*)").matcher(div.findElementByAttValue("class", "notop", false, false).getText().toString().trim());
            if(m.find()) header = m.group(1).trim();
            m = Pattern.compile("^(.*) учебный год, (.*)$").matcher(div.findElementByAttValue("class", "info", false, false).getText().toString().trim());
            if(m.find()){
                if(!Objects.equals(header, "")) header += " - ";
                header += m.group(2) + " (" + m.group(1).replace(" ", "") + ")";
            }
            json.put("header", header);
            TagNode[] trs = div.findElementByAttValue("class", "table-rating", false, false).getElementsByName("tbody", false)[0].getElementsByName("tr", false);
            int counter = 0;
            for(TagNode tr : trs){
                if(counter++ == 0) continue;
                TagNode[] tds = tr.getElementsByName("td", false);
                if(tds == null || tds.length == 0) continue;
                JSONObject user = new JSONObject();
                user.put("number", Integer.parseInt(tds[0].getText().toString().trim()));
                String fio = tds[1].getText().toString().trim();
                user.put("fio", fio);
                String meta = tds[3].getText().toString().trim();
                m = Pattern.compile("гр. (.*), каф. (.*)").matcher(meta);
                if(m.find()){
                    user.put("group", m.group(1).trim());
                    user.put("department", m.group(2).trim());
                } else {
                    user.put("group", "");
                    user.put("department", "");
                }
                user.put("is_me", Objects.equals(fio, username));
                TagNode[] is = tds[2].getAllElements(false);
                if(is != null && is.length > 0){
                    m = Pattern.compile("^icon-expand_.* (.*)$").matcher(is[0].getAttributeByName("class"));
                    if(m.find()){
                        user.put("change", m.group(1).trim());
                    } else {
                        user.put("change", "none");
                    }
                    user.put("delta", is[0].getAttributeByName("title"));
                } else {
                    user.put("change", "none");
                    user.put("delta", "0");
                }
                list.put(user);
            }
            json.put("list", list);
            delegate.finish(json);
        } catch (Exception e) {
            Static.error(e);
            delegate.finish(null);
        }
    }
}
