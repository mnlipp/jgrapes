/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.jdrupes;

/**
 * A class for channels identified by a name (<code>string</code>).
 * Instances of this class represent channels
 * that use their name as key for matching channels with handlers.
 * 
 * @author mnl
 */
final public class NamedChannel implements Channel {

	private final String name;

	/**
	 * Creates a new named channel with the given name.
	 * 
	 * @param name the channel's name
	 */
	public NamedChannel(String name) {
		super();
		this.name = name;
	}

	/**
	 * Returns the name of the channel as its key.
	 * 
	 * @return the name
	 * 
	 * @see org.jdrupes.Channel#getMatchKey()
	 */
	@Override
	public Object getMatchKey() {
		return name;
	}

	/**
	 * Returns <code>true</code> if the <code>handlerKey</code>
	 * is the broadcast channel's key or matches the name
	 * of this channel.
	 * 
	 * @see org.jdrupes.internal.Matchable#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(Object handlerKey) {
		return handlerKey.equals(BROADCAST.getMatchKey())
				|| handlerKey.equals(name);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "NamedChannel [name=" + name + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/* (non-Javadoc)
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
		NamedChannel other = (NamedChannel) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
}
