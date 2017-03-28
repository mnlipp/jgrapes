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
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.core.internal.Common;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.FileOpened;
import org.jgrapes.io.events.IOError;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.events.SaveInput;
import org.jgrapes.io.events.SaveOutput;
import org.jgrapes.io.events.StreamFile;
import org.jgrapes.io.util.ManagedBufferQueue;
import org.jgrapes.io.util.ManagedByteBuffer;

/**
 * A component that reads from or writes to a file.
 * 
 * @author Michael N. Lipp
 */
public class FileStorage extends Component {

    private int bufferSize;

	private Map<Channel, Writer> inputWriters = Collections
	        .synchronizedMap(new WeakHashMap<>());
	private Map<Channel, Writer> outputWriters = Collections
	        .synchronizedMap(new WeakHashMap<>());
	
	/**
	 * Create a new instance using the given size for the read buffers.
	 * 
	 * @param channel the component's channel. Used for sending {@link Output}
	 * events and receiving {@link Input} events 
	 * @param bufferSize the size of the buffers used for reading
	 */
	public FileStorage(Channel channel, int bufferSize) {
		super(channel);
		this.bufferSize = bufferSize;
	}

	/**
	 * Create a new instance using the default buffer size of 8192.
	 * 
	 * @param channel the component's channel. Used for sending {@link Output}
	 * events and receiving {@link Input} events 
	 */
	public FileStorage(Channel channel) {
		this(channel, 8192);
	}

	/**
	 * Opens a file for reading using the properties of the event and streams
	 * its content as a sequence of {@link Output} events with the 
	 * end of record flag set in the last event. All generated events are 
	 * considered responses to this event and therefore fired using the event 
	 * processor from the event's I/O subchannel.
	 * 
	 * @param event the event
	 * @throws InterruptedException if the execution was interrupted
	 */
	@Handler
	public void onStreamFile(StreamFile event)
	        throws InterruptedException {
		if (Arrays.asList(event.options())
		        .contains(StandardOpenOption.WRITE)) {
			throw new IllegalArgumentException(
			        "Cannot stream file opened for writing.");
		}
		for (IOSubchannel channel : event.channels(IOSubchannel.class)) {
			if (inputWriters.containsKey(channel)) {
				channel.fire(new IOError(event,
				        new IllegalStateException("File is already open.")));
			} else {
				new FileStreamer(event, channel);
			}
		}
	}

	private class FileStreamer {

		private final IOSubchannel channel;
		private Path path;
		private AsynchronousFileChannel ioChannel = null;
		private ManagedBufferQueue<ManagedByteBuffer, ByteBuffer> ioBuffers;
		private long offset = 0;
		private CompletionHandler<Integer, ManagedByteBuffer> 
			readCompletionHandler = new ReadCompletionHandler();

		private FileStreamer(StreamFile event, IOSubchannel channel)
		        throws InterruptedException {
			this.channel = channel;
			path = event.path();
			offset = 0;
			try {
				ioChannel = AsynchronousFileChannel
				        .open(event.path(), event.options());
			} catch (IOException e) {
				channel.fire(new IOError(event, e));
				return;
			}
			channel.fire(new FileOpened(event.path(), event.options()));
			// Reading from file
			ioBuffers = new ManagedBufferQueue<>(ManagedByteBuffer.class,
					ByteBuffer.allocateDirect(bufferSize),
					ByteBuffer.allocateDirect(bufferSize));
			ManagedByteBuffer buffer = ioBuffers.acquire();
			registerAsGenerator();
			synchronized (ioChannel) {
				ioChannel.read(buffer.backingBuffer(), offset, buffer,
						readCompletionHandler);
			}
		}

		private class ReadCompletionHandler
		        implements CompletionHandler<Integer, ManagedByteBuffer> {

			@Override
			public void completed(Integer result, ManagedByteBuffer buffer) {
				if (result >= 0) {
					offset += result;
					boolean eof = true;
					try {
						eof = (offset == ioChannel.size());
					} catch (IOException e1) {
						// Handled like true
					} 
					channel.fire(new Output<>(buffer, eof));
					if (!eof) {
						try {
							ManagedByteBuffer nextBuffer = ioBuffers.acquire();
							nextBuffer.clear();
							synchronized (ioChannel) {
								ioChannel.read(nextBuffer.backingBuffer(), offset,
							        nextBuffer, readCompletionHandler);
							}
						} catch (InterruptedException e) {
							// Results in empty buffer
						}
						return;
					}
				}
				try {
					ioChannel.close();
					channel.fire(new Closed());
				} catch (ClosedChannelException e) {
					// Can be ignored
				} catch (IOException e) {
					channel.fire(new IOError(null, e));
				}
				unregisterAsGenerator();
			}
			
			@Override
			public void failed(Throwable exc, ManagedByteBuffer context) {
				if (!(exc instanceof AsynchronousCloseException)) {
					channel.fire(new IOError(null, exc));
				}
				channel.fire(new Closed());
				unregisterAsGenerator();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("FileStreamer [");
			if (channel != null) {
				builder.append("channel=");
				builder.append(Common.channelToString(channel));
				builder.append(", ");
			}
			if (path != null) {
				builder.append("path=");
				builder.append(path);
				builder.append(", ");
			}
			builder.append("offset=");
			builder.append(offset);
			builder.append("]");
			return builder.toString();
		}

	}

	/**
	 * Opens a file for writing using the properties of the event. All data from
	 * subsequent {@link Input} events is written to the file.
	 * The end of record flag is ignored.
	 * 
	 * @param event the event
	 * @throws InterruptedException if the execution was interrupted
	 */
	@Handler
	public void onSaveInput(SaveInput event) throws InterruptedException {
		if (!Arrays.asList(event.options())
		        .contains(StandardOpenOption.WRITE)) {
			throw new IllegalArgumentException(
			        "File must be opened for writing.");
		}
		for (IOSubchannel channel : event.channels(IOSubchannel.class)) {
			if (inputWriters.containsKey(channel)) {
				channel.fire(new IOError(event,
				        new IllegalStateException("File is already open.")));
			} else {
				inputWriters.put(channel, new Writer(event, channel));
			}
		}
	}
	
	@Handler
	public void onInput(Input<ManagedByteBuffer> event) {
		for (Channel channel: event.channels()) {
			Writer writer = inputWriters.get(channel);
			if (writer != null) {
				writer.write(event.buffer());
			}
		}
	}
	
	/**
	 * Opens a file for writing using the properties of the event. All data from
	 * subsequent {@link Output} events is written to the file. 
	 * The end of record flag is ignored.
	 * 
	 * @param event the event
	 * @throws InterruptedException if the execution was interrupted
	 */
	@Handler
	public void onSaveOutput(SaveOutput event) throws InterruptedException {
		if (!Arrays.asList(event.options())
		        .contains(StandardOpenOption.WRITE)) {
			throw new IllegalArgumentException(
			        "File must be opened for writing.");
		}
		for (IOSubchannel channel : event.channels(IOSubchannel.class)) {
			if (outputWriters.containsKey(channel)) {
				channel.fire(new IOError(event,
				        new IllegalStateException("File is already open.")));
			} else {
				outputWriters.put(channel, new Writer(event, channel));
			}
		}
	}
	
	@Handler
	public void onOutput(Output<ManagedByteBuffer> event) {
		for (Channel channel: event.channels()) {
			Writer writer = outputWriters.get(channel);
			if (writer != null) {
				writer.write(event.buffer());
			}
		}
	}

	@Handler
	public void onClosed(Closed event) throws InterruptedException {
		for (Channel channel: event.channels()) {
			Writer writer = inputWriters.get(channel);
			if (writer != null) {
				writer.close(event);
			}
		}
		for (Channel channel: event.channels()) {
			Writer writer = outputWriters.get(channel);
			if (writer != null) {
				writer.close(event);
			}
		}
	}
	
	@Handler
	public void onStop(Stop event) throws InterruptedException {
		while (inputWriters.size() > 0) {
			Writer handler = inputWriters.entrySet().iterator().next()
			        .getValue();
			handler.close(event);
		}
		while (outputWriters.size() > 0) {
			Writer handler = outputWriters.entrySet().iterator().next()
			        .getValue();
			handler.close(event);
		}
	}

	private class Writer {

		/**
		 * The write context needs to be finer grained than the general file
		 * connection context because an asynchronous write may be only
		 * partially successful, i.e. not all data provided by the write event
		 * may successfully be written in one asynchronous write invocation.
		 */
		private class WriteContext {
			public ManagedByteBuffer buffer;
			public long pos;

			public WriteContext(ManagedByteBuffer buffer, long pos) {
				this.buffer = buffer;
				this.pos = pos;
			}
		}

		private final IOSubchannel channel;
		private Path path;
		private AsynchronousFileChannel ioChannel = null;
		private long offset = 0;
		private CompletionHandler<Integer, WriteContext> 
			writeCompletionHandler = new WriteCompletionHandler();
		private int outstandingAsyncs = 0;

		public Writer(SaveInput event, IOSubchannel channel)
		        throws InterruptedException {
			this(event, event.path(), event.options(), channel);
		}

		public Writer(SaveOutput event, IOSubchannel channel)
		        throws InterruptedException {
			this(event, event.path(), event.options(), channel);
		}

		private Writer(Event<?> event, Path path, OpenOption[] options,
		        IOSubchannel channel) throws InterruptedException {
			this.channel = channel;
			this.path = path;
			offset = 0;
			try {
				ioChannel = AsynchronousFileChannel.open(path, options);
			} catch (IOException e) {
				channel.fire(new IOError(event, e));
				return;
			}
			channel.fire(new FileOpened(path, options));
		}

		public void write(ManagedByteBuffer buffer) {
			int written = buffer.remaining();
			if (written == 0) {
				return;
			}
			buffer.lockBuffer();
			synchronized (ioChannel) {
				if (outstandingAsyncs == 0) {
					registerAsGenerator();
				}
				outstandingAsyncs += 1;
				ioChannel.write(buffer.backingBuffer(), offset,
				        new WriteContext(buffer, offset),
				        writeCompletionHandler);
			}
			offset += written;
		}

		private class WriteCompletionHandler
		        implements CompletionHandler<Integer, WriteContext> {

			@Override
			public void completed(Integer result, WriteContext context) {
				ManagedByteBuffer buffer = context.buffer;
				if (buffer.hasRemaining()) {
					ioChannel.write(buffer.backingBuffer(),
					        context.pos + buffer.position(),
					        context, writeCompletionHandler);
					return;
				}
				buffer.unlockBuffer();
				handled();
			}

			@Override
			public void failed(Throwable exc, WriteContext context) {
				try {
					if (!(exc instanceof AsynchronousCloseException)) {
						channel.fire(new IOError(null, exc));
					}
				} finally {
					handled();
				}
			}
			
			private void handled() {
				synchronized (ioChannel) {
					if (--outstandingAsyncs == 0) {
						unregisterAsGenerator();
						ioChannel.notifyAll();
					}
				}
			}
		}

		public void close(Event<?> event)
				throws InterruptedException {
			try {
				synchronized (ioChannel) {
					while (outstandingAsyncs > 0) {
						ioChannel.wait();
					}
					ioChannel.close();
				}
			} catch (ClosedChannelException e) {
				// Can be ignored
			} catch (IOException e) {
				channel.fire(new IOError(event, e));
			}
			inputWriters.remove(channel);
			outputWriters.remove(channel);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("FileConnection [");
			if (channel != null) {
				builder.append("channel=");
				builder.append(Common.channelToString(channel));
				builder.append(", ");
			}
			if (path != null) {
				builder.append("path=");
				builder.append(path);
				builder.append(", ");
			}
			builder.append("offset=");
			builder.append(offset);
			builder.append("]");
			return builder.toString();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Components.objectName(this));
		builder.append(" [");
		if (inputWriters != null) {
			builder.append(inputWriters.values().stream()
			        .map(c -> Components.objectName(c))
			        .collect(Collectors.toList()));
		}
		builder.append("]");
		return builder.toString();
	}
}
