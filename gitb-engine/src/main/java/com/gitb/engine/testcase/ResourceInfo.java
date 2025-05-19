package com.gitb.engine.testcase;

import java.util.Optional;

public record ResourceInfo(Optional<String> testSuiteId, String resourcePath) {
}
