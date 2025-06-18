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

import javax.xml.xpath.XPathExpression;

/**
 * Created by tuncay on 9/25/14.
 */
public class BinaryType extends PrimitiveType {
    private byte[] content;

    public BinaryType() {
        this(null);
    }

    public BinaryType(byte[] content) {
        this.content = content;
    }

    @Override
    public String getType() {
        return DataType.BINARY_DATA_TYPE;
    }

    @Override
    public DataType processXPath(XPathExpression expression, String returnType) {
        return toObjectType().processXPath(expression, returnType);
    }

    @Override
    public void deserialize(byte[] content, String encoding) {
        setValue(content);
    }

    @Override
    public byte[] serialize(String encoding) {
        return (byte[]) getValue();
    }

    @Override
    public Object getValue() {
        return content;
    }

    @Override
    public void setValue(Object value) {
        this.content = (byte [])value;
    }

    @Override
    protected StringType toStringType() {
        return new StringType(new String((byte[]) getValue()));
    }

    @Override
    public ObjectType toObjectType() {
        ObjectType type = new ObjectType();
        type.deserialize((byte[]) getValue());
        return type;
    }

    @Override
    protected SchemaType toSchemaType() {
        SchemaType type = new SchemaType();
        type.deserialize((byte[]) getValue());
        return type;
    }

}
