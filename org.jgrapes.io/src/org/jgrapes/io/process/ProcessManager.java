/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2023 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public 
 * License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License 
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.io.process;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.IOSubchannel.DefaultIOSubchannel;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Opening;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.events.ProcessExited;
import org.jgrapes.io.events.ProcessStarted;
import org.jgrapes.io.events.StartProcess;
import org.jgrapes.io.events.StartProcessError;
import org.jgrapes.io.util.InputStreamPipeline;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedBufferPool;

/**
 * Provides a component that executes processes. A process is started
 * by firing a {@link StartProcess} event. In response, the
 * {@link ProcessManager} starts the process and creates a 
 * {@link ProcessChannel} (i.e. an {@link IOSubchannel) for communication
 * with the process. It fires an {@link Opening} and {@link ProcessStarted} 
 * event on the newly created channel.
 * 
 * Data may be sent to the process's stdin by firing {@link Output}
 * events on the {@link ProcessChannel}. As usual, these events should
 * be fired using the channels {@link IOSubchannel#responsePipeline()
 * response pipeline}. Data generated by the process is provided by
 * {@link Input} events. In order to distinguish between stdout and stderr,
 * the events have an association with class {@link FileDescriptor} as
 * key and an associated value of 1 (stdout) or 2 (stderr).
 * 
 * When the process terminated, three {@link Closed} events are fired on
 * the {@link ProcessChannel} one each for stdout and stderr (with the
 * same association as was used for the {@link Input} events) and a 
 * as third event a {@link ProcessExited} (specialized {@link Closed})
 * with the process's exit value. Note that the sequence in which these
 * events are sent is undefined.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class ProcessManager extends Component {

    private ExecutorService executorService
        = Components.defaultExecutorService();
    private final Set<ProcessChannel> channels
        = Collections.synchronizedSet(new HashSet<>());

    /**
     * Creates a new connector, using itself as component channel. 
     */
    public ProcessManager() {
        this(Channel.SELF);
    }

    /**
     * Create a new instance using the given channel.
     *
     * @param componentChannel the component channel
     */
    public ProcessManager(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * Sets an executor service to be used by the event pipelines
     * that forward data from the process.
     * 
     * @param executorService the executorService to set
     * @return the process manager for easy chaining
     * @see Manager#newEventPipeline(ExecutorService)
     */
    public ProcessManager setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    /**
     * Start a new process using the data from the event.
     *
     * @param event the event
     */
    @Handler
    public void onStartProcess(StartProcess event) {
        var pbd = new ProcessBuilder(event.command());
        if (event.directory() != null) {
            pbd.directory(event.directory());
        }
        if (event.environment() != null) {
            Map<String, String> env = pbd.environment();
            for (var entry : event.environment().entrySet()) {
                if (entry.getValue() == null) {
                    env.remove(entry.getValue());
                    continue;
                }
                env.put(entry.getKey(), entry.getValue());
            }
        }
        try {
            Process proc;
            new ProcessChannel(event, proc = pbd.start());
            logger.fine(() -> "Started process pid=" + proc.toHandle().pid());
        } catch (IOException e) {
            fire(new StartProcessError(event, "Failed to start process.", e));
        }
    }

    /**
     * Writes the data passed in the event. 
     * 
     * The end of record flag is used to determine if a channel is 
     * eligible for purging. If the flag is set and all output has 
     * been processed, the channel is purgeable until input is 
     * received or another output event causes the state to be 
     * reevaluated. 
     *
     * @param event the event
     * @param channel the channel
     * @throws InterruptedException the interrupted exception
     * @throws IOException 
     */
    @Handler
    public void onOutput(Output<ByteBuffer> event,
            ProcessChannel channel) throws InterruptedException, IOException {
        if (channels.contains(channel)) {
            channel.write(event);
        }
    }

    /**
     * Closes the output to the process (the process's stdin).
     * 
     * If the event has an association with key {@link Process},
     * the event additionally causes the process to be "closed",
     * i.e. to be terminated (see {@link ProcessHandle#destroy}).
     *
     * @param event the event
     * @throws IOException if an I/O exception occurred
     * @throws InterruptedException if the execution was interrupted
     */
    @Handler
    public void onClose(Close event) {
        for (Channel channel : event.channels()) {
            if (channel instanceof ProcessChannel
                && channels.contains(channel)) {
                ((ProcessChannel) channel).close(event);
            }
        }
    }

    /**
     * Stop all running processes.
     * 
     * @param event
     */
    @Handler
    public void onStop(Stop event) {
        Set<ProcessChannel> copy;
        synchronized (channels) {
            copy = new HashSet<>(channels);
        }
        for (var channel : copy) {
            channel.doClose(true);
        }
    }

    /**
     * Handles closed events from stdout and stderr.
     *
     * @param event the event
     * @throws IOException if an I/O exception occurred
     * @throws InterruptedException if the execution was interrupted
     */
    @Handler(priority = -100)
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public void onClosed(Closed<?> event)
            throws IOException, InterruptedException {
        for (Channel channel : event.channels()) {
            if (channel instanceof ProcessChannel
                && channels.contains(channel)) {
                ((ProcessChannel) channel).closed(event);
            }
        }
    }

    /**
     * The Class ProcessChannel.
     */
    public class ProcessChannel extends DefaultIOSubchannel {

        private final StartProcess startEvent;
        private final Process process;
        private final EventPipeline downPipeline;
        private boolean running;
        private final AtomicBoolean closing = new AtomicBoolean();
        private final AtomicBoolean terminating = new AtomicBoolean();
        private boolean outOpen;
        private boolean errOpen;

        /**
         * Instantiates a new process channel.
         *
         * @param startEvent the start event
         * @param process the process
         */
        private ProcessChannel(StartProcess startEvent, Process process) {
            super(channel(), newEventPipeline());
            this.startEvent = startEvent;
            this.process = process;

            // Register
            synchronized (ProcessManager.this) {
                if (channels.isEmpty()) {
                    registerAsGenerator();
                }
                channels.add(this);
            }

            // Using the channel for two streams requires more buffers.
            setByteBufferPool(new ManagedBufferPool<>(ManagedBuffer::new,
                () -> {
                    return ByteBuffer.allocate(4096);
                }, 4).setName(Components.objectName(this)
                    + ".upstream.byteBuffers"));
            running = true;

            if (executorService == null) {
                downPipeline = newEventPipeline();
            } else {
                downPipeline = newEventPipeline(executorService);
            }

            // (1) Opening, (2) ProcessStarted(Opened), (3) process I/O
            downPipeline().fire(Event.onCompletion(new Opening<Void>(),
                o -> downPipeline().fire(Event.onCompletion(
                    new ProcessStarted(startEvent), s -> startIO()), this)),
                this);
        }

        /**
         * Write the given data to the process (to its stdin).
         *
         * @param event the event
         * @throws IOException Signals that an I/O exception has occurred.
         */
        private void write(Output<ByteBuffer> event) throws IOException {
            var source = event.buffer().backingBuffer();
            process.getOutputStream().write(source.array(), source.position(),
                source.remaining());
        }

        private void startIO() {
            // Regrettably, the streams cannot be used with nio select.
            outOpen = true;
            executorService.submit(
                new InputStreamPipeline(process.getInputStream(), this,
                    downPipeline()).sendInputEvents().setEventAssociations(
                        Map.of(FileDescriptor.class, 1)));
            errOpen = true;
            executorService.submit(
                new InputStreamPipeline(process.getErrorStream(), this,
                    downPipeline()).sendInputEvents().setEventAssociations(
                        Map.of(FileDescriptor.class, 2)));
            process.onExit().thenAccept(p -> {
                running = false;
                logger
                    .fine(() -> "Process pid=" + p.toHandle().pid()
                        + " has exited with: " + p.exitValue());
                downPipeline()
                    .fire(new ProcessExited(startEvent,
                        p.exitValue()), this);
                maybeUnregister();
            });
        }

        /**
         * Close the stream to the process (its stdin) and optionally
         * terminates the process.
         *
         * @param event the event
         * @throws IOException Signals that an I/O exception has occurred.
         */
        private void close(Close event) {
            doClose(event.associated(Process.class, Object.class).isPresent());
        }

        private void doClose(boolean terminate) {
            if (!closing.getAndSet(true)) {
                try {
                    process.getOutputStream().close();
                } catch (IOException e) {
                    // Just trying to be nice
                    logger.log(Level.FINE, e,
                        () -> "Failed to close pipe to process (ignored): "
                            + e.getMessage());
                }
            }
            if (terminate && !terminating.getAndSet(true)) {
                process.toHandle().destroy();
            }
        }

        /**
         * Handles closed events from the process's output stream. 
         *
         * @param event the event
         */
        private void closed(Closed<?> event) {
            switch (event.associated(FileDescriptor.class, Integer.class)
                .orElse(-1)) {
            case 1:
                outOpen = false;
                break;
            case 2:
                errOpen = false;
                break;
            default:
                return;
            }
            maybeUnregister();
        }

        private void maybeUnregister() {
            if (!running && !outOpen && !errOpen) {
                synchronized (channels) {
                    channels.remove(this);
                    if (channels.isEmpty()) {
                        unregisterAsGenerator();
                    }
                }
            }
        }

        /**
         * Return the event that caused this channel to be created.
         *
         * @return the start event
         */
        public StartProcess startEvent() {
            return startEvent;
        }

        /**
         * Gets the down pipeline.
         *
         * @return the downPipeline
         */
        public EventPipeline downPipeline() {
            return downPipeline;
        }
    }
}
