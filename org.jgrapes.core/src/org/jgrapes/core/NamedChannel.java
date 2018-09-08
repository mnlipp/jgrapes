/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.core;

/**
 * This class provides channels that are identified by a name
 * (<code>string</code>). Instances of this class represent channels that use
 * their name as value for matching channels with handlers.
 */
public final class NamedChannel implements Channel {

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
     * Returns the name of the channel as its value.
     * 
     * @return the name
     * 
     * @see org.jgrapes.core.Channel#defaultCriterion()
     */
    @Override
    public Object defaultCriterion() {
        return name;
    }

    /**
     * Returns <code>true</code> if the <code>value</code>
     * matches the name of this channel or is the broadcast channel's value. 
     * 
     * @see org.jgrapes.core.Eligible#isEligibleFor(java.lang.Object)
     */
    @Override
    public boolean isEligibleFor(Object value) {
        return value.equals(BROADCAST.defaultCriterion())
            || value.equals(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "NamedChannel [name=" + name + "]";
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public int hashCode() {
        @SuppressWarnings("PMD.AvoidFinalLocalVariable")
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        NamedChannel other = (NamedChannel) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

}
