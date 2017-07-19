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
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Manager;
import org.jgrapes.core.Self;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Error;
import org.jgrapes.core.events.Start;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.IOSubchannel.DefaultSubchannel;
import org.jgrapes.io.NioHandler;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.IOError;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.NioRegistration;
import org.jgrapes.io.events.NioRegistration.Registration;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.AvailabilityListener;
import org.jgrapes.io.util.ManagedBufferQueue;
import org.jgrapes.io.util.ManagedByteBuffer;
import org.jgrapes.io.util.PermitsPool;
import org.jgrapes.net.events.Accepted;
import org.jgrapes.net.events.Ready;

/**
 * Provides a TCP server. The server binds to the given address. If the
 * address is {@code null}, address and port are automatically assigned.
 * <P>
 * The end of record flag is not used by the server.
 */
public class TcpServer extends Component implements NioHandler {

	private SocketAddress serverAddress = null;
	private ServerSocketChannel serverSocketChannel = null;
	private int bufferSize = 0;
	private Set<TcpChannel> channels = new HashSet<>();
	private boolean closing = false;
	private ExecutorService executorService = null;
	private int backlog = 0;
	private PermitsPool connLimiter = null;
	private Registration registration = null;
	private AvailabilityListener permitsListener = new AvailabilityListener() {
		
		@Override
		public void availabilityChanged(PermitsPool pool, boolean available) {
			if (registration == null) {
				return;
			}
			registration.updateInterested(
					(Boolean)available ? SelectionKey.OP_ACCEPT : 0);
		}
	};

	/**
	 * Creates a new server, using itself as component channel. 
	 */
	public TcpServer() {
		super();
	}

	/**
	 * Creates a new server using the given channel.
	 * 
	 * @param componentChannel the component's channel
	 */
	public TcpServer(Channel componentChannel) {
		super(componentChannel);
	}
	
	/**
	 * Sets the address to bind to. If none is set, the address and port
	 * are assigned automatically.
	 * 
	 * @param serverAddress the address to bind to
	 * @return the TCP server for easy chaining
	 */
	public TcpServer setServerAddress(SocketAddress serverAddress) {
		this.serverAddress = serverAddress;
		return this;
	}

	/**
	 * Returns the server address. Before starting, the address is the
	 * address set with {@link #setServerAddress(SocketAddress)}. After
	 * starting the address is obtained from the created socket.  
	 * 
	 * @return the serverAddress
	 */
	public SocketAddress serverAddress() {
		try {
			return serverSocketChannel == null ? serverAddress
					: serverSocketChannel.getLocalAddress();
		} catch (IOException e) {
			return serverAddress;
		}
	}

	/**
	 * Sets the buffer size for the send an receive buffers.
	 * If no size is set, the system defaults will be used.
	 * 
	 * @param bufferSize the size to use for the send and receive buffers
	 * @return the TCP server for easy chaining
	 */
	public TcpServer setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
		return this;
	}
	
	/**
	 * @return the bufferSize
	 */
	public int bufferSize() {
		return bufferSize;
	}

	/**
	 * Sets the backlog size.
	 * 
	 * @param backlog the backlog to set
	 * @return the TCP server for easy chaining
	 */
	public TcpServer setBacklog(int backlog) {
		this.backlog = backlog;
		return this;
	}

	/**
	 * @return the backlog
	 */
	public int backlog() {
		return backlog;
	}

	/**
	 * Sets a permit "pool". A new connection is created only if a permit
	 * can be obtained from the pool.
	 * 
	 * @param connectionLimiter the connection pool to set
	 * @return the TCP server for easy chaining
	 */
	public TcpServer setConnectionLimiter(PermitsPool connectionLimiter) {
		if (connLimiter != null) {
			connLimiter.removeListener(permitsListener);
		}
		this.connLimiter = connectionLimiter;
		if (connLimiter != null) {
			connLimiter.addListener(permitsListener);
		}
		return this;
	}

	/**
	 * @return the connection Limiter
	 */
	public PermitsPool getConnectionLimiter() {
		return connLimiter;
	}

	/**
	 * Sets an executor service to be used by the event pipelines
	 * that process the data from the network. Setting this
	 * to an executor service with a limited number of threads
	 * allows to control the maximum load from the network.
	 * 
	 * @param executorService the executorService to set
	 * @return the TCP server for easy chaining
	 * @see Manager#newEventPipeline(ExecutorService)
	 */
	public TcpServer setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
		return this;
	}

	/**
	 * @return the executorService
	 */
	public ExecutorService executorService() {
		return executorService;
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
		serverSocketChannel.bind(serverAddress, backlog);
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
			registration = event.event().get();
			fire(new Ready(serverSocketChannel.getLocalAddress()));
			return;
		}
		if (handler instanceof TcpChannel) {
			((TcpChannel)handler)
				.registrationComplete(event.event());
		}
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.io.NioSelectable#handleOps(int)
	 */
	@Override
	public void handleOps(int ops) {
		synchronized (channels) {
			if ((ops & SelectionKey.OP_ACCEPT) != 0 && !closing) {
				try {
					if (connLimiter == null || connLimiter.tryAcquire()) {
						SocketChannel socketChannel = serverSocketChannel.accept();
						channels.add(new TcpChannel(socketChannel));
					}
				} catch (IOException e) {
					fire(new IOError(null, e));
				}
			}
		}
	}

	private boolean removeChannel(TcpChannel channel) {
		synchronized (channels) {
			if(!channels.remove(channel)) {
				// Closed already
				return false;
			}
			if (connLimiter != null) {
				connLimiter.release();
			}
			// In case the server is shutting down
			channels.notifyAll();
		}			
		return true;
	}
	
	/**
	 * Writes the data passed in the event to the client. The end of record
	 * flag is ignored.
	 * 
	 * @param event the event
	 * @throws IOException if an error occurs
	 */
	@Handler
	public void onOutput(Output<ManagedByteBuffer> event,
			TcpChannel channel) throws IOException {
		if (channels.contains(channel)) {
			channel.write(event);
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
			if (channel instanceof TcpChannel) {
				if (channels.contains(channel)) {
					((TcpChannel)channel).close();
				}
			} else {
				subOnly = false;
			}
		}
		if (subOnly || !serverSocketChannel.isOpen()) {
			return;
		}
		synchronized (channels) {
			closing = true;
			// Copy to avoid concurrent modification exception
			Set<TcpChannel> conns = new HashSet<>(channels);
			for (TcpChannel conn : conns) {
				conn.close();
			}
			while (channels.size() > 0) {
				channels.wait();
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
	 */
	public class TcpChannel 
		extends DefaultSubchannel implements NioHandler {

		private SocketChannel nioChannel;
		private EventPipeline downPipeline;
		private ManagedBufferQueue<ManagedByteBuffer, ByteBuffer> readBuffers;
		private Registration registration = null;
		private Queue<ManagedByteBuffer.Reader> pendingWrites = new ArrayDeque<>();
		private boolean pendingClose = false;
		
		/**
		 * @param nioChannel the channel
		 * @throws IOException if an I/O error occured
		 */
		public TcpChannel(SocketChannel nioChannel)	throws IOException {
			super(TcpServer.this);
			this.nioChannel = nioChannel;
			if (executorService != null) {
				downPipeline = newEventPipeline(executorService);
			} else {
				downPipeline = newEventPipeline();
			}

			int writeBufferSize = bufferSize == 0 
					? nioChannel.socket().getSendBufferSize() : bufferSize;
			setByteBufferPool(new ManagedBufferQueue<>(ManagedByteBuffer::new,
					() -> { return ByteBuffer.allocate(writeBufferSize); }, 2));
			
			int readBufferSize = bufferSize == 0 
					? nioChannel.socket().getReceiveBufferSize() : bufferSize;
			readBuffers = new ManagedBufferQueue<>(ManagedByteBuffer::new,
					() -> { return ByteBuffer.allocate(readBufferSize); }, 2);
			
			// Register with dispatcher
			nioChannel.configureBlocking(false);
			TcpServer.this.fire(
			        new NioRegistration(this, nioChannel, 0, TcpServer.this),
			        Channel.BROADCAST);
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
					nioChannel.getRemoteAddress(), false,
					Collections.emptyList()), this);
			registration.updateInterested(SelectionKey.OP_READ);

		}

		/**
		 * Write the data on this channel.
		 * 
		 * @param event the event
		 * @throws IOException if an error occurred
		 */
		public void write(Output<ManagedByteBuffer> event) throws IOException {
			synchronized(pendingWrites) {
				if (!nioChannel.isOpen()) {
					return;
				}
				ManagedByteBuffer.Reader reader = event.buffer().newReader();
				if (!pendingWrites.isEmpty()) {
					reader.managedBuffer().lockBuffer();
					pendingWrites.add(reader);
					return;
				}
				nioChannel.write(reader.get());
				if (!reader.get().hasRemaining()) {
					return;
				}
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
			buffer.unlockBuffer();
			synchronized (nioChannel) {
				if (nioChannel.socket().isOutputShutdown()) {
					// Client confirms our close, complete close
					nioChannel.close();
					return;
				}
				
			}
			// Client initiates close
			removeChannel();
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
						registration.updateInterested(SelectionKey.OP_READ);
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
		 * Closes this channel.
		 * 
		 * @throws IOException if an error occurs
		 * @throws InterruptedException if the execution was interrupted 
		 */
		public void close() throws IOException, InterruptedException {
			removeChannel();
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

		private void removeChannel() throws InterruptedException {
			if (TcpServer.this.removeChannel(this)) {
				Closed evt = new Closed();
				downPipeline.fire(evt, this);
				evt.get();
			}
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return IOSubchannel.toString(this);
		}
	}
	
}
