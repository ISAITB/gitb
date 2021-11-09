package com.gitb.types;

import com.gitb.exceptions.GITBEngineInternalError;

import javax.xml.xpath.XPathExpression;
import java.io.UnsupportedEncodingException;

/**
 * Created by senan on 9/8/14.
 */
public class StringType extends PrimitiveType {
    private String data;
    private String encoding;

	public StringType() {
        this("");
	}

	public StringType(String data) {
        this(data, DEFAULT_ENCODING);
	}

    public StringType(String data, String encoding) {
        this.data = data;
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
            return ((String) getValue()).getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            throw new GITBEngineInternalError(e);
        }
    }

    @Override
    public Object getValue() {
        return this.data;
    }

    @Override
    public void setValue(Object value) {
        this.data = (String) value;
    }

    @Override
    public String toString() {
        return (String) this.getValue();
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
        type.setValue(Boolean.valueOf((String) getValue()));
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
	    return new StringType((String) this.getValue(), this.encoding);
    }

    public String getEncoding() {
        return encoding;
    }
}
