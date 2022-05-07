/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2022 Michael N. Lipp
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

/**
 * I/O related components built on top of the core package.
 * 
 * I/O Subchannels
 * ---------------
 * 
 * A central concept introduced by this package are 
 * {@link org.jgrapes.io.IOSubchannel}s. They allow the 
 * {@link org.jgrapes.io.events.Input} and 
 * {@link org.jgrapes.io.events.Output} events from different connections
 * to be transferred on the same {@link org.jgrapes.core.Channel}. 
 * Details can be found in the description of the interface.
 * 
 * Connection Provider Components
 * ------------------------------
 * 
 * Connection provider components actively create a connection to some
 * other process or wait for other processes to create a connection that
 * is used to exchange information. Such components must (directly or 
 * indirectly) register as generator
 * (see {@link org.jgrapes.core.internal.ComponentVertex#registerAsGenerator})
 * while connections are active or the components are waiting for incoming 
 * connections to prevent premature termination of the framework.
 * 
 * When a new connection is established, these components usually create 
 * a {@link org.jgrapes.io.IOSubchannel} that represents the
 * connection with the other process. On this sub channel, the connection 
 * provider components should send events according to the following 
 * convention:
 * 
 *  * An {@link org.jgrapes.io.events.Opening} event to inform other
 *    interested components about the new I/O subchannel. A component's 
 *    handler might e.g. send a {@link org.jgrapes.io.events.SaveInput}
 *    event to log all input on this channel. Only after the completion
 *    of `Opening` event should the next event be sent on the I/O 
 *    subchannel.
 *    
 *  * An {@link org.jgrapes.io.events.Opened} event (or some derived class
 *    with more information about the type of connection that has been
 *    opened).
 *    
 *  * {@link org.jgrapes.io.events.Input} events as data from the client
 *    arrives.
 *    
 *  * Finally a {@link org.jgrapes.io.events.Close} event that indicates
 *    that the connection has terminated.
 * 
 * Using Buffers
 * -------------
 * 
 * The data associated with {@link org.jgrapes.io.events.Input} and
 * {@link org.jgrapes.io.events.Output} events is stored in NIO buffers.
 * JGrapes manages these buffers in pools. To support this, JGrapes
 * defines the class {@link org.jgrapes.io.util.ManagedBuffer}
 * that wraps a NIO buffer, adding the information required for 
 * managing it. 
 * 
 * Pooling is not done to avoid garbage collection, but for 
 * shaping streams of data. Imagine a pipeline where stage A produces
 * data much faster than stage B can handle it. If we allowed
 * arbitrary buffer allocation, it might happen that a lot of memory 
 * is used for buffers created by stage A and not yet consumed by stage B.
 * 
 * Using a buffer pool limits the the production rate of stage A without
 * reducing the overall performance. When all buffers are in use, stage A
 * has to wait until some data is consumed by stage B and a buffer is
 * freed. But as soon as this is the case, stage A can continue to 
 * produce data in parallel (unless you set the pool size to 1, of course).
 *
 */
package org.jgrapes.io;
