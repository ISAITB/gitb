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
