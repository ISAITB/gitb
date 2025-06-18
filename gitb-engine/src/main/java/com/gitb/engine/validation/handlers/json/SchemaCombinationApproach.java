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

package com.gitb.engine.validation.handlers.json;

/**
 * Enum to determine how multiple validation artifacts are to be combined for the validation.
 */
public enum SchemaCombinationApproach {

    /** All artifacts must be successfully validated against to consider validation as successful. */
    ALL(SchemaCombinationApproach.ALL_VALUE),
    /** Any of the artifacts must be successfully validated against to consider validation as successful. */
    ANY(SchemaCombinationApproach.ANY_VALUE),
    /** At least one of the artifacts must be successfully validated against to consider validation as successful. */
    ONE_OF(SchemaCombinationApproach.ONE_OF_VALUE);

    public static final String ALL_VALUE = "allOf";
    public static final String ANY_VALUE = "anyOf";
    public static final String ONE_OF_VALUE = "oneOf";

    private final String name;

    /**
     * @param name The enum's text value.
     */
    SchemaCombinationApproach(String name) {
        this.name = name;
    }

    /**
     * @return The enum's text value.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the enum instance corresponding to the provided text value.
     *
     * @param name The text value.
     * @return The enum instance.
     * @throws IllegalArgumentException if no enum instance could be matched.
     */
    public static SchemaCombinationApproach byName(String name) {
        if (ALL.name.equals(name)) {
            return ALL;
        } else if (ANY.name.equals(name)) {
            return ANY;
        } else if (ONE_OF.name.equals(name)) {
            return ONE_OF;
        } else {
            throw new IllegalArgumentException("Unknown type name for artifact combination approach ["+name+"]");
        }
    }

}
