
package com.gitb.engine.messaging.handlers.layer.application.domibus.stub.ecodex;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the backend.ecodex.org._1_1 package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName _ListPendingMessagesRequest_QNAME = new QName("http://org.ecodex.backend/1_1/", "listPendingMessagesRequest");
    private static final QName _GetStatusRequest_QNAME = new QName("http://org.ecodex.backend/1_1/", "getStatusRequest");
    private static final QName _StatusRequest_QNAME = new QName("http://org.ecodex.backend/1_1/", "statusRequest");
    private static final QName _GetStatusResponse_QNAME = new QName("http://org.ecodex.backend/1_1/", "getStatusResponse");
    private static final QName _GetErrorsRequest_QNAME = new QName("http://org.ecodex.backend/1_1/", "getErrorsRequest");
    private static final QName _GetMessageErrorsResponse_QNAME = new QName("http://org.ecodex.backend/1_1/", "getMessageErrorsResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: backend.ecodex.org._1_1
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link FaultDetail }
     * 
     * @return
     *     the new instance of {@link FaultDetail }
     */
    public FaultDetail createFaultDetail() {
        return new FaultDetail();
    }

    /**
     * Create an instance of {@link RetrieveMessageRequest }
     * 
     * @return
     *     the new instance of {@link RetrieveMessageRequest }
     */
    public RetrieveMessageRequest createRetrieveMessageRequest() {
        return new RetrieveMessageRequest();
    }

    /**
     * Create an instance of {@link RetrieveMessageResponse }
     * 
     * @return
     *     the new instance of {@link RetrieveMessageResponse }
     */
    public RetrieveMessageResponse createRetrieveMessageResponse() {
        return new RetrieveMessageResponse();
    }

    /**
     * Create an instance of {@link LargePayloadType }
     * 
     * @return
     *     the new instance of {@link LargePayloadType }
     */
    public LargePayloadType createLargePayloadType() {
        return new LargePayloadType();
    }

    /**
     * Create an instance of {@link ListPendingMessagesResponse }
     * 
     * @return
     *     the new instance of {@link ListPendingMessagesResponse }
     */
    public ListPendingMessagesResponse createListPendingMessagesResponse() {
        return new ListPendingMessagesResponse();
    }

    /**
     * Create an instance of {@link MessageErrorsRequest }
     * 
     * @return
     *     the new instance of {@link MessageErrorsRequest }
     */
    public MessageErrorsRequest createMessageErrorsRequest() {
        return new MessageErrorsRequest();
    }

    /**
     * Create an instance of {@link MessageStatusRequest }
     * 
     * @return
     *     the new instance of {@link MessageStatusRequest }
     */
    public MessageStatusRequest createMessageStatusRequest() {
        return new MessageStatusRequest();
    }

    /**
     * Create an instance of {@link SubmitRequest }
     * 
     * @return
     *     the new instance of {@link SubmitRequest }
     */
    public SubmitRequest createSubmitRequest() {
        return new SubmitRequest();
    }

    /**
     * Create an instance of {@link SubmitResponse }
     * 
     * @return
     *     the new instance of {@link SubmitResponse }
     */
    public SubmitResponse createSubmitResponse() {
        return new SubmitResponse();
    }

    /**
     * Create an instance of {@link GetStatusRequest }
     * 
     * @return
     *     the new instance of {@link GetStatusRequest }
     */
    public GetStatusRequest createGetStatusRequest() {
        return new GetStatusRequest();
    }

    /**
     * Create an instance of {@link StatusRequest }
     * 
     * @return
     *     the new instance of {@link StatusRequest }
     */
    public StatusRequest createStatusRequest() {
        return new StatusRequest();
    }

    /**
     * Create an instance of {@link GetErrorsRequest }
     * 
     * @return
     *     the new instance of {@link GetErrorsRequest }
     */
    public GetErrorsRequest createGetErrorsRequest() {
        return new GetErrorsRequest();
    }

    /**
     * Create an instance of {@link ErrorResultImplArray }
     * 
     * @return
     *     the new instance of {@link ErrorResultImplArray }
     */
    public ErrorResultImplArray createErrorResultImplArray() {
        return new ErrorResultImplArray();
    }

    /**
     * Create an instance of {@link PayloadType }
     * 
     * @return
     *     the new instance of {@link PayloadType }
     */
    public PayloadType createPayloadType() {
        return new PayloadType();
    }

    /**
     * Create an instance of {@link ErrorResultImpl }
     * 
     * @return
     *     the new instance of {@link ErrorResultImpl }
     */
    public ErrorResultImpl createErrorResultImpl() {
        return new ErrorResultImpl();
    }

    /**
     * Create an instance of {@link PayloadURLType }
     * 
     * @return
     *     the new instance of {@link PayloadURLType }
     */
    public PayloadURLType createPayloadURLType() {
        return new PayloadURLType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Object }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link Object }{@code >}
     */
    @XmlElementDecl(namespace = "http://org.ecodex.backend/1_1/", name = "listPendingMessagesRequest")
    public JAXBElement<Object> createListPendingMessagesRequest(Object value) {
        return new JAXBElement<>(_ListPendingMessagesRequest_QNAME, Object.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetStatusRequest }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link GetStatusRequest }{@code >}
     */
    @XmlElementDecl(namespace = "http://org.ecodex.backend/1_1/", name = "getStatusRequest")
    public JAXBElement<GetStatusRequest> createGetStatusRequest(GetStatusRequest value) {
        return new JAXBElement<>(_GetStatusRequest_QNAME, GetStatusRequest.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link StatusRequest }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link StatusRequest }{@code >}
     */
    @XmlElementDecl(namespace = "http://org.ecodex.backend/1_1/", name = "statusRequest")
    public JAXBElement<StatusRequest> createStatusRequest(StatusRequest value) {
        return new JAXBElement<>(_StatusRequest_QNAME, StatusRequest.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link MessageStatus }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link MessageStatus }{@code >}
     */
    @XmlElementDecl(namespace = "http://org.ecodex.backend/1_1/", name = "getStatusResponse")
    public JAXBElement<MessageStatus> createGetStatusResponse(MessageStatus value) {
        return new JAXBElement<>(_GetStatusResponse_QNAME, MessageStatus.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetErrorsRequest }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link GetErrorsRequest }{@code >}
     */
    @XmlElementDecl(namespace = "http://org.ecodex.backend/1_1/", name = "getErrorsRequest")
    public JAXBElement<GetErrorsRequest> createGetErrorsRequest(GetErrorsRequest value) {
        return new JAXBElement<>(_GetErrorsRequest_QNAME, GetErrorsRequest.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ErrorResultImplArray }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link ErrorResultImplArray }{@code >}
     */
    @XmlElementDecl(namespace = "http://org.ecodex.backend/1_1/", name = "getMessageErrorsResponse")
    public JAXBElement<ErrorResultImplArray> createGetMessageErrorsResponse(ErrorResultImplArray value) {
        return new JAXBElement<>(_GetMessageErrorsResponse_QNAME, ErrorResultImplArray.class, null, value);
    }

}
