
package com.gitb.engine.messaging.handlers.layer.application.domibus.stub.ebms;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for UserMessage complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>{@code
 * <complexType name="UserMessage">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <all>
 *         <element name="MessageInfo" type="{http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/}MessageInfo" minOccurs="0"/>
 *         <element name="PartyInfo" type="{http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/}PartyInfo"/>
 *         <element name="CollaborationInfo" type="{http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/}CollaborationInfo"/>
 *         <element name="MessageProperties" type="{http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/}MessageProperties" minOccurs="0"/>
 *         <element name="PayloadInfo" type="{http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/}PayloadInfo" minOccurs="0"/>
 *       </all>
 *       <attribute name="mpc" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UserMessage", propOrder = {

})
public class UserMessage {

    @XmlElement(name = "MessageInfo")
    protected MessageInfo messageInfo;
    @XmlElement(name = "PartyInfo", required = true)
    protected PartyInfo partyInfo;
    @XmlElement(name = "CollaborationInfo", required = true)
    protected CollaborationInfo collaborationInfo;
    @XmlElement(name = "MessageProperties")
    protected MessageProperties messageProperties;
    @XmlElement(name = "PayloadInfo")
    protected PayloadInfo payloadInfo;
    @XmlAttribute(name = "mpc")
    @XmlSchemaType(name = "anyURI")
    protected String mpc;

    /**
     * Gets the value of the messageInfo property.
     * 
     * @return
     *     possible object is
     *     {@link MessageInfo }
     *     
     */
    public MessageInfo getMessageInfo() {
        return messageInfo;
    }

    /**
     * Sets the value of the messageInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link MessageInfo }
     *     
     */
    public void setMessageInfo(MessageInfo value) {
        this.messageInfo = value;
    }

    /**
     * Gets the value of the partyInfo property.
     * 
     * @return
     *     possible object is
     *     {@link PartyInfo }
     *     
     */
    public PartyInfo getPartyInfo() {
        return partyInfo;
    }

    /**
     * Sets the value of the partyInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link PartyInfo }
     *     
     */
    public void setPartyInfo(PartyInfo value) {
        this.partyInfo = value;
    }

    /**
     * Gets the value of the collaborationInfo property.
     * 
     * @return
     *     possible object is
     *     {@link CollaborationInfo }
     *     
     */
    public CollaborationInfo getCollaborationInfo() {
        return collaborationInfo;
    }

    /**
     * Sets the value of the collaborationInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link CollaborationInfo }
     *     
     */
    public void setCollaborationInfo(CollaborationInfo value) {
        this.collaborationInfo = value;
    }

    /**
     * Gets the value of the messageProperties property.
     * 
     * @return
     *     possible object is
     *     {@link MessageProperties }
     *     
     */
    public MessageProperties getMessageProperties() {
        return messageProperties;
    }

    /**
     * Sets the value of the messageProperties property.
     * 
     * @param value
     *     allowed object is
     *     {@link MessageProperties }
     *     
     */
    public void setMessageProperties(MessageProperties value) {
        this.messageProperties = value;
    }

    /**
     * Gets the value of the payloadInfo property.
     * 
     * @return
     *     possible object is
     *     {@link PayloadInfo }
     *     
     */
    public PayloadInfo getPayloadInfo() {
        return payloadInfo;
    }

    /**
     * Sets the value of the payloadInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link PayloadInfo }
     *     
     */
    public void setPayloadInfo(PayloadInfo value) {
        this.payloadInfo = value;
    }

    /**
     * Gets the value of the mpc property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMpc() {
        return mpc;
    }

    /**
     * Sets the value of the mpc property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMpc(String value) {
        this.mpc = value;
    }

}
