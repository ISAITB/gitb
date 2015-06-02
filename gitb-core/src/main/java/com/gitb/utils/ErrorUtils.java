package com.gitb.utils;

import com.gitb.core.ErrorCode;
import com.gitb.core.ErrorInfo;

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
}
