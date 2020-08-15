/**
 *  JSoko - A Java implementation of the game of Sokoban
 *  Copyright (c) 2014 by Matthias Meger, Germany
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
package my.boxman.jsoko.pushesLowerBoundCalculation;

import my.boxman.jsoko.board.Board;
import my.boxman.jsoko.deadlockdetection.BipartiteMatchings;
import my.boxman.jsoko.deadlockdetection.DeadlockDetection;
import my.boxman.jsoko.resourceHandling.Settings;

/**
 * A {@code PushesLowerboundCalculation} object is used to estimate the needed pushes from the current board position to a solved board
 * position. Therefore the distance of every box to a goal is calculated. Every box is assigned to one specific goal in such a way that the
 * sum of all box->goal distances (regarding box pushes) is as low as possible.
 * <p>
 * The calculated total distance is never higher, but usually smaller than the really needed number of pushes to solve the level.
 */
public class LowerBoundCalculation {

	/** This value is returned when the lower bound calculation has found a deadlock. */
	public static final int DEADLOCK = Integer.MAX_VALUE;

	/** Object for penalty calculations. Penalties are precalculated values that increase the calculated pushes lower bound. */
	private final Penalty penalty;

	/** The pushes lower bound is calculated using a bipartite matching algorithm. */
	private final BipartiteMatchings minimumMatching;

	/** The board the lower bound is calculated for. */
	private final Board board;

	/** During the lower bound calculation the deadlock detection is used to identify deadlocks. */
	private final DeadlockDetection deadlockDetection;

	/**
	 * Creates an Object for calculating the pushes lower bound of a board position.
	 *
	 * @param board  the board the lower bound is to be calculated for
	 */
	public LowerBoundCalculation(Board board) {
		this.board 		  = board;
		penalty 		  = new Penalty(board);
		minimumMatching   = new BipartiteMatchings(board);
		deadlockDetection = new DeadlockDetection(board);
	}

	/**
	 * Computes a minimum number of pushes needed to push every box to a goal on the current board. The calculated number of pushes in never
	 * higher than the real minimum number but usually lower.<br>
	 * This method takes "frozen boxes" into account when calculating the pushes lower bound, hence it's necessary that the the "freeze"
	 * status of every box on the board is up to date.
	 * <p>
	 * Note: the detection changes the board while trying to find a deadlock.
	 *
	 * @return lower bound of the current board
	 */
	public int calculatePushesLowerbound() {
		return calculatePushesLowerBound(board.boxData.getBoxPosition(0));
	}

	/**
	 * Computes a minimum number of pushes needed to push every box to a goal on the current board. The calculated number of pushes in never
	 * higher than the real minimum number but usually lower.<br>
	 * This method takes "frozen boxes" into account when calculating the pushes lower bound, hence it's necessary that the the "freeze"
	 * status of every box on the board is up to date.
	 * <p>
	 * Note: the detection changes the board while trying to find a deadlock.
	 *
	 * @param newBoxPosition  position of the last pushed box (needed for quicker deadlock identifying)
	 * @return lower bound of the current board
	 */
	public int calculatePushesLowerBound(int newBoxPosition) {

		if(deadlockDetection.isDeadlock(newBoxPosition)) {
			return DEADLOCK;
		}

		int pushesLowerbound = minimumMatching.calculatePushesLowerBound(Settings.SearchDirection.FORWARD);

		// The forward search can add an additional penalty for linear conflicts of boxes.
		return pushesLowerbound == DEADLOCK ? DEADLOCK : pushesLowerbound + penalty.calculatePenalty();
	}

	/**
	 * Computes the lower bound for the backwards search, taking into account a possibly existing deadlock.
	 * If a deadlock is recognized, we return {@link_LowerBound#DEADLOCK}.
	 *
	 * @return current lower bound of the board position
	 */
	public int calculatePushesLowerBoundBackwardsSearch() {
		return calculatePushesLowerboundBackwardsSearch(board.boxData.getBoxPosition(0));
	}

	/**
	 * Computes the lower bound for the backwards search, taking into account a possibly existing deadlock.
	 * If a deadlock is recognized, we return {@link_LowerBound#DEADLOCK}.
	 *
	 * @param newBoxPosition  position of the last pulled box (needed for quicker deadlock identifying)
	 * @return current pushes lower bound of the board position
	 */
	public int calculatePushesLowerboundBackwardsSearch(int newBoxPosition) {

		if(deadlockDetection.isBackwardDeadlock(newBoxPosition)) {
			return DEADLOCK;
		};

		return minimumMatching.calculatePushesLowerBound(Settings.SearchDirection.BACKWARD);
	}
}
