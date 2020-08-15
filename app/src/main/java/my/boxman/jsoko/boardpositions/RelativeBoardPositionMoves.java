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
 * Like its super class {@link RelativeBoardPosition} instances of this class store
 * board positions relative to its predecessor by noting the difference,
 * and extends that by a moves count of the player.
 */
public class RelativeBoardPositionMoves extends RelativeBoardPosition implements IBoardPositionMoves {

	/**
	 * Total number of moves the player needs to reach this board configuration.
	 */
	protected short movesCount = 0;

	/**
	 * @param board  the board of the current level
	 * @param boxNo		 number of the pushed box
	 * @param direction	 direction into which the box was pushed
	 * @param precedingBoardPosition reference to the preceding board configuration
	 */
	public RelativeBoardPositionMoves(Board board, int boxNo,
									  int direction, IBoardPosition precedingBoardPosition) {

		// Save number of boxes in a static variable
		boxCount = (short) board.boxCount;

		// For move-board positions the player position is saved as it is (not the top-left player position!)
		playerPosition = (short) board.playerPosition;

		// Save the new box number
		positionData &= CLEAR_BOX_NO;
		positionData |= boxNo;

		// Save the direction the box has been pushed to.
		positionData |= (direction << 13);

		// Save the preceding board position.
		this.precedingBoardPosition = precedingBoardPosition;

		// Calculate the hash value of this board position.
		calculateHashValue(boxNo, direction);

	}

	/**
	 * Sets the (total) number of moves of the player to reach this board configuration.
	 *
	 * @param movesCount  number of moves the player needs to reach this
	 */
	@Override
	public void setMovesCount(int movesCount) {
		this.movesCount = (short) movesCount;
	}

	/**
	 * Returns the (total) number of moves of the player to reach this board configuration.
	 *
	 * @return  number of moves the player needs to reach this
	 */
	@Override
	public short getTotalMovesCount() {
		return movesCount;
	}
}