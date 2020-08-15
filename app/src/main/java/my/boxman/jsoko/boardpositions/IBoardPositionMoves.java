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


/**
 * This interface shall be implemented by board configurations
 * (e.g. occurring during a solution search),
 * which do not only know their pushes count,
 * but also know their moves count.
 */
public interface IBoardPositionMoves extends IBoardPosition {
       
    /**
     * Sets the number of moves of this configuration.
     * 
     * @param movesCount  number of moves the player has done up to here
     */
    abstract public void setMovesCount(int movesCount);
        
    /**
     * Returns the total number of moves the player has done to reach this configuration.
     * 
     * @return number of moves up to this configuration	
     */
    abstract public short getTotalMovesCount();
}