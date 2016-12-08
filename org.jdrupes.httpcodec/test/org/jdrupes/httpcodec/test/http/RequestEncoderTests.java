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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpProtocol;
import org.jdrupes.httpcodec.protocols.http.client.HttpRequestEncoder;
import org.junit.Test;

/**
 * Tests focusing on encoding the response header.
 * 
 * @author Michael N. Lipp
 */
public class RequestEncoderTests {

	@Test
	public void testSimpleRequest() throws URISyntaxException {
		HttpRequest request = new HttpRequest("GET", new URI("/"),
		        HttpProtocol.HTTP_1_1, false);
		HttpRequestEncoder encoder = new HttpRequestEncoder(null);
		encoder.encode(request);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		HttpRequestEncoder.Result result = encoder.encode(out);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		String encoded = new String(out.array(), 0, out.position());
		assertTrue(encoded.contains("GET / HTTP/1.1\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\n"));
	}

}
