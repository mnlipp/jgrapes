/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017  Michael N. Lipp
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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.LinkedIOSubchannel;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedBufferQueue;
import org.jgrapes.io.util.ManagedByteBuffer;
import org.jgrapes.net.events.Accepted;

/**
 * A component that receives and send byte buffers on an
 * encrypted channel and sends and receives the corresponding
 * decrypted data on its own channel.
 */
public class SslServer extends Component {

	private static final Logger logger 
		= Logger.getLogger(SslServer.class.getName());
	
	private SSLContext sslContext;
	
	/**
	 * @param componentChannel the component's channel
	 * @param encryptedChannel the channel with the encrypted data
	 * @param sslContext the SSL context to use
	 */
	public SslServer(Channel componentChannel, Channel encryptedChannel,
			SSLContext sslContext) {
		super(componentChannel);
		this.sslContext = sslContext;
		Handler.Evaluator.add(
				this, "onAccepted", encryptedChannel.defaultCriterion());
		Handler.Evaluator.add(
				this, "onInput", encryptedChannel.defaultCriterion());
		Handler.Evaluator.add(
				this, "onClosed", encryptedChannel.defaultCriterion());
	}

	/**
	 * Creates a new downstream connection as {@link LinkedIOSubchannel} 
	 * of the network connection together with an {@link SSLEngine}.
	 * 
	 * @param event
	 *            the accepted event
	 */
	@Handler(dynamic=true)
	public void onAccepted(Accepted event, IOSubchannel channel) {
		new SslConn(event, channel);
	}

	/**
	 * Handles encrypted data from upstream (the network). The data is 
	 * send through the {@link SSLEngine} and events are sent downstream 
	 * (and in the initial phases upstream) according to the conversion 
	 * results.
	 * 
	 * @param event the event
	 * @throws InterruptedException 
	 * @throws SSLException 
	 */
	@Handler(dynamic = true)
	public void onInput(Input<ManagedByteBuffer> event, IOSubchannel channel)
	        throws InterruptedException, SSLException {
		final SslConn downChannel = (SslConn) LinkedIOSubchannel
		        .lookupLinked(channel);
		if (downChannel == null || downChannel.converterComponent() != this) {
			return;
		}
		downChannel.sendDownstream(event);
	}

	/**
	 * Handles a close event from the encrypted channel (client).
	 * 
	 * @param event the event
	 * @throws InterruptedException 
	 * @throws SSLException 
	 */
	@Handler(dynamic = true)
	public void onClosed(Closed event, IOSubchannel netChannel)
	        throws SSLException, InterruptedException {
		final SslConn downChannel = (SslConn) LinkedIOSubchannel
		        .lookupLinked(netChannel);
		if (downChannel == null || downChannel.converterComponent() != this) {
			return;
		}
		downChannel.upstreamClosed();
	}
	
	/**
	 * Sends decrypted data through the engine and then upstream.
	 * 
	 * @param event
	 *            the event with the data
	 * @throws InterruptedException if the execution was interrupted
	 * @throws SSLException if some SSL related problem occurs
	 */
	@Handler
	public void onOutput(Output<ManagedBuffer<ByteBuffer>> event,
	        SslConn downChannel)
	        throws InterruptedException, SSLException {
		if (downChannel.converterComponent() != this) {
			return;
		}
		ByteBuffer output = event.buffer().backingBuffer().duplicate();
		while (output.hasRemaining()) {
			ManagedByteBuffer out = downChannel.upstreamBuffer();
			downChannel.sslEngine.wrap(output, out.backingBuffer());
			downChannel.upstreamChannel().respond(new Output<>(
			        out, event.isEndOfRecord()));
		}
	}

	/**
	 * Handles a close event from downstream.
	 * 
	 * @param event
	 *            the close event
	 * @throws SSLException if an SSL related problem occurs
	 * @throws InterruptedException if the execution was interrupted
	 */
	@Handler
	public void onClose(Close event, SslConn connection)
	        throws InterruptedException, SSLException {
		if (connection.converterComponent() != this) {
			return;
		}
		connection.sslEngine.closeOutbound();
		while (!connection.sslEngine.isOutboundDone()) {
			ManagedByteBuffer feedback = connection.upstreamBuffer();
			connection.sslEngine.wrap(ManagedByteBuffer.EMPTY_BUFFER
			        .backingBuffer(), feedback.backingBuffer());
			connection.upstreamChannel()
			        .respond(new Output<>(feedback, false));
		}
		connection.upstreamChannel().respond(new Close());
	}
	
	private class SslConn extends LinkedIOSubchannel {
		public SocketAddress localAddress;
		public SocketAddress remoteAddress;
		public SSLEngine sslEngine;
		private ManagedBufferQueue<ManagedByteBuffer, ByteBuffer> downstreamPool;
		private ManagedBufferQueue<ManagedByteBuffer, ByteBuffer> upstreamPool;
		private boolean isInputClosed = false;

		public SslConn(Accepted event, IOSubchannel upstreamChannel) {
			super(SslServer.this, upstreamChannel);
			localAddress = event.localAddress();
			remoteAddress = event.remoteAddress();
			if (remoteAddress instanceof InetSocketAddress) {
				sslEngine = sslContext.createSSLEngine(
						((InetSocketAddress)remoteAddress).getAddress().getHostAddress(),
						((InetSocketAddress)remoteAddress).getPort());
			} else {
				sslEngine = sslContext.createSSLEngine();
			}
			sslEngine.setUseClientMode(false);
		}
		
		@Override
		public ManagedBufferQueue<ManagedByteBuffer, ByteBuffer> bufferPool() {
			if (downstreamPool == null) {
				// Adding 50, see https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/samples/sslengine/SSLEngineSimpleDemo.java
				int bufSize = sslEngine.getSession()
						.getApplicationBufferSize() + 50;
				downstreamPool = new ManagedBufferQueue<>(ManagedByteBuffer.class,
						ByteBuffer.allocate(bufSize), 
						ByteBuffer.allocate(bufSize));
			}
			return downstreamPool;
		}
		
		public ManagedByteBuffer upstreamBuffer() throws InterruptedException {
			if (upstreamPool == null) {
				ManagedByteBuffer testBuf = upstreamChannel().bufferPool().acquire();
				if (testBuf.capacity() 
						>= sslEngine.getSession().getPacketBufferSize() + 50) {
					upstreamPool = upstreamChannel().bufferPool();
					return testBuf;
				} else {
					int bufSize = sslEngine.getSession().getPacketBufferSize() + 50;
					upstreamPool = new ManagedBufferQueue<>(ManagedByteBuffer.class,
							ByteBuffer.allocate(bufSize), 
							ByteBuffer.allocate(bufSize));
				}
			}
			return upstreamPool.acquire();
		}
		
		public void sendDownstream(Input<ManagedByteBuffer> event)
				throws SSLException, InterruptedException {
			ManagedByteBuffer unwrapped = bufferPool().acquire();
			ByteBuffer input = event.buffer().duplicate();
			SSLEngineResult unwrapResult;
			while (true) {
				unwrapResult = sslEngine.unwrap(
						input, unwrapped.backingBuffer());
				// Handle any handshaking procedures
				switch (unwrapResult.getHandshakeStatus()) {
				case NEED_TASK:
					while (true) {
						Runnable runnable = sslEngine.getDelegatedTask();
						if (runnable == null) {
							break;
						}
						runnable.run();
					}
					continue;
					
				case NEED_WRAP:
					ManagedByteBuffer feedback = upstreamBuffer();
					SSLEngineResult wrapResult = sslEngine.wrap(
							ManagedByteBuffer.EMPTY_BUFFER
							.backingBuffer(), feedback.backingBuffer());
					upstreamChannel().respond(new Output<>(feedback, false));
					if (wrapResult.getHandshakeStatus() == HandshakeStatus.FINISHED) {
						fireAccepted();
					}
					continue;
					
				case FINISHED:
					fireAccepted();
					// fall through
				case NEED_UNWRAP:
					if (input.hasRemaining()) {
						continue;
					}
					break;
					
				default:
					break;
				}
				// Handshake done
				break;
			}
			if (unwrapped.position() == 0) {
				// Was only handshake
				unwrapped.unlockBuffer();
			} else {
				// forward data received
				unwrapped.flip();
				fire(new Input<>(unwrapped, sslEngine.isInboundDone()), this);
			}
			
			// final message?
			if (unwrapResult.getStatus() == Status.CLOSED
					&& ! isInputClosed) {
				Closed evt = new Closed();
				newEventPipeline().fire(evt, this);
				evt.get();
				isInputClosed = true;
			}
		}

		private void fireAccepted() {
			List<SNIServerName> snis = Collections.emptyList();
			if (sslEngine.getSession() instanceof ExtendedSSLSession) {
				snis = ((ExtendedSSLSession)sslEngine.getSession())
					.getRequestedServerNames();
			}
			fire(new Accepted(
					localAddress, remoteAddress, true, snis), this);
		}
		
		public void upstreamClosed()
				throws SSLException, InterruptedException {
			if (!isInputClosed) {
				// was not properly closed on SSL layer
				Closed evt = new Closed();
				newEventPipeline().fire(evt, this);
				evt.get();
			}
			try {
				sslEngine.closeInbound();
				while (!sslEngine.isOutboundDone()) {
					ManagedByteBuffer feedback = upstreamBuffer();
					sslEngine.wrap(ManagedByteBuffer.EMPTY_BUFFER
							.backingBuffer(),feedback.backingBuffer());
					upstreamChannel().respond(new Output<>(feedback, false));
				}
			} catch (SSLException e) {
				// Several clients (notably chromium, see
				// https://bugs.chromium.org/p/chromium/issues/detail?id=118366
				// don't close the connection properly. So nobody is really
				// interested in this message
				logger.log(Level.FINEST, e.getMessage(), e);
			}
		}

	}
}
