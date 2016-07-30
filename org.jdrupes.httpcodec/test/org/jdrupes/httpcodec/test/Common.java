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

import java.nio.ByteBuffer;

import org.jdrupes.httpcodec.HttpRequestDecoder;
import org.jdrupes.httpcodec.HttpResponseDecoder;
import org.jdrupes.httpcodec.HttpResponseEncoder;
import org.jdrupes.httpcodec.HttpResponseEncoder.Result;
import org.jdrupes.httpcodec.ProtocolException;

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
			HttpResponseEncoder encoder, ByteBuffer in, ByteBuffer out) {
		ByteBuffer tinyIn = ByteBuffer.allocate(1);
		tinyIn.flip(); // Initially empty
		ByteBuffer tinyOut = ByteBuffer.allocate(1);
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
