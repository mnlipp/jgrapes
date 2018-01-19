/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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
/**
 * I/O related components built on top of the core package.
 * 
 * JGrapes manages buffers in pools. The framework therefore
 * defines the {@link org.jgrapes.io.util.ManagedBuffer}
 * that wraps a NIO buffer, adding the information required for 
 * managing the buffer. 
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
@org.osgi.annotation.versioning.Version("${api_version}")
package org.jgrapes.io;
