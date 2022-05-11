package com.gitb.engine.messaging.handlers.layer.application.smp;

import com.gitb.core.Configuration;
import com.gitb.engine.messaging.handlers.KeyStoreFactory;
import com.gitb.messaging.Message;
import com.gitb.engine.messaging.handlers.layer.application.http.HttpMessagingHandler;
import com.gitb.engine.messaging.handlers.layer.application.http.HttpSender;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.types.*;
import com.gitb.utils.ConfigurationUtils;
import com.gitb.utils.XMLUtils;
import org.w3c.dom.Document;

import java.util.List;

/**
 * Created by senan on 06.01.2015.
 */
public class SMPSender extends HttpSender {

    public static final String HTTP_METHOD = "POST";

    public static final String HTTP_CONTENT_TYPE_HEADER = "Content-Type";
    public static final String HTTP_CONTENT_TYPE_VALUE = "text/xml";

    public SMPSender(SessionContext session, TransactionContext transaction) {
        super(session, transaction);
    }

    @Override
    public Message send(List<Configuration> configurations, Message input) throws Exception {
        ObjectType smpMetadata = (ObjectType) input.getFragments().get(SMPMessagingHandler.SMP_METADATA_FIELD_NAME);

        Document document = (Document) smpMetadata.getValue();
        document = XMLUtils.sign(document, KeyStoreFactory.getInstance().getCertificate(), KeyStoreFactory.getInstance().getPrivateKey());

        //define message parameters
        BinaryType body = new BinaryType();
        body.setValue(XMLUtils.convertDocumentToByteArray(document));

        //define headers
        MapType headers = new MapType();
        headers.addItem(HTTP_CONTENT_TYPE_HEADER, new StringType(HTTP_CONTENT_TYPE_VALUE));

        Message message = new Message();
        message.getFragments().put(HttpMessagingHandler.HTTP_BODY_FIELD_NAME, body);
        message.getFragments().put(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME, headers);

        configurations.add(ConfigurationUtils.constructConfiguration(HttpMessagingHandler.HTTP_METHOD_CONFIG_NAME, HTTP_METHOD));

        super.send(configurations, message);

        return message;
    }

}
