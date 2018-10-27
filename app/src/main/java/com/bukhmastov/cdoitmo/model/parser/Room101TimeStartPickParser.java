package com.bukhmastov.cdoitmo.model.parser;

import androidx.annotation.NonNull;

import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.room101.request.ROption;
import com.bukhmastov.cdoitmo.model.room101.request.Room101Request;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import org.htmlcleaner.TagNode;

import java.util.ArrayList;

public class Room101TimeStartPickParser extends Parser<Room101Request> {

    public Room101TimeStartPickParser(@NonNull String html) {
        super(html);
    }

    @Override
    protected Room101Request doParse(TagNode root) throws Throwable {
        Room101Request room101Request = new Room101Request();
        room101Request.setType("time_start_pick");
        room101Request.setOptions(new ArrayList<>());
        TagNode[] tables = root.getElementsByAttValue("class", "d_table min_lmargin_table", true, false);
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
            if (tr == null || index == 1) {
                continue;
            }
            TagNode td = tr.getElementsByName("td", false)[0];
            if (td == null) {
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
            String time = input.getAttributeByName("value");
            if (StringUtils.isBlank(time)) {
                continue;
            }
            ROption option = new ROption();
            option.setTime(time);
            try {
                option.setAvailable(tr.getElementsByName("td", false)[2].getText().toString().trim());
            } catch (Exception e) {
                option.setAvailable("");
            }
            room101Request.getOptions().add(option);
        }
        return room101Request;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Parse.ROOM_101_TIME_START_PICK;
    }
}
