package utils;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class used to create a ZIP archive from a file.
 */
public class ZipArchiver {

    private final List<File> fileList = new ArrayList<>();
    private final Path inputFile;
    private final Path outputFile;
    private final char[] password;

    /**
     * Constructor.
     *
     * @param inputFile The input file (file or directory). In case of a directory the root folder is not added by default.
     * @param outputFile The output ZIP archive.
     */
    public ZipArchiver(Path inputFile, Path outputFile) {
        this(inputFile, outputFile, null);
    }

    public ZipArchiver(Path inputFile, Path outputFile, char[] password) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.password = password;
    }

    /**
     * Traverse a directory and get all files, and add the file into fileList
     *
     * @param node file or directory
     */
    private void generateFileList(File node){
        // Add file only
        if (node.isFile()){
            fileList.add(node);
        } else if (node.isDirectory()){
            String[] subNote = node.list();
            if (subNote != null) {
                for (String filename : subNote) {
                    generateFileList(new File(node, filename));
                }
            }
        }
    }

    /**
     * Build the ZIP archive.
     */
    public void zip() {
        generateFileList(inputFile.toFile());
        try {
            Files.createDirectories(outputFile.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create ZIP output file", e);
        }
        ZipFile zipFile = new ZipFile(outputFile.toFile());
        ZipParameters zipParameters = new ZipParameters();
        if (password != null) {
            zipFile.setPassword(password);
            zipParameters.setEncryptFiles(true);
            zipParameters.setEncryptionMethod(EncryptionMethod.AES);
            zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
        }
        try {
            if (Files.isDirectory(inputFile)) {
                zipParameters.setIncludeRootFolder(false);
                zipFile.addFolder(inputFile.toFile(), zipParameters);
            } else {
                zipFile.addFile(inputFile.toFile(), zipParameters);
            }
        } catch (ZipException e) {
            throw new IllegalStateException("Error while creating ZIP archive", e);
        }
    }

    public Map<String, Path> unzip() {
        Map<String, Path> extractedFiles = new HashMap<>();
        try {
            ZipFile zipFile = new ZipFile(inputFile.toFile(), password);
            if (zipFile.getFileHeaders() != null) {
                for (FileHeader header: zipFile.getFileHeaders()) {
                    zipFile.extractFile(header, outputFile.toFile().getAbsolutePath());
                    extractedFiles.put(header.getFileName(), Paths.get(outputFile.toFile().getAbsolutePath(), header.getFileName()));
                }
            }
        } catch (ZipException e) {
            throw new IllegalStateException("Unable to extract ZIP archive", e);
        }
        return extractedFiles;
    }

}
