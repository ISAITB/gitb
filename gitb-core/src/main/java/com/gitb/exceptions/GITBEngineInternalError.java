package com.gitb.exceptions;

import com.gitb.core.ErrorCode;
import com.gitb.core.ErrorInfo;
import com.gitb.utils.ErrorUtils;

/**
 * Created by tuncay on 9/30/14.
 */
public class GITBEngineInternalError extends GITBEngineRuntimeException{

    private final ErrorInfo errorInfo;

    public GITBEngineInternalError() {
        errorInfo = ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "");
    }

    public GITBEngineInternalError(String message) {
        super(message);
        errorInfo = ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, message);
    }

    public GITBEngineInternalError(Throwable cause) {
        super(cause);
        errorInfo = ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "");
    }

    public GITBEngineInternalError(String message, Throwable cause) {
        super(message, cause);
        errorInfo = ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, message);
    }

    public GITBEngineInternalError(ErrorInfo errorInfo) {
        super(errorInfo.getDescription());
        this.errorInfo = errorInfo;
    }

    public GITBEngineInternalError(ErrorInfo errorInfo, String message) {
        super(message);
        this.errorInfo = errorInfo;
    }

    public GITBEngineInternalError(ErrorInfo errorInfo, Throwable cause) {
        super(errorInfo.getDescription(), cause);
        this.errorInfo = errorInfo;
    }

    public GITBEngineInternalError(ErrorInfo errorInfo, String message, Throwable cause) {
        super(message, cause);
        this.errorInfo = errorInfo;
    }

    public ErrorInfo getErrorInfo() {
        return errorInfo;
    }
}
