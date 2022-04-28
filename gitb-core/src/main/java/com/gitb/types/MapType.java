package com.gitb.types;

import javax.xml.xpath.XPathExpression;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by senan on 9/8/14.
 */
public class MapType extends ContainerType {
    //Elements of the map
    private Map<String, DataType> elements;

	public MapType() {
		elements = new LinkedHashMap<>();
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

    public Map<String, DataType> getItems() {
        return elements;
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
    protected MapType toMapType() {
        MapType map = new MapType();
        if (elements != null) {
            for (Map.Entry<String, DataType> entry: elements.entrySet()) {
                map.addItem(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    public void clear() {
	    if (elements != null) {
	        elements.clear();
        }
    }

    @Override
    protected StringType toStringType() {
        StringType type = new StringType();
        StringBuilder str = new StringBuilder();
        var iterator = elements.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            str.append("[").append(entry.getKey()).append("]=[").append((String) entry.getValue().convertTo(STRING_DATA_TYPE).getValue()).append("]");
            if (iterator.hasNext()) {
                str.append(",");
            }
        }
        type.setValue(str.toString());
        return type;
    }

    public static MapType fromMap(Map<String, String> map) {
        var mapType = new MapType();
        if (map != null) {
            for (var entry: map.entrySet()) {
                mapType.addItem(entry.getKey(), new StringType(entry.getValue()));
            }
        }
        return mapType;
    }

}
