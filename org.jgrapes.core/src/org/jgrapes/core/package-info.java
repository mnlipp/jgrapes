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

/**
 * This package defines the interfaces and classes that provide
 * the core functionality of JGrapes.
 * <P>
 * The package supports some specific (java.util) logging settings.
 * <dl>
 *   <dt>{@code org.jgrapes.core.handlerTracking.level}</dt>
 *   <dd>If set to {@code FINE}, causes events and their handlers to be logged
 *   before the handler is invoked. If set to {@code FINER} additionally 
 *   causes events without any handler to be logged and if set to {@code
 *   FINEST} also logs the result of invoking a handler
 *   (the additional logging is done with log level
 *   {@code FINE}, just as the logging of the invocations). Enabling the
 *   invocation logging involves some performance penalty because additional
 *   information (not required for normal operation) has to be maintained.</dd>
 *   
 *   <dt>{@code org.jgrapes.core.classNames.level}</dt>
 *   <dd>If set to {@code FINER}, class names are converted to fully
 *   qualified names in {@code toString()} methods.</dd>
 *   </dl>
 * 
 * @author Michael N. Lipp
 */
@org.osgi.annotation.versioning.Version("${api_version}")
package org.jgrapes.core;
