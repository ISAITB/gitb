package com.gitb.types;

import javax.xml.xpath.XPathExpression;

/**
 * Created by senan on 9/15/14.
 */
public class NumberType extends PrimitiveType {

    private Double value = new Double(0);

    @Override
    public String getType() {
        return DataType.NUMBER_DATA_TYPE;
    }

    @Override
    public DataType processXPath(XPathExpression expression, String returnType) {
        //expression evaluates/returns itself
        return this;
    }

    @Override
    public void deserialize(byte[] content, String encoding) {
        setValue(Double.parseDouble(new String(content)));
    }


    @Override
    public byte[] serialize(String encoding) {
        return Double.toString(value).getBytes();
    }

    @Override
    public void setValue(Object value) {
        if(value instanceof String){
            this.value = Double.parseDouble((String)value);
        } else{
            this.value = (double) value;
        }
    }

    @Override
    public Object getValue() {
        return this.value;
    }

    public double doubleValue() {
        return this.value.doubleValue();
    }

    public int intValue() {
        return this.value.intValue();
    }

    public long longValue() {
        return this.value.longValue();
    }

    public float floatValue() {
        return this.value.floatValue();
    }

    public float shortValue() {
        return this.value.shortValue();
    }

    public String stringValue() {
        return this.value.toString();
    }

    @Override
    public StringType toStringType() {
        return new StringType(String.valueOf(value));
    }

    @Override
    public BooleanType toBooleanType() {
        return new BooleanType(this.value.intValue() == 1);
    }
}
