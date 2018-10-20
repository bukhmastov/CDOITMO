package com.bukhmastov.cdoitmo.util.singleton;

public class NumberUtils {

    public static Double toDouble(String value) {
        try {
            if (value == null) {
                return null;
            }
            return Double.valueOf(value.replace(",", ".").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer toInteger(String value) {
        try {
            return Integer.valueOf(value);
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
