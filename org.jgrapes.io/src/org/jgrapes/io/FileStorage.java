/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
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
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.FileOpened;
import org.jgrapes.io.events.IOError;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.OpenFile;
import org.jgrapes.io.events.Opening;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.events.SaveInput;
import org.jgrapes.io.events.SaveOutput;
import org.jgrapes.io.events.StreamFile;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedBufferPool;

/**
 * A component that reads from or writes to a file.
 */
@SuppressWarnings({ "PMD.ExcessiveImports", "PMD.CouplingBetweenObjects" })
public class FileStorage extends Component {

    private int bufferSize;

    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<Channel, Writer> inputWriters = Collections
        .synchronizedMap(new WeakHashMap<>());
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<Channel, Writer> outputWriters = Collections
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
    @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops",
        "PMD.AccessorClassGeneration", "PMD.AvoidDuplicateLiterals" })
    public void onStreamFile(StreamFile event)
            throws InterruptedException {
        if (Arrays.asList(event.options())
            .contains(StandardOpenOption.WRITE)) {
            throw new IllegalArgumentException(
                "Cannot stream file opened for writing.");
        }
        for (IOSubchannel channel : event.channels(IOSubchannel.class)) {
            if (inputWriters.containsKey(channel)) {
                channel.respond(new IOError(event,
                    new IllegalStateException("File is already open.")));
            } else {
                new FileStreamer(event, channel);
            }
        }
    }

    /**
     * A file streamer.
     */
    private final class FileStreamer {

        private final IOSubchannel channel;
        private final Path path;
        @SuppressWarnings("PMD.ImmutableField")
        private AsynchronousFileChannel ioChannel;
        private ManagedBufferPool<ManagedBuffer<ByteBuffer>,
                ByteBuffer> ioBuffers;
        private long offset;
        private final CompletionHandler<Integer,
                ManagedBuffer<ByteBuffer>> readCompletionHandler
                    = new ReadCompletionHandler();

        private FileStreamer(StreamFile event, IOSubchannel channel)
                throws InterruptedException {
            this.channel = channel;
            path = event.path();
            offset = 0;
            try {
                try {
                    ioChannel = AsynchronousFileChannel
                        .open(event.path(), event.options());
                } catch (UnsupportedOperationException e) {
                    runReaderThread(event);
                    return;
                }
            } catch (IOException e) {
                channel.respond(new IOError(event, e));
                return;
            }
            registerAsGenerator();
            // Reading from file
            ioBuffers = new ManagedBufferPool<>(ManagedBuffer::new,
                () -> {
                    return ByteBuffer.allocateDirect(bufferSize);
                }, 2);
            ManagedBuffer<ByteBuffer> buffer = ioBuffers.acquire();
            // (1) Opening, (2) FileOpened, (3) Output events
            channel.respond(Event
                .onCompletion(new Opening<OpenFile>().setResult(event), e -> {
                    channel.respond(new FileOpened(event));
                    // Start reading.
                    synchronized (ioChannel) {
                        ioChannel.read(buffer.backingBuffer(), offset, buffer,
                            readCompletionHandler);
                    }
                }));
        }

        /**
         * The read completion handler.
         */
        private final class ReadCompletionHandler implements
                CompletionHandler<Integer, ManagedBuffer<ByteBuffer>> {
            @Override
            @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis",
                "PMD.EmptyCatchBlock", "PMD.AvoidDuplicateLiterals" })
            public void completed(
                    Integer result, ManagedBuffer<ByteBuffer> buffer) {
                if (result >= 0) {
                    offset += result;
                    boolean eof = true;
                    try {
                        eof = offset == ioChannel.size();
                    } catch (IOException e1) {
                        // Handled like true
                    }
                    channel.respond(Output.fromSink(buffer, eof));
                    if (!eof) {
                        try {
                            ManagedBuffer<ByteBuffer> nextBuffer
                                = ioBuffers.acquire();
                            nextBuffer.clear();
                            synchronized (ioChannel) {
                                ioChannel.read(nextBuffer.backingBuffer(),
                                    offset,
                                    nextBuffer, readCompletionHandler);
                            }
                        } catch (InterruptedException e) {
                            // Results in empty buffer
                        }
                        return;
                    }
                }
                IOException ioExc = null;
                try {
                    ioChannel.close();
                } catch (ClosedChannelException e) {
                    // Can be ignored
                } catch (IOException e) {
                    ioExc = e;
                }
                channel.respond(new Closed<Void>(ioExc));
                unregisterAsGenerator();
            }

            @Override
            public void failed(
                    Throwable exc, ManagedBuffer<ByteBuffer> context) {
                channel.respond(new Closed<Void>(exc));
                unregisterAsGenerator();
            }
        }

        /**
         * Stream file that doesn't support asynchronous I/O.
         * 
         * @param event
         * @throws IOException
         */
        private void runReaderThread(StreamFile event)
                throws IOException {
            ioBuffers = new ManagedBufferPool<>(ManagedBuffer::new,
                () -> {
                    return ByteBuffer.allocateDirect(bufferSize);
                }, 2);
            @SuppressWarnings("PMD.CloseResource")
            final SeekableByteChannel ioChannel
                = Files.newByteChannel(event.path(), event.options());
            activeEventPipeline().executorService().submit(new Runnable() {
                @Override
                @SuppressWarnings("PMD.EmptyCatchBlock")
                public void run() {
                    // Reading from file
                    IOException ioExc = null;
                    try {
                        long size = ioChannel.size();
                        while (ioChannel.position() < size) {
                            ManagedBuffer<ByteBuffer> buffer
                                = ioBuffers.acquire();
                            buffer.fillFromChannel(ioChannel);
                            channel.respond(Output.fromSink(buffer,
                                ioChannel.position() == size));
                        }
                        ioChannel.close();
                    } catch (InterruptedException e) {
                        return;
                    } catch (ClosedChannelException e) {
                        // Can be ignored
                    } catch (IOException e) {
                        ioExc = e;
                    }
                    channel.respond(new Closed<Void>(ioExc));
                }
            });
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(50);
            builder.append("FileStreamer [");
            if (channel != null) {
                builder.append("channel=");
                builder.append(Channel.toString(channel));
                builder.append(", ");
            }
            if (path != null) {
                builder.append("path=").append(path).append(", ");
            }
            builder.append("offset=")
                .append(offset)
                .append(']');
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
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public void onSaveInput(SaveInput event) throws InterruptedException {
        if (!Arrays.asList(event.options())
            .contains(StandardOpenOption.WRITE)) {
            throw new IllegalArgumentException(
                "File must be opened for writing.");
        }
        for (IOSubchannel channel : event.channels(IOSubchannel.class)) {
            if (inputWriters.containsKey(channel)) {
                channel.respond(new IOError(event,
                    new IllegalStateException("File is already open.")));
            } else {
                new Writer(event, channel);
            }
        }
    }

    /**
     * Handle input by writing it to the file, if a channel exists.
     *
     * @param event the event
     * @param channel the channel
     */
    @Handler
    public void onInput(Input<ByteBuffer> event, Channel channel) {
        Writer writer = inputWriters.get(channel);
        if (writer != null) {
            writer.write(event.buffer());
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
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public void onSaveOutput(SaveOutput event) throws InterruptedException {
        if (!Arrays.asList(event.options())
            .contains(StandardOpenOption.WRITE)) {
            throw new IllegalArgumentException(
                "File must be opened for writing.");
        }
        for (IOSubchannel channel : event.channels(IOSubchannel.class)) {
            if (outputWriters.containsKey(channel)) {
                channel.respond(new IOError(event,
                    new IllegalStateException("File is already open.")));
            } else {
                new Writer(event, channel);
            }
        }
    }

    /**
     * Handle {@link Output} events by writing them to the file, if
     * a channel exists.
     *
     * @param event the event
     * @param channel the channel
     */
    @Handler
    public void onOutput(Output<ByteBuffer> event, Channel channel) {
        Writer writer = outputWriters.get(channel);
        if (writer != null) {
            writer.write(event.buffer());
        }
    }

    /**
     * Handle close by closing the file associated with the channel.
     *
     * @param event the event
     * @param channel the channel
     * @throws InterruptedException the interrupted exception
     */
    @Handler
    public void onClose(Close event, Channel channel)
            throws InterruptedException {
        Writer writer = inputWriters.get(channel);
        if (writer != null) {
            writer.close(event);
        }
        writer = outputWriters.get(channel);
        if (writer != null) {
            writer.close(event);
        }
    }

    /**
     * Handle stop by closing all files.
     *
     * @param event the event
     * @throws InterruptedException the interrupted exception
     */
    @Handler(priority = -1000)
    public void onStop(Stop event) throws InterruptedException {
        while (!inputWriters.isEmpty()) {
            Writer handler = inputWriters.entrySet().iterator().next()
                .getValue();
            handler.close(event);
        }
        while (!outputWriters.isEmpty()) {
            Writer handler = outputWriters.entrySet().iterator().next()
                .getValue();
            handler.close(event);
        }
    }

    /**
     * A writer.
     */
    private class Writer {

        private final IOSubchannel channel;
        private Path path;
        private AsynchronousFileChannel ioChannel;
        private long offset;
        private final CompletionHandler<Integer,
                WriteContext> writeCompletionHandler
                    = new WriteCompletionHandler();
        private int outstandingAsyncs;

        /**
         * The write context needs to be finer grained than the general file
         * connection context because an asynchronous write may be only
         * partially successful, i.e. not all data provided by the write event
         * may successfully be written in one asynchronous write invocation.
         */
        private class WriteContext {
            public final ManagedBuffer<ByteBuffer>.ByteBufferView reader;
            public final long pos;

            /**
             * Instantiates a new write context.
             *
             * @param reader the reader
             * @param pos the pos
             */
            public WriteContext(
                    ManagedBuffer<ByteBuffer>.ByteBufferView reader, long pos) {
                this.reader = reader;
                this.pos = pos;
            }
        }

        /**
         * Instantiates a new writer.
         *
         * @param event the event
         * @param channel the channel
         * @throws InterruptedException the interrupted exception
         */
        public Writer(SaveInput event, IOSubchannel channel)
                throws InterruptedException {
            this(event, event.path(), event.options(), channel);
            inputWriters.put(channel, this);
            channel.respond(new FileOpened(event));
        }

        /**
         * Instantiates a new writer.
         *
         * @param event the event
         * @param channel the channel
         * @throws InterruptedException the interrupted exception
         */
        public Writer(SaveOutput event, IOSubchannel channel)
                throws InterruptedException {
            this(event, event.path(), event.options(), channel);
            outputWriters.put(channel, this);
            channel.respond(new FileOpened(event));
        }

        private Writer(Event<?> event, Path path, OpenOption[] options,
                IOSubchannel channel) throws InterruptedException {
            this.channel = channel;
            this.path = path;
            offset = 0;
            try {
                ioChannel = AsynchronousFileChannel.open(path, options);
            } catch (IOException e) {
                channel.respond(new IOError(event, e));
            }
        }

        /**
         * Write the buffer.
         *
         * @param buffer the buffer
         */
        public void write(ManagedBuffer<ByteBuffer> buffer) {
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
                ManagedBuffer<ByteBuffer>.ByteBufferView reader
                    = buffer.newByteBufferView();
                ioChannel.write(reader.get(), offset,
                    new WriteContext(reader, offset),
                    writeCompletionHandler);
            }
            offset += written;
        }

        /**
         * A write completion handler.
         */
        private final class WriteCompletionHandler
                implements CompletionHandler<Integer, WriteContext> {

            @Override
            public void completed(Integer result, WriteContext context) {
                ManagedBuffer<ByteBuffer>.ByteBufferView reader
                    = context.reader;
                if (reader.get().hasRemaining()) {
                    ioChannel.write(reader.get(),
                        context.pos + reader.get().position(),
                        context, writeCompletionHandler);
                    return;
                }
                reader.managedBuffer().unlockBuffer();
                handled();
            }

            @Override
            public void failed(Throwable exc, WriteContext context) {
                try {
                    if (!(exc instanceof AsynchronousCloseException)) {
                        channel.respond(new IOError(null, exc));
                    }
                } finally {
                    handled();
                }
            }

            @SuppressWarnings("PMD.AssignmentInOperand")
            private void handled() {
                synchronized (ioChannel) {
                    if (--outstandingAsyncs == 0) {
                        unregisterAsGenerator();
                        ioChannel.notifyAll();
                    }
                }
            }
        }

        /**
         * Close.
         *
         * @param event the event
         * @throws InterruptedException the interrupted exception
         */
        @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis",
            "PMD.EmptyCatchBlock" })
        public void close(Event<?> event)
                throws InterruptedException {
            IOException ioExc = null;
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
                ioExc = e;
            }
            channel.respond(new Closed<Void>(ioExc));
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
            StringBuilder builder = new StringBuilder(50);
            builder.append("FileConnection [");
            if (channel != null) {
                builder.append("channel=")
                    .append(Channel.toString(channel))
                    .append(", ");
            }
            if (path != null) {
                builder.append("path=")
                    .append(path)
                    .append(", ");
            }
            builder.append("offset=")
                .append(offset)
                .append(']');
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
        builder.append(Components.objectName(this))
            .append(" [");
        if (inputWriters != null) {
            builder.append(inputWriters.values().stream()
                .map(chnl -> Components.objectName(chnl))
                .collect(Collectors.toList()));
        }
        builder.append(']');
        return builder.toString();
    }
}
