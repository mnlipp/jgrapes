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
package org.jdrupes.httpcodec.internal;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.jdrupes.httpcodec.HttpRequest;
import org.jdrupes.httpcodec.HttpResponse;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.client.HttpRequestEncoder;
import org.jdrupes.httpcodec.client.HttpRequestEncoder.Result;
import org.jdrupes.httpcodec.client.HttpResponseDecoder;

/**
 * An engine that can be used as an HTTP client. It has an associated
 * request encoder and a response decoder. This is the base class that
 * has access to (and provides) the internal API.
 * 
 * @author Michael N. Lipp
 */
public abstract class ClientEngine extends Engine<HttpResponse, HttpRequest> {

	private HttpRequestEncoder requestEncoder;
	private HttpResponseDecoder responseDecoder;
	
	/**
	 * 
	 */
	public ClientEngine() {
		requestEncoder = new HttpRequestEncoder(this);
		responseDecoder = new HttpResponseDecoder(this);
	}
	
	/**
	 * @return the requestEncoder
	 */
	public HttpRequestEncoder requestEncoder() {
		return requestEncoder;
	}
	/**
	 * @return the responseDecoder
	 */
	public HttpResponseDecoder responseDecoder() {
		return responseDecoder;
	}

	/**
	 * Convenience method to invoke the encoder's encode method.
	 * 
	 * @see org.jdrupes.httpcodec.client.HttpRequestEncoder#encode(java.nio.ByteBuffer)
	 */
	public Result encode(ByteBuffer out) {
		return requestEncoder.encode(out);
	}

	/**
	 * Convenience method to invoke the encoder's encode method.
	 * 
	 * @see org.jdrupes.httpcodec.client.HttpRequestEncoder#encode(java.nio.Buffer,
	 *      java.nio.ByteBuffer, boolean)
	 */
	public Result encode(Buffer in, ByteBuffer out, boolean endOfInput) {
		return requestEncoder.encode(in, out, endOfInput);
	}

	/**
	 * Convenience method to invoke the encoder's encode method.
	 * 
	 * @see org.jdrupes.httpcodec.internal.Encoder#encode(org.jdrupes.httpcodec.internal.MessageHeader)
	 */
	public void encode(HttpRequest messageHeader) {
		requestEncoder.encode(messageHeader);
	}

	/**
	 * Convenience method to invoke the decoder's decode method.
	 * 
	 * @see org.jdrupes.httpcodec.client.HttpResponseDecoder#decodeResponseTo(org.jdrupes.httpcodec.HttpRequest)
	 */
	public void decodeResponseTo(HttpRequest request) {
		responseDecoder.decodeResponseTo(request);
	}

	/**
	 * Convenience method to invoke the decoder's decode method.
	 * 
	 * @see org.jdrupes.httpcodec.client.HttpResponseDecoder#decode(java.nio.ByteBuffer,
	 *      java.nio.Buffer, boolean)
	 */
	public org.jdrupes.httpcodec.client.HttpResponseDecoder.Result decode(
	        ByteBuffer in, Buffer out, boolean endOfInput)
	        throws ProtocolException {
		return responseDecoder.decode(in, out, endOfInput);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jdrupes.httpcodec.internal.Engine#decoding(org.jdrupes.httpcodec.
	 * internal.MessageHeader)
	 */
	@Override
	void decoding(HttpResponse response) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jdrupes.httpcodec.internal.Engine#encoding(org.jdrupes.httpcodec.
	 * internal.MessageHeader)
	 */
	@Override
	void encoding(HttpRequest request) {
		responseDecoder.decodeResponseTo(request);
	}

}
