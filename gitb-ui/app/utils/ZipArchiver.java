package utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Class used to create a ZIP archive from a file.
 */
public class ZipArchiver {

    private Map<String, File> fileList = new LinkedHashMap<>();
    private final Path inputFile;
    private final Path outputFile;
    private final String rootPath;

    /**
     * Constructor.
     *
     * @param inputFile The input file (file or directory). In case of a directory the root folder is not added by default.
     * @param outputFile The output ZIP archive.
     */
    public ZipArchiver(Path inputFile, Path outputFile) {
        this(inputFile, outputFile, false);
    }

    /**
     * Constructor.
     *
     * @param inputFile The input file (file or directory).
     * @param outputFile The output ZIP archive.
     * @param includeRoot Whether or not the root directory should be included (if inputFile is a directory).
     */
    public ZipArchiver(Path inputFile, Path outputFile, boolean includeRoot) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        if (Files.isDirectory(inputFile) && !includeRoot) {
            rootPath = inputFile.toAbsolutePath().toString();
        } else {
            rootPath = inputFile.getParent().toAbsolutePath().toString();
        }
    }

    /**
     * Traverse a directory and get all files, and add the file into fileList
     *
     * @param node file or directory
     */
    private void generateFileList(File node){
        // Add file only
        if (node.isFile()){
            fileList.put(generateZipEntry(node), node);
        } else if (node.isDirectory()){
            String[] subNote = node.list();
            for (String filename : subNote) {
                generateFileList(new File(node, filename));
            }
        }
    }

    /**
     * Generate the path entry for the ZIP archive.
     *
     * @param fileToZip The file to process.
     * @return The path string.
     */
    private String generateZipEntry(File fileToZip) {
        return fileToZip.getAbsolutePath().substring(rootPath.length()+1, fileToZip.getAbsolutePath().length());
    }

    /**
     * Build the ZIP archive.
     */
    public void zip() {
        generateFileList(inputFile.toFile());

        byte[] buffer = new byte[1024];
        try {
            Files.createDirectories(outputFile.getParent());
            try (
                OutputStream fos = Files.newOutputStream(outputFile);
                ZipOutputStream zos = new ZipOutputStream(fos)
            ) {
                for (Map.Entry<String, File> entry : fileList.entrySet()) {
                    ZipEntry ze = new ZipEntry(entry.getKey());
                    zos.putNextEntry(ze);
                    try (InputStream in = Files.newInputStream(entry.getValue().toPath())) {
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                    }
                }
                zos.closeEntry();
            }
        } catch(IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
