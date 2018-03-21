package com.gitb.messaging.layer.application.as2.peppol;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.core.MessagingModule;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.ServerUtils;
import com.gitb.messaging.layer.application.http.HttpMessagingHandler;
import com.gitb.messaging.layer.application.https.HttpsMessagingHandler;
import com.gitb.messaging.model.InitiateResponse;
import com.gitb.messaging.model.tcp.ITransactionListener;
import com.gitb.messaging.model.tcp.ITransactionReceiver;
import com.gitb.messaging.model.tcp.ITransactionSender;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.messaging.utils.MessagingHandlerUtils;
import com.gitb.utils.ConfigurationUtils;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.kohsuke.MetaInfServices;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by senan on 11.11.2014.
 */
@MetaInfServices(IMessagingHandler.class)
public class PeppolAS2MessagingHandler extends HttpsMessagingHandler{
    public static final String HTTP_HEADERS_FIELD_NAME            = HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME;
    public static final String BUSINESS_HEADER_FIELD_NAME         = "business_header";
    public static final String BUSINESS_MESSAGE_FIELD_NAME        = "business_message";
    public static final String BUSINESS_DOCUMENT_FIELD_NAME       = "business_document";
    public static final String AS2_MDN_FIELD_NAME                 = "as2_mdn";
    public static final String AS2_ADDRESS_CONFIG_NAME            = "as2.address";

    public static final String DOCUMENT_IDENTIFIER_CONFIG_NAME    = "document.identifier";
    public static final String PROCESS_IDENTIFIER_CONFIG_NAME     = "process.identifier";
    public static final String PARTICIPANT_IDENTIFIER_CONFIG_NAME = "participant.identifier";

    public static final String DOCUMENT_IDENTIFIER_PATTERN        = "(.+?)::(.+?)##(.+?)::(.+?)";
    public static final String PARTICIPANT_IDENTIFIER_PATTERN     = "(\\d{4}):(\\w+)";

    public static final int MAX_DOCUMENT_TYPE_IDENTIFIER_VALUE_LENGTH = 500;
    public static final int MAX_PROCESS_IDENTIFIER_VALUE_LENGTH       = 200;
    public static final int MAX_PARTICIPANT_IDENTIFIER_VALUE_LENGTH   = 50;

    private static final Charset CHARSET_ISO88591 = Charset.forName("ISO-8859-1");
    private static final Charset CHARSET_ASCII    = Charset.forName("US-ASCII");

    private static final String MODULE_DEFINITION_XML = "/peppol-as2-messaging-definition.xml";

    private static MessagingModule module = MessagingHandlerUtils.readModuleDefinition(MODULE_DEFINITION_XML);

    public MessagingModule getModuleDefinition() {
        return module;
    }

    @Override
    public ITransactionReceiver getReceiver(SessionContext sessionContext, TransactionContext transactionContext) throws IOException {
        return new PeppolAS2Receiver(sessionContext, transactionContext);
    }

    @Override
    public ITransactionSender getSender(SessionContext sessionContext, TransactionContext transactionContext) {
        return new PeppolAS2Sender(sessionContext, transactionContext);
    }

    @Override
    public ITransactionListener getListener(SessionContext sessionContext, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
        return new PeppolAS2Listener(sessionContext, receiverTransactionContext, senderTransactionContext);
    }

    protected boolean validateMetadata() {
        return true;
    }

    @Override
    protected void validateActorConfigurations(List<ActorConfiguration> actorConfigurations) {
        super.validateActorConfigurations(actorConfigurations);
        if (validateMetadata()) {
            for (ActorConfiguration actorConfiguration : actorConfigurations) {
                Configuration participantIdentifierConfig = ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), PARTICIPANT_IDENTIFIER_CONFIG_NAME);
                String participantIdentifier = participantIdentifierConfig.getValue();
                if(!validateParticipantIdentifier(participantIdentifier)) {
                    throw new GITBEngineInternalError("Invalid participant identifier [" + participantIdentifier + "]");
                }
            }
        }
    }

    @Override
    protected void validateReceiveConfigurations(List<Configuration> configurations) {
        super.validateReceiveConfigurations(configurations);
        if (validateMetadata()) {
            //validate document identifier
            Configuration documentIdentifierConfiguration = ConfigurationUtils.getConfiguration(configurations, DOCUMENT_IDENTIFIER_CONFIG_NAME);
            if(documentIdentifierConfiguration != null) { //may be null if receiving mdn, not business message
                String documentIdentifier = documentIdentifierConfiguration.getValue();
                if (!validateDocumentIdentifier(documentIdentifier)) {
                    throw new GITBEngineInternalError("Invalid document identifier [" + documentIdentifier + "]");
                }
            }

            //validate process identifier
            Configuration processIdentifierConfiguration = ConfigurationUtils.getConfiguration(configurations, PROCESS_IDENTIFIER_CONFIG_NAME);
            if(processIdentifierConfiguration != null) { //may be null if receiving mdn, not business message
                String processIdentifier = processIdentifierConfiguration.getValue();
                if(!validateProcessIdentifier(processIdentifier)){
                    throw new GITBEngineInternalError("Invalid process identifier [" + processIdentifier + "]");
                }
            }
        }
    }

    public static boolean validateParticipantIdentifier(String participantIdentifier) {
        int length = participantIdentifier.length();

        if(length == 0 || length > MAX_PARTICIPANT_IDENTIFIER_VALUE_LENGTH)
            return false;

        if(!CHARSET_ASCII.newEncoder().canEncode(participantIdentifier)) {
            return false;
        }

        if(!Pattern.compile(PARTICIPANT_IDENTIFIER_PATTERN).matcher(participantIdentifier).matches())
            return false;

        return true;
    }

    public static boolean validateDocumentIdentifier(String documentIdentifier) {
        int length = documentIdentifier.length();

        if(length == 0 || length > MAX_DOCUMENT_TYPE_IDENTIFIER_VALUE_LENGTH)
            return false;

        if(!CHARSET_ISO88591.newEncoder().canEncode(documentIdentifier)) {
            return false;
        }

        if(!Pattern.compile(DOCUMENT_IDENTIFIER_PATTERN).matcher(documentIdentifier).matches()) {
            return false;
        }

        return true;
    }

    public static boolean validateProcessIdentifier(String processIdentifier) {
        int length = processIdentifier.length();

        if(length == 0 || length > MAX_PROCESS_IDENTIFIER_VALUE_LENGTH)
            return false;

        if(!CHARSET_ISO88591.newEncoder().canEncode(processIdentifier)) {
            return false;
        }

        return true;
    }

    public static String getCN (X509Certificate aCert) throws CertificateEncodingException {
        final X500Name x500name = new JcaX509CertificateHolder(aCert).getSubject ();
        final RDN cn = x500name.getRDNs (BCStyle.CN)[0];
        return IETFUtils.valueToString(cn.getFirst().getValue());
    }
}
