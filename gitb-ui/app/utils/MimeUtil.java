package utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;

public class MimeUtil {

    private final static Tika tika = new Tika();

    public static String getBase64FromDataURL(String dataURL) {
        String result = null;
        if (dataURL != null) {
            result = dataURL.substring(dataURL.indexOf(",")+1);
        }
        return result;
    }

    public static String getMimeTypeFromBase64(String base64) {
        return getMimeType(Base64.decodeBase64(base64));
    }

    public static String getMimeType(byte[] bytes) {
        return tika.detect(bytes);
    }

    public static boolean isDataURL(String value) {
        return value != null && value.startsWith("data:") && value.contains(";base64,");
    }

    public static String getExtensionFromMimeType(String mimeType) {
        String extension = null;
        if (mimeType != null) {
            try {
                MimeType mimeTypeObj = TikaConfig.getDefaultConfig().getMimeRepository().forName(mimeType);
                extension = mimeTypeObj.getExtension();
            } catch (MimeTypeException e) {
                extension = null;
            }
        }
        return extension;
    }

    public static String getMimeType(String content, boolean notEncoded) {
        if (notEncoded) {
            return getMimeType(content.getBytes());
        } else {
            if (isDataURL(content)) {
                return getMimeTypeFromBase64(getBase64FromDataURL(content));
            } else {
                return getMimeTypeFromBase64(content);
            }
        }
    }

    public static String getMimeTypeFromDataURL(String dataURL) {
        return getMimeTypeFromBase64(getBase64FromDataURL(dataURL));
    }

}
