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
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;

import org.jdrupes.httpcodec.util.ByteBufferOutputStream;

/**
 * @author Michael N. Lipp
 */
public class HttpResponseEncoder {

	private ByteBufferOutputStream outStream;
	private Writer writer;
	private HttpResponse response = null;

	public HttpResponseEncoder() {
		outStream = new ByteBufferOutputStream();
		try {
			writer = new OutputStreamWriter(outStream, "ascii");
		} catch (UnsupportedEncodingException e) {
		}
	}
	
	public boolean encode (HttpResponse response, ByteBuffer out) {
		try {
			if (this.response != null && this.response != response) {
				throw new IllegalStateException();
			}
			outStream.assignBuffer(out);
			if (this.response == null) {
				this.response = response;
				writer.write(response.getProtocol().toString());
				writer.write(" ");
				writer.write(Integer.toString(response.getStatusCode()));
				writer.write(" ");
				writer.write(response.getReasonPhrase());
				writer.write("\r\n");
				writer.write("\r\n");
				writer.flush();
			}
		} catch (IOException e) {
		}
		return outStream.remaining() < 0;
	}
	
	public boolean encode 
		(HttpResponse response, ByteBuffer in, ByteBuffer out) {
		return false;
	}
}
