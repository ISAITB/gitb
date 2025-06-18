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

package com.gitb.engine.testcase;

import com.gitb.types.StringType;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public class StoredStringType extends StringType {

    private final Path reference;

    StoredStringType(Path sessionFolder, StringType wrappedType) {
        try {
            reference = Files.createFile(Path.of(sessionFolder.toString(), UUID.randomUUID().toString()));
            Files.writeString(reference, (String) wrappedType.getValue(), Charset.forName(wrappedType.getEncoding()));
        } catch (IOException e) {
            throw new IllegalStateException("Error while storing session data to temporary filesystem", e);
        }
    }

    @Override
    public Object getValue() {
        try {
            return new String(Files.readAllBytes(reference), getEncoding());
        } catch (IOException e) {
            throw new IllegalStateException("Error while reading session data from temporary filesystem", e);
        }
    }

    @Override
    public void setValue(Object value) {
        try {
            Files.write(reference, ((String) value).getBytes(getEncoding()), StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new IllegalStateException("Error while storing session data to temporary filesystem", e);
        }
    }
}
