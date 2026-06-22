/*
 * Copyright (C) 2026 European Union
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

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Set;

public class MimeUtil {

    private static final Tika tika = new Tika();
    private static final Set<String> imageMimeTypes = Set.of("image/png", "image/x-png", "image/jpeg", "image/gif", "image/svg+xml");
    private static final String PBE_ALGORITHM = "PBEWithMD5AndDES";
    private static final int PBE_ITERATIONS = 1000;
    private static final int PBE_SALT_SIZE = 8;

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
        return value != null && value.startsWith("data:") && value.substring(0, Math.min(50, value.length())).contains(";base64,");
    }

    public static String getExtensionFromMimeType(String mimeType) {
        String extension = null;
        if (mimeType != null) {
            try {
                MimeType mimeTypeObj = TikaConfig.getDefaultConfig().getMimeRepository().forName(mimeType);
                extension = mimeTypeObj.getExtension();
            } catch (MimeTypeException e) {
                // Ignore.
            }
        }
        return extension;
    }

    public static String getMimeType(String content, boolean notEncoded, boolean refineTextPlain) {
        String mimeType;
        byte[] contentBytes = null;
        if (notEncoded) {
            mimeType = getMimeType(content.getBytes());
        } else {
            // Base64 content
            if (isDataURL(content)) {
                content = getBase64FromDataURL(content);
            }
            // Decoded base64 content
            contentBytes = Base64.decodeBase64(content);
            mimeType = getMimeType(contentBytes);
        }
        if (refineTextPlain && mimeType != null && mimeType.contains("text/plain")) {
            if (contentBytes != null) {
                mimeType = refineTextMimeType(new String(contentBytes, StandardCharsets.UTF_8));
            } else {
                mimeType = refineTextMimeType(content);
            }
        }
        return mimeType;
    }

    public static String getMimeTypeFromDataURL(String dataURL) {
        return getMimeTypeFromBase64(getBase64FromDataURL(dataURL));
    }

    public static String refineTextMimeType(String content) {
        String prefix = content.stripLeading();
        if (prefix.length() > 256) prefix = prefix.substring(0, 256);
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        if (prefix.startsWith("{") || prefix.startsWith("[")) return "application/json";
        if (lowerPrefix.startsWith("<!doctype html") || lowerPrefix.startsWith("<html")) return "text/html";
        if (prefix.startsWith("<")) return "application/xml";
        return "text/plain";
    }

    public static String refineTextMimeType(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            char[] buf = new char[256];
            int n = reader.read(buf);
            return refineTextMimeType(n > 0 ? new String(buf, 0, n) : "");
        } catch (IOException e) {
            return "text/plain";
        }
    }

    public static String encryptString(String input) {
        return encryptString(input, Configurations.MASTER_PASSWORD());
    }

    public static String encryptString(String input, char[] key) {
        try {
            byte[] salt = new byte[PBE_SALT_SIZE];
            new SecureRandom().nextBytes(salt);
            Cipher cipher = Cipher.getInstance(PBE_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, pbeKey(key), new PBEParameterSpec(salt, PBE_ITERATIONS));
            byte[] encrypted = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[PBE_SALT_SIZE + encrypted.length];
            System.arraycopy(salt, 0, combined, 0, PBE_SALT_SIZE);
            System.arraycopy(encrypted, 0, combined, PBE_SALT_SIZE, encrypted.length);
            return Base64.encodeBase64String(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public static String decryptString(String input) {
        return decryptString(input, Configurations.MASTER_PASSWORD());
    }

    public static String decryptString(String input, char[] key) {
        try {
            byte[] combined = Base64.decodeBase64(input);
            byte[] salt = new byte[PBE_SALT_SIZE];
            System.arraycopy(combined, 0, salt, 0, PBE_SALT_SIZE);
            byte[] ciphertext = new byte[combined.length - PBE_SALT_SIZE];
            System.arraycopy(combined, PBE_SALT_SIZE, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance(PBE_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, pbeKey(key), new PBEParameterSpec(salt, PBE_ITERATIONS));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    private static SecretKey pbeKey(char[] key) throws Exception {
        return SecretKeyFactory.getInstance(PBE_ALGORITHM).generateSecret(new PBEKeySpec(key));
    }

}
