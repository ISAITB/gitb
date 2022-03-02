package com.gitb.utils;

import com.gitb.core.ErrorCode;
import com.gitb.core.ErrorInfo;
import com.gitb.tdl.TestStep;

/**
 * Created by root on 2/25/15.
 */
public class ErrorUtils {
    public static ErrorInfo errorInfo(ErrorCode errorCode, String message) {
        ErrorInfo info = new ErrorInfo();
        info.setErrorCode(errorCode);
        info.setDescription(message);

        return info;
    }

    public static ErrorInfo errorInfo(ErrorCode errorCode) {
        ErrorInfo info = new ErrorInfo();
        info.setErrorCode(errorCode);
        info.setDescription("");

        return info;
    }

    private static String extractStepName(Object step) {
        String name = step.getClass().getSimpleName();
        if (name.endsWith("Step")) {
            return name.substring(0, name.indexOf("Step"));
        } else {
            return name;
        }
    }

    public static String extractStepDescription(Object step) {
        String description = null;
        if (step instanceof TestStep) {
            description = ((TestStep) step).getDesc();
            if (description != null && description.isBlank()) {
                description = null;
            }
        }
        if (description == null) {
            description = extractStepName(step);
        }
        return description;
    }

}
