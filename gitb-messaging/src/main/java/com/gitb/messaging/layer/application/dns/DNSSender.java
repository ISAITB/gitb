package com.gitb.messaging.layer.application.dns;

import com.gitb.core.Configuration;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.Message;
import com.gitb.messaging.layer.transport.udp.UDPSender;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.types.StringType;
import com.gitb.utils.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Created by serbay.
 */
public class DNSSender extends UDPSender {

	private static final Logger logger = LoggerFactory.getLogger(DNSSender.class);

	protected DNSSender(SessionContext session, TransactionContext transaction) {
		super(session, transaction);
	}

	@Override
	public Message send(List<Configuration> configurations, Message message) throws Exception {
		StringType address = (StringType) message.getFragments().get(DNSMessagingHandler.DNS_ADDRESS_FIELD_NAME);

		DatagramSocket socket = transaction.getParameter(DatagramSocket.class);
		DatagramPacket incomingPacket = transaction.getParameter(DatagramPacket.class);
		DNSReceiver.DNSRequestMetadata metadata = transaction.getParameter(DNSReceiver.DNSRequestMetadata.class);

		DNSRecord dnsRecord = new DNSRecord(metadata.getDomain().getValue(), (String) address.getValue());

		transaction.setParameter(DNSRecord.class, dnsRecord);

		org.xbill.DNS.Message response = generateResponse(metadata.getQuery(), configurations);

		StringType domain = (StringType) DataTypeFactory.getInstance().create(DataType.STRING_DATA_TYPE);
		domain.setValue(dnsRecord.getDomain());

		message.getFragments()
			.put(DNSMessagingHandler.DNS_DOMAIN_CONFIG_NAME, domain);
		message.getFragments()
			.put(DNSMessagingHandler.DNS_ADDRESS_FIELD_NAME, address);

        //check the query with the actual domain
        String query = metadata.getQuery().getQuestion().getName().toString();
        if(query.endsWith(".")) {
            query = query.substring(0, query.length()-1);
        }

        String value = domain.getValue().toString();
        if(value.endsWith(".")) {
            value = value.substring(0, value.length()-1);
        }
        if(!query.contentEquals(value)){
            transaction.addNonCriticalError(new Exception(
                    "Wrong DNS query \"" + metadata.getQuery().getQuestion().getName() + "\" has been sent to DNS Server. " +
                    "It should have been \"" + domain.getValue() + "\""
            ));
        }

		if(response != null) {
			byte[] rawOutput = response.toWire();

			DatagramPacket output = new DatagramPacket(rawOutput, rawOutput.length, incomingPacket.getAddress(), incomingPacket.getPort());

			socket.send(output);

		} else {
			throw new GITBEngineInternalError("Could not generate the DNS query response for packet: ["+incomingPacket+"].");
		}

        return message;
	}

	private org.xbill.DNS.Message generateResponse(org.xbill.DNS.Message query, List<Configuration> configurations) throws TextParseException, UnknownHostException {
		if(query.getHeader().getOpcode() != Opcode.QUERY) {
			return ErrorMessages.makeErrorMessage(query, Rcode.NOTIMPL);
		}

		Record queryRecord = query.getQuestion();
		Name name = queryRecord.getName();
		int type = queryRecord.getType();
		int dclass = queryRecord.getDClass();

		logger.debug("Generating response for the domain name: ["+name+"]");

		if(query.getTSIG() != null) {
			logger.debug("TSIG is not null. Returning error response for the query ["+name+"]");
			return ErrorMessages.makeErrorMessage(query, Rcode.NOTIMPL);
		}

		org.xbill.DNS.Message response = new org.xbill.DNS.Message();
		response.getHeader().setID(query.getHeader().getID());
		response.getHeader().setFlag(Flags.QR);
		response.addRecord(queryRecord, Section.QUESTION);

		if(type == Type.AXFR) {
			logger.debug("Record type is AXFR. Returning error response for the query ["+name+"]");
			return ErrorMessages.makeErrorMessage(query, Rcode.REFUSED);
		}

		if(!Type.isRR(type) && type != Type.ANY) {
			logger.debug("Record type is not RR or ANY. Returning error response for the query ["+name+"]");
			return ErrorMessages.makeErrorMessage(query, Rcode.NOTIMPL);
		}

		if(type == Type.SIG) {
			logger.debug("Record type is SIG. Returning error response for the query ["+name+"]");
			return ErrorMessages.makeErrorMessage(query, Rcode.NOTIMPL);
		}

		response.getHeader().setFlag(Flags.AA);

		RRset rrsetResponse = findMatchingDNSRecords(name);

		logger.debug("Found rrset for the query ["+name+"]: ["+rrsetResponse+"]");

		if(rrsetResponse != null && rrsetResponse.size() > 0) {
			Record record = rrsetResponse.first();

			if(!response.findRecord(record)) {
				logger.debug("Found record for the query ["+name+"]: ["+record+"]");
				response.addRecord(record, Section.ANSWER);
			}
		}

		return response;
	}

	private RRset findMatchingDNSRecords(Name name) throws TextParseException, UnknownHostException {
		DNSRecord dnsRecord = transaction.getParameter(DNSRecord.class);

		Name registeredName = Name.fromString(dnsRecord.getDomain());
		InetAddress address = null;

        String registeredDomain = dnsRecord.getDomain().endsWith(".") ? dnsRecord.getDomain().substring(0, dnsRecord.getDomain().length()-1) : dnsRecord.getDomain();
        String queryName = name.toString().endsWith(".") ? name.toString().substring(0, name.toString().length()-1) : name.toString();

		if(registeredDomain.equals(queryName)) {
			address = InetAddress.getByName(dnsRecord.getAddress());
			logger.debug("Found matching record for ["+name+"]: ["+address+"]");
		} else {
			logger.debug("Cannot find a registered DNS record for ["+name+"] asking configured DNS server");
			try {
				address = InetAddress.getByName(name.toString());

			} catch (UnknownHostException e) {
				logger.debug("Configured DNS server could not find the domain ["+name+"]");
			}
		}

		if(address != null) {
			Record record = new ARecord(name, DClass.IN, TTL.MAX_VALUE, address);
			return new RRset(record);
		} else {
			return null;
		}
	}
}
