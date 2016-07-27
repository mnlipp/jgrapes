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

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;

import org.jdrupes.httpcodec.internal.CodecResult;
import org.jdrupes.httpcodec.internal.Encoder;

/**
 * @author Michael N. Lipp
 *
 */
public class HttpRequestEncoder extends Encoder<HttpRequest> {

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.internal.Encoder#newResult(boolean, boolean)
	 */
	@Override
	protected Result newResult(boolean overflow, boolean underflow) {
		return new Result(overflow, underflow);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.internal.Encoder#startMessage(org.jdrupes.httpcodec.internal.MessageHeader, java.io.Writer)
	 */
	@Override
	protected void startMessage(HttpRequest messageHeader, Writer writer)
	        throws IOException {
		writer.write(messageHeader.getMethod());
		writer.write(" ");
		writer.write(messageHeader.getRequestUri().toString());
		writer.write(" ");
		writer.write(messageHeader.getProtocol().toString());
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
	public Result encode(ByteBuffer in, ByteBuffer out) {
		return (Result)super.encode(in, out);
	}

	public class Result extends CodecResult {

		/**
		 * Returns a new result.
		 *
		 * @param overflow
		 *            {@code true} if the data didn't fit in the out buffer
		 * @param underflow
		 *            {@code true} if more data is expected
		 */
		public Result(boolean overflow, boolean underflow) {
			super(overflow, underflow);
		}
	}
	
}
