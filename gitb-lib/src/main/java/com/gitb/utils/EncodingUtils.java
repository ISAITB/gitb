package com.gitb.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by senan on 03.12.2014.
 */
public class EncodingUtils {

    public static String DATA_URL_PATTERN = "^data:.+\\/(.+);base64,(.*)$";

    public static String extractBase64FromDataURL(String dataURL) {
       Pattern p = Pattern.compile(DATA_URL_PATTERN);
        Matcher m = p.matcher(dataURL);

        if (m.find()) {
            String base64 = m.group(2);
            return base64;
        }

        //cannot find
        return null;
    }
}
