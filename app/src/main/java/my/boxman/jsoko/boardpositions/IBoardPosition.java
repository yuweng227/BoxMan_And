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

import my.boxman.jsoko.resourceHandling.Settings;

/**
 * All kinds of board configurations have to implement this interface,
 * so that we can pass them around just as IBoardPosition,
 * and handle them in a unified way.
 * Board configurations are used by all kinds of solution search.
 * <p>
 * A board configuration includes the position (location) of all boxes and of the player.
 * This interface also contains:
 * <ul>
 *   <li>search direction (of solver algorithm)
 *   <li>pushes count
 *   <li>direction and box number of the last push
 * </ul>
 * <p>
 * This is an interface, because board configurations already derive from
 * {@link AbsoluteBoardPosition} or {@link RelativeBoardPosition},
 * depending on their coding nature.  Using this interface we can handle
 * a mixture of both.
 */
public interface IBoardPosition {

    /**
     * Returns all box positions and the player position in one array.
     *
     * @return an <code>Array</code> containing all box positions and the player position
     */
    abstract int[] getPositions();


    /**
     * Returns the stored player position.
     *
     * @return the player position
     */
    abstract int getPlayerPosition();


    /**
     * Sets the search direction this board position is created in.
     *
     * @param searchDirection the search direction
     */
    abstract void setSearchDirection(Settings.SearchDirection searchDirection);


    /**
     * Returns the search direction this board position has been created in.
     *
     * @return the search direction
     */
    abstract Settings.SearchDirection getSearchDirection();


    /**
     * Returns the number of pushes that have been done for reaching this board position.
     *
     * @return the number of pushes
     */
    abstract int getPushesCount();


    /**
     * Returns the number of the pushed box.
     *
     * @return the number of the pushed box
     */
    abstract int getBoxNo();


    /**
     * Returns the direction the box has been pushed to.
     *
     * @return the direction the box has been pushed to
     */
    abstract int getDirection();


    /**
     * Returns the preceding board position of this board position.
     *
     * @return the preceding board position
     */
    abstract IBoardPosition getPrecedingBoardPosition();
}