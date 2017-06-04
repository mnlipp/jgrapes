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

package org.jgrapes.portal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 */
public interface Portlet {
	
	enum RenderMode { Preview, View, Edit, Help }
	
	static final Set<RenderMode> PREVIEW_ONLY_PORTLET_MODES
		= Collections.unmodifiableSet(
				new HashSet<>(Arrays.asList(RenderMode.Preview)));

	static final Set<RenderMode> VIEWABLE_PORTLET_MODES
		= Collections.unmodifiableSet(
				new HashSet<>(Arrays.asList(
						RenderMode.Preview, RenderMode.View)));

	static final Set<RenderMode> EDITABLE_PREVIEW_PORTLET_MODES
		= Collections.unmodifiableSet(
				new HashSet<>(Arrays.asList(
						RenderMode.Preview, RenderMode.Edit)));

	static final Set<RenderMode> EDITABLE_PORTLET_MODES
		= Collections.unmodifiableSet(
				new HashSet<>(Arrays.asList(
						RenderMode.Preview, RenderMode.View, RenderMode.Edit)));

}
