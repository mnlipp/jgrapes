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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * An {@link OutputStream} that is backed by a {@link ByteBuffer} assigned to
 * the stream. If the buffer becomes full, one or more buffers are allocated as
 * intermediate storage. Their content is copied to the next assigned buffer(s).
 * <p>
 * While writing to this stream, {@link #remaining()} should be checked
 * regularly and the production of data should be suspended if possible when no
 * more space is left to avoid the usage of intermediate storage.
 * 
 * @author Michael N. Lipp
 *
 */
public class ByteBufferOutputStream extends OutputStream {

	private ByteBuffer assignedBuffer = null;
	private Queue<ByteBuffer> overflows = new ArrayDeque<>();
	private ByteBuffer current = null;
	private int overflowBufferSize = 0;
	private long bytesWritten = 0;

	/**
	 * Creates a new instance with an unset overflow buffer size.
	 */
	public ByteBufferOutputStream() {
		super();
	}

	/**
	 * Creates a new instance with the given overflow buffer size.
	 * 
	 * @param overflowBufferSize
	 *            the overflow buffer size to use
	 */
	public ByteBufferOutputStream(int overflowBufferSize) {
		super();
		this.overflowBufferSize = overflowBufferSize;
	}

	/**
	 * Returns the size of the buffers that will be allocated as overflow
	 * buffers.
	 *
	 * @return the allocation size for the overflow buffers
	 */
	public int getOverflowBufferSize() {
		return overflowBufferSize;
	}

	/**
	 * The size of the buffers that are allocated if the assigned buffer
	 * overflows. If not set, buffers are allocated with one fourth of the size
	 * of the assigned buffer or 4096 if no buffer has been assigned yet.
	 * 
	 * @param overflowBufferSize
	 *            the size
	 */
	public void setOverflowBufferSize(int overflowBufferSize) {
		this.overflowBufferSize = overflowBufferSize;
	}

	/**
	 * Clear any buffered data and prepares the buffer for reuse.
	 */
	public void clear() {
		assignedBuffer = null;
		current = null;
		bytesWritten = 0;
		overflows.clear();
	}

	/**
	 * Assign a new buffer to this output stream. If the previously used buffer
	 * had become full and intermediate storage was allocated, the data from the
	 * intermediate storage is copied to the new buffer first. Then, the new
	 * buffer is used for all subsequent write operations.
	 * 
	 * @param buffer
	 *            the buffer to use
	 */
	public void assignBuffer(ByteBuffer buffer) {
		assignedBuffer = buffer;
		// Move any overflow to the new buffer
		while (!overflows.isEmpty()) {
			ByteBuffer head = overflows.peek();
			// Do a "flip with position to mark"
			int writePos = head.position(); // Save position
			head.reset();
			head.limit(writePos);
			if (!ByteBufferUtils.putAsMuchAsPossible(assignedBuffer, head)) {
				// Cannot transfer everything, done what's possible
				head.mark(); // new position for next put
				head.limit(head.capacity());
				head.position(writePos);
				return;
			}
			overflows.remove();
		}
		current = assignedBuffer;
	}

	private void allocateOverflowBuffer() {
		current = ByteBuffer.allocate(
		        overflowBufferSize != 0
		        ? overflowBufferSize
		        : Math.max(4096, (assignedBuffer == null
		                          ? 0 : assignedBuffer.capacity() / 4)));
		current.mark();
		overflows.add(current);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public void write(int b) throws IOException {
		if (current == null || current.remaining() == 0) {
			allocateOverflowBuffer();
		}
		current.put((byte) b);
		bytesWritten += 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	@Override
	public void write(byte[] b, int offset, int length) throws IOException {
		if (current == null) {
			allocateOverflowBuffer();
		}
		bytesWritten += length;
		while (true) {
			if (current.remaining() >= length) {
				current.put(b, offset, length);
				return;
			}
			if (current.remaining() > 0) {
				int processed = current.remaining();
				current.put(b, offset, processed);
				offset += processed;
				length -= processed;
			}
			allocateOverflowBuffer();
		}
	}

	/**
	 * Copies the data from the given buffer to this output stream.
	 * 
	 * @param b the buffer
	 */
	public void write(ByteBuffer b) {
		if (current == null) {
			allocateOverflowBuffer();
		}
		bytesWritten += b.remaining();
		while (true) {
			if (ByteBufferUtils.putAsMuchAsPossible(current, b)) {
				return;
			}
			allocateOverflowBuffer();
		}
	}

	/**
	 * Copies length bytes from the given buffer to this output stream.
	 * 
	 * @param b the buffer
	 * @param length the number of bytes to copy
	 */
	public void write(ByteBuffer b, int length) {
		if (b.remaining() <= length) {
			write(b);
			return;
		}
		int savedLimit = b.limit();
		b.limit(b.position() + length);
		write(b);
		b.limit(savedLimit);
	}

	/**
	 * Returns the number of bytes remaining in the assigned buffer. A negative
	 * value indicates that the assigned buffer is full and one or more overflow
	 * buffers are being used. The absolute value of the negative number is
	 * the number of bytes in the overflow buffer(s).
	 * 
	 * @return the bytes remaining or buffered (negative value)
	 */
	public long remaining() {
		if (overflows.isEmpty()) {
			if (assignedBuffer == null) {
				return 0;
			}
			return assignedBuffer.remaining();
		}
		long sum = 0;
		for (ByteBuffer b : overflows) {
			int curPos = b.position(); // Save position
			b.reset();
			sum += curPos - b.position();
			b.position(curPos);
		}
		return -sum;
	}

	/**
	 * Does not have any effect. May be called for consistent usage of the
	 * output stream.
	 * 
	 * @throws IOException
	 *             if there is still data in intermediate storage
	 * @see java.io.OutputStream#close()
	 */
	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		super.close();
	}

	/**
	 *	The number of bytes written to this output stream.
	 * 
	 * @return the number of bytes written
	 */
	public long bytesWritten() {
		return bytesWritten;
	}
}
