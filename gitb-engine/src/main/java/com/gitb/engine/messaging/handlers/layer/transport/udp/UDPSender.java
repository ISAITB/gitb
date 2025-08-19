/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package com.gitb.engine.messaging.handlers.layer.transport.udp;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.engine.messaging.handlers.ServerUtils;
import com.gitb.engine.messaging.handlers.layer.AbstractDatagramSender;
import com.gitb.engine.messaging.handlers.layer.transport.tcp.TCPMessagingHandler;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.types.BinaryType;
import com.gitb.utils.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.Objects;

/**
 * Created by serbay.
 */
public class UDPSender extends AbstractDatagramSender {

    private static final Logger logger = LoggerFactory.getLogger(UDPSender.class);

    protected UDPSender(SessionContext session, TransactionContext transaction) {
        super(session, transaction);
    }

    @Override
    public Message send(List<Configuration> configurations, Message message) throws Exception {
        super.send(configurations, message);

        ActorConfiguration actorConfiguration = transaction.getWith();

        Configuration ipAddressConfig = ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), ServerUtils.IP_ADDRESS_CONFIG_NAME);
        Configuration portConfig = ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), ServerUtils.PORT_CONFIG_NAME);

        logger.debug(addMarker(), "Sending a datagram message to [{}:{}].", Objects.requireNonNull(ipAddressConfig).getValue(), Objects.requireNonNull(portConfig).getValue());

        DatagramSocket socket = transaction.getParameter(DatagramSocket.class);
        DatagramPacket packet = transaction.getParameter(DatagramPacket.class);

        BinaryType binaryData = (BinaryType) message.getFragments().get(TCPMessagingHandler.CONTENT_MESSAGE_FIELD_NAME);
        byte[] data = binaryData.getValue();

        packet.setAddress(InetAddress.getByName(ipAddressConfig.getValue()));
        packet.setPort(Integer.parseInt(portConfig.getValue()));
        packet.setData(data);
        packet.setLength(data.length);

        socket.send(packet);

        logger.debug(addMarker(), "Sent [{}] bytes to [{}:{}]", packet.getData().length, packet.getAddress(), packet.getPort());

        return message;
    }
}
