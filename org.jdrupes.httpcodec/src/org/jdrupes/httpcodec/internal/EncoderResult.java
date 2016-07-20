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
package org.jdrupes.httpcodec.internal;

/**
 * @author Michael N. Lipp
 *
 */
class EncoderResult {

	private boolean overflow;
	private boolean underflow;
	private boolean closeConnection;

	/**
	 * Creates a new result with the given values.
	 * 
	 * @param overflow
	 * @param underflow
	 * @param closeConnection
	 */
	protected EncoderResult(boolean overflow, boolean underflow,
	        boolean closeConnection) {
		super();
		this.overflow = overflow;
		this.underflow = underflow;
		this.closeConnection = closeConnection;
	}

	/**
	 * Indicates that the data didn't fit in the out buffer. The encoding method
	 * that has returned this result should be re-invoked with the same
	 * parameters except for a new (or cleared) output buffer.
	 * 
	 * @return {@code true} if overflow occurred
	 */
	public boolean isOverflow() {
		return overflow;
	}

	/**
	 * Indicates that more data is expected. The encoding method that has
	 * returned this result should be re-invoked with the same parameters except
	 * for an input buffer with additional information.
	 * 
	 * @return {@code true} if underflow occurred
	 */
	public boolean isUnderflow() {
		return underflow;
	}

	/**
	 * Indicates that the connection to the receiver of the response must be
	 * closed to complete the encoding of the response.
	 * 
	 * @return the value
	 */
	public boolean getCloseConnection() {
		return closeConnection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (overflow ? 1231 : 1237);
		result = prime * result + (closeConnection ? 1231 : 1237);
		result = prime * result + (underflow ? 1231 : 1237);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EncoderResult other = (EncoderResult) obj;
		if (overflow != other.overflow)
			return false;
		if (closeConnection != other.closeConnection)
			return false;
		if (underflow != other.underflow)
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("EncoderResult [overflow=");
		builder.append(overflow);
		builder.append(", underflow=");
		builder.append(underflow);
		builder.append(", sendClose=");
		builder.append(closeConnection);
		builder.append("]");
		return builder.toString();
	}

}
