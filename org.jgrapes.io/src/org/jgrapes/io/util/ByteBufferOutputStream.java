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
import org.jgrapes.io.DataConnection;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Write;

/**
 * An {@link OutputStream} that is backed by {@link ByteBuffer}s obtained
 * from a queue. When a byte buffer is full, a {@link Write} event is
 * generated and a new buffer is fetched from the queue.
 * 
 * @author Michael N. Lipp
 *
 */
public class ByteBufferOutputStream extends OutputStream {

	private DataConnection connection;
	private EventPipeline pipeline;
	private ManagedByteBuffer buffer;
	
	/**
	 * Creates a new instance.
	 * 
	 * @param connection the connection to send on
	 * @param pipeline the pipeline to be used to send the events
	 * @throws InterruptedException if the current is interrupted
	 * while trying to get a new buffer from the queue
	 */
	public ByteBufferOutputStream (DataConnection connection,
			EventPipeline pipeline)	throws InterruptedException {
		super();
		this.connection = connection;
		this.pipeline = pipeline;
		buffer = connection.acquireByteBuffer();
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
	 * Creates and fires a {@link Write} event with the buffer being filled and
	 * obtains a new buffer from the queue.
	 */
	@Override
	public void flush() throws IOException {
		pipeline.add(new Write<ManagedByteBuffer>(connection, buffer), 
				connection.getChannel());
		try {
			buffer = connection.acquireByteBuffer();
		} catch (InterruptedException e) {
			throw new InterruptedIOException(e.getMessage());
		}
	}

	/**
	 * calls {@link #flush()} and fires a {@link Close} event.
	 */
	@Override
	public void close() throws IOException {
		flush();
		pipeline.add(new Close<>(connection), connection.getChannel());
	}

}
