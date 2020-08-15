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

import java.util.Random;

import my.boxman.jsoko.board.Board;
import my.boxman.jsoko.resourceHandling.Settings;

/**
 * This class saves a board position, that means every box position and the player position.
 * The player position is saved using the most top-left position the player can reach.
 * Instances of this class are used as basis for relatively saved board positions.
 */
public class AbsoluteBoardPosition extends BoardPosition {

	protected int[] positions;

	// Bits for "searchSolution", indicating which search direction has been used
	// while this object has been created.
	protected boolean backwardsSearch;
	protected boolean forwardsSearch;

	// Constructor
	AbsoluteBoardPosition() {
		/*empty*/
	}

	/**
	 * Creates an object holding all box positions and the player position.
	 * 创建一个保存所有箱子位置和玩家位置的对象。
	 * @param board  the board of the current level
	 */
	public AbsoluteBoardPosition(Board board) {

		// The main object is used to get all necessary data
		BoardPosition.board = board;

		// Save number of boxes
		boxCount = (short) board.boxCount;

		// Array for all box and the player position.
		positions = new int[boxCount + 1];			// +1 for player position

		// Save positions of all boxes. Inactive boxes will have a position of 0.
		// 保存所有箱子的位置。未动过的箱子的位置为 0。
		for (short boxNo = 0; boxNo < boxCount; boxNo++) {
			if (!board.boxData.isBoxInactive(boxNo)) {
				positions[boxNo] = board.boxData.getBoxPosition(boxNo);
			}
		}

		// To be able to identify equal board positions the top-left player position is stored.
		positions[boxCount] = board.playersReachableSquares.getPlayerPositionTopLeft();

		// Calculate the hash value of this board position.
		calculateHashValue();
	}

	/**
	 * Calculates the hash value for this board position.
	 * 计算此局面的哈希值。
	 */
	protected void calculateHashValue() {

		// Fill the Zobrist values if they aren't filled yet.
		if (zobristValues == null
				|| board.size > zobristValues.length) {
			zobristValues = new int[board.size];
			Random randomGenerator = new Random(42);
			for (int i = zobristValues.length; --i != -1;) {
				zobristValues[i] = randomGenerator.nextInt();
			}

			//        	for(int i=0; i<zobristValues.length; i++)
			//        		for(int j=i+1; j<zobristValues.length; j++)
			//        			if(zobristValues[i] == zobristValues[j])
			//        				System.out.println("bad zobrist");
		}

		// Calculate the hash value for this board position
		for (int index = 0; index < boxCount + 1; index++) {
			hashvalue ^= zobristValues[positions[index]];
		}
	}

	/* (non-Javadoc)
	 * @see de.sokoban_online.jsoko.boardpositions.BoardPosition#getPositions()
	 */
	@Override
	final public int[] getPositions() {
		return positions;
	}

	/**
	 * An absolute board position usually hasn't a preceding board position. This method is
	 * implemented for easier working with linked lists.
	 *
	 * @return always null
	 */
	@Override
	final public IBoardPosition getPrecedingBoardPosition() {
		return null;
	}

	/**
	 * Sets the search direction of the solution search this class is created in.
	 *
	 * @param searchDirection Direction of the search
	 */
	@Override
	final public void setSearchDirection(Settings.SearchDirection searchDirection) {

		if (searchDirection == Settings.SearchDirection.FORWARD) {
			forwardsSearch  = true;
		} else {
			backwardsSearch = true;
		}
	}

	/* (non-Javadoc)
	 * @see de.sokoban_online.jsoko.boardpositions.IBoardPosition#getSearchDirection()
	 */
	@Override
	final public Settings.SearchDirection getSearchDirection() {
		return forwardsSearch ? Settings.SearchDirection.FORWARD : Settings.SearchDirection.BACKWARD;
	}

	/**
	 * For avoiding some casts this method is implemented here. Actually, this method
	 * is only needed for relative board positions.
	 *
	 * @return always 0
	 */
	@Override
	final public int getDirection() {
		return 0;
	}

	/**
	 * Absolute board positions are only created when no box has been pushed. This value is interpreted
	 * during a search for a solution (no box has been pushed so no tunnel check has to be performed).
	 *
	 * @return always <code>NO_BOX_PUSHED</code>
	 */
	@Override
	final public int getBoxNo() {
		return NO_BOX_PUSHED;
	}

	/**
	 * Return the player position of this board position.
	 *
	 * @return the player position
	 */
	@Override
	final public int getPlayerPosition() {
		return positions[boxCount];
	}

	/**
	 * Returns the number of pushes that were made to reach this board position.
	 * An absolute board position is only created at the beginning, so this is
	 * always 0.
	 * This number is used in the solving methods.
	 *
	 * @return always 0
	 */
	@Override
	final public int getPushesCount() {
		return 0;
	}

	/**
	 * Sets the position of the box represented by the passed box number to 0, indicating
	 * that this box is inactive.
	 *
	 * @param boxNo Number of the box to be set at position 0.
	 */
	final public void setBoxInactive(int boxNo) {
		positions[boxNo] = 0;
		calculateHashValue();
	}
}