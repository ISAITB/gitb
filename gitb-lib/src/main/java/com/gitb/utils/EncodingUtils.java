package com.gitb.utils;

/**
 * Created by senan on 03.12.2014.
 */
public class EncodingUtils {

    public static String extractBase64FromDataURL(String dataURL) {
        if (dataURL != null) {
            int index = dataURL.indexOf("base64,");
            if (index > 0) {
                return dataURL.substring(index+"base64,".length());
            }
        }
        return null;
    }
}
