package com.gitb.types;

import com.gitb.core.ErrorCode;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.utils.ErrorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import javax.xml.xpath.*;
import java.io.*;

/**
 * Created by senan on 9/8/14.
 */
public class ObjectType extends DataType {
    private static Logger logger = LoggerFactory.getLogger(ObjectType.class);
    public static String DEFAULT_COMMON_ENCODING_FORMAT = "utf-8";
    public static String DEFAULT_ENCODING = "utf-8";

    //The default storage type is XML
    protected Node value;

    public ObjectType() {
        try {
            value =  DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
	        throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.DATATYPE_ERROR, "Document could not be constructed."), e);
        }
    }

    public ObjectType(Node node) {
        value = node;
    }

    @Override
    public DataType processXPath(XPathExpression expression, String returnType) {
        QName type = XPathConstantForDataType(returnType);
        if(type == null) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Expression cannot return the expected ["+returnType+"] return type..."));
        }
        //Create an empty value for the expected return type
        DataType result = DataTypeFactory.getInstance().create(returnType);
        try {
            //Evaluate the expression with the expected type
            Object object = expression.evaluate(this.value, type);
            //If return type is primitive directly set the value (Java types matches)
            if (result instanceof PrimitiveType){
                result.setValue(object);
            }
            //If return type is a list type, cast each Node in the NodeList to the contained type
            else if(result instanceof ListType){
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
                result.deserialize(serializeNode((Node)object), ObjectType.DEFAULT_COMMON_ENCODING_FORMAT);
            }
        } catch (XPathExpressionException e) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Error while evaluating XPath expression"), e);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public String getType() {
        return DataType.OBJECT_DATA_TYPE;
    }


    @Override
    public void deserialize(byte[] content, String encoding) {
        InputSource inputSource = new InputSource(new ByteArrayInputStream(content));
        inputSource.setEncoding(encoding);
        deserialize(inputSource);
    }

    protected void deserialize(InputSource inputSource) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputSource);
            setValue(document);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
            te.printStackTrace();
            //TODO Handle exception
        }
        return baos.toByteArray();
    }

    protected Transformer constructTransformerForSerialization() throws TransformerConfigurationException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
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
            logger.error("Transformer exception when converting Node to String");
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
