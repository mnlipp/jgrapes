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

package org.jgrapes.portal.defaulttheme;

import java.io.InputStream;

import org.jgrapes.portal.ResourceNotFoundException;
import org.jgrapes.portal.ThemeProvider;

/**
 * 
 */
public class DefaultThemeProvider extends ThemeProvider {

	/* (non-Javadoc)
	 * @see org.jgrapes.portal.ThemeProvider#providesTheme(java.lang.String)
	 */
	@Override
	public boolean providesTheme(String theme) {
		return "default".equals(theme);
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.portal.ThemeProvider#getResourceAsStream(java.lang.String)
	 */
	@Override
	public InputStream getResourceAsStream(String name)
	        throws ResourceNotFoundException {
		InputStream in = getClass().getResourceAsStream(name);
		if (in == null) {
			throw new ResourceNotFoundException();
		}
		return in;
	}
}
