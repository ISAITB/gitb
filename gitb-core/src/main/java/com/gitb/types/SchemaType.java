package com.gitb.types;

import org.w3c.dom.Node;

/**
 * Created by tuncay on 10/28/14.
 */
public class SchemaType extends ObjectType{

    public SchemaType() {
        super();
    }

    public SchemaType(Node value) {
        super(value);
    }

    public String getType() {
        return DataType.SCHEMA_DATA_TYPE;
    }

    @Override
    public ObjectType toObjectType() {
        ObjectType type = new ObjectType();
        type.setValue(getValue());
        type.setSize(getSize());
        return type;
    }
}
