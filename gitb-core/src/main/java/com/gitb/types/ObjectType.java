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

import com.gitb.core.ErrorCode;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.utils.BomStrippingReader;
import com.gitb.utils.ErrorUtils;
import com.gitb.utils.XMLUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
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

    public static final String DEFAULT_ENCODING = "utf-8";

    //The default storage type is XML
    protected Node value;
    private String encoding = DEFAULT_ENCODING;
    private Long size = null;

    public ObjectType() {
        value = emptyNode();
    }

    public ObjectType(Node node) {
        value = node;
    }

    protected Node emptyNode() {
        try {
            return XMLUtils.getSecureDocumentBuilderFactory().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.DATATYPE_ERROR, "Document could not be constructed."), e);
        }
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
                object = expression.evaluate(getValue(), type);
            } catch (XPathExpressionException e) {
                // Make a second attempt, evaluating as a string
                object = expression.evaluate(getValue());
            }
            //If return type is a list type, cast each Node in the NodeList to the contained type
            if(result instanceof ListType){
                String containedType = ((ListType) result).getContainedType();
                if (object instanceof NodeList) {
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
                            item.deserialize(serializeNode(node), ObjectType.DEFAULT_ENCODING);
                        }
                        ((ListType) result).append(item);
                    }
                } else if (object instanceof String) {
                    var item = new StringType();
                    item.setValue(object);
                    ((ListType) result).append(item);
                }
            }
            //For other types
            else {
                //Try casting from XML format
                if (object instanceof Node) {
                    result.deserialize(serializeNode((Node) object), ObjectType.DEFAULT_ENCODING);
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
    public void deserialize(byte[] content)  {
        deserialize(content, DEFAULT_ENCODING);
    }

    @Override
    public void deserialize(byte[] content, String encoding) {
        if (content != null && content.length > 0) {
            deserialize(new ByteArrayInputStream(content), encoding);
        }
    }

    @Override
    public void deserialize(InputStream inputStream, String encoding) {
        try (var in = BoundedInputStream.builder().setInputStream(inputStream).get()) {
            InputSource inputSource = new InputSource(new BomStrippingReader(in));
            inputSource.setEncoding(encoding);
            deserialize(inputSource);
            size = in.getCount();
        } catch (IOException e) {
            throw new IllegalStateException("Error while reading stream.", e);
        }
    }

    @Override
    public void deserialize(InputStream inputStream)  {
        deserialize(inputStream, DEFAULT_ENCODING);
    }

    protected Node deserializeToNode(InputSource inputSource) {
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
            return document;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException("Unable to deserialise variable as XML", e);
        }
    }

    private void deserialize(InputSource inputSource) {
        String encoding = DEFAULT_ENCODING;
        if (inputSource != null && inputSource.getEncoding() != null) {
            encoding = inputSource.getEncoding();
        }
        setEncoding(encoding);
        setValue(deserializeToNode(inputSource));
    }

    private byte[] serializeNode(Node item) {
        return serializeNodeToByteStream(item, null).toByteArray();
    }

    protected ByteArrayOutputStream serializeNodeToByteStream(Node item, String encoding) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Transformer t = constructTransformerForSerialization();
            if (encoding != null) {
                t.setOutputProperty(OutputKeys.ENCODING, encoding);
            }
            t.transform(new DOMSource(item), new StreamResult(baos));
        } catch (TransformerException te) {
            throw new IllegalStateException("Unable to serialize object type", te);
        }
        return baos;
    }

    protected ByteArrayOutputStream serializeToByteStream(String encoding) {
        return serializeNodeToByteStream((Node) getValue(), encoding);
    }

    @Override
    public OutputStream serializeToStream(String encoding)  {
        return serializeToByteStream(encoding);
    }

    @Override
    public OutputStream serializeToStreamByDefaultEncoding()  {
        return serializeToStream(DEFAULT_ENCODING);
    }

    @Override
    public byte[] serialize(String encoding) {
        return serializeToByteStream(encoding).toByteArray();
    }

    @Override
    public byte[] serializeByDefaultEncoding()  {
        return serialize(DEFAULT_ENCODING);
    }

    private Transformer constructTransformerForSerialization() throws TransformerConfigurationException {
        Transformer transformer = XMLUtils.getSecureTransformerFactory().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        return transformer;
    }

    public String toString() {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = constructTransformerForSerialization();
            t.transform(new DOMSource((Node) getValue()), new StreamResult(sw));
        } catch (TransformerException te) {
            throw new IllegalStateException("Transformer exception when converting Node to String", te);
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
        this.size = null;
    }

    private QName XPathConstantForDataType(String type) {
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
    protected StringType toStringType() {
        return new StringType(new String(serializeByDefaultEncoding()));
    }

    @Override
    protected BinaryType toBinaryType() {
        BinaryType type = new BinaryType();
        type.setValue(serialize(getEncoding()));
        return type;
    }

    @Override
    public SchemaType toSchemaType() {
        SchemaType type = new SchemaType();
        type.setEncoding(getEncoding());
        type.setSize(getSize());
        type.setValue(getValue());
        return type;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
