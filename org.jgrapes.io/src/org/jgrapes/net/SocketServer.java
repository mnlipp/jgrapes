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

package org.jgrapes.net;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.Self;
import org.jgrapes.core.Subchannel;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Error;
import org.jgrapes.core.events.Start;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.NioHandler;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.IOError;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.NioRegistration;
import org.jgrapes.io.events.NioRegistration.Registration;
import org.jgrapes.io.events.Opening;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.events.Purge;
import org.jgrapes.io.util.AvailabilityListener;
import org.jgrapes.io.util.LinkedIOSubchannel;
import org.jgrapes.io.util.PermitsPool;
import org.jgrapes.net.events.Accepted;
import org.jgrapes.net.events.Ready;
import org.jgrapes.util.events.ConfigurationUpdate;

/**
 * Provides a socket server. The server binds to the given address. If the
 * address is {@code null}, address and port are automatically assigned.
 * The port may be overwritten by a configuration event
 * (see {@link #onConfigurationUpdate(ConfigurationUpdate)}).
 * 
 * For each established connection, the server creates a new
 * {@link LinkedIOSubchannel}. The servers basic operation is to
 * fire {@link Input} (and {@link Closed}) events on the
 * appropriate subchannel in response to data received from the
 * network and to handle {@link Output} (and {@link Close}) events 
 * on the subchannel and forward the information to the network
 * connection.
 * 
 * The server supports limiting the number of concurrent connections
 * with a {@link PermitsPool}. If such a pool is set as connection
 * limiter (see {@link #setConnectionLimiter(PermitsPool)}), a
 * permit is acquired for each new connection attempt. If no more
 * permits are available, the server sends a {@link Purge} event on
 * each channel that is purgeable for at least the time span
 * set with {@link #setMinimalPurgeableTime(long)}. Purgeability 
 * is derived from the end of record flag of {@link Output} events
 * (see {@link #onOutput(Output, SocketChannelImpl)}. When using this feature, 
 * make sure that connections are either short lived or the application
 * level components support the {@link Purge} event. Else, it may become
 * impossible to establish new connections.
 */
@SuppressWarnings({ "PMD.ExcessiveImports", "PMD.ExcessivePublicCount",
    "PMD.NcssCount", "PMD.EmptyCatchBlock", "PMD.AvoidDuplicateLiterals",
    "PMD.ExcessiveClassLength", "PMD.CouplingBetweenObjects" })
public class SocketServer extends SocketConnectionManager
        implements NioHandler {

    private SocketAddress serverAddress;
    private ServerSocketChannel serverSocketChannel;
    private boolean closing;
    private int backlog;
    private PermitsPool connLimiter;
    private Registration registration;
    @SuppressWarnings("PMD.SingularField")
    private Thread purger;
    private long minimumPurgeableTime;

    /**
     * The purger thread.
     */
    private class Purger extends Thread implements AvailabilityListener {

        private boolean permitsAvailable = true;

        /**
         * Instantiates a new purger.
         */
        public Purger() {
            setName(Components.simpleObjectName(this));
        }

        @Override
        public void availabilityChanged(PermitsPool pool, boolean available) {
            if (registration == null) {
                return;
            }
            synchronized (this) {
                permitsAvailable = available;
                registration.updateInterested(
                    permitsAvailable ? SelectionKey.OP_ACCEPT : 0);
                if (!permitsAvailable) {
                    this.notifyAll();
                }
            }
        }

        @Override
        @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops",
            "PMD.DataflowAnomalyAnalysis", "PMD.CognitiveComplexity" })
        public void run() {
            if (connLimiter == null) {
                return;
            }
            try {
                connLimiter.addListener(this);
                while (serverSocketChannel.isOpen()) {
                    synchronized (this) {
                        while (permitsAvailable) {
                            wait();
                        }
                    }
                    // Copy to avoid ConcurrentModificationException
                    List<SocketChannelImpl> candidates;
                    synchronized (channels) {
                        candidates = new ArrayList<>(channels);
                    }
                    long purgeableSince
                        = System.currentTimeMillis() - minimumPurgeableTime;
                    candidates = candidates.stream()
                        .filter(channel -> channel.isPurgeable()
                            && channel.purgeableSince() < purgeableSince)
                        .sorted(new Comparator<>() {
                            @Override
                            @SuppressWarnings("PMD.ShortVariable")
                            public int compare(SocketChannelImpl c1,
                                    SocketChannelImpl c2) {
                                if (c1.purgeableSince() < c2
                                    .purgeableSince()) {
                                    return 1;
                                }
                                if (c1.purgeableSince() > c2
                                    .purgeableSince()) {
                                    return -1;
                                }
                                return 0;
                            }
                        })
                        .collect(Collectors.toList());
                    for (SocketChannelImpl channel : candidates) {
                        // Sorting may have taken time...
                        if (!channel.isPurgeable()) {
                            continue;
                        }
                        channel.downPipeline().fire(new Purge(), channel);
                        // Continue only as long as necessary
                        if (permitsAvailable) {
                            break;
                        }
                    }
                    sleep(1000);
                }
            } catch (InterruptedException e) {
                // Fall through
            } finally {
                connLimiter.removeListener(this);
            }
        }

    }

    /**
     * Creates a new server, using itself as component channel. 
     */
    public SocketServer() {
        this(Channel.SELF);
    }

    /**
     * Creates a new server using the given channel.
     * 
     * @param componentChannel the component's channel
     */
    public SocketServer(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * Sets the address to bind to. If none is set, the address and port
     * are assigned automatically.
     * 
     * @param serverAddress the address to bind to
     * @return the socket server for easy chaining
     */
    public SocketServer setServerAddress(SocketAddress serverAddress) {
        this.serverAddress = serverAddress;
        return this;
    }

    @Override
    public SocketServer setBufferSize(int size) {
        super.setBufferSize(size);
        return this;
    }

    /**
     * The component can be configured with events that include
     * a path (see @link {@link ConfigurationUpdate#paths()})
     * that matches this components path (see {@link Manager#componentPath()}).
     * 
     * The following properties are recognized:
     * 
     * `hostname`
     * : If given, is used as first parameter for 
     *   {@link InetSocketAddress#InetSocketAddress(String, int)}.
     * 
     * `port`
     * : If given, is used as parameter for 
     *   {@link InetSocketAddress#InetSocketAddress(String, int)} 
     *   or {@link InetSocketAddress#InetSocketAddress(int)}, 
     *   depending on whether a host name is specified. Defaults to "0".
     *   
     * `backlog`
     * : See {@link #setBacklog(int)}.
     * 
     * `bufferSize`
     * : See {@link #setBufferSize(int)}.
     * 
     * `maxConnections`
     * : Calls {@link #setConnectionLimiter} with a
     *   {@link PermitsPool} of the specified size.
     * 
     * `minimalPurgeableTime`
     * : See {@link #setMinimalPurgeableTime(long)}.
     * 
     * @param event the event
     */
    @Handler
    @SuppressWarnings("PMD.ConfusingTernary")
    public void onConfigurationUpdate(ConfigurationUpdate event) {
        event.values(componentPath()).ifPresent(values -> {
            String hostname = values.get("hostname");
            if (hostname != null) {
                setServerAddress(new InetSocketAddress(hostname,
                    Integer.parseInt(values.getOrDefault("port", "0"))));
            } else if (values.containsKey("port")) {
                setServerAddress(new InetSocketAddress(
                    Integer.parseInt(values.get("port"))));
            }
            Optional.ofNullable(values.get("backlog")).ifPresent(
                value -> setBacklog(Integer.parseInt(value)));
            Optional.ofNullable(values.get("bufferSize")).ifPresent(
                value -> setBufferSize(Integer.parseInt(value)));
            Optional.ofNullable(values.get("maxConnections"))
                .map(Integer::parseInt).map(PermitsPool::new)
                .ifPresent(this::setConnectionLimiter);
            Optional.ofNullable(values.get("minimalPurgeableTime"))
                .map(Long::parseLong).ifPresent(this::setMinimalPurgeableTime);
        });
    }

    /**
     * Returns the server address. Before starting, the address is the
     * address set with {@link #setServerAddress(InetSocketAddress)}. After
     * starting the address is obtained from the created socket.  
     * 
     * @return the serverAddress
     */
    public SocketAddress serverAddress() {
        try {
            return serverSocketChannel == null ? serverAddress
                : serverSocketChannel.getLocalAddress();
        } catch (IOException e) {
            return serverAddress;
        }
    }

    /**
     * Sets the backlog size.
     * 
     * @param backlog the backlog to set
     * @return the socket server for easy chaining
     */
    public SocketServer setBacklog(int backlog) {
        this.backlog = backlog;
        return this;
    }

    /**
     * Return the configured backlog size.
     *
     * @return the backlog
     */
    public int backlog() {
        return backlog;
    }

    /**
     * Sets a permit "pool". A new connection is created only if a permit
     * can be obtained from the pool. 
     * 
     * A connection limiter must be set before starting the component.
     * 
     * @param connectionLimiter the connection pool to set
     * @return the socket server for easy chaining
     */
    public SocketServer setConnectionLimiter(PermitsPool connectionLimiter) {
        this.connLimiter = connectionLimiter;
        return this;
    }

    /**
     * Returns the connection limiter.
     *
     * @return the connection Limiter
     */
    public PermitsPool getConnectionLimiter() {
        return connLimiter;
    }

    /**
     * Sets a minimal time that a connection must be purgeable (idle)
     * before it may be purged.
     *
     * @param millis the millis
     * @return the socket server
     */
    public SocketServer setMinimalPurgeableTime(long millis) {
        this.minimumPurgeableTime = millis;
        return this;
    }

    /**
     * Gets the minimal purgeable time.
     *
     * @return the minimal purgeable time
     */
    public long getMinimalPurgeableTime() {
        return minimumPurgeableTime;
    }

    /**
     * Starts the server.
     * 
     * @param event the start event
     * @throws IOException if an I/O exception occurred
     */
    @Handler
    public void onStart(Start event) throws IOException {
        closing = false;
        if (serverAddress instanceof UnixDomainSocketAddress) {
            serverSocketChannel
                = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        } else {
            serverSocketChannel = ServerSocketChannel.open();
        }
        serverSocketChannel.bind(serverAddress, backlog);
        MBeanView.addServer(this);
        fire(new NioRegistration(this, serverSocketChannel,
            SelectionKey.OP_ACCEPT, this), Channel.BROADCAST);
    }

    /**
     * Handles the successful channel registration.
     *
     * @param event the event
     * @throws InterruptedException the interrupted exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler(channels = Self.class)
    public void onRegistered(NioRegistration.Completed event)
            throws InterruptedException, IOException {
        NioHandler handler = event.event().handler();
        if (handler == this) {
            if (event.event().get() == null) {
                fire(new Error(event,
                    "Registration failed, no NioDispatcher?"));
                return;
            }
            registration = event.event().get();
            purger = (Components.useVirtualThreads() ? Thread.ofVirtual()
                : Thread.ofPlatform()).start(new Purger());
            fire(new Ready(serverSocketChannel.getLocalAddress()));
            return;
        }
        if (handler instanceof SocketChannelImpl channel
            && channels.contains(channel)) {
            var accepted = new Accepted(channel.nioChannel().getLocalAddress(),
                channel.nioChannel().getRemoteAddress(), false,
                Collections.emptyList());
            var registration = event.event().get();
            // (1) Opening, (2) Accepted, (3) process input
            channel.downPipeline().fire(Event.onCompletion(new Opening<Void>(),
                e -> {
                    channel.downPipeline().fire(accepted, channel);
                    channel.registrationComplete(registration);
                }), channel);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.io.NioSelectable#handleOps(int)
     */
    @Override
    public void handleOps(int ops) {
        if ((ops & SelectionKey.OP_ACCEPT) == 0 || closing) {
            return;
        }
        synchronized (channels) {
            if (connLimiter != null && !connLimiter.tryAcquire()) {
                return;
            }
            try {
                @SuppressWarnings("PMD.CloseResource")
                SocketChannel socketChannel = serverSocketChannel.accept();
                if (socketChannel == null) {
                    // "False alarm"
                    if (connLimiter != null) {
                        connLimiter.release();
                    }
                    return;
                }
                new SocketChannelImpl(null, socketChannel);
            } catch (IOException e) {
                fire(new IOError(null, e));
            }
        }
    }

    @Override
    protected boolean removeChannel(SocketChannelImpl channel) {
        synchronized (channels) {
            if (!channels.remove(channel)) {
                // Closed already
                return false;
            }
            // In case the server is shutting down
            channels.notifyAll();
        }
        if (connLimiter != null) {
            connLimiter.release();
        }
        return true;
    }

    /**
     * Shuts down the server or one of the connections to the server.
     *
     * @param event the event
     * @throws IOException if an I/O exception occurred
     * @throws InterruptedException if the execution was interrupted
     */
    @Handler
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public void onClose(Close event) throws IOException, InterruptedException {
        boolean closeServer = false;
        for (Channel channel : event.channels()) {
            if (channels.contains(channel)) {
                ((SocketChannelImpl) channel).close();
                continue;
            }
            if (channel instanceof Subchannel) {
                // Some subchannel that we're not interested in.
                continue;
            }
            // Close event on "main" channel
            closeServer = true;
        }
        if (!closeServer) {
            // Only connection(s) were to be closed.
            return;
        }
        if (!serverSocketChannel.isOpen()) {
            // Closed already
            fire(new Closed<Void>());
            return;
        }
        synchronized (channels) {
            closing = true;
            // Copy to avoid concurrent modification exception
            Set<SocketChannelImpl> conns = new HashSet<>(channels);
            for (SocketChannelImpl conn : conns) {
                conn.close();
            }
            while (!channels.isEmpty()) {
                channels.wait();
            }
        }
        serverSocketChannel.close();
        purger.interrupt();
        closing = false;
        fire(new Closed<Void>());
    }

    /**
     * Shuts down the server by firing a {@link Close} using the
     * server as channel. Note that this automatically results
     * in closing all open connections by the runtime system
     * and thus in {@link Closed} events on all subchannels.
     * 
     * @param event the event
     * @throws InterruptedException 
     */
    @Handler(priority = -1000)
    public void onStop(Stop event) throws InterruptedException {
        if (closing || !serverSocketChannel.isOpen()) {
            return;
        }
        newEventPipeline().fire(new Close(), this).get();
    }

    /**
     * The Interface of the SocketServer MXBean.
     */
    public interface SocketServerMXBean {

        /**
         * The Class ChannelInfo.
         */
        class ChannelInfo {

            private final SocketChannelImpl channel;

            /**
             * Instantiates a new channel info.
             *
             * @param channel the channel
             */
            public ChannelInfo(SocketChannelImpl channel) {
                this.channel = channel;
            }

            /**
             * Checks if is purgeable.
             *
             * @return true, if is purgeable
             */
            public boolean isPurgeable() {
                return channel.isPurgeable();
            }

            /**
             * Gets the downstream pool.
             *
             * @return the downstream pool
             */
            public String getDownstreamPool() {
                return channel.readBuffers().name();
            }

            /**
             * Gets the upstream pool.
             *
             * @return the upstream pool
             */
            public String getUpstreamPool() {
                return channel.byteBufferPool().name();
            }
        }

        /**
         * Gets the component path.
         *
         * @return the component path
         */
        String getComponentPath();

        /**
         * Gets the channel count.
         *
         * @return the channel count
         */
        int getChannelCount();

        /**
         * Gets the channels.
         *
         * @return the channels
         */
        SortedMap<String, ChannelInfo> getChannels();

    }

    /**
     * The Class SocketServerInfo.
     */
    public static class SocketServerInfo implements SocketServerMXBean {

        private static MBeanServer mbs
            = ManagementFactory.getPlatformMBeanServer();

        private ObjectName mbeanName;
        private final WeakReference<SocketServer> serverRef;

        /**
         * Instantiates a new socket server info.
         *
         * @param server the server
         */
        @SuppressWarnings({ "PMD.EmptyCatchBlock",
            "PMD.AvoidCatchingGenericException",
            "PMD.ConstructorCallsOverridableMethod" })
        public SocketServerInfo(SocketServer server) {
            serverRef = new WeakReference<>(server);
            try {
                String endPoint = "";
                if (server.serverAddress instanceof InetSocketAddress addr) {
                    endPoint = " (" + addr.getHostName() + ":" + addr.getPort()
                        + ")";
                } else if (server.serverAddress instanceof UnixDomainSocketAddress addr) {
                    endPoint = " (" + addr.getPath() + ")";
                }
                mbeanName = new ObjectName("org.jgrapes.io:type="
                    + SocketServer.class.getSimpleName() + ",name="
                    + ObjectName
                        .quote(Components.objectName(server) + endPoint));
            } catch (MalformedObjectNameException e) {
                // Should not happen
            }
            try {
                mbs.unregisterMBean(mbeanName);
            } catch (Exception e) {
                // Just in case, should not work
            }
            try {
                mbs.registerMBean(this, mbeanName);
            } catch (InstanceAlreadyExistsException | MBeanRegistrationException
                    | NotCompliantMBeanException e) {
                // Have to live with that
            }
        }

        /**
         * Server.
         *
         * @return the optional
         */
        @SuppressWarnings({ "PMD.AvoidCatchingGenericException",
            "PMD.EmptyCatchBlock" })
        public Optional<SocketServer> server() {
            SocketServer server = serverRef.get();
            if (server == null) {
                try {
                    mbs.unregisterMBean(mbeanName);
                } catch (Exception e) {
                    // Should work.
                }
            }
            return Optional.ofNullable(server);
        }

        @Override
        public String getComponentPath() {
            return server().map(mgr -> mgr.componentPath()).orElse("<removed>");
        }

        @Override
        public int getChannelCount() {
            return server().map(server -> server.channels.size()).orElse(0);
        }

        @Override
        @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
        public SortedMap<String, ChannelInfo> getChannels() {
            return server().map(server -> {
                SortedMap<String, ChannelInfo> result = new TreeMap<>();
                for (SocketChannelImpl channel : server.channels) {
                    result.put(channel.nioChannel().socket()
                        .getRemoteSocketAddress().toString(),
                        new ChannelInfo(channel));
                }
                return result;
            }).orElse(Collections.emptySortedMap());
        }
    }

    /**
     * An MBean interface for getting information about the socket servers
     * and established connections.
     */
    public interface SocketServerSummaryMXBean {

        /**
         * Gets the connections per server statistics.
         *
         * @return the connections per server statistics
         */
        IntSummaryStatistics getConnectionsPerServerStatistics();

        /**
         * Gets the servers.
         *
         * @return the servers
         */
        Set<SocketServerMXBean> getServers();
    }

    /**
     * The MBeanView.
     */
    private static final class MBeanView implements SocketServerSummaryMXBean {
        private static Set<SocketServerInfo> serverInfos = new HashSet<>();

        /**
         * Adds the server to the reported servers.
         *
         * @param server the server
         */
        public static void addServer(SocketServer server) {
            synchronized (serverInfos) {
                serverInfos.add(new SocketServerInfo(server));
            }
        }

        /**
         * Returns the infos.
         *
         * @return the sets the
         */
        private Set<SocketServerInfo> infos() {
            Set<SocketServerInfo> expired = new HashSet<>();
            synchronized (serverInfos) {
                for (SocketServerInfo serverInfo : serverInfos) {
                    if (!serverInfo.server().isPresent()) {
                        expired.add(serverInfo);
                    }
                }
                serverInfos.removeAll(expired);
            }
            return serverInfos;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Set<SocketServerMXBean> getServers() {
            return (Set<SocketServerMXBean>) (Object) infos();
        }

        @Override
        public IntSummaryStatistics getConnectionsPerServerStatistics() {
            return infos().stream().map(info -> info.server().get())
                .filter(ref -> ref != null).collect(
                    Collectors.summarizingInt(srv -> srv.channels.size()));
        }
    }

    static {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName mxbeanName = new ObjectName("org.jgrapes.io:type="
                + SocketServer.class.getSimpleName() + "s");
            mbs.registerMBean(new MBeanView(), mxbeanName);
        } catch (MalformedObjectNameException | InstanceAlreadyExistsException
                | MBeanRegistrationException | NotCompliantMBeanException e) {
            // Does not happen
        }
    }

}
