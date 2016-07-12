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
package org.jgrapes.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import org.jgrapes.core.AbstractComponent;
import org.jgrapes.core.Channel;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.Eof;
import org.jgrapes.io.events.FileOpened;
import org.jgrapes.io.events.IOError;
import org.jgrapes.io.events.OpenFile;
import org.jgrapes.io.events.Read;
import org.jgrapes.io.events.Write;
import org.jgrapes.io.util.ManagedBufferQueue;
import org.jgrapes.io.util.ManagedByteBuffer;

/**
 * A component that reads from or writes to a file. Read events generated
 * by this component are processed by an independent event pipeline.
 * 
 * @author Michael N. Lipp
 */
public class File extends AbstractComponent implements DataConnection {

	private class WriteContext {
		public ManagedByteBuffer buffer;
		public long pos;
		public WriteContext(ManagedByteBuffer buffer, long pos) {
			this.buffer = buffer;
			this.pos = pos;
		}
	}

	private EventPipeline downPipeline;
	private EventPipeline upPipeline;
	private Path path;
	private AsynchronousFileChannel ioChannel = null;
	private ManagedBufferQueue<ManagedByteBuffer,ByteBuffer> ioBuffers;
	private long offset = 0;
	private CompletionHandler<Integer, ManagedByteBuffer> 
		readCompletionHandler = new ReadCompletionHandler();
	private CompletionHandler<Integer, WriteContext> 
		writeCompletionHandler = new WriteCompletionHandler();
	private int outstandingAsyncs = 0;
	private boolean reading = false;
	
	/**
	 * Create a new instance using the given size for the read buffers.
	 * 
	 * @param channel the component's channel. Used for sending {@link Read}
	 * events and receiving {@link Write} events 
	 * @param bufferSize the size of the buffers used for reading
	 */
	public File(Channel channel, int bufferSize) {
		super (channel);
		downPipeline = newEventPipeline();
		upPipeline = newEventPipeline();
		ioBuffers = new ManagedBufferQueue<>(ManagedByteBuffer.class,
			ByteBuffer.allocateDirect(bufferSize), 
			ByteBuffer.allocateDirect(bufferSize));
	}

	/**
	 * Create a new instance using the default buffer size of 4096.
	 * 
	 * @param channel the component's channel. Used for sending {@link Read}
	 * events and receiving {@link Write} events 
	 */
	public File(Channel channel) {
		this(channel, 4096);
	}
	
	/* (non-Javadoc)
	 * @see org.jgrapes.io.DataConnection#getByteBuffer()
	 */
	@Override
	public ManagedByteBuffer acquireByteBuffer() throws InterruptedException {
		return ioBuffers.acquire();
	}
	
	/* (non-Javadoc)
	 * @see org.jgrapes.io.DataConnection#getPipeline()
	 */
	@Override
	public EventPipeline getResponsePipeline() {
		return upPipeline;
	}

	public boolean isOpen() {
		return ioChannel != null && ioChannel.isOpen();
	}
	
	@Handler
	public void open(OpenFile event) throws InterruptedException {
		if (isOpen()) {
			downPipeline.add(new IOError(event, 
					new IllegalStateException("File is already open.")), 
					getChannel());
		}
		path = event.getPath();
		try {
			ioChannel = AsynchronousFileChannel
					.open(event.getPath(), event.getOptions());
		} catch (IOException e) {
			downPipeline.add(new IOError(event, e), getChannel());
		}
		offset = 0;
		if (Arrays.asList(event.getOptions())
				.contains(StandardOpenOption.WRITE)) {
			// Writing to file
			reading = false;
			downPipeline.add(new FileOpened(this, event.getPath(), 
				event.getOptions()), getChannel());
		} else {
			// Reading from file
			reading = true;
			ManagedByteBuffer buffer = ioBuffers.acquire();
			registerAsGenerator();
			downPipeline.add(new FileOpened
				(this, event.getPath(), event.getOptions()), getChannel());
			ioChannel.read
				(buffer.getBuffer(), offset, buffer, readCompletionHandler);
			synchronized (ioChannel) {
				outstandingAsyncs += 1;
			}
		}
	}

	private abstract class BaseCompletionHandler<C> 
		implements CompletionHandler<Integer, C> {
		
		@Override
		public void failed(Throwable exc, C context) {
			try {
				if (!(exc instanceof AsynchronousCloseException)) {
					downPipeline.add(new IOError(null, exc), getChannel());
				}
			} finally {
				handled();
			}
		}
		
		protected void handled() {
			synchronized (ioChannel) {
				if (--outstandingAsyncs == 0) {
					ioChannel.notifyAll();
				}
			}
		}
	}
	
	private class ReadCompletionHandler 
		extends BaseCompletionHandler<ManagedByteBuffer> {
		
		@Override
		public void completed(Integer result, ManagedByteBuffer buffer) {
			try {
				if (!isOpen()) {
					return;
				}
				if (result == -1) {
					downPipeline.add(new Eof(File.this), getChannel());
					downPipeline.add(new Close<>(File.this), getChannel());
					return;
				}
				buffer.flip();
				downPipeline.add(new Read<>(File.this, buffer), getChannel());
				offset += result;
				try {
					ManagedByteBuffer nextBuffer = ioBuffers.acquire();
					nextBuffer.clear();
					ioChannel.read(nextBuffer.getBuffer(), offset, nextBuffer,
					        readCompletionHandler);
					synchronized (ioChannel) {
						outstandingAsyncs += 1;
					}
				} catch (InterruptedException e) {
				}
			} finally {
				handled();
			}
		}

	}
	
	@Handler
	public void onWrite(Write<ManagedByteBuffer> event) {
		ManagedByteBuffer buffer = event.getBuffer();
		int written = buffer.remaining();
		if (written == 0) {
			return;
		}
		buffer.lockBuffer();
		synchronized (ioChannel) {
			ioChannel.write(buffer.getBuffer(), offset, 
					new WriteContext(buffer, offset), writeCompletionHandler);
			outstandingAsyncs += 1;
		}
		offset += written;
	}
	
	private class WriteCompletionHandler 
		extends BaseCompletionHandler<WriteContext> {

		@Override
		public void completed(Integer result, WriteContext context) {
			ManagedByteBuffer buffer = context.buffer;
			if (buffer.hasRemaining()) {
				ioChannel.write(buffer.getBuffer(), 
						context.pos + buffer.position(),
						context, writeCompletionHandler);
				return;
			}
			buffer.unlockBuffer();
			handled();
		}

	}

	@Handler(events={Close.class, Stop.class})
	public void close(Close<DataConnection> event) throws InterruptedException {
		if (isOpen()) {
			try {
				synchronized (ioChannel) {
					while (outstandingAsyncs != 0) {
						ioChannel.wait();
					}
					ioChannel.close();
				}
				downPipeline.add(new Closed<>(this), getChannel());
			} catch (ClosedChannelException e) {
			} catch (IOException e) {
				downPipeline.add(new IOError(event, e), getChannel());
			}
			if (reading) {
				unregisterAsGenerator();
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Components.objectName(this));
		builder.append(" [");
		if (isOpen() && path != null) {
			builder.append("path=");
			builder.append(path);
		} else {
			builder.append("(closed)");
		}
		builder.append("]");
		return builder.toString();
	}
	
}
