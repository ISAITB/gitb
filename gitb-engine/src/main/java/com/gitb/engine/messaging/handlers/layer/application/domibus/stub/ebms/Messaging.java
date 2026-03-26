
package com.gitb.engine.messaging.handlers.layer.application.domibus.stub.ebms;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for Messaging complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>{@code
 * <complexType name="Messaging">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="UserMessage" type="{http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/}UserMessage" minOccurs="0"/>
 *       </sequence>
 *       <attribute name="mustUnderstand" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlRootElement(name = "Messaging")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Messaging", propOrder = {
    "userMessage"
})
public class Messaging {

    @XmlElement(name = "UserMessage")
    protected UserMessage userMessage;
    @XmlAttribute(name = "mustUnderstand")
    protected Boolean mustUnderstand;

    /**
     * Gets the value of the userMessage property.
     * 
     * @return
     *     possible object is
     *     {@link UserMessage }
     *     
     */
    public UserMessage getUserMessage() {
        return userMessage;
    }

    /**
     * Sets the value of the userMessage property.
     * 
     * @param value
     *     allowed object is
     *     {@link UserMessage }
     *     
     */
    public void setUserMessage(UserMessage value) {
        this.userMessage = value;
    }

    /**
     * Gets the value of the mustUnderstand property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isMustUnderstand() {
        return mustUnderstand;
    }

    /**
     * Sets the value of the mustUnderstand property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setMustUnderstand(Boolean value) {
        this.mustUnderstand = value;
    }

}
