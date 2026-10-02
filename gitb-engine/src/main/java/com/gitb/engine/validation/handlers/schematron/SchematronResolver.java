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

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;

/**
 * Created by senan on 30.10.2014.
 */
public class SchematronResolver implements URIResolver {

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

    @Override
    public Source resolve(String href, String baseURI) {
        if (href != null && hasUriScheme(href)) {
            // Not a path within the test suite (e.g. a remote URL).
            rejectedReference = href;
            throw new GITBEngineInternalError(getRejectionMessage());
        }
        ModuleManager moduleManager = ModuleManager.getInstance();
        ITestCaseRepository repository = moduleManager.getTestCaseRepository();
        String parentFolder;
        if (baseURI == null || baseURI.isBlank()) {
            parentFolder = this.resource.substring(0, this.resource.lastIndexOf("/")+1);
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
