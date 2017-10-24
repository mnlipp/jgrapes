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

import java.text.NumberFormat;
import java.util.Locale;

/**
 * 
 */
public class Utils {

	private Utils() {
	}

	/**
	 * Utility method to format a memory size to a maximum
	 * of 4 digits.
	 * 
	 * @param locale the locale
	 * @param size the size value to format
	 * @return the formatted value
	 */
	public static String formatMemorySize(Locale locale, long size) {
		int scale = 0;
		while (size > 10000 && scale < 5) {
				size = size / 1024;
				scale += 1;
		}
		String unit = "PiB";
		switch (scale) {
		case 0:
			unit = "B";
			break;
		case 1:
			unit = "kiB";
			break;
		case 2:
			unit = "MiB";
			break;
		case 3:
			unit = "GiB";
			break;
		case 4:
			unit = "TiB";
			break;
		default:
			break;
		}
		return NumberFormat.getInstance(locale).format(size) + " " + unit;
		
	}
	
}
