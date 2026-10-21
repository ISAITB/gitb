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

package com.gitb.engine.utils.rdf;

import com.gitb.utils.UriAuthorizer;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.op.OpService;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.service.ServiceExecutorRegistry;
import org.apache.jena.sparql.service.single.ChainingServiceExecutor;
import org.apache.jena.sparql.service.single.ServiceExecutor;
import org.apache.jena.sys.JenaSystem;

/**
 * Executor for SPARQL SERVICE calls that authorizes the targeted endpoint before any call is made.
 * <p>
 * SHACL shapes (SPARQL constraints) and SPARQL queries may include SERVICE clauses that call out to arbitrary
 * endpoints. This would let their authors trigger requests to arbitrary locations, including internal ones. The
 * HTTP client used for SERVICE calls (see {@link RdfSecurity}) never follows redirects, so an authorized endpoint
 * cannot be used to reach one that is not.
 */
public class AuthorizingServiceExecutor implements ChainingServiceExecutor {

    // Service identifiers that refer to the dataset being queried, and not to a remote location.
    private static final String INTERNAL_SERVICE_PREFIX = "urn:x-arq:";

    /**
     * Register the executor to be consulted before any other executor. Calling this multiple times is safe.
     */
    public static void install() {
        // Jena must be initialised before any of its classes are used.
        JenaSystem.init();
        var registry = ServiceExecutorRegistry.get();
        synchronized (AuthorizingServiceExecutor.class) {
            if (registry.getSingleChain().stream().noneMatch(AuthorizingServiceExecutor.class::isInstance)) {
                registry.addSingleLink(new AuthorizingServiceExecutor());
            }
        }
    }

    @Override
    public QueryIterator createExecution(OpService opExecute, OpService opOriginal, Binding binding, ExecutionContext execCxt, ServiceExecutor chain) {
        Node service = opExecute.getService();
        if (service != null && service.isURI() && !service.getURI().startsWith(INTERNAL_SERVICE_PREFIX)) {
            UriAuthorizer.global().check(service.getURI());
        } else if (service != null && !service.isURI() && !service.isVariable()) {
            // Anything other than a URI cannot be a valid service.
            throw new com.gitb.utils.UriNotAllowedException("The SPARQL service [%s] is not allowed.".formatted(service));
        }
        return chain.createExecution(opExecute, opOriginal, binding, execCxt);
    }

}
