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
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.NamedChannel;
import org.jgrapes.core.Self;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Error;
import org.jgrapes.core.events.Start;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.NioHandler;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.IOError;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.NioRegistration;
import org.jgrapes.io.events.NioRegistration.Registration;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.ManagedBufferQueue;
import org.jgrapes.io.util.ManagedByteBuffer;
import org.jgrapes.net.events.Accepted;
import org.jgrapes.net.events.Ready;

/**
 * Provides a TCP server. The server binds to the given address. If the
 * address is {@code null}, address and port are automatically assigned.
 * <P>
 * The end of record flag is not used by the server.
 * 
 * @author Michael N. Lipp
 */
public class TcpServer extends Component implements NioHandler {

	public static final NamedChannel 
		DEFAULT_CHANNEL = new NamedChannel("server");
	
	private SocketAddress serverAddress;
	private ServerSocketChannel serverSocketChannel;
	private int bufferSize;
	private Set<Connection> connections = new HashSet<>();
	private boolean closing = false;

	/**
	 * Creates a new server listening on the given address. 
	 * The channel is set to the {@link NamedChannel} "server". The size of
	 * the send and receive buffers is set to the platform defaults. 
	 * 
	 * @param serverAddress the address to bind to
	 */
	public TcpServer(SocketAddress serverAddress) {
		this(DEFAULT_CHANNEL, serverAddress);
	}

	/**
	 * Creates a new server using the given channel and address.
	 * 
	 * @param componentChannel the component's channel
	 * @param serverAddress the address to bind to
	 */
	public TcpServer(Channel componentChannel,	SocketAddress serverAddress) {
		this(componentChannel, serverAddress, 0);
	}

	/**
	 * Creates a new server listening on the given address.
	 * The channel is set to the {@link NamedChannel} "server". 
	 * 
	 * @param serverAddress the address to bind to
	 * @param bufferSize the size to use for the send and receive buffers
	 */
	public TcpServer(SocketAddress serverAddress, int bufferSize) {
		this(DEFAULT_CHANNEL, serverAddress, bufferSize);
	}

	/**
	 * Creates a new server using the given channel and address.
	 * 
	 * @param componentChannel the component's channel
	 * @param serverAddress the address to bind to
	 * @param bufferSize the size to use for the send and receive buffers
	 */
	public TcpServer(Channel componentChannel, 
			SocketAddress serverAddress, int bufferSize) {
		super(componentChannel);
		this.serverAddress = serverAddress;
		this.bufferSize = bufferSize;
	}

	/**
	 * Starts the server.
	 * 
	 * @param event the start event
	 * @throws IOException if an I/O exception occurred
	 */
	@Handler
	public void onStart(Start event) throws IOException {
		closing = false;
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(serverAddress);
		fire(new NioRegistration(this, serverSocketChannel, 
				SelectionKey.OP_ACCEPT, this), Channel.BROADCAST);
	}

	@Handler(channels=Self.class)
	public void onRegistered(NioRegistration.Completed event) 
			throws InterruptedException, IOException {
		NioHandler handler = event.event().handler(); 
		if (handler == this) {
			if (event.event().get() == null) {
				fire(new Error(event, 
						"Registration failed, no NioDispatcher?"));
				return;
			}
			fire(new Ready(serverSocketChannel.getLocalAddress()));
			return;
		}
		if (handler instanceof Connection) {
			((Connection)handler)
				.registrationComplete(event.event());
		}
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.io.NioSelectable#handleOps(int)
	 */
	@Override
	public void handleOps(int ops) {
		synchronized (connections) {
			if ((ops & SelectionKey.OP_ACCEPT) != 0 && !closing) {
				try {
					SocketChannel socketChannel = serverSocketChannel.accept();
					connections.add(new Connection(socketChannel));
				} catch (IOException e) {
					fire(new IOError(null, e));
				}
			}
		}
	}

	/**
	 * Writes the data passed in the event to the client. The end of record
	 * flag is ignored.
	 * 
	 * @param event the event
	 * @throws IOException if an error occurs
	 */
	@Handler
	public void onOutput(Output<ManagedByteBuffer> event) throws IOException {
		for (Connection connection: event.channels(Connection.class)) {
			if (connections.contains(connection)) {
				connection.write(event);
			}
		}
	}

	/**
	 * Shuts down the server or one of the connections to the server
	 * 
	 * @param event the event
	 * @throws IOException if an I/O exception occurred
	 * @throws InterruptedException if the execution was interrupted 
	 */
	@Handler
	public void onClose(Close event) throws IOException, InterruptedException {
		boolean subOnly = true;
		for (Channel channel: event.channels()) {
			if (channel instanceof Connection) {
				if (connections.contains(channel)) {
					((Connection)channel).close();
				}
			} else {
				subOnly = false;
			}
		}
		if (subOnly || !serverSocketChannel.isOpen()) {
			return;
		}
		synchronized (connections) {
			closing = true;
			// Copy to avoid concurrent modification exception
			Set<Connection> conns = new HashSet<>(connections);
			for (Connection conn : conns) {
				conn.close();
			}
			while (connections.size() > 0) {
				connections.wait();
			}
		}
		serverSocketChannel.close();
		closing = false;
		fire(new Closed());
	}

	/**
	 * Shuts down the server.
	 * 
	 * @param event the event
	 */
	@Handler
	public void onStop(Stop event) {
		if (closing || !serverSocketChannel.isOpen()) {
			return;
		}
		newSyncEventPipeline().fire(new Close(), channel());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return Components.objectName(this);
	}
	
	/**
	 * The internal representation of a connected client. 
	 * 
	 * @author Michael N. Lipp
	 *
	 */
	public class Connection implements NioHandler, IOSubchannel {

		private SocketChannel nioChannel;
		private EventPipeline downPipeline;
		private EventPipeline upPipeline;
		private ManagedBufferQueue<ManagedByteBuffer, ByteBuffer> readBuffers;
		private ManagedBufferQueue<ManagedByteBuffer, ByteBuffer> writeBuffers;
		private Registration registration = null;
		private Queue<ManagedByteBuffer.Reader> pendingWrites = new ArrayDeque<>();
		private boolean pendingClose = false;
		
		/**
		 * @param nioChannel the channel
		 * @throws SocketException if an error occurred
		 */
		public Connection(SocketChannel nioChannel)	throws SocketException {
			this.nioChannel = nioChannel;
			downPipeline = newEventPipeline();
			upPipeline = newEventPipeline();

			int writeBufferSize = bufferSize == 0 
					? nioChannel.socket().getSendBufferSize() : bufferSize;
			writeBuffers = new ManagedBufferQueue<>(ManagedByteBuffer.class, 
					ByteBuffer.allocate(writeBufferSize),
					ByteBuffer.allocate(writeBufferSize));
			
			int readBufferSize = bufferSize == 0 
					? nioChannel.socket().getReceiveBufferSize() : bufferSize;
			readBuffers = new ManagedBufferQueue<>(ManagedByteBuffer.class,
					ByteBuffer.allocate(readBufferSize),
					ByteBuffer.allocate(readBufferSize));
			
			// Register with dispatcher
			TcpServer.this.fire(
			        new NioRegistration(this, nioChannel, 0, TcpServer.this),
			        Channel.BROADCAST);
		}

		/* (non-Javadoc)
		 * @see org.jgrapes.io.IOSubchannel#getMainChannel()
		 */
		@Override
		public Channel mainChannel() {
			return TcpServer.this.channel();
		}

		/* (non-Javadoc)
		 * @see org.jgrapes.io.Connection#bufferPool()
		 */
		@Override
		public ManagedBufferQueue<ManagedByteBuffer, ByteBuffer> bufferPool() {
			return writeBuffers;
		}

		/* (non-Javadoc)
		 * @see org.jgrapes.io.DataConnection#getPipeline()
		 */
		@Override
		public EventPipeline responsePipeline() {
			return upPipeline;
		}

		/**
		 * Invoked when registration has completed.
		 * 
		 * @param event the completed event
		 * @throws InterruptedException if the execution was interrupted
		 * @throws IOException if an I/O error occurred
		 */
		public void registrationComplete(NioRegistration event)
		        throws InterruptedException, IOException {
			registration = event.get();
			downPipeline.fire(new Accepted(nioChannel.getLocalAddress(),
					nioChannel.getRemoteAddress()), this);
			registration.updateInterested(SelectionKey.OP_READ);

		}

		/**
		 * Write the data on this connection.
		 * 
		 * @param event the event
		 * @throws IOException if an error occurred
		 */
		public void write(Output<ManagedByteBuffer> event) throws IOException {
			if (!nioChannel.isOpen()) {
				return;
			}
			ManagedByteBuffer.Reader reader = event.buffer().newReader();
			synchronized(pendingWrites) {
				if (!pendingWrites.isEmpty()) {
					reader.managedBuffer().lockBuffer();
					pendingWrites.add(reader);
					return;
				}
			}
			nioChannel.write(reader.get());
			if (!reader.get().hasRemaining()) {
				return;
			}
			synchronized(pendingWrites) {
				reader.managedBuffer().lockBuffer();
				pendingWrites.add(reader);
				if (pendingWrites.size() == 1) {
					registration.updateInterested(
							SelectionKey.OP_READ | SelectionKey.OP_WRITE);
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
				downPipeline.fire(new IOError(null, e));
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
			ManagedByteBuffer buffer;
			buffer = readBuffers.acquire();
			int bytes = nioChannel.read(buffer.backingBuffer());
			if (bytes == 0) {
				buffer.unlockBuffer();
				return;
			}
			if (bytes > 0) {
				buffer.flip();
				downPipeline.fire(
						new Input<ManagedByteBuffer>(buffer, false), this);
				return;
			}
			// EOF (-1) from client
			synchronized (nioChannel) {
				if (nioChannel.socket().isOutputShutdown()) {
					// Client confirms our close, complete close
					nioChannel.close();
					return;
				}
				
			}
			// Client initiates close
			removeConnection();
			synchronized (pendingWrites) {
				synchronized (nioChannel) {
					if (!pendingWrites.isEmpty()) {
						// Pending writes, delay close
						pendingClose = true;
						// Mark as client initiated close
						nioChannel.shutdownInput();
						return;
					}
					// Nothing left to do, close
					nioChannel.close();
				}
			}
		}

		/**
		 * Checks if there is still data to be written. This may be
		 * a left over in an incompletely written buffer or a complete
		 * pending buffer. 
		 * 
		 * @throws IOException
		 * @throws InterruptedException 
		 */
		private void handleWriteOp() 
				throws IOException, InterruptedException {
			while (true) {
				ManagedByteBuffer.Reader head = null;
				synchronized (pendingWrites) {
					if (pendingWrites.isEmpty()) {
						// Nothing left to write, stop getting ops
						registration.updateInterested(
								SelectionKey.OP_READ);
						// Was the connection closed while we were writing?
						if (pendingClose) {
							synchronized (nioChannel) {
								if (nioChannel.socket().isInputShutdown()) {
									// Delayed close from client, complete
									nioChannel.close();
								} else {
									// Delayed close from server, initiate
									nioChannel.shutdownOutput();
								}
							}
							pendingClose = false;
						}
						break; // Nothing left to do
					}
					head = pendingWrites.peek();
					if (!head.get().hasRemaining()) {
						// Nothing left in head buffer, try next
						head.managedBuffer().unlockBuffer();
						pendingWrites.remove();
						continue;
					}
				}
				nioChannel.write(head.get()); // write...
				break; // ... and wait for next op
			}
		}

		/**
		 * Closes this connection.
		 * 
		 * @throws IOException if an error occurs
		 * @throws InterruptedException if the execution was interrupted 
		 */
		public void close() throws IOException, InterruptedException {
			removeConnection();
			synchronized (pendingWrites) {
				if (!pendingWrites.isEmpty()) {
					// Pending writes, delay close until done
					pendingClose = true;
					return;
				}
				// Nothing left to do, proceed
				synchronized (nioChannel) {
					if (nioChannel.isOpen()) {
						// Initiate close, must be confirmed by client
						nioChannel.shutdownOutput();
					}
				}
			}
		}

		private void removeConnection() throws InterruptedException {
			synchronized (connections) {
				if(!connections.remove(this)) {
					// Closed already
					return;
				}
				// In case the server is shutting down
				connections.notifyAll();
			}			
			Closed evt = new Closed();
			downPipeline.fire(evt, this);
			evt.get();
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return IOSubchannel.toString(this);
		}
	}
	
}
