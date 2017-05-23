package org.jgrapes.io.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

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

	/**
	 * Creates a new pipeline that sends the data from the given input stream
	 * as events on thegiven channel.
	 * 
	 * @param in the input stream to read from
	 * @param channel the channel to send to
	 */
	public InputStreamPipeline(InputStream in, IOSubchannel channel) {
		this.inStream = in;
		this.channel = channel;
	}

	@Override
	public void run() {
		// Reading from stream
		try (ReadableByteChannel inChannel = Channels.newChannel(inStream)) {
			while (true) {
				ManagedByteBuffer buffer 
					= channel.byteBufferPool().acquire();
				int read = inChannel.read(buffer.backingBuffer());
				boolean eof = (read == -1);
				channel.respond(new Output<>(buffer, eof));
				if (eof) {
					break;
				}
			}
			channel.respond(new Closed());
		} catch (InterruptedException e) {
			// Just stop
		} catch (IOException e) {
			channel.respond(new IOError(null, e));
		}
	}
}