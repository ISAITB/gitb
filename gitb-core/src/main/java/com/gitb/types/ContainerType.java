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

package com.gitb.types;

import com.gitb.core.ErrorCode;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.utils.ErrorUtils;

import java.io.InputStream;
import java.io.OutputStream;


/**
 * Created by tuncay on 9/2/14.
 */
public abstract class ContainerType extends DataType {

    private static final String DEFAULT_ENCODING = "utf-8";

    public abstract boolean isEmpty();

    public abstract int getSize();

	public void setValue(Object value) {
	    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.DATATYPE_ERROR, "The value of a container type can not be set directly"));
	}

    @Override
    public void deserialize(byte[] content, String encoding) {
        throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.DATATYPE_ERROR, "Container type is not a deserializable type"));
    }

    @Override
    public void deserialize(byte[] content) {
        deserialize(content, DEFAULT_ENCODING);
    }

    @Override
    public void deserialize(InputStream inputStream, String encoding) {
	    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.DATATYPE_ERROR, "Container type is not a deserializable type"));
    }

    @Override
    public void deserialize(InputStream inputStream) {
        deserialize(inputStream, DEFAULT_ENCODING);
    }

    @Override
    public OutputStream serializeToStream(String encoding) {
	    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.DATATYPE_ERROR, "Container type is not a deserializable type"));
    }

    @Override
    public OutputStream serializeToStreamByDefaultEncoding() {
        return serializeToStream(DEFAULT_ENCODING);
    }

    @Override
    public byte[] serialize(String encoding) {
        throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.DATATYPE_ERROR, "Container type is not a serializable type"));
    }


    @Override
    public byte[] serializeByDefaultEncoding() {
        return serialize(DEFAULT_ENCODING);
    }
}
