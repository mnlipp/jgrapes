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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.jgrapes.core.AbstractComponent;
import org.jgrapes.core.Channel;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Utils;
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

/**
 * A component that reads from or writes to a file. Read events generated
 * by this component are processed by an independent event pipeline.
 * 
 * @author Michael N. Lipp
 */
public class File extends AbstractComponent implements Connection<ByteBuffer> {

	private class Context {
		public ByteBuffer buf;
		public long pos;
		public Context(ByteBuffer buf, long pos) {
			this.buf = buf;
			this.pos = pos;
		}
	}

	private EventPipeline pipeline;
	private Path path;
	private AsynchronousFileChannel ioChannel = null;
	private BlockingQueue<ByteBuffer> ioBuffers = new ArrayBlockingQueue<>(2);
	private long offset = 0;
	private CompletionHandler<Integer, Context> 
		readCompletionHandler = new ReadCompletionHandler();
	private CompletionHandler<Integer, Context> 
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
		pipeline = newEventPipeline();
		ioBuffers.add(ByteBuffer.allocateDirect(bufferSize));
		ioBuffers.add(ByteBuffer.allocateDirect(bufferSize));
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
	 * @see org.jgrapes.io.Connection#getBuffer()
	 */
	@Override
	public ByteBuffer acquireWriteBuffer() throws InterruptedException {
		return ioBuffers.take();
	}
	
	/* (non-Javadoc)
	 * @see org.jgrapes.io.Connection#releaseReadBuffer(java.nio.Buffer)
	 */
	@Override
	public void releaseReadBuffer(ByteBuffer buffer) {
		buffer.clear();
		ioBuffers.add(buffer);
	}

	public boolean isOpen() {
		return ioChannel != null && ioChannel.isOpen();
	}
	
	@Handler
	public void open(OpenFile event) throws InterruptedException {
		if (isOpen()) {
			pipeline.add(new IOError(event, 
					new IllegalStateException("File is already open.")), 
					getChannel());
		}
		path = event.getPath();
		try {
			ioChannel = AsynchronousFileChannel
					.open(event.getPath(), event.getOptions());
		} catch (IOException e) {
			pipeline.add(new IOError(event, e), getChannel());
		}
		offset = 0;
		if (Arrays.asList(event.getOptions())
				.contains(StandardOpenOption.WRITE)) {
			// Writing to file
			reading = false;
			pipeline.add(new FileOpened<>(this, event.getPath(), 
				event.getOptions()), getChannel());
		} else {
			// Reading from file
			reading = true;
			ByteBuffer buffer = ioBuffers.take();
			registerAsGenerator();
			pipeline.add(new FileOpened<>
				(this, event.getPath(), event.getOptions()), getChannel());
			ioChannel.read(buffer, offset, 
					new Context(buffer, offset), readCompletionHandler);
			synchronized (ioChannel) {
				outstandingAsyncs += 1;
			}
		}
	}

	private abstract class BaseCompletionHandler 
		implements CompletionHandler<Integer, Context> {
		
		@Override
		public void failed(Throwable exc, Context buffer) {
			try {
				if (!(exc instanceof AsynchronousCloseException)) {
					pipeline.add(new IOError(null, exc), getChannel());
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
	
	private class ReadCompletionHandler extends BaseCompletionHandler {
		
		@Override
		public void completed(Integer result, Context context) {
			try {
				if (!isOpen()) {
					return;
				}
				if (result == -1) {
					pipeline.add(new Eof<>(File.this), getChannel());
					pipeline.add(new Close<>(File.this), getChannel());
					return;
				}
				context.buf.flip();
				pipeline.add(new Read<>
					(File.this, context.buf, ioBuffers), getChannel());
				offset += result;
				try {
					ByteBuffer nextBuffer = ioBuffers.take();
					nextBuffer.clear();
					ioChannel.read(nextBuffer, offset,
					        new Context(nextBuffer, offset),
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
	public void write(Write<ByteBuffer> event) {
		ByteBuffer buffer = event.getBuffer();
		buffer.flip();
		int written = buffer.remaining();
		if (written == 0) {
			return;
		}
		synchronized (ioChannel) {
			ioChannel.write(event.getBuffer(), offset, 
					new Context(buffer,offset), writeCompletionHandler);
			outstandingAsyncs += 1;
		}
		offset += written;
	}
	
	private class WriteCompletionHandler extends BaseCompletionHandler {

		@Override
		public void completed(Integer result, Context context) {
			if (context.buf.hasRemaining()) {
				ioChannel.write(context.buf, 
						context.pos + context.buf.position(),
						context, writeCompletionHandler);
				handled();
				return;
			}
			context.buf.clear();
			ioBuffers.add(context.buf);
			handled();
		}

	}

	@Handler(events={Close.class, Stop.class})
	public void close(Close<Connection<?>> event) throws InterruptedException {
		if (isOpen()) {
			try {
				synchronized (ioChannel) {
					while (outstandingAsyncs != 0) {
						ioChannel.wait();
					}
					ioChannel.close();
				}
				pipeline.add(new Closed<>(this), getChannel());
			} catch (ClosedChannelException e) {
			} catch (IOException e) {
				pipeline.add(new IOError(event, e), getChannel());
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
		builder.append(Utils.objectName(this));
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
