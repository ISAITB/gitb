package com.gitb.types;

/**
 * Created by tuncay on 10/28/14.
 */
public class SchemaType extends ObjectType{
    protected String testSuiteId;
    protected String schemaLocation;

    public String getSchemaLocation(){
        return schemaLocation;
    }

    public void setSchemaLocation(String schemaLocation) {
        this.schemaLocation = schemaLocation;
    }

    public String getTestSuiteId() {
        return testSuiteId;
    }

    public void setTestSuiteId(String testSuiteId) {
        this.testSuiteId = testSuiteId;
    }

    public String getType() {
        return DataType.SCHEMA_DATA_TYPE;
    }

    @Override
    public ObjectType toObjectType() {
        ObjectType type = new ObjectType();
        type.setValue(getValue());
        return type;
    }
}
