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
