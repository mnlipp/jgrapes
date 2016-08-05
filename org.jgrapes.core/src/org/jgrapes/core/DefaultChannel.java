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
package org.jgrapes.core;

/**
 * An interface that can be used to specify the component's channel (see
 * {@link Component#getChannel()}) as channel in handler annotations.
 * <P>
 * Using the component's channel is the default if no channels are specified in
 * the annotation, so specifying this channel only in the handler annotation is
 * equivalent to specifying no channel at all. This special channel class is
 * required if you want to specify a handler that handles events fired on the
 * component's channel or on additional channels.
 * 
 * @author Michael N. Lipp
 */
public interface DefaultChannel extends Channel {

}
