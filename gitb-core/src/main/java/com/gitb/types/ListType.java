package com.gitb.types;

import com.gitb.core.ErrorCode;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.utils.ErrorUtils;

import javax.xml.xpath.XPathExpression;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tuncay on 9/2/14.
 */
public class ListType extends ContainerType {
    //Elements of the list
    protected List<DataType> elements;
    //The type for all elements
    protected String containedType;

    public ListType() {
	    elements = new ArrayList<>();
    }

    public ListType(String containedType) {
        this.containedType = containedType;
        elements = new ArrayList<>();
    }

    /**
     * Append the element to the end of the list
     * @param element
     */
    public void append(DataType element){
        if (this.elements != null) {
            this.elements.add(element);
        }
        if (containedType == null) {
            containedType = element.getType();
        }
    }

    /**
     * Get the element at the index
     * @param index
     * @return
     */
    public DataType getItem(int index){
        return elements.get(index);
    }

    /**
     * Get the type of all contained elements
     * @return
     */
    public String getContainedType() {
        return containedType;
    }

	public void setContainedType(String containedType) {
		this.containedType = containedType;
	}

	@Override
    public DataType processXPath(XPathExpression expression, String returnType) {
        if (returnType == null) {
            returnType = DataType.STRING_DATA_TYPE;
        }
        DataTypeFactory dataTypeFactory = DataTypeFactory.getInstance();
        //TODO Check is return type is list type and handle the casting exception
        ListType result = (ListType) DataTypeFactory.getInstance().create(returnType);
        for(DataType item:elements){
            result.append(item.processXPath(expression, returnType));
        }
        return result;
    }

    @Override
    public String getType() {
        if (containedType == null) {
            return DataType.LIST_DATA_TYPE;
        } else {
            return DataType.LIST_DATA_TYPE + DataType.CONTAINER_TYPE_PARENTHESIS[0] + containedType + DataType.CONTAINER_TYPE_PARENTHESIS[1];
        }
    }

    @Override
    public Object getValue() {
        return this.elements;
    }

    @Override
    public void setValue(Object value) {
        // The only allowed case is a direct assignment of another list of the same type.
        if (value instanceof ListType) {
            String leftContainedType = getContainedType();
            String resultContainedType = ((ListType) value).getContainedType();
            if (leftContainedType == null || resultContainedType == null) {
                throw new IllegalStateException("Unable to determine the contained types for the list variables in the assign expression.");
            } else if (!leftContainedType.equals(resultContainedType)) {
                throw new IllegalStateException("The assigned variable is of type list["+leftContainedType+"] whereas the variable toassign is of type list["+resultContainedType+"]");
            } else {
                elements.clear();
                List<DataType> items = (List<DataType>)((ListType) value).getValue();
                for (DataType item: items) {
                    append(item);
                }
            }
        } else {
            throw new IllegalStateException("Only list types can be directly assigned to other list types.");
        }
    }

    @Override
    public boolean isEmpty() {
        return this.elements.isEmpty();
    }

    @Override
    public int getSize() {
        return this.elements.size();
    }

    @Override
    public ListType toListType() {
        ListType list = new ListType(getContainedType());
        for (DataType obj: elements) {
            list.append(obj);
        }
        return list;
    }

    @Override
    public MapType toMapType() {
        MapType map = new MapType();
        int counter = 0;
        for (DataType obj: elements) {
            map.addItem(String.valueOf(counter++), obj);
        }
        return map;
    }
}
