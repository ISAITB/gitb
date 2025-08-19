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

import com.gitb.exceptions.GITBEngineInternalError;

import javax.xml.xpath.XPathExpression;
import java.io.UnsupportedEncodingException;
import java.util.Objects;

/**
 * Created by senan on 9/8/14.
 */
public class StringType extends PrimitiveType<String> {

    private String data;
    private String encoding;

	public StringType() {
        this("");
	}

	public StringType(String data) {
        this(data, DEFAULT_ENCODING);
	}

    public StringType(String data, String encoding) {
        this.data = Objects.requireNonNullElse(data, "");
        this.encoding = encoding;
    }

	@Override
    public String getType() {
        return DataType.STRING_DATA_TYPE;
    }

    @Override
    public DataType processXPath(XPathExpression expression, String returnType) {
	    return toObjectType().processXPath(expression, returnType);
    }

    @Override
    public void deserialize(byte[] content, String encoding) {
        try {
            setValue(new String(content, encoding));
            this.encoding = encoding;
        } catch(UnsupportedEncodingException uee) {
            throw new IllegalStateException("Invalid Test Case!", uee);
        }
    }


    @Override
    public byte[] serialize(String encoding) {
        try {
            return getValue().getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            throw new GITBEngineInternalError(e);
        }
    }

    @Override
    public String getValue() {
        return this.data;
    }

    @Override
    public void setValue(Object value) {
        this.data = (String) value;
    }

    @Override
    public String toString() {
        return this.getValue();
    }

    @Override
    protected BinaryType toBinaryType() {
        BinaryType type = new BinaryType();
        type.deserialize(serialize(encoding));
        return type;
    }

    @Override
    protected NumberType toNumberType() {
        NumberType type = new NumberType();
        type.setValue(getValue());
        return type;
    }

    @Override
    protected BooleanType toBooleanType() {
        BooleanType type = new BooleanType();
        type.setValue(Boolean.valueOf(getValue()));
        return type;
    }

    @Override
    public ObjectType toObjectType() {
        ObjectType type = new ObjectType();
        type.deserialize(serialize(encoding));
        return type;
    }

    @Override
    protected SchemaType toSchemaType() {
        SchemaType type = new SchemaType();
        type.deserialize(serialize(encoding));
        return type;
    }

    @Override
    protected StringType toStringType() {
	    return new StringType(this.getValue(), this.encoding);
    }

    public String getEncoding() {
        return encoding;
    }
}
