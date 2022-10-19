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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.jgrapes.core.Channel;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Manager;
import org.jgrapes.core.Subchannel.DefaultSubchannel;
import org.jgrapes.mail.events.OpenMailConnection;

/**
 * Provides a base class for mail components using connections.
 */
public abstract class MailConnectionManager<
        C extends MailStoreMonitor.AbstractMailChannel>
        extends MailComponent {

    protected final Set<C> channels = new HashSet<>();
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
    public MailConnectionManager<C>
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
        return executorService;
    }

    /**
     * A sub-channel for mail connections.
     */
    protected abstract class AbstractMailChannel extends DefaultSubchannel
            implements MailChannel {

        private final EventPipeline downPipeline;
        private final OpenMailConnection openEvent;

        /**
         * Instantiates a new mail channel instance.
         *
         * @param event the main channel
         */
        public AbstractMailChannel(OpenMailConnection event, Channel channel) {
            super(channel);
            openEvent = event;
            if (executorService == null) {
                downPipeline = newEventPipeline();
            } else {
                downPipeline = newEventPipeline(executorService);
            }
        }

        /**
         * Returns the event that caused this connection to be opened.
         * 
         * @return the event
         */
        public Optional<OpenMailConnection> openEvent() {
            return Optional.ofNullable(openEvent);
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
