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

import com.gitb.utils.EncodingUtils;
import config.Configurations;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class MimeUtil {

    private static final Tika tika = new Tika();
    private static final Set<String> imageMimeTypes = Set.of("image/png", "image/x-png", "image/jpeg", "image/gif", "image/svg+xml");
    // "application/x-tika-ooxml" is Tika-core's own generic placeholder for a recognised-but-undisambiguated
    // OOXML package (Word/PowerPoint/Excel all look the same until the well-known entries are inspected) -
    // it has no extension of its own, so it must trigger the same zip-container refinement as a plain zip.
    private static final Set<String> zipMimeTypes = Set.of("application/zip", "application/x-zip-compressed", "application/x-tika-ooxml");
    private static final String ODF_MIMETYPE_ENTRY = "mimetype";
    private static final String OOXML_MARKER_ENTRY = "[Content_Types].xml";
    private static final String OOXML_WORD_ENTRY = "word/document.xml";
    private static final String OOXML_POWERPOINT_ENTRY = "ppt/presentation.xml";
    private static final String OOXML_EXCEL_ENTRY = "xl/workbook.xml";
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

    /**
     * Detects the mime type of the given content. This is the single place detection happens - the result
     * is already refined and ready to use directly (callers that only need this and not also the file
     * extension no longer need to call anything else):
     *  - a ZIP-based container recognised as OOXML/ODF is resolved to its specific mime type (e.g. a DOCX
     *    is reported as such, rather than as a generic ZIP or Tika's own undisambiguated
     *    "application/x-tika-ooxml" bucket type - see {@link #refineZipContainerMimeType(byte[])});
     *  - a generic "text/plain" is refined to a more specific syntax where recognisable (JSON, HTML, XML -
     *    see {@link #refineTextMimeType(String)}).
     *
     * See {@link #getFileExtension(byte[])} for the (still just two calls) equivalent that also resolves
     * the file extension to use for this content.
     */
    public static String getMimeType(byte[] bytes) {
        return refineMimeType(tika.detect(bytes), bytes);
    }

    /**
     * Same as {@link #getMimeType(byte[])}, for a file already on disk.
     */
    public static String getMimeType(Path path) {
        try {
            return refineMimeType(tika.detect(path), path);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read file", e);
        }
    }

    private static String refineMimeType(String mimeType, byte[] content) {
        if (mimeType != null && zipMimeTypes.contains(mimeType)) {
            String refinedMimeType = refineZipContainerMimeType(content);
            if (refinedMimeType != null) {
                return refinedMimeType;
            }
        } else if (mimeType != null && mimeType.contains("text/plain")) {
            return refineTextMimeType(new String(content, StandardCharsets.UTF_8));
        }
        return mimeType;
    }

    private static String refineMimeType(String mimeType, Path path) {
        if (mimeType != null && zipMimeTypes.contains(mimeType)) {
            String refinedMimeType = refineZipContainerMimeType(path);
            if (refinedMimeType != null) {
                return refinedMimeType;
            }
        } else if (mimeType != null && mimeType.contains("text/plain")) {
            return refineTextMimeType(path);
        }
        return mimeType;
    }

    public static boolean isDataURL(String value) {
        return EncodingUtils.isDataUrl(value);
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

    /**
     * Detects the mime type of the given content, refined as per {@link #getMimeType(byte[])}.
     *
     * @param notEncoded Whether {@code content} is the raw content itself, rather than base64-encoded
     *                    (possibly as a full data URL).
     */
    public static String getMimeType(String content, boolean notEncoded) {
        if (notEncoded) {
            return getMimeType(content.getBytes());
        } else {
            String base64 = isDataURL(content) ? getBase64FromDataURL(content) : content;
            return getMimeType(Base64.decodeBase64(base64));
        }
    }

    public static String getMimeTypeFromDataURL(String dataURL) {
        return getMimeTypeFromBase64(getBase64FromDataURL(dataURL));
    }

    private static String refineTextMimeType(String content) {
        String prefix = content.stripLeading();
        if (prefix.length() > 256) prefix = prefix.substring(0, 256);
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        if (prefix.startsWith("{") || prefix.startsWith("[")) return "application/json";
        if (lowerPrefix.startsWith("<!doctype html") || lowerPrefix.startsWith("<html")) return "text/html";
        if (prefix.startsWith("<")) return "application/xml";
        return "text/plain";
    }

    private static String refineTextMimeType(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            char[] buf = new char[256];
            int n = reader.read(buf);
            return refineTextMimeType(n > 0 ? new String(buf, 0, n) : "");
        } catch (IOException e) {
            return "text/plain";
        }
    }

    /**
     * Best-effort file extension (leading dot included) for the given file, or "" if it cannot be
     * determined - just the (already refined) mime type from {@link #getMimeType(Path)}, mapped to an
     * extension.
     */
    public static String getFileExtension(Path path) {
        return resolveExtension(getMimeType(path));
    }

    /**
     * Same as {@link #getFileExtension(Path)}, for content already held in memory.
     */
    public static String getFileExtension(byte[] content) {
        return resolveExtension(getMimeType(content));
    }

    private static String resolveExtension(String mimeType) {
        if ("application/octet-stream".equals(mimeType)) {
            // Tika's generic fallback for content it could not recognise at all - not a meaningful
            // extension to report (its registered extension, ".bin", would be misleading here).
            return "";
        }
        return StringUtils.defaultString(getExtensionFromMimeType(mimeType));
    }

    /**
     * Tika's magic-byte detection cannot distinguish OOXML/ODF documents from a plain ZIP archive - they
     * all start with the same local file header. This best-effort check inspects a few well-known entries
     * to recognise the common office document container formats, without needing Tika's heavier
     * parser/detector modules.
     */
    private static String refineZipContainerMimeType(Path path) {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            byte[] odfMimetypeContent = null;
            ZipEntry odfMimetypeEntry = zip.getEntry(ODF_MIMETYPE_ENTRY);
            if (odfMimetypeEntry != null) {
                // ODF: the first (stored, uncompressed) entry holds the exact mime type as its content.
                try (InputStream is = zip.getInputStream(odfMimetypeEntry)) {
                    odfMimetypeContent = is.readAllBytes();
                }
            }
            boolean isOoxml = zip.getEntry(OOXML_MARKER_ENTRY) != null;
            boolean isWord = zip.getEntry(OOXML_WORD_ENTRY) != null;
            boolean isPowerpoint = zip.getEntry(OOXML_POWERPOINT_ENTRY) != null;
            boolean isExcel = zip.getEntry(OOXML_EXCEL_ENTRY) != null;
            return declaredZipContainerMimeType(isOoxml, isWord, isPowerpoint, isExcel, odfMimetypeContent);
        } catch (IOException e) {
            // Not a valid/readable ZIP - leave the mime type as plain zip.
            return null;
        }
    }

    /**
     * Same as {@link #refineZipContainerMimeType(Path)}, for content already held in memory. Entries are
     * only accessible sequentially via {@link ZipInputStream} (unlike {@link ZipFile}'s random-access
     * lookups), so all entry names of interest are collected in a single pass.
     */
    private static String refineZipContainerMimeType(byte[] content) {
        boolean isOdf = false;
        boolean isOoxml = false;
        boolean isWord = false;
        boolean isPowerpoint = false;
        boolean isExcel = false;
        byte[] odfMimetypeContent = null;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                switch (entry.getName()) {
                    case ODF_MIMETYPE_ENTRY -> { isOdf = true; odfMimetypeContent = zip.readAllBytes(); }
                    case OOXML_MARKER_ENTRY -> isOoxml = true;
                    case OOXML_WORD_ENTRY -> isWord = true;
                    case OOXML_POWERPOINT_ENTRY -> isPowerpoint = true;
                    case OOXML_EXCEL_ENTRY -> isExcel = true;
                    default -> { /* Not a part we need to recognise the container format. */ }
                }
            }
        } catch (IOException e) {
            // Not a valid/readable ZIP - leave the mime type as plain zip.
            return null;
        }
        return declaredZipContainerMimeType(isOoxml, isWord, isPowerpoint, isExcel, isOdf ? odfMimetypeContent : null);
    }

    /**
     * Decides the mime type of a ZIP-based container from the well-known entries found within it - shared
     * by the {@link Path} and {@code byte[]} variants of {@code refineZipContainerMimeType} above.
     */
    private static String declaredZipContainerMimeType(boolean isOoxml, boolean isWord, boolean isPowerpoint, boolean isExcel, byte[] odfMimetypeContent) {
        if (odfMimetypeContent != null) {
            // ODF: the first (stored, uncompressed) entry holds the exact mime type as its content.
            String declaredMimeType = new String(odfMimetypeContent, StandardCharsets.UTF_8).trim();
            if (StringUtils.isNotBlank(declaredMimeType)) {
                return declaredMimeType;
            }
        } else if (isOoxml) {
            // OOXML: distinguish by the well-known top-level part.
            if (isWord) {
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            } else if (isPowerpoint) {
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            } else if (isExcel) {
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            }
        }
        return null;
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
