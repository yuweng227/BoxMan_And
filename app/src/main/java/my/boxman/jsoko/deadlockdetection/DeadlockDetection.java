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
import my.boxman.jsoko.resourceHandling.Settings;
import my.boxman.jsoko.resourceHandling.Settings.SearchDirection;

/**
 * This class checks the board for a deadlock.
 */
public final class DeadlockDetection {

	// For easy access we use a direct reference to the board.
	Board board;

	public int deadlockType = 0;

	/** Object for detecting corral deadlocks. */
	public final CorralDeadlockDetection corralDeadlockDetection;

	/** Object for detecting freeze deadlocks. */
	public final FreezeDeadlockDetection freezeDeadlockDetection;

	/** Object for detecting closed diagonal deadlocks. */
	public final ClosedDiagonalDeadlock closedDiagonalDeadlockDetection;

	/** Object for detecting bipartite matching deadlocks. */
	public final BipartiteMatchings bipartiteDeadlockDetection;


	/**
	 * Creates an object for the deadlock detection.
	 *
	 * @param board  the board of the current level
	 */
	public DeadlockDetection(Board board) {

		this.board = board;

		// Create the objects for the deadlock detection.
		corralDeadlockDetection 		= new CorralDeadlockDetection(board, DeadlockDetection.this);
		freezeDeadlockDetection 		= new FreezeDeadlockDetection(board);
		closedDiagonalDeadlockDetection = new ClosedDiagonalDeadlock(board);
		bipartiteDeadlockDetection 		= new BipartiteMatchings(board);

	}

	/**
	 * Searches for deadlock pattern on the board.<br>
	 * If a deadlock is recognized, the board is definitely unsolvable.<br>
	 * <p>
	 * Note: if no deadlock is recognized this just means that JSoko
	 * can't detect any deadlock but the board nevertheless may contain
	 * an unknown deadlock pattern.
	 *
	 * @return <code>true</code> when the current board is not solvable anymore, <code>false</code> otherwise
	 */
	public boolean isDeadlock() {
		return isDeadlock(board.boxData.getBoxPosition(0));
	}

	public boolean isDeadlock(int x, int y) {
		return isDeadlock(x + board.width * y);
	}

	/**
	 * Searches for deadlock pattern on the board.<br>
	 * If a deadlock is recognized, the board is definitely unsolvable.<br>
	 * <p>
	 * Note: if no deadlock is recognized this just means that JSoko
	 * can't detect any deadlock but the board nevertheless may contain
	 * an unknown deadlock pattern.
	 *
	 * @param newBoxPosition  new position of the pushed box
	 * @return <code>true</code> when the current board is not solvable anymore, <code>false</code> otherwise
	 */
	public boolean isDeadlock(int newBoxPosition) {

		deadlockType = 0;

        if (newBoxPosition < 0 || newBoxPosition >= board.size) {
           return false;
        }

        // “简单”死锁与“冻结”型死锁，可以并称为“阻滞”型死锁

		/**
		 * Detection of 'Simple deadlocks'.
		 * “简单”死锁的检测
		 */
		if (Settings.detectSimpleDeadlocks && board.isSimpleDeadlockSquare(newBoxPosition)) {
			deadlockType = 1;
			return true;
		}

		long timeLimit = System.currentTimeMillis() + 150; // 100 milli seconds for the deadlock detection（将整个死锁检测，控制在 100 毫秒之内。暂时改为 200 毫秒）

		/**
		 * Detection of 'Immovable box on non-goal' deadlocks.
		 * 不可移动且不在目标位的箱子的死锁检测。或称之为“冻结”型死锁的检测
		 */
		if (Settings.detectFreezeDeadlocks) {

            // All frozen boxes are marked as "frozen". Checking all boxes takes some time.
            // Hence, it's assumed that the user hasn't continued playing when a freeze deadlock has
			// occurred earlier. This means we only check those frozen boxes which don't create a
			// deadlock situation that is: boxes on goals. Nevertheless, the freeze check will also
			// mark those boxes as frozen which are not on a goal that constitute in the "frozen" status
			// of a box on a goal. This means only frozen boxes on a non goal that don't contribute
			// to a "frozen" status of a "box on a goal" aren't marked as frozen here, like these two:
			// 为所有被“冻结”的箱子做标记，这需要一些时间，同时，此检查属于“时点”性质。
			// 若“冻结”组合中包含非目标位的箱子，就会造成死锁。
            // #######
            // # $$  #
            // #     #
            // ~~~~~~~
            board.boxData.setAllBoxesNotFrozen();
            for (int boxNo = 0; boxNo < board.boxCount; boxNo++) {
                int boxPosition = board.boxData.getBoxPosition(boxNo);
                if (board.isBoxOnGoal(boxPosition) && board.boxData.isBoxFrozen(boxNo) == false) {
                    if (freezeDeadlockDetection.isDeadlock(boxPosition, true)) {
                        if (boxPosition == newBoxPosition) {  // 快手添加：仅对“当前”箱子报告死锁
                            deadlockType = 2;
                            return true;
                        }
                    }
                }
            }

            // Now check the freeze and deadlock status of the pushed box. This needn't to be done
			// 现在检查被推箱子的冻结和死锁状态，但这不是必需的。
            // if the box is on a goal, since in that case the check would have already been done above.
			// 因为，若该箱子在目标位上，上面的检查就已经完成了。
            if (!board.isBoxOnGoal(newBoxPosition) && freezeDeadlockDetection.isDeadlock(newBoxPosition, true)) {
				deadlockType = 2;
                return true;
            }
		}

		/**
		 * Detection of 'Closed diagonal' deadlocks.
		 * “对角”型死锁的检测，或称之为“双 L”型死锁的检测
		 */
		if(Settings.detectClosedDiagonalDeadlocks == true && closedDiagonalDeadlockDetection.isDeadlock(newBoxPosition)) {
			deadlockType = 3;
			return true;
		}

		/**
		 * Detection of "bipartite matching" deadlocks.
		 * “箱位”匹配之检测
		 * This deadlock detection leverages the found "frozen" box information.
		 * 此死锁检测会利用上面找到的被“冻结”箱子的信息。
		 */
		if(Settings.detectBipartiteDeadlocks == true && bipartiteDeadlockDetection.isDeadlock(SearchDirection.FORWARD)) {
			deadlockType = 4;
			return true;
		}

		long timeToStopCorralDetection = timeLimit;

		/**
		 * Detection of 'Corral' deadlocks. At least 10 ms should be left to do a detection.
		 * “围栏”型死锁的检测，留给这个检测的时间消耗，不能低于10毫秒。（暂时改为 50 毫秒）
		 */
		if (Settings.detectCorralDeadlocks && (timeToStopCorralDetection - System.currentTimeMillis() > 50)) {
			if (corralDeadlockDetection.isDeadlock(newBoxPosition, timeToStopCorralDetection)) {
				deadlockType = 5;
				return true;
			}
		}

		// No deadlock has been found.
		return false;
	}

	/**
	 * Searches for deadlock pattern on the board regarding pulls instead of pushes.<br>
	 * If a deadlock is recognized, the board is definitely unsolvable.<br>
	 * <p>
	 * Note: if no deadlock is recognized this just means that JSoko
	 * can't detect any deadlock but the board nevertheless may contain
	 * an unknown deadlock pattern.
	 *
	 * @return <code>true</code> when the current board is not solvable anymore, <code>false</code> otherwise
	 */
	public boolean isBackwardDeadlock() {
		return isBackwardDeadlock(board.boxData.getBoxPosition(0));
	}

	/**
	 * Searches for deadlock pattern on the board regarding pulls instead of pushes.<br>
	 * If a deadlock is recognized, the board is definitely unsolvable.<br>
	 * <p>
	 * Note: if no deadlock is recognized this just means that JSoko
	 * can't detect any deadlock but the board nevertheless may contain
	 * an unknown deadlock pattern.
	 *
	 * @param newBoxPosition  new position of the pushed box
	 * @return <code>true</code> when the current board is not solvable anymore, <code>false</code> otherwise
	 */
	public boolean isBackwardDeadlock(int newBoxPosition) {

		// Simple deadlock squares are valid for pulls and for pushes.
		if (board.isSimpleDeadlockSquare(newBoxPosition)) {
			return true;
		}

		if(bipartiteDeadlockDetection.isDeadlock(SearchDirection.BACKWARD)) {
			return true;
		}

		return false;
	}
}