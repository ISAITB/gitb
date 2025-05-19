package com.gitb.engine.validation.handlers.json;

import com.gitb.engine.ModuleManager;
import com.gitb.engine.testcase.ResourceInfo;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.repository.ITestCaseRepository;
import com.google.gson.JsonParser;
import com.networknt.schema.AbsoluteIri;
import com.networknt.schema.resource.InputStreamSource;
import com.networknt.schema.resource.SchemaLoader;
import com.networknt.schema.resource.UriSchemaLoader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class LocalSchemaResolver implements SchemaLoader {

    private static final Logger LOG = LoggerFactory.getLogger(LocalSchemaResolver.class);

    private final UriSchemaLoader uriSchemaLoader = new UriSchemaLoader();
    private final ITestCaseRepository repository = ModuleManager.getInstance().getTestCaseRepository();
    private final TestCaseContext testCaseContext;
    private final SharedSchemaInfo sharedSchemaInfo;
    private final Supplier<Map<String, ResourceInfo>> cacheLoader;

    public LocalSchemaResolver(SharedSchemaInfo sharedSchemaInfo, TestCaseContext testCaseContext) {
        this.testCaseContext = testCaseContext;
        this.sharedSchemaInfo = sharedSchemaInfo;
        this.cacheLoader = createCacheLoader(sharedSchemaInfo);
    }

    @Override
    public InputStreamSource getSchema(AbsoluteIri absoluteIri) {
        String idToCheck = schemaIdToUse(absoluteIri.toString());
        Optional<ResourceInfo> schemaInfo = testCaseContext.getCachedResource(idToCheck, cacheLoader);
        if (schemaInfo.isEmpty()) {
            LOG.debug("Schema with URI {} not found locally. Looking up remotely.", absoluteIri);
            return uriSchemaLoader.getSchema(absoluteIri);
        } else {
            LOG.debug("Schema with URI {} found locally.", absoluteIri);
            return () -> repository.getTestArtifact(schemaInfo.get().testSuiteId().orElse(null), sharedSchemaInfo.testCaseId(), schemaInfo.get().resourcePath());
        }
    }

    private String schemaIdToUse(String uri) {
        return StringUtils.appendIfMissing(uri, "#");
    }

    private Supplier<Map<String, ResourceInfo>> createCacheLoader(SharedSchemaInfo sharedSchemaInfo) {
        return () -> {
            Map<String, ResourceInfo> entries = new HashMap<>();
            sharedSchemaInfo.schemaPaths().forEach(resourcePath -> {
                InputStream resourceStream  = repository.getTestArtifact(sharedSchemaInfo.testSuiteId().orElse(null), sharedSchemaInfo.testCaseId(), resourcePath);
                if (resourceStream != null) {
                    String schemaId = readSchemaId(sharedSchemaInfo.testSuiteId(), resourcePath, resourceStream);
                    if (schemaId != null) {
                        entries.put(schemaIdToUse(schemaId), new ResourceInfo(sharedSchemaInfo.testSuiteId(), resourcePath));
                    }
                } else {
                    throw new IllegalStateException("Unable to find shared schema at path [%s]%s".formatted(resourcePath, testSuiteReferenceForError(sharedSchemaInfo.testSuiteId())));
                }
            });
            return entries;
        };
    }

    private String readSchemaId(Optional<String> fromTestSuite, String resourcePath, InputStream schemaStream) {
        try (var fileReader = new InputStreamReader(schemaStream)) {
            var json = JsonParser.parseReader(fileReader).getAsJsonObject();
            if (json.has("$id")) {
                return json.get("$id").getAsString();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected error while processing shared schema at path [%s]%s".formatted(resourcePath, testSuiteReferenceForError(fromTestSuite)), e);
        }
        return null;
    }

    private String testSuiteReferenceForError(Optional<String> fromTestSuite) {
        return fromTestSuite.map(" from test suite [%s]"::formatted).orElse("");
    }

}
