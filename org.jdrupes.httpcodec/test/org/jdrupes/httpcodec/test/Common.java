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
package org.jdrupes.httpcodec.test;

import static org.junit.Assert.assertTrue;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.Encoder;
import org.jdrupes.httpcodec.ProtocolException;

/**
 * @author Michael N. Lipp
 *
 */
public class Common {

	public static Encoder.Result tinyEncodeLoop(
			Encoder<?> encoder, Buffer in, ByteBuffer out) {
		return tinyEncodeLoop(encoder, in, 1, out, 1, false);
	}
	
	public static Encoder.Result tinyEncodeLoop(
			Encoder<?> encoder, Buffer in, int inSize,
			ByteBuffer out, int outSize, boolean fillInOnStart) {
		Buffer tinyIn = (in instanceof CharBuffer) 
				? CharBuffer.allocate(inSize) : ByteBuffer.allocate(inSize);
		if (fillInOnStart) {
			fillBuffer(tinyIn, in);
		} else {
			tinyIn.flip(); // Initially empty
		}
		ByteBuffer tinyOut = ByteBuffer.allocate(outSize);
		Encoder.Result lastResult;
		while (true) {
			lastResult = encoder.encode(tinyIn, tinyOut, !in.hasRemaining());
			if (lastResult.isOverflow()) {
				tinyOut.flip();
				out.put(tinyOut);
				tinyOut.compact();
				continue;
			}
			if (lastResult.isUnderflow()) {
				assertTrue(in.hasRemaining());
				tinyIn.clear();
				fillBuffer(tinyIn, in);
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

	private static void fillBuffer(Buffer dest, Buffer src) {
		if (dest.remaining() >= src.remaining()) {
			if (dest instanceof ByteBuffer) {
				((ByteBuffer)dest).put((ByteBuffer)src);
			} else {
				((CharBuffer)dest).put((CharBuffer)src);
			}
		} else if (dest.remaining() > 0) {
			int oldLimit = src.limit();
			src.limit(src.position() + dest.remaining());
			if (dest instanceof ByteBuffer) {
				((ByteBuffer)dest).put((ByteBuffer)src);
			} else {
				((CharBuffer)dest).put((CharBuffer)src);
			}
			src.limit(oldLimit);
		}
		dest.flip();		
	}
	
	public static Decoder.Result<?> tinyDecodeLoop(
			Decoder<?,?> decoder, ByteBuffer in, Buffer out) 
					throws ProtocolException {
		ByteBuffer tinyIn = ByteBuffer.allocate(1);
		tinyIn.flip(); // Initially empty
		Buffer tinyOut = (out instanceof ByteBuffer) 
				? ByteBuffer.allocate(1) : CharBuffer.allocate(1);
		boolean endOfInput = false;
		Decoder.Result<?> lastResult;
		while (true) {
			lastResult = decoder.decode(tinyIn, tinyOut, endOfInput);
			if (lastResult.isOverflow()) {
				tinyOut.flip();
				if (out instanceof ByteBuffer) {
					((ByteBuffer)out).put((ByteBuffer)tinyOut);
					((ByteBuffer)tinyOut).compact();
				} else {
					((CharBuffer)out).put(((CharBuffer)tinyOut).get());
					((CharBuffer)tinyOut).compact();
				}
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
			if (out instanceof ByteBuffer) {
				((ByteBuffer)out).put((ByteBuffer)tinyOut);
				((ByteBuffer)tinyOut).compact();
			} else {
				((CharBuffer)out).put(((CharBuffer)tinyOut).get());
				((CharBuffer)tinyOut).compact();
			}
		}
		return lastResult;
	}
}
