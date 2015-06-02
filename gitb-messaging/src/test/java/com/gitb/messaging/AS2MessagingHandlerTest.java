package com.gitb.messaging;

import com.gitb.core.ActorConfiguration;
import com.gitb.messaging.layer.application.as2.AS2MessagingHandler;
import com.gitb.messaging.model.InitiateResponse;
import com.gitb.types.BinaryType;
import com.gitb.utils.ConfigurationUtils;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.xml.bind.DatatypeConverter;
import javax.xml.soap.SOAPException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static org.junit.Assert.assertNotNull;

/**
 * Created by senan on 05.11.2014.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AS2MessagingHandlerTest extends MessagingHandlerTest {
    protected static Message message;

    private static final String AS2_NAME_CONFIG_NAME   = "as2.name";
    private static final String PUBLIC_KEY_CONFIG_NAME = "public.key";
    private static final String PUBLIC_KEY_LOCATION    = "/keystore/gitb-engine.pem";

    protected final String MESSAGE_STR = "" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<StandardBusinessDocument xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""+
            "                          xmlns=\"http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader\">"+
            "    <StandardBusinessDocumentHeader>"+
            "        <HeaderVersion>1.0</HeaderVersion>"+
            "        <Sender>"+
            "            <Identifier Authority=\"iso6523-actorid-upis\">0088:7315458756324</Identifier>"+
            "        </Sender>"+
            "        <Receiver>"+
            "            <Identifier Authority=\"iso6523-actorid-upis\">0088:4562458856624</Identifier>"+
            "        </Receiver>"+"        <DocumentIdentification>"+
            "            <Standard>urn:oasis:names:specification:ubl:schema:xsd:Invoice-2</Standard>"+
            "            <TypeVersion>2.1</TypeVersion>"+
            "            <InstanceIdentifier>123123</InstanceIdentifier>"+
            "            <Type>Invoice</Type>"+
            "            <CreationDateAndTime>2013-02-19T05:10:10Z</CreationDateAndTime>"+
            "        </DocumentIdentification>"+
            "        <BusinessScope>"+
            "            <Scope>"+
            "                <Type>DOCUMENTID</Type>"+
            "                <InstanceIdentifier>urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:www.cenbii.eu:transaction:biitrns010:ver2.0:extended:urn:www.peppol.eu:bis:peppol4a:ver2.0::2.1</InstanceIdentifier>"+
            "            </Scope>"+
            "            <Scope>"+
            "                <Type>PROCESSID</Type>"+
            "                <InstanceIdentifier>urn:www.cenbii.eu:profile:bii04:ver1.0</InstanceIdentifier>"+
            "            </Scope>"+"        </BusinessScope>"+
            "    </StandardBusinessDocumentHeader>"+
            "    <Invoice"+
            "            xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\""+
            "            xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\""+
            "            xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\">"+
            "        <cbc:UBLVersionID>2.1</cbc:UBLVersionID>"+
            "        <cbc:CustomizationID schemeID=\"PEPPOL\">"+
            "            urn:www.cenbii.eu:transaction:biitrns010:ver2.0:extended:urn:www.peppol.eu:bis:peppol4a:ver2.0"+
            "        </cbc:CustomizationID>"+
            "        <cbc:ProfileID>urn:www.cenbii.eu:profile:bii04:ver1.0</cbc:ProfileID>"+
            "        <cbc:ID>008660-AB</cbc:ID>"+
            "        <cbc:IssueDate>2011-05-10</cbc:IssueDate>"+
            "        <cbc:InvoiceTypeCode>380</cbc:InvoiceTypeCode>"+
            "        <!-- reduced instance file -->"+
            "    </Invoice>"+
            "</StandardBusinessDocument>";


    @BeforeClass
    public static void init() {
        receiverHandler = new AS2MessagingHandler();
        senderHandler = new AS2MessagingHandler();
    }

    @Test
    public void handlersShouldNotBeNull() throws IOException, SOAPException {
        assertNotNull(receiverHandler);
        assertNotNull(senderHandler);

        assertNotNull(receiverHandler);
        assertNotNull(senderHandler);

        BinaryType msg = new BinaryType();
        msg.setValue(MESSAGE_STR.getBytes());

        message = new Message();
        message
                .getFragments()
                .put(AS2MessagingHandler.AS2_MESSAGE_FIELD_NAME, msg);
    }

    @Test
    public void test_1_InitAndEndSession() {
        senderActorConfigurations = new ArrayList<>();

        ActorConfiguration receiverActorConfiguration = new ActorConfiguration();
        receiverActorConfiguration.setActor("test");
        receiverActorConfiguration.getConfig().add(ConfigurationUtils.constructConfiguration(ServerUtils.IP_ADDRESS_CONFIG_NAME, "127.0.0.1"));
        receiverActorConfiguration.getConfig().add(ConfigurationUtils.constructConfiguration(ServerUtils.PORT_CONFIG_NAME, "7000"));
        receiverActorConfiguration.getConfig().add(ConfigurationUtils.constructConfiguration(PUBLIC_KEY_CONFIG_NAME, getPublicKey()));
        receiverActorConfiguration.getConfig().add(ConfigurationUtils.constructConfiguration(AS2_NAME_CONFIG_NAME, "gitb-engine"));
        senderActorConfigurations.add(receiverActorConfiguration);

        ActorConfiguration receiverActorConfiguration2 = new ActorConfiguration();
        receiverActorConfiguration2.setActor("test2");
        receiverActorConfiguration2.getConfig().add(ConfigurationUtils.constructConfiguration(ServerUtils.IP_ADDRESS_CONFIG_NAME, "127.0.0.1"));
        receiverActorConfiguration2.getConfig().add(ConfigurationUtils.constructConfiguration(ServerUtils.PORT_CONFIG_NAME, "7001"));
        receiverActorConfiguration2.getConfig().add(ConfigurationUtils.constructConfiguration(PUBLIC_KEY_CONFIG_NAME, getPublicKey()));
        receiverActorConfiguration2.getConfig().add(ConfigurationUtils.constructConfiguration(AS2_NAME_CONFIG_NAME, "gitb-engine"));
        senderActorConfigurations.add(receiverActorConfiguration2);

        InitiateResponse response = receiverHandler.initiate(senderActorConfigurations);
        assertNotNull(response);

        receiverSessionId = response.getSessionId();
        assertNotNull(receiverSessionId);

        receiverActorConfigurations = response.getActorConfigurations();
        assertNotNull(receiverActorConfigurations);

        receiverActorConfigurations.get(0).getConfig().add(ConfigurationUtils.constructConfiguration(PUBLIC_KEY_CONFIG_NAME, getPublicKey()));
        receiverActorConfigurations.get(0).getConfig().add(ConfigurationUtils.constructConfiguration(AS2_NAME_CONFIG_NAME, "gitb-engine"));

        InitiateResponse response2 = senderHandler.initiate(receiverActorConfigurations);

        senderSessionId = response2.getSessionId();
        assertNotNull(senderSessionId);
    }

    @Override
    protected void messageReceived(Message message) {
        assertNotNull(message);
    }

    @Override
    protected Message constructMessage() {
        return message;
    }

    private String getPublicKey() {
        InputStream stream = this.getClass().getResourceAsStream(PUBLIC_KEY_LOCATION);
        String publicKey = null;
        try {
            publicKey = IOUtils.toString(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //convert to data uri
        publicKey = "data:application/pkix-cert;base64," + new String(DatatypeConverter.printBase64Binary(publicKey.getBytes()));

        return publicKey;
    }

}
