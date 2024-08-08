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

package org.jgrapes.io.util;

import java.beans.ConstructorProperties;
import java.lang.management.ManagementFactory;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.Buffer;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.jgrapes.core.Components;
import org.jgrapes.core.Components.Timer;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Output;

/**
 * A queue based buffer pool. Using buffers from a pool is an important
 * feature for limiting the computational resources for an {@link IOSubchannel}.
 * A producer of {@link Output} events that simply creates its own buffers
 * may produce and enqueue a large number of events that are not consumed
 * as fast as they are produced. 
 * 
 * Using a buffer pool with a typical size of two synchronizes the 
 * producer and the consumers of events nicely. The producer
 * (thread) holds one buffer and fills it, the consumer (thread) holds 
 * the other buffer and works with its content. If the producer finishes
 * before the consumer, it has to stop until the consumer has processed 
 * previous event and releases the buffer. The consumer can continue
 * without delay, because the data has already been prepared and enqueued
 * as the next event.
 * 
 * One of the biggest problems when using a pool can be to identify 
 * leaking buffers, i.e. buffers that are not properly returned to the pool.
 * This implementation therefore tracks all created buffers 
 * (with a small overhead) and logs a warning if a buffer is no longer
 * used (referenced) but has not been returned to the pool. If the
 * log level for {@link ManagedBufferPool} is set to {@link Level#FINE},
 * the warning also includes a stack trace of the call to {@link #acquire()}
 * that handed out the buffer. Providing this information in addition 
 * obviously requires a larger overhead and is therefore limited to the
 * finer log levels.
 *
 * @param <W> the type of the wrapped (managed) buffer
 * @param <T> the type of the content buffer that is wrapped
 */
@SuppressWarnings({ "PMD.ExcessiveImports", "PMD.NcssCount",
    "PMD.EmptyCatchBlock", "PMD.CouplingBetweenObjects" })
public class ManagedBufferPool<W extends ManagedBuffer<T>, T extends Buffer>
        implements BufferCollector<W> {

    @SuppressWarnings("PMD.FieldNamingConventions")
    protected final Logger logger
        = Logger.getLogger(ManagedBufferPool.class.getName());

    private static long defaultDrainDelay = 1500;
    private static long acquireWarningLimit = 1000;

    private String name = Components.objectName(this);
    private BiFunction<T, BufferCollector<W>, W> wrapper;
    private Supplier<T> bufferFactory;
    private BufferMonitor bufferMonitor;
    private BlockingQueue<W> queue;
    private int bufferSize = -1;
    private int preservedBufs;
    private int maximumBufs;
    private AtomicInteger createdBufs;
    private long drainDelay = -1;
    private final AtomicReference<Timer> idleTimer
        = new AtomicReference<>(null);

    /**
     * Sets the default delay after which buffers are removed from
     * the pool. The default value is 1500ms.
     * 
     * @param delay the delay in ms
     */
    public static void setDefaultDrainDelay(long delay) {
        defaultDrainDelay = delay;
    }

    /**
     * Returns the default drain delay.
     * 
     * @return the delay
     */
    public static long defaultDrainDelay() {
        return defaultDrainDelay;
    }

    /**
     * Create a pool that contains a varying number of (wrapped) buffers.
     * The pool is initially empty. When buffers are requested and none 
     * are left in the pool, new buffers are created up to the given 
     * upper limit. Recollected buffers are put in the pool until it holds
     * the number specified by the lower threshold. Any additional 
     * recollected buffers are discarded. 
     * 
     * @param wrapper the function that converts buffers to managed buffers
     * @param bufferFactory a function that creates a new buffer
     * @param lowerThreshold the number of buffers kept in the pool
     * @param upperLimit the maximum number of buffers
     */
    public ManagedBufferPool(BiFunction<T, BufferCollector<W>, W> wrapper,
            Supplier<T> bufferFactory, int lowerThreshold, int upperLimit) {
        this.wrapper = wrapper;
        this.bufferFactory = bufferFactory;
        preservedBufs = lowerThreshold;
        maximumBufs = upperLimit;
        createdBufs = new AtomicInteger();
        queue = new ArrayBlockingQueue<>(lowerThreshold);
        bufferMonitor = new BufferMonitor(upperLimit);
        MBeanView.addPool(this);
    }

    /**
     * Create a pool that keeps up to the given number of (wrapped) buffers
     * in the pool and also uses that number as upper limit.
     * 
     * @param wrapper the function that converts buffers to managed buffers
     * @param bufferFactory a function that creates a new buffer
     * @param buffers the number of buffers
     */
    public ManagedBufferPool(BiFunction<T, BufferCollector<W>, W> wrapper,
            Supplier<T> bufferFactory, int buffers) {
        this(wrapper, bufferFactory, buffers, buffers);
    }

    /**
     * Sets a name for this pool (to be used in status reports).
     * 
     * @param name the name
     * @return the object for easy chaining
     */
    public ManagedBufferPool<W, T> setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Returns the name of this pool.
     * 
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * Sets the delay after which buffers are removed from
     * the pool.
     * 
     * @param delay the delay
     * @return the object for easy chaining
     */
    public ManagedBufferPool<W, T> setDrainDelay(long delay) {
        this.drainDelay = delay;
        return this;
    }

    private W createBuffer() {
        createdBufs.incrementAndGet();
        W buffer = wrapper.apply(this.bufferFactory.get(), this);
        bufferMonitor.put(buffer, new BufferProperties());
        bufferSize = buffer.capacity();
        return buffer;
    }

    /**
     * Removes the buffer from the pool.
     * 
     * @param buffer the buffer to remove
     */
    @SuppressWarnings("PMD.GuardLogStatement")
    private void removeBuffer(W buffer) {
        createdBufs.decrementAndGet();
        if (bufferMonitor.remove(buffer) == null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.WARNING,
                    "Attempt to remove unknown buffer from pool.",
                    new Throwable());
            } else {
                logger.warning("Attempt to remove unknown buffer from pool.");
            }
        }
    }

    /**
     * Returns the size of the buffers managed by this pool.
     * 
     * @return the buffer size
     */
    public int bufferSize() {
        if (bufferSize < 0) {
            createBuffer().unlockBuffer();
        }
        return bufferSize;
    }

    /**
     * Acquires a managed buffer from the pool. If the pool is empty,
     * waits for a buffer to become available. The acquired buffer has 
     * a lock count of one.
     * 
     * @return the acquired buffer
     * @throws InterruptedException if the current thread is interrupted
     */
    @SuppressWarnings("PMD.GuardLogStatement")
    public W acquire() throws InterruptedException {
        // Stop draining, because we obviously need this kind of buffers
        Optional.ofNullable(idleTimer.getAndSet(null)).ifPresent(
            timer -> timer.cancel());
        if (createdBufs.get() < maximumBufs) {
            // Haven't reached maximum, so if no buffer is queued, create one.
            W buffer = queue.poll();
            if (buffer != null) {
                buffer.lockBuffer();
                return buffer;
            }
            return createBuffer();
        }
        // Wait for buffer to become available.
        if (logger.isLoggable(Level.FINE)) {
            // If configured, log message after waiting some time.
            W buffer = queue.poll(acquireWarningLimit, TimeUnit.MILLISECONDS);
            if (buffer != null) {
                buffer.lockBuffer();
                return buffer;
            }
            logger.log(Level.FINE,
                Thread.currentThread().getName() + " waiting > "
                    + acquireWarningLimit + "ms for buffer, while executing:",
                new Throwable());
        }
        W buffer = queue.take();
        buffer.lockBuffer();
        return buffer;
    }

    /**
     * Re-adds the buffer to the pool. The buffer is cleared.
     *
     * @param buffer the buffer
     * @see org.jgrapes.io.util.BufferCollector#recollect(org.jgrapes.io.util.ManagedBuffer)
     */
    @Override
    public void recollect(W buffer) {
        if (queue.size() < preservedBufs) {
            long effectiveDrainDelay
                = drainDelay > 0 ? drainDelay : defaultDrainDelay;
            if (effectiveDrainDelay > 0) {
                // Enqueue
                buffer.clear();
                queue.add(buffer);
                Timer old = idleTimer.getAndSet(Components.schedule(this::drain,
                    Duration.ofMillis(effectiveDrainDelay)));
                if (old != null) {
                    old.cancel();
                }
                return;
            }
        }
        // Discard
        removeBuffer(buffer);
    }

    @SuppressWarnings({ "PMD.UnusedFormalParameter",
        "PMD.UnusedPrivateMethod" })
    private void drain(Timer timer) {
        idleTimer.set(null);
        while (true) {
            W buffer = queue.poll();
            if (buffer == null) {
                break;
            }
            removeBuffer(buffer);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(50);
        builder.append("ManagedBufferPool [");
        if (queue != null) {
            builder.append("queue=").append(queue);
        }
        builder.append(']');
        return builder.toString();
    }

    /**
     * Buffer properties.
     */
    private class BufferProperties {

        private final StackTraceElement[] createdBy;

        /**
         * Instantiates new buffer properties.
         */
        public BufferProperties() {
            if (logger.isLoggable(Level.FINE)) {
                createdBy = Thread.currentThread().getStackTrace();
            } else {
                createdBy = new StackTraceElement[0];
            }
        }

        /**
         * Returns where the buffer was created.
         *
         * @return the stack trace element[]
         */
        @SuppressWarnings("PMD.MethodReturnsInternalArray")
        public StackTraceElement[] createdBy() {
            return createdBy;
        }
    }

    /**
     * This is basically a WeakHashMap. We cannot use WeakHashMap
     * because there is no "hook" into the collection of orphaned
     * references, which is what we want here.
     */
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private class BufferMonitor {

        private final Entry<W>[] data;
        private int indexMask;
        private final ReferenceQueue<W> orphanedEntries
            = new ReferenceQueue<>();

        /**
         * An Entry.
         *
         * @param <B> the generic type
         */
        private class Entry<B extends ManagedBuffer<?>> extends WeakReference<B>
                implements Map.Entry<B, BufferProperties> {
            /* default */ final int index;
            /* default */ BufferProperties props;
            /* default */ Entry<B> next;

            /**
             * Instantiates a new entry.
             *
             * @param buffer the buffer
             * @param props the props
             * @param queue the queue
             * @param index the index
             * @param next the next
             */
            /* default */ Entry(B buffer, BufferProperties props,
                    ReferenceQueue<B> queue, int index, Entry<B> next) {
                super(buffer, queue);
                this.index = index;
                this.props = props;
                this.next = next;
            }

            @Override
            public B getKey() {
                return get();
            }

            @Override
            public BufferProperties getValue() {
                return props;
            }

            @Override
            public BufferProperties setValue(BufferProperties props) {
                return this.props = props;
            }
        }

        /**
         * @param data
         */
        @SuppressWarnings("unchecked")
        public BufferMonitor(int maxBuffers) {
            int lists = 1;
            while (lists < maxBuffers) {
                lists <<= 1;
                indexMask = (indexMask << 1) + 1;
            }
            data = new Entry[lists];
        }

        /**
         * Put an entry in the map.
         *
         * @param buffer the buffer
         * @param properties the properties
         * @return the buffer properties
         */
        @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
        public BufferProperties put(W buffer, BufferProperties properties) {
            check();
            int index = buffer.hashCode() & indexMask;
            synchronized (data) {
                Entry<W> entry = data[index];
                Entry<W> prev = null;
                while (true) {
                    if (entry == null) {
                        // Not found, create new.
                        entry = new Entry<>(buffer, properties,
                            orphanedEntries, index, null);
                        if (prev == null) {
                            data[index] = entry; // Is first.
                        } else {
                            prev.next = entry; // Is next (last).
                        }
                        return properties;
                    }
                    if (entry.getKey() == buffer) { // NOPMD
                        // Found, update.
                        BufferProperties old = entry.getValue();
                        entry.setValue(properties);
                        return old;
                    }
                    prev = entry;
                    entry = entry.next;
                }
            }
        }

        /**
         * Returns the properties for the given buffer.
         *
         * @param buffer the buffer
         * @return the buffer properties
         */
        @SuppressWarnings("unused")
        public BufferProperties get(ManagedBuffer<?> buffer) {
            check();
            int index = buffer.hashCode() & indexMask;
            synchronized (data) {
                Entry<W> entry = data[index];
                while (entry != null) {
                    if (entry.getKey() == buffer) {
                        return entry.getValue();
                    }
                    entry = entry.next;
                }
                return null;
            }
        }

        /**
         * Removes the given buffer.
         *
         * @param buffer the buffer
         * @return the buffer properties
         */
        public BufferProperties remove(ManagedBuffer<?> buffer) {
            check();
            int index = buffer.hashCode() & indexMask;
            synchronized (data) {
                Entry<W> entry = data[index];
                Entry<W> prev = null;
                while (entry != null) {
                    if (entry.getKey() == buffer) {
                        if (prev == null) {
                            data[index] = entry.next; // Was first.
                        } else {
                            prev.next = entry.next;
                        }
                        return entry.getValue();
                    }
                    prev = entry;
                    entry = entry.next;
                }
                return null;
            }
        }

        @SuppressWarnings("PMD.CompareObjectsWithEquals")
        private BufferProperties remove(Entry<W> toBeRemoved) {
            synchronized (data) {
                Entry<W> entry = data[toBeRemoved.index];
                Entry<W> prev = null;
                while (entry != null) {
                    if (entry == toBeRemoved) {
                        if (prev == null) {
                            data[toBeRemoved.index] = entry.next; // Was first.
                        } else {
                            prev.next = entry.next;
                        }
                        return entry.getValue();
                    }
                    prev = entry;
                    entry = entry.next;
                }
                return null;
            }
        }

        private void check() {
            while (true) {
                @SuppressWarnings("unchecked")
                Entry<W> entry = (Entry<W>) orphanedEntries.poll();
                if (entry == null) {
                    return;
                }
                // Managed buffer has not been properly recollected, fix.
                BufferProperties props = remove(entry);
                if (props == null) {
                    return;
                }
                createdBufs.decrementAndGet();
                // Create warning
                if (logger.isLoggable(Level.WARNING)) {
                    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
                    final StringBuilder msg = new StringBuilder(
                        "Orphaned buffer from pool ");
                    msg.append(name());
                    StackTraceElement[] trace = props.createdBy();
                    if (trace != null) {
                        msg.append(", created");
                        for (StackTraceElement e : trace) {
                            msg.append(System.lineSeparator()).append("\tat ")
                                .append(e.toString());
                        }
                    }
                    logger.warning(msg.toString());
                }
            }
        }
    }

    /**
     * An MBean interface for getting information about the managed
     * buffer pools. Note that created buffer pools are tracked using
     * weak references. Therefore, the MBean may report more pools than
     * are really in use. 
     */
    public interface ManagedBufferPoolMXBean {

        /**
         * Information about a single managed pool.
         */
        @SuppressWarnings("PMD.DataClass")
        class PoolInfo {
            private final int created;
            private final int pooled;
            private final int preserved;
            private final int maximum;
            private final int bufferSize;

            /**
             * Instantiates a new pool info.
             *
             * @param created the created
             * @param pooled the pooled
             * @param preserved the preserved
             * @param maximum the maximum
             * @param bufferSize the buffer size
             */
            @ConstructorProperties({ "created", "pooled",
                "preserved", "maximum", "bufferSize" })
            public PoolInfo(int created, int pooled,
                    int preserved, int maximum, int bufferSize) {
                this.created = created;
                this.pooled = pooled;
                this.preserved = preserved;
                this.maximum = maximum;
                this.bufferSize = bufferSize;
            }

            /**
             * The number of buffers created by this pool.
             * 
             * @return the value
             */
            public int getCreated() {
                return created;
            }

            /**
             * The number of buffers pooled (ready to be acquired).
             * 
             * @return the value
             */
            public int getPooled() {
                return pooled;
            }

            /**
             * The number of buffers preserved.
             * 
             * @return the value
             */
            public int getPreserved() {
                return preserved;
            }

            /**
             * The maximum number of buffers created by this pool.
             * 
             * @return the value
             */
            public int getMaximum() {
                return maximum;
            }

            /**
             * The size of the buffers in items.
             * 
             * @return the buffer size
             */
            public int getBufferSize() {
                return bufferSize;
            }
        }

        /**
         * Three views on the existing pool.
         */
        class PoolInfos {
            private final SortedMap<String, PoolInfo> allPools;
            private final SortedMap<String, PoolInfo> nonEmptyPools;
            private final SortedMap<String, PoolInfo> usedPools;

            /**
             * Instantiates a new pool infos.
             *
             * @param pools the pools
             */
            public PoolInfos(Set<ManagedBufferPool<?, ?>> pools) {
                allPools = new TreeMap<>();
                nonEmptyPools = new TreeMap<>();
                usedPools = new TreeMap<>();

                @SuppressWarnings("PMD.UseConcurrentHashMap")
                Map<String, Integer> dupsNext = new HashMap<>();
                for (ManagedBufferPool<?, ?> mbp : pools) {
                    String key = mbp.name();
                    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
                    PoolInfo infos = new PoolInfo(
                        mbp.createdBufs.get(), mbp.queue.size(),
                        mbp.preservedBufs, mbp.maximumBufs,
                        mbp.bufferSize());
                    if (allPools.containsKey(key)
                        || dupsNext.containsKey(key)) {
                        if (allPools.containsKey(key)) {
                            // Found first duplicate, rename
                            allPools.put(key + "#1", allPools.get(key));
                            allPools.remove(key);
                            dupsNext.put(key, 2);
                        }
                        allPools.put(key + "#"
                            + dupsNext.put(key, dupsNext.get(key) + 1), infos);
                    } else {
                        allPools.put(key, infos);
                    }
                }
                for (Map.Entry<String, PoolInfo> e : allPools.entrySet()) {
                    PoolInfo infos = e.getValue();
                    if (infos.getPooled() > 0) {
                        nonEmptyPools.put(e.getKey(), infos);
                    }
                    if (infos.getCreated() > 0) {
                        usedPools.put(e.getKey(), infos);
                    }
                }
            }

            /**
             * All pools.
             *
             * @return the all pools
             */
            public SortedMap<String, PoolInfo> getAllPools() {
                return allPools;
            }

            /**
             * Pools that have at least managed buffer enqueued
             * (ready to be acquired).
             *
             * @return the non empty pools
             */
            public SortedMap<String, PoolInfo> getNonEmptyPools() {
                return nonEmptyPools;
            }

            /**
             * Pools that have at least one associated buffer
             * (in pool or in use).
             *
             * @return the used pools
             */
            public SortedMap<String, PoolInfo> getUsedPools() {
                return usedPools;
            }
        }

        /**
         * Set the default drain delay.
         * 
         * @param millis the drain delay in milli seconds
         */
        void setDefaultDrainDelay(long millis);

        /**
         * Returns the drain delay in milli seconds.
         * 
         * @return the value
         */
        long getDefaultDrainDelay();

        /**
         * Set the acquire warning limit.
         * 
         * @param millis the limit
         */
        void setAcquireWarningLimit(long millis);

        /**
         * Returns the acquire warning limit.
         * 
         * @return the value
         */
        long getAcquireWarningLimit();

        /**
         * Informations about the pools.
         * 
         * @return the map
         */
        PoolInfos getPoolInfos();

        /**
         * Summary information about the pooled buffers.
         * 
         * @return the values
         */
        IntSummaryStatistics getPooledPerPoolStatistics();

        /**
         * Summary information about the created buffers.
         * 
         * @return the values
         */
        IntSummaryStatistics getCreatedPerPoolStatistics();
    }

    /**
     * The MBean view
     */
    private static final class MBeanView implements ManagedBufferPoolMXBean {

        private static Set<ManagedBufferPool<?, ?>> allPools
            = Collections.synchronizedSet(
                Collections.newSetFromMap(new WeakHashMap<>()));

        /**
         * Adds the pool.
         *
         * @param pool the pool
         */
        public static void addPool(ManagedBufferPool<?, ?> pool) {
            allPools.add(pool);
        }

        @Override
        public void setDefaultDrainDelay(long millis) {
            ManagedBufferPool.setDefaultDrainDelay(millis);
        }

        @Override
        public long getDefaultDrainDelay() {
            return defaultDrainDelay();
        }

        @Override
        public void setAcquireWarningLimit(long millis) {
            acquireWarningLimit = millis;
        }

        @Override
        public long getAcquireWarningLimit() {
            return acquireWarningLimit;
        }

        @Override
        public PoolInfos getPoolInfos() {
            return new PoolInfos(allPools);
        }

        @Override
        public IntSummaryStatistics getPooledPerPoolStatistics() {
            return allPools.stream().collect(
                Collectors.summarizingInt(mbp -> mbp.queue.size()));
        }

        @Override
        public IntSummaryStatistics getCreatedPerPoolStatistics() {
            return allPools.stream().collect(
                Collectors.summarizingInt(mbp -> mbp.createdBufs.get()));
        }
    }

    static {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName mxbeanName = new ObjectName("org.jgrapes.io:type="
                + ManagedBufferPool.class.getSimpleName() + "s");
            mbs.registerMBean(new MBeanView(), mxbeanName);
        } catch (MalformedObjectNameException | InstanceAlreadyExistsException
                | MBeanRegistrationException | NotCompliantMBeanException e) {
            // Does not happen
        }
    }
}
