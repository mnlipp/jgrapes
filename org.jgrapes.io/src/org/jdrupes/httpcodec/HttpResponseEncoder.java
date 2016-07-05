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
import java.util.Iterator;
import java.util.Map;

import org.jdrupes.httpcodec.util.ByteBufferOutputStream;

/**
 * @author Michael N. Lipp
 */
public class HttpResponseEncoder {

	private ByteBufferOutputStream outStream;
	private Writer writer;
	private HttpResponse response = null;
	private Iterator<Map.Entry<String,HttpFieldValue>> headerIter = null;

	public HttpResponseEncoder() {
		outStream = new ByteBufferOutputStream();
		try {
			writer = new OutputStreamWriter(outStream, "ascii");
		} catch (UnsupportedEncodingException e) {
		}
	}
	
	public EncoderResult encode (HttpResponse response, ByteBuffer out) {
		outStream.assignBuffer(out);
		try {
			if (this.response == null) {
				newResponse(response);
			}
			// Sanity check
			if (this.response != response) {
				throw new IllegalStateException();
			}
			// If headers remain (new request or buffers full) write them
			if (headerIter != null) {
				continueHeaders();
				if (headerIter != null) {
					return new EncoderResult(true, false);
				}
			}
		} catch (IOException e) {
		}
		return new EncoderResult(false, false);
	}

	/**
	 * Called when encode is invoked with a new response (initial state).
	 * 
	 * @param response
	 * @throws IOException 
	 */
	private void newResponse(HttpResponse response) throws IOException {
		// Invocation with new response
		this.response = response;
		
		// Complete content type
		HttpMediaTypeFieldValue contentType 
			= (HttpMediaTypeFieldValue) response.headers().get("content-type");
		String charset = null;
		if (contentType != null) {
			charset = contentType.getParameter("charset");
			if (charset == null) {
				charset = "utf-8";
				contentType.setParameter("charset", charset);
			}
		}
		
		// Write status line
		writer.write(response.getProtocol().toString());
		writer.write(" ");
		writer.write(Integer.toString(response.getStatusCode()));
		writer.write(" ");
		writer.write(response.getReasonPhrase());
		writer.write("\r\n");
		
		// Start headers
		headerIter = response.headers().entrySet().iterator();
	}
	
	private void continueHeaders() throws IOException {
		while (true) {
			if (!headerIter.hasNext()) {
				writer.write("\r\n");
				writer.flush();
				headerIter = null;
				return;
			}
			Map.Entry<String, HttpFieldValue> header = headerIter.next();
			writer.write(header.getKey());
			writer.write(": ");
			writer.write(header.getValue().asString());
			writer.write("\r\n");
			writer.flush();
			if (outStream.remaining() < 80) {
				return;
			}
		}
	}

	public boolean encode 
		(HttpResponse response, ByteBuffer in, ByteBuffer out) {
		return false;
	}
}
