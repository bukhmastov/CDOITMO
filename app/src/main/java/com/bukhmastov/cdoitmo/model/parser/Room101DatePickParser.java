package com.bukhmastov.cdoitmo.model.parser;

import androidx.annotation.NonNull;

import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.room101.request.ROption;
import com.bukhmastov.cdoitmo.model.room101.request.Room101Request;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import org.htmlcleaner.TagNode;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Room101DatePickParser extends Parser<Room101Request> {

    public Room101DatePickParser(@NonNull String html) {
        super(html);
    }

    @Override
    protected Room101Request doParse(TagNode root) throws Throwable {
        Room101Request room101Request = new Room101Request();
        room101Request.setType("date_pick");
        room101Request.setOptions(new ArrayList<>());
        TagNode[] tables = root.getElementsByAttValue("class", "d_table2 calendar_1", true, false);
        if (tables == null || tables.length == 0) {
            return room101Request;
        }
        TagNode table = tables[0];
        if (table == null) {
            return room101Request;
        }
        TagNode[] trs = table.getElementsByName("tbody", false)[0].getElementsByName("tr", false);
        if (trs == null) {
            return room101Request;
        }
        int index = 0;
        for (TagNode tr : trs) {
            index++;
            if (tr == null || index < 3) {
                continue;
            }
            TagNode[] tds = tr.getElementsByName("td", false);
            if (tds == null) {
                continue;
            }
            for (TagNode td : tds) {
                if (td == null || !td.hasChildren()) {
                    continue;
                }
                TagNode[] inputs = td.getElementsByName("input", false);
                if (inputs == null || inputs.length == 0) {
                    continue;
                }
                TagNode input = inputs[0];
                if (input == null || input.hasAttribute("disabled")) {
                    continue;
                }
                String onclick = input.getAttributeByName("onclick");
                if (StringUtils.isBlank(onclick)) {
                    continue;
                }
                Matcher m = Pattern.compile(".*dateRequest\\.value='(.*)';.*").matcher(onclick);
                if (m.find()) {
                    ROption option = new ROption();
                    option.setTime(m.group(1));
                    option.setAvailable("");
                    room101Request.getOptions().add(option);
                }
            }
        }
        return room101Request;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Parse.ROOM_101_DATE_PICK;
    }
}
