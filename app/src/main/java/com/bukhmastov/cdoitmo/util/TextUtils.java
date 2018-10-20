package com.bukhmastov.cdoitmo.util;

import android.content.Context;

import java.text.ParseException;
import java.util.Locale;

public interface TextUtils {

    Locale getLocale(Context context, StoragePref storagePref);

    String capitalizeFirstLetter(String text);

    String escapeString(String text);

    String cuteDate(Context context, StoragePref storagePref, String date_format, String date_string) throws ParseException;

    String cuteDate(Context context, StoragePref storagePref, String date_format, String date_start, String date_end) throws ParseException;

    String cuteDateWithoutTime(Context context, StoragePref storagePref, String date_format, String date_string) throws ParseException;

    String ldgZero(int number);

    String prettifyGroupNumber(String group);

    String getRandomString(int length);

    String bytes2readable(Context context, StoragePref storagePref, long bytes);

    String crypt(String value);

    String crypt(String value, String algorithm);
}
