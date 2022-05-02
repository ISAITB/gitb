package com.gitb.engine.messaging.handlers.layer.application.dns;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.engine.messaging.handlers.layer.transport.udp.UDPMessagingHandler;
import com.gitb.engine.messaging.handlers.layer.transport.udp.UDPReceiver;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.types.BinaryType;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.types.StringType;
import com.gitb.utils.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by serbay.
 */
public class DNSReceiver extends UDPReceiver {

	private static final Logger logger = LoggerFactory.getLogger(DNSReceiver.class);

	protected DNSReceiver(SessionContext session, TransactionContext transaction) {
		super(session, transaction);
	}

	@Override
	public Message receive(List<Configuration> configurations, Message inputs) throws Exception {
		Message udpMessage = super.receive(configurations, inputs);

		logger.debug(addMarker(), "Received a message.");

        Configuration domainConfiguration = ConfigurationUtils.getConfiguration(configurations, DNSMessagingHandler.DNS_DOMAIN_CONFIG_NAME);
//      Configuration addressConfiguration = ConfigurationUtils.getConfiguration(configurations, DNSMessagingHandler.DNS_ADDRESS_FIELD_NAME);

//      DNSRecord dnsRecord = new DNSRecord(domainConfiguration.getValue(), addressConfiguration.getValue());

		BinaryType binaryData = (BinaryType) udpMessage.getFragments().get(UDPMessagingHandler.CONTENT_MESSAGE_FIELD_NAME);

		byte[] data = (byte[]) binaryData.getValue();

		org.xbill.DNS.Message query = new org.xbill.DNS.Message(data);

//		transaction.setParameter(org.xbill.DNS.Message.class, query);
//      transaction.setParameter(DNSRecord.class, dnsRecord);

		DNSRequestMetadata metadata = new DNSRequestMetadata(domainConfiguration, query);

		transaction.setParameter(DNSRequestMetadata.class, metadata);

		DataTypeFactory factory = DataTypeFactory.getInstance();

		StringType domain = (StringType) factory.create(DataType.STRING_DATA_TYPE);
		domain.setValue(query.getQuestion().getName().toString());

		Message message = new Message();
		message.getFragments()
			.put(DNSMessagingHandler.DNS_DOMAIN_CONFIG_NAME, domain);


		return message;
	}

	public static class DNSRequestMetadata {
		private final Configuration domain;
		private final org.xbill.DNS.Message query;


		public DNSRequestMetadata(Configuration domain, org.xbill.DNS.Message query) {
			this.domain = domain;
			this.query = query;
		}

		public Configuration getDomain() {
			return domain;
		}

		public org.xbill.DNS.Message getQuery() {
			return query;
		}
	}
}
