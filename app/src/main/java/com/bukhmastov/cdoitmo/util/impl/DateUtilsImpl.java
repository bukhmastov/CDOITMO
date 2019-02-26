package com.bukhmastov.cdoitmo.util.impl;

import android.content.Context;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.DateUtils;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.inject.Inject;

public class DateUtilsImpl implements DateUtils {

    @Inject
    Time time;
    @Inject
    StoragePref storagePref;

    public DateUtilsImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public String cuteDate(Context context, String dateFormat, String dateString) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat(dateFormat, StringUtils.getLocale(context, storagePref));
        Calendar date = time.getCalendar();
        date.setTime(format.parse(dateString));
        return (new StringBuilder())
                .append(date.get(Calendar.DATE))
                .append(" ")
                .append(time.getGenitiveMonth(context, date.get(Calendar.MONTH)))
                .append(" ")
                .append(date.get(Calendar.YEAR))
                .append(" ")
                .append(StringUtils.ldgZero(date.get(Calendar.HOUR_OF_DAY)))
                .append(":")
                .append(StringUtils.ldgZero(date.get(Calendar.MINUTE)))
                .toString();
    }

    @Override
    public String cuteDate(Context context, String dateFormat, String dateStart, String dateEnd) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat(dateFormat, StringUtils.getLocale(context, storagePref));
        Calendar calendarStart = time.getCalendar();
        Calendar calendarEnd = time.getCalendar();
        calendarStart.setTime(format.parse(dateStart));
        calendarEnd.setTime(format.parse(dateEnd));
        boolean diffDays = calendarStart.get(Calendar.DATE) != calendarEnd.get(Calendar.DATE);
        boolean diffMonths = calendarStart.get(Calendar.MONTH) != calendarEnd.get(Calendar.MONTH);
        boolean diffYears = calendarStart.get(Calendar.YEAR) != calendarEnd.get(Calendar.YEAR);
        StringBuilder sb = new StringBuilder();
        if (diffDays || diffMonths || diffYears) {
            sb.append(calendarStart.get(Calendar.DATE));
        }
        if (diffMonths || diffYears) {
            sb.append(" ").append(time.getGenitiveMonth(context, calendarStart.get(Calendar.MONTH)));
        }
        if (diffYears) {
            sb.append(" ").append(calendarStart.get(Calendar.YEAR));
        }
        if (diffDays || diffMonths || diffYears) {
            sb.append(" - ");
        }
        sb.append(calendarEnd.get(Calendar.DATE)).append(" ").append(time.getGenitiveMonth(context, calendarEnd.get(Calendar.MONTH))).append(" ").append(calendarEnd.get(Calendar.YEAR));
        return sb.toString();
    }

    @Override
    public String cuteDateWithoutTime(Context context, String dateFormat, String dateString) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat(dateFormat, StringUtils.getLocale(context, storagePref));
        Calendar date = time.getCalendar();
        date.setTime(format.parse(dateString));
        return (new StringBuilder())
                .append(date.get(Calendar.DATE))
                .append(" ")
                .append(time.getGenitiveMonth(context, date.get(Calendar.MONTH)))
                .append(" ")
                .append(date.get(Calendar.YEAR))
                .toString();
    }
}
