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
