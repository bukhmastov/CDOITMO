package com.bukhmastov.cdoitmo.util.singleton;

import android.os.Build;
import android.text.Html;

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

    @NonNull
    public static String defaultIfNull(String string, String def) {
        return string == null ? def : string;
    }

    @NonNull
    public static String defaultIfEmpty(String string, String def) {
        return isEmpty(string) ? def : string;
    }

    @NonNull
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
        if (value == null) {
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
}
