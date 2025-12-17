
package com.gitb.engine.messaging.handlers.layer.application.domibus.stub.ecodex;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for mshRole.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <pre>{@code
 * <simpleType name="mshRole">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="SENDING"/>
 *     <enumeration value="RECEIVING"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@XmlType(name = "mshRole")
@XmlEnum
public enum MshRole {

    SENDING,
    RECEIVING;

    public String value() {
        return name();
    }

    public static MshRole fromValue(String v) {
        return valueOf(v);
    }

}
