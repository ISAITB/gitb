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
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by tuncay on 9/2/14.
 */
public abstract class DataType {
    public static final String BOOLEAN_DATA_TYPE = "boolean";
    public static final String NUMBER_DATA_TYPE = "number";
    public static final String STRING_DATA_TYPE = "string";
    public static final String BINARY_DATA_TYPE = "binary";
    public static final String LIST_DATA_TYPE = "list";
    public static final String MAP_DATA_TYPE = "map";
    public static final String SCHEMA_DATA_TYPE="schema";
    public static final String OBJECT_DATA_TYPE = "object";
    public static final String[] CONTAINER_TYPE_PARENTHESIS = {"[", "]"};

    protected String importPath;
    protected String importTestSuite;
    private String contentType;

    public abstract String getType();

    public abstract DataType processXPath(XPathExpression expression, String returnType);

    public abstract void deserialize(byte[] content, String encoding);

	public abstract void deserialize(byte[] content);

    public abstract void deserialize(InputStream inputStream, String encoding);

	public abstract void deserialize(InputStream inputStream);

    public abstract OutputStream serializeToStream(String encoding);

    public abstract OutputStream serializeToStreamByDefaultEncoding();

    public abstract byte[] serialize(String encoding);

    public abstract byte[] serializeByDefaultEncoding();

    public abstract Object getValue();

    public abstract void setValue(Object value);

    public void copyFrom(DataType other) {
        copyFrom(other, getType());
    }

    public void copyFrom(DataType other, String conversionDataType) {
        setValue(other.convertTo(conversionDataType).getValue());
        setImportPath(other.getImportPath());
        setImportTestSuite(other.getImportTestSuite());
    }

    public static boolean isListType(String typeDefinition) {
        return typeDefinition.equals(LIST_DATA_TYPE) || (typeDefinition.startsWith(LIST_DATA_TYPE+CONTAINER_TYPE_PARENTHESIS[0]) && typeDefinition.endsWith(CONTAINER_TYPE_PARENTHESIS[1]));
    }

    public static boolean isFileType(String typeDefinition) {
        return DataType.BINARY_DATA_TYPE.equals(typeDefinition)
                || DataType.SCHEMA_DATA_TYPE.equals(typeDefinition)
                || DataType.OBJECT_DATA_TYPE.equals(typeDefinition);
    }

    public DataType convertTo(String targetType) {
        DataType convertedType;
        if (targetType == null || this.getType().equals(targetType)) {
            convertedType = this;
        } else {
            switch (targetType) {
                case (DataType.BOOLEAN_DATA_TYPE):
                    convertedType = toBooleanType(); break;
                case (DataType.NUMBER_DATA_TYPE):
                    convertedType = toNumberType(); break;
                case (DataType.STRING_DATA_TYPE):
                    convertedType = toStringType(); break;
                case (DataType.BINARY_DATA_TYPE):
                    convertedType = toBinaryType(); break;
                case (DataType.SCHEMA_DATA_TYPE):
                    convertedType = toSchemaType(); break;
                case (DataType.OBJECT_DATA_TYPE):
                    convertedType = toObjectType(); break;
                case (DataType.MAP_DATA_TYPE):
                    convertedType = toMapType(); break;
                default:
                    if (isListType(targetType)) {
                        if (this instanceof ListType) {
                            convertedType = this;
                        } else {
                            convertedType = toListType();
                        }
                    } else {
                        throw new IllegalArgumentException("Unknown target conversion type ["+targetType+"]");
                    }
            }
            convertedType.setImportPath(getImportPath());
            convertedType.setImportTestSuite(getImportTestSuite());
            convertedType.setContentType(getContentType());
        }
        return convertedType;
    }

    protected BooleanType toBooleanType() {
        throw new IllegalArgumentException("Conversion from ["+this.getType()+"] to ["+BOOLEAN_DATA_TYPE+"] not supported");
    }

    protected NumberType toNumberType() {
        throw new IllegalArgumentException("Conversion from ["+this.getType()+"] to ["+NUMBER_DATA_TYPE+"] not supported");
    }

    protected StringType toStringType() {
        throw new IllegalArgumentException("Conversion from ["+this.getType()+"] to ["+STRING_DATA_TYPE+"] not supported");
    }

    protected BinaryType toBinaryType() {
        throw new IllegalArgumentException("Conversion from ["+this.getType()+"] to ["+BINARY_DATA_TYPE+"] not supported");
    }

    protected ListType toListType() {
        if (this instanceof ListType) {
            return (ListType)this;
        } else {
            ListType list = new ListType(getType());
            list.append(this);
            return list;
        }
    }

    protected MapType toMapType() {
        throw new IllegalArgumentException("Conversion from ["+this.getType()+"] to ["+MAP_DATA_TYPE+"] not supported");
    }

    protected SchemaType toSchemaType() {
        throw new IllegalArgumentException("Conversion from ["+this.getType()+"] to ["+SCHEMA_DATA_TYPE+"] not supported");
    }

    public ObjectType toObjectType() {
        throw new IllegalArgumentException("Conversion from ["+this.getType()+"] to ["+OBJECT_DATA_TYPE+"] not supported");
    }

    public String getImportPath() {
        return importPath;
    }

    public void setImportPath(String importPath) {
        this.importPath = importPath;
    }

    public String getImportTestSuite() {
        return importTestSuite;
    }

    public void setImportTestSuite(String importTestSuite) {
        this.importTestSuite = importTestSuite;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
