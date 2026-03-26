
package com.gitb.engine.messaging.handlers.layer.application.domibus.stub.ecodex;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>{@code
 * <complexType>
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="bodyload" type="{http://org.ecodex.backend/1_1/}LargePayloadType" minOccurs="0"/>
 *         <element name="payload" type="{http://org.ecodex.backend/1_1/}LargePayloadType" maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "bodyload",
    "payload"
})
@XmlRootElement(name = "retrieveMessageResponse")
public class RetrieveMessageResponse {

    protected LargePayloadType bodyload;
    protected List<LargePayloadType> payload;

    /**
     * Gets the value of the bodyload property.
     * 
     * @return
     *     possible object is
     *     {@link LargePayloadType }
     *     
     */
    public LargePayloadType getBodyload() {
        return bodyload;
    }

    /**
     * Sets the value of the bodyload property.
     * 
     * @param value
     *     allowed object is
     *     {@link LargePayloadType }
     *     
     */
    public void setBodyload(LargePayloadType value) {
        this.bodyload = value;
    }

    /**
     * Gets the value of the payload property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a {@code set} method for the payload property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPayload().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link LargePayloadType }
     * 
     * 
     * @return
     *     The value of the payload property.
     */
    public List<LargePayloadType> getPayload() {
        if (payload == null) {
            payload = new ArrayList<>();
        }
        return this.payload;
    }

}
