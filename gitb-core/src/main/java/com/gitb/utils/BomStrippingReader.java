package com.gitb.utils;

import java.io.*;

/**
 * BOM stripping reader class.
 * <p>
 * This reader ensures that the BOM information is correctly handled in unicode files.
 *
 * @author simatosc.
 */
public class BomStrippingReader extends Reader {

    private InputStreamReader internal;

    /**
     * Constructor.
     *
     * @param in The stream to read the file bytes from.
     */
    public BomStrippingReader(InputStream in) {
        try {
            String charsetName = "UTF-8";
            PushbackInputStream pushbackStream = new PushbackInputStream(in, 4);
            byte[] buffer = new byte[4];
            int bytesRead = pushbackStream.read(buffer, 0, buffer.length);
            int unread = 0;
            if (bytesRead >= 4 &&
                    buffer[0] == (byte)0x00 && buffer[1] == (byte)0x00 && buffer[2] == (byte)0xFE && buffer[3] == (byte)0xFF) {
                // 00 00 FE FF (UTF-32, big-endian).
                charsetName = "UTF-32BE";
                unread = bytesRead - 4;
            } else if (bytesRead >= 4 &&
                    buffer[0] == (byte)0xFF && buffer[1] == (byte)0xFE && buffer[2] == (byte)0x00 && buffer[3] == (byte)0x00) {
                // FF FE 00 00 (UTF-32, little-endian).
                charsetName = "UTF-32LE";
                unread = bytesRead - 4;
            } else if (bytesRead >= 3 &&
                    buffer[0] == (byte)0xEF && buffer[1] == (byte)0xBB && buffer[2] == (byte)0xBF) {
                // EF BB BF (UTF-8).
                charsetName = "UTF-8";
                unread = bytesRead - 3;
            } else if (bytesRead >= 2 &&
                    buffer[0] == (byte)0xFE && buffer[1] == (byte)0xFF) {
                // FE FF (UTF-16, big-endian).
                charsetName = "UTF-16BE";
                unread = bytesRead - 2;
            } else if (bytesRead >= 2 &&
                    buffer[0] == (byte)0xFF && buffer[1] == (byte)0xFE) {
                // FF FE (UTF-16, little-endian).
                charsetName = "UTF-16LE";
                unread = bytesRead - 2;
            } else {
                unread = bytesRead;
            }
            if (unread > 0) {
                pushbackStream.unread(buffer, bytesRead-unread, unread);
            }
            internal = new InputStreamReader(pushbackStream, charsetName);
        } catch (IOException e) {
            throw new IllegalStateException("Error checking for BOM characters.", e);
        }
    }

    /**
     * @see Reader#read(char[], int, int)
     */
    @Override
    public int read(char[] cbuf, int offset, int length) throws IOException {
        return internal.read(cbuf, offset, length);
    }

    /**
     * @see Reader#close()
     */
    @Override
    public void close() throws IOException {
        internal.close();
    }

}
