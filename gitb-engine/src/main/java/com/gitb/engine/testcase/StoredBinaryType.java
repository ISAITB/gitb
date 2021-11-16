package com.gitb.engine.testcase;

import com.gitb.types.BinaryType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public class StoredBinaryType extends BinaryType {

    private final Path reference;

    StoredBinaryType(Path sessionFolder, BinaryType wrappedType) {
        setImportPath(wrappedType.getImportPath());
        setImportTestSuite(wrappedType.getImportTestSuite());
        try {
            reference = Files.createFile(Path.of(sessionFolder.toString(), UUID.randomUUID().toString()));
            Files.write(reference, (byte[]) wrappedType.getValue());
        } catch (IOException e) {
            throw new IllegalStateException("Error while storing session data to temporary filesystem", e);
        }
    }

    @Override
    public Object getValue() {
        try {
            return Files.readAllBytes(reference);
        } catch (IOException e) {
            throw new IllegalStateException("Error while reading session data from temporary filesystem", e);
        }
    }

    @Override
    public void setValue(Object value) {
        try {
            Files.write(reference, (byte[]) value, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new IllegalStateException("Error while storing session data to temporary filesystem", e);
        }
    }
}
