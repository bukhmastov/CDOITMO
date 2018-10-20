package com.bukhmastov.cdoitmo.model.parser;

import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.exception.SilentException;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.rating.pickerall.RFaculty;
import com.bukhmastov.cdoitmo.model.rating.pickerall.RatingPickerAll;

import org.htmlcleaner.TagNode;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RatingPickerAllParser extends Parser<RatingPickerAll> {

    private static final Pattern patternFaculty = Pattern.compile("^(.*) \\((.{1,10})\\)$");

    public RatingPickerAllParser(@NonNull String html) {
        super(html);
    }

    @Override
    protected RatingPickerAll doParse(TagNode root) throws Throwable {
        TagNode page = root.findElementByAttValue("class", "c-page", true, false);
        if (page == null) {
            throw new SilentException();
        }
        TagNode div = page.findElementByAttValue("class", "p-inner nobt", false, false);
        if (div == null) {
            throw new SilentException();
        }
        RatingPickerAll rating = new RatingPickerAll();
        rating.setFaculties(new ArrayList<>());
        TagNode[] spans = div.getElementsByName("span", false);
        if (spans != null) {
            for (TagNode span : spans) {
                if (span == null) {
                    continue;
                }
                RFaculty faculty = new RFaculty();
                String name = span.getText().toString().trim();
                Matcher matcher = patternFaculty.matcher(name);
                if (matcher.find()) {
                    name = matcher.group(2) + " (" + matcher.group(1) + ")";
                }
                faculty.setName(name);
                rating.getFaculties().add(faculty);
            }
        }
        TagNode[] links = div.getElementsByAttValue("class", "big-links left", false, false);
        if (links != null) {
            for (int i = 0; i < rating.getFaculties().size(); i++) {
                TagNode link = links[i];
                if (link == null) {
                    continue;
                }
                TagNode[] as = link.getElementsByName("a", false);
                if (as == null || as.length == 0) {
                    continue;
                }
                String[] attrs = as[0].getAttributeByName("href").replace("&amp;", "&").split("&");
                for (String attr : attrs) {
                    String[] pair = attr.split("=");
                    if ("depId".equals(pair[0])) {
                        rating.getFaculties().get(i).setDepId(pair[1]);
                    }
                }
            }
        }
        return rating;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Parse.RATING_LIST;
    }
}
