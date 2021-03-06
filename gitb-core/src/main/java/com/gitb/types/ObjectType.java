package com.gitb.types;

import com.gitb.core.ErrorCode;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.utils.BomStrippingReader;
import com.gitb.utils.ErrorUtils;
import com.gitb.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;

/**
 * Created by senan on 9/8/14.
 */
public class ObjectType extends DataType {
    public static String DEFAULT_COMMON_ENCODING_FORMAT = "utf-8";
    public static String DEFAULT_ENCODING = "utf-8";

    //The default storage type is XML
    protected Node value;

    public ObjectType() {
        try {
            value =  XMLUtils.getSecureDocumentBuilderFactory().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
	        throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.DATATYPE_ERROR, "Document could not be constructed."), e);
        }
    }

    public ObjectType(Node node) {
        value = node;
    }

    @Override
    public DataType processXPath(XPathExpression expression, String returnType) {
        if (returnType == null) {
            returnType = DataType.STRING_DATA_TYPE;
        }
        QName type = XPathConstantForDataType(returnType);
        if(type == null) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Expression cannot return the expected ["+returnType+"] return type..."));
        }
        //Create an empty value for the expected return type
        DataType result = DataTypeFactory.getInstance().create(returnType);
        try {
            //Evaluate the expression with the expected type
            Object object;
            try {
                object = expression.evaluate(this.value, type);
            } catch (XPathExpressionException e) {
                // Make a second attempt, evaluating as a string
                object = expression.evaluate(this.value);
            }
            //If return type is a list type, cast each Node in the NodeList to the contained type
            if(result instanceof ListType){
                String containedType = ((ListType) result).getContainedType();
                NodeList nodeList = (NodeList) object;
                for(int i=0; i<nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);
                    DataType item = DataTypeFactory.getInstance().create(containedType);
                    if(item instanceof  PrimitiveType){
                        item.deserialize(node.getTextContent().getBytes());
                    }else if (item instanceof ObjectType){
                        item.setValue(node);
                    }else {
                        //For other types, try casting from XML format
                        item.deserialize(serializeNode(node), ObjectType.DEFAULT_COMMON_ENCODING_FORMAT);
                    }
                    ((ListType) result).append(item);
                }
            }
            //For other types
            else {
                //Try casting from XML format
                if (object instanceof Node) {
                    result.deserialize(serializeNode((Node) object), ObjectType.DEFAULT_COMMON_ENCODING_FORMAT);
                } else {
                    DataType resultType;
                    if (object instanceof Boolean) {
                        resultType = new BooleanType((Boolean)object);
                    } else if (object instanceof Number) {
                        resultType = new NumberType();
                        resultType.setValue(((Number)object).doubleValue());
                    } else if (object instanceof String) {
                        resultType = new StringType((String)object);
                    } else {
                        throw new IllegalStateException("Could not parse the result of expression ["+expression+"] as a ["+returnType+"]");
                    }
                    result = resultType.convertTo(result.getType());
                }
            }
        } catch (XPathExpressionException e) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Error while evaluating XPath expression"), e);
        }

        return result;
    }

    @Override
    public String getType() {
        return DataType.OBJECT_DATA_TYPE;
    }


    @Override
    public void deserialize(byte[] content, String encoding) {
        InputSource inputSource = null;
        if (content != null && content.length > 0) {
            inputSource = new InputSource(new BomStrippingReader(new ByteArrayInputStream(content)));
            inputSource.setEncoding(encoding);
        }
        deserialize(inputSource);
    }

    protected void deserialize(InputSource inputSource) {
        try {
            DocumentBuilderFactory factory = XMLUtils.getSecureDocumentBuilderFactory();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document;
            if (inputSource != null) {
                document = builder.parse(inputSource);
            } else {
                document = builder.newDocument();
            }
            setValue(document);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException("Unable to deserialise variable as XML", e);
        }
    }

    @Override
    public void deserialize(byte[] content)  {
        deserialize(content, DEFAULT_ENCODING);
    }

    @Override
    public void deserialize(InputStream inputStream, String encoding)   {
        InputSource inputSource = new InputSource(inputStream);
        inputSource.setEncoding(encoding);
        deserialize(inputSource);
    }

    @Override
    public void deserialize(InputStream inputStream)  {
        deserialize(inputStream, DEFAULT_ENCODING);
    }

    @Override
    public OutputStream serializeToStream(String encoding)  {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Transformer t = constructTransformerForSerialization();
            t.setOutputProperty(OutputKeys.ENCODING, encoding);
            t.transform(new DOMSource(value), new StreamResult(baos));
        } catch (TransformerException te) {
            te.printStackTrace();
        }
        return baos;
    }

    @Override
    public OutputStream serializeToStreamByDefaultEncoding()  {
        return serializeToStream(DEFAULT_ENCODING);
    }

    @Override
    public byte[] serialize(String encoding) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Transformer t = constructTransformerForSerialization();
            t.setOutputProperty(OutputKeys.ENCODING, encoding);
            t.transform(new DOMSource(value), new StreamResult(baos));
        } catch (TransformerException te) {
            te.printStackTrace();
            //TODO Handle exception
        }
        return baos.toByteArray();
    }

    @Override
    public byte[] serializeByDefaultEncoding()  {
        return serialize(DEFAULT_ENCODING);
    }

    /**
     * Serialize a Node to OutputStream
     * @param item
     * @return
     */
    protected byte[] serializeNode(Node item) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Transformer t = constructTransformerForSerialization();
            t.transform(new DOMSource(item), new StreamResult(baos));
        } catch (TransformerException te) {
            throw new IllegalStateException(te);
        }
        return baos.toByteArray();
    }

    protected Transformer constructTransformerForSerialization() throws TransformerConfigurationException {
        Transformer transformer = XMLUtils.getSecureTransformerFactory().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        return transformer;
    }

    public String toString() {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = constructTransformerForSerialization();
            t.transform(new DOMSource(value), new StreamResult(sw));
        } catch (TransformerException te) {
            throw new IllegalStateException("Transformer exception when converting Node to String", te);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sw.toString();
    }

    @Override
    public Object getValue() {
        return this.value;
    }

    @Override
    public void setValue(Object value) {
        this.value = (Node) value;
    }

    public static QName XPathConstantForDataType(String type) {
        switch(type) {
            case DataType.OBJECT_DATA_TYPE:
                return XPathConstants.NODE;
            case DataType.STRING_DATA_TYPE:
                return XPathConstants.STRING;
            case DataType.BOOLEAN_DATA_TYPE:
                return XPathConstants.BOOLEAN;
            case DataType.NUMBER_DATA_TYPE:
                return XPathConstants.NUMBER;
            default:
                if(DataTypeFactory.isContainerType(type))
                    return XPathConstants.NODESET;
                else
                    return XPathConstants.NODE;
        }
    }

    @Override
    public StringType toStringType() {
        return new StringType(new String(serializeByDefaultEncoding()));
    }

    @Override
    public BinaryType toBinaryType() {
        BinaryType type = new BinaryType();
        type.setValue(serializeByDefaultEncoding());
        return type;
    }

    @Override
    public SchemaType toSchemaType() {
        SchemaType type = new SchemaType();
        type.setValue(getValue());
        return type;
    }

}
