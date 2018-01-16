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

/**
 * A class that manages a set of permits and notifies listeners
 * about changes of availability.
 * 
 * Listeners are added using {@link WeakReference}, so removing
 * them isn't strictly necessary.
 */
public class PermitsPool {

	private int permits;
	private List<WeakReference<AvailabilityListener>> 
		listeners = new LinkedList<>();
	private boolean lastNotification = true;
	
	/**
	 * @param permits
	 */
	public PermitsPool(int permits) {
		this.permits = permits;
	}

	/**
	 * Adds an AvailabilityListener.
	 * 
	 * @param listener the AvailabilityListener
	 */
	public void addListener(AvailabilityListener listener) {
		synchronized (listeners) {
			listeners.add(new WeakReference<>(listener));
		}
	}

	/**
	 * @param listener the AvailabilityListener
	 */
	public void removeListener(AvailabilityListener listener) {
		synchronized (listeners) {
			for (Iterator<WeakReference<AvailabilityListener>> 
				iter = listeners.iterator(); iter.hasNext();) {
				WeakReference<AvailabilityListener> item = iter.next();
				if (item.get() == null || item.get() == listener) {
					iter.remove();
				}
			}
		}
	}

	private void notifyAvailabilityListeners() {
		boolean available = (permits > 0);
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
	public synchronized void release() {
		permits += 1;
		notifyAvailabilityListeners();
	}

	/**
	 * Try to acquire a permit
	 * 
	 * @return `true` if successful
	 */
	public synchronized boolean tryAcquire() {
		if (permits > 0) {
			permits -= 1;
			notifyAvailabilityListeners();
			return true;
		}
		return false;
	}

}
