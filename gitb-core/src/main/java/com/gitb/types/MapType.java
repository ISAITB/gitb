/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package com.gitb.types;

import javax.xml.xpath.XPathExpression;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by senan on 9/8/14.
 */
public class MapType extends ContainerType {
    //Elements of the map
    private final Map<String, DataType> elements;

	public MapType() {
		elements = new LinkedHashMap<>();
	}

	public void addItem(String key, DataType element){
        elements.put(key, element);
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
        Optional<Map<String, DataType>> itemsToSet = Optional.empty();
        // The only allowed case is a direct assignment of another map.
        if (value instanceof MapType) {
            itemsToSet = Optional.of((Map<String, DataType>)((MapType) value).getValue());
        } else if (value instanceof Map) {
            itemsToSet = Optional.of((Map<String, DataType>)value);
        }
        if (itemsToSet.isPresent()) {
            elements.clear();
            elements.putAll(itemsToSet.get());
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
        for (Map.Entry<String, DataType> entry : elements.entrySet()) {
            map.addItem(entry.getKey(), entry.getValue());
        }
        return map;
    }

    public void clear() {
        elements.clear();
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
