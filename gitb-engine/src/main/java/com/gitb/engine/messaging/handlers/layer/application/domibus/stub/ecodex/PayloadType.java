
package com.gitb.engine.messaging.handlers.layer.application.domibus.stub.ecodex;

import com.gitb.engine.messaging.handlers.layer.application.domibus.stub.xmlmime.Base64Binary;
import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for PayloadType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>{@code
 * <complexType name="PayloadType">
 *   <simpleContent>
 *     <extension base="<http://www.w3.org/2005/05/xmlmime>base64Binary">
 *       <attribute name="payloadId" use="required" type="{http://www.w3.org/2001/XMLSchema}token" />
 *     </extension>
 *   </simpleContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PayloadType")
public class PayloadType
    extends Base64Binary
{

    @XmlAttribute(name = "payloadId", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String payloadId;

    /**
     * Gets the value of the payloadId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPayloadId() {
        return payloadId;
    }

    /**
     * Sets the value of the payloadId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPayloadId(String value) {
        this.payloadId = value;
    }

}
