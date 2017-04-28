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

import java.time.Instant;
import java.util.HashMap;

/**
 * 
 */
@SuppressWarnings("serial")
public class Session extends HashMap<Object, Object> {

	private SessionManager sessionManager;
	private String id;
	private Instant createdAt;
	private Instant lastUsedAt;
	
	/**
	 * 
	 */
	Session(SessionManager sessionManager, String id) {
		this.sessionManager = sessionManager;
		this.id = id;
		createdAt = Instant.now();
		lastUsedAt = createdAt;
	}

	String id() {
		return id;
	}
	
	public Instant createdAt() {
		return createdAt;
	}

	public Instant lastUsedAt() {
		return lastUsedAt;
	}

	void setLastUsedAt(Instant now) {
		this.lastUsedAt = now;
	}
	
	public void discard() {
		sessionManager.discard(this);
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
