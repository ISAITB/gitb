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

	//public static final String DEFAULT_ENCODING = "utf-8";

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

    public static boolean isListType(String typeDefinition) {
        return typeDefinition.equals(LIST_DATA_TYPE) || (typeDefinition.startsWith(LIST_DATA_TYPE+CONTAINER_TYPE_PARENTHESIS[0]) && typeDefinition.endsWith(CONTAINER_TYPE_PARENTHESIS[1]));
    }

    public static boolean isFileType(String typeDefinition) {
        return DataType.BINARY_DATA_TYPE.equals(typeDefinition)
                || DataType.SCHEMA_DATA_TYPE.equals(typeDefinition)
                || DataType.OBJECT_DATA_TYPE.equals(typeDefinition);
    }

    public DataType convertTo(String targetType) {
        if (this.getType().equals(targetType)) {
            return this;
        }
        switch (targetType) {
            case (DataType.BOOLEAN_DATA_TYPE):
                return toBooleanType();
            case (DataType.NUMBER_DATA_TYPE):
                return toNumberType();
            case (DataType.STRING_DATA_TYPE):
                return toStringType();
            case (DataType.BINARY_DATA_TYPE):
                return toBinaryType();
            case (DataType.SCHEMA_DATA_TYPE):
                return toSchemaType();
            case (DataType.OBJECT_DATA_TYPE):
                return toObjectType();
            case (DataType.MAP_DATA_TYPE):
                return toMapType();
            default:
                if (isListType(targetType)) {
                    return toListType();
                } else {
                    throw new IllegalArgumentException("Unknown target conversion type ["+targetType+"]");
                }
        }
    }

    public BooleanType toBooleanType() {
        throw new IllegalArgumentException("Conversion from ["+this.getType()+"] to ["+BOOLEAN_DATA_TYPE+"] not supported");
    }

    public NumberType toNumberType() {
        throw new IllegalArgumentException("Conversion from ["+this.getType()+"] to ["+NUMBER_DATA_TYPE+"] not supported");
    }

    public StringType toStringType() {
        throw new IllegalArgumentException("Conversion from ["+this.getType()+"] to ["+STRING_DATA_TYPE+"] not supported");
    }

    public BinaryType toBinaryType() {
        throw new IllegalArgumentException("Conversion from ["+this.getType()+"] to ["+BINARY_DATA_TYPE+"] not supported");
    }

    public ListType toListType() {
        if (this instanceof ListType) {
            return (ListType)this;
        } else {
            ListType list = new ListType(getType());
            list.append(this);
            return list;
        }
    }

    public MapType toMapType() {
        throw new IllegalArgumentException("Conversion from ["+this.getType()+"] to ["+MAP_DATA_TYPE+"] not supported");
    }

    public SchemaType toSchemaType() {
        throw new IllegalArgumentException("Conversion from ["+this.getType()+"] to ["+SCHEMA_DATA_TYPE+"] not supported");
    }

    public ObjectType toObjectType() {
        throw new IllegalArgumentException("Conversion from ["+this.getType()+"] to ["+OBJECT_DATA_TYPE+"] not supported");
    }

}
