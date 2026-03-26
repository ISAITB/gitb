
package com.gitb.engine.messaging.handlers.layer.application.domibus.stub.ecodex;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for errorResultImplArray complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>{@code
 * <complexType name="errorResultImplArray">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="item" type="{http://org.ecodex.backend/1_1/}errorResultImpl" maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "errorResultImplArray", propOrder = {
    "item"
})
public class ErrorResultImplArray {

    @XmlElement(nillable = true)
    protected List<ErrorResultImpl> item;

    /**
     * Gets the value of the item property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a {@code set} method for the item property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getItem().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ErrorResultImpl }
     * 
     * 
     * @return
     *     The value of the item property.
     */
    public List<ErrorResultImpl> getItem() {
        if (item == null) {
            item = new ArrayList<>();
        }
        return this.item;
    }

}
