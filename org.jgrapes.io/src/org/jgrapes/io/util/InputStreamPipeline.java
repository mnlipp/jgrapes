package org.jgrapes.io.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.jgrapes.core.EventPipeline;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.IOError;
import org.jgrapes.io.events.Output;

/**
 * Forwards the content of an input stream as a sequence of {@link Output}
 * events.
 */
public class InputStreamPipeline implements Runnable {
	
	private InputStream inStream;
	private IOSubchannel channel;
	private EventPipeline eventPipeline;
	private boolean sendClose = true;

	/**
	 * Creates a new pipeline that sends the data from the given input stream
	 * as events on the given channel, using the given event pipeline.
	 * 
	 * @param in the input stream to read from
	 * @param channel the channel to send to
	 * @param eventPipeline
	 *            the event pipeline used for firing events
	 */
	public InputStreamPipeline(InputStream in, IOSubchannel channel,
			EventPipeline eventPipeline) {
		this.inStream = in;
		this.channel = channel;
		this.eventPipeline = eventPipeline;
	}

	/**
	 * Creates a new pipeline that sends the data from the given input stream
	 * as events on the given channel, using the channel's response pipeline.
	 * 
	 * @param in the input stream to read from
	 * @param channel the channel to send to
	 */
	public InputStreamPipeline(InputStream in, IOSubchannel channel) {
		this(in, channel, channel.responsePipeline());
	}
	
	/**
	 * Suppresses the sending of a close event when the stream is closed. 
	 * 
	 * @return the stream for easy chaining
	 */
	public InputStreamPipeline suppressClose() {
		sendClose = false;
		return this;
	}
	
	
	@Override
	public void run() {
		// Reading from stream
		try (ReadableByteChannel inChannel = Channels.newChannel(inStream)) {
			while (true) {
				ManagedByteBuffer buffer = channel.byteBufferPool().acquire();
				int read;
				try {
					read = inChannel.read(buffer.backingBuffer());
				} catch (IOException e) {
					// Read failed, release buffer
					buffer.unlockBuffer();
					throw e;
				}
				boolean eof = (read == -1);
				eventPipeline.fire(new Output<>(buffer, eof), channel);
				if (eof) {
					break;
				}
			}
			if (sendClose) {
				eventPipeline.fire(new Closed(), channel);
			}
		} catch (InterruptedException e) {
			// Just stop
		} catch (IOException e) {
			eventPipeline.fire(new IOError(null, e), channel);
		}
	}
}