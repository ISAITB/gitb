package com.gitb.engine.testcase;

import com.gitb.types.ObjectType;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public class StoredObjectType extends ObjectType {

    private final Path reference;

    StoredObjectType(Path sessionFolder, ObjectType wrappedType) {
        super(null);
        setImportPath(wrappedType.getImportPath());
        setImportTestSuite(wrappedType.getImportTestSuite());
        reference = Path.of(sessionFolder.toString(), UUID.randomUUID().toString());
        try {
            if (wrappedType.getValue() != null) {
                var content = wrappedType.serialize(wrappedType.getEncoding());
                if (content != null && content.length > 0) {
                    Files.createFile(reference);
                    Files.write(reference, content);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error while storing session data to temporary filesystem", e);
        }
    }

    @Override
    public Object getValue() {
        if (Files.exists(reference)) {
            try (InputStream in = Files.newInputStream(reference)) {
                InputSource inputSource = new InputSource(in);
                inputSource.setEncoding(getEncoding());
                return deserializeToNode(inputSource);
            } catch (IOException e) {
                throw new IllegalStateException("Error while reading session data from temporary filesystem", e);
            }
        } else {
            return emptyNode();
        }
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof Node) {
            var content = serializeNodeToByteStream((Node) value, getEncoding()).toByteArray();
            if (content.length > 0) {
                try {
                    if (!Files.exists(reference)) {
                        Files.createFile(reference);
                    }
                    Files.write(reference, content, StandardOpenOption.WRITE);
                } catch (IOException e) {
                    throw new IllegalStateException("Error while storing session data to temporary filesystem", e);
                }
            }
        }
    }

}
