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
package my.boxman.jsoko.deadlockdetection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import my.boxman.jsoko.board.Board;
import my.boxman.jsoko.board.BoxData;
import my.boxman.jsoko.board.DirectionConstants;
import my.boxman.jsoko.board.Directions;
import my.boxman.jsoko.boardpositions.AbsoluteBoardPosition;
import my.boxman.jsoko.boardpositions.BoardPosition;
import my.boxman.jsoko.boardpositions.CorralBoardPosition;
import my.boxman.jsoko.boardpositions.IBoardPosition;
import my.boxman.jsoko.resourceHandling.IntStack;
import my.boxman.jsoko.resourceHandling.Settings.SearchDirection;

/**
 *  Class for detecting corral deadlocks.
 *  <p>
 *  Example level:
 *  <pre>
 *  ########
 *  #.  $  #
 *  #.@$   #
 *  ########
 *  </pre>
 *  Pushing the lower box to the right has created an area the player
 *  can't reach (right from the boxes). This area is called "corral"
 *  and is checked for being a deadlock in this class.
 *  向右推动下面的箱子会创建一个玩家无法到达的区域（该箱子右侧），这个区域称为“围栏”。这个类，是检查是否为此类中的死锁。
 */
public final class CorralDeadlockDetection implements DirectionConstants {

	/**
	 * Flag indicating that no box has been pushed. This constant is
	 * used as "box number", hence it's necessary that there are
	 * fewer than 511 boxes on the board.
	 * 指示没有动过的箱子的标志，这个常数被用作“箱子编号”，因此板上的箱子数必须少于511
	 */
	private final short NO_BOX_PUSHED = 511;

	/** Direct reference to the board of the current level. */
	private final Board board;

	/** Object for other deadlock detections than corral deadlock detection. */
	private DeadlockDetection deadlockDetection;

	/**
	 * Flag indicating that the corral deadlock detection has to be aborted due to
	 * reaching the time limit.
	 * 指示围栏死锁检测由于达到时限而必须中止的标志。
	 */
	private boolean isCorralDetectionToBeAborted;

	/** Time when to stop the deadlock detection in order to avoid too long runs.
	 * 停止死锁检测以避免长时间运行的时间
	 */
	private long timeWhenToStopDeadlockDetection;

	/**
	 * Counter for the number of corrals occurred during ALL deadlock detection runs.
	 * Every new found corral gets an own number so corral board positions belonging
	 * to this corral can be identified by checking this number.
	 * 计数器，用于所有死锁检测运行期间出现的围栏数量。
	 * 每个新发现的畜栏都有一个自己的编号，因此可以通过检查这个编号来识别属于这个畜栏的畜栏板位置。
	 */
	private int totalingCorralNo = 0;

	/**
	 * Lowest corral number of a specific deadlock detection run. All corrals found during a specific deadlock detection run (that is:
	 * a call of {@link #isDeadlock(int, long)} have a number >= this number. This number is used to identify corral board positions
	 * stored in the storage of previous deadlock detection runs.
	 * This way the storage needn't to be cleared for every run but the stored information can be reused.
	 * 特定死锁检测运行的最低围栏数。在特定死锁检测运行期间找到的所有畜栏。
	 * 也就是说，{@link#isDeadlock(int,long)}的调用有一个号码，这个数字用于识别存储在先前死锁检测运行的存储中的围栏板位置。
	 * 这样，不必每次运行都清除存储，但是可以重用存储的信息。
	 */
	public int mainCorralNo = 0;

	/** These variables are used to cache the created arrays that store corral information.
	 * 这些变量用于缓存所创建的存储畜栏信息的数组
	 */
	private int indexArraysCache = 0;
	private final ArrayList<byte[]> corralArrayCache;

	/**
	 * Storage for the board positions created during the corral detection. The storage
	 * keeps the board positions as long as this object lives.
	 */
	private final BoardPositionStorage boardPositionsStorage;

	/**
	 * Stack for positions used in method {@link #isACorralDeadlock(int, IBoardPosition, int)}.
	 * Instance variable for better performance.
	 */
	private final IntStack positions;


	/**
	 * The {@code CorralDeadlockDetection} detects corral deadlocks.
	 *  <p>
	 *  Example level:
	 *  <pre>
	 *  ########
	 *  #.  $  #
	 *  #.@$   #
	 *  ########
	 *  </pre>
	 *  Pushing the lower box to the right creates an area the player
	 *  can't reach (right from the boxes). This area is called "corral"
	 *  and is checked for being a deadlock.
	 *
	 * @param board  the board of the current level
	 * @param deadlockDetection  the reference to the deadlock detection object */
	public CorralDeadlockDetection(Board board, DeadlockDetection deadlockDetection) {

		this.board = board;
		this.deadlockDetection = deadlockDetection; // used for other deadlock detection methods

		// Create a storage for storing all board positions during the deadlock detection.
		boardPositionsStorage = new BoardPositionStorage(50000);

		positions = new IntStack(DIRS_COUNT * board.boxCount);

		// Cache for the arrays used in the deadlock detection. This cache is used
		// to reuse the arrays of a previous deadlock detection run.
		corralArrayCache = new ArrayList<byte[]>(1500);

	}

	/**
	 * Returns whether the current board position is a corral deadlock.
	 *
	 * @param newBoxPosition  the new position of the pushed box
	 * @param timeWhenToStopDetection  the system time this deadlock detection has to stop the detection
	 *
	 * @return <code>true</code> if the current board position is a corral deadlock, or
	 *		   <code>false</code> if there couldn't be detected a corral deadlock in the current board position
	 */
	final public boolean isDeadlock(int newBoxPosition, long timeWhenToStopDetection) {

		// The corral detection is aborted as soon as the time stamp passed to this method has been reached.
		isCorralDetectionToBeAborted = false;

		// The stop time is passed and not the ms because the call of this method also needs some time
		// in some cases. Hence, System.currentTimeMillis() + timeLimit wouldn't be that accurate.
		timeWhenToStopDeadlockDetection = timeWhenToStopDetection;

		// All corrals that are checked in this deadlock run have a corral number >= this number.
		mainCorralNo = ++totalingCorralNo;

		// We can reuse all stored arrays in the cache since this is a new deadlock detection run.
		indexArraysCache = 0;

		// Start the deadlock check using the current board.
		// 使用当前的局面启动死锁检查。
		boolean isDeadlock = isACorralDeadlock(newBoxPosition, new AbsoluteBoardPosition(board), 0);

		// Only the classified corrals (-> deadlock or "not deadlock") are useful in the next runs.
		// Hence, the not classified ones can be removed from time to time to keep the storage small.
		if((totalingCorralNo&65535) == 0) {
			Collection<CorralBoardPosition> boardPositions = boardPositionsStorage.values();
			for(Iterator<CorralBoardPosition> iterator = boardPositions.iterator(); iterator.hasNext();) {
				CorralBoardPosition bp = iterator.next();
				if(!bp.isClassified()) {
					iterator.remove();
				}
			}
		}

		return isDeadlock;
	}

	/**
	 * Brute force search to find a way of pushing a box out of the corral or pushing all boxes
	 * to a goal. When none of that is possible the corral is a deadlock situation.
	 *
	 * @param corral  positions that are part of the corral are marked with the passed indicator value
	 * @param corralSquareIndicatorValue  positions marked with this value are part of the corral
	 * @param currentCorralNo  number of the currently analyzed corral
	 * @param currentBoardPosition  the current board position
	 * @param recursionDepth  recursion depth to avoid stack overflows
	 *
	 * @return <code>true</code> deadlock, <code>false</code> not a deadlock
	 */
	private final boolean solveCorral(byte[] corral, byte corralSquareIndicatorValue, int currentCorralNo,
			IBoardPosition currentBoardPosition, int recursionDepth) {

		// Stop search when corral is solved or the stack size may overflow.
		// Note: this method is called from itself after a deadlock check has been performed.
		// Frozen boxes on non-goals (which don't belong to the corral) are therefore considered
		// deadlock by the freeze check. This means it is sufficient to check only corral boxes for
		// being on a goal.
		if (board.boxData.isEveryCorralBoxOnAGoal() || recursionDepth > 1000) {
			return false;
		}

		// The reachable player squares are changed (for instance in method removePushableNotCorralBoxes).
		// However, they are needed for checking which box can be pushed to which direction.
		// Hence, the reachable squares are cloned.
		// This method assumes that the player reachable squares are up-to-date at this point!
		// ("new CorralBoardPosition(...)" for instance updates the reachable squares before this method is called)
		Board.PlayersReachableSquares playersReachableSquares = board.playersReachableSquares.getClone();

		for (int boxNo = 0; boxNo < board.boxCount; boxNo++) {

			// Only "active" boxes that are pushable and part of the corral need to be considered.
			if (board.boxData.isBoxInactive(boxNo) || board.boxData.isBoxFrozen(boxNo) || !board.boxData.isBoxInCorral(boxNo)) {
				continue;
			}

			int boxPosition = board.boxData.getBoxPosition(boxNo);

			// Terminate the corral search if it lasts too long.
			if (System.currentTimeMillis() > timeWhenToStopDeadlockDetection) {
				isCorralDetectionToBeAborted = true;
				return false;
			}

			// Try to push the box to every direction.
			for (int direction = 0; direction < DIRS_COUNT; direction++) {

				int newBoxPosition = board.getPosition(boxPosition, direction);

				// Continue with next direction if the box can't be pushed to that direction.
				if (!playersReachableSquares.isSquareReachable(board.getPositionAtOppositeDirection(boxPosition, direction))
						|| !board.isAccessibleBox(newBoxPosition)) {
					continue;
				}

				int playerPositionBackup = board.playerPosition;

				// Do the push on the board.
				board.pushBox(boxPosition, newBoxPosition);
				board.playerPosition = boxPosition;

				// The new board situation has to be saved in the storage => create a board position to be stored.
				CorralBoardPosition newBoardPosition = new CorralBoardPosition(board, boxNo, direction, currentBoardPosition, currentCorralNo);

				// Check if the storage already contains a duplicate of the board position.
				CorralBoardPosition oldBoardPosition = boardPositionsStorage.getBoardPosition(newBoardPosition);

				// Attention: if the oldBoardPosition has been considered not to be a deadlock,
				// we nevertheless can't immediately return "false" here!
				// It may be that oldBoardPosition has been investigated in context of a "small" corral,
				// but the current corral may be part of a bigger corral, which might not be solvable!

				// Continue with the next direction if:
				// - the current board position has been analyzed (and therefore stored) before being identified as a deadlock
				// - the current board position has been reached before during the analysis of the current corral
				// - the current board position is a deadlock
				if (oldBoardPosition != null
						&& (oldBoardPosition.isCorralDeadlock() || oldBoardPosition.getCorralNo() == currentCorralNo)
						|| isADeadlock(newBoxPosition, newBoardPosition, recursionDepth+1)) {

					// Undo the push.
					board.pushBoxUndo(newBoxPosition, boxPosition);
					board.playerPosition = playerPositionBackup;

					// Falls die Deadlockprüfungen durchlaufen wurden und ein Deadlock
					// erkannt wurde, so wird dieses Deadlock hier nicht für die aktuelle
					// Stellung gespeichert, denn:
					// sollte durch den Zug ein weiteres Corral entstanden sein, würde die
					// Stellung in "isACorralDeadlock" bereits als Deadlock markiert werden.
					// In allen anderen Fällen ist die Deadlockerkennung recht schnell und kann
					// deshalb jedes Mal erneut durchgeführt werden!
					// (vielleicht später einmal ändern, falls "n Kisten für n-y Zielfelder"-Deadlocks
					// auch eine lange Laufzeit zur Erkennung benötigen)
					// Es kann aber natürlich auch sein, dass sehr viele Züge gemacht werden müssen,
					// bis festgestellt wurde, dass es ein Deadlock ist, ohne dass dabei ein neues
					// Corral aufgetreten ist. In diesem Fall könnte es sich auch lohnen die Stellung
					// als Deadlock zu speichern)

					// Board position is already analyzed for this corral or it's a deadlock => continue with next direction
					continue;
				}

				// If the box has left the corral the whole corral is considered NOT to be a deadlock.
				if (corral[newBoxPosition] != corralSquareIndicatorValue) {

					// Restore the original board.
					board.pushBoxUndo(newBoxPosition, boxPosition);
					board.playerPosition = playerPositionBackup;

					return false;
				}

				// Remove all pushable boxes that aren't part of the corral.
				boolean isABoxSetInactive = removePushableNotCorralBoxes();

				// Die erreichte Stellung als während der Corralanalyse erreicht kennzeichnen,
				// indem ein RelativeStellungobjekt zur Stellung gespeichert wird.
				// (um Rekursionen zu vermeiden)
				// Obwohl dieser Code hier nur durchlaufen wird, nachdem etwas weiter oben
				// deadlockprüfungen(...) mit dem Ergebnis "Kein Deadlock" durchlaufen wurde,
				// darf die Stellung nicht als KeindDeadlock gespeichert werden!!!
				// Denn die Deadlockprüfungen erkennen ja nicht jedes Deadlock!
				// Um also auch etwas kompliziertere Corraldeadlocks erkennen zu müssen,
				// muss hier solange geprüft werden, bis feststeht, dass das Corral
				// nicht durchbrochen werden kann.

				// If any pushable box has been removed by setting it "inactive" a new board position has
				// been created. Hence, create an object for this new board position.
				if (isABoxSetInactive == true) {
					newBoardPosition = new CorralBoardPosition(board, boxNo, direction, currentBoardPosition, currentCorralNo);
				}

				boardPositionsStorage.storeBoardPosition(newBoardPosition);

				// Durch das eventuelle Deaktivieren einiger Kisten kann eine neue Stellung entstanden
				// sein. Wurden Kisten deaktiviert, so kann keine Suchstellung herausgekommen sein
				// (= es kann keine Stellung herausgekommen sein, die eine Suchrichtung hat).
				// Wurden keine Kisten deaktivert, so hat sich die Stellung nicht geändert.
				// Die Stellung wurde aber schon weiter oben auf Gleichheit mit einer Suchstellung
				// geprüft, so dass dies hier nicht noch einmal gemacht werden muss.

				// Try to solve the corral in the next recursion depth.
				boolean isSolvingToBeContinued = solveCorral(corral, corralSquareIndicatorValue, currentCorralNo, newBoardPosition, recursionDepth+1);

				// Restore the original board.
				board.pushBoxUndo(newBoxPosition, boxPosition);
				board.playerPosition = playerPositionBackup;

				// If "isSolvingToBeContinued" is false this means a deeper recursion depth has
				// shown that the corral can't be proven to be a deadlock (box can be pushed
				// out of the corral or all boxes can be pushed to goals). In this case the whole
				// corral isn't a deadlock => return false.
				if (isSolvingToBeContinued == false) {
					return false;
				}
			}
		}

		// The corral analyzed is a deadlock.
		return true;
	}

	/**
	 * Removes all boxes from the board that are pushable to any direction and aren't part of the corral.
	 * <p>
	 * The corral deadlock detection tries to push a box out of the corral or to push all boxes of the corral to a goal.
	 * It's too complex to consider all boxes on the board when trying to do that (in fact, this is requires a complete solver coding).
	 * Hence, all boxes that aren't part of the corral are removed, so the corral coding just has to deal with the corral boxes.
	 * However, boxes that can't be pushed at all (without pushing a corral box first) can stay on the board. These boxes may block
	 * the player and the reachable squares for the corral boxes which may help to prove that a corral is a deadlock.
	 *
	 * @return <code>true</code> at least one box has been removed, or
	 *		   <code>false</code> no box has been removed
     */
	final private boolean removePushableNotCorralBoxes() {

		boolean atLeastOneBoxHasBeenRemoved = false;

		board.playersReachableSquares.update();

		boolean aBoxHasBeenRemoved = true;

		// If any box has been removed then ALL the other boxes have to be checked again.
		while (aBoxHasBeenRemoved == true) {
			aBoxHasBeenRemoved = false;

			for (int boxNo = 0; boxNo < board.boxCount; boxNo++) {

				// Only boxes that are active (= logically on the board), not frozen and not part of the corral may be removed.
				if (board.boxData.isBoxInactive(boxNo) || board.boxData.isBoxFrozen(boxNo) || board.boxData.isBoxInCorral(boxNo)) {
					continue;
				}

				int boxPosition = board.boxData.getBoxPosition(boxNo);

				for (int direction = 0; direction < DIRS_COUNT; direction++) {

					int newBoxPosition = board.getPositionAtOppositeDirection(boxPosition, direction);

					// Continue with next direction if box isn't pushable.
					if (!board.playersReachableSquares.isSquareReachable(board.getPosition(boxPosition, direction))
				  	 || !board.isAccessibleBox(newBoxPosition)) {
						continue;
					}


					// Box can be pushed => remove it.
					board.removeBox(boxPosition);
					board.boxData.setBoxInactive(boxNo);

					aBoxHasBeenRemoved = true;
					atLeastOneBoxHasBeenRemoved = true;

					// The removed box allows the player to reach more positions. Removing a box from the board
					// can only increase the number of reachable squares. Hence, we just have to enlarge the
					// reachable area of the player.
					board.playersReachableSquares.enlarge(boxPosition);

					// Box has been removed. Hence, no further check necessary.
					break;
				}
			}
		}

		return atLeastOneBoxHasBeenRemoved;
	}

	/**
	 * This method is called during the search for corral deadlocks: during the search there may occur "sub corrals", so this
	 * method is called by "solveCorral" to find corral deadlocks in these sub corrals.
	 * <p>
	 * The current board position is checked for deadlocks.
	 *
	 * @param newBoxPosition  new position of the pushed box
	 * @param currentPosition current board position
	 * @param recursionDepth  recursion depth to avoid stack overflows
	 *
	 * @return <code>true</code>board position is a deadlock; <code>false</code> otherwise
	 */
	private final boolean isADeadlock(int newBoxPosition, BoardPosition currentPosition, int recursionDepth) {

		// For the freeze check frozen boxes aren't marked, because the corral deadlock detection doesn't benefit
		// much from this information. If used all boxes have to be set to "not frozen" after every push and
		// every time the blocker boxes have to determined new. This would last too long.
		// The normal deadlock detection (#deadlockDetection.isDeadlock) can't
		// be used because the corral deadlock detection has to be called using
		// the internal method "isACorralDeadlock".
		return board.isSimpleDeadlockSquare(newBoxPosition)
				|| deadlockDetection.freezeDeadlockDetection.isDeadlock(newBoxPosition, false)
				|| deadlockDetection.closedDiagonalDeadlockDetection.isDeadlock(newBoxPosition)
				|| deadlockDetection.bipartiteDeadlockDetection.isDeadlock(SearchDirection.FORWARD)
				|| isACorralDeadlock(newBoxPosition, currentPosition, recursionDepth+1);
	}

	/**
	 * Checks if there is an area the player can't reach that makes the level unsolvable.
	 *
	 * @param newBoxPosition  new box position
	 * @param currentPosition current board position on the board
	 * @param recursionDepth  recursion depth to avoid stack overflows
	 *
	 * @return {@code true} if there is a deadlock, {@code false} = otherwise
	 */
	private final boolean isACorralDeadlock(int newBoxPosition, IBoardPosition currentPosition, int recursionDepth) {

		// Constant indicating potential corral squares -> starting from positions marked with
		// this constants corrals are searched.
		final byte POTENTIAL_CORRAL_SQUARE = 1;

		// Create a new array for storing the corral information.
		final byte[] corral = getCorralArray();

		// Value for marking the positions belonging to the current corral.
		byte corralSquareIndicatorValue = -1;

		// Create a backup of the current box data.
		final BoxData boxDataBackup = (BoxData) board.boxData.clone();

		/* Now all potential corral positions are collected. Starting from the pushed box all adjacent boxes are identified
		 * and all adjacent boxes of these boxes.
		 * All positions next to one of these boxes are collected and treated as "potential corral positions". */
		// Start with the last pushed box.
		positions.add(newBoxPosition);
		corral[newBoxPosition] = POTENTIAL_CORRAL_SQUARE;

		while (!positions.isEmpty()) {
			int position = positions.remove();

			if (board.isBox(position)) {
				for (int direction = 0; direction < DIRS_COUNT; direction++) {
					int neighborPosition = board.getPosition(position, direction);
					if (corral[neighborPosition] != POTENTIAL_CORRAL_SQUARE && !board.isWall(neighborPosition)) {
						positions.add(neighborPosition);
						corral[neighborPosition] = POTENTIAL_CORRAL_SQUARE;
					}
				}
			}
		}

		/* A corral is an area the player can't enter. The positions marked as potential corral positions are now checked
		 * for being individual corrals. Each of those corrals is checked for being a deadlock situation. */

		// Process all corrals one after the other.
		for (int position = board.firstRelevantSquare; position < board.lastRelevantSquare && !isCorralDetectionToBeAborted; position++) {

			if (corral[position] != POTENTIAL_CORRAL_SQUARE || board.isBox(position)) {
				continue;
			}

			// Mark each player reachable position.
			board.playersReachableSquares.update(corral, --corralSquareIndicatorValue, position);

			// If the corral includes the current player position it' irrelevant.
			if (corral[board.playerPosition] == corralSquareIndicatorValue) {
				continue;
			}

			for (int boxNo = 0; boxNo < board.boxCount; boxNo++) {

				if (board.boxData.isBoxInactive(boxNo)) {
					continue;
				}

				int boxPosition = board.boxData.getBoxPosition(boxNo);

				int boxPositionUp    = board.getPosition(boxPosition, UP);
				int boxPositionDown  = board.getPosition(boxPosition, DOWN);
				int boxPositionLeft  = board.getPosition(boxPosition, LEFT);
				int boxPositionRight = board.getPosition(boxPosition, RIGHT);

				if (corral[boxPositionUp]    == corralSquareIndicatorValue && !board.isBox(boxPositionUp)
				 || corral[boxPositionDown]  == corralSquareIndicatorValue && !board.isBox(boxPositionDown)
				 || corral[boxPositionLeft]  == corralSquareIndicatorValue && !board.isBox(boxPositionLeft)
				 || corral[boxPositionRight] == corralSquareIndicatorValue && !board.isBox(boxPositionRight)) {
					// The box is part of the corral because there is a neighbor position which is part of the corral.
					// The position of the box is also part of the corral.
					// Note: corral positions having a box aren't considered as corral neighbor positions!
					corral[boxPosition] = corralSquareIndicatorValue;
					board.boxData.setBoxInCorral(boxNo);
				} else {
					// The box isn't part of the corral. It may however still be marked as
					// such from a higher recursion depth. Hence, remove it from the corral.
					board.boxData.removeBoxFromCorral(boxNo);
				}
			}

			/* At this point all boxes adjacent to the corral have been marked as corral boxes. */

			// Now all boxes which are immovable are also marked as corral boxes.
			for (int boxNo = 0; boxNo < board.boxCount; boxNo++) {

				if (!board.boxData.isBoxInCorral(boxNo)) {
					continue;
				}

				int boxPosition = board.boxData.getBoxPosition(boxNo);

				for (int direction = 0; direction < DIRS_COUNT; direction++) {
					int neighborSquare = board.getPosition(boxPosition, direction);

					// Check there is a neighbor box which isn't part of the corral, yet.
					if (!board.isBox(neighborSquare) || board.boxData.isBoxInCorral(board.getBoxNo(neighborSquare))) {
						continue;
					}

					/* Check if the box is blocked on the other axis, too => immovable. */
					int orthogonalDirection  = Directions.getOrthogonalDirection(direction);
					int neighbor1 = board.getPosition(neighborSquare, orthogonalDirection);
					int neighbor2 = board.getPositionAtOppositeDirection(neighborSquare, orthogonalDirection);
					if (
							// Blocked by a wall
							board.isWall(neighbor1) || board.isWall(neighbor2)
							||

							// Blocked by a box of the corral
							(board.isBox(neighbor1) && board.boxData.isBoxInCorral(board.getBoxNo(neighbor1)))
							|| (board.isBox(neighbor2) && board.boxData.isBoxInCorral(board.getBoxNo(neighbor2))) ||

							// Immoveable due to deadlock squares
							(board.isSimpleDeadlockSquare(neighbor1) && board.isSimpleDeadlockSquare(neighbor2))) {

						// The box is immovable. It's added to the corral.
						corral[neighborSquare] = corralSquareIndicatorValue;
						board.boxData.setBoxInCorral(board.getBoxNo(neighborSquare));
					}
				}
			}


			boolean isDeadlock = false;

			// Note: we also have to check corrals that contain only one box, because the
			// "BipartiteMatchings"-Deadlock test wouldn't detect them.

			// The corral detection only considers the corral boxes and immovable boxes for a better performance.
			removePushableNotCorralBoxes();

			// Create a new board position for storing the current board for the new corral to be checked.
			CorralBoardPosition newBoardPosition = new CorralBoardPosition(board, NO_BOX_PUSHED, 0, currentPosition, ++totalingCorralNo);

			// Store the new board position in the transposition table.
			CorralBoardPosition oldBoardPosition = boardPositionsStorage.storeBoardPosition(newBoardPosition);

			// Check the corral for deadlock, if it's hasn't been checked before -> == null.
			// However, since the corral detection may have been aborted due to time constraints the old
			// board position may not have been classified, yet. Hence, it may still be in status -> "isBeeingAnalyzed"
			// In that case the corral has to be checked for being a deadlock, too.
			// mainCorralNo: the number of the first corral that is investigated. All sub corrals found during the deadlock check
			// have a higher number. This means: all board position having a lower corral number are old and hence are not
			// further investigated in the current deadlock detection run.
			if (oldBoardPosition == null
					|| (oldBoardPosition.isBeeingAnalyzed() && oldBoardPosition.getCorralNo() < mainCorralNo)) {

				// Every corral gets a unique number calculated in the instance variable "totalingCorralNo", since this is a method
				// called recursively: solveCorral->IsADeadlock->isACorralDeadlock->solveCorral->...
				// Hence, while trying to "solve" a corral a new sub corral may occur which has to be solved first.
				// For every saved corral board position the number of the corral it belongs to is saved.

				// Every corral board position is saved in a transposition table. This way duplicates can be detected that may
				// occur in levels like this:
				// ############
				// #          #
				// #   #$ #   #
				// #####+ #####
				//     ####
				// pushing the box up results in a corral. While solving the corral the box is pushed out of the corral to the right.
				// This creates a new corral on the right. While solving that new corral the box is pushed back the left, ...

				// Check the new corral for being a deadlock.
				isDeadlock = solveCorral(corral, corralSquareIndicatorValue, totalingCorralNo, newBoardPosition, recursionDepth+1);

				if (isDeadlock) {
					newBoardPosition.setCorralDeadlock();
				} else {
					newBoardPosition.setNotCorralDeadlock();
				}
			}

			// The corral check is over => Restore the board for checking the next corral.
			for (int boxNo = 0; boxNo < board.boxCount; boxNo++) {
				// Remove boxes of corral.
				if (board.boxData.isBoxActive(boxNo)) {
					board.removeBox(board.boxData.getBoxPosition(boxNo));
				}

				// Set the original active boxes back on the board.
				if (boxDataBackup.isBoxActive(boxNo)) {
					board.setBoxWithNo(boxNo, boxDataBackup.getBoxPosition(boxNo));
				}
			}
			board.boxData = (BoxData) boxDataBackup.clone();

			// If a deadlock has been detected no further corral has to be checked. We can return the result immediately.
			if (isDeadlock || oldBoardPosition != null && oldBoardPosition.isCorralDeadlock()) {
				return true;
			}

			// The current analyzed corral isn't a deadlock. We now check the next corral.
			// Only of all the corrals of the current board position aren't a deadlock we return "false".
		}

		return false;
	}

	/**
	 * The deadlock detection needs many byte arrays of the size "board.size". Therefore this method
	 * stores the created arrays in a list. When the next deadlock detection is started these arrays
	 * can be reused which results in a better performance.
	 * 死锁检测需要许多大小为“board.size”的字节数组。因此，此方法将创建的数组存储在列表中。
	 * 当下一次死锁检测开始时，这些数组可以被重用，从而获得更好的性能。
	 * @return byte array for corral information
	 */
	final private byte[] getCorralArray() {

		// For every call of "isDeadlock" this counter is set back to 0.
		// Then this method can reuse the arrays stored in the previous call of "isDeadlock".
		indexArraysCache++;

		// Return a new array if none resuable is available at the momement.
		if (indexArraysCache > corralArrayCache.size()) {
			byte[] corral = new byte[board.size];
			corralArrayCache.add(corral);
			return corral;
		}

		// Reuse an array already stored.
		byte[] corral = corralArrayCache.get(indexArraysCache - 1);
		Arrays.fill(corral, (byte) 0);

		return corral;
	}

	/**
	 * DEBUG: show statistics about the stored corral board positions.
	 */
	public void debugShowStatistic() {
		boardPositionsStorage.debugShowStatistic();
	}

	/**
	 * Storage for {@code CorralBoardPosition}s.
	 * <<p>
	 * A hash table is used to store the board positions.
	 * Board positions with the same hash value are stored in a linked list
	 * in the same slot of the hash table.
	 */
	@SuppressWarnings("serial")
	public class BoardPositionStorage extends HashMap<CorralBoardPosition, CorralBoardPosition> {

		/**
		 * Creates an object for storing board positions in a hash table.
		 *
		 * @param initialCapacity	the initial capacity of this hash table.
		 */
		public BoardPositionStorage(int initialCapacity) {
			super(initialCapacity);
		}

		/**
		 * Stores the passed board position in this storage.
		 * The calling method assumed the passed board position really to be stored
		 * in the hash table, viz. when the board position is changed later in the
		 * program it's assumed to also be changed in the hash table.
		 * Nevertheless, in many cases this method knows that the passed board position
		 * will never be changed. Therefore the board position isn't saved when an equivalent
		 * board position is already stored in the hash table.
		 *
		 * @param boardPosition  board position to be stored
		 * @return an equivalent board position to the passed one that has
		 *         been replaced by the new passed board position.
		 */
		public CorralBoardPosition storeBoardPosition(CorralBoardPosition boardPosition) {

			// Get the board position that is equal to the passed one.
			CorralBoardPosition oldCorralBoardPosition = get(boardPosition);

			// If there isn't an equivalent board position in the hash table store the passed one and return.
			if (oldCorralBoardPosition == null) {
				return put(boardPosition, boardPosition);
			}

			// Handle the case when the old corral board position is from an old corral that
			// has been analyzed before the current corral analyzing run.
			if (oldCorralBoardPosition.getCorralNo() < mainCorralNo) {
				put(boardPosition, boardPosition);

				if (oldCorralBoardPosition.isCorralDeadlock()) {
					boardPosition.setCorralDeadlock();
				}
				if (oldCorralBoardPosition.isNotCorralDeadlock()) {
					boardPosition.setNotCorralDeadlock();
				}

				return oldCorralBoardPosition;
			}

			// The corral board position belongs to the current search depth. This means the current board position has
			// already been stored in the transposition table.
			// gespeichert. Therefore we can return immediately.
			// If a new classification is to be saved for that board position that is done directly by calling
			// the method on the board position object (see "isACorralDeadlock").
			if (oldCorralBoardPosition.getCorralNo() == boardPosition.getCorralNo()) {
				return oldCorralBoardPosition;
			}

			// Die Corralstellung gehört zum aktuellen Hauptcorral.
			// Denn der Fall, dass es eine Stellung zu einem alten Hauptcorral
			// ist, wurde weiter oben bereits abgefangen (-> "< g_NrHauptcorral")
			// (Hauptcorral = das erste Corral, was erkannt wird während der
			// Corralanalyse. Beim Versuch dieses Hauptcorral zu durchbrechen
			// können dann Subcorrals entstehen, die dann in tieferliegenden
			// Suchebenen analysiert werden).
			// Die alte Stellung ist eine also eine Stellung, die in einer höheren
			// Suchebene bereits erreicht wurde.
			// Diese höhere Ebene kann bereits klassifiziert worden sein!
			// Dieser Fall tritt z.B. ein, wenn das gleiche Subcorral mehrmals
			// während der Suche erreicht wird und bereits beim ersten mal
			// als Deadlock klassifiziert wurde.
			if (oldCorralBoardPosition.getCorralNo() < boardPosition.getCorralNo()) {
				// Falls die alte Stellung bereits klassifiziert ist,
				// so reicht es die neue Corralnr zu setzen (wenn dies überhaupt
				// notwendig ist ?!)
				if (oldCorralBoardPosition.isClassified()) {
					oldCorralBoardPosition.setCorralNo(boardPosition.getCorralNo());
				} else {
					// Es darf nicht einfach die neue Corralnr gesetzt werden, denn sonst würde
					// dieses Corral ja plötzlich zum aktuellen Corral gehören und für das Corral
					// in der höheren Ebene gälte es nicht mehr als schon erreicht!
					put(boardPosition, boardPosition);
				}

				return oldCorralBoardPosition;
			}

			// The old board position has been created in a deeper search depth.
			if (oldCorralBoardPosition.getCorralNo() > boardPosition.getCorralNo()) {
				put(boardPosition, boardPosition);
				if (oldCorralBoardPosition.isCorralDeadlock()) {
					boardPosition.setCorralDeadlock();
				}

				// A "not a deadlock" classification of the old board position is
				// used in the new one, too. This is done, although it's not known
				// if the current corral is actually as big in the higher recursion depth
				// as it is now. When it's bigger in the higher depth, it's possible
				// that the same board position is nonetheless a deadlock.
				// However, this case is handled in "solveCorral".
				if (oldCorralBoardPosition.isNotCorralDeadlock()) {
					boardPosition.setNotCorralDeadlock();
				}

				return oldCorralBoardPosition;
			}


			return oldCorralBoardPosition;
		}

		/**
		 * Returns the board position stored under the passed key.
		 * <p>
		 * This is equal to {@link #get(Object)}. It's an own method
		 * because there is also an extra method to store; additionally
		 * this extra method can be used to find all calls in JSoko
		 * to get a board position from this storage using Eclipse tools.
		 *
		 * @param boardPosition  the "key" to return the board position for
		 * @return the stored board position
		 */
		public CorralBoardPosition getBoardPosition(CorralBoardPosition boardPosition) {
			return get(boardPosition);
		}

		/**
		 * Debug method: prints statistics about the stored board positions.
		 */
		public void debugShowStatistic() {

			int deadlocksFound = 0;
			int consideredNotADeadlock = 0;
			int beingAnalyzed = 0;
			int errors = 0;

			Collection<CorralBoardPosition> collection = values();
			for (CorralBoardPosition bp : collection) {
				if(bp.isBeeingAnalyzed()) {
					beingAnalyzed++;
				}
				if(bp.isCorralDeadlock()) {
					deadlocksFound++;
				}
				if(bp.isNotCorralDeadlock()) {
					consideredNotADeadlock++;
				}
				if(bp.isCorralDeadlock() && bp.isNotCorralDeadlock()) {
					errors++;
				}
			}

			System.out.println("\n\ncorral storage statistics");
			System.out.println("-------------------------\n");
			System.out.println("Number of stored board positions: " + size());
			System.out.println("Number of deadlocks:      " + deadlocksFound);
			System.out.println("Number of not deadlocks:  " + consideredNotADeadlock);
			System.out.println("Number of being analyzed: " + beingAnalyzed);
			if(errors > 0) {
				System.out.println("Number of errors: "+errors);
			}
			System.out.println("-------------------------\n");
		}
	}
}