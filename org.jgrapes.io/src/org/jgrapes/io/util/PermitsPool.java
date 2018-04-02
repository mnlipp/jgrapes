/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * A class that manages a set of permits and notifies listeners
 * about changes of availability.
 * 
 * Listeners are added using {@link WeakReference}, so removing
 * them isn't strictly necessary.
 */
public class PermitsPool {

	private MySemaphore delegee;
	private List<WeakReference<AvailabilityListener>> 
		listeners = new LinkedList<>();
	private boolean lastNotification = true;
	
	private static class MySemaphore extends Semaphore {
		private static final long serialVersionUID = 8758302721594300704L;

		public MySemaphore(int permits) {
			super(permits);
		}

		@Override
		public void reducePermits(int reduction) {
			super.reducePermits(reduction);
		}
	}
	
	/**
	 * Instantiates a new permits pool.
	 *
	 * @param permits the permits
	 */
	public PermitsPool(int permits) {
		delegee = new MySemaphore(permits);
	}

	/**
	 * Returns the number of currently available permits.
	 *
	 * @return the result
	 */
	public int availablePermits() {
		return delegee.availablePermits();
	}

	/**
	 * Adds the given number of permits to the pool.
	 *
	 * @param permits the number of permits to add
	 * @return the permits pool
	 */
	public PermitsPool augmentPermits(int permits) {
		delegee.release(permits);
		return this;
	}
	
	/**
	 * Remove the given number of permits from the pool.
	 *
	 * @param permits the number of permits to remove
	 * @return the permits pool
	 */
	public PermitsPool reducePermits(int permits) {
		delegee.reducePermits(permits);
		return this;
	}
	
	/**
	 * Adds an AvailabilityListener.
	 *
	 * @param listener the AvailabilityListener
	 * @return the permits pool
	 */
	public PermitsPool addListener(AvailabilityListener listener) {
		synchronized (listeners) {
			listeners.add(new WeakReference<>(listener));
		}
		return this;
	}

	/**
	 * Removes the listener.
	 *
	 * @param listener the AvailabilityListener
	 * @return the permits pool
	 */
	public PermitsPool removeListener(AvailabilityListener listener) {
		synchronized (listeners) {
			for (Iterator<WeakReference<AvailabilityListener>> 
				iter = listeners.iterator(); iter.hasNext();) {
				WeakReference<AvailabilityListener> item = iter.next();
				if (item.get() == null || item.get() == listener) {
					iter.remove();
				}
			}
		}
		return this;
	}

	private void notifyAvailabilityListeners() {
		boolean available = (availablePermits() > 0);
		if (available == lastNotification) {
			return;
		}
		lastNotification = available;
		List<AvailabilityListener> copy = new ArrayList<>();
		synchronized (listeners) {
			for (Iterator<WeakReference<AvailabilityListener>> 
				iter = listeners.iterator(); iter.hasNext();) {
				WeakReference<AvailabilityListener> item = iter.next();
				AvailabilityListener listener = item.get();
				if (listener == null) {
					iter.remove();
					continue;
				}
				copy.add(listener);
			}
		}
		for (AvailabilityListener l: copy) {
			l.availabilityChanged(this, available);
		}
	}

	/**
	 * Release a previously obtained permit.
	 */
	public synchronized PermitsPool release() {
		delegee.release();;
		notifyAvailabilityListeners();
		return this;
	}

	/**
	 * Acquire a permit, waiting until one becomes available.
	 *
	 * @return the permits pool
	 * @throws InterruptedException the interrupted exception
	 */
	public PermitsPool acquire() throws InterruptedException {
		delegee.acquire();
		notifyAvailabilityListeners();
		return this;
	}
	
	/**
	 * Try to acquire a permit.
	 *
	 * @return `true` if successful
	 */
	public synchronized boolean tryAcquire() {
		boolean gotOne = delegee.tryAcquire();
		if (gotOne) {
			notifyAvailabilityListeners();
			return true;
		}
		return false;
	}

}
