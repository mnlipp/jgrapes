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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * Provides a wrapper that makes a {@link CharsetDecoder} behave like an
 * optimized {@code CharsetDecoder}.
 * <P>
 * The default implementations of the {@link CharsetDecoder}s in the JRE leave
 * data in the input buffer if there isn't sufficient data to decode the next
 * character. The invoker is expected to add more data to the input buffer in
 * response to the underflow result. This processing model can be used if you
 * have a single buffer that you fill using some source and drain using the
 * decoder.
 * <P>
 * If you use a model where one buffer is drained by the decoder while in
 * parallel another buffer is being filled by the source, this processing model
 * induces some difficulties, because an unknown number of bytes would have to
 * be copied over from the newly filled buffer to the previously decoded buffer.
 * This class provides what the documentation of
 * {@link CharsetDecoder#decodeLoop} calls an "optimized implementation". It
 * always drains the input buffer completely, caching any bytes that cannot be
 * decoded (yet) because they don't encode a complete character. When the
 * {@link #decode(ByteBuffer, CharBuffer, boolean)} method is invoked for the
 * next time, as many bytes as needed to decode a complete character will be
 * added to the cached bytes and the now available character is appended to the
 * output. Then, the rest of the input is bulk processed as usual.
 * 
 * @author Michael N. Lipp
 */
public class OptimizedCharsetDecoder {

	private CharsetDecoder backing;
	private ByteBuffer pending;

	/**
	 * Creates a new instance.
	 * 
	 * @param backing the backing charset decoder
	 */
	public OptimizedCharsetDecoder(CharsetDecoder backing) {
		super();
		this.backing = backing;
		pending = ByteBuffer.allocate(32);
	}

	/**
	 * @return the result
	 * @see java.nio.charset.CharsetDecoder#averageCharsPerByte()
	 */
	public final float averageCharsPerByte() {
		return backing.averageCharsPerByte();
	}

	/**
	 * @return the result
	 * @see java.nio.charset.CharsetDecoder#charset()
	 */
	public final Charset charset() {
		return backing.charset();
	}

	/**
	 * @param in the data to decode
	 * @param out the decoded data
	 * @param endOfInput {@code true} if the completes the input
	 * @return the result
	 * @see java.nio.charset.CharsetDecoder#decode(java.nio.ByteBuffer,
	 *      java.nio.CharBuffer, boolean)
	 */
	public final CoderResult decode(ByteBuffer in, CharBuffer out,
	        boolean endOfInput) {
		CoderResult result;
		if (pending.position() > 0) {
			if (!out.hasRemaining()) {
				// There must be space for "the one" decoded character
				return CoderResult.OVERFLOW;
			}
			while (true) {
				if (!in.hasRemaining()) {
					if (!endOfInput) {
						return CoderResult.UNDERFLOW;
					}
					return backing.decode(pending, out, endOfInput);
				}
				pending.put(in.get());
				pending.flip();
				result = backing.decode(pending, out, endOfInput);
				pending.compact();
				if (result.isOverflow() || result.isMalformed()) {
					return result;
				}
				if (pending.position() > 0) {
					continue;
				}
				break;
			}
		}
		// Handle rest of input
		result = backing.decode(in, out, endOfInput);
		if (result.isUnderflow() && in.hasRemaining()) {
			byte[] remaining = new byte[in.remaining()];
			in.get(remaining);
			pending.put(remaining);
		}
		return result;
	}

	/**
	 * @param in the data
	 * @return the result
	 * @throws CharacterCodingException if an error occurred
	 * @see java.nio.charset.CharsetDecoder#decode(java.nio.ByteBuffer)
	 */
	public final CharBuffer decode(ByteBuffer in)
	        throws CharacterCodingException {
		return backing.decode(in);
	}

	/**
	 * @return the result
	 * @see java.nio.charset.CharsetDecoder#detectedCharset()
	 */
	public Charset detectedCharset() {
		return backing.detectedCharset();
	}

	/**
	 * @param out the decoded data
	 * @return the result
	 * @see java.nio.charset.CharsetDecoder#flush(java.nio.CharBuffer)
	 */
	public final CoderResult flush(CharBuffer out) {
		if (pending.position() > 0) {
			// Shouldn't happen, hard to know what to do...
			pending.clear();
		}
		return backing.flush(out);
	}

	/**
	 * @return the result
	 * @see java.nio.charset.CharsetDecoder#isAutoDetecting()
	 */
	public boolean isAutoDetecting() {
		return backing.isAutoDetecting();
	}

	/**
	 * @return the result
	 * @see java.nio.charset.CharsetDecoder#isCharsetDetected()
	 */
	public boolean isCharsetDetected() {
		return backing.isCharsetDetected();
	}

	/**
	 * @return the result
	 * @see java.nio.charset.CharsetDecoder#malformedInputAction()
	 */
	public CodingErrorAction malformedInputAction() {
		return backing.malformedInputAction();
	}

	/**
	 * @return the result
	 * @see java.nio.charset.CharsetDecoder#maxCharsPerByte()
	 */
	public final float maxCharsPerByte() {
		return backing.maxCharsPerByte();
	}

	/**
	 * @param newAction the action
	 * @return the result
	 * @see java.nio.charset.CharsetDecoder#onMalformedInput
	 *      (java.nio.charset.CodingErrorAction)
	 */
	public final CharsetDecoder onMalformedInput(CodingErrorAction newAction) {
		return backing.onMalformedInput(newAction);
	}

	/**
	 * @param newAction the action
	 * @return the result
	 * @see java.nio.charset.CharsetDecoder#onUnmappableCharacter
	 *      (java.nio.charset.CodingErrorAction)
	 */
	public final CharsetDecoder onUnmappableCharacter(
	        CodingErrorAction newAction) {
		return backing.onUnmappableCharacter(newAction);
	}

	/**
	 * @param newReplacement the replacement
	 * @return the result
	 * @see java.nio.charset.CharsetDecoder#replaceWith(java.lang.String)
	 */
	public final CharsetDecoder replaceWith(String newReplacement) {
		return backing.replaceWith(newReplacement);
	}

	/**
	 * @return the result
	 * @see java.nio.charset.CharsetDecoder#replacement()
	 */
	public final String replacement() {
		return backing.replacement();
	}

	/**
	 * @return the result
	 * @see java.nio.charset.CharsetDecoder#reset()
	 */
	public final CharsetDecoder reset() {
		pending.clear();
		return backing.reset();
	}

	/**
	 * @return the result
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return backing.toString();
	}

	/**
	 * @return the result
	 * @see java.nio.charset.CharsetDecoder#unmappableCharacterAction()
	 */
	public CodingErrorAction unmappableCharacterAction() {
		return backing.unmappableCharacterAction();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return backing.hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OptimizedCharsetDecoder other = (OptimizedCharsetDecoder) obj;
		if (backing == null) {
			if (other.backing != null)
				return false;
		} else if (!backing.equals(other.backing))
			return false;
		return true;
	}
}
