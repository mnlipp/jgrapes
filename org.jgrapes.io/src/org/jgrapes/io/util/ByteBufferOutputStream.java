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

package org.jgrapes.io.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.jgrapes.core.EventPipeline;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;

/**
 * An {@link OutputStream} that is backed by {@link ByteBuffer}s obtained from a
 * queue. When a byte buffer is full, a {@link Output} event (default) is
 * generated and a new buffer is fetched from the queue.
 *
 */
public class ByteBufferOutputStream extends OutputStream {

	private IOSubchannel channel;
	private EventPipeline eventPipeline;
	private boolean sendInputEvents;
	private ManagedByteBuffer buffer;
	private boolean sendClose;

	/**
	 * Creates a new instance that uses {@link Output} events to dispatch
	 * buffers.
	 * 
	 * @param channel
	 *            the channel to fire events on
	 * @param eventPipeline
	 *            the event pipeline used for firing events
	 */
	public ByteBufferOutputStream(IOSubchannel channel,
	        EventPipeline eventPipeline) {
		this.channel = channel;
		this.eventPipeline = eventPipeline;
		sendInputEvents = false;
		sendClose = true;
		buffer = null;
	}

	/**
	 * Causes the data to be fired as {@link Input} events rather
	 * than the usual {@link Output} events. 
	 * 
	 * @return the stream for easy chaining
	 */
	public ByteBufferOutputStream sendInputEvents() {
		sendInputEvents = true;
		return this;
	}
	
	/**
	 * Suppresses the sending of a close event when the stream is closed. 
	 * 
	 * @return the stream for easy chaining
	 */
	public ByteBufferOutputStream suppressClose() {
		sendClose = false;
		return this;
	}
	
	private void ensureBufferAvailable() throws IOException {
		if (buffer != null) {
			return;
		}
		try {
			buffer = channel.byteBufferPool().acquire();
		} catch (InterruptedException e) {
			throw new IOException(e);
		}			
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public void write(int data) throws IOException {
		ensureBufferAvailable();
		buffer.put((byte) data);
		if (!buffer.hasRemaining()) {
			flush(false);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	@Override
	public void write(byte[] data, int offset, int length) throws IOException {
		while (true) {
			ensureBufferAvailable();
			if (buffer.remaining() > length) {
				buffer.put(data, offset, length);
				break;
			} else if (buffer.remaining() == length) {
				buffer.put(data, offset, length);
				flush(false);
				break;
			} else {
				int chunkSize = buffer.remaining();
				buffer.put(data, offset, chunkSize);
				flush(false);
				length -= chunkSize;
				offset += chunkSize;
			}
		}
	}

	/**
	 * Creates and fires a {@link Output} event with the buffer being filled and
	 * obtains a new buffer from the queue unless the end of record is set. 
	 * The end of record flag of the event is set according to the parameter.
	 */
	private void flush(boolean endOfRecord) throws IOException {
		if (buffer == null) {
			if (!endOfRecord) {
				return;
			}
			ensureBufferAvailable();
		}
		if (buffer.position() == 0 && !endOfRecord) {
			// Nothing to flush
			buffer.unlockBuffer();
		} else if (sendInputEvents) {
			buffer.flip();
			eventPipeline.fire(
					new Input<ManagedByteBuffer>(buffer, endOfRecord), channel);
		} else {
			eventPipeline.fire(
					new Output<ManagedByteBuffer>(buffer, endOfRecord), channel);
		}
		buffer = null;
	}

	/**
	 * Creates and fires a {@link Output} event with the buffer being filled and
	 * obtains a new buffer from the queue.
	 */
	@Override
	public void flush() throws IOException {
		flush(false);
	}
	
	/**
	 * Sends any remaining data with the end of record flag set.
	 */
	@Override
	public void close() throws IOException {
		flush(true);
		if (sendClose) {
			eventPipeline.fire(new Close(), channel);
		}
	}

}
