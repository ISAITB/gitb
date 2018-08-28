package com.gitb.types;

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
        if(this.elements != null)
            this.elements.add(element);
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
        return DataType.LIST_DATA_TYPE + DataType.CONTAINER_TYPE_PARENTHESIS[0] + containedType + DataType.CONTAINER_TYPE_PARENTHESIS[1];
    }

    @Override
    public Object getValue() {
        return this.elements;
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
        ListType list = new ListType();
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
