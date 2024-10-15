package com.gitb.types;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tdl.NamedTypedString;
import com.gitb.tdl.Variable;

import java.util.Base64;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tuncay on 9/24/14.
 */
public class DataTypeFactory {
    private static final Pattern containerTypePattern = Pattern.compile("^(list)(?:\\[([a-z]+)\\])?$");
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
    private static String parseContainerType(String type) {
        Matcher matcher = containerTypePattern.matcher(type);
        matcher.find();
        return matcher.group(1);
    }

    /**
     * Take the child type identifier from the whole type
     * @param type
     * @return
     */
    private static String parseContainedType(String type) {
        Matcher matcher = containerTypePattern.matcher(type);
        String containedType = null;
        if (matcher.find()) {
            int count = matcher.groupCount();
            if (count == 2) {
                containedType = matcher.group(2);
            }
        }
        return containedType;
    }

    /**
     * Initialize an empty value from the type identifier
     * @param type
     * @return
     */
    public DataType create(String type){
        DataType data;
        switch (type) {
            case DataType.BOOLEAN_DATA_TYPE:
                data = new BooleanType();
                break;
            case DataType.BINARY_DATA_TYPE:
                data = new BinaryType();
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
                    if (DataType.LIST_DATA_TYPE.equals(containerType)) {
                        if (containedType == null) {
                            containedType = DataType.STRING_DATA_TYPE;
                        }
                        data = new ListType(containedType);
                    } else {
                        throw new IllegalStateException("Unsupported container type ["+containerType+"]");
                    }
                } else {
                    throw new IllegalStateException("Unknown data type ["+type+"]");
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
            throw new IllegalStateException(e);
        }
        return data;
    }

    public DataType create(byte[] content, String type) {
        //Create the empty value
        DataType data = create(type);
        try {
            data.deserialize(content);
        }catch(Exception e){
            throw new IllegalStateException(e);
        }
        return data;
    }

    public AnyContent applyFilter(AnyContent content, Function<AnyContent, Boolean> filter) {
        AnyContent result = null;
        if (content != null && filter.apply(content)) {
            var childrenRemoved = content.getItem().removeIf((x) -> x == null || applyFilter(x, filter) == null);
            if (!childrenRemoved || !content.getItem().isEmpty()) {
                result = content;
            }
        }
        return result;
    }

    public DataType create(AnyContent content) {
        return create(content, (x) -> true);
    }

    public static String determineDataType(AnyContent content) {
        String declaredType = null;
        if (content != null) {
            declaredType = content.getType();
            if (declaredType == null) {
                if (content.getItem().size() > 0) {
                    boolean namedChildren = false;
                    for (AnyContent child: content.getItem()) {
                        if (child.getName() != null) {
                            namedChildren = true;
                        } else {
                            namedChildren = false;
                            break;
                        }
                    }
                    if (namedChildren) {
                        // Named child items - this can only be a map.
                        declaredType = DataType.MAP_DATA_TYPE;
                    } else {
                        declaredType = DataType.LIST_DATA_TYPE;
                    }
                } else {
                    if (content.getEmbeddingMethod() == ValueEmbeddingEnumeration.BASE_64) {
                        declaredType = DataType.BINARY_DATA_TYPE;
                    } else {
                        declaredType = DataType.STRING_DATA_TYPE;
                    }
                }
            }
        }
        return declaredType;
    }

    public DataType create(AnyContent content, Function<AnyContent, Boolean> filter) {
        DataType type = null;
        if (content != null && filter.apply(content)) {
            String declaredType = determineDataType(content);
            if (DataType.MAP_DATA_TYPE.equals(declaredType)) {
                type = new MapType();
                for (AnyContent child : content.getItem()) {
                    var childData = create(child, filter);
                    if (childData != null) {
                        ((MapType)type).addItem(child.getName(), childData);
                    }
                }
                if (((MapType) type).isEmpty()) {
                    type = null;
                }
            } else if (DataType.STRING_DATA_TYPE.equals(declaredType)) {
                type = new StringType();
                type.setValue(content.getValue());
            } else if (DataType.BINARY_DATA_TYPE.equals(declaredType)) {
                type = new BinaryType();
                if (ValueEmbeddingEnumeration.BASE_64.equals(content.getEmbeddingMethod())) {
                    type.setValue(Base64.getDecoder().decode(content.getValue()));
                } else {
                    throw new IllegalStateException("Only base64 embedding supported for binary types");
                }
            } else if (DataType.BOOLEAN_DATA_TYPE.equals(declaredType)) {
                type = new BooleanType();
                type.setValue(Boolean.valueOf(content.getValue()));
            } else if (DataType.NUMBER_DATA_TYPE.equals(declaredType)) {
                type = new NumberType();
                type.setValue(content.getValue());
            } else if (DataType.LIST_DATA_TYPE.equals(declaredType)) {
                type = new ListType();
                for (AnyContent child : content.getItem()) {
                    var childData = create(child, filter);
                    if (childData != null) {
                        ((ListType)type).append(childData);
                    }
                }
                if (((ListType) type).isEmpty()) {
                    type = null;
                }
            } else if (DataType.OBJECT_DATA_TYPE.equals(declaredType)) {
                type = new ObjectType();
                if (ValueEmbeddingEnumeration.BASE_64.equals(content.getEmbeddingMethod())) {
                    type.deserialize(Base64.getDecoder().decode(content.getValue()));
                } else if (ValueEmbeddingEnumeration.STRING.equals(content.getEmbeddingMethod())) {
                    type.deserialize(content.getValue().getBytes());
                } else {
                    throw new IllegalStateException("Only base64 and string embedding supported for object types");
                }
            } else {
                throw new IllegalStateException("Unsupported data type [" + declaredType + "]");
            }
        }
        return type;
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
                if (!variable.getValue().isEmpty()) {
                    data.deserialize(variable.getValue().get(0).getValue().getBytes());
                }
            }
            //Container Types
            else if (data instanceof ContainerType) {
                if(data instanceof MapType){
                    for (NamedTypedString binding : variable.getValue()) {
                        if (binding.getName() == null) {
                            throw new IllegalStateException("A map variable's value was found for which no name was declared.");
                        }
                        DataType item = create(binding.getType());
                        item.deserialize(binding.getValue().getBytes());
                        ((MapType)data).addItem(binding.getName(), item);
                    }
                }
                //ListType
                else {
                    for (NamedTypedString binding : variable.getValue()) {
                        DataType item = create(((ListType)data).getContainedType());
                        item.deserialize(binding.getValue().getBytes());
                        ((ListType)data).append(item);
                    }
                }
            }
            //Complex Types
            else {
                if(!variable.getValue().isEmpty()) {
                    data.deserialize(variable.getValue().get(0).getValue().getBytes());
                }
            }
        }catch (Exception e){
            throw new IllegalStateException(e);
        }
        return data;
    }

}
