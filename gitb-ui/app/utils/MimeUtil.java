/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package utils;

import config.Configurations;
import org.apache.commons.codec.binary.Base64;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.jasypt.util.text.BasicTextEncryptor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public class MimeUtil {

    private static final Tika tika = new Tika();
    private static final Set<String> imageMimeTypes = Set.of("image/png", "image/x-png", "image/jpeg", "image/gif", "image/svg+xml");

    public static String base64AsDataURL(String base64Content) {
        return base64AsDataURL(base64Content, null);
    }

    public static String base64AsDataURL(String base64Content, String mimeType) {
        if (base64Content.startsWith("data:")) {
            return base64Content;
        } else {
            return createDataURLString(base64Content, mimeType);
        }
    }

    private static String createDataURLString(String base64, String mimeType) {
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        return "data:" + mimeType + ";base64," + base64;
    }

    private static String getBytesAsDataURL(byte[] content, String mimeType) {
        return createDataURLString(Base64.encodeBase64String(content), mimeType);
    }

    public static String getFileAsDataURL(File file, String mimeType) {
        try {
            return getBytesAsDataURL(Files.readAllBytes(file.toPath()), mimeType);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to convert file to Base64 string", e);
        }
    }

    public static String getBase64FromDataURL(String dataURL) {
        String result = null;
        if (dataURL != null) {
            result = dataURL.substring(dataURL.indexOf(",")+1);
        }
        return result;
    }

    public static boolean isImageType(String mimeType) {
        return imageMimeTypes.contains(mimeType.toLowerCase(Locale.getDefault()));
    }

    public static String getMimeTypeFromBase64(String base64) {
        return getMimeType(Base64.decodeBase64(base64));
    }

    public static String getMimeType(byte[] bytes) {
        return tika.detect(bytes);
    }

    public static String getMimeType(Path path) {
        try {
            return tika.detect(path);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read file", e);
        }
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

    public static String encryptString(String input) {
        return encryptString(input, Configurations.MASTER_PASSWORD());
    }

    public static String encryptString(String input, char[] key) {
        return new Encryptor(key).encryptor.encrypt(input);
    }

    public static String decryptString(String input) {
        return new Encryptor(Configurations.MASTER_PASSWORD()).encryptor.decrypt(input);
    }

    public static String decryptString(String input, char[] key) {
        return new Encryptor(key).encryptor.decrypt(input);
    }

    private static class Encryptor {

        private BasicTextEncryptor encryptor;

        private Encryptor(char[] key) {
            encryptor = new BasicTextEncryptor();
            encryptor.setPasswordCharArray(key);
        }

    }
}
