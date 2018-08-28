package com.gitb.types;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathExpression;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by senan on 9/8/14.
 */
public class MapType extends ContainerType {
    private static Logger logger = LoggerFactory.getLogger(MapType.class);
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
        logger.error("Invalid Test Case", "You can not run an expression over Map type");
        //TODO throw Invalid Test case exception
        return null;
    }

    @Override
    public Object getValue() {
        return elements;
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
