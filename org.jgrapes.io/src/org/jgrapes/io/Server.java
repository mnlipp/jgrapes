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
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.jgrapes.core.AbstractComponent;
import org.jgrapes.core.Channel;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.NamedChannel;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.Accepted;
import org.jgrapes.io.events.Eof;
import org.jgrapes.io.events.IOError;
import org.jgrapes.io.events.Read;
import org.jgrapes.io.events.Write;
import org.jgrapes.io.events.NioRegistration;
import org.jgrapes.io.events.NioRegistration.Registration;

/**
 * @author Michael N. Lipp
 *
 */
public class Server extends AbstractComponent implements NioHandler {

	public final static NamedChannel CHANNEL = new NamedChannel("server");
	
	private SocketAddress serverAddress;
	private ServerSocketChannel serverSocketChannel;
	private int bufferSize;
	
	/**
	 * 
	 */
	public Server(SocketAddress serverAddress) {
		this(CHANNEL, serverAddress, 1600);
	}

	/**
	 * 
	 */
	public Server(SocketAddress serverAddress, int bufferSize) {
		this(CHANNEL, serverAddress, bufferSize);
	}

	/**
	 * @param componentChannel
	 */
	public Server(Channel componentChannel, 
			SocketAddress serverAddress, int bufferSize) {
		super(componentChannel);
		this.serverAddress = serverAddress;
		this.bufferSize = bufferSize;
	}

	@Handler
	public void onStart(Start event) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(serverAddress);
		fire(new NioRegistration<>(this, serverSocketChannel, 
				SelectionKey.OP_ACCEPT, this), Channel.BROADCAST);
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.io.NioSelectable#handleOps(int)
	 */
	@Override
	public void handleOps(int ops) {
		if ((ops & SelectionKey.OP_ACCEPT) != 0) {
			try {
				SocketChannel socketChannel = serverSocketChannel.accept();
				new SocketConnection(socketChannel);
			} catch (IOException e) {
				fire(new IOError(null, e));
			}
		}
	}

	@Handler
	public void onRegistered(NioRegistration<SocketConnection>.Completed event) 
			throws InterruptedException, IOException {
		NioRegistration<SocketConnection> initialEvent 
			= event.getInitialEvent();
		initialEvent.getHandler().registrationComplete(initialEvent);
	}

	@Handler
	public void onWrite(Write<ByteBuffer> event) throws IOException {
		if (!(event.getConnection() instanceof SocketConnection)) {
			return;
		}
		((SocketConnection)event.getConnection()).write(event);
	}
	
	@Handler(events={Close.class, Stop.class})
	public void onClose(Close<Connection<?>> event) throws IOException {
		if (event.getConnection() == this) {
			serverSocketChannel.close();
		} else {
			((SocketConnection)event.getConnection()).close();
		}
	}

	public class SocketConnection 
		implements NioHandler, Connection<ByteBuffer> {

		private SocketChannel nioChannel;
		private EventPipeline pipeline;
		private BlockingQueue<ByteBuffer> readBuffers;
		private BlockingQueue<ByteBuffer> writeBuffers;
		private Registration registration = null;
		private Queue<ByteBuffer> pendingWrites = new ArrayDeque<>();
		private boolean pendingClose = false;
		
		/**
		 * @param nioChannel
		 */
		public SocketConnection(SocketChannel nioChannel) {
			this.nioChannel = nioChannel;
			pipeline = newEventPipeline();

			writeBuffers = new ArrayBlockingQueue<>(2);
			writeBuffers.add(ByteBuffer.allocate(bufferSize));
			writeBuffers.add(ByteBuffer.allocate(bufferSize));
			
			readBuffers = new ArrayBlockingQueue<>(2);
			readBuffers.add(ByteBuffer.allocate(bufferSize));
			readBuffers.add(ByteBuffer.allocate(bufferSize));
			
			// Register with dispatcher
			fire(new NioRegistration<SocketConnection>
					(this, nioChannel, SelectionKey.OP_READ, getChannel()),
					Channel.BROADCAST);
		}

		/* (non-Javadoc)
		 * @see org.jgrapes.io.Connection#getBuffer()
		 */
		@Override
		public ByteBuffer acquireWriteBuffer() throws InterruptedException {
			return writeBuffers.take();
		}

		/* (non-Javadoc)
		 * @see org.jgrapes.io.Connection#releaseReadBuffer(java.nio.Buffer)
		 */
		@Override
		public void releaseReadBuffer(ByteBuffer buffer) {
			readBuffers.add(buffer);
		}

		/**
		 * Invoked when registration has completed.
		 * 
		 * @param event
		 * @throws InterruptedException
		 */
		public void registrationComplete(
		        NioRegistration<SocketConnection> event)
		        throws InterruptedException {
			registration = event.get();
			pipeline.add(new Accepted<>(this), getChannel());

		}

		/* (non-Javadoc)
		 * @see org.jgrapes.io.Connection#getChannel()
		 */
		@Override
		public Channel getChannel() {
			return Server.this.getChannel();
		}

		public void write(Write<ByteBuffer> event) throws IOException {
			if (!nioChannel.isOpen()) {
				return;
			}
			ByteBuffer buffer = event.getBuffer();
			buffer.flip();
			synchronized(pendingWrites) {
				if (!pendingWrites.isEmpty()) {
					pendingWrites.add(buffer);
					return;
				}
			}
			nioChannel.write(buffer);
			if (!buffer.hasRemaining()) {
				buffer.clear();
				writeBuffers.add(buffer);
				return;
			}
			synchronized(pendingWrites) {
				pendingWrites.add(buffer);
				if (pendingWrites.size() == 1) {
					registration.updateInterested
						(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
				}
			}
		}

		@Override
		public void handleOps(int ops) {
			try {
				if ((ops & SelectionKey.OP_READ) != 0) {
					ByteBuffer buffer;
					buffer = readBuffers.take();
					int bytes = nioChannel.read(buffer);
					if (bytes == 0) {
						return;
					}
					if (bytes > 0) {
						buffer.flip();
						pipeline.add(new Read<ByteBuffer>
							(this, buffer, readBuffers), getChannel());
						return;
					}
					pipeline.add(new Eof<>(this), getChannel());
					close();
				}
				if ((ops & SelectionKey.OP_WRITE) != 0) {
					while (true) {
						ByteBuffer head = null;
						synchronized (pendingWrites) {
							if (pendingWrites.isEmpty()) {
								registration.updateInterested
									(SelectionKey.OP_READ);
								if (pendingClose) {
									close();
								}
								break;
							}
							head = pendingWrites.peek();
							if (!head.hasRemaining()) {
								head = pendingWrites.remove();
								head.clear();
								writeBuffers.add(head);
								continue;
							}
						}
						nioChannel.write(head);
					}
				}
			} catch (InterruptedException | IOException e) {
				pipeline.add(new IOError(null, e), getChannel());
			}
		}
		
		public void close() throws IOException {
			if (!nioChannel.isOpen()) {
				return;
			}
			synchronized (pendingWrites) {
				if (!pendingWrites.isEmpty()) {
					pendingClose = true;
					return;
				}
			}
			nioChannel.close();
			pipeline.add(new Closed<>(this), getChannel());
		}

	}
}
