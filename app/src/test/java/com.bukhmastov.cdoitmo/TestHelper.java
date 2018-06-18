package com.bukhmastov.cdoitmo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Collectors;

public class TestHelper {
    public static String readResource(String path) {
        path = "src/test/resources/" + path;
        try {
            return Files.newBufferedReader(Paths.get(path)).lines().collect(Collectors.joining("\n"));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
