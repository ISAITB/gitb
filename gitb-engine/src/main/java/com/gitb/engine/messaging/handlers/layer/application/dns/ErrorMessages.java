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

package com.gitb.engine.messaging.handlers.layer.application.dns;

import java.io.*;

import org.xbill.DNS.*;

/**
 * **********************************************************************
 * Routines for creating different kinds of DNS error responses.
 * ************************************************************************
 * This code was adapted from Brian Wellington's jnamed code.
 *
 * @see org.xbill.DNS.Message
 */

public class ErrorMessages {

	/**
	 * ******************************************************************
	 * Create a format error message (FORMERR).
	 * ********************************************************************
	 *
	 * @param in The malformed packet.
	 * @return A DNS error message.
	 */
	public static Message makeFormatErrorMessage(byte[] in) {
		Header header;
		try {
			header = new Header(in);
		} catch (IOException e) {
			header = new Header(0);
		}
		Message response = new Message();
		response.setHeader(header);
		for (int i = 0; i < 4; i++)
			response.removeAllRecords(i);
		header.setRcode(Rcode.FORMERR);
		return response;
	}

	/**
	 * ******************************************************************
	 * Create an arbitrary DNS error message.
	 * ********************************************************************
	 *
	 * @param query The query sent by the user.
	 * @param rcode The response code to use for this error.
	 * @return A DNS error message.
	 * @see org.xbill.DNS.Rcode
	 */
	public static Message makeErrorMessage(Message query, int rcode) {
		Header header = query.getHeader();
		Message response = new Message();
		response.setHeader(header);
		for (int i = 0; i < 4; i++)
			response.removeAllRecords(i);
		header.setRcode(rcode);
		return response;
	}
}
