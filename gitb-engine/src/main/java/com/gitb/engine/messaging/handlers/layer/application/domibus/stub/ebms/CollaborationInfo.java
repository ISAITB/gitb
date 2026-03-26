
package com.gitb.engine.messaging.handlers.layer.application.domibus.stub.ebms;

import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for CollaborationInfo complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>{@code
 * <complexType name="CollaborationInfo">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <all>
 *         <element name="AgreementRef" type="{http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/}AgreementRef" minOccurs="0"/>
 *         <element name="Service" type="{http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/}Service"/>
 *         <element name="Action" type="{http://www.w3.org/2001/XMLSchema}token"/>
 *         <element name="ConversationId" type="{http://www.w3.org/2001/XMLSchema}token" minOccurs="0"/>
 *       </all>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CollaborationInfo", propOrder = {

})
public class CollaborationInfo {

    @XmlElement(name = "AgreementRef")
    protected AgreementRef agreementRef;
    @XmlElement(name = "Service", required = true)
    protected Service service;
    @XmlElement(name = "Action", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String action;
    @XmlElement(name = "ConversationId")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String conversationId;

    /**
     * Gets the value of the agreementRef property.
     * 
     * @return
     *     possible object is
     *     {@link AgreementRef }
     *     
     */
    public AgreementRef getAgreementRef() {
        return agreementRef;
    }

    /**
     * Sets the value of the agreementRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link AgreementRef }
     *     
     */
    public void setAgreementRef(AgreementRef value) {
        this.agreementRef = value;
    }

    /**
     * Gets the value of the service property.
     * 
     * @return
     *     possible object is
     *     {@link Service }
     *     
     */
    public Service getService() {
        return service;
    }

    /**
     * Sets the value of the service property.
     * 
     * @param value
     *     allowed object is
     *     {@link Service }
     *     
     */
    public void setService(Service value) {
        this.service = value;
    }

    /**
     * Gets the value of the action property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAction() {
        return action;
    }

    /**
     * Sets the value of the action property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAction(String value) {
        this.action = value;
    }

    /**
     * Gets the value of the conversationId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getConversationId() {
        return conversationId;
    }

    /**
     * Sets the value of the conversationId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setConversationId(String value) {
        this.conversationId = value;
    }

}
