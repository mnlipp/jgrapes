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
package org.jdrupes.httpcodec.test.http;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.Encoder;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpProtocol;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.server.HttpResponseEncoder;
import org.junit.Test;

/**
 * Tests focusing on encoding the response header.
 * 
 * @author Michael N. Lipp
 */
public class ResponseEncoderTests {

	@Test
	public void testSimpleResponse() {
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_1,
		        HttpStatus.OK, false);
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		Encoder.Result result = encoder.encode(out);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		String encoded = new String(out.array(), 0, out.position());
		assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\n"));
	}

	@Test
	public void testSimpleResponseTinyOut() {
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_1,
		        HttpStatus.OK, false);
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		ByteBuffer tinyOut = ByteBuffer.allocate(1);
		while (true) {
			Encoder.Result result = encoder.encode(tinyOut);
			assertFalse(result.isUnderflow());
			assertFalse(result.getCloseConnection());
			tinyOut.flip();
			if (tinyOut.hasRemaining()) {
				out.put(tinyOut);
				tinyOut.compact();
			}
			if (!result.isOverflow()) {
				break;
			}
		}
		String encoded = new String(out.array(), out.arrayOffset(),
		        out.position());
		assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\n"));
	}

}
