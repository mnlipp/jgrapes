/*******************************************************************************
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2018  Michael N. Lipp
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
 *******************************************************************************/

package org.jgrapes.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Optional;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.events.IOError;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedBufferPool;
import org.jgrapes.util.events.ConfigurationUpdate;

/**
 * A component that watches for new input on an
 * {@link InputStream}. If new input becomes
 * available, it is fired as {@link Input} event.
 * 
 * This component should only be used to monitor an
 * input stream that is available during the complete
 * lifetime of the application. A typical usage is
 * to make data from `System.in` available as events.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class InputStreamMonitor extends Component implements Runnable {

    @SuppressWarnings("PMD.SingularField")
    private Channel dataChannel;
    @SuppressWarnings("PMD.SingularField")
    private InputStream input;
    private boolean registered;
    private Thread runner;
    @SuppressWarnings("PMD.SingularField")
    private ManagedBufferPool<ManagedBuffer<ByteBuffer>, ByteBuffer> buffers;
    private int bufferSize = 2048;

    /**
     * Creates a new input stream monitor with its channel set to the given 
     * channel. The channel is also used for firing the {@link Input}
     * events.
     *
     * @param componentChannel the component channel
     * @param input the input stream
     * @param dataChannel the data channel
     */
    public InputStreamMonitor(
            Channel componentChannel, InputStream input, Channel dataChannel) {
        super(componentChannel);
        this.input = input;
        this.dataChannel = dataChannel;
    }

    /**
     * Creates a new input stream monitor with its channel set to the given 
     * channel. The channel is also used for firing the {@link Input}
     * events.
     *
     * @param componentChannel the component channel
     * @param input the input
     */
    public InputStreamMonitor(Channel componentChannel, InputStream input) {
        this(componentChannel, input, componentChannel);
    }

    /**
     * Sets the buffer size.
     *
     * @param bufferSize the buffer size
     * @return the input stream monitor for easy chaining
     */
    public InputStreamMonitor setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    /**
     * Returns the buffer size.
     *
     * @return the buffer size
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * The component can be configured with events that include
     * a path (see @link {@link ConfigurationUpdate#paths()})
     * that matches this components path (see {@link Manager#componentPath()}).
     * 
     * The following properties are recognized:
     * 
     * `bufferSize`
     * : See {@link #setBufferSize(int)}.
     * 
     * @param event the event
     */
    @Handler
    public void onConfigurationUpdate(ConfigurationUpdate event) {
        event.values(componentPath()).ifPresent(values -> {
            Optional.ofNullable(values.get("bufferSize")).ifPresent(
                value -> setBufferSize(Integer.parseInt(value)));
        });
    }

    /**
     * Starts a thread that continuously reads available
     * data from the input stream. 
     *
     * @param event the event
     */
    @Handler
    public void onStart(Start event) {
        synchronized (this) {
            if (runner != null) {
                return;
            }
            buffers = new ManagedBufferPool<>(ManagedBuffer::new,
                () -> {
                    return ByteBuffer.allocateDirect(bufferSize);
                }, 2);
            runner = new Thread(this, Components.simpleObjectName(this));
            // Because this cannot reliably be stopped, it doesn't prevent
            // shutdown.
            runner.setDaemon(true);
            runner.start();
        }
    }

    /**
     * Stops the thread that reads data from the input stream.
     * Note that the input stream is not closed.
     *
     * @param event the event
     * @throws InterruptedException the interrupted exception
     */
    @Handler(priority = -10_000)
    public void onStop(Stop event) throws InterruptedException {
        synchronized (this) {
            if (runner == null) {
                return;
            }
            runner.interrupt();
            synchronized (this) {
                if (registered) {
                    unregisterAsGenerator();
                    registered = false;
                }
            }
            runner = null;
        }
    }

    @Override
    public void run() {
        Thread.currentThread().setName(Components.simpleObjectName(this));
        try {
            synchronized (this) {
                registerAsGenerator();
                registered = true;
            }
            @SuppressWarnings("PMD.CloseResource")
            ReadableByteChannel inChannel = Channels.newChannel(input);
            while (!Thread.currentThread().isInterrupted()) {
                ManagedBuffer<ByteBuffer> buffer = buffers.acquire();
                int read = buffer.fillFromChannel(inChannel);
                boolean eof = read == -1;
                fire(Input.fromSink(buffer, eof), dataChannel);
                if (eof) {
                    break;
                }
            }
        } catch (InterruptedException e) { // NOPMD
            // Some called stop(), so what?
        } catch (IOException e) {
            fire(new IOError(null, e), channel());
        } finally {
            synchronized (this) {
                if (registered) {
                    unregisterAsGenerator();
                    registered = false;
                }
            }
        }
    }

}
