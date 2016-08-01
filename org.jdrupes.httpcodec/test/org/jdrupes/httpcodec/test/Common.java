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
package org.jdrupes.httpcodec.test;

import static org.junit.Assert.assertTrue;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.client.HttpResponseDecoder;
import org.jdrupes.httpcodec.server.HttpResponseEncoder;

/**
 * @author Michael N. Lipp
 *
 */
public class Common {

	/**
	 * 
	 */
	public Common() {
		// TODO Auto-generated constructor stub
	}

	public static HttpResponseEncoder.Result tinyEncodeLoop(
			HttpResponseEncoder encoder, Buffer in, ByteBuffer out) {
		return tinyEncodeLoop(encoder, in, 1, out, 1);
	}
	
	public static HttpResponseEncoder.Result tinyEncodeLoop(
			HttpResponseEncoder encoder, Buffer in, int inSize,
			ByteBuffer out, int outSize) {
		Buffer tinyIn = (in instanceof CharBuffer) ? CharBuffer.allocate(1)
				: ByteBuffer.allocate(inSize);
		tinyIn.flip(); // Initially empty
		ByteBuffer tinyOut = ByteBuffer.allocate(outSize);
		boolean endOfInput = false;
		HttpResponseEncoder.Result lastResult;
		while (true) {
			lastResult = encoder.encode(tinyIn, tinyOut, endOfInput);
			if (lastResult.isOverflow()) {
				tinyOut.flip();
				out.put(tinyOut);
				tinyOut.compact();
				continue;
			}
			if (lastResult.isUnderflow()) {
				assertTrue(in.hasRemaining());
				tinyIn.clear();
				if (in instanceof ByteBuffer) {
					((ByteBuffer)tinyIn).put(((ByteBuffer)in).get());
				} else {
					((CharBuffer)tinyIn).put(((CharBuffer)in).get());
				}
				tinyIn.flip();
				if (!in.hasRemaining()) {
					endOfInput = true;
				}
				continue;
			}
			break;
		}
		if (tinyOut.position() > 0) {
			tinyOut.flip();
			out.put(tinyOut);
			tinyOut.compact();
		}
		return lastResult;
	}

	public static HttpResponseDecoder.Result tinyDecodeLoop(
			HttpResponseDecoder decoder, ByteBuffer in, ByteBuffer out) 
					throws ProtocolException {
		ByteBuffer tinyIn = ByteBuffer.allocate(1);
		tinyIn.flip(); // Initially empty
		ByteBuffer tinyOut = ByteBuffer.allocate(1);
		boolean endOfInput = false;
		HttpResponseDecoder.Result lastResult;
		while (true) {
			lastResult = decoder.decode(tinyIn, tinyOut, endOfInput);
			if (lastResult.isOverflow()) {
				tinyOut.flip();
				out.put(tinyOut);
				tinyOut.compact();
				continue;
			}
			if (lastResult.isUnderflow()) {
				assertTrue(in.hasRemaining());
				tinyIn.clear();
				tinyIn.put(in.get());
				tinyIn.flip();
				if (!in.hasRemaining()) {
					endOfInput = true;
				}
				continue;
			}
			break;
		}
		if (tinyOut.position() > 0) {
			tinyOut.flip();
			out.put(tinyOut);
			tinyOut.compact();
		}
		return lastResult;
	}
}
