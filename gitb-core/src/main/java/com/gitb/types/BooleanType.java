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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathExpression;
import java.io.*;

/**
 * Created by senan on 9/11/14.
 */
public class BooleanType extends PrimitiveType {
    private static Logger logger = LoggerFactory.getLogger(BooleanType.class);
    private boolean value = false;

    public BooleanType() {}

    public BooleanType(boolean bool) {
        this.value = bool;
    }

    @Override
    public String getType() {
        return DataType.BOOLEAN_DATA_TYPE;
    }

    @Override
    public DataType processXPath(XPathExpression expression, String returnType) {
        //expression evaluates/returns itself
        return this;
    }

    @Override
    public void deserialize(byte[] content, String encoding) {
        setValue(Boolean.parseBoolean(new String(content)));
    }

    @Override
    public byte[] serialize(String encoding) {
	    try {
		    return Boolean.toString(value).getBytes(encoding);
	    } catch (UnsupportedEncodingException e) {
		    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.DATATYPE_ERROR, "An error related to encoding occurred during serialization."), e);
	    }
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        this.value = (boolean) value;
    }

    @Override
    protected StringType toStringType() {
        return new StringType(Boolean.valueOf(value).toString());
    }

    @Override
    protected NumberType toNumberType() {
        NumberType type = new NumberType();
        type.setValue(value?1.0:0.0);
        return type;
    }
}
