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
 * A <code>CorralBoardPosition</code> is a board configuration created during
 * corral analysis.
 * It contains information about the corral, and always is a relative configuration,
 * i.e. codes a difference to another configuration.
 *
 * A CorralBoardPosition stores inactive boxes with a position of 0. These 0-position can only
 * be handled by instances of this class. Hence, only CorralBoardPositions can reference to
 * CorralBoardPositions. If an RelativeBoardPosition has a CorralBoardPosition as
 * preceding board position it won't be able to handle the inactive box positions.
 */
public final class CorralBoardPosition extends RelativeBoardPosition implements Cloneable {

	// Konstanten, die verwendet werden, um festzustellen, ob eine
	// Stellung eine Deadlockstellung ist oder nicht.
	protected static final int CORRAL_DEADLOCK    = 2;
	protected static final int NO_CORRAL_DEADLOCK = 3;

	protected static final int CORRAL_DATA_BITS   = (1 << 19) - 1;
	protected static final int POSITION_DATA_BITS = ~ CORRAL_DATA_BITS;

	// Die Corralnummer wird in den ersten 17 Bit gespeichert. Diese Konstante
	// enthält den Wert, der in den ersten 17 Bits gesetzte Bits enthält,
	// um damit Bitverknüpfungen durchführen zu können.
	protected static final int MAX_CORRAL_NO = (1 << 17) - 1;

	// Wenn das n-te Bit gesetzt ist, bedeutet das, dass die n-te Kiste deaktiv ist (= nicht auf dem Feld)
	protected boolean[] isBoxInactive;

	// Nimmt die Nummer des Corrals auf
	private int corralNo;

	// Diese Flags geben an, ob die Stellung bereits als Deadlock bzw. KeinDeadlock klassifiziert wurde
	private boolean isDeadlock;
	private boolean isNotDeadlock;

	/**
	 * Constructor for cloning.
	 */
	CorralBoardPosition() {
		/*empty*/
	}

	/**
	 * Creates an object for storing a board position occurred in the corral detection.
	 *
	 * @param board  the board of the current level
	 * @param boxNo the number of the pushed box
	 * @param direction the direction the box is pushed to
	 * @param precedingBoardPosition the preceding board position
	 * @param corralNo the number of the corral
	 */
	public CorralBoardPosition(Board board, int boxNo,
							   int direction, IBoardPosition precedingBoardPosition, int corralNo) {

		this(board, boxNo, direction, precedingBoardPosition);
		setCorralNo(corralNo);
	}

	/**
	 * Creates an object for storing a board position occurred in the corral detection.
	 *  @param board  the board of the current level
	 * @param boxNo the number of the pushed box
	 * @param direction the direction the box is pushed to
	 * @param precedingBoardPosition the preceding board position
	 */
	public CorralBoardPosition(Board board, int boxNo,
			int direction, IBoardPosition precedingBoardPosition) {
		super(board, boxNo, direction, precedingBoardPosition);

		isBoxInactive = new boolean[boxCount];

		// The hash value calculated by the super class may be wrong, because there may be some inactive
		// boxes in the current board position. Therefore the hash value for this position has always
		// to be recalculated considering every box again.
		hashvalue = 0;

		// Calculate the hash value using all active boxes.
		for (int boxNo2 = 0; boxNo2 < boxCount; boxNo2++) {
			if (board.boxData.isBoxInactive(boxNo2)) {
				isBoxInactive[boxNo2] = true;
				continue;
			}
			hashvalue ^= zobristValues[board.boxData.getBoxPosition(boxNo2)];
		}

		// The player uses the same zobrist values as the boxes. Hence, a swap of a box and the player position
		// results in the same hash value. But something like "hashvalue ^= zobristValues[playerPosition]>>>1"
		// doesn't seem to pay off ...
		hashvalue ^= zobristValues[playerPosition];
	}

	/**
	 * Returns, whether a box is inactive, and therefore a position value 0
	 * has to be assumed.
	 *
	 * @param  boxNo number of the box to be checked for being inactive
	 * @return <code>true</code> if the box is inactive, and
	 * 		  <code>false</code> if the box is active
	 */
	protected boolean isBoxInactive(int boxNo) {
		return isBoxInactive[boxNo];
	}

	/**
	 * Mark this board position to be a deadlock.
	 */
	public void setCorralDeadlock() {
		isDeadlock = true;
	}

	/**
	 * Mark this board position not to be a deadlock.
	 * <p>
	 * Attention: this board position is only for the current
	 * investigated corral assumed not to be a deadlock.
	 * Nevertheless it can be a deadlock, because not all
	 * deadlocks are detected.
	 */
	public void setNotCorralDeadlock() {
		isNotDeadlock = true;
	}

	/**
	 * Sets the number of the corral this board position belongs to.
	 *
	 * @param corralNo the number of the corral
	 */
	public void setCorralNo(int corralNo) {
		this.corralNo = corralNo;
	}

	/**
	 * Returns the number of the corral this board position belongs to.
	 *
	 * @return the number of the corral
	 */
	public int getCorralNo() {
		return corralNo;
	}

	/* (non-Javadoc)
	 * @see de.sokoban_online.jsoko.boardpositions.RelativeBoardPosition#getPositions()
	 */
	@Override
	public int[] getPositions() {

		int[] boxPositions = super.getPositions();

		// Bei deaktiven Kisten wird die Position auf 0 gesetzt.
		for (int boxNo = 0; boxNo < boxCount; boxNo++) {
			if (isBoxInactive(boxNo)) {
				boxPositions[boxNo] = 0;
			}
		}

		return boxPositions;
	}

	/**
	 * Returns whether this board position has been proven to be a corral deadlock.
	 *
	 * @return <code>true</code> if this board position is a corral deadlock, and
	 * 	     <code>false</code> if this board position has not been proven to be a deadlock
	 */
	public boolean isCorralDeadlock() {
		return isDeadlock;
	}

	/**
	 * Returns whether this board position is classified not to be a corral deadlock.
	 * <p>
	 * This board position can be a corral deadlock, anyhow. For example the corral
	 * detection could be aborted due to a reached time limit. Nevertheless it would
	 * be classified as not to be a corral deadlock.
	 *
	 * @return <code>true</code> this board position has been classified not to be a corral deadlock
	 * 		  <code>false</code> otherwise
	 */
	public boolean isNotCorralDeadlock() {
		return isNotDeadlock;
	}

	/**
	 * Returns whether this board position has been classified (deadlock or no deadlock).
	 * <p>
	 * This method returns the same value as <code>!isBeeingAnalyzed()</code>
	 *
	 * @return <code>true</code> this board position has been classified.
	 * 		<code>false</code> this board position has not been classified yet.
	 */
	public boolean isClassified() {
		return isDeadlock || isNotDeadlock;
	}

	/**
	 * Returns whether is currently being analyzed to be a corral deadlock or not.
	 * <p>
	 * Every corral (= board position) not classified counts as "beeingAnalyzed". Due to every
	 * board position occurred during the corral detection getting a corral number - even if it
	 * isn't a corral at all - finally there can be board positions still having the status
	 * "isBeeingAnalyzed".
	 *
	 * @return <code>true</code> if the deadlock status of this board position
	 *                           is currently being analyzed, and
	 * 		  <code>false</code> otherwise
	 */
	public boolean isBeeingAnalyzed() {
		return !isDeadlock && !isNotDeadlock;
	}

	/* (non-Javadoc)
	 * @see de.sokoban_online.jsoko.boardpositions.RelativeBoardPosition#clone()
	 */
	@Override
	final public Object clone() {

		CorralBoardPosition clone = new CorralBoardPosition();
		clone.setPrecedingBoardPosition(getPrecedingBoardPosition());
		clone.playerPosition = playerPosition;

		// DeaktiveKisten ändert sich nicht, deshalb muss dieses Array an dieser
		// Stelle nicht geklont werden.
		clone.isBoxInactive = isBoxInactive;
		clone.positionData = positionData;
		clone.hashvalue = hashvalue;

		return clone;
	}
}
