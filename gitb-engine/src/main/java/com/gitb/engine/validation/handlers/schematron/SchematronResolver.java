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

import com.gitb.engine.ModuleManager;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.repository.ITestCaseRepository;
import com.helger.io.resource.IReadableResource;
import com.helger.schematron.resolve.ISchematronIncludeResolver;
import org.jspecify.annotations.NonNull;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Resolver of the resources referenced by Schematron rules. It serves only artifacts of the test suite, both for
 * XSLT-based Schematron (as a {@link URIResolver}) and for "pure" Schematron (as an {@link ISchematronIncludeResolver}).
 * Any other reference (e.g. a remote URL) is rejected.
 * <p/>
 * Created by senan on 30.10.2014.
 */
public class SchematronResolver implements URIResolver, ISchematronIncludeResolver {

    private static final String PROTOCOL = "file:///";

    /**
     * Path to the folder that contains root resource (i.e. and XSD schema or Schematron file, etc)
     */
    private final String resource;
    private final String testSuiteId;
    private final String testCaseId;
    private String rejectedReference;

    public SchematronResolver(String testSuiteId, String testCaseId, String path) {
        this.testSuiteId = testSuiteId;
        this.testCaseId = testCaseId;
        this.resource = path;
    }

    /**
     * Get the message to report in case a reference was rejected during resolution. Rejections can otherwise be
     * hidden by the Schematron processing (e.g. reported as a generic invalid Schematron file).
     *
     * @return The message, or null if no reference was rejected.
     */
    public String getRejectionMessage() {
        if (rejectedReference == null) {
            return null;
        }
        return "Loading of referenced resource [%s] was blocked.".formatted(rejectedReference);
    }

    /**
     * Check whether the provided reference starts with a URI scheme (i.e. has a colon before any slash), meaning
     * that it cannot be a relative path within the test suite.
     *
     * @param href The reference to check.
     * @return The check result.
     */
    private static boolean hasUriScheme(String href) {
        int colonIndex = href.indexOf(':');
        int slashIndex = href.indexOf('/');
        return colonIndex >= 0 && (slashIndex < 0 || colonIndex < slashIndex);
    }

    /**
     * Reject the provided reference if it is not a path within the test suite (e.g. a remote URL).
     *
     * @param href The reference to check.
     */
    private void rejectIfNotTestSuitePath(String href) {
        if (href != null && hasUriScheme(href)) {
            rejectedReference = href;
            throw new GITBEngineInternalError(getRejectionMessage());
        }
    }

    /**
     * Get the folder within the test suite of the root Schematron, against which references are resolved.
     *
     * @return The folder path (with a trailing slash if not empty).
     */
    private String rootFolder() {
        return this.resource.substring(0, this.resource.lastIndexOf("/")+1);
    }

    /**
     * Resolve an included resource of "pure" Schematron. Only artifacts of the test suite are served, resolved
     * relative to the root Schematron. A missing artifact is an error (never a fall-back to the file system).
     *
     * @param href The reference to resolve.
     * @return The resolved resource.
     * @throws IOException If the artifact cannot be read.
     */
    @Override
    @NonNull
    public IReadableResource getResolvedSchematronResource(@NonNull String href) throws IOException {
        rejectIfNotTestSuitePath(href);
        if (this.resource == null) {
            throw new GITBEngineInternalError("Referenced resource [%s] cannot be resolved because the Schematron rules are not part of the test suite.".formatted(href));
        }
        String artifactPath = rootFolder() + href;
        ITestCaseRepository repository = ModuleManager.getInstance().getTestCaseRepository();
        try (InputStream stream = repository.getTestArtifact(testSuiteId, testCaseId, artifactPath)) {
            if (stream == null) {
                throw new GITBEngineInternalError("Referenced artifact [%s] could not be found in the test suite.".formatted(artifactPath));
            }
            return new StringResource(new String(stream.readAllBytes(), StandardCharsets.UTF_8), artifactPath);
        }
    }

    @Override
    public Source resolve(String href, String baseURI) {
        rejectIfNotTestSuitePath(href);
        ModuleManager moduleManager = ModuleManager.getInstance();
        ITestCaseRepository repository = moduleManager.getTestCaseRepository();
        String parentFolder;
        if (baseURI == null || baseURI.isBlank()) {
            parentFolder = rootFolder();
        } else {
            parentFolder = baseURI.substring(PROTOCOL.length(), baseURI.lastIndexOf("/")+1);
        }

        String artifactPath = parentFolder + href;

        InputStream resource  = repository.getTestArtifact(testSuiteId, testCaseId, artifactPath);
        if(resource != null) {
            return new StreamSource(resource);
        }
        return null;
    }
}
