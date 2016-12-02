package com.gitb.types;

import javax.xml.xpath.XPathExpression;

/**
 * Created by tuncay on 9/25/14.
 */
public class BinaryType extends PrimitiveType {
    private byte[] content;

    public BinaryType(){
        content = null;
    }

    @Override
    public String getType() {
        return DataType.BINARY_DATA_TYPE;
    }

    @Override
    public DataType processXPath(XPathExpression expression, String returnType) {
        return this;
    }

    @Override
    public void deserialize(byte[] content, String encoding) {
        this.content = content;
    }

    @Override
    public byte[] serialize(String encoding) {
        return content;
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
    public StringType toStringType() {
        return new StringType(new String(content));
    }

    @Override
    public ObjectType toObjectType() {
        ObjectType type = new ObjectType();
        type.deserialize(content);
        return type;
    }

    @Override
    public SchemaType toSchemaType() {
        SchemaType type = new SchemaType();
        type.deserialize(content);
        return type;
    }

}
