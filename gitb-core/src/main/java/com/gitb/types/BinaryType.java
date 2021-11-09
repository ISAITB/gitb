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
    public StringType toStringType() {
        return new StringType(new String((byte[]) getValue()));
    }

    @Override
    public ObjectType toObjectType() {
        ObjectType type = new ObjectType();
        type.deserialize((byte[]) getValue());
        return type;
    }

    @Override
    public SchemaType toSchemaType() {
        SchemaType type = new SchemaType();
        type.deserialize((byte[]) getValue());
        return type;
    }

}
