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
import org.jgrapes.core.Manager;
import org.jgrapes.core.Self;
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
import org.jgrapes.io.events.Output;
import org.jgrapes.io.events.Purge;
import org.jgrapes.io.util.AvailabilityListener;
import org.jgrapes.io.util.LinkedIOSubchannel;
import org.jgrapes.io.util.PermitsPool;
import org.jgrapes.net.events.Accepted;
import org.jgrapes.net.events.Ready;
import org.jgrapes.util.events.ConfigurationUpdate;

/**
 * Provides a TCP server. The server binds to the given address. If the
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
 * (see {@link #onOutput(Output, TcpChannel)}. When using this feature, 
 * make sure that connections are either short lived or the application
 * level components support the {@link Purge} event. Else, it may become
 * impossible to establish new connections.
 */
@SuppressWarnings({ "PMD.ExcessiveImports", "PMD.ExcessivePublicCount",
    "PMD.NcssCount", "PMD.EmptyCatchBlock", "PMD.AvoidDuplicateLiterals",
    "PMD.ExcessiveClassLength" })
public class TcpServer extends TcpConnectionManager implements NioHandler {

    private InetSocketAddress serverAddress;
    private ServerSocketChannel serverSocketChannel;
    private boolean closing;
    private int backlog;
    private PermitsPool connLimiter;
    private Registration registration;
    private Purger purger;
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
            setDaemon(true);
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
            "PMD.DataflowAnomalyAnalysis" })
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
                    List<TcpChannel> candidates;
                    synchronized (channels) {
                        candidates = new ArrayList<>(channels);
                    }
                    long purgeableSince
                        = System.currentTimeMillis() - minimumPurgeableTime;
                    candidates = candidates.stream()
                        .filter(channel -> channel.isPurgeable()
                            && channel.becamePurgeableAt() < purgeableSince)
                        .sorted(new Comparator<TcpChannel>() {
                            @Override
                            @SuppressWarnings("PMD.ShortVariable")
                            public int compare(TcpChannel c1, TcpChannel c2) {
                                if (c1.becamePurgeableAt() < c2
                                    .becamePurgeableAt()) {
                                    return 1;
                                }
                                if (c1.becamePurgeableAt() > c2
                                    .becamePurgeableAt()) {
                                    return -1;
                                }
                                return 0;
                            }
                        })
                        .collect(Collectors.toList());
                    for (TcpChannel channel : candidates) {
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
    public TcpServer() {
        this(Channel.SELF);
    }

    /**
     * Creates a new server using the given channel.
     * 
     * @param componentChannel the component's channel
     */
    public TcpServer(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * Sets the address to bind to. If none is set, the address and port
     * are assigned automatically.
     * 
     * @param serverAddress the address to bind to
     * @return the TCP server for easy chaining
     */
    public TcpServer setServerAddress(InetSocketAddress serverAddress) {
        this.serverAddress = serverAddress;
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
        });
    }

    /**
     * Returns the server address. Before starting, the address is the
     * address set with {@link #setServerAddress(InetSocketAddress)}. After
     * starting the address is obtained from the created socket.  
     * 
     * @return the serverAddress
     */
    public InetSocketAddress serverAddress() {
        try {
            return serverSocketChannel == null ? serverAddress
                : (InetSocketAddress) serverSocketChannel.getLocalAddress();
        } catch (IOException e) {
            return serverAddress;
        }
    }

    /**
     * Sets the backlog size.
     * 
     * @param backlog the backlog to set
     * @return the TCP server for easy chaining
     */
    public TcpServer setBacklog(int backlog) {
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
     * @return the TCP server for easy chaining
     */
    public TcpServer setConnectionLimiter(PermitsPool connectionLimiter) {
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
     * @return the tcp server
     */
    public TcpServer setMinimalPurgeableTime(long millis) {
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
        serverSocketChannel = ServerSocketChannel.open();
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
            purger = new Purger();
            purger.start();
            fire(new Ready(serverSocketChannel.getLocalAddress()));
            return;
        }
        if (handler instanceof TcpChannel) {
            TcpChannel channel = (TcpChannel) handler;
            channel.downPipeline()
                .fire(new Accepted(channel.nioChannel().getLocalAddress(),
                    channel.nioChannel().getRemoteAddress(), false,
                    Collections.emptyList()), channel);
            channel.registrationComplete(event.event());
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
                SocketChannel socketChannel = serverSocketChannel.accept();
                if (socketChannel == null) {
                    // "False alarm"
                    if (connLimiter != null) {
                        connLimiter.release();
                    }
                    return;
                }
                channels.add(new TcpChannel(socketChannel));
            } catch (IOException e) {
                fire(new IOError(null, e));
            }
        }
    }

    @Override
    protected boolean removeChannel(TcpChannel channel) {
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
        boolean subOnly = true;
        for (Channel channel : event.channels()) {
            if (channel instanceof TcpChannel) {
                if (channels.contains(channel)) {
                    ((TcpChannel) channel).close();
                }
            } else {
                subOnly = false;
            }
        }
        if (subOnly || !serverSocketChannel.isOpen()) {
            // Closed already
            fire(new Closed());
            return;
        }
        synchronized (channels) {
            closing = true;
            // Copy to avoid concurrent modification exception
            Set<TcpChannel> conns = new HashSet<>(channels);
            for (TcpChannel conn : conns) {
                conn.close();
            }
            while (!channels.isEmpty()) {
                channels.wait();
            }
        }
        serverSocketChannel.close();
        purger.interrupt();
        closing = false;
        fire(new Closed());
    }

    /**
     * Shuts down the server by firing a {@link Close} using the
     * server as channel. Note that this automatically results
     * in closing all open connections by the runtime system
     * and thus in {@link Closed} events on all subchannels.
     * 
     * @param event the event
     */
    @Handler(priority = -1000)
    public void onStop(Stop event) {
        if (closing || !serverSocketChannel.isOpen()) {
            return;
        }
        newSyncEventPipeline().fire(new Close(), this);
    }

    /**
     * The Interface of the TcpServer MXBean.
     */
    public interface TcpServerMXBean {

        /**
         * The Class ChannelInfo.
         */
        class ChannelInfo {

            private final TcpChannel channel;

            /**
             * Instantiates a new channel info.
             *
             * @param channel the channel
             */
            public ChannelInfo(TcpChannel channel) {
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
         * Gets the port.
         *
         * @return the port
         */
        int getPort();

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
     * The Class TcpServerInfo.
     */
    public static class TcpServerInfo implements TcpServerMXBean {

        private static MBeanServer mbs
            = ManagementFactory.getPlatformMBeanServer();

        private ObjectName mbeanName;
        private final WeakReference<TcpServer> serverRef;

        /**
         * Instantiates a new tcp server info.
         *
         * @param server the server
         */
        @SuppressWarnings({ "PMD.EmptyCatchBlock",
            "PMD.AvoidCatchingGenericException" })
        public TcpServerInfo(TcpServer server) {
            serverRef = new WeakReference<>(server);
            try {
                int port = server.serverAddress().getPort();
                mbeanName = new ObjectName("org.jgrapes.io:type="
                    + TcpServer.class.getSimpleName() + ",name="
                    + ObjectName.quote(Components.objectName(server)
                        + " (:" + port + ")"));
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
        public Optional<TcpServer> server() {
            TcpServer server = serverRef.get();
            if (server == null) {
                try {
                    mbs.unregisterMBean(mbeanName);
                } catch (Exception e) {
                    // Should work.
                }
            }
            return Optional.ofNullable(server);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jgrapes.net.TcpServer.TcpServerMXBean#getComponentPath()
         */
        @Override
        public String getComponentPath() {
            return server().map(mgr -> mgr.componentPath()).orElse("<removed>");
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jgrapes.net.TcpServer.TcpServerMXBean#getPort()
         */
        @Override
        public int getPort() {
            return server().map(server -> server
                .serverAddress().getPort()).orElse(0);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jgrapes.net.TcpServer.TcpServerMXBean#getChannelCount()
         */
        @Override
        public int getChannelCount() {
            return server().map(server -> server.channels.size()).orElse(0);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jgrapes.net.TcpServer.TcpServerMXBean#getChannels()
         */
        @Override
        @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
        public SortedMap<String, ChannelInfo> getChannels() {
            return server().map(server -> {
                SortedMap<String, ChannelInfo> result = new TreeMap<>();
                for (TcpChannel channel : server.channels) {
                    result.put(channel.nioChannel().socket()
                        .getRemoteSocketAddress().toString(),
                        new ChannelInfo(channel));
                }
                return result;
            }).orElse(Collections.emptySortedMap());
        }
    }

    /**
     * An MBean interface for getting information about the TCP servers
     * and established connections.
     */
    public interface TcpServerSummaryMXBean {

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
        Set<TcpServerMXBean> getServers();
    }

    /**
     * The MBeanView.
     */
    private static class MBeanView implements TcpServerSummaryMXBean {
        private static Set<TcpServerInfo> serverInfos = new HashSet<>();

        /**
         * Adds the server to the reported servers.
         *
         * @param server the server
         */
        public static void addServer(TcpServer server) {
            synchronized (serverInfos) {
                serverInfos.add(new TcpServerInfo(server));
            }
        }

        /**
         * Returns the infos.
         *
         * @return the sets the
         */
        private Set<TcpServerInfo> infos() {
            Set<TcpServerInfo> expired = new HashSet<>();
            synchronized (serverInfos) {
                for (TcpServerInfo serverInfo : serverInfos) {
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
        public Set<TcpServerMXBean> getServers() {
            return (Set<TcpServerMXBean>) (Object) infos();
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
                + TcpServer.class.getSimpleName() + "s");
            mbs.registerMBean(new MBeanView(), mxbeanName);
        } catch (MalformedObjectNameException | InstanceAlreadyExistsException
                | MBeanRegistrationException | NotCompliantMBeanException e) {
            // Does not happen
        }
    }

}
