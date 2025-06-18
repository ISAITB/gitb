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

package com.gitb.reports;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class ReportSpecs {

    private Function<String, String> resourceResolver;
    private boolean includeContextItems = true;
    private boolean includeLogs = true;
    private boolean includeDocumentation = true;
    private boolean includeTestSteps = true;
    private int contextItemTruncateLimit = 1000;
    private final Set<String> mimeTypesToConvertToStrings = new HashSet<>();
    private Path tempFolderPath;

    private ReportSpecs() {}

    public static ReportSpecs build() {
        return new ReportSpecs();
    }

    public ReportSpecs withTempFolder(Path tempFolderPath) {
        this.tempFolderPath = tempFolderPath;
        return this;
    }

    public ReportSpecs withResourceResolver(Function<String, String> resolver) {
        this.resourceResolver = resolver;
        return this;
    }

    public ReportSpecs withContextItems(boolean include) {
        this.includeContextItems = include;
        return this;
    }

    public ReportSpecs withTestSteps(boolean include) {
        this.includeTestSteps = include;
        return this;
    }

    public ReportSpecs withDocumentation(boolean include) {
        this.includeDocumentation = include;
        return this;
    }

    public ReportSpecs withLogs(boolean include) {
        this.includeLogs = include;
        return this;
    }

    public ReportSpecs withContextItemTruncateLimit(int limit) {
        this.contextItemTruncateLimit = limit;
        return this;
    }

    public ReportSpecs withMimeTypesToConvertToStrings(Collection<String> mimeTypes) {
        mimeTypesToConvertToStrings.addAll(mimeTypes);
        return this;
    }

    public Function<String, String> getResourceResolver() {
        return resourceResolver;
    }

    public boolean isIncludeContextItems() {
        return includeContextItems;
    }

    public boolean isIncludeLogs() {
        return includeLogs;
    }

    public int getContextItemTruncateLimit() {
        return contextItemTruncateLimit;
    }

    public Set<String> getMimeTypesToConvertToStrings() {
        return mimeTypesToConvertToStrings;
    }

    public boolean isIncludeDocumentation() {
        return includeDocumentation;
    }

    public boolean isIncludeTestSteps() {
        return includeTestSteps;
    }

    public Path getTempFolderPath() {
        return tempFolderPath;
    }
}
