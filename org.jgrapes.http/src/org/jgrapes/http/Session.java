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

package org.jgrapes.http;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;

/**
 * Represents a session.
 */
@SuppressWarnings("serial")
public class Session extends HashMap<Serializable, Serializable> {

	private String id;
	private Instant createdAt;
	private Instant lastUsedAt;
	
	/**
	 * Create a new session.
	 */
	public Session(String id) {
		this.id = id;
		createdAt = Instant.now();
		lastUsedAt = createdAt;
	}

	/**
	 * Returns the session id.
	 * 
	 * @return the id
	 */
	public String id() {
		return id;
	}
	
	/**
	 * Returns the creation time stamp.
	 * 
	 * @return the creation time stamp
	 */
	public Instant createdAt() {
		return createdAt;
	}

	/**
	 * Returns the last used (referenced in request) time stamp. 
	 * 
	 * @return the last used timestamp
	 */
	public Instant lastUsedAt() {
		return lastUsedAt;
	}

	/**
	 * Updates the last used time stamp.
	 */
	void updateLastUsedAt() {
		this.lastUsedAt = Instant.now();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return id.hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (getClass() != obj.getClass()) {
			return false;
		}
		Session other = (Session) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}
}
