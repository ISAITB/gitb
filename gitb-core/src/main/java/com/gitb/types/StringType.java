package com.gitb.types;

import com.gitb.exceptions.GITBEngineInternalError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathExpression;
import java.io.*;

/**
 * Created by senan on 9/8/14.
 */
public class StringType extends PrimitiveType {
    private static Logger logger = LoggerFactory.getLogger(StringType.class);
    private String data = "";

	public StringType() {
	}

	public StringType(String data) {
		this.data = data;
	}

	@Override
    public String getType() {
        return DataType.STRING_DATA_TYPE;
    }

    @Override
    public DataType processXPath(XPathExpression expression, String returnType) {
        //expression evaluates/returns itself
        return this;
    }

    @Override
    public void deserialize(byte[] content, String encoding) {
        try {
            setValue(new String(content, encoding));
        }catch(UnsupportedEncodingException uee){
           logger.error("Invalid Test Case!", uee);
        }
    }


    @Override
    public byte[] serialize(String encoding) {
        try {
            return data.getBytes(encoding);
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
    public BinaryType toBinaryType() {
        BinaryType type = new BinaryType();
        type.deserialize(serializeByDefaultEncoding());
        return type;
    }

    @Override
    public NumberType toNumberType() {
        NumberType type = new NumberType();
        type.setValue(data);
        return type;
    }

    @Override
    public BooleanType toBooleanType() {
        BooleanType type = new BooleanType();
        type.setValue(Boolean.valueOf(data));
        return type;
    }

    @Override
    public ObjectType toObjectType() {
        ObjectType type = new ObjectType();
        type.deserialize(serializeByDefaultEncoding());
        return type;
    }

    @Override
    public SchemaType toSchemaType() {
        SchemaType type = new SchemaType();
        type.deserialize(serializeByDefaultEncoding());
        return type;
    }

}
