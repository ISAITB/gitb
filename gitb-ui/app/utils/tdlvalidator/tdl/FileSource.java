package utils.tdlvalidator.tdl;

import com.gitb.vs.tdl.InputStreamSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class FileSource implements InputStreamSource {

    private final File file;

    public FileSource(File file) {
        this.file = file;
    }

    @Override
    public InputStream getInputStream() {
        try {
            return Files.newInputStream(file.toPath());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open stream from file", e);
        }
    }
}
