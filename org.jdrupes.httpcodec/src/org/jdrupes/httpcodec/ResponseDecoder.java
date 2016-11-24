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

/**
 * A decoder that is used to encode a request. This type of decoder 
 * provides an additional method and has an extended result type.
 * 
 * @param <T>
 *            the type of the message header to be decoded (the response)
 * @param <Q>
 *            the type of the message header that caused the response (the
 *            request)
 * 
 * @author Michael N. Lipp
 */
public interface ResponseDecoder<T extends MessageHeader, 
	Q extends MessageHeader> extends Decoder<T> {

	/**
	 * Causes the decoder to interpret the data in invocations of
	 * {@link #decode(ByteBuffer, Buffer, boolean)} as response to the given
	 * request header. Some protocols need information from the previously 
	 * sent request in order to interpret the response correctly.
	 * <P>
	 * Must be invoked before the first invocation of
	 * {@link #decode(ByteBuffer, Buffer, boolean)} for a given response.
	 * 
	 * @param request
	 *            the request header
	 */
	public void decodeResponseTo(Q request);

	/**
	 * Decodes the next chunk of data.
	 * 
	 * @param in
	 *            holds the data to be decoded
	 * @param out
	 *            gets the body data (if any) written to it
	 * @param endOfInput
	 *            {@code true} if there is no input left beyond the data
	 *            currently in the {@code in} buffer (indicates end of body or
	 *            no body at all)
	 * @return the result
	 * @throws ProtocolException
	 *             if the message violates the HTTP
	 */
	@Override
	public Result decode(ByteBuffer in, Buffer out, boolean endOfInput)
	        throws ProtocolException;
	
	/**
	 * The result from encoding a response. In addition to the usual
	 * codec result, a result decoder may signal to the invoker that the
	 * connection to the responder must be closed.
	 * 
	 * @author Michael N. Lipp
	 */
	public class Result extends Decoder.Result {

		private boolean closeConnection;
		private String newProtocol;
		private ResponseDecoder<MessageHeader, MessageHeader> newDecoder;
		private RequestEncoder<MessageHeader> newEncoder;
		
		/**
		 * Returns a new result.
		 * 
		 * @param headerCompleted
		 *            indicates that the message header has been completed and
		 *            the message (without body) is available
		 * @param overflow
		 *            {@code true} if the data didn't fit in the out buffer
		 * @param underflow
		 *            {@code true} if more data is expected
		 * @param closeConnection
		 *            {@code true} if the connection should be closed
		 * @param newProtocol the name of the new protocol if a switch occurred
		 * @param newDecoder the new decoder if a switch occurred
		 * @param newEncoder the new decoder if a switch occurred
		 */
		public Result(boolean headerCompleted, boolean overflow,
		        boolean underflow, boolean closeConnection,
		        String newProtocol, ResponseDecoder<MessageHeader, 
		        MessageHeader> newDecoder, 
		        RequestEncoder<MessageHeader> newEncoder) {
			super(headerCompleted, overflow, underflow);
			this.closeConnection = closeConnection;
			this.newProtocol = newProtocol;
			this.newDecoder = newDecoder;
			this.newEncoder = newEncoder;
		}

		/**
		 * Indicates that the connection to the sender of the response must be
		 * closed.
		 * 
		 * @return the value
		 */
		public boolean getCloseConnection() {
			return closeConnection;
		}
		
		/**
		 * The name of the protocol to be used for the next request
		 * if a protocol switch occured.
		 * 
		 * @return the name or {@code null} if no protocol switch occured
		 */
		public String newProtocol() {
			return newProtocol;
		}
		
		/**
		 * The response decoder to be used for the next response
		 * if a protocol switch occurred.
		 * 
		 * @return the decoder or {@code null} if no protocol switch occured
		 */
		public ResponseDecoder<MessageHeader, MessageHeader> newDecoder() {
			return newDecoder;
		}
		
		/**
		 * The request encoder to be used for the next request
		 * if a protocol switch occurred.
		 * 
		 * @return the encoder or {@code null} if no protocol switch occured
		 */
		public RequestEncoder<MessageHeader> newEncoder() {
			return newEncoder;
		}
	}
}
