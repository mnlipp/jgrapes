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
package org.jdrupes.httpcodec.protocols.http.server;

import java.io.IOException;
import java.io.Writer;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import org.jdrupes.httpcodec.Codec;
import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.Encoder;
import org.jdrupes.httpcodec.plugin.ProtocolProvider;
import org.jdrupes.httpcodec.protocols.http.HttpEncoder;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.protocols.http.fields.HttpField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpStringListField;

/**
 * @author Michael N. Lipp
 */
public class HttpResponseEncoder extends HttpEncoder<HttpResponse> {

	private static ServiceLoader<ProtocolProvider> pluginLoader 
		= ServiceLoader.load(ProtocolProvider.class);
	private Map<String,ProtocolProvider> plugins = new HashMap<>();
	private String switchingTo;
	private ProtocolProvider protocolPlugin;

	/**
	 * Creates a new encoder that belongs to the given HTTP engine.
	 */
	public HttpResponseEncoder() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.protocols.http.HttpEncoder#encode(org.jdrupes.httpcodec.protocols.http.HttpMessageHeader)
	 */
	@Override
	public void encode(HttpResponse messageHeader) {
		if (messageHeader.getStatusCode()
					== HttpStatus.SWITCHING_PROTOCOLS.getStatusCode()) {
			switchingTo = prepareSwitchProtocol(messageHeader);
		}
		super.encode(messageHeader);
	}

	private String prepareSwitchProtocol(HttpResponse response) {
		Optional<String> protocol = response
				.getField(HttpStringListField.class, HttpField.UPGRADE)
				.map(l -> l.get(0));
		if (!protocol.isPresent()) {
			response.setStatus(HttpStatus.BAD_REQUEST)
				.setMessageHasBody(false).clearHeaders();
			return null;
		}
		synchronized (pluginLoader) {
			if (plugins.containsKey(protocol.get())) {
				protocolPlugin = plugins.get(protocol.get());
			} else {
				protocolPlugin = StreamSupport
						.stream(pluginLoader.spliterator(), false)
						.filter(p -> p.supportsProtocol(protocol.get()))
						.findFirst().get();
				plugins.put(protocol.get(), protocolPlugin);
			}
		}
		if (protocolPlugin == null) {
			response.setStatus(HttpStatus.BAD_REQUEST)
				.setMessageHasBody(false).clearHeaders();
			return null;
		}
		protocolPlugin.augmentInitialResponse(response);
		if (response.getStatusCode() 
				!= HttpStatus.SWITCHING_PROTOCOLS.getStatusCode()) {
			// Not switching after all
			return null;
		}
		return protocol.get();
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.internal.Encoder#encode(java.nio.ByteBuffer, java.nio.ByteBuffer)
	 */
	@Override
	public Result encode(Buffer in, ByteBuffer out, boolean endOfInput) {
		Result result = (Result)super.encode(in, out, endOfInput);
		if (switchingTo != null && endOfInput 
				&& !result.isUnderflow() && !result.isOverflow()) {
			// Last invocation of encode
			result = newResult(false, false, 
					result.getCloseConnection(), switchingTo, 
					protocolPlugin.createRequestDecoder(switchingTo), 
					protocolPlugin.createResponseEncoder(switchingTo));
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.internal.Encoder#startMessage(java.io.Writer)
	 */
	@Override
	protected void startMessage(HttpResponse response, Writer writer)
	        throws IOException {
		writer.write(response.getProtocol().toString());
		writer.write(" ");
		writer.write(Integer.toString(response.getStatusCode()));
		writer.write(" ");
		writer.write(response.getReasonPhrase());
		writer.write("\r\n");
	}

	/**
	 * Overrides the base interface's factory method in order to make
	 * it return the extended return type.
	 * 
	 * @param overflow
	 *            {@code true} if the data didn't fit in the out buffer
	 * @param underflow
	 *            {@code true} if more data is expected
	 * @param closeConnection
	 *            {@code true} if the connection should be closed
	 */
	@Override
	public Result newResult
		(boolean overflow, boolean underflow, boolean closeConnection) {
		// Cannot add the information about the protocol switch here
		// because we cannot know if this is the last invocation of encode
		// (and therefore newResult).
		return newResult(overflow, underflow, isClosed(), null, null, null);
	}

	/**
	 * Factory method for result.
	 * 
	 * @param overflow
	 *            {@code true} if the data didn't fit in the out buffer
	 * @param underflow
	 *            {@code true} if more data is expected
	 * @param closeConnection
	 *            {@code true} if the connection should be closed
	 * @param newProtocol the name of the new protocol if a switch occurred
	 * @param newDecoder the new decoder if a switch occurred
	 * @param newEncoder the new decoder if a switch occurred
	 * @return the result
	 */
	public Result newResult (boolean overflow, boolean underflow,
	        boolean closeConnection, String newProtocol,
	        Decoder<?, ?> newDecoder, Encoder<?> newEncoder) {
		return new Result (overflow, underflow, closeConnection,
				newProtocol, newDecoder, newEncoder) {
		};
	}
	
	/**
	 * The result from encoding a response. In addition to the usual
	 * codec result, a result encoder may signal to the invoker that the
	 * connection to the requester must be closed and that the protocol has
	 * been switched.
	 * 
	 * @author Michael N. Lipp
	 */
	public abstract static class Result extends Codec.Result
		implements Codec.ProtocolSwitchResult {

		private String newProtocol;
		private Decoder<?, ?> newDecoder;
		private Encoder<?> newEncoder;
		
		/**
		 * Returns a new result.
		 * 
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
		protected Result(boolean overflow, boolean underflow,
		        boolean closeConnection, String newProtocol,
		        Decoder<?, ?> newDecoder, Encoder<?> newEncoder) {
			super(overflow, underflow, closeConnection);
			this.newProtocol = newProtocol;
			this.newEncoder = newEncoder;
			this.newDecoder = newDecoder;
		}

		@Override
		public String newProtocol() {
			return newProtocol;
		}
		
		@Override
		public Encoder<?> newEncoder() {
			return newEncoder;
		}
		
		@Override
		public Decoder<?, ?> newDecoder() {
			return newDecoder;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result
			        + ((newDecoder == null) ? 0 : newDecoder.hashCode());
			result = prime * result
			        + ((newEncoder == null) ? 0 : newEncoder.hashCode());
			result = prime * result
			        + ((newProtocol == null) ? 0 : newProtocol.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			Result other = (Result) obj;
			if (newDecoder == null) {
				if (other.newDecoder != null)
					return false;
			} else if (!newDecoder.equals(other.newDecoder))
				return false;
			if (newEncoder == null) {
				if (other.newEncoder != null)
					return false;
			} else if (!newEncoder.equals(other.newEncoder))
				return false;
			if (newProtocol == null) {
				if (other.newProtocol != null)
					return false;
			} else if (!newProtocol.equals(other.newProtocol))
				return false;
			return true;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("HttpResponseEncoder.Result [overflow=");
			builder.append(isOverflow());
			builder.append(", underflow=");
			builder.append(isUnderflow());
			builder.append(", closeConnection=");
			builder.append(getCloseConnection());
			builder.append(", ");
			if (newProtocol != null) {
				builder.append("newProtocol=");
				builder.append(newProtocol);
				builder.append(", ");
			}
			if (newDecoder != null) {
				builder.append("newDecoder=");
				builder.append(newDecoder);
				builder.append(", ");
			}
			if (newEncoder != null) {
				builder.append("newEncoder=");
				builder.append(newEncoder);
			}
			builder.append("]");
			return builder.toString();
		}


	}
}
