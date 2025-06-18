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

package com.gitb.engine.messaging.handlers.layer.transport.tcp;

import com.gitb.core.MessagingModule;
import com.gitb.engine.messaging.MessagingHandler;
import com.gitb.engine.messaging.handlers.layer.AbstractMessagingHandler;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.engine.messaging.handlers.model.tcp.ITransactionListener;
import com.gitb.engine.messaging.handlers.model.tcp.ITransactionReceiver;
import com.gitb.engine.messaging.handlers.model.tcp.ITransactionSender;
import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;

import java.io.*;

/**
 * Created by serbay on 9/24/14.
 */
@MessagingHandler(name="TCPMessaging")
public class TCPMessagingHandler extends AbstractMessagingHandler {

	public static final String CONTENT_MESSAGE_FIELD_NAME = "content";
	public static final String MODULE_DEFINITION_XML = "/messaging/tcp-messaging-definition.xml";

	public static final int TCP_STOP_CHARACTER = -1;

	private static final MessagingModule module = MessagingHandlerUtils.readModuleDefinition(MODULE_DEFINITION_XML);

	@Override
	public MessagingModule getModuleDefinition() {
		return module;
	}

	@Override
	public ITransactionReceiver getReceiver(SessionContext sessionContext, TransactionContext transactionContext) throws IOException {
		return new TCPReceiver(sessionContext, transactionContext);
	}

	@Override
	public ITransactionSender getSender(SessionContext sessionContext, TransactionContext transactionContext) {
		return new TCPSender(sessionContext, transactionContext);
	}

    @Override
    public ITransactionListener getListener(SessionContext sessionContext, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
        return new TCPListener(sessionContext, receiverTransactionContext, senderTransactionContext);
    }

    public static void sendBytes(OutputStream outputStream, byte[] bytes) throws IOException {
        outputStream.write(bytes);
        outputStream.flush();
    }

    public static void readBytes(InputStream inputStream, byte[] buffer) throws IOException {
        inputStream.read(buffer); //read to buffer
    }

    public static byte[] readBytes(InputStream inputStream, int stopCharacter) throws IOException{
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int c = 0;

        while(true) {
            c = inputStream.read();
            if(c == stopCharacter) {
                break;
            }
            byteArrayOutputStream.write(c);
        }

        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] readBytes(InputStream inputStream) throws IOException {
        return readBytes(inputStream, TCP_STOP_CHARACTER);
    }

    public static String readLines(InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String input = "";
        String read = "";
        while ((read = reader.readLine()) != null) {
            input = input + read;
        }
        return input;
    }
}
