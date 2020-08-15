/**
 *  JSoko - A Java implementation of the game of Sokoban
 *  Copyright (c) 2012 by Matthias Meger, Germany
 *
 *  This file is part of JSoko.
 *
 *	JSoko is free software; you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation; either version 2 of the License, or
 *	(at your option) any later version.
 *
 *	This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package my.boxman.jsoko.resourceHandling;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class uses the ability of the {@link LinkedHashMap} to
 * maintain a (doubly linked) list of entries according to LRU.
 * <p>
 * The LRU list is currently not used for searching, though.
 * An alternate implementation could do so, but the HashMap is good enough
 * for searching entries by key.
 * <p>
 * We <em>do</em> use the LRU list for eventual deletion of old elements,
 * in order to hold down memory consumption.
 * <p>
 * FFS/hm: missing: equals() hashCode(), clone(), toString()
 *
 * @author Heiner Marxen
 */
public class LruCache<K,V> {

	public static final float DFT_LOAD_FACTOR = 0.75f;

	/**
	 * The fixed value for {@code accessOrder} in the constructor
	 * {@link LinkedHashMap#LinkedHashMap(int, float, boolean)}
	 * to achieve access ordering (not the default insertion ordering).
	 */
	private static final boolean accessOrder = true;

	/**
	 * Our main component, the map itself.
	 * <p>
	 * FFS: We may want to offer more of the operations on LinkedHashMap
	 * for our LruCache, also.  In that case go on and add them.
	 */
	private LinkedHashMap<K, V> map;

	/**
	 * Current bound for "low RAM condition".
	 * Negative values indicate "no such limit exists".
	 *
	 * @see #setMinRAMinMiB(long)
	 */
	private long minRAMinMiB = -1;

	/**
	 * @return the minRAMinMiB
	 * @see #setMinRAMinMiB(long)
	 */
	public long getMinRAMinMiB() {
		return minRAMinMiB;
	}

	/**
	 * If the available memory (RAM) is less than this bound,
	 * then we are going to remove the eldest entry whenever we add a new one,
	 * i.e. we do not expand our memory foot print.
	 * <p>
	 * Negative values indicate "no such limit exists".
	 *
	 * @param minRAMinMiB the minRAMinMiB to set
	 * @see Utilities#getMaxUsableRAMinMiB()
	 */
	public void setMinRAMinMiB(long minRAMinMiB) {
		this.minRAMinMiB = minRAMinMiB;
	}

	/**
	 * Creates an {@code LruCache} object with an initial capacity.
	 * @param initialCapacity provide memory for that many elements
	 */
	public LruCache(int initialCapacity) {
		// We must use this incarnation of the constructor(), in order to
		// set the access order to the non-standard value.
		map = new LinkedHashMap<K, V>( initialCapacity, DFT_LOAD_FACTOR,
				                       accessOrder);
	}

	/**
	 * Inside this method (we override it) we implement our deletion strategy.
	 * Either we return {@code true}, indicating the caller shall delete
	 * that eldest entry, but in that case we are not allowed to change
	 * the object ourselves.
	 * <p>
	 * Or we decide to take our own steps, return {@code false} to hinder
	 * the caller to take any action, but may have removed one or more
	 * elements ourselves.
	 *
	 * @param eldest   the currently eldest element, deletion candidate
	 * @return whether the caller shall really remove that eldest entry
	 */
	protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
		if (minRAMinMiB >= 0) {
			// We are limited...
			if (size() >= 2) {
				// We are large enough to loose an entry...
				if (Utilities.getMaxUsableRAMinMiB() < minRAMinMiB) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @return number of elements in the cache
	 */
	public int size()
	{
		return map.size();
	}

	/**
	 * This is like {@link LinkedHashMap#get(Object)}, but forces the
	 * correct type of the key.
	 *
	 * @param key the key for which we search the mapped value
	 * @return the mapped value, or {@code null}.
	 */
	public V getV(K key) {
		return map.get(key);
	}

	/**
	 * Adds a key/value pair to the cache mapping.
	 * We expect the key to be new, but we do not enforce (or check) that,
	 * since the typical cache user will first use {@link #getV(Object)},
	 * anyhow.
	 *
	 * @param key   key of the new cache entry
	 * @param value value of the new cache entry
	 */
	public void add(K key, V value) {
		map.put(key, value);
	}

	/**
	 * Removes all cached key/value pairs.
	 */
	public void clear() {
		map.clear();
	}

	/**
	 * Trim down the memory usage to the currently needed amount.
	 * This is a user hint, and we need not really take any action,
	 * e.g. if the implementation does not know how to do that.
	 * @see ArrayList#trimToSize()
	 */
	public void trimToSize() {
		// we cannot do anything useful, here
	}
}
