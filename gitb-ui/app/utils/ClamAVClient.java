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

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * Simple client for ClamAV's clamd scanner. Provides straightforward instream scanning.
 *
 * This client has been copied to the project source from the GitHub repository: https://github.com/solita/clamav-java
 * This has been done to avoid an additional dependency considering that it included only a single class.
 *
 * @See https://github.com/solita/clamav-java
 * Created by simatosc.
 */
public class ClamAVClient {

    private String hostName;
    private int port;
    private int timeout;

    // "do not exceed StreamMaxLength as defined in clamd.conf, otherwise clamd will reply with INSTREAM size limit exceeded and close the connection."
    private static final int CHUNK_SIZE = 2048;
    private static final int DEFAULT_TIMEOUT = 500;
    private static final int PONG_REPLY_LEN = 4;

    /**
     * @param hostName The hostname of the server running clamav-daemon
     * @param port The port that clamav-daemon listens to(By default it might not listen to a port. Check your clamav configuration).
     * @param timeout zero means infinite timeout. Not a good idea, but will be accepted.
     */
    public ClamAVClient(String hostName, int port, int timeout)  {
        if (timeout < 0) {
            throw new IllegalArgumentException("Negative timeout value does not make sense.");
        }
        this.hostName = hostName;
        this.port = port;
        this.timeout = timeout;
    }

    public ClamAVClient(String hostName, int port) {
        this(hostName, port, DEFAULT_TIMEOUT);
    }

    /**
     * Run PING command to clamd to test it is responding.
     *
     * @return true if the server responded with proper ping reply.
     */
    public boolean ping() throws IOException {
        try (Socket s = new Socket(hostName,port); OutputStream outs = s.getOutputStream()) {
            s.setSoTimeout(timeout);
            outs.write(asBytes("zPING\0"));
            outs.flush();
            byte[] b = new byte[PONG_REPLY_LEN];
            InputStream inputStream = s.getInputStream();
            int copyIndex = 0;
            int readResult;
            do {
                readResult = inputStream.read(b, copyIndex, Math.max(b.length - copyIndex, 0));
                copyIndex += readResult;
            } while (readResult > 0);
            return Arrays.equals(b, asBytes("PONG"));
        }
    }

    /**
     * Streams the given data to the server in chunks. The whole data is not kept in memory.
     * This method is preferred if you don't want to keep the data in memory, for instance by scanning a file on disk.
     * Since the parameter InputStream is not reset, you can not use the stream afterwards, as it will be left in a EOF-state.
     * If your goal is to scan some data, and then pass that data further, consider using a temporary file with {@link #scan(File) scan(File in)}.
     * <p>
     * Opens a socket and reads the reply. Parameter input stream is NOT closed.
     *
     * @param is data to scan. Not closed by this method!
     * @return server reply
     */
    public byte[] scan(InputStream is) throws IOException {
        try (Socket s = new Socket(hostName,port); OutputStream outs = new BufferedOutputStream(s.getOutputStream())) {
            s.setSoTimeout(timeout);

            // handshake
            outs.write(asBytes("zINSTREAM\0"));
            outs.flush();
            byte[] chunk = new byte[CHUNK_SIZE];

            try (InputStream clamIs = s.getInputStream()) {
                // send data
                int read = is.read(chunk);
                while (read >= 0) {
                    // The format of the chunk is: '<length><data>' where <length> is the size of the following data in bytes expressed as a 4 byte unsigned
                    // integer in network byte order and <data> is the actual chunk. Streaming is terminated by sending a zero-length chunk.
                    byte[] chunkSize = ByteBuffer.allocate(4).putInt(read).array();

                    outs.write(chunkSize);
                    outs.write(chunk, 0, read);
                    if (clamIs.available() > 0) {
                        // reply from server before scan command has been terminated.
                        byte[] reply = assertSizeLimit(readAll(clamIs));
                        throw new IOException("Scan aborted. Reply from server: " + new String(reply, StandardCharsets.US_ASCII));
                    }
                    read = is.read(chunk);
                }

                // terminate scan
                outs.write(new byte[]{0,0,0,0});
                outs.flush();
                // read reply
                return assertSizeLimit(readAll(clamIs));
            }
        }
    }

    /**
     * Scans bytes for virus by passing the bytes to clamav
     *
     * @param in data to scan
     * @return server reply
     * @deprecated Use the streaming or file based version.
     **/
    @Deprecated
    public byte[] scan(byte[] in) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(in);
        return scan(bis);
    }

    /**
     * Scans a file for virus by passing the file to clamav
     *
     * @param file file to scan
     * @return server reply
     **/
    public byte[] scan(File file) throws IOException {
        try (InputStream in = Files.newInputStream(file.toPath())) {
            return scan(in);
        }
    }

    /**
     * Interpret the result from a  ClamAV scan, and determine if the result means the data is clean
     *
     * @param reply The reply from the server after scanning
     * @return true if no virus was found according to the clamd reply message
     */
    public static boolean isCleanReply(byte[] reply) {
        String r = new String(reply, StandardCharsets.US_ASCII);
        return (r.contains("OK") && !r.contains("FOUND"));
    }


    private byte[] assertSizeLimit(byte[] reply) {
        String r = new String(reply, StandardCharsets.US_ASCII);
        if (r.startsWith("INSTREAM size limit exceeded.")) {
            throw new IllegalStateException("Clamd size limit exceeded. Full reply from server: " + r);
        }
        return reply;
    }

    // byte conversion based on ASCII character set regardless of the current system locale
    private static byte[] asBytes(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    // reads all available bytes from the stream
    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();

        byte[] buf = new byte[2000];
        int read = 0;
        do {
            read = is.read(buf);
            tmp.write(buf, 0, read);
        } while ((read > 0) && (is.available() > 0));
        return tmp.toByteArray();
    }
}
