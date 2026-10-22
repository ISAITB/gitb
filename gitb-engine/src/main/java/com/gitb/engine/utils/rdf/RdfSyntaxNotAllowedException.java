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

import com.gitb.exceptions.GITBEngineInternalError;

/**
 * Exception raised when RDF content is not processed because its syntax is not accepted by the test engine's
 * configuration (see {@link RdfContentRestrictions}).
 */
public class RdfSyntaxNotAllowedException extends GITBEngineInternalError {

    /**
     * Constructor.
     *
     * @param contentType The content type that is not allowed (null if it could not be determined).
     */
    public RdfSyntaxNotAllowedException(String contentType) {
        super(contentType == null
                ? "The RDF content type could not be determined and this is required by the test engine's configuration."
                : "The RDF content type [%s] is not allowed by the test engine's configuration.".formatted(contentType));
    }

}
