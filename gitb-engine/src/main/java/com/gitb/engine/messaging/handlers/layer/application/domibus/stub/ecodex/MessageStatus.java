
package com.gitb.engine.messaging.handlers.layer.application.domibus.stub.ecodex;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for messageStatus.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <pre>{@code
 * <simpleType name="messageStatus">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="READY_TO_SEND"/>
 *     <enumeration value="READY_TO_PULL"/>
 *     <enumeration value="BEING_PULLED"/>
 *     <enumeration value="SEND_ENQUEUED"/>
 *     <enumeration value="SEND_IN_PROGRESS"/>
 *     <enumeration value="WAITING_FOR_RECEIPT"/>
 *     <enumeration value="ACKNOWLEDGED"/>
 *     <enumeration value="ACKNOWLEDGED_WITH_WARNING"/>
 *     <enumeration value="SEND_ATTEMPT_FAILED"/>
 *     <enumeration value="SEND_FAILURE"/>
 *     <enumeration value="NOT_FOUND"/>
 *     <enumeration value="WAITING_FOR_RETRY"/>
 *     <enumeration value="RECEIVED"/>
 *     <enumeration value="RECEIVED_WITH_WARNINGS"/>
 *     <enumeration value="DELETED"/>
 *     <enumeration value="DOWNLOADED"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@XmlType(name = "messageStatus")
@XmlEnum
public enum MessageStatus {

    READY_TO_SEND,
    READY_TO_PULL,
    BEING_PULLED,
    SEND_ENQUEUED,
    SEND_IN_PROGRESS,
    WAITING_FOR_RECEIPT,
    ACKNOWLEDGED,
    ACKNOWLEDGED_WITH_WARNING,
    SEND_ATTEMPT_FAILED,
    SEND_FAILURE,
    NOT_FOUND,
    WAITING_FOR_RETRY,
    RECEIVED,
    RECEIVED_WITH_WARNINGS,
    DELETED,
    DOWNLOADED;

    public String value() {
        return name();
    }

    public static MessageStatus fromValue(String v) {
        return valueOf(v);
    }

}
