package com.bukhmastov.cdoitmo.util;

import android.content.Context;

import java.text.ParseException;

public interface DateUtils {

    String cuteDate(Context context, String dateFormat, String dateString) throws ParseException;

    String cuteDate(Context context, String dateFormat, String dateStart, String dateEnd) throws ParseException;

    String cuteDateWithoutTime(Context context, String dateFormat, String dateString) throws ParseException;
}
