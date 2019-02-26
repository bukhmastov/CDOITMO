package com.bukhmastov.cdoitmo.util.singleton;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.text.Html;

import com.bukhmastov.cdoitmo.util.StoragePref;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class StringUtils {

    public static boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }

    public static boolean isBlank(String string) {
        return string == null || string.trim().isEmpty();
    }

    public static boolean isNotEmpty(String string) {
        return !isEmpty(string);
    }

    public static boolean isNotBlank(String string) {
        return !isBlank(string);
    }

    @NonNull
    public static String emptyIfNull(String string) {
        return defaultIfNull(string, "");
    }

    public static String defaultIfNull(String string, String def) {
        return string == null ? def : string;
    }

    public static String defaultIfEmpty(String string, String def) {
        return isEmpty(string) ? def : string;
    }

    public static String defaultIfBlank(String string, String def) {
        return isBlank(string) ? def : string;
    }

    public static int getWordDeclinationByNumber(@Nullable Integer number) {
        if (number == null) {
            return 0;
        }
        switch (number % 100) {
            case 10: case 11: case 12: case 13: case 14: return 3;
            default:
                switch (number % 10) {
                    case 1: return 1;
                    case 2: case 3: case 4: return 2;
                    default: return 3;
                }
        }
    }

    @NonNull
    @SuppressWarnings("deprecation")
    public static String removeHtmlTags(@Nullable String value) {
        if (isEmpty(value)) {
            return "";
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString().trim();
            } else {
                return Html.fromHtml(value).toString().trim();
            }
        } catch (Exception e) {
            return "";
        }
    }

    @Nullable
    @SafeVarargs
    public static <T> T nvlt(T...values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @NonNull
    public static String escapeString(@Nullable String value) {
        if (isEmpty(value)) {
            return "";
        }
        return value.trim().replaceAll("\\s{2,}", " ");
    }

    public static String capitalizeFirstLetter(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    public static Locale getLocale(Context context, StoragePref storagePref) {
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

    @SuppressLint("DefaultLocale")
    public static String ldgZero(int number) {
        return String.format("%02d", number);
    }

    public static String prettifyGroupNumber(String group) {
        if (group != null && !group.isEmpty()) {
            Matcher m;
            m = Pattern.compile("(.*)([a-zа-яё])(\\d{4,}[a-zа-яё]?)(.*)", Pattern.CASE_INSENSITIVE).matcher(group);
            if (m.find()) {
                group = m.group(1) +
                        Transliterate.letter2lat(m.group(2).toUpperCase()) +
                        m.group(3).toLowerCase() +
                        m.group(4);
            }
            m = Pattern.compile("(.*)([a-zа-яё]{2}\\d{2}[a-zа-яё]{1,3})(.*)", Pattern.CASE_INSENSITIVE).matcher(group);
            if (m.find()) {
                group = m.group(1) + m.group(2).toUpperCase() + m.group(3);
            }
        }
        return group;
    }

    public static String getRandomString(int length) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        Random rnd = new Random();
        StringBuilder string = new StringBuilder();
        while (string.length() < length) {
            string.append(alphabet.charAt((int) (rnd.nextFloat() * alphabet.length())));
        }
        return string.toString();
    }

    public static String bytes2readable(Context context, StoragePref storagePref, long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".charAt(exp - 1) + "i";
        return String.format(getLocale(context, storagePref), "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static String crypt(String value) {
        return crypt(value, "SHA-256", true);
    }

    public static String cryptMD5(String value) {
        return crypt(value, "MD5", false);
    }

    public static String cryptSHA1(String value) {
        return crypt(value, "SHA-1", false);
    }

    public static String cryptSHA256(String value) {
        return crypt(value, "SHA-256", false);
    }

    private static String crypt(String value, String algorithm, boolean fallback) {
        String hash;
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] bytes = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }
            hash = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            if (!fallback) {
                return Transliterate.cyr2lat(value);
            }
            switch (algorithm) {
                case "SHA-256":
                    hash = crypt(value, "SHA-1", true);
                    break;
                case "SHA-1":
                    hash = crypt(value, "MD5", true);
                    break;
                case "MD5":
                    hash = Transliterate.cyr2lat(value);
                    break;
                default:
                    hash = Transliterate.cyr2lat(value);
                    break;
            }
        }
        return hash;
    }
}
