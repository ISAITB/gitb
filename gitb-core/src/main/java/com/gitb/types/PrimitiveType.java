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
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by tuncay on 9/25/14.
 */
public abstract class PrimitiveType extends DataType {
    public static final String DEFAULT_ENCODING = "utf-8";

    @Override
    public void deserialize(byte[] content) {
        deserialize(content, PrimitiveType.DEFAULT_ENCODING);
    }

    @Override
    public void deserialize(InputStream inputStream){
        deserialize(inputStream, PrimitiveType.DEFAULT_ENCODING);
    }

    @Override
    public void deserialize(InputStream inputStream, String encoding) {
	    try {
		    deserialize(IOUtils.toByteArray(inputStream), encoding);
	    } catch (IOException e) {
		    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.DATATYPE_ERROR, "An error occurred during deserialization."), e);
	    }
    }

    @Override
    public OutputStream serializeToStream(String encoding) {
	    try {
		    byte [] content = serialize(encoding);
		    OutputStream os = new ByteArrayOutputStream();
		    os.write(content);

		    return os;
	    } catch (IOException e) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.DATATYPE_ERROR, "An error occurred during serialization."), e);
	    }
    }

    @Override
    public OutputStream serializeToStreamByDefaultEncoding() {
        return serializeToStream(PrimitiveType.DEFAULT_ENCODING);
    }

    @Override
    public byte[] serializeByDefaultEncoding() {
        return serialize(PrimitiveType.DEFAULT_ENCODING);
    }

}
