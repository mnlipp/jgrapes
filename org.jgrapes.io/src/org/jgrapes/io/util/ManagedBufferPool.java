/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016  Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
 */
public class ManagedBufferPool<W extends ManagedBuffer<T>, T extends Buffer>
	implements BufferCollector {

	protected static final Logger logger 
		= Logger.getLogger(ManagedBufferPool.class.getName());
	
	private static long defaultDrainDelay = 1500;
	private static long acquireWarningLimit = 1000;
	
	private String name = Components.objectName(this);
	private BiFunction<T, BufferCollector,W> wrapper = null;
	private Supplier<T> bufferFactory = null;
	private Map<Integer,List<BufferMonitor>> bufferMonitors = new HashMap<>();
	private BlockingQueue<W> queue;
	private int bufferSize = -1;
	private int preservedBufs;
	private int maximumBufs;
	private AtomicInteger createdBufs;
	private long drainDelay = -1;
	private AtomicReference<Timer> idleTimer = new AtomicReference<>(null);
		
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
	public ManagedBufferPool(BiFunction<T,BufferCollector, W> wrapper, 
			Supplier<T> bufferFactory, int lowerThreshold, int upperLimit) {
		this.wrapper = wrapper;
		this.bufferFactory = bufferFactory;
		preservedBufs = lowerThreshold;
		maximumBufs = upperLimit;
		createdBufs = new AtomicInteger();
		queue = new ArrayBlockingQueue<W>(lowerThreshold);
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
	public ManagedBufferPool(BiFunction<T,BufferCollector, W> wrapper, 
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
		BufferMonitor monitor = new BufferMonitor(buffer);
		synchronized (bufferMonitors) {
			bufferMonitors.computeIfAbsent(System.identityHashCode(buffer), 
					key -> new LinkedList<>()).add(monitor);
		}
		bufferSize = buffer.capacity();
		return buffer;
	}

	/**
	 * Removes the buffer from the pool.
	 * 
	 * @param buffer the buffer to remove
	 */
	private void removeBuffer(ManagedBuffer<?> buffer) {
		createdBufs.decrementAndGet();
		synchronized (bufferMonitors) {
			int hashCode = System.identityHashCode(buffer);
			List<BufferMonitor> candidates = bufferMonitors.get(hashCode);
			boolean found = false;
			if (candidates != null) {
				for (Iterator<BufferMonitor> itr = candidates.iterator(); 
					itr.hasNext(); ) {
					BufferMonitor monitor = itr.next();
					if (buffer == monitor.get()) {
						itr.remove();
						found = true;
						break;
					}
				}
				if (candidates.isEmpty()) {
					bufferMonitors.remove(hashCode);
				}
			}
			if (!found) {
				logger.warning("Attempt to remove unknown buffer from pool.");
			}
		}
	}
	
	/**
	 * Removes a buffer monitor from the pool. This is needed to clean
	 * up the monitors of orphaned buffers.
	 * 
	 * @param monitor
	 */
	private void removeBufferMonitor(
			ManagedBufferPool<?,?>.BufferMonitor monitor) {
		synchronized (bufferMonitors) {
			for (Iterator<Integer> hci = bufferMonitors.keySet().iterator();
					hci.hasNext();) {
				List<BufferMonitor> candidates = bufferMonitors.get(hci.next());
				for (Iterator<BufferMonitor> itr = candidates.iterator(); 
					itr.hasNext(); ) {
					BufferMonitor candidate = itr.next();
					if (candidate == monitor) {
						itr.remove();
						break;
					}
				}
				if (candidates.isEmpty()) {
					hci.remove();
				}
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
	public W acquire() throws InterruptedException {
		// Stop draining, because we obviously need this kind of buffers 
		Optional.ofNullable(idleTimer.getAndSet(null)).ifPresent(
				timer -> timer.cancel());
		if (createdBufs.get() < maximumBufs) {
			// Haven't reached maximum, so if no buffer is queued, create one.
			W buffer = queue.poll();
			if (buffer != null) {
				return buffer;
			}
			return createBuffer();
		}
		// Wait for buffer to become available.
		if (logger.isLoggable(Level.FINE)) {
			// If configured, log message after waiting some time.
			W buffer = queue.poll(acquireWarningLimit, TimeUnit.MILLISECONDS);
			if (buffer != null) {
				return buffer;
			}
			logger.fine("Waiting > " + acquireWarningLimit + "ms for buffer: " 
						+ Thread.currentThread().getName());
		}
		return queue.take();
	}
	
	/**
	 * Re-adds the buffer to the pool. The buffer is cleared.
	 * 
	 * @see org.jgrapes.io.util.BufferCollector#recollect(org.jgrapes.io.util.ManagedBuffer)
	 */
	@Override
	public void recollect(ManagedBuffer<?> buffer) {
		if (queue.size() < preservedBufs) {
			long effectiveDrainDelay 
				= drainDelay > 0 ? drainDelay : defaultDrainDelay;
			if (effectiveDrainDelay > 0) {
				// Enqueue
				buffer.clear();
				buffer.lockBuffer();
				@SuppressWarnings("unchecked")
				W buf = (W)buffer;
				queue.add(buf);
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
	
	private void drain(Timer timer) {
		idleTimer.set(null);
		while(true) {
			ManagedBuffer<?> buffer = queue.poll();
			if (buffer == null) {
				break;
			}
			removeBuffer(buffer);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ManagedBufferPool [");
		if (queue != null) {
			builder.append("queue=");
			builder.append(queue);
		}
		builder.append("]");
		return builder.toString();
	}

	/**
	 * To identify buffers not returned to the pool, 
	 * all buffers handed out are tracked using this class, 
	 * essentially a weak reference. Instances are created for each 
	 * buffer and removed when the buffer is returned to the pool.
	 * 
	 * If the weak reference is collected by the garbage collector,
	 * this means that the buffer has not been returned. This condition
	 * is watched for by the {@link OrphanedCollector} and produces
	 * a warning.
	 */
	private class BufferMonitor extends WeakReference<ManagedBuffer<?>> {

		private StackTraceElement[] createdBy;
		
		public BufferMonitor(ManagedBuffer<?> referent) {
			super(referent, OrphanedCollector.orphanedBuffers);
			if (logger.isLoggable(Level.FINE)) {
				createdBy = Thread.currentThread().getStackTrace();
			}
		}

		public ManagedBufferPool<?, ?> manager() {
			return ManagedBufferPool.this;
		}
		
		public StackTraceElement[] createdBy() {
			return createdBy;
		}
	}
	
	private static class OrphanedCollector extends Thread {
		
		private static ReferenceQueue<ManagedBuffer<?>> orphanedBuffers 
			= new ReferenceQueue<>();
		
		public OrphanedCollector() {
			setName(ManagedBufferPool.class.getSimpleName() + ".Collector");
			setDaemon(true);
			start();
		}

		@Override
		public void run() {
			while(true) {
				try {
					ManagedBufferPool<?,?>.BufferMonitor monitor 
						= (ManagedBufferPool<?,?>.BufferMonitor)orphanedBuffers
							.remove();
					ManagedBufferPool<?,?> mbp = monitor.manager();
					// Managed buffer has not been properly recollected, fix.
					mbp.createdBufs.decrementAndGet();
					mbp.removeBufferMonitor(monitor);
					// Create warning
					if (logger.isLoggable(Level.WARNING)) {
						final StringBuilder msg = new StringBuilder(
								"Orphaned buffer from pool " + mbp.name());
						StackTraceElement[] st = monitor.createdBy();
						if (st != null) {
							msg.append(", created");
							for (StackTraceElement e: st) {
								msg.append(System.lineSeparator());
								msg.append("\tat ");
								msg.append(e.toString());
							}
						}
						logger.warning(msg.toString());
					}
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}
	
	static {
		new OrphanedCollector();
	}
	
	/**
	 * An MBean interface for getting information about the managed
	 * buffer pools. Note that created buffer pools are tracked using
	 * weak references. Therefore, the MBean may report more pools than
	 * are really in use. 
	 */
	public static interface ManagedBufferPoolMXBean {

		/**
		 * Information about a single managed pool.
		 */
		public static class PoolInfo {
			private int created;
			private int pooled;
			private int bufferSize;
			
			@ConstructorProperties({"created", "pooled", "bufferSize"})
			public PoolInfo(int created, int pooled, int bufferSize) {
				this.created = created;
				this.pooled = pooled;
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
		public static class PoolInfos {
			private SortedMap<String,PoolInfo> allPools;
			private SortedMap<String,PoolInfo> nonEmptyPools;
			private SortedMap<String,PoolInfo> usedPools;

			public PoolInfos(Set<ManagedBufferPool<?, ?>> pools) {
				allPools = new TreeMap<>();
				nonEmptyPools = new TreeMap<>();
				usedPools = new TreeMap<>();
				
				Map<String, Integer> dupsNext = new HashMap<>();
				for (ManagedBufferPool<?,?> mbp: pools) {
					String key = mbp.name();
					PoolInfo qi = new PoolInfo(
							mbp.createdBufs.get(), mbp.queue.size(), mbp.bufferSize());
					if (allPools.containsKey(key) || dupsNext.containsKey(key)) {
						if (allPools.containsKey(key)) {
							// Found first duplicate, rename
							allPools.put(key + "#1", allPools.get(key));
							allPools.remove(key);
							dupsNext.put(key, 2);
						}
						allPools.put(key + "#"
								+ (dupsNext.put(key, dupsNext.get(key) + 1)), qi);
					} else {
						allPools.put(key, qi);
					}
				}
				for (Map.Entry<String,PoolInfo> e: allPools.entrySet()) {
					PoolInfo qi = e.getValue();
					if (qi.getPooled() > 0) {
						nonEmptyPools.put(e.getKey(), qi);
					}
					if (qi.getCreated() > 0) {
						usedPools.put(e.getKey(), qi);
					}
				}
			}
			
			/**
			 * All pools.
			 */
			public SortedMap<String, PoolInfo> getAllPools() {
				return allPools;
			}

			/**
			 * Pools that have at least managed buffer enqueued
			 * (ready to be acquired).
			 */
			public SortedMap<String, PoolInfo> getNonEmptyPools() {
				return nonEmptyPools;
			}

			/**
			 * Pools that have at least one associated buffer
			 * (in pool or in use).
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
	
	private static class MBeanView implements ManagedBufferPoolMXBean {

		private static Set<ManagedBufferPool<?,?>> allPools
			= Collections.synchronizedSet(
				Collections.newSetFromMap(
						new WeakHashMap<ManagedBufferPool<?, ?>, Boolean>()));

		public static void addPool(ManagedBufferPool<?, ?> pool) {
			allPools.add(pool);
		}
		
		@Override
		public void setDefaultDrainDelay(long millis) {
			ManagedBufferPool.setDefaultDrainDelay(millis);
		}

		@Override
		public long getDefaultDrainDelay() {
			return ManagedBufferPool.defaultDrainDelay();
		}

		@Override
		public void setAcquireWarningLimit(long millis) {
			ManagedBufferPool.acquireWarningLimit = millis;
		}

		@Override
		public long getAcquireWarningLimit() {
			return ManagedBufferPool.acquireWarningLimit;
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
					+ ManagedBufferPool.class.getSimpleName());
			mbs.registerMBean(new MBeanView(), mxbeanName);
		} catch (MalformedObjectNameException | InstanceAlreadyExistsException
				| MBeanRegistrationException | NotCompliantMBeanException e) {
			// Does not happen
		}		
	}
}
