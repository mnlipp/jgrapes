/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements the {@link Session} interface using a {@link HashMap}.
 */
@SuppressWarnings("serial")
public class InMemorySession extends HashMap<Serializable, Serializable>
	implements Session {

	@SuppressWarnings("PMD.ShortVariable")
	private final String id;
	private final Instant createdAt;
	private Instant lastUsedAt;
	private final Map<?,?> transientData = new ConcurrentHashMap<>();
	
	/**
	 * Create a new session.
	 */
	@SuppressWarnings("PMD.ShortVariable")
	public InMemorySession(String id) {
		this.id = id;
		createdAt = Instant.now();
		lastUsedAt = createdAt;
	}

	/**
	 * Returns the session id.
	 * 
	 * @return the id
	 */
	@SuppressWarnings("PMD.ShortMethodName")
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
	public void updateLastUsedAt() {
		this.lastUsedAt = Instant.now();
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.http.Session#transientData()
	 */
	@Override
	public Map<?, ?> transientData() {
		return transientData;
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
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		InMemorySession other = (InMemorySession) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(50);
		builder.append("InMemorySession [");
		if (id != null) {
			builder.append("id=");
			builder.append(id);
			builder.append(", ");
		}
		if (createdAt != null) {
			builder.append("createdAt=");
			builder.append(createdAt);
			builder.append(", ");
		}
		if (lastUsedAt != null) {
			builder.append("lastUsedAt=");
			builder.append(lastUsedAt);
		}
		builder.append(']');
		return builder.toString();
	}
	
}
