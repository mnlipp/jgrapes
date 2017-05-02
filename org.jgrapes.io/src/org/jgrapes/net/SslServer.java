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
	 * @param plainChannel the component's channel
	 * @param encryptedChannel the channel with the encrypted data
	 * @param sslContext the SSL context to use
	 */
	public SslServer(Channel plainChannel, Channel encryptedChannel,
			SSLContext sslContext) {
		super(plainChannel);
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
	public void onAccepted(Accepted event, IOSubchannel encryptedChannel) {
		new PlainChannel(event, encryptedChannel);
	}

	/**
	 * Handles encrypted data from upstream (the network). The data is 
	 * send through the {@link SSLEngine} and events are sent downstream 
	 * (and in the initial phases upstream) according to the conversion 
	 * results.
	 * 
	 * @param event the event
	 * @param encryptedChannel the channel for exchanging the encrypted data
	 * @throws InterruptedException 
	 * @throws SSLException 
	 */
	@Handler(dynamic = true)
	public void onInput(
			Input<ManagedByteBuffer> event, IOSubchannel encryptedChannel)
	        throws InterruptedException, SSLException {
		final PlainChannel plainChannel = (PlainChannel) LinkedIOSubchannel
		        .lookupLinked(encryptedChannel);
		if (plainChannel == null || plainChannel.converterComponent() != this) {
			return;
		}
		plainChannel.sendDownstream(event);
	}

	/**
	 * Handles a close event from the encrypted channel (client).
	 * 
	 * @param event the event
	 * @param encryptedChannel the channel for exchanging the encrypted data
	 * @throws InterruptedException 
	 * @throws SSLException 
	 */
	@Handler(dynamic = true)
	public void onClosed(Closed event, IOSubchannel encryptedChannel)
	        throws SSLException, InterruptedException {
		final PlainChannel plainChannel = (PlainChannel) LinkedIOSubchannel
		        .lookupLinked(encryptedChannel);
		if (plainChannel == null || plainChannel.converterComponent() != this) {
			return;
		}
		plainChannel.upstreamClosed();
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
	        PlainChannel plainChannel)
	        throws InterruptedException, SSLException {
		if (plainChannel.converterComponent() != this) {
			return;
		}
		plainChannel.sendUpstream(event);
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
	public void onClose(Close event, PlainChannel plainChannel)
	        throws InterruptedException, SSLException {
		if (plainChannel.converterComponent() != this) {
			return;
		}
		plainChannel.close(event);
	}
	
	private class PlainChannel extends LinkedIOSubchannel {
		public SocketAddress localAddress;
		public SocketAddress remoteAddress;
		public SSLEngine sslEngine;
		private ManagedBufferQueue<ManagedByteBuffer, ByteBuffer> downstreamPool;
		private boolean isInputClosed = false;

		public PlainChannel(Accepted event, IOSubchannel upstreamChannel) {
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
			
			// Create buffer pools, adding 50 to application buffer size, see 
			// https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/samples/sslengine/SSLEngineSimpleDemo.java
			int bufSize = sslEngine.getSession().getApplicationBufferSize() + 50;
			downstreamPool = new ManagedBufferQueue<>(ManagedByteBuffer::new,
					ByteBuffer.allocate(bufSize), 
					ByteBuffer.allocate(bufSize));
			setByteBufferPool(new ManagedBufferQueue<>(ManagedByteBuffer::new,
					ByteBuffer.allocate(bufSize), 
					ByteBuffer.allocate(bufSize)));
		}
		
		public void sendDownstream(Input<ManagedByteBuffer> event)
				throws SSLException, InterruptedException {
			ManagedByteBuffer unwrapped = downstreamPool.acquire();
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
					ManagedByteBuffer feedback = byteBufferPool().acquire();
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

		public void sendUpstream(Output<ManagedBuffer<ByteBuffer>> event)
				throws SSLException, InterruptedException {
			ByteBuffer output = event.buffer().backingBuffer().duplicate();
			while (output.hasRemaining()) {
				ManagedByteBuffer out = byteBufferPool().acquire();
				sslEngine.wrap(output, out.backingBuffer());
				upstreamChannel().respond(
						new Output<>(out, event.isEndOfRecord()));
			}
		}

		public void close(Close event) 
				throws InterruptedException, SSLException {
			sslEngine.closeOutbound();
			while (!sslEngine.isOutboundDone()) {
				ManagedByteBuffer feedback = byteBufferPool().acquire();
				sslEngine.wrap(ManagedByteBuffer.EMPTY_BUFFER
				        .backingBuffer(), feedback.backingBuffer());
				upstreamChannel().respond(new Output<>(feedback, false));
			}
			upstreamChannel().respond(new Close());
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
					ManagedByteBuffer feedback = byteBufferPool().acquire();
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
