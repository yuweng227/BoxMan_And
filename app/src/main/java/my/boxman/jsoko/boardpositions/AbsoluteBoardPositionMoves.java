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
package my.boxman.jsoko.boardpositions;

import my.boxman.jsoko.board.Board;

/**
 * Like its super class {@link AbsoluteBoardPosition} instances of this class store
 * literal board configurations, i.e. all box positions and the player position,
 * but here extended by a moves count of the player.
 * <p>
 * This class is used by several solvers.
 */
public class AbsoluteBoardPositionMoves extends AbsoluteBoardPosition implements
		IBoardPositionMoves {

	// Number of moves that have been made until this board position has been reached.
	short movesCount = 0;

	/**
	 * Creates an object for storing a board position.
	 *
	 * @param board  the board of the current level
	 */
	public AbsoluteBoardPositionMoves(Board board) {

		// The main object is used to get all necessary data
		BoardPosition.board = board;

		// Save number of boxes
		boxCount = (short) board.boxCount;

		// Array for all box and the player position.
		positions = new int[boxCount + 1];			// +1 for player position

		// Save positions of all boxes. Inactive boxes will have a position of 0.
		for(short boxNo = 0; boxNo < boxCount; boxNo++) {
			if (board.boxData.isBoxInactive(boxNo)) {
				continue;
			}
			positions[boxNo] = board.boxData.getBoxPosition(boxNo);
		}

		// Store player position.
		positions[boxCount] = board.playerPosition;

		// Calculate the hash value of this board position.
		calculateHashValue();
	}

	/**
	 * Sets the number of moves.
	 *
	 * @param movesCount	Number of moves the player has done
	 */
	@Override
	final public void setMovesCount(int movesCount) {
		this.movesCount = (short) movesCount;
	}

	/**
	 * Returns the number of moves the player has done for reaching this board position.
	 *
	 * @return	Number of moves of this board position.
	 */
	@Override
	public short getTotalMovesCount() {
		return movesCount;
	}
}