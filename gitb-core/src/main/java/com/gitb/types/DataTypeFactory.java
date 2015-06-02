package com.gitb.types;

import com.gitb.ModuleManager;
import com.gitb.tdl.TypedBinding;
import com.gitb.tdl.Variable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tuncay on 9/24/14.
 */
public class DataTypeFactory {
    private static Pattern containerTypePattern = Pattern.compile("([a-z]+)\\[([a-z]+)\\]");
    private static DataTypeFactory instance = null;

    public static DataTypeFactory getInstance(){
        if(instance == null){
            instance = new DataTypeFactory();
        }
        return instance;
    }

    protected DataTypeFactory() {

    }

    /**
     * Check if a type identifier indicates a container type
     * @param type
     * @return
     */
    public static boolean isContainerType(String type){
        return containerTypePattern.matcher(type).matches();
    }

    /**
     * Take the container name from the whole type
     * @param type
     * @return
     */
    public static String parseContainerType(String type) {
        Matcher matcher = containerTypePattern.matcher(type);
        matcher.find();
        return matcher.group(1);
    }

    /**
     * Take the child type identifier from the whole type
     * @param type
     * @return
     */
    public static String parseContainedType(String type) {
        Matcher matcher = containerTypePattern.matcher(type);
        matcher.find();
        return matcher.group(2);
    }

    /**
     * Initialize an empty value from the type identifier
     * @param type
     * @return
     */
    public DataType create(String type){
        DataType data = null;
        switch (type) {
            case DataType.BOOLEAN_DATA_TYPE:
                data = new BooleanType();
                break;
            case DataType.NUMBER_DATA_TYPE:
                data = new NumberType();
                break;
            case DataType.STRING_DATA_TYPE:
                data = new StringType();
                break;
            case DataType.MAP_DATA_TYPE:
                data = new MapType();
                break;
            case DataType.OBJECT_DATA_TYPE:
                data = new ObjectType();
                break;
            case DataType.SCHEMA_DATA_TYPE:
                data = new SchemaType();
                break;
            default:
                if(isContainerType(type)){
                    String containerType = parseContainerType(type);
                    String containedType = parseContainedType(type);
                    switch (containerType){
                        case DataType.LIST_DATA_TYPE:
                            data = new ListType(containedType);
                        default:
                            //TODO throw Invalid Test Case exception (No such container type)
                    }
                }else{
                   // It is a plugged in type
                    data = ModuleManager.getInstance().getDataType(type);
                }
        }
        return data;
    }

    /**
     * Initialize
     * @param content
     * @param type
     * @param encoding
     * @return
     */
    public DataType create(byte[] content, String type, String encoding){
        //Create the empty value
        DataType data = create(type);
        try {
            data.deserialize(content, encoding);
        }catch(Exception e){
            //TODO Handle exception
            e.printStackTrace();
        }
        return data;
    }

    public DataType create(byte[] content, String type) {
        //Create the empty value
        DataType data = create(type);
        try {
            data.deserialize(content);
        }catch(Exception e){
            //TODO Handle exception
            e.printStackTrace();
        }
        return data;
    }
    /**
     * Initialize a value from the Variable definition
     * @param variable
     * @return
     */
    public DataType create(Variable variable) {
        String type = variable.getType();
        //Create the empty value
        DataType data = create(type);
        try {
            //Primitive Types
            if (data instanceof PrimitiveType) {
                if (variable.getValue().size() > 0) {
                    data.deserialize(variable.getValue().get(0).getValue().getBytes());
                }
            }
            //Container Types
            else if (data instanceof ContainerType) {
                if(data instanceof MapType){
                    for (TypedBinding binding : variable.getValue()) {
                        //TODO Check if binding has name
                        DataType item = create(binding.getType());
                        item.deserialize(binding.getValue().getBytes());
                        ((MapType)data).addItem(binding.getName(), item);
                    }
                }
                //ListType
                else {
                    for (TypedBinding binding : variable.getValue()) {
                        DataType item = create(((ListType)data).getContainedType());
                        item.deserialize(binding.getValue().getBytes());
                        ((ListType)data).append(item);
                    }
                }
            }
            //Complex Types
            else {
                if(variable.getValue().size() > 0) {
                    data.deserialize(variable.getValue().get(0).getValue().getBytes());
                }
            }
        }catch (Exception e){
            //TODO Handle exceptions
            e.printStackTrace();
        }
        return data;
    }

}
