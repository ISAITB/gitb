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

package com.gitb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enum used to define the XML Schema version to use.
 */
public enum XmlSchemaVersion {

    VERSION_1_0("1.0"),
    VERSION_1_1("1.1");

    private static final Logger LOG = LoggerFactory.getLogger(XmlSchemaVersion.class);
    private final String stringValue;

    /**
     * Constructor.
     *
     * @param stringValue The string value for the schema as defined in configuration.
     */
    XmlSchemaVersion(String stringValue) {
        this.stringValue = stringValue;
    }

    /**
     * Return the schema version to use for the provided string value.
     *
     * @param value The value.
     * @return The schema version.
     */
    public static XmlSchemaVersion from(String value) {
        if (VERSION_1_0.stringValue.equals(value)) {
            return VERSION_1_0;
        } else if (VERSION_1_1.stringValue.equals(value)) {
            return VERSION_1_1;
        } else {
            LOG.warn("Invalid XML Schema version [{}] (expected '{}' or '{}'. Considering '{}' by default.", value, VERSION_1_0.stringValue, VERSION_1_1.stringValue, VERSION_1_0.stringValue);
            return VERSION_1_0;
        }
    }

    @Override
    public String toString() {
        return stringValue;
    }
}
