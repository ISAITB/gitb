package com.gitb.types;

/**
 * Created by tuncay on 10/28/14.
 */
public class SchemaType extends ObjectType{
    protected String schemaLocation;

    public String getSchemaLocation(){
        return schemaLocation;
    }

    public void setSchemaLocation(String schemaLocation) {
        this.schemaLocation = schemaLocation;
    }

    public String getType() {
        return DataType.SCHEMA_DATA_TYPE;
    }
}
