/*******************************************************************************
 * This file is part of the JDrupes non-blocking HTTP Codec
 * Copyright (C) 2016  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package org.jdrupes.httpcodec.protocols.websocket;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.Encoder;
import org.jdrupes.httpcodec.ResponseDecoder;
import org.jdrupes.httpcodec.plugin.ProtocolProvider;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.fields.HttpIntField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpStringField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpUnquotedStringField;
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
	public Encoder<?> createRequestEncoder(String protocol) {
		return null;
	}

	/* (non-Javadoc)
	 * @see ProtocolProvider#createRequestDecoder()
	 */
	@Override
	public Decoder<?, ?> createRequestDecoder(String protocol) {
		return new WsDecoder();
	}

	/* (non-Javadoc)
	 * @see ProtocolProvider#createResponseEncoder()
	 */
	@Override
	public Encoder<?> createResponseEncoder(String protocol) {
		return new WsEncoder(false);
	}

	/* (non-Javadoc)
	 * @see ProtocolProvider#createResponseDecoder()
	 */
	@Override
	public ResponseDecoder<?, ?> createResponseDecoder(String protocol) {
		return null;
	}

	
	
}
