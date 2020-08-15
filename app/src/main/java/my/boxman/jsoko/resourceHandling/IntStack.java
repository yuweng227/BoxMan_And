/**
 *  JSoko - A Java implementation of the game of Sokoban
 *  Copyright (c) 2016 by Matthias Meger, Germany
 *
 *  This file is part of JSoko.
 *
 *	JSoko is free software; you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation; either version 3 of the License, or
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

/**
 * The <code>IntStack</code> class represents a last-in-first-out
 * (LIFO) stack of int values.
 * <p>
 * An <code>IntStack</code> has a fixed size to be specified at
 * creation.
 * <p>
 * When a stack is first created, it is empty.
 */
public class IntStack {

	/** Array to store the int values. */
	private final int[] data;

	/** topOfStack */
	private int topOfStack;

	public void initValue(int pos) {
		data[pos] = 0;
		topOfStack = -1;
	}

	/**
	 * Creates an empty stack.
	 * <p>
	 * The <code>IntStack</code> class represents a last-in-first-out
	 * (LIFO) stack of int values.
	 * <p>
	 * An <code>IntStack</code> has a fixed size to be specified at
	 * creation.
	 *
	 * @param size the maximum number of int values the stack can store
	 */
	public IntStack (int size) {
		data = new int[size];
		topOfStack = -1;
	}


	/**
	 * Adds the passed int value to the stack.
	 * <p>
	 * Throws {@code ArrayIndexOutOfBoundsException} when stack size is exceeded.
	 *
	 * @param value  the value to be added
	 */
	public void add(int value) {
		data[++topOfStack] = value;
	}

	/**
	 * Removes and returns the last added value of this stack.
	 * <p>
	 * Throws {@code ArrayIndexOutOfBoundsException} when stack size is empty when called.
	 *
	 * @return  the value
	 */
	public int remove() {
		return data[topOfStack--];
	}

	/**
	 * Returns whether this stack is empty.
	 * <p>
	 * The stack is empty when no values are stored for removing.
	 *
	 * @return  <code>true</code> when the stack is empty, <code>false</code> otherwise
	 */
	public boolean isEmpty() {
		return topOfStack == -1;
	}

	/**
	 * Removes all stored values.
	 * <p>
	 * After calling this method {@link #isEmpty()} returns <code>true</code>.
	 */
	public void clear() {
		topOfStack = -1;
	}
}
