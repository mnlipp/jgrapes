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
package org.jdrupes.httpcodec.plugin;

import org.jdrupes.httpcodec.MessageHeader;
import org.jdrupes.httpcodec.RequestDecoder;
import org.jdrupes.httpcodec.RequestEncoder;
import org.jdrupes.httpcodec.ResponseDecoder;
import org.jdrupes.httpcodec.ResponseEncoder;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;

/**
 * @author Michael N. Lipp
 */
public abstract class ProtocolProvider {

	/**
	 * Checks if the plugin supports the given protocol.
	 * 
	 * @protocol the protocol in question
	 */
	abstract public boolean supportsProtocol(String protocol);

	/**
	 * Add any required information to the "switching protocols" response
	 * that is sent as the last package of the HTTP and starts the usage
	 * of the new protocol.
	 * 
	 * @param response the response
	 */
	abstract public void augmentInitialResponse(HttpResponse response);
	
	/**
	 * Creates a new request encoder for the protocol.
	 * 
	 * @param the protocol, which must be supported by this plugin
	 * @return the request encoder
	 */
	abstract public RequestEncoder<? extends MessageHeader> 
		createRequestEncoder(String protocol);
	
	/**
	 * Creates a new request decoder for the protocol.
	 * 
	 * @param the protocol, which must be supported by this plugin
	 * @return the request decoder
	 */
	abstract public RequestDecoder<? extends MessageHeader, 
			? extends MessageHeader> createRequestDecoder(String protocol);
	
	/**
	 * Creates a new response encoder for the protocol.
	 * 
	 * @param the protocol, which must be supported by this plugin
	 * @return the response encoder
	 */
	abstract public ResponseEncoder<? extends MessageHeader>
		createResponseEncoder(String protocol);
	
	/**
	 * Creates a new response decoder for the protocol.
	 * 
	 * @param the protocol, which must be supported by this plugin
	 * @return the response decoder
	 */
	abstract public ResponseDecoder<? extends MessageHeader,
			? extends MessageHeader> createResponseDecoder(String protocol);
	
	
}
