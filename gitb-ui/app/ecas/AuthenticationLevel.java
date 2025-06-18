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

package ecas;

public enum AuthenticationLevel {

    BASIC("BASIC"), MEDIUM("MEDIUM"), HIGH("HIGH");


    private final String levelName;

    AuthenticationLevel(String levelName) {
        this.levelName = levelName;
    }

    public static AuthenticationLevel fromName(String levelName) {
        if (BASIC.levelName.equalsIgnoreCase(levelName)) {
            return BASIC;
        } else if (MEDIUM.levelName.equalsIgnoreCase(levelName)) {
            return MEDIUM;
        } else if (HIGH.levelName.equalsIgnoreCase(levelName)) {
            return HIGH;
        } else {
            throw new IllegalArgumentException("Unknown authentication level [%s]".formatted(levelName));
        }
    }

    @Override
    public String toString() {
        return levelName;
    }
}
