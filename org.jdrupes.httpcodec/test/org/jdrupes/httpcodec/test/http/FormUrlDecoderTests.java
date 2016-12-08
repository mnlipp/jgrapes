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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.Map;

import org.jdrupes.httpcodec.util.FormUrlDecoder;
import org.junit.Test;

public class FormUrlDecoderTests {

	@Test
	public void test() throws UnsupportedEncodingException {
		FormUrlDecoder decoder = new FormUrlDecoder();
		ByteBuffer in = ByteBuffer.wrap(("first=" + URLEncoder
		        .encode("ValueÄ", "utf-8") + "&second=Val").getBytes("utf-8"));
		decoder.addData(in);
		in = ByteBuffer.wrap((URLEncoder.encode("ueÖ", "utf-8") + "&third="
		        + URLEncoder.encode("ValueÜ", "utf-8"))
		                .getBytes("utf-8"));
		decoder.addData(in);
		Map<String, String> fields = decoder.getFields();
		assertEquals(3, fields.size());
		assertEquals("ValueÄ", fields.get("first"));
		assertEquals("ValueÖ", fields.get("second"));
		assertEquals("ValueÜ", fields.get("third"));
	}

}
