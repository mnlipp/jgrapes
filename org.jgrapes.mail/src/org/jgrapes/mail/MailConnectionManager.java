/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2022 Michael N. Lipp
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

package org.jgrapes.mail;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Manager;
import org.jgrapes.core.Subchannel.DefaultSubchannel;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.mail.events.OpenMailConnection;

/**
 * Provides a base class for mail components using connections.
 *
 * @param <O> the type of the open event
 * @param <C> the type of the channel
 */
public abstract class MailConnectionManager<O extends OpenMailConnection,
        C extends MailConnectionManager<O, C>.AbstractMailChannel>
        extends MailComponent {

    protected final Set<AbstractMailChannel> channels = new HashSet<>();
    private ExecutorService executorService;

    /**
     * Creates a new server using the given channel.
     * 
     * @param componentChannel the component's channel
     */
    public MailConnectionManager(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * Sets an executor service to be used by the event pipelines
     * that process the data from the network. Setting this
     * to an executor service with a limited number of threads
     * allows to control the maximum load from the network.
     * 
     * @param executorService the executorService to set
     * @return the TCP connection manager for easy chaining
     * @see Manager#newEventPipeline(ExecutorService)
     */
    public MailConnectionManager<O, C>
            setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    /**
     * Returns the executor service.
     *
     * @return the executorService
     */
    public ExecutorService executorService() {
        if (executorService == null) {
            return Components.defaultExecutorService();
        }
        return executorService;
    }

    /**
     * If channels are event generators, register the component as
     * generator upon the creation of the first channel and unregister
     * it when closing the last one.  
     *
     * @return true, if channels generate
     */
    protected abstract boolean channelsGenerate();

    /**
     * Stops the thread that is associated with this dispatcher.
     * 
     * @param event the event
     * @throws InterruptedException if the execution is interrupted
     */
    @Handler
    public void onStop(Stop event) throws InterruptedException {
        while (true) {
            AbstractMailChannel channel;
            synchronized (channels) {
                var itr = channels.iterator();
                if (!itr.hasNext()) {
                    return;
                }
                channel = itr.next();
            }
            channel.close();
        }
    }

    /**
     * A sub-channel for mail connections.
     */
    protected abstract class AbstractMailChannel
            extends DefaultSubchannel implements MailChannel {

        private final EventPipeline downPipeline;
        private final O openEvent;

        /**
         * Instantiates a new mail channel instance.
         *
         * @param event the main channel
         */
        public AbstractMailChannel(O event, Channel channel) {
            super(channel);
            synchronized (this) {
                if (channels.isEmpty()) {
                    registerAsGenerator();
                }
                channels.add(this);
            }
            openEvent = event;
            if (executorService == null) {
                downPipeline = newEventPipeline();
            } else {
                downPipeline = newEventPipeline(executorService);
            }
        }

        /**
         * Close the channel.
         */
        public void close() {
            synchronized (this) {
                channels.remove(this);
                if (channels.isEmpty()) {
                    unregisterAsGenerator();
                }
            }
        }

        /**
         * Returns the event that caused this connection to be opened.
         * 
         * @return the event
         */
        public O openEvent() {
            return openEvent;
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
