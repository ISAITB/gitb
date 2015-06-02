package com.gitb.types;

import com.gitb.tdl.Binding;
import com.gitb.tdl.TypedBinding;
import com.gitb.tdl.Variable;

import javax.xml.crypto.Data;
import javax.xml.xpath.XPathExpression;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tuncay on 9/2/14.
 */
public abstract class DataType {
    public static final String BOOLEAN_DATA_TYPE = "boolean";
    public static final String NUMBER_DATA_TYPE = "number";
    public static final String STRING_DATA_TYPE = "string";
    public static final String BINARY_DATA_TYPE = "binary";
    public static final String LIST_DATA_TYPE = "list";
    public static final String MAP_DATA_TYPE = "map";
    public static final String SCHEMA_DATA_TYPE="schema";
    public static final String OBJECT_DATA_TYPE = "object";
    public static final String[] CONTAINER_TYPE_PARENTHESIS = {"[", "]"};

	//public static final String DEFAULT_ENCODING = "utf-8";

    public abstract String getType();

    public abstract DataType processXPath(XPathExpression expression, String returnType);

    public abstract void deserialize(byte[] content, String encoding);

	public abstract void deserialize(byte[] content);

    public abstract void deserialize(InputStream inputStream, String encoding);

	public abstract void deserialize(InputStream inputStream);

    public abstract OutputStream serializeToStream(String encoding);

    public abstract OutputStream serializeToStreamByDefaultEncoding();

    public abstract byte[] serialize(String encoding);

    public abstract byte[] serializeByDefaultEncoding();

    public abstract Object getValue();

    public abstract void setValue(Object value);
}
