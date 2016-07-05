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
package org.jdrupes.httpcodec;

/**
 * @author Michael N. Lipp
 *
 */
public class EncoderResult {

	private boolean overflow;
	private boolean mustBeClosed;
	
	/**
	 * @param overflow
	 * @param mustBeClosed
	 */
	public EncoderResult(boolean overflow, boolean mustBeClosed) {
		super();
		this.mustBeClosed = mustBeClosed;
		this.overflow = overflow;
	}

	/**
	 * @return the overflow
	 */
	public boolean isOverflow() {
		return overflow;
	}

	/**
	 * @return the mustBeClosed
	 */
	public boolean mustBeClosed() {
		return mustBeClosed;
	}

	
}
