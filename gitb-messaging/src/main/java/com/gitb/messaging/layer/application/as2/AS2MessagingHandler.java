package com.gitb.messaging.layer.application.as2;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.core.MessagingModule;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.KeyStoreFactory;
import com.gitb.messaging.ServerUtils;
import com.gitb.messaging.layer.AbstractMessagingHandler;
import com.gitb.messaging.layer.application.http.HttpMessagingHandler;
import com.gitb.messaging.model.tcp.ITransactionReceiver;
import com.gitb.messaging.model.tcp.ITransactionSender;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.messaging.utils.MessagingHandlerUtils;
import com.gitb.types.MapType;
import com.gitb.utils.ConfigurationUtils;
import com.gitb.utils.EncodingUtils;
import com.helger.as2lib.crypto.ICryptoHelper;
import com.helger.as2lib.disposition.DispositionOptions;
import com.helger.as2lib.util.AS2Util;
import com.helger.as2lib.util.CAS2Header;
import org.apache.commons.codec.binary.Base64;
import org.kohsuke.MetaInfServices;

import javax.mail.internet.MimeBodyPart;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Created by senan on 11.11.2014.
 */
@MetaInfServices(IMessagingHandler.class)
public class AS2MessagingHandler extends AbstractMessagingHandler{

    public static final String HTTP_HEADERS_FIELD_NAME = HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME;
    public static final String HTTP_METHOD = "POST";

    public static final String AS2_PUBLIC_KEY_CONFIG_NAME = "public.key";
    public static final String AS2_MESSAGE_FIELD_NAME  = "as2_message";
    public static final String AS2_NAME_CONFIG_NAME = "as2.name";

    public static final String AS2_FROM    = "gitb-engine";
    public static final String AS2_SUBJECT = "AS2 Test Message";

    public static final String SIGNING_ALGORITHM    = "sha1";
    public static final String ENCRYPTION_ALGORITHM = "3des";
    public static final String DEFAULT_MDN_OPTIONS = "signed-receipt-protocol=required, " +
            "pkcs7-signature; signed-receipt-micalg=required, sha1";

    public static final String HEADER_RECEIVED_CONTENT_MIC = "Received-Content-MIC";
    public static final String HEADER_DISPOSITION = "Disposition";
    public static final String HEADER_ORIGINAL_MESSAGE_ID = "Original-Message-ID";
    public static final String HEADER_FINAL_RECIPIENT = "Final-Recipient";
    public static final String HEADER_ORIGINAL_RECIPIENT = "Original-Recipient";
    public static final String HEADER_REPORTING_UA = "Reporting-UA";

    private static final String MODULE_DEFINITION_XML = "/as2-messaging-definition.xml";

    private static MessagingModule module = MessagingHandlerUtils.readModuleDefinition(MODULE_DEFINITION_XML);

    public MessagingModule getModuleDefinition() {
        return module;
    }

    @Override
    public ITransactionReceiver getReceiver(SessionContext sessionContext, TransactionContext transactionContext) throws IOException {
        return new AS2Receiver(sessionContext, transactionContext);
    }

    @Override
    public ITransactionSender getSender(SessionContext sessionContext, TransactionContext transactionContext) {
        return new AS2Sender(sessionContext, transactionContext);
    }

    public static MimeBodyPart sign(MimeBodyPart mimeBody, X509Certificate senderCerificate, PrivateKey privateKey) throws Exception {
        ICryptoHelper cryptoHelper = AS2Util.getCryptoHelper();
        MimeBodyPart signed = cryptoHelper.sign(mimeBody, senderCerificate, privateKey, SIGNING_ALGORITHM);
        return signed;
    }

    public static MimeBodyPart encrypt(MimeBodyPart mimeBody, X509Certificate receiverCertificate) throws Exception {
        ICryptoHelper cryptoHelper = AS2Util.getCryptoHelper();
        MimeBodyPart encrypted = cryptoHelper.encrypt(mimeBody, receiverCertificate, ENCRYPTION_ALGORITHM);
        return encrypted;
    }

    public static MimeBodyPart decrypt(MimeBodyPart mimeBody, X509Certificate receiverCertificate, PrivateKey privateKey) throws Exception {
        ICryptoHelper cryptoHelper = AS2Util.getCryptoHelper();
        if(cryptoHelper.isEncrypted(mimeBody)) {
            return cryptoHelper.decrypt(mimeBody, receiverCertificate, privateKey);
        }
        return mimeBody;
    }

    public static MimeBodyPart verify(MimeBodyPart mimeBody, X509Certificate senderCertificate) throws Exception {
        ICryptoHelper cryptoHelper = AS2Util.getCryptoHelper();
        if(cryptoHelper.isSigned(mimeBody)) {
            return cryptoHelper.verify(mimeBody, senderCertificate);
        }
        return mimeBody;
    }

    public static X509Certificate getSUTCertificate(TransactionContext transaction) throws UnsupportedEncodingException {
        ActorConfiguration actorConfiguration = transaction.getWith();
        Configuration publicKey = ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), AS2_PUBLIC_KEY_CONFIG_NAME);
        String base64   = EncodingUtils.extractBase64FromDataURL(publicKey.getValue());
        byte [] decoded = Base64.decodeBase64(base64);
        if(decoded == null){
            throw new GITBEngineInternalError("Public Certificate provided has a wrong format.");
        }
        X509Certificate cert = KeyStoreFactory.getInstance().generateCertificate(decoded);
        if(cert == null) {
            throw new GITBEngineInternalError("Public Certificate provided has a wrong format.");
        }
        return cert;
    }

    public static String calculateMIC(MimeBodyPart mimeBody, MapType headers, boolean includeHeaders) throws Exception {
        ICryptoHelper cryptoHelper = AS2Util.getCryptoHelper();
        String sDispositionOptions = ServerUtils.getHeader(headers, CAS2Header.HEADER_DISPOSITION_NOTIFICATION_OPTIONS);
        DispositionOptions dispositionOptions = DispositionOptions.createFromString (sDispositionOptions);
        return cryptoHelper.calculateMIC(mimeBody, dispositionOptions.getMICAlg(), includeHeaders);
    }

    public static String generateMessageId(String as2To) {
        Random rand = new Random(); //creates a 4-digit random number
        int random = rand.nextInt(8999) + 1000;

        SimpleDateFormat formatter  = new SimpleDateFormat("ddMMyyyyHHmmssZ");

        return "<m-" + AS2_FROM +"-" + formatter.format(new Date()) + "-" + random + "@" +
                AS2_FROM + "_" + as2To + ">";
    }
}
