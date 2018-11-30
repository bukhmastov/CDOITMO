package com.bukhmastov.cdoitmo.util.singleton;

import androidx.annotation.Nullable;

public class NumberUtils {

    public static Double toDouble(@Nullable String value) {
        try {
            if (value == null) {
                return null;
            }
            return Double.valueOf(value.replace(",", ".").replace(" ", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer toInteger(@Nullable String value) {
        try {
            if (value == null) {
                return null;
            }
            return Integer.valueOf(value.replace(" ", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer toDoubleInteger(String value) {
        try {
            Double aDouble = toDouble(value);
            if (aDouble == null) {
                return null;
            }
            return aDouble.intValue();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String prettyDouble(Double value) {
        return prettyDouble(value, "");
    }

    public static String prettyDouble(Double value, String def) {
        if (value == null || value == -1.0) {
            return def;
        }
        try {
            if (value == Double.parseDouble(value.intValue() + ".0")) {
                return String.valueOf(value.intValue());
            }
        } catch (NumberFormatException ignore) {}
        return String.valueOf(value);
    }
}
