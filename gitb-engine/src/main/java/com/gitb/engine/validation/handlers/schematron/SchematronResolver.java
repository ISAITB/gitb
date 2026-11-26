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
import com.gitb.engine.utils.HandlerUtils;
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
import java.net.URI;
import java.net.URISyntaxException;
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
     * Path used for the system ID of Schematron rules that are not part of the test suite.
     */
    private static final String DEFAULT_SYSTEM_ID_PATH = "schematron.xslt";

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
     * Reject the provided reference if it is not a path within the test suite (e.g. a remote URL).
     *
     * @param href The reference to check.
     */
    private void rejectIfNotTestSuitePath(String href) {
        if (href != null && HandlerUtils.hasUriScheme(href)) {
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

    /**
     * Get the system ID to set on the XSLT-based Schematron when compiling it, so that the references it makes
     * (e.g. via {@code document()} or {@code xsl:include}) are resolved relative to its location within the test suite.
     * <p/>
     * The system ID must be absolute and independent of the environment. If a relative one is set, Saxon makes it
     * absolute against the working directory of the process, which is then used as the base of all references.
     *
     * @return The system ID.
     */
    public String getSystemId() {
        String path = (this.resource == null || this.resource.isBlank()) ? DEFAULT_SYSTEM_ID_PATH : this.resource;
        try {
            // The path is encoded as needed (e.g. for spaces) and decoded again in resolve.
            return new URI("file", "", "/" + path, null).toString();
        } catch (URISyntaxException e) {
            return PROTOCOL + path;
        }
    }

    /**
     * Get the folder within the test suite against which a reference is resolved.
     *
     * @param baseURI The base URI of the reference (as given by the XSLT processor).
     * @return The folder path (with a trailing slash if not empty).
     */
    private String baseFolder(String baseURI) {
        if (baseURI != null && baseURI.startsWith(PROTOCOL)) {
            // A base that we set (see getSystemId), directly or via a module that was resolved relative to it.
            try {
                String path = new URI(baseURI).getPath();
                if (path != null && path.startsWith("/")) {
                    return path.substring(1, path.lastIndexOf("/") + 1);
                }
            } catch (URISyntaxException e) {
                // Use the root folder.
            }
        }
        return rootFolder();
    }

    @Override
    public Source resolve(String href, String baseURI) {
        rejectIfNotTestSuitePath(href);
        if (this.resource == null) {
            throw new GITBEngineInternalError("Referenced resource [%s] cannot be resolved because the Schematron rules are not part of the test suite.".formatted(href));
        }
        ITestCaseRepository repository = ModuleManager.getInstance().getTestCaseRepository();
        String artifactPath = baseFolder(baseURI) + href;
        InputStream stream = repository.getTestArtifact(testSuiteId, testCaseId, artifactPath);
        if (stream == null) {
            // Never return null as the XSLT processor would then try to resolve the reference by itself (e.g. from the file system).
            throw new GITBEngineInternalError("Referenced artifact [%s] could not be found in the test suite.".formatted(artifactPath));
        }
        return new StreamSource(stream);
    }
}
