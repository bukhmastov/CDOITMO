package com.bukhmastov.cdoitmo.util.impl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.Transliterate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import dagger.Lazy;

public class TextUtilsImpl implements TextUtils {

    @Inject
    Lazy<Time> time;

    public TextUtilsImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    @SuppressWarnings("deprecation")
    public Locale getLocale(Context context, StoragePref storagePref) {
        Locale locale;
        String lang = storagePref.get(context, "pref_lang", "default");
        switch (lang) {
            case "ru": case "en": {
                locale = new Locale(lang);
                break;
            }
            default: case "default": {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    locale = new Locale(context.getResources().getConfiguration().getLocales().get(0).getCountry());
                } else {
                    locale = new Locale(context.getResources().getConfiguration().locale.getCountry());
                }
                break;
            }
        }
        return locale;
    }

    @Override
    public String capitalizeFirstLetter(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    @Override
    @SuppressWarnings("deprecation")
    public String escapeString(String text) {
        if (text == null) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim();
        } else {
            return android.text.Html.fromHtml(text).toString().trim();
        }
    }

    @Override
    public String cuteDate(Context context, StoragePref storagePref, String date_format, String date_string) throws ParseException {
        SimpleDateFormat format_input = new SimpleDateFormat(date_format, getLocale(context, storagePref));
        Calendar date = time.get().getCalendar();
        date.setTime(format_input.parse(date_string));
        return (new StringBuilder())
                .append(date.get(Calendar.DATE))
                .append(" ")
                .append(time.get().getGenitiveMonth(context, date.get(Calendar.MONTH)))
                .append(" ")
                .append(date.get(Calendar.YEAR))
                .append(" ")
                .append(ldgZero(date.get(Calendar.HOUR_OF_DAY)))
                .append(":")
                .append(ldgZero(date.get(Calendar.MINUTE)))
                .toString();
    }

    @Override
    public String cuteDate(Context context, StoragePref storagePref, String date_format, String date_start, String date_end) throws ParseException {
        SimpleDateFormat format_input = new SimpleDateFormat(date_format, getLocale(context, storagePref));
        Calendar calendar_start = time.get().getCalendar();
        Calendar calendar_end = time.get().getCalendar();
        calendar_start.setTime(format_input.parse(date_start));
        calendar_end.setTime(format_input.parse(date_end));
        boolean diff_days = calendar_start.get(Calendar.DATE) != calendar_end.get(Calendar.DATE);
        boolean diff_months = calendar_start.get(Calendar.MONTH) != calendar_end.get(Calendar.MONTH);
        boolean diff_years = calendar_start.get(Calendar.YEAR) != calendar_end.get(Calendar.YEAR);
        StringBuilder sb = new StringBuilder();
        if (diff_days || diff_months || diff_years) {
            sb.append(calendar_start.get(Calendar.DATE));
        }
        if (diff_months || diff_years) {
            sb.append(" ").append(time.get().getGenitiveMonth(context, calendar_start.get(Calendar.MONTH)));
        }
        if (diff_years) {
            sb.append(" ").append(calendar_start.get(Calendar.YEAR));
        }
        if (diff_days || diff_months || diff_years) {
            sb.append(" - ");
        }
        sb.append(calendar_end.get(Calendar.DATE)).append(" ").append(time.get().getGenitiveMonth(context, calendar_end.get(Calendar.MONTH))).append(" ").append(calendar_end.get(Calendar.YEAR));
        return sb.toString();
    }

    @Override
    public String cuteDateWithoutTime(Context context, StoragePref storagePref, String date_format, String date_string) throws ParseException {
        SimpleDateFormat format_input = new SimpleDateFormat(date_format, getLocale(context, storagePref));
        Calendar date = time.get().getCalendar();
        date.setTime(format_input.parse(date_string));
        return (new StringBuilder())
                .append(date.get(Calendar.DATE))
                .append(" ")
                .append(time.get().getGenitiveMonth(context, date.get(Calendar.MONTH)))
                .append(" ")
                .append(date.get(Calendar.YEAR))
                .toString();
    }

    @Override
    @SuppressLint("DefaultLocale")
    public String ldgZero(int number) {
        return String.format("%02d", number);
    }

    @Override
    public JSONArray string2jsonArray(String text) throws JSONException {
        JSONArray json;
        if (text == null || text.isEmpty()) {
            json = new JSONArray();
        } else {
            json = new JSONArray(text);
        }
        return json;
    }

    @Override
    public JSONObject string2json(String text) throws JSONException {
        JSONObject json;
        if (text == null || text.isEmpty()) {
            json = new JSONObject();
        } else {
            json = new JSONObject(text);
        }
        return json;
    }

    @Override
    public String prettifyGroupNumber(String group) {
        if (group != null && !group.isEmpty()) {
            Matcher m;
            m = Pattern.compile("(.*)([a-zа-яё])(\\d{4}[a-zа-яё]?)(.*)", Pattern.CASE_INSENSITIVE).matcher(group);
            if (m.find()) {
                group = m.group(1) + m.group(2).toUpperCase() + m.group(3).toLowerCase() + m.group(4);
            }
            m = Pattern.compile("(.*)([a-zа-яё]{2}\\d{2}[a-zа-яё]{1,3})(.*)", Pattern.CASE_INSENSITIVE).matcher(group);
            if (m.find()) {
                group = m.group(1) + m.group(2).toUpperCase() + m.group(3);
            }
        }
        return group;
    }

    @Override
    public String getRandomString(int length) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        Random rnd = new Random();
        StringBuilder string = new StringBuilder();
        while (string.length() < length) {
            string.append(alphabet.charAt((int) (rnd.nextFloat() * alphabet.length())));
        }
        return string.toString();
    }

    @Override
    public String getStringSafely(JSONObject json, String key, String def) {
        try {
            return json.getString(key);
        } catch (Exception e) {
            return def;
        }
    }

    @Override
    public String bytes2readable(Context context, StoragePref storagePref, long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".charAt(exp - 1) + "i";
        return String.format(getLocale(context, storagePref), "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    @Override
    public String crypt(String value) {
        return crypt(value, "SHA-256");
    }

    @Override
    public String crypt(String value, String algorithm) {
        String hash = null;
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] bytes = md.digest(value.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }
            hash = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            switch (algorithm) {
                case "SHA-256":
                    hash = crypt(value, "SHA-1");
                    break;
                case "SHA-1":
                    hash = crypt(value, "MD5");
                    break;
                case "MD5":
                    hash = Transliterate.cyr2lat(value);
                    break;
            }
        } catch (UnsupportedEncodingException e) {
            hash = Transliterate.cyr2lat(value);
        }
        return hash;
    }
}
