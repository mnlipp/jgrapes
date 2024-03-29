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

package org.jgrapes.core.internal;

import java.util.concurrent.ExecutorService;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;

/**
 * If a pipeline has been associated with the current thread, forward any
 * added event to the associated pipeline. Else forward added events to
 * a fall back pipeline.
 */
class FeedBackPipelineFilter implements InternalEventPipeline {

    private final ComponentTree componentTree;
    private final InternalEventPipeline fallback;

    /**
     * Create a new instance that forwards events added from different threads
     * to the given fall back pipeline.
     *
     * @param componentTree the component tree
     * @param fallback the fallback pipeline
     */
    public FeedBackPipelineFilter(ComponentTree componentTree,
            InternalEventPipeline fallback) {
        this.componentTree = componentTree;
        this.fallback = fallback;
    }

    @Override
    public <T extends Event<?>> T add(T event, Channel... channels) {
        InternalEventPipeline pipeline = componentTree.dispatchingPipeline();
        if (pipeline != null) {
            return pipeline.add(event, channels);
        }
        return fallback.add(event, channels);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jgrapes.core.internal.MergingEventPipeline#merge(org.jgrapes.core.
     * EventPipeline)
     */
    @Override
    public void merge(InternalEventPipeline other) {
        InternalEventPipeline pipeline = componentTree.dispatchingPipeline();
        if (pipeline == null) {
            fallback.merge(other);
            return;
        }
        pipeline.merge(other);
    }

    @Override
    public void awaitExhaustion() throws InterruptedException {
        InternalEventPipeline pipeline = componentTree.dispatchingPipeline();
        if (pipeline != null) {
            pipeline.awaitExhaustion();
            return;
        }
        fallback.awaitExhaustion();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.core.internal.InternalEventPipeline#executorService()
     */
    @Override
    public ExecutorService executorService() {
        InternalEventPipeline pipeline = componentTree.dispatchingPipeline();
        if (pipeline != null) {
            return pipeline.executorService();
        }
        return fallback.executorService();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public String toString() {
        StringBuilder builder = new StringBuilder(50);
        String sinkName = "(current) ";
        builder.append("FeedBackPipelineFilter [");
        InternalEventPipeline pipeline = componentTree.dispatchingPipeline();
        if (pipeline == null) {
            pipeline = fallback;
            sinkName = "(fallback) ";
        }
        builder.append(sinkName)
            .append(pipeline)
            .append(']');
        return builder.toString();
    }

}
