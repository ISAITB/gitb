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

package com.gitb.reports.dto.tar;

import java.util.List;

public class ContextItem {

    private final String key;
    private final String value;
    private final List<ContextItem> items;

    public ContextItem(String key, String value) {
        this(key, value, null);
    }

    public ContextItem(String key, List<ContextItem> items) {
        this(key, null, items);
    }

    private ContextItem(String key, String value, List<ContextItem> items) {
        this.key = key;
        this.value = value;
        this.items = items;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public List<ContextItem> getItems() {
        return items;
    }

}
