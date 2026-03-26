
package com.gitb.engine.messaging.handlers.layer.application.domibus.stub.ecodex;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;

import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for errorResultImpl complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>{@code
 * <complexType name="errorResultImpl">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="errorCode" type="{http://org.ecodex.backend/1_1/}errorCode" minOccurs="0"/>
 *         <element name="errorDetail" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="messageInErrorId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="mshRole" type="{http://org.ecodex.backend/1_1/}mshRole" minOccurs="0"/>
 *         <element name="notified" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
 *         <element name="timestamp" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "errorResultImpl", propOrder = {
    "errorCode",
    "errorDetail",
    "messageInErrorId",
    "mshRole",
    "notified",
    "timestamp"
})
public class ErrorResultImpl {

    @XmlSchemaType(name = "string")
    protected ErrorCode errorCode;
    protected String errorDetail;
    protected String messageInErrorId;
    @XmlSchemaType(name = "string")
    protected MshRole mshRole;
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar notified;
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar timestamp;

    /**
     * Gets the value of the errorCode property.
     * 
     * @return
     *     possible object is
     *     {@link ErrorCode }
     *     
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Sets the value of the errorCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link ErrorCode }
     *     
     */
    public void setErrorCode(ErrorCode value) {
        this.errorCode = value;
    }

    /**
     * Gets the value of the errorDetail property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getErrorDetail() {
        return errorDetail;
    }

    /**
     * Sets the value of the errorDetail property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setErrorDetail(String value) {
        this.errorDetail = value;
    }

    /**
     * Gets the value of the messageInErrorId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMessageInErrorId() {
        return messageInErrorId;
    }

    /**
     * Sets the value of the messageInErrorId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMessageInErrorId(String value) {
        this.messageInErrorId = value;
    }

    /**
     * Gets the value of the mshRole property.
     * 
     * @return
     *     possible object is
     *     {@link MshRole }
     *     
     */
    public MshRole getMshRole() {
        return mshRole;
    }

    /**
     * Sets the value of the mshRole property.
     * 
     * @param value
     *     allowed object is
     *     {@link MshRole }
     *     
     */
    public void setMshRole(MshRole value) {
        this.mshRole = value;
    }

    /**
     * Gets the value of the notified property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getNotified() {
        return notified;
    }

    /**
     * Sets the value of the notified property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setNotified(XMLGregorianCalendar value) {
        this.notified = value;
    }

    /**
     * Gets the value of the timestamp property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the value of the timestamp property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setTimestamp(XMLGregorianCalendar value) {
        this.timestamp = value;
    }

}
