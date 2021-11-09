package com.gitb.engine.testcase;

import com.gitb.types.SchemaType;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public class StoredSchemaType extends SchemaType {

    private final Path reference;

    StoredSchemaType(Path sessionFolder, SchemaType wrappedType) {
        super(null);
        this.testSuiteId = wrappedType.getTestSuiteId();
        this.schemaLocation = wrappedType.getSchemaLocation();
        try {
            reference = Files.createFile(Path.of(sessionFolder.toString(), UUID.randomUUID().toString()));
            Files.write(reference, wrappedType.serialize(wrappedType.getEncoding()));
        } catch (IOException e) {
            throw new IllegalStateException("Error while storing session data to temporary filesystem", e);
        }
    }

    @Override
    public Object getValue() {
        try (InputStream in = Files.newInputStream(reference)) {
            InputSource inputSource = new InputSource(in);
            inputSource.setEncoding(getEncoding());
            return deserializeToNode(inputSource);
        } catch (IOException e) {
            throw new IllegalStateException("Error while reading session data from temporary filesystem", e);
        }
    }

    @Override
    public void setValue(Object value) {
        try {
            Files.write(reference, serializeNodeToByteStream((Node) value, getEncoding()).toByteArray(), StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new IllegalStateException("Error while storing session data to temporary filesystem", e);
        }
    }

}
