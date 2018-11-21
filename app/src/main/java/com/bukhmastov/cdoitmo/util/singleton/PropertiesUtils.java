package com.bukhmastov.cdoitmo.util.singleton;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropertiesUtils {

    private static final Map<String, Properties> CACHED_PROPERTIES = new HashMap<>();

    public static String getIsuProperty(Context context, String key) throws IOException {
        return getProperties(context, "isu.properties").getProperty(key);
    }

    private static Properties getProperties(Context context, String name) throws IOException {
        if (CACHED_PROPERTIES.containsKey(name)) {
            return CACHED_PROPERTIES.get(name);
        }
        Properties properties = new Properties();
        AssetManager assetManager = context.getAssets();
        try (InputStream inputStream = assetManager.open(name)) {
            properties.load(inputStream);
            CACHED_PROPERTIES.put(name, properties);
            return properties;
        }
    }
}
