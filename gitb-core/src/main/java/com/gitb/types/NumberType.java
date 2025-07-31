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
 * Created by senan on 9/15/14.
 */
public class NumberType extends PrimitiveType<Double> {

    private Double value = (double) 0;

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
    public Double getValue() {
        return this.value;
    }

    public double doubleValue() {
        return this.value;
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
    protected StringType toStringType() {
        return new StringType(String.valueOf(value));
    }

    @Override
    protected BooleanType toBooleanType() {
        return new BooleanType(this.value.intValue() == 1);
    }
}
