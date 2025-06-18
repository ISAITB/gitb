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
 * Class used to encapsulate the messages reported by the internal validation engine.
 */
public class Message {

    private final String contentPath;
    private String description;

    /**
     * Constructor.
     *
     * @param description The message description.
     */
    public Message(String description) {
        this(description, null);
    }

    /**
     * Constructor.
     *
     * @param description The message description.
     * @param contentPath The relevant content's path expression.
     */
    public Message(String description, String contentPath) {
        this.description = description;
        this.contentPath = contentPath;
    }

    /**
     * @param description The message description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return The message description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return The relevant content's path expression.
     */
    public String getContentPath() {
        return contentPath;
    }

}
