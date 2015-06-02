package net.validex.gitb.model;

/**
 * Created by root on 3/11/15.
 */
public class ValidationRequest {
    private final String filename;
    private final String fileContents;

    public ValidationRequest(String filename, String fileContents) {
        this.filename = filename;
        this.fileContents = fileContents;
    }

    public String getFilename() {
        return filename;
    }

    public String getFileContents() {
        return fileContents;
    }
}
