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
package org.jdrupes.httpcodec.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Michael N. Lipp
 *
 */
public class HttpUtils {

	private HttpUtils() {
	}

	final public static String TCHARS 
		= "!#$%&'*+-.0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_"
				+ "^`abcdefghijklmnopqrstuvwxyz|~";

	/**
	 * Wraps the given map as a map with case insensitive keys.
	 * 
	 * @param map
	 * @return
	 */
	public static <V> Map<String,V> caseInsensitiveMap(Map<String,V> map) {
		return new CaseInsensitiveMap<>(map);
	}
	
	private static class CaseInsensitiveMap<V> implements Map<String, V> {

		private Map<String,V> backing;

		/**
		 * @param backing
		 */
		public CaseInsensitiveMap(Map<String, V> backing) {
			this.backing = backing;
		}

		/**
		 * @see java.util.Map#clear()
		 */
		public void clear() {
			backing.clear();
		}

		/**
		 * @see java.util.Map#compute(java.lang.Object, java.util.function.BiFunction)
		 */
		public V compute(String key,
		        BiFunction<? super String, ? super V, ? extends V> 
				remappingFunction) {
			return backing.compute(key.toLowerCase(), remappingFunction);
		}

		/**
		 * @see java.util.Map#computeIfAbsent(java.lang.Object, java.util.function.Function)
		 */
		public V computeIfAbsent(String key,
		        Function<? super String, ? extends V> mappingFunction) {
			return backing.computeIfAbsent(key.toLowerCase(), mappingFunction);
		}

		/**
		 * @see java.util.Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)
		 */
		public V computeIfPresent(String key,
		        BiFunction<? super String, ? super V, ? extends V> 
				remappingFunction) {
			return backing.computeIfPresent(key.toLowerCase(),
					remappingFunction);
		}

		/**
		 * @see java.util.Map#containsKey(java.lang.Object)
		 */
		public boolean containsKey(Object key) {
			if (!(key instanceof String)) {
				return false;
			}
			return backing.containsKey(((String)key).toLowerCase());
		}

		/**
		 * @see java.util.Map#containsValue(java.lang.Object)
		 */
		public boolean containsValue(Object value) {
			return backing.containsValue(value);
		}

		/**
		 * @see java.util.Map#entrySet()
		 */
		public Set<java.util.Map.Entry<String, V>> entrySet() {
			return backing.entrySet();
		}

		/**
		 * @see java.util.Map#equals(java.lang.Object)
		 */
		public boolean equals(Object o) {
			return backing.equals(o);
		}

		/**
		 * @see java.util.Map#get(java.lang.Object)
		 */
		public V get(Object key) {
			if (!(key instanceof String)) {
				return null;
			}
			return backing.get(((String)key).toLowerCase());
		}

		/**
		 * @see java.util.Map#getOrDefault(java.lang.Object, java.lang.Object)
		 */
		public V getOrDefault(Object key, V defaultValue) {
			if (!(key instanceof String)) {
				throw new IllegalArgumentException
					("Key must be of type String.");
			}
			return backing.getOrDefault(((String)key).toLowerCase(),
					defaultValue);
		}

		/**
		 * @see java.util.Map#hashCode()
		 */
		public int hashCode() {
			return backing.hashCode();
		}

		/**
		 * @see java.util.Map#isEmpty()
		 */
		public boolean isEmpty() {
			return backing.isEmpty();
		}

		/**
		 * @return
		 * @see java.util.Map#keySet()
		 */
		public Set<String> keySet() {
			return backing.keySet().stream()
					.map(String::toLowerCase).collect(Collectors.toSet());
		}

		/**
		 * @see java.util.Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)
		 */
		public V merge(String key, V value,
		        BiFunction<? super V, ? super V, ? extends V> 
				remappingFunction) {
			return backing.merge(key.toLowerCase(), value, remappingFunction);
		}

		/**
		 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
		 */
		public V put(String key, V value) {
			return backing.put(key.toLowerCase(), value);
		}

		/**
		 * @param m
		 * @see java.util.Map#putAll(java.util.Map)
		 */
		public void putAll(Map<? extends String, ? extends V> m) {
			for(Map.Entry<? extends String, ? extends V> e: m.entrySet()) {
				put(e.getKey(), e.getValue());
			}
		}

		/**
		 * @see java.util.Map#putIfAbsent(java.lang.Object, java.lang.Object)
		 */
		public V putIfAbsent(String key, V value) {
			return backing.putIfAbsent(key.toLowerCase(), value);
		}

		/**
		 * @see java.util.Map#remove(java.lang.Object, java.lang.Object)
		 */
		public boolean remove(Object key, Object value) {
			if (!(key instanceof String)) {
				return false;
			}
			return backing.remove(((String)key).toLowerCase(), value);
		}

		/**
		 * @param key
		 * @return
		 * @see java.util.Map#remove(java.lang.Object)
		 */
		public V remove(Object key) {
			if (!(key instanceof String)) {
				return null;
			}
			return backing.remove(((String)key).toLowerCase());
		}

		/**
		 * @see java.util.Map#replace(java.lang.Object, 
		 * java.lang.Object, java.lang.Object)
		 */
		public boolean replace(String key, V oldValue, V newValue) {
			return backing.replace(key.toLowerCase(), oldValue, newValue);
		}

		/**
		 * @see java.util.Map#replace(java.lang.Object, java.lang.Object)
		 */
		public V replace(String key, V value) {
			return backing.replace(key.toLowerCase(), value);
		}

		/**
		 * @return
		 * @see java.util.Map#size()
		 */
		public int size() {
			return backing.size();
		}

		/**
		 * @return
		 * @see java.util.Map#values()
		 */
		public Collection<V> values() {
			return backing.values();
		}

		
	}

}
