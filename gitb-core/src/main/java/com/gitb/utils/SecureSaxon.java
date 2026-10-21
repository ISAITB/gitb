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

package com.gitb.utils;

import com.gitb.exceptions.GITBEngineInternalError;
import net.sf.saxon.Configuration;
import net.sf.saxon.jaxp.SaxonTransformerFactory;
import net.sf.saxon.lib.CollectionFinder;
import net.sf.saxon.lib.EnvironmentVariableResolver;
import net.sf.saxon.lib.Feature;
import net.sf.saxon.lib.ResourceRequest;
import net.sf.saxon.lib.ResourceResolver;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.xpath.XPathFactoryImpl;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathFactory;
import java.net.URI;
import java.util.Set;

/**
 * Factory for Saxon processors (XPath, XSLT) that cannot be used to reach resources the engine should not reach.
 * <p>
 * XPath expressions and stylesheets are authored by test case designers and may be applied to data provided by users,
 * so a default Saxon configuration is not acceptable: functions such as {@code doc()}, {@code unparsed-text()},
 * {@code json-doc()} or {@code collection()} could read local files, and {@code environment-variable()} would expose the
 * engine's secrets. The configurations created here:
 * <ul>
 *     <li>Only load HTTP(S) resources, subject to the {@link UriAuthorizer#global() URI authorizer} (including redirects).
 *     All other schemes (e.g. {@code file:}, {@code jar:}, {@code classpath:}) are rejected.</li>
 *     <li>Do not support collections ({@code collection()} and {@code uri-collection()}).</li>
 *     <li>Hide the environment variables.</li>
 *     <li>Disallow extension functions (which also disables {@code xsl:result-document}, so stylesheets cannot write documents).</li>
 * </ul>
 * A new configuration is created for each factory (as Saxon does by default), the resolvers used are stateless.
 */
public final class SecureSaxon {

    private static final ResourceResolver RESOURCE_RESOLVER = new AuthorizingResourceResolver();
    private static final CollectionFinder COLLECTION_FINDER = (context, collectionUri) -> {
        throw new XPathException("Collections are not supported (requested collection [%s]).".formatted(collectionUri));
    };
    private static final EnvironmentVariableResolver ENVIRONMENT_VARIABLE_RESOLVER = new EnvironmentVariableResolver() {
        @Override
        public Set<String> getAvailableEnvironmentVariables() {
            return Set.of();
        }

        @Override
        public String getEnvironmentVariable(String name) {
            return null;
        }
    };
    private SecureSaxon() {}

    /**
     * Create a new restricted Saxon configuration.
     *
     * @return The configuration.
     */
    public static Configuration newConfiguration() {
        Configuration configuration = Configuration.newConfiguration();
        restrict(configuration);
        configuration.setResourceResolver(RESOURCE_RESOLVER);
        return configuration;
    }

    /**
     * Apply the restrictions (other than the resolution of resources) to an existing configuration.
     *
     * @param configuration The configuration to restrict.
     */
    private static void restrict(Configuration configuration) {
        configuration.setConfigurationProperty(Feature.ALLOW_EXTERNAL_FUNCTIONS, false);
        configuration.setCollectionFinder(COLLECTION_FINDER);
        configuration.setConfigurationProperty(Feature.ENVIRONMENT_VARIABLE_RESOLVER, ENVIRONMENT_VARIABLE_RESOLVER);
    }

    /**
     * Restrict a Saxon transformer factory that was created by a third-party library (e.g. for XSLT-based Schematron).
     * <p>
     * The resolution of resources is not changed as the library is expected to set a resolver of its own. It must
     * itself ensure that this resolver does not load resources that should not be reachable.
     *
     * @param factory The factory to restrict.
     * @throws IllegalStateException If the factory is not a Saxon factory (in which case it cannot be restricted).
     */
    public static void restrict(TransformerFactory factory) {
        if (factory instanceof SaxonTransformerFactory saxonFactory) {
            restrict(saxonFactory.getConfiguration());
        } else {
            throw new IllegalStateException("Unable to restrict the transformer factory (unexpected type [%s])".formatted(factory.getClass().getName()));
        }
    }

    /**
     * @return A new XPath factory with a restricted configuration.
     */
    public static XPathFactory newXPathFactory() {
        return new XPathFactoryImpl(newConfiguration());
    }

    /**
     * @return A new transformer factory with a restricted configuration.
     */
    public static TransformerFactory newTransformerFactory() {
        var factory = new SaxonTransformerFactory(newConfiguration());
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return factory;
    }

    /**
     * @return A new Saxon processor (s9api) with a restricted configuration.
     */
    public static Processor newProcessor() {
        return new Processor(newConfiguration());
    }

    /**
     * Resource resolver that only loads HTTP(S) resources, subject to authorization. It never returns null (which would
     * let Saxon fall back to its default resolution).
     */
    private static final class AuthorizingResourceResolver implements ResourceResolver {

        @Override
        public Source resolve(ResourceRequest request) throws XPathException {
            URI uri = toUri(request);
            var authorizer = UriAuthorizer.global();
            try {
                authorizer.check(uri);
                var response = GuardedUriReader.open(uri, null, authorizer);
                try {
                    // The stream is handed over to Saxon, that closes it once read.
                    String systemId = response.uri().toString();
                    if (ResourceRequest.TEXT_NATURE.equals(request.nature) || ResourceRequest.BINARY_NATURE.equals(request.nature)) {
                        var source = new StreamSource(response.stream());
                        source.setSystemId(systemId);
                        return source;
                    } else {
                        // Parse XML with the hardened parser (no DOCTYPE, no external entities).
                        var source = XMLUtils.getSecureSaxSource(response.stream());
                        source.setSystemId(systemId);
                        return source;
                    }
                } catch (RuntimeException e) {
                    response.closeQuietly();
                    throw e;
                }
            } catch (GITBEngineInternalError e) {
                throw new XPathException(e.getMessage(), e);
            }
        }

        private URI toUri(ResourceRequest request) throws XPathException {
            try {
                URI uri;
                if (request.uri != null) {
                    uri = new URI(request.uri);
                } else if (request.relativeUri != null) {
                    uri = request.baseUri == null ? new URI(request.relativeUri) : new URI(request.baseUri).resolve(request.relativeUri);
                } else {
                    throw new XPathException("A resource was requested without specifying its URI.");
                }
                return uri;
            } catch (java.net.URISyntaxException e) {
                throw new XPathException("Invalid URI [%s]".formatted(request.uri != null ? request.uri : request.relativeUri), e);
            }
        }
    }

}
