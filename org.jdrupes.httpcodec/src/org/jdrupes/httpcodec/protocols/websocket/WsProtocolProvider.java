/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016  Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package org.jdrupes.httpcodec.protocols.websocket;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

import org.jdrupes.httpcodec.MessageHeader;
import org.jdrupes.httpcodec.RequestDecoder;
import org.jdrupes.httpcodec.RequestEncoder;
import org.jdrupes.httpcodec.ResponseDecoder;
import org.jdrupes.httpcodec.ResponseEncoder;
import org.jdrupes.httpcodec.fields.HttpIntField;
import org.jdrupes.httpcodec.fields.HttpStringField;
import org.jdrupes.httpcodec.fields.HttpUnquotedStringField;
import org.jdrupes.httpcodec.plugin.ProtocolProvider;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;

/**
 * @author Michael N. Lipp
 *
 */
public class WsProtocolProvider extends ProtocolProvider {

	/* (non-Javadoc)
	 * @see ProtocolProvider#supportsProtocol(java.lang.String)
	 */
	@Override
	public boolean supportsProtocol(String protocol) {
		return protocol.equalsIgnoreCase("websocket");
	}

	/* (non-Javadoc)
	 * @see ProtocolProvider#augmentInitialResponse
	 */
	@Override
	public void augmentInitialResponse(HttpResponse response) {
		Optional<String> wsKey = response.getRequest()
			.flatMap(r -> r.getStringField("Sec-WebSocket-Key"))
			.map(HttpStringField::getValue);
		if (!wsKey.isPresent()) {
			response.setStatus(HttpStatus.BAD_REQUEST)
				.setMessageHasBody(false).clearHeaders();
			return;
		}
		// RFC 6455 4.1
		if(response.getRequest().flatMap
				(r -> r.getField(HttpIntField.class, "Sec-WebSocket-Version"))
				.map(HttpIntField::asInt).orElse(-1) != 13) {
			response.setStatus(HttpStatus.BAD_REQUEST)
				.setMessageHasBody(false).clearHeaders();
			// RFC 6455 4.4
			response.setField(new HttpIntField("Sec-WebSocket-Version", 13));
			return;
			
		}
		String s = wsKey.get() + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
		try {
			MessageDigest crypt = MessageDigest.getInstance("SHA-1");
			byte[] sha1 = crypt.digest(s.getBytes("ascii"));
			String accept = Base64.getEncoder().encodeToString(sha1);
			response.setField(new HttpUnquotedStringField
					("Sec-WebSocket-Accept", accept));
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR)
				.setMessageHasBody(false).clearHeaders();
			return;
		}
 	}

	/* (non-Javadoc)
	 * @see ProtocolProvider#createRequestEncoder()
	 */
	@Override
	public RequestEncoder<? extends MessageHeader> 
		createRequestEncoder(String protocol) {
		return null;
	}

	/* (non-Javadoc)
	 * @see ProtocolProvider#createRequestDecoder()
	 */
	@Override
	public RequestDecoder<? extends MessageHeader, 
			? extends MessageHeader> createRequestDecoder(String protocol) {
		return new WsRequestDecoder();
	}

	/* (non-Javadoc)
	 * @see ProtocolProvider#createResponseEncoder()
	 */
	@Override
	public ResponseEncoder<? extends MessageHeader> 
		createResponseEncoder(String protocol) {
		return new WsResponseEncoder();
	}

	/* (non-Javadoc)
	 * @see ProtocolProvider#createResponseDecoder()
	 */
	@Override
	public ResponseDecoder<? extends MessageHeader, 
			? extends MessageHeader> createResponseDecoder(String protocol) {
		return null;
	}

	
	
}
