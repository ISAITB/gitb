package com.gitb.engine.validation.handlers.json;

import java.util.List;
import java.util.Optional;

public record SharedSchemaInfo(Optional<String> testSuiteId, String testCaseId, List<String> schemaPaths) {
}
