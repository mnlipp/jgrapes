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
package org.jdrupes.httpcodec.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Michael N. Lipp
 *
 */
public class FormUrlDecoder {

	private Map<String,String> fields = new HashMap<>();
	private String rest = "";
	
	public void addData(ByteBuffer b) {
		try {
			String data;
			if (b.hasArray()) {
				data = rest + new String(b.array(),
						b.arrayOffset() + b.position(), b.remaining(), "ascii");
			} else {
				byte[] bc = new byte[b.remaining()];
				b.get(bc);
				data = rest + new String(bc, "ascii");
			}
			int oldPos = 0;
			while (true) {
				int newPos = data.indexOf('&', oldPos);
				if (newPos < 0) {
					rest = data.substring(oldPos);
					break;
				}
				split(data, oldPos, newPos);
				oldPos = newPos + 1;
			}
		} catch (UnsupportedEncodingException e) {
			// Using only built-in encodings
			e.printStackTrace();
		}
	}

	private void split(String pairString, int pairStart, int pairEnd) {
		int eqPos = pairString.indexOf('=', pairStart);
		if (eqPos < 0) {
			return;
		}
		try {
			fields.put(URLDecoder.decode(pairString.substring(pairStart, eqPos),
			        "utf-8"),
			        URLDecoder.decode(pairString.substring(eqPos + 1, pairEnd),
			                "utf-8"));
		} catch (UnsupportedEncodingException e) {
			// Using only built-in encodings
		}
	}
	
	public Map<String,String> getFields() {
		split(rest, 0, rest.length());
		rest = "";
		return fields;
	}
}
