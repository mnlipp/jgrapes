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
package org.jdrupes.httpcodec.server;

import java.io.IOException;
import java.io.Writer;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.jdrupes.httpcodec.HttpResponse;
import org.jdrupes.httpcodec.internal.CodecResult;
import org.jdrupes.httpcodec.internal.Encoder;
import org.jdrupes.httpcodec.internal.Engine;
import org.jdrupes.httpcodec.internal.MessageHeader;

/**
 * @author Michael N. Lipp
 */
public class HttpResponseEncoder extends Encoder<HttpResponse> {

	/**
	 * Creates a new encoder that belongs to the given HTTP engine.
	 * 
	 * @param engine the engine
	 */
	public HttpResponseEncoder
		(Engine<? extends MessageHeader,HttpResponse> engine) {
		super(engine);
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

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.internal.Encoder#encode(java.nio.ByteBuffer)
	 */
	@Override
	public Result encode(ByteBuffer out) {
		return (Result)super.encode(out);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.internal.Encoder#encode(java.nio.ByteBuffer, java.nio.ByteBuffer)
	 */
	@Override
	public Result encode(Buffer in, ByteBuffer out, boolean endOfInput) {
		return (Result)super.encode(in, out, endOfInput);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.internal.Encoder#newResult(boolean, boolean)
	 */
	@Override
	protected Result newResult(boolean overflow, boolean underflow) {
		return new Result(overflow, underflow, isClosed());
	}

	public class Result extends CodecResult {

		boolean closeConnection;
		
		/**
		 * Returns a new result.
		 * 
		 * @param overflow
		 *            {@code true} if the data didn't fit in the out buffer
		 * @param underflow
		 *            {@code true} if more data is expected
		 * @param closeConnection
		 *            {@code true} if the connection should be closed
		 */
		public Result(boolean overflow, boolean underflow,
		        boolean closeConnection) {
			super(overflow, underflow);
			this.closeConnection = closeConnection;
		}

		/**
		 * Indicates that the connection to the receiver of the response must be
		 * closed.
		 * 
		 * @return the value
		 */
		public boolean getCloseConnection() {
			return closeConnection;
		}
	}

}
