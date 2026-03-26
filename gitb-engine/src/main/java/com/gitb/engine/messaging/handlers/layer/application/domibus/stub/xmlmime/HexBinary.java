
package com.gitb.engine.messaging.handlers.layer.application.domibus.stub.xmlmime;

import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.HexBinaryAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for hexBinary complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>{@code
 * <complexType name="hexBinary">
 *   <simpleContent>
 *     <extension base="<http://www.w3.org/2001/XMLSchema>hexBinary">
 *       <attribute ref="{http://www.w3.org/2005/05/xmlmime}contentType"/>
 *     </extension>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "hexBinary", propOrder = {
    "value"
})
public class HexBinary {

    @XmlValue
    @XmlJavaTypeAdapter(HexBinaryAdapter.class)
    @XmlSchemaType(name = "hexBinary")
    protected byte[] value;
    @XmlAttribute(name = "contentType", namespace = "http://www.w3.org/2005/05/xmlmime")
    protected String contentType;

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public byte[] getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(byte[] value) {
        this.value = value;
    }

    /**
     * Gets the value of the contentType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the value of the contentType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setContentType(String value) {
        this.contentType = value;
    }

}
