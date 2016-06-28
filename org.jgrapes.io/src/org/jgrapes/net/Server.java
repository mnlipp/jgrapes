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
package org.jgrapes.net;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
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
import org.jgrapes.core.Self;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import org.jgrapes.core.events.Stop;
import org.jgrapes.core.events.Error;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.Connection;
import org.jgrapes.io.DataConnection;
import org.jgrapes.io.NioHandler;
import org.jgrapes.io.events.Eof;
import org.jgrapes.io.events.IOError;
import org.jgrapes.io.events.Read;
import org.jgrapes.io.events.Write;
import org.jgrapes.io.events.NioRegistration;
import org.jgrapes.io.events.NioRegistration.Registration;
import org.jgrapes.net.events.Accepted;
import org.jgrapes.net.events.Ready;

/**
 * Provides a TCP server. The server binds to the given address. If the
 * address is {@code null}, address and port are automatically assigned.
 * 
 * @author Michael N. Lipp
 *
 */
public class Server extends AbstractComponent 
	implements NioHandler, Connection {

	public final static NamedChannel 
		DEFAULT_CHANNEL = new NamedChannel("server");
	
	private SocketAddress serverAddress;
	private ServerSocketChannel serverSocketChannel;
	private int bufferSize;
	
	/**
	 * Creates a new server listening on the given address. 
	 * The channel is set to the {@link NamedChannel} "server". The size of
	 * the send and receive buffers is set to the platform defaults. 
	 * 
	 * @param serverAddress the address to bind to
	 */
	public Server(SocketAddress serverAddress) {
		this(DEFAULT_CHANNEL, serverAddress);
	}

	/**
	 * Creates a new server using the given channel and address.
	 * 
	 * @param componentChannel the component's channel
	 * @param serverAddress the address to bind to
	 */
	public Server(Channel componentChannel,	SocketAddress serverAddress) {
		this(componentChannel, serverAddress, 0);
	}

	/**
	 * Creates a new server listening on the given address.
	 * The channel is set to the {@link NamedChannel} "server". 
	 * 
	 * @param serverAddress the address to bind to
	 * @param bufferSize the size to use for the send and receive buffers
	 */
	public Server(SocketAddress serverAddress, int bufferSize) {
		this(DEFAULT_CHANNEL, serverAddress, bufferSize);
	}

	/**
	 * Creates a new server using the given channel and address.
	 * 
	 * @param componentChannel the component's channel
	 * @param serverAddress the address to bind to
	 * @param bufferSize the size to use for the send and receive buffers
	 */
	public Server(Channel componentChannel, 
			SocketAddress serverAddress, int bufferSize) {
		super(componentChannel);
		this.serverAddress = serverAddress;
		this.bufferSize = bufferSize;
	}

	/**
	 * Starts the server.
	 * 
	 * @param event
	 * @throws IOException
	 */
	@Handler
	public void onStart(Start event) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(serverAddress);
		fire(new NioRegistration(this, serverSocketChannel, 
				SelectionKey.OP_ACCEPT, this), Channel.BROADCAST);
	}

	@Handler(channels=Self.class)
	public void onRegistered(NioRegistration.Completed event) 
			throws InterruptedException, IOException {
		NioHandler handler = event.getCompleted().getHandler(); 
		if (handler == this) {
			if (event.getCompleted().get() == null) {
				fire(new Error(event, 
						"Registration failed, no NioDispatcher?"));
				return;
			}
			fire(new Ready(this, serverSocketChannel.getLocalAddress()));
			return;
		}
		if (handler instanceof SocketConnection) {
			((SocketConnection)handler)
				.registrationComplete(event.getCompleted());
		}
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

	/**
	 * Writes the data passed in the event to the client. 
	 * 
	 * @param event the event
	 * @throws IOException if an error occurs
	 */
	@Handler
	public void onWrite(Write<ByteBuffer> event) throws IOException {
		if (!(event.getConnection() instanceof SocketConnection)) {
			return;
		}
		((SocketConnection)event.getConnection()).write(event);
	}

	/**
	 * Shuts down the server or one of the connections to the server
	 * 
	 * @param event the event
	 * @throws IOException if an error occurs
	 */
	@Handler
	public void onClose(Close<? extends Connection> event) throws IOException {
		if (event.getConnection() == this) {
			if (!serverSocketChannel.isOpen()) {
				return;
			}
			serverSocketChannel.close();
			fire(new Closed<>(this));
		} else if (event.getConnection() instanceof SocketConnection) {
			((SocketConnection)event.getConnection()).close();
		}
	}

	/**
	 * Shuts down the server.
	 * 
	 * @param event the event
	 */
	@Handler
	public void onStop(Stop event) {
		if (!serverSocketChannel.isOpen()) {
			return;
		}
		newSyncEventPipeline().add(new Close<>(this), getChannel());
	}

	/**
	 * The internal representation of a connected client. 
	 * 
	 * @author Michael N. Lipp
	 *
	 */
	public class SocketConnection 
		implements NioHandler, DataConnection<ByteBuffer> {

		private SocketChannel nioChannel;
		private EventPipeline pipeline;
		private BlockingQueue<ByteBuffer> readBuffers;
		private BlockingQueue<ByteBuffer> writeBuffers;
		private Registration registration = null;
		private Queue<Write<ByteBuffer>> pendingWrites = new ArrayDeque<>();
		private boolean pendingClose = false;
		
		/**
		 * @param nioChannel
		 * @throws SocketException 
		 */
		public SocketConnection(SocketChannel nioChannel)
				throws SocketException {
			this.nioChannel = nioChannel;
			pipeline = newEventPipeline();

			int writeBufferSize = bufferSize == 0 
					? nioChannel.socket().getSendBufferSize() : bufferSize;
			writeBuffers = new ArrayBlockingQueue<>(2);
			writeBuffers.add(ByteBuffer.allocate(writeBufferSize));
			writeBuffers.add(ByteBuffer.allocate(writeBufferSize));
			
			int readBufferSize = bufferSize == 0 
					? nioChannel.socket().getReceiveBufferSize() : bufferSize;
			readBuffers = new ArrayBlockingQueue<>(2);
			readBuffers.add(ByteBuffer.allocate(readBufferSize));
			readBuffers.add(ByteBuffer.allocate(readBufferSize));
			
			// Register with dispatcher
			fire(new NioRegistration (this, nioChannel, 0, Server.this),
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
		 * @see org.jgrapes.io.DataConnection#releaseWriteBuffer(java.nio.Buffer)
		 */
		@Override
		public void releaseWriteBuffer(ByteBuffer buffer) {
			buffer.clear();
			writeBuffers.add(buffer);
		}

		/* (non-Javadoc)
		 * @see org.jgrapes.io.Connection#releaseReadBuffer(java.nio.Buffer)
		 */
		@Override
		public void releaseReadBuffer(ByteBuffer buffer) {
			buffer.clear();
			readBuffers.add(buffer);
		}

		/**
		 * Invoked when registration has completed.
		 * 
		 * @param event the completed event
		 * @throws InterruptedException
		 * @throws IOException 
		 */
		public void registrationComplete(NioRegistration event)
		        throws InterruptedException, IOException {
			registration = event.get();
			pipeline.add(new Accepted<>(this, nioChannel.getLocalAddress(),
					nioChannel.getRemoteAddress()), getChannel());
			registration.updateInterested(SelectionKey.OP_READ);

		}

		/* (non-Javadoc)
		 * @see org.jgrapes.io.Connection#getChannel()
		 */
		@Override
		public Channel getChannel() {
			return Server.this.getChannel();
		}

		/**
		 * Write the data on this connection.
		 * 
		 * @param event the event
		 * @throws IOException if an error occurs
		 */
		public void write(Write<ByteBuffer> event) throws IOException {
			ByteBuffer buffer = event.getBuffer();
			if (!nioChannel.isOpen()) {
				return;
			}
			synchronized(pendingWrites) {
				if (!pendingWrites.isEmpty()) {
					event.lockBuffer();
					pendingWrites.add(event);
					return;
				}
			}
			nioChannel.write(buffer);
			if (!buffer.hasRemaining()) {
				buffer.clear();
				return;
			}
			synchronized(pendingWrites) {
				event.lockBuffer();
				pendingWrites.add(event);
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
					handleReadOp();
				}
				if ((ops & SelectionKey.OP_WRITE) != 0) {
					handleWriteOp();
				}
			} catch (InterruptedException | IOException e) {
				pipeline.add(new IOError(null, e), getChannel());
			}
		}

		/**
		 * Gets a buffer from the pool and reads available data into it.
		 * Sends the result as event. 
		 * 
		 * @throws InterruptedException
		 * @throws IOException
		 */
		private void handleReadOp() throws InterruptedException, IOException {
			ByteBuffer buffer;
			buffer = readBuffers.take();
			int bytes = nioChannel.read(buffer);
			if (bytes == 0) {
				buffer.clear();
				readBuffers.add(buffer);
				return;
			}
			if (bytes > 0) {
				buffer.flip();
				pipeline.add(new Read<ByteBuffer>
					(this, buffer, readBuffers), getChannel());
				return;
			}
			pipeline.add(new Eof(this), getChannel());
			close();
		}
		
		/**
		 * Checks if there is still data to be written. This may be
		 * a left over in an incompletely written buffer or a complete
		 * pending buffer. 
		 * 
		 * @throws IOException
		 */
		private void handleWriteOp() throws IOException {
			while (true) {
				Write<ByteBuffer> head = null;
				synchronized (pendingWrites) {
					if (pendingWrites.isEmpty()) {
						// Nothing left to write, stop getting ops
						registration.updateInterested
							(SelectionKey.OP_READ);
						// Stream closed while we were writing?
						if (pendingClose) {
							close();
						}
						break; // Nothing left to do
					}
					head = pendingWrites.peek();
					if (!head.getBuffer().hasRemaining()) {
						// Nothing left in head buffer, try next
						head.getBuffer().clear();
						head.unlockBuffer();
						pendingWrites.remove();
						continue;
					}
				}
				nioChannel.write(head.getBuffer()); // write...
				break; // ... and wait for next op
			}
		}

		/**
		 * Closes this connection.
		 * 
		 * @throws IOException if an error occurs
		 */
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
