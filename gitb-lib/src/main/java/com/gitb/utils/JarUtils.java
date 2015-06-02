package com.gitb.utils;

import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by root on 3/11/15.
 */
public class JarUtils {
    public static ZipInputStream getJar(Class<?> clazz) throws IOException {
        CodeSource src = clazz.getProtectionDomain().getCodeSource();

        if(src == null) {
            return null;
        }

        URL jar = src.getLocation();

        return new ZipInputStream(jar.openStream());
    }

    public static List<String> getJarFileNamesForPattern(Class<?> clazz, Pattern pattern) throws IOException {

        List<String> result = new ArrayList<>();

        ZipInputStream zip = getJar(clazz);

        ZipEntry zipEntry = null;
        while((zipEntry = zip.getNextEntry()) != null) {
            String name = zipEntry.getName();

            if(pattern.matcher(name).matches()) {
                result.add(name);
            }
        }

        return result;
    }
}
