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

/**
 * This is the abstract super class of {@link AbsoluteBoardPosition}
 * and {@link RelativeBoardPosition}.
 * Both store board configurations, but one really stores the locations of the boxes
 * and the player, while the other only stores the difference with respect to
 * another board configuration.
 */
abstract public class BoardPosition implements IBoardPosition {

	/** Number of boxes in board configurations.
	 */
    protected static short boxCount;

    /** Reference to the board which holds the box data. */
    protected static Board board;

    /**
     * The highest encodable box number (in 9 bits) indicates the special case,
     * that there is no last moved (pushed) box.
     * This happens during corral analysis, when a configuration is created without
     * deactivating a box.
     */
    protected static final short NO_BOX_PUSHED = 511;

    /**
     * Random ints for Zobrist hash value calculation.
     * This array is filled in an AbsoluteBoardPosition
    // because that class is first to be instantiated.
     */
    protected static volatile int[] zobristValues = null;


    /** The hash value of this configuration. */
    protected int hashvalue;

    @Override
	public abstract int[] getPositions();
    @Override
	public abstract int getPlayerPosition();
    @Override
	public abstract int getBoxNo();
    @Override
	public abstract int getDirection();
    @Override
	public abstract IBoardPosition getPrecedingBoardPosition();


    /**
     * Returns the hash value of this board position.
     *
     *@return   hash value of this board position
     */
    @Override
	final public int hashCode() {
    	return hashvalue;
    }


    /**
     * Compares two board configurations for equality as hash table entry.
     *
     *@param  boardPosition Objekt, auf das die Gleichheit geprüft werden soll
     *@return true = Objekte sind gleich, false = Objekte sind ungleich
     */
    @Override
	public boolean equals(Object boardPosition) {

        // Compare player positions.
        if(boardPosition == null || getPlayerPosition() != ((IBoardPosition) boardPosition).getPlayerPosition()) {
			return false;
		}

        // Get the positions arrays of both board positions.
        int[] positions = getPositions();
        int[] positionsToBeCompared = ((IBoardPosition) boardPosition).getPositions();

        // 0 is a special position (inactive box) and can occur multiple times.
        // Hence, special logic to check 0 values.
        int zeros1 = 0;
        int zeros2 = 0;
        for(int boxNo=0; boxNo < boxCount; boxNo++){
        	if (positions[boxNo] == 0) {
        		zeros1++;
        	}
        	if (positionsToBeCompared[boxNo] == 0) {
        		zeros2++;
        	}
        }
        if (zeros1 != zeros2) {
        	return false;
        }

        // Compare the positions of all boxes.
        int counter2;
        for(int counter = 0; counter < boxCount; counter++) {
        	for(counter2 = 0; counter2 < boxCount; counter2++){
        		if(positions[counter] == positionsToBeCompared[counter2]) {
        			break;
        		}
        	}
        	if(counter2 == boxCount) {
        		return false;
        	}
        }

        return true;
    }
}