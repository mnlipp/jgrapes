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

import org.jdrupes.httpcodec.MessageHeader;
import org.jdrupes.httpcodec.RequestDecoder;
import org.jdrupes.httpcodec.RequestEncoder;
import org.jdrupes.httpcodec.ResponseDecoder;
import org.jdrupes.httpcodec.ResponseEncoder;
import org.jdrupes.httpcodec.plugin.ProtocolProvider;
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
	}

	/* (non-Javadoc)
	 * @see ProtocolProvider#createRequestEncoder()
	 */
	@Override
	public RequestEncoder<? extends MessageHeader> createRequestEncoder() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see ProtocolProvider#createRequestDecoder()
	 */
	@Override
	public RequestDecoder<? extends MessageHeader, 
			? extends MessageHeader> createRequestDecoder() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see ProtocolProvider#createResponseEncoder()
	 */
	@Override
	public ResponseEncoder<? extends MessageHeader> createResponseEncoder() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see ProtocolProvider#createResponseDecoder()
	 */
	@Override
	public ResponseDecoder<? extends MessageHeader, 
			? extends MessageHeader> createResponseDecoder() {
		// TODO Auto-generated method stub
		return null;
	}

	
	
}
