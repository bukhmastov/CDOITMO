package com.bukhmastov.cdoitmo.util.singleton;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AssetUtils {

    public static String readTextFile(Context context, String assetName) throws IOException {
        AssetManager assetManager = context.getAssets();
        try (
            InputStream inputStream = assetManager.open(assetName);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader)
        ) {
            StringBuilder content = new StringBuilder();
            for (String line; (line = reader.readLine()) != null;) {
                content.append(line).append('\n');
            }
            return content.toString();
        }
    }
}
