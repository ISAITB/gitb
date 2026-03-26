
package com.gitb.engine.messaging.handlers.layer.application.domibus.stub.ecodex;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for errorCode.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <pre>{@code
 * <simpleType name="errorCode">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="EBMS_0001"/>
 *     <enumeration value="EBMS_0002"/>
 *     <enumeration value="EBMS_0003"/>
 *     <enumeration value="EBMS_0004"/>
 *     <enumeration value="EBMS_0005"/>
 *     <enumeration value="EBMS_0006"/>
 *     <enumeration value="EBMS_0007"/>
 *     <enumeration value="EBMS_0008"/>
 *     <enumeration value="EBMS_0009"/>
 *     <enumeration value="EBMS_0010"/>
 *     <enumeration value="EBMS_0011"/>
 *     <enumeration value="EBMS_0101"/>
 *     <enumeration value="EBMS_0102"/>
 *     <enumeration value="EBMS_0103"/>
 *     <enumeration value="EBMS_0201"/>
 *     <enumeration value="EBMS_0202"/>
 *     <enumeration value="EBMS_0301"/>
 *     <enumeration value="EBMS_0302"/>
 *     <enumeration value="EBMS_0303"/>
 *     <enumeration value="EBMS_0020"/>
 *     <enumeration value="EBMS_0021"/>
 *     <enumeration value="EBMS_0022"/>
 *     <enumeration value="EBMS_0023"/>
 *     <enumeration value="EBMS_0030"/>
 *     <enumeration value="EBMS_0031"/>
 *     <enumeration value="EBMS_0040"/>
 *     <enumeration value="EBMS_0041"/>
 *     <enumeration value="EBMS_0042"/>
 *     <enumeration value="EBMS_0043"/>
 *     <enumeration value="EBMS_0044"/>
 *     <enumeration value="EBMS_0045"/>
 *     <enumeration value="EBMS_0046"/>
 *     <enumeration value="EBMS_0047"/>
 *     <enumeration value="EBMS_0048"/>
 *     <enumeration value="EBMS_0049"/>
 *     <enumeration value="EBMS_0050"/>
 *     <enumeration value="EBMS_0051"/>
 *     <enumeration value="EBMS_0052"/>
 *     <enumeration value="EBMS_0053"/>
 *     <enumeration value="EBMS_0054"/>
 *     <enumeration value="EBMS_0055"/>
 *     <enumeration value="EBMS_0060"/>
 *     <enumeration value="EBMS_0065"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@XmlType(name = "errorCode")
@XmlEnum
public enum ErrorCode {

    EBMS_0001,
    EBMS_0002,
    EBMS_0003,
    EBMS_0004,
    EBMS_0005,
    EBMS_0006,
    EBMS_0007,
    EBMS_0008,
    EBMS_0009,
    EBMS_0010,
    EBMS_0011,
    EBMS_0101,
    EBMS_0102,
    EBMS_0103,
    EBMS_0201,
    EBMS_0202,
    EBMS_0301,
    EBMS_0302,
    EBMS_0303,
    EBMS_0020,
    EBMS_0021,
    EBMS_0022,
    EBMS_0023,
    EBMS_0030,
    EBMS_0031,
    EBMS_0040,
    EBMS_0041,
    EBMS_0042,
    EBMS_0043,
    EBMS_0044,
    EBMS_0045,
    EBMS_0046,
    EBMS_0047,
    EBMS_0048,
    EBMS_0049,
    EBMS_0050,
    EBMS_0051,
    EBMS_0052,
    EBMS_0053,
    EBMS_0054,
    EBMS_0055,
    EBMS_0060,
    EBMS_0065;

    public String value() {
        return name();
    }

    public static ErrorCode fromValue(String v) {
        return valueOf(v);
    }

}
