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

import java.io.InputStream;

/**
 * 
 */
public abstract class ThemeProvider {

	/**
	 * Return the id of the theme.
	 * 
	 * @return the result
	 */
	public abstract String themeId();

	/**
	 * Return the name of the theme. The default implementation 
	 * uses the theme id, replaces underscores with spaces and
	 * capitalizes the first character.
	 * 
	 * @return the result
	 */
	public String themeName() {
		return (Character.toUpperCase(themeId().charAt(0))
				+ themeId().substring(1)).replace('_', ' ');
	}

	/**
	 * Find and open the given resource.
	 * 
	 * @param name the resource name
	 * @return the data as input stream
	 */
	public InputStream getResourceAsStream(String name)
	        throws ResourceNotFoundException {
		InputStream in = getClass().getResourceAsStream(name);
		if (in == null) {
			throw new ResourceNotFoundException();
		}
		return in;
	}
}
