package com.bukhmastov.cdoitmo.util.singleton;

import android.content.Context;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {

    public static File makeTempSharedFile(Context context, String name, byte[] data) throws IOException {
        File file = new File(context.getCacheDir(), "shared" + File.separator + name);
        if (!file.exists()) {
            if (file.getParentFile() == null || !file.getParentFile().mkdirs()) {
                if (!file.createNewFile()) {
                    throw new IOException("Failed to create file: " + file.getPath());
                }
            }
        }
        try (
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos)
        ) {
            bos.write(data);
            bos.flush();
        }
        file.deleteOnExit();
        return file;
    }
}
