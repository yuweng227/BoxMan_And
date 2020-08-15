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
package my.boxman.jsoko.deadlockdetection;

import my.boxman.jsoko.board.Board;
import my.boxman.jsoko.board.DirectionConstants;

/**+
 * This class is used do detect a special type of deadlock on the board:
 * the "<em>closed diagonal deadlocks</em>".
 * <p>
 * A "closed diagonal deadlock" occurs if a closed diagonal cannot be opened and it's
 * impossible to push all boxes to a goal.
 * Examples of closed diagonal deadlocks:<pre>
 *  **               #    *$
 *  * $       *$    $ #   *.#      #
 *   #.$     * #   $.*     # *    # *     $*
 *    # *   * *    $$       * *    $ #   $.#
 *     **   **               $$     #    *$
 *</pre>
 */
public class ClosedDiagonalDeadlock implements DirectionConstants {

	/** Constant for "none" (as in "none position found yet"). */
	final private int NONE = -1;

	/** The board of the game. */
	private Board board;


	/**
	 * Creates an object for detecting "closed diagonal" deadlocks.
	 *
	 * @param board  the board of the current level
	 */
	public ClosedDiagonalDeadlock(final Board board) {

		// Save the reference to the board object for easier access of the board.
		this.board = board;
	}

	/**
	 * Returns whether the current board position is a "closed diagonal" deadlock.
	 *
	 * @param boxPosition  the position of the box that is to be checked for being part
	 *                     of a deadlock
	 * @return <code>true</code> if the board position is a "closed diagonal deadlock"
	 * 		  <code>false</code> otherwise
	 */
	public boolean isDeadlock(int boxPosition) {

		// The player position is only important if the player hasn't moved, yet. The reason for this is
		// that the player can only be part of a closed diagonal when the level is in the start position.
		// Example:
		// **
		// *@*   <- player "in" the diagonal to be investigated
		//  * *
		//   **
		// If the player has already been moved or isn't in a diagonal then the player position is set to
		// 0 for avoiding problems. For instance the "show reachable squares" feature checks for deadlocks
		// BEFORE the player is moved. Example:
		// ##                                                              ##
		// *@ $    board the "show reachable squares" feature will check:  *@$
		//  $$                                                              $$
		// The board that is checked isn't a deadlock because the player is located in the diagonal. However,
		// in reality the player would be outside, hence the situation will be a deadlock. To avoid these
		// false-negative deadlocks the player position is only passed to the check method if the player
		// hasn't moved, yet.

		// Set the player position to be passed for checking for a closed diagonal deadlock. If the player can't
		// move it's possible that he is in a diagonal and therefore the valid player position is used. Otherwise
		// the player position is set to 0.
		int playerPosition = board.isBoxOrWall(board.playerPosition+board.offset[UP])   &&
		board.isBoxOrWall(board.playerPosition+board.offset[DOWN]) &&
		board.isBoxOrWall(board.playerPosition+board.offset[LEFT]) &&
		board.isBoxOrWall(board.playerPosition+board.offset[RIGHT]) ? board.playerPosition : 0;

		return
			// Check the box square and its left neighbor for a closed diagonal deadlock.
			isBoxBlockedOnClosedDiagonalDeadlock(boxPosition + board.offset[LEFT],
					                             boxPosition, playerPosition)
		||
			// Check the box square and its right neighbor for a closed diagonal deadlock.
			isBoxBlockedOnClosedDiagonalDeadlock(boxPosition + board.offset[RIGHT],
					                             boxPosition, playerPosition);
	}


	/**
	 * Returns whether the current board position contains a closed diagonal deadlock.
	 * <p>
	 * Example:<pre>
	 *    *$
	 *   * #
	 *  * *
	 *  **
	 * </pre>
	 *
	 *  How the algorithm works:
	 *  <p>
	 *  There are two diagonals with directions 'right-up' and 'left-up' respectively;
	 *  each '%' square around the closed diagonal contains a box or a wall:
	 * <pre>
	 *  ---%%   left-down/right-up diagonal
	 *  --%-%
	 *  -%-%-
	 *  %-%--
	 *  %%---
	 *
	 *  %%---   left-up/right-down diagonal
	 *  %-%--
	 *  -%-%-
	 *  --%-%
	 *  ---%%
	 * </pre>
	 * The search visits squares along the diagonals as long as there are boxes or walls
	 * to the left and to the right of the diagonal square in the middle, and until a
	 * blocking square (either a box or a wall) has been found in both ends of the diagonal.
	 * <p>
	 * Situation a:
	 * <p>
	 *   If the pushed box belongs to one of the rows in the middle, then the search
	 *   only investigates two diagonals as depicted above, but the search must look
	 *   for a blocking square in each end of these diagonals:
	 *   <pre>
	 *   %%---  <- diagonal is closed at this end
	 *   %-%--
	 *   -%-$@  <- pushed box is in the middle of the rows of the diagonal
	 *   --%-%
	 *   ---%%  <- diagonal is closed at this end
	 *   </pre>
	 *   The search has to investigate the diagonal left-up and the right-up.
	 * <p>
	 * Situation b:
	 * <p>
	 *   If the pushed box is located at the first or the last row in the figure,
	 *   then there is already a blocked end point for the diagonal, and the search
	 *   must investigate 4 separate diagonals, starting from the 2 boxes in that row
	 *   (left/up, right/up, left/down, right/down):
	 *   <pre>
	 * 	  %$---      $%---      ---%$      ---$%    <- pushed box is on the first row
	 *    %-%--      %-%--      --%-%      --%-%       of the whole figure
	 *    -%-%-  or  -%-%-  or  -%-%-  or  -%-%-                          or
	 *    --%-%      --%-%      %-%--      %-%--
	 *    ---%$      ---$%      $%---      $%---    <- pushed box is on the last row
	 *                                                 of the whole figure
	 *   </pre>
	 *   If a diagonal is blocked in both ends by a box or by a wall, and pushing boxes
	 *   inwards always produces new smaller closed diagonals with at least one box
	 *   at a non-goal square, then the closed diagonal is a deadlock.
	 * <p>
	 * Player inside: <br>
	 *   Testing if the player is inside the closed diagonal is not strictly necessary;
	 *   the player cannot be inside a closed diagonal after a push. Making the test
	 *   anyway this deadlock check can also be used for checking if the starting position
	 *   contains a closed diagonal deadlock.
	 * <p>
	 * Box in the middle of the diagonal:<br>
	 *   A diagonal may need to be checked in to directions: up and down. If the first
	 *   search along the diagonal found a goal square or a wall square on the diagonal,
	 *   then the second search along the diagonal in the opposite direction must start
	 *   from this square to test correctly for goal/wall square sequences.
	 * <p>
	 *   Example:
	 * <pre>
	 *   -**-----
	 *   -*.*----  <- goal that is found by the left/up search which is start point of the right/down search
	 *   --*-*---
	 *   ---*-*@-  <- pushed box
	 *   ----*-*-
	 *   -----*-*
	 *   ------*$
	 * </pre>
	 *   In this situation the left/up search reaches the end of the diagonal but
	 *   can't prove the diagonal not to be a deadlock.
	 *   Hence the search must check the diagonal also in the opposite direction.
	 *   This however musn't be done beginning from the pushed box position but
	 *   from the found goal square position.
	 * <p>
	 * End of diagonal:<br>
	 *   A possible closed end of a diagonal may also be a double wall like this:<pre>
	 *    #     <- diagonal is closed at this
	 *   # *    <- end by two walls
	 *    * $@
	 *     * #  <- diagonal is closed at this
	 *      #   <- end by two walls
	 * </pre>
	 * 'All goals and walls' sequence:<br>
	 *   A closed diagonal isn't a deadlock if all boxes are on goals or can be pushed
	 *   to goals. There are two possible ways such a situation can occur:
	 *   <br>
	 *   a) All border of the diagonal contains only goals or walls:<pre>
	 *    **
	 *   * *
	 *   **  </pre>
	 *   b) A block of squares in the diagonal contains of only goals and walls:<pre>
	 *   $$
	 *   $ $
	 *    $.#     <- a block of four goals/walls
	 *     *.$    <- in the diagonal
	 *      $ $
	 *       $$
	 *   </pre>
	 *
	 *   Such a 4 goals/walls block can be used to open the diagonal at both sides.
	 *   Hence it's not a "closed diagonal deadlock".
	 *
	 * @param diagonalStartingPosition  the position of the (presumed) square on the diagonal.
	 *                                  Must be a neighbor of the box on the same row
	 * @param boxPosition    the position of the box checked for being a part of a
	 *                       closed diagonal deadlock
	 * @param playerPosition the position of the player (may differ from the real position
	 *                       on the board)
	 * @return <code>true</code> if the board position is a "closed diagonal" deadlock <br>
	 * 		  <code>false</code> otherwise
	 */
	private boolean isBoxBlockedOnClosedDiagonalDeadlock(int diagonalStartingPosition, int boxPosition, int playerPosition) {

		// Horizontal and vertical direction offsets. For instance: position + dy results in the position under the current position.
		int dx , dy;

		// A diagonal may need to be checked in to directions: up and down. If the first search along the diagonal found
		// a goal square or a wall square, then the second search along the diagonal in the opposite direction must start
		// from this square to test correctly for goal/wall square sequences. Hence the position is saved in this variable.
		int goalOrWallSquareOnDiagonalPosition = NONE;

		// The position of a square on the diagonal.
		int currentDiagonalSquarePosition;

		// Indicates whether the whole checked diagonal consists only of boxes on goals and/or walls. If this is the case
		// no box has to be moved anymore and therefore the pattern isn't a deadlock. Examples:
		// #*     **    **
		// # *   * *   *.#
		//  **   **    **
		boolean isAllGoalsAndWallsSequence;

		// Check whether the starting diagonal square is itself an end point of the diagonal (see "Situation b" in the method description).
		boolean isStartingSquareOneEndOfTheDiagonal = board.isBoxOrWall(diagonalStartingPosition);

		// Start with investigating the left-up diagonal.
		int diagonalHorizontalDirection = LEFT;
		int diagonalVerticalDirection   = UP;

		// Until all diagonals have been investigated, or until a closed diagonal deadlock has been found.
		while(true) {
			// Investigate the first (part of the) diagonal going left / right.
			dx = board.offset[diagonalHorizontalDirection];

			// Investigate the first (part of the) diagonal going up / down.
			dy = board.offset[diagonalVerticalDirection];

			// No goals or walls found on the diagonal yet. If a goal or wall on the diagonal is found then its position is saved here.
			goalOrWallSquareOnDiagonalPosition = NONE;

			// Until the current diagonal has been investigated both upwards and downwards. (Diagonals where the start square itself
			// is blocked by a box or a wall only needs investigation in one direction).
			currentDiagonal:
				while(true) {
					// If the start point itself is a blocked diagonal square then skip to the next diagonal square in the row above/below the start square.
					if(isStartingSquareOneEndOfTheDiagonal == true) {

						// Investigating the 'right-up' or 'right-down' diagonal the diagonal starts at the leftmost square.
						if(dx == board.offset[RIGHT]) {
							currentDiagonalSquarePosition = diagonalStartingPosition < boxPosition ? diagonalStartingPosition : boxPosition;
						}
						// Investigating the 'left-up' or 'left-down' diagonal the diagonal starts at the rightmost square.
						else {
							currentDiagonalSquarePosition = diagonalStartingPosition > boxPosition? diagonalStartingPosition : boxPosition;
						}

						// Check whether the starting diagonal square is a goal square or a wall square and the next square in the row
						// (in the horizontal direction for the diagonal) is a goal square or a wall square, too.
						isAllGoalsAndWallsSequence = board.isGoalOrWall(currentDiagonalSquarePosition) && board.isGoalOrWall(currentDiagonalSquarePosition + dx);

						// move to the row above/below the diagonal starting point to the next diagonal square.
						currentDiagonalSquarePosition += dx + dy;
					}
					else {
						// If no goals or walls found on the diagonal during the first left-going search along the diagonal or this is the first left-going search.
						if(goalOrWallSquareOnDiagonalPosition == NONE) {
							// Start the search at the passed starting position.
							currentDiagonalSquarePosition = diagonalStartingPosition;
							isAllGoalsAndWallsSequence = false;
						} else {
							// The first search along the diagonal found a goal square or a wall square on the diagonal. To test correctly for
							// goals/walls sequences, this second search along the diagonal in the opposite direction must start from this square.

							// Start this search at the found goal because this starts a new 'all goals and walls' sequence.
							currentDiagonalSquarePosition = goalOrWallSquareOnDiagonalPosition;

							// If the next square in this row (according to the horizontal direction of the current search direction along the diagonal) also is
							// a goal or a wall, then this second search along the diagonal begins with an 'all goals and walls' sequence.
							isAllGoalsAndWallsSequence = board.isGoalOrWall(currentDiagonalSquarePosition + dx);

							// Move to the next square of the diagonal so the current goal isn't investigated again. Note: this might cause a jump behind
							// a double wall like this:
							//    .#  <- current diagonal position
							//    #!  <- "!" = new "current diagonal position"
							// However, this is no problem. It's just checked whether the diagonal continues after the double walls.
							currentDiagonalSquarePosition += dx + dy;
						}
					}

					// In a level like this it can happen that the currentDiagonalSquarePosition is outside the reachable board.
					// In this case the square must also be treated as wall:
					//	 #######
					//	#.$@ $.#  <- push to left will also investigate the positions outside the board
					//	 #######
					//  In such a case the the
					if(board.isOuterSquareOrWall(currentDiagonalSquarePosition) && board.isWall(currentDiagonalSquarePosition) == false) {
						break currentDiagonal;
					}

					// Continue until the investigation of the diagonal in the current direction has finished (by jumping out of this while).
					while(true) {

						// If the player is located at a diagonal square (can only occur in the start position) then there isn't a deadlock in this diagonal.
						if(playerPosition == currentDiagonalSquarePosition) {
							break currentDiagonal;
						}

						// Calculate the position of the left and right neighbor (according to the horizontal direction of the diagonal).
						// Example right-up:
						//  adb     a = neighbor 1, d = current diagonal square, b = neighbor 2
						// % %
						// %%
						int diagonalSquareNeighbor1Position = currentDiagonalSquarePosition - dx;
						int diagonalSquareNeighbor2Position = currentDiagonalSquarePosition + dx;

						// If there isn't a box or a wall at the neighbor1 position the diagonal isn't closed. Hence continue with the next diagonal.
						if(board.isBoxOrWall(diagonalSquareNeighbor1Position) == false) {
							break currentDiagonal;
						}

						// If there is an 'all goals or walls' sequence of most recently visited squares around the diagonal
						// check whether this sequence continues.
						if(isAllGoalsAndWallsSequence == true) {

							// Example with the diagonal going 'right-up':
							//
							//   ?!    <---- '!' is the current  diagonal square
							//  %.#    <---- '.' is the previous diagonal square, '#' is the next square in diagonal direction
							// % %
							// %%
							//
							// The previous diagonal square happened to be a goal square, thereby starting a new 'all goals and walls' sequence.
							// The next square (in the diagonal direction) after the previous diagonal square was a goal or a wall (a wall in this example),
							// so it extended the 'all goals and walls' sequence.
							//
							// If the '?' square is a goal or a wall and the current diagonal square '!' also is a goal or a wall, then some of the boxes
							// around the diagonal can be pushed inwards to goal squares and open up the diagonal without creating a deadlock.

							// Check whether the sequence continues with the "?" square.
							isAllGoalsAndWallsSequence = board.isGoalOrWall(diagonalSquareNeighbor1Position);

							// If the "!" square is also a goal or a wall then the diagonal isn't a deadlock.
							if(isAllGoalsAndWallsSequence && board.isGoalOrWall(currentDiagonalSquarePosition)) {
								break currentDiagonal;
							}
						}

						// Check whether the diagonal square is an empty floor square.
						if(board.isBoxOrWall(currentDiagonalSquarePosition) == false) {

							// The current diagonal only continues if there is a box or a wall to the left and right of the current diagonal square.
							// The neighbor 1 has already been checked. Hence, check the other neighbor here, too. If there isn't a box neither a wall at
							// that position the diagonal isn't closed.
							if(board.isBoxOrWall(diagonalSquareNeighbor2Position) == false) {
								break currentDiagonal;
							}

							// Check whether there is a goal at the current diagonal square.
							if(board.isGoal(currentDiagonalSquarePosition)) {

								// The goal on the diagonal square starts a new sequence of 'all goals and walls' squares, or extends an existing sequence.
								isAllGoalsAndWallsSequence = true;

								// If this is the first found goal on the diagonal during the search in this direction remember its position. The search of the
								// diagonal in the opposite direction must then start at this position.
								if(goalOrWallSquareOnDiagonalPosition == NONE) {
									goalOrWallSquareOnDiagonalPosition = currentDiagonalSquarePosition;
								}
							}

							// If there was an 'all goals and walls' sequence of squares on, and around, the most recently visited part of the diagonal,
							// then check whether the next square is also a goal or a wall. If yes, it extends the 'all goals and walls' sequence of squares.
							if(isAllGoalsAndWallsSequence == true) {
								isAllGoalsAndWallsSequence = board.isGoalOrWall(diagonalSquareNeighbor2Position);
							}

							// The diagonal doesn't end with a blocking box or wall at the diagonal square. However, it might end with two walls like this:
							//   #      #      <- currentDiagonalSquarePosition + dy
							//  * #    *d#     <- d = currentDiagonalSquare; '#' = diagonalSquareNeighbor2Position
							// * *    * *
							// **     **
							// The diagonal only continues if it isn't blocked by two walls.
							if(board.isWall(diagonalSquareNeighbor2Position) == false || board.isWall(currentDiagonalSquarePosition+dy) == false) {

								// Advance to the next diagonal square.
								currentDiagonalSquarePosition += dx + dy;

								continue;
							}

							// The search has found an end of the diagonal. If it is an 'all goals and walls' sequence then the diagonal isn't a deadlock. Hence immediately
							// check the next diagonal.
							if(isAllGoalsAndWallsSequence == true) {
								break currentDiagonal;
							}

							// The diagonal ends with an empty floor square having 2 neighboring walls which block the access
							// to the next diagonal square in the current direction. Remember the starting point for the search
							// along the diagonal in the opposite direction (which is only done if "isStartingSquareOneEndOfDiagonal" is false).
							// The starting point is the next diagonal square in the current direction, so the current diagonal square is
							// also examined by the downward search. That is necessary for a correct 'all goals and walls' analysis.
							// The goal is treated as goal because two walls are always a beginning of a 'all goals and walls' sequence.
							//  #!    ! = currentDiagonalSquarePosition + dx + dy
							// * #
							// **
							goalOrWallSquareOnDiagonalPosition = currentDiagonalSquarePosition + dx + dy;
						}

						// At this coding line it's sure that the diagonal is blocked by a box or a wall on the diagonal square or two walls AND
						// that "isAllGoalsAndWallsSequence" isn't true. Examples:
						//  *$  <- blocked by box   *#  <- blocked by wall  #   <- blocked by
						// * *                     * *                     * #	<- two walls
						// **                      $*                      $*

						// If start square is in the middle of the diagonal (see "Situation a" in method description) the search starts with either left-up or right-up.
						// After the up-direction has been investigated the search has still the chance to prove the diagonal not to be a deadlock by searching the diagonal
						// in the opposite direction. Hence the direction is switched here.
						if (isStartingSquareOneEndOfTheDiagonal == false && dy == board.offset[UP]) {

							// If diagonal ends with a wall or a goal square  AND the there hasn't been found a goal in the diagonal, yet,
							// then the search in the opposite direction starts with the "currentDiagonalSquarePosition" which is treated as goal to
							// ensure the 'all goals and walls' sequence is correctly handled.
							if ((board.isWall(currentDiagonalSquarePosition) || board.isGoal(currentDiagonalSquarePosition)) && goalOrWallSquareOnDiagonalPosition == NONE) {
								goalOrWallSquareOnDiagonalPosition = currentDiagonalSquarePosition;
							}

							// Flip the diagonal direction to prepare visiting the squares along the diagonal in the opposite direction (downwards).
							dx = -dx;
							dy = -dy;

							// Investigate the squares on the diagonal in the opposite direction.
							continue currentDiagonal;
						} else {
							// Both ends of the diagonal are blocked by boxes or walls and
							// pushing boxes inwards along the diagonal creates a deadlock.
							// Furthermore, there aren't all boxes on goals at the moment.
							// Hence, the situation has been proven being a deadlock.
							return true;
						}
					} // End of while for current direction of current diagonal
				} // End of while for current diagonal

			// Search the diagonals in this order: left/up, right/up, left/down, right/down.
			if (diagonalVerticalDirection == UP) {
				if(diagonalHorizontalDirection == LEFT) {
					diagonalHorizontalDirection = RIGHT;
				} else {
					// If the pushed box is located at one of the middle rows
					// ("Situation a" in the method description) then the search only
					// needs to investigate the two diagonals 'left-up' and 'right-up'
					// (but during the search the program also must find both end points
					// of the diagonal).
					// At this point of code the left-up and the right-up diagonals
					// have both been investigated and no deadlock has been found.
					if(isStartingSquareOneEndOfTheDiagonal == false) {
						return false;
					}

					diagonalVerticalDirection   = DOWN;
					diagonalHorizontalDirection = LEFT;
				}
			} else {
				if(diagonalHorizontalDirection == LEFT) {
					diagonalHorizontalDirection = RIGHT;
				} else {
					// The search is over; all 4 diagonals emanating from the starting row
					// have been investigated and no deadlock has been found.
					return false;
				}
			}
		}
	}
}