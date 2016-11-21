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
package org.jdrupes.httpcodec;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * An engine that can be used as a server. It has an associated
 * request decoder and response encoder.
 * 
 * @param <Q> the message header type handled by the decoder (the request)
 * @param <R> the message header type handled be the encoder (the response)
 * 
 * @author Michael N. Lipp
 */
public class ServerEngine<Q extends MessageHeader, R extends MessageHeader>
	extends Engine<Q, R> {

	private RequestDecoder<Q, R> requestDecoder;
	private Encoder<R> responseEncoder;
	
	/**
	 * Creates a new instance.
	 */
	public ServerEngine
		(RequestDecoder<Q, R> requestDecoder, Encoder<R> responseEncoder) {
		this.requestDecoder = requestDecoder;
		this.responseEncoder = responseEncoder;
	}

	/**
	 * Returns the request decoder.
	 * 
	 * @return the request decoder
	 */
	public Decoder<Q> requestDecoder() {
		return requestDecoder;
	}

	/**
	 * Returns the response encoder.
	 * 
	 * @return the response encoder
	 */
	public Encoder<R> responseEncoder() {
		return responseEncoder;
	}

	/**
	 * Convenience method to invoke the decoder's decode method.
	 * 
	 * @param in the data to decode
	 * @param out the decoded data
	 * @param endOfInput {@code true} if this invocation finishes the message
	 * @return the result
	 * @throws ProtocolException if the input violates the protocol
	 */
	public RequestDecoder.Result<R> decode
		(ByteBuffer in, Buffer out, boolean endOfInput)
			throws ProtocolException {
		return requestDecoder.decode(in, out, endOfInput);
	}

	/**
	 * Convenience method to invoke the encoder's encode method.
	 * 
	 * @param messageHeader the message header
	 */
	public void encode(R messageHeader) {
		responseEncoder.encode(messageHeader);
	}

	/**
	 * Convenience method to invoke the encoder's encode method.
	 * 
	 * @param out the decoded data
	 * @return the result
	 */
	public Codec.Result encode(
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
	 */
	public Codec.Result encode(
	        Buffer in, ByteBuffer out, boolean endOfInput) {
		return responseEncoder.encode(in, out, endOfInput);
	}

	/**
	 * Returns the last fully decoded request if it exists.
	 * 
	 * @return the request
	 */
	public Optional<Q> currentRequest() {
		return requestDecoder.getHeader();
	}
	
}
