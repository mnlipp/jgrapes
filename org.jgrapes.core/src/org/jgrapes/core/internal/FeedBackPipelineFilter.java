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
package org.jgrapes.core.internal;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.EventPipeline;

/**
 * If a pipeline has been associated with the current thread, forward any
 * added event to the associated pipeline. Else forward added events to
 * a fall back pipeline.
 * 
 * @author Michael N. Lipp
 */
class FeedBackPipelineFilter implements MergingEventPipeline {

	protected static ThreadLocal<ExecutingEventPipeline> 
		currentPipeline = new ThreadLocal<>();
	private MergingEventPipeline fallback;
	
	/**
	 * Create a new instance that forwards events added from different threads
	 * to the given fall back pipeline.
	 * 
	 * @param fallback
	 */
	public FeedBackPipelineFilter(MergingEventPipeline fallback) {
		super();
		this.fallback = fallback;
	}

	/**
	 * Associate the invoking thread with the given pipeline.
	 * 
	 * @param pipeline the pipeline
	 */
	public static void setAssociatedPipeline(ExecutingEventPipeline pipeline) {
		currentPipeline.set(pipeline);
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.EventPipeline#add(org.jgrapes.core.internal.EventBase, org.jgrapes.core.Channel[])
	 */
	@Override
	public void add(EventBase event, Channel... channels) {
		EventPipeline pipeline = currentPipeline.get();
		if (pipeline != null) {
			pipeline.add(event, channels);
		} else {
			fallback.add(event, channels);
		}
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.internal.MergingEventPipeline#merge(org.jgrapes.core.EventPipeline)
	 */
	@Override
	public void merge(EventPipeline other) {
		MergingEventPipeline pipeline = currentPipeline.get();
		if (pipeline != null) {
			pipeline.merge(other);
		} else {
			fallback.merge(other);
		}
	}

	/**
	 * Set the data stored for a given {@link Component} in the context
	 * of this pipeline.
	 * 
	 * @param component the component
	 * @param data the data
	 */
	static void setComponentContext(Component component, Object data) {
		ExecutingEventPipeline pipeline = currentPipeline.get();
		if (pipeline == null) {
			throw new IllegalStateException
				("setComponentContext may only be called in handler.");
		}
		pipeline.setComponentContext(component, data);
	}

	/**
	 * Get the data stored for a given {@link Component} in the context
	 * of this pipeline.
	 * 
	 * @param component the component
	 * @return the data
	 */
	static Object getComponentContext(Component component) {
		ExecutingEventPipeline pipeline = currentPipeline.get();
		if (pipeline == null) {
			throw new IllegalStateException
				("getComponentContext may only be called in handler.");
		}
		return pipeline.getComponentContext(component);
	}
	
	
}
