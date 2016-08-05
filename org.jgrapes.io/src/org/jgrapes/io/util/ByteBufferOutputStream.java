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
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.jgrapes.core.EventPipeline;
import org.jgrapes.io.Connection;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;

/**
 * An {@link OutputStream} that is backed by {@link ByteBuffer}s obtained
 * from a queue. When a byte buffer is full, a {@link Output} event (default) 
 * is generated and a new buffer is fetched from the queue.
 * 
 * @author Michael N. Lipp
 *
 */
public class ByteBufferOutputStream extends OutputStream {

	private Connection connection;
	private EventPipeline eventPipeline;
	private boolean inputMode;
	private ManagedByteBuffer buffer;
	
	/**
	 * Creates a new instance.
	 * 
	 * @param connection the connection to send on
	 * @param eventPipeline the event pipeline used for firing events
	 * @param inputMode if {@code true} use {@link Input} events
	 * to dispatch buffers
	 * @throws InterruptedException if the current is interrupted
	 * while trying to get a new buffer from the queue
	 */
	public ByteBufferOutputStream(Connection connection,
	        EventPipeline eventPipeline, boolean inputMode)
	        		throws InterruptedException {
		super();
		this.connection = connection;
		this.eventPipeline = eventPipeline;
		this.inputMode = inputMode;
		buffer = connection.bufferPool().acquire();
	}

	/**
	 * Creates a new instance that uses {@link Output} events to dispatch
	 * buffers.
	 * 
	 * @param connection the connection to send on
	 * @param eventPipeline the event pipeline used for firing events
	 * @throws InterruptedException if the current is interrupted
	 * while trying to get a new buffer from the queue
	 */
	public ByteBufferOutputStream(Connection connection,
	        EventPipeline eventPipeline) throws InterruptedException {
		this(connection, eventPipeline, false);
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public void write(int b) throws IOException {
		buffer.put((byte)b);
		if (!buffer.hasRemaining()) {
			flush();
		}
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	@Override
	public void write(byte[] b, int offset, int length) throws IOException {
		while (true) {
			if (buffer.remaining() > length) {
				buffer.put(b, offset, length);
				break;
			} else if (buffer.remaining() == length) {
				buffer.put(b, offset, length);
				flush();
				break;
			} else {
				int chunkSize = buffer.remaining();
				buffer.put(b, offset, chunkSize);
				flush();
				length -= chunkSize;
				offset += chunkSize;
			}
		}
	}

	/**
	 * Creates and fires a {@link Output} event with the buffer being filled and
	 * obtains a new buffer from the queue.
	 */
	@Override
	public void flush() throws IOException {
		if (inputMode) {
			buffer.flip();
			eventPipeline
			        .fire(new Input<ManagedByteBuffer>(connection, buffer));
		} else {
			eventPipeline
			        .fire(new Output<ManagedByteBuffer>(connection, buffer));
		}
		try {
			buffer = connection.bufferPool().acquire();
		} catch (InterruptedException e) {
			throw new InterruptedIOException(e.getMessage());
		}
	}

	/**
	 * Calls {@link #flush()} and fires a {@link Closed} event.
	 */
	@Override
	public void close() throws IOException {
		flush();
		eventPipeline.fire(new Closed(connection));
	}

}
