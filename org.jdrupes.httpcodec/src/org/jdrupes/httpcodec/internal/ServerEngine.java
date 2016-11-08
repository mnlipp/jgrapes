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
import org.jdrupes.httpcodec.server.HttpRequestDecoder;
import org.jdrupes.httpcodec.server.HttpRequestDecoder.Result;
import org.jdrupes.httpcodec.server.HttpResponseEncoder;

/**
 * An engine that can be used as an HTTP server. It has an associated
 * request decoder and response encoder. This is the base class that
 * has access to (and provides) the internal API.
 * 
 * @author Michael N. Lipp
 */
public abstract class ServerEngine extends Engine<HttpRequest, HttpResponse> {

	private HttpRequestDecoder requestDecoder;
	private HttpResponseEncoder responseEncoder;
	
	/**
	 * 
	 */
	public ServerEngine() {
		requestDecoder = new HttpRequestDecoder(this);
		responseEncoder = new HttpResponseEncoder(this);
	}

	/**
	 * @return the requestDecoder
	 */
	public HttpRequestDecoder requestDecoder() {
		return requestDecoder;
	}

	/**
	 * @return the responseEncoder
	 */
	public HttpResponseEncoder responseEncoder() {
		return responseEncoder;
	}

	/**
	 * Convenience method to invoke the encoder's encode method.
	 * 
	 * @param messageHeader the message header
	 * @see org.jdrupes.httpcodec.internal.Encoder#encode(org.jdrupes.httpcodec.internal.MessageHeader)
	 */
	public void encode(HttpResponse messageHeader) {
		responseEncoder.encode(messageHeader);
	}

	/**
	 * Convenience method to invoke the decoder's decode method.
	 * 
	 * @param in the data to decode
	 * @param out the decoded data
	 * @param endOfInput {@code true} if this invocation finishes the message
	 * @return the result
	 * @see org.jdrupes.httpcodec.server.HttpRequestDecoder#decode(java.nio.ByteBuffer, java.nio.Buffer, boolean)
	 */
	public Result decode(ByteBuffer in, Buffer out, boolean endOfInput) {
		return requestDecoder.decode(in, out, endOfInput);
	}

	/**
	 * Convenience method to invoke the encoder's encode method.
	 * 
	 * @param out the decoded data
	 * @return the result
	 * @see org.jdrupes.httpcodec.server.HttpResponseEncoder#encode(java.nio.ByteBuffer)
	 */
	public org.jdrupes.httpcodec.server.HttpResponseEncoder.Result encode(
	        ByteBuffer out) {
		return responseEncoder.encode(out);
	}

	/**
	 * Convenience method to invoke the encoder's encode method.
	 * 
	 * @param in the data to encode
	 * @param out the encoded data
	 * @param endOfInput {@code true} if this invocation finishes the message
	 * @return the result
	 * @see org.jdrupes.httpcodec.server.HttpResponseEncoder#encode(java.nio.Buffer, java.nio.ByteBuffer, boolean)
	 */
	public org.jdrupes.httpcodec.server.HttpResponseEncoder.Result encode(
	        Buffer in, ByteBuffer out, boolean endOfInput) {
		return responseEncoder.encode(in, out, endOfInput);
	}

	/**
	 * Returns the last fully decoded request.
	 * 
	 * @return the request
	 */
	public HttpRequest currentRequest() {
		return requestDecoder.getHeader();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jdrupes.httpcodec.internal.Engine#decoding(org.jdrupes.httpcodec.
	 * internal.MessageHeader)
	 */
	@Override
	void decoding(HttpRequest request) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jdrupes.httpcodec.internal.Engine#encoding(org.jdrupes.httpcodec.
	 * internal.MessageHeader)
	 */
	@Override
	void encoding(HttpResponse response) {
	}

}
