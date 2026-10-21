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

package com.gitb.engine.validation.handlers.xsd;

import com.gitb.engine.ModuleManager;
import com.gitb.engine.utils.HandlerUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.repository.ITestCaseRepository;
import com.gitb.utils.GuardedUriReader;
import com.gitb.utils.UriAuthorizer;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by senan on 10/10/14.
 */
public class XSDResolver implements LSResourceResolver {

    private static final String PROTOCOL = "file:///";

    /**
     * Path to the folder that contains root resource (i.e. and XSD schema or Schematron file, etc)
     */
    private final String resource;
    private final String testCaseId;
    private final String testSuiteId;

    public XSDResolver(String testSuiteId, String testCaseId, String path) {
        this.resource = path;
        this.testCaseId = testCaseId;
        this.testSuiteId = testSuiteId;
    }

    /**
     * Resolve a resource referenced by a schema (e.g. an import or include).
     * <p>
     * The resource can either be an artifact of the test suite (a relative reference), or a remote resource (an absolute
     * HTTP(S) reference or a reference relative to a schema that was itself loaded remotely). Remote resources are
     * subject to authorization (internal addresses can only be accessed if whitelisted). References of any other kind
     * (e.g. a file) are rejected.
     * <p>
     * This method never returns null for a reference with a location, as this would make the parser attempt to load the
     * resource itself.
     */
    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        if (systemId == null) {
            // No location was provided (e.g. an import with only a namespace) so there is nothing that could be loaded.
            return null;
        }
        URI remoteUri = determineRemoteUri(systemId, baseURI);
        if (remoteUri != null) {
            return loadRemoteResource(publicId, remoteUri);
        }
        if (HandlerUtils.hasUriScheme(systemId)) {
            throw new GITBEngineInternalError("Loading of referenced resource [%s] was blocked. Only http and https resources, or resources of the test suite, can be referenced.".formatted(systemId));
        }
        return loadTestSuiteResource(publicId, systemId, baseURI);
    }

    /**
     * Determine whether the reference is to a remote resource.
     *
     * @param systemId The reference.
     * @param baseURI The base against which a relative reference is resolved.
     * @return The URI of the remote resource, or null if the reference is not to a remote resource.
     */
    private URI determineRemoteUri(String systemId, String baseURI) {
        try {
            if (HandlerUtils.hasUriScheme(systemId)) {
                URI uri = new URI(systemId);
                return isHttp(uri) ? uri : null;
            } else if (baseURI != null) {
                URI base = new URI(baseURI);
                if (isHttp(base)) {
                    // A relative reference from a schema that was loaded remotely.
                    return base.resolve(systemId);
                }
            }
            return null;
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw new GITBEngineInternalError("Referenced resource [%s] is not a valid reference.".formatted(systemId), e);
        }
    }

    private boolean isHttp(URI uri) {
        return "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
    }

    private LSInput loadRemoteResource(String publicId, URI uri) {
        // The URI (and any redirects) are authorized, throwing an exception if not allowed.
        try (var response = GuardedUriReader.open(uri, null, UriAuthorizer.global())) {
            byte[] content = response.stream().readAllBytes();
            /*
             * The final location is set as the system ID to resolve any relative references of this resource against it
             * (that are expected to also be remote).
             */
            String finalLocation = response.uri().toString();
            return new Input(publicId, finalLocation, finalLocation, new ByteArrayInputStream(content));
        } catch (IOException e) {
            throw new GITBEngineInternalError("Unable to read referenced resource [%s]".formatted(uri), e);
        }
    }

    private LSInput loadTestSuiteResource(String publicId, String systemId, String baseURI) {
        ITestCaseRepository repository = ModuleManager.getInstance().getTestCaseRepository();
        String parentFolder;
        if (baseURI == null) {
            if (resource == null) {
                throw new GITBEngineInternalError("Referenced resource [%s] cannot be resolved because the schema is not part of the test suite.".formatted(systemId));
            }
            parentFolder = resource.substring(0, resource.lastIndexOf("/")+1);
        } else {
            parentFolder = baseURI.substring(PROTOCOL.length(), baseURI.lastIndexOf("/")+1);
        }
        String artifactPath = parentFolder + systemId;
        InputStream resource  = repository.getTestArtifact(testSuiteId, testCaseId, artifactPath);
        if(resource != null) {
            // the "/" prevents the system to add the path where mvn command executed.
            String baseUriToSet = parentFolder;
            if (!parentFolder.startsWith("/")) {
                baseUriToSet = "/" + baseUriToSet;
            }
            return new Input(publicId, systemId, baseUriToSet, resource);
        }
        throw new GITBEngineInternalError("Referenced resource [%s] could not be found in the test suite.".formatted(artifactPath));
    }

    public static class Input implements LSInput {

        private String publicId;

        private String systemId;

        private String baseURI;

        public String getPublicId() {
            return publicId;
        }

        public void setPublicId(String publicId) {
            this.publicId = publicId;
        }

        public String getBaseURI() {
            return baseURI;
        }

        public InputStream getByteStream() {
            return null;
        }

        public boolean getCertifiedText() {
            return false;
        }

        public Reader getCharacterStream() {
            return null;
        }

        public String getEncoding() {
            return null;
        }

        public String getStringData() {
            synchronized (inputStream) {
                try {
                    byte[] input = new byte[inputStream.available()];
                    int readCount = inputStream.read(input);
                    if (readCount > 0) {
                        return new String(input);
                    } else {
                        return "";
                    }
                } catch (IOException e) {
                    throw new GITBEngineInternalError(e);
                }
            }
        }

        public void setBaseURI(String baseURI) {
            this.baseURI = baseURI;
        }

        public void setByteStream(InputStream byteStream) {
            // Do nothing.
        }

        public void setCertifiedText(boolean certifiedText) {
            // Do nothing.
        }

        public void setCharacterStream(Reader characterStream) {
            // Do nothing.
        }

        public void setEncoding(String encoding) {
            // Do nothing.
        }

        public void setStringData(String stringData) {
            // Do nothing.
        }

        public String getSystemId() {
            return systemId;
        }

        public void setSystemId(String systemId) {
            this.systemId = systemId;
        }

        private final BufferedInputStream inputStream;

        public Input(String publicId, String sysId, String baseUri, InputStream input) {
            this.publicId = publicId;
            this.systemId = sysId;
            this.baseURI  = baseUri;
            this.inputStream = new BufferedInputStream(input);
        }
    }
}
