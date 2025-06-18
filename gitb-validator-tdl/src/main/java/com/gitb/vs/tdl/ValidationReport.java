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

package com.gitb.vs.tdl;

import com.gitb.vs.tdl.util.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple class to represent the result of a validation.
 */
public class ValidationReport {

    private List<ValidationReportItem> items = new ArrayList<>();

    public List<ValidationReportItem> getItems() {
        return items;
    }

    public void addItem(ErrorCode error, String location, String... messageArguments) {
        items.add(new ValidationReportItem(error, location, messageArguments));
    }

    protected ValidationReport sort() {
        items.sort((o1, o2) -> {
            if (o1.getLevel() == o2.getLevel()) {
                return 0;
            } else if (o1.getLevel() == ErrorLevel.ERROR || o1.getLevel() == ErrorLevel.WARNING && o2.getLevel() == ErrorLevel.INFO) {
                return -1;
            } else {
                return 1;
            }
        });
        return this;
    }

    public static class ValidationReportItem {

        private String code;
        private String description;
        private String location;
        private ErrorLevel level;

        ValidationReportItem(ErrorCode error, String location, String... messageArguments) {
            this.code = error.getCode();
            this.location = Utils.standardisePath(location);
            this.description = error.getMessage(messageArguments);
            this.level = error.getLevel();
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public ErrorLevel getLevel() {
            return level;
        }

        public String getLocation() {
            return location;
        }
    }

}
