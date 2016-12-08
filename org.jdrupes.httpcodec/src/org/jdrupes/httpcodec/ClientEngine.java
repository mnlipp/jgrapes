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
package org.jdrupes.httpcodec;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * An engine that can be used as a client. It has an associated
 * request encoder and a response decoder.
 * 
 * @param <Q> the message header type handled be the encoder (the request)
 * @param <R> the message header type handled by the decoder (the response)
 * 
 * @author Michael N. Lipp
 */
public abstract class ClientEngine<Q extends MessageHeader, 
	R extends MessageHeader> extends Engine {

	private Encoder<Q> requestEncoder;
	private ResponseDecoder<R, Q> responseDecoder;
	
	/**
	 * Creates a new instance.
	 * 
	 * @param requestEncoder the encoder for the request
	 * @param responseDecoder the decoder for the response
	 */
	public ClientEngine(Encoder<Q> requestEncoder, 
			ResponseDecoder<R, Q> responseDecoder) {
		this.requestEncoder = requestEncoder;
		this.responseDecoder = responseDecoder;
	}
	
	/**
	 * @return the requestEncoder
	 */
	public Encoder<Q> requestEncoder() {
		return requestEncoder;
	}
	/**
	 * @return the responseDecoder
	 */
	public Decoder<R,Q> responseDecoder() {
		return responseDecoder;
	}

	/**
	 * Convenience method to invoke the encoder's encode method.
	 * 
	 * @param out the buffer to use for the result
	 * @return the result
	 */
	public Codec.Result encode(ByteBuffer out) {
		return requestEncoder.encode(out);
	}

	/**
	 * Convenience method to invoke the encoder's encode method.
	 * 
	 * @param in the buffer with the data to encode
	 * @param out the buffer to use for the result
	 * @param endOfInput {@code true} if end of input
	 * @return the result
	 */
	public Codec.Result encode(Buffer in, ByteBuffer out, boolean endOfInput) {
		return requestEncoder.encode(in, out, endOfInput);
	}

	/**
	 * Convenience method to invoke the encoder's encode method.
	 * 
	 * @param messageHeader the message header
	 */
	public void encode(Q messageHeader) {
		requestEncoder.encode(messageHeader);
	}

	/**
	 * Convenience method to invoke the decoder's decode method.
	 * 
	 * @param request the request
	 */
	public void decodeResponseTo(Q request) {
		responseDecoder.decodeResponseTo(request);
	}

	/**
	 * Convenience method to invoke the decoder's decode method.
	 * 
	 * @param in the buffer with the data to decode
	 * @param out the buffer to use for the result
	 * @param endOfInput {@code true} if end of input
	 * @return the result
	 * @throws ProtocolException if the input violates the protocol
	 * @see org.jdrupes.httpcodec.protocols.http.client.HttpResponseDecoder#decode(java.nio.ByteBuffer,
	 *      java.nio.Buffer, boolean)
	 */
	public Decoder.Result<Q> decode(
	        ByteBuffer in, Buffer out, boolean endOfInput)
	        throws ProtocolException {
		return responseDecoder.decode(in, out, endOfInput);
	}
	
}
