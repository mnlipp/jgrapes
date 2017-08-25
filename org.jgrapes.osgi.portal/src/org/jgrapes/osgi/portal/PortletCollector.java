/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017  Michael N. Lipp
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

package org.jgrapes.osgi.portal;

import java.util.stream.StreamSupport;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.portal.PortletFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * 
 */
public class PortletCollector extends Component 
	implements ServiceTrackerCustomizer<PortletFactory<?>, PortletFactory<?>> {

	private BundleContext context;
	private ServiceTracker<PortletFactory<?>, PortletFactory<?>> serviceTracker;
	
	/**
	 * @param componentChannel
	 */
	public PortletCollector(Channel componentChannel, BundleContext context) {
		super(componentChannel);
		this.context = context;
		@SuppressWarnings("unchecked")
		Class<PortletFactory<?>> cls 
			= (Class<PortletFactory<?>>)(Class<?>)PortletFactory.class;
		serviceTracker = new ServiceTracker<>(
				context, cls, this);
		serviceTracker.open();
	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
	 */
	@Override
	public PortletFactory<?> addingService(
	        ServiceReference<PortletFactory<?>> reference) {
		PortletFactory<?> portletFactory = context.getService(reference);
		if (StreamSupport.stream(spliterator(), false)
				.filter(c -> c.getClass()
						.equals(portletFactory.componentType()))
				.count() == 0) {
			attach(portletFactory.create(channel()));
		}
		return portletFactory;
	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	@Override
	public void modifiedService(ServiceReference<PortletFactory<?>> reference,
	        PortletFactory<?> service) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	@Override
	public void removedService(ServiceReference<PortletFactory<?>> reference,
	        PortletFactory<?> service) {
		for (ComponentType child: this) {
			if (child.getClass().equals(service.componentType())) {
				Components.manager(child).detach();
			}
		}
	}

}
