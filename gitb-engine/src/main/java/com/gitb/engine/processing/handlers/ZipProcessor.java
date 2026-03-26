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

package com.gitb.engine.processing.handlers;

import com.gitb.core.*;
import com.gitb.engine.processing.ProcessingHandler;
import com.gitb.engine.utils.HandlerUtils;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.ProcessingModule;
import com.gitb.tr.TestResultType;
import com.gitb.types.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.gitb.engine.processing.handlers.ZipProcessor.HANDLER_NAME;

@ProcessingHandler(name=HANDLER_NAME)
public class ZipProcessor extends AbstractProcessingHandler  {

    static final String HANDLER_NAME = "ZipProcessor";
    private static final Logger LOG = LoggerFactory.getLogger(ZipProcessor.class);
    private static final String OPERATION_INITIALIZE = "initialize";
    private static final String OPERATION_EXTRACT = "extract";
    private static final String INPUT_ZIP = "zip";
    private static final String INPUT_CASE = "case";
    private static final String INPUT_PATH = "path";
    private static final String INPUT_MATCH_TYPE = "match";
    private static final String OUTPUT_OUTPUT = "output";

    @Override
    protected ProcessingModule createProcessingModule() {
        ProcessingModule module = new ProcessingModule();
        module.setId(HANDLER_NAME);
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());
        module.getOperation().add(createProcessingOperation(OPERATION_INITIALIZE,
                Collections.singletonList(
                        createParameter(INPUT_ZIP, "binary", UsageEnumeration.R, ConfigurationType.SIMPLE, "The ZIP file to extract from.")
                ),
                Collections.singletonList(
                        createParameter(OUTPUT_OUTPUT, "map", UsageEnumeration.R, ConfigurationType.SIMPLE, "The archive's entry information.")
                )
        ));
        module.getOperation().add(createProcessingOperation(OPERATION_EXTRACT,
                List.of(
                        createParameter(INPUT_PATH, "list[string]", UsageEnumeration.R, ConfigurationType.SIMPLE, "The path pattern used for extraction."),
                        createParameter(INPUT_CASE, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, "Whether the path matching should be case sensitive (true, false). Default is false."),
                        createParameter(INPUT_MATCH_TYPE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The type of matching to perform on the path ('exact', 'regexp'). Default is 'regexp'.")
                ),
                Collections.singletonList(
                        createParameter(OUTPUT_OUTPUT, "map", UsageEnumeration.R, ConfigurationType.SIMPLE, "The extracted entries.")
                )
        ));
        return module;
    }

    @Override
    public ProcessingReport process(String processingSessionId, String operation, ProcessingData input) {
        String testSessionId = getAndConvert(input.getData(), HandlerUtils.SESSION_INPUT, DataType.STRING_DATA_TYPE, StringType.class).getValue();
        if (testSessionId.equals(processingSessionId)) {
            // If we have gone through the beginTransaction step, this identifier will always differ.
            LOG.warn(MarkerFactory.getDetachedMarker(testSessionId), "When using the %s you should carry out processing operations within a transaction.".formatted(HANDLER_NAME));
        }
        if (operation == null) operation = OPERATION_EXTRACT;
        ProcessingData data = new ProcessingData();
        try {
            switch (operation) {
                case OPERATION_INITIALIZE -> initializeArchive(processingSessionId, testSessionId, input, data);
                case OPERATION_EXTRACT -> extractFromArchive(processingSessionId, testSessionId, input, data);
                default -> throw new IllegalArgumentException("Operation [%s] is not valid".formatted(operation));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unexpected error while processing ZIP archive", e);
        }
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

    private void extractFromArchive(String processingSessionId, String testSessionId, ProcessingData input, ProcessingData output) throws IOException {
        Path archiveRoot = archivePath(processingSessionId, testSessionId);
        if (Files.notExists(archiveRoot)) {
            throw new IllegalStateException("No archive found to extract from. Make sure you first call the [%s] operation to load the archive.".formatted(OPERATION_INITIALIZE));
        }
        MatchType matchType = Optional.ofNullable(getAndConvert(input.getData(), INPUT_MATCH_TYPE, DataType.STRING_DATA_TYPE, StringType.class)).map(x -> MatchType.forValue(x.getValue())).orElse(MatchType.REGEXP);
        boolean caseSensitive = Optional.ofNullable(getAndConvert(input.getData(), INPUT_CASE, DataType.BOOLEAN_DATA_TYPE, BooleanType.class)).map(BooleanType::getValue).orElse(false);
        List<String> paths = Optional.ofNullable(getAndConvert(input.getData(), INPUT_PATH, DataType.LIST_DATA_TYPE, ListType.class)).map(x -> x.getElements().stream().map(y -> (String) y.convertTo(DataType.STRING_DATA_TYPE).getValue()).toList()).orElseGet(ArrayList::new);
        List<DataType> entryList = new ArrayList<>();
        byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(archiveRoot))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory() && entryMatches(entry, paths, caseSensitive, matchType)) {
                    int len;
                    byte[] entryBytes;
                    try (var bos = new ByteArrayOutputStream()) {
                        while ((len = zis.read(buffer)) > 0) {
                            bos.write(buffer, 0, len);
                        }
                        bos.flush();
                        entryBytes = bos.toByteArray();
                    }
                    MapType entryMap = new MapType();
                    entryMap.addItem("path", new StringType(entry.getName()));
                    entryMap.addItem("content", new BinaryType(entryBytes));
                    entryList.add(entryMap);
                }
                entry = zis.getNextEntry();
            }
        }
        if (!entryList.isEmpty()) {
            output.addInput("entry", new ListType(entryList));
        }
        output.addInput("entries", new NumberType(entryList.size()));
        output.addInput("matched", new BooleanType(!entryList.isEmpty()));
    }

    private boolean entryMatches(ZipEntry zipEntry, List<String> paths, boolean caseSensitive, MatchType matchType) {
        if (matchType == MatchType.EXACT) {
            if (caseSensitive) {
                return paths.stream().anyMatch(path -> Strings.CS.equals(path, zipEntry.getName()));
            } else {
                return paths.stream().anyMatch(path -> Strings.CI.equals(path, zipEntry.getName()));
            }
        } else {
            return paths.stream().anyMatch(path -> {
                String pattern = caseSensitive ? path : "(?i)" + path;
                return zipEntry.getName().matches(pattern);
            });
        }
    }

    private void initializeArchive(String processingSessionId, String testSessionId, ProcessingData input, ProcessingData output) throws IOException {
        BinaryType zipData = getAndConvert(input.getData(), INPUT_ZIP, DataType.BINARY_DATA_TYPE, BinaryType.class);
        Path archiveRoot = archivePath(processingSessionId, testSessionId);
        FileUtils.deleteQuietly(archiveRoot.toFile());
        Files.createDirectories(archiveRoot.getParent());
        // Write the file on disk at a temporary location.
        Files.write(archiveRoot, zipData.getValue(), StandardOpenOption.CREATE);
        // Read the file entries.
        List<DataType> entryPaths = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(archiveRoot))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    entryPaths.add(new StringType(entry.getName()));
                }
                entry = zis.getNextEntry();
            }
        }
        output.addInput("entries", new NumberType(entryPaths.size()));
        output.addInput("entryPaths", new ListType(DataType.STRING_DATA_TYPE, entryPaths));
    }

    @Override
    public String beginTransaction(String stepId, List<Configuration> config) {
        /*
         * Generate a transaction-specific session identifier as we may have parallel transactions in a single test session
         * and we will need to refer to the session ID to retrieve the archive data.
         */
        return UUID.randomUUID().toString();
    }

    private Path archivePath(String processingSessionId, String testSessionId) {
        return getScope(testSessionId).getContext().getDataFolder().resolve("zip-processor").resolve(processingSessionId);
    }

    private enum MatchType {

        EXACT("exact"),
        REGEXP("regexp");

        private final String value;

        MatchType(String regexp) {
            this.value = regexp;
        }

        public static MatchType forValue(String value) {
            if (EXACT.value.equals(value)) {
                return EXACT;
            } else if (REGEXP.value.equals(value)) {
                return REGEXP;
            } else {
                throw new IllegalArgumentException("Unexpected value for [%s] input. Expected %s or %s.".formatted(INPUT_MATCH_TYPE, EXACT.value, REGEXP.value));
            }
        }

    }
}
