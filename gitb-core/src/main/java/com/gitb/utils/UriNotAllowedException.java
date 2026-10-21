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

/**
 * Exception raised when a resource lookup is blocked because the URI (or the kind of resource) is not permitted.
 */
public class UriNotAllowedException extends GITBEngineInternalError {

    /**
     * Constructor.
     *
     * @param message The message (must not include information on the resolved addresses of the URI).
     */
    public UriNotAllowedException(String message) {
        super(message);
    }

}
