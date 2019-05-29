package com.gitb.types;

import javax.xml.xpath.XPathExpression;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by senan on 9/8/14.
 */
public class MapType extends ContainerType {
    //Elements of the map
    private Map<String, DataType> elements;

	public MapType() {
		elements = new HashMap<>();
	}

	public void addItem(String key, DataType element){
        if(elements != null){
            elements.put(key, element);
        }
    }

	public DataType removeItem(String key) {
		return elements.remove(key);
	}

    public DataType getItem(String key){
        return elements.get(key);
    }

    @Override
    public String getType() {
        return DataType.MAP_DATA_TYPE;
    }

    @Override
    public DataType processXPath(XPathExpression expression, String returnType) {
	    throw new IllegalStateException("You can not run an expression over Map type");
    }

    @Override
    public Object getValue() {
        return elements;
    }

    @Override
    public void setValue(Object value) {
        // The only allowed case is a direct assignment of another list of the same type.
        if (value instanceof MapType) {
            elements.clear();
            Map<String, DataType> items = (Map<String, DataType>)((MapType) value).getValue();
            for (Map.Entry<String, DataType> entry: items.entrySet()) {
                elements.put(entry.getKey(), entry.getValue());
            }
        } else {
            throw new IllegalStateException("Only map types can be directly assigned to other map types.");
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
    public MapType toMapType() {
        MapType map = new MapType();
        if (elements != null) {
            for (Map.Entry<String, DataType> entry: elements.entrySet()) {
                map.addItem(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

}
