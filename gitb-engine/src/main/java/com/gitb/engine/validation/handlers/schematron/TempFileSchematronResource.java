/*
 * Copyright (C) 2026 European Union
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

package com.gitb.engine.validation.handlers.schematron;

import com.helger.io.resource.FileSystemResource;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * An {@code IReadableResource} for Schematron content, backed by a genuine, short-lived temporary file rather
 * than held purely in memory (unlike {@link StringResource}) - needed specifically for
 * {@link com.helger.schematron.pure.SchematronResourcePure} (the "pure" Schematron engine).
 * <p/>
 * Instances must be {@linkplain #close() closed} once validation has completed, to remove the backing temporary
 * file.
 */
class TempFileSchematronResource extends FileSystemResource implements AutoCloseable {

    private final String logicalPath;
    private final Path tempFile;

    /**
     * Constructor.
     *
     * @param content The Schematron content to write to the backing temporary file.
     * @param logicalPath The schema's own logical path (e.g. its test-suite-relative import path), reported by
     *   {@link #getPath()}/{@link #getResourceID()} instead of the temporary file's own path.
     */
    TempFileSchematronResource(String content, String logicalPath) {
        super(createTempFile(content));
        this.logicalPath = logicalPath;
        this.tempFile = getAsFile().toPath();
    }

    private static File createTempFile(String content) {
        try {
            Path tempFile = Files.createTempFile("schematron", ".sch");
            Files.writeString(tempFile, content, StandardCharsets.UTF_8);
            return tempFile.toFile();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create a temporary file for Schematron content", e);
        }
    }

    @Override
    public String getPath() {
        return logicalPath;
    }

    @Override
    public String getResourceID() {
        return logicalPath;
    }

    /**
     * Delete the backing temporary file. Best-effort: a failure to delete is not worth failing validation over,
     * so it is silently ignored (the file is created under the JVM's standard temporary directory, which is
     * cleaned up by the OS regardless).
     */
    @Override
    public void close() {
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            // Best-effort cleanup only - see the Javadoc above.
        }
    }

}
