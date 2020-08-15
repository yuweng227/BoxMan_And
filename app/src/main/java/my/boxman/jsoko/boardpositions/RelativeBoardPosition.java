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
package my.boxman.jsoko.boardpositions;

import my.boxman.jsoko.board.Board;
import my.boxman.jsoko.resourceHandling.Settings;

/**
 * Instances of this class store a board configuration relative to the preceding
 * board configuration.
 * For this we remember which box has been pushed into which direction.
 * Additionally we store which boxes are inactive, since during corral analysis
 * we may generate board configurations without pushing a box, by removing a box
 * from the board (setting it inactive).
 */
public class RelativeBoardPosition extends BoardPosition implements Cloneable {

	/**
	 * Bit mask to clear the box number from a <code>positionData</code>
	 * (by anding).
	 */
    protected static final int CLEAR_BOX_NO = ~ ((1 << 10) - 1);

    /**
	 * Bit mask to clear the search direction from a <code>positionData</code>.
	 */
    protected static final int CLEAR_SEARCH_DIRECTION = ~ (3 << 10);

	/**
	 * Extension for relative board position storage.
	 */
	protected IBoardPosition precedingBoardPosition;

	/**
	 * The position of the player is stored redundantly, just for speed.
	 * It could be recomputed each time, since the player is always
	 * located besides the moved (pushed) box, and from there we could
	 * determine the smallest (upper left) reachable player position.
	 */
	protected short playerPosition;

	/**
	 * We use a compact encoding for box number and direction.
	 * We stuff them both into a <code>short</code>, not using
	 * the highest (sign) bit, in order to avoid certain problems.
	 * Additionally we reserve 2 bits for the "search direction"
	 * <p>
	 * We arrive at this design for the bits in the short:
	 * <br>00..09 = 10 bits for the box number of the moved box
	 * <br>10..11 = 2 bits reserved for search direction
	 * <br>12..12 = unused
	 * <br>13..14 = 2 bit for the direction of the push/pull
	 * <br>15..15 = 1 sign bit, not used
	 * <p>
	 * The direction is the direction to which the box has been pushed/pulled
	 * to reach the current board position.
	 */
	protected short positionData = 0;


	/**
	 * Constructor for cloning
	 */
	public RelativeBoardPosition() { }

	/**
	 * Creates an object for storing the current board situation.
	 *
	 * @param board  the board of the current level
	 * @param boxNo			Number of the pushed box (maximal 2047!)
	 * @param direction		Direction the box has been pushed to
	 * @param precedingBoardPosition	The preceding board position
	 */
	public RelativeBoardPosition(Board board, int boxNo, int direction, IBoardPosition precedingBoardPosition) {

		// The static variables "application" and "boxCount" don't have to be set, because an
		// AbsoluteBoardPosition must have been created before an instance of this class is created.
		// Hence, these variables are already set by the AbsoluteBoardPosition.

		// Determine the player position top-left. This is needed for identifying equal board positions.
		playerPosition = (short) board.playersReachableSquares.getPlayerPositionTopLeft();

		// Set the new boxNo.
		positionData &= CLEAR_BOX_NO;
		positionData |= boxNo;

		// Direction the box has been pushed to.
		positionData |= (direction << 13);

		// Set the preceding board position.
		this.precedingBoardPosition = precedingBoardPosition;

		// Calculate the hash value of this board position.
		calculateHashValue(boxNo, direction);
	}

	/**
	 * Calculates the hash value for this board position.
	 *
	 * @param boxNo Number of the pushed box
	 * @param direction Direction the box is pushed to
	 */
	protected void calculateHashValue(int boxNo, int direction) {

		// NO_BOX_PUSHED also means the player hasn't moved, so the player position hasn't to be xored.
		if (boxNo == NO_BOX_PUSHED) {
			hashvalue = precedingBoardPosition.hashCode();

			return;
		}

		// Calculate the hash value for this board position.
		// This coding relies on the box to be on the correct position at the moment!
		int boxPosition = board.boxData.getBoxPosition(boxNo);
		hashvalue = precedingBoardPosition.hashCode()
				^ zobristValues[board.getPositionAtOppositeDirection(boxPosition, direction)]
				^ zobristValues[boxPosition]
				^ zobristValues[precedingBoardPosition.getPlayerPosition()]
				^ zobristValues[playerPosition];
	}

	// returns the box positions and the player position of this object
	@Override
	public int[] getPositions() {

		// We recompute the absolute box positions from the relative encoding
		int boxesDifferences[] = new int[boxCount+1];

		IBoardPosition currentBoardPosition = this;

		// Go through all board positions until an absolute board position has been reached.
		do {
			int boxNo     = currentBoardPosition.getBoxNo();
			int direction = currentBoardPosition.getDirection();

			// The box has been moved in some direction to reach this
			// board position.  We sum the location changes per box,
			// except for NO_BOX_PUSHED, which encodes "no move".
			if (boxNo != NO_BOX_PUSHED) {
				boxesDifferences[boxNo] += board.offset[direction];
			}
			currentBoardPosition = currentBoardPosition.getPrecedingBoardPosition();
		} while (currentBoardPosition.getPrecedingBoardPosition() != null);

		// We have reached something without a predecessor, so it is
		// an absolute board position.  We take the box array of it
		// as base, and add all box move deltas we collected above.
		int[] boxPositions = currentBoardPosition.getPositions();

        for (int boxNo = 0; boxNo < boxCount; boxNo++) {
            boxesDifferences[boxNo] += boxPositions[boxNo];
        }

		// Our player position is not subject to relative encoding,
		// it is always directly stored: just copy it to the result.
        boxesDifferences[boxCount] = playerPosition;

		return boxesDifferences;
	}

	/**
	 * Returns the preceding board position of this board position.
	 *
	 * @return preceding board position
	 */
	@Override
	final public IBoardPosition getPrecedingBoardPosition() {
		return precedingBoardPosition;
	}

	/**
	 *  Setzt eine neue vorige Stellung für die aktuelle Stellung.
	 * Dabei muss natürlich sichergestellt sein, dass die Kistennr und die
	 * Verschiebungsrichtung sich nun auf diese neue Stellung beziehen!
	 * Achtung: Selbst wenn die neue vorige Stellung mit der alten
	 * vorigen Stellung gleich ist, so muss sie nicht identisch sein!
	 * Beispiel:
	 * alte Stellung: Kiste0 : x=1, y=4
	 *                Kiste1 : x=5, y=2
	 * neue Stellung: Kiste0 : x=5, y=2
	 *				  Kiste1 : x=1, y=4
	 *
	 * Ist nun gespeichert: Kiste0 wurde nach rechts geschoben, so wird
	 * nun eine andere Stellung ermittelt, da die vorige Stellung
	 * die Kistenpositionen anders angeordnet hatte!
	 *
	 * @param precedingBoardPosition the preceding board position
	 */
	final public void setPrecedingBoardPosition(
			IBoardPosition precedingBoardPosition) {

		this.precedingBoardPosition = precedingBoardPosition;
	}

	/**
	 * Returns the direction the box has been pushed.
	 *
	 * @return Direction of the push
	 */
	@Override
	final public int getDirection() {
		return positionData >>> 13;
	}

	/**
	 * Returns the number of the pushed box.
	 *
	 * @return Number of the pushed box
	 */
	@Override
	public int getBoxNo() {
		return positionData & ((1 << 10) - 1);
	}

	/**
	 * Return the player position of this board position.
	 *
	 * @return the player position
	 */
	@Override
	final public int getPlayerPosition() {
		return playerPosition;
	}

	// Remember the search direction which encountered the board position.
	// We use 2 bits of positionData reserved for just this purpose
	/* (non-Javadoc)
	 * @see de.sokoban_online.jsoko.boardpositions.ISearchBoardPosition#setSearchDirection(int)
	 */
	@Override
	final public void setSearchDirection(Settings.SearchDirection searchDirection) {

		// Clear old search direction.
		positionData &= CLEAR_SEARCH_DIRECTION;

		switch(searchDirection) {
			case FORWARD:
				positionData |= (1 << 10);
				break;
			case BACKWARD:
				positionData |= (2 << 10);
				break;
			case BACKWARD_GOAL_ROOM:
				positionData |= (3 << 10);
				break;
			default:
				break;
		}
	}

	/* (non-Javadoc)
	 * @see de.sokoban_online.jsoko.boardpositions.ISearchBoardPosition#getSearchDirection()
	 */
	@Override
	final public Settings.SearchDirection getSearchDirection() {

		int directionValue = (positionData >>> 10) & 3;

		switch(directionValue) {
			case 1:
				return Settings.SearchDirection.FORWARD;
			case 2:
				return Settings.SearchDirection.BACKWARD;
			case 3:
				return Settings.SearchDirection.BACKWARD_GOAL_ROOM;
		}

		return Settings.SearchDirection.UNKNOWN;
	}

	/* (non-Javadoc)
	 * @see de.sokoban_online.jsoko.boardpositions.RelativeBoardPosition#clone()
	 */
	@Override
	public Object clone() {

		RelativeBoardPosition clone = new RelativeBoardPosition();
		clone.setPrecedingBoardPosition(getPrecedingBoardPosition());
		clone.playerPosition = playerPosition;

		clone.positionData = positionData;
		clone.hashvalue = hashvalue;

		return clone;
	}

	/**
	 * Returns the number of pushes of this board position.
	 *
	 * @return number of pushes
	 */
	@Override
	public int getPushesCount() {

		int pushesCount = 0;

		for (BoardPosition currentBoardPosition = this; currentBoardPosition
				.getPrecedingBoardPosition() != null; currentBoardPosition = (BoardPosition) currentBoardPosition
				.getPrecedingBoardPosition()) {
			if (currentBoardPosition.getBoxNo() != NO_BOX_PUSHED) {
				pushesCount++;
			}
		}

		return pushesCount;
	}
}