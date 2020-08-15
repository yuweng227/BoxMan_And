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
import my.boxman.jsoko.board.Directions;

/**
 * This class checks whether a box is frozen.  A box is "frozen", when it
 * can never again been moved (pushed).
 * <p>
 * If a box is <em>frozen</em> and is not located on a goal,
 * the complete board is a deadlock.  Examples for <em>frozen</em> boxes:<pre>
 *
 * 1) **
 *    *$
 *
 * 2)   #
 *     $$
 *    #$
 *
 * 3)  #
 *    $$
 *    #
 *
 * ...</pre>
 *
 * To determine whether a box is frozen, we try to move (push) it.  Can it be moved into
 * any direction, it is not frozen.
 * We have to take into account, that an adjacent box, which seems to block our box,
 * may be movable... except it is frozen itself.
 * Therefore this calculation is recursive!
 * <p>
 * NB: This calculation is not thread safe!
 */
public final class FreezeDeadlockDetection {

	// Constants for the axes
	// FFS/hm: axis logic by dirs (1 dir represents the axis)
	private static final byte HORIZONTAL = 2;
	private static final byte VERTICAL   = 0;

	/** For easy access we have a direct reference to the board object */
	private Board board;

	/** This variable is set by "checkBoxPushability", when a box is blocked
	 *  on a non-goal square along some axis.
	 */
	private boolean isABoxFrozenOnANotGoalSquare;

	// Falls eine Freeze-Situation auftritt, in der alle geblockten Kisten auf Zielfeldern stehen,
	// so werden hier die Positionen und die Anzahl dieser Kisten gespeichert.
	// Diese Kisten können im weiteren Verlauf wie Mauern behandelt werden!
	private int   frozenBoxesOnAGoalCount = 0;
	private int[] frozenBoxesOnAGoalPositions;

	// Timestamp and square array indicating whether a square has already been investigated.
	// 时间戳和格子数组指示器，无论格子是否已检查过。
	private int   timestamp = 2;		// 必须是偶数，且不能为 0
	private int[] freezeValues;

	public void initValue(int pos) {
		freezeValues[pos] = 0;
	}

	/**
	 * Creates an object for detecting freeze deadlocks.
	 *
	 * @param board  the board of the current level
	 */
	public FreezeDeadlockDetection(Board board) {

		this.board = board;

		// For every square the result (frozen or not frozen) is saved in this array, every
		// time the freeze status is investigated. This avoids too many calculations in levels
		// including a lot of frozen neighbor boxes.
		freezeValues = new int[board.size];
	}

	/**
	 * 返回当前位置是否为“冻结”死锁。
	 *
	 * 这个方法检查一个箱子是否被其它箱子或墙阻拦而不能再动了。
	 * 若是，则再看其是否在目标点位上，在，则标记其到“冻结”列表，不在，则为“死锁”。
	 *
	 * @param boxPosition  the position of the just pushed box
	 * @param setFrozenBoxes  flag indicating whether frozen boxes are to be marked as frozen
	 * @return <code>true</code> the board position is a freeze deadlock, and
	 * 		<code>false</code> the board position is not a freeze deadlock
	 */
	public boolean isDeadlock(int boxPosition, boolean setFrozenBoxes) {

		// Wird gesetzt, falls es ein Deadlock ist
		boolean isDeadlock = false;

		// This variable is set by "checkBoxPushability", when a box is blocked
		// on a non-goal square along some axis.
		isABoxFrozenOnANotGoalSquare = false;

		// Hier werden die Positionen aller auf Zielfeldern geblockter Kisten
		// die "check_kiste_verschiebbar" entdeckt abgelegt.
		frozenBoxesOnAGoalPositions = new int[board.boxCount];
		frozenBoxesOnAGoalCount = 0;

		// Increase the timestamp.
		// All squares with a value "timestamp" have been proven not to be frozen.
		// All squares with a value "timestamp+1" have been proven to be frozen.
		// Hence, the timestamp has to be increased by 2 for every run.
		timestamp += 2;

		// Check the horizontal axis.
		// 1. Ist die Kiste horizontal blockiert ? -> Nein, kein Deadlock
		// 2. Sind Kisten oder von ihnen abhängige Kisten auf einem
		// Nichtzielfeld blockiert ?
		if (checkBoxPushability(boxPosition, HORIZONTAL) == true) {
			return false;
		}

		isDeadlock = isABoxFrozenOnANotGoalSquare;

		// Diese Variable wird von "checkBoxPushability" gesetzt,
		// falls eine Kiste auf einem nicht Zielfeld ist.
		isABoxFrozenOnANotGoalSquare = false;

		// Check vertical axis.
		if (checkBoxPushability(boxPosition, VERTICAL) == true) {
			return false;
		}

		// The box isn't movable along any axis.
		// Falls in einem der beiden Fälle eine Kiste auf einem NichtZielfeld stand,
		// so handelt es sich um ein Deadlock!
		isDeadlock |= isABoxFrozenOnANotGoalSquare;

		// Falls eine Freezesituation ohne Deadlock zu sein aufgetreten ist, werden die Blocker
		// markiert. Blockierte Kisten auf NichtZielfeldern erzeugen automatisch eine Deadlock-
		// situation, so dass ein markieren derselbigen als Blocker unnötig ist.
		// -> Alle auf Zielfeldern geblockten Kisten bekommen den Status "Blocker"
		if (setFrozenBoxes == true && isDeadlock == false) {
			for (int boxNo = 0; boxNo < frozenBoxesOnAGoalCount; boxNo++) {
				board.boxData.setBoxFrozen(board.getBoxNo(frozenBoxesOnAGoalPositions[boxNo]));
			}
		}

		// If there hasn't been a deadlock detected additionally check for
		// a "box blocked on closed diagonal deadlock".

		return isDeadlock;
	}

	/**
	 * 返回当前位置是否为“冻结”。
	 *
	 * 这个方法仅检查一个箱子是否被其它箱子或墙阻拦而不能再动了。
	 */
	public boolean isFrozenBoxes(int boxPosition) {

		// Hier werden die Positionen aller auf Zielfeldern geblockter Kisten
		// die "check_kiste_verschiebbar" entdeckt abgelegt.
		frozenBoxesOnAGoalPositions = new int[board.boxCount];
		frozenBoxesOnAGoalCount = 0;

		// Increase the timestamp.
		// All squares with a value "timestamp" have been proven not to be frozen.
		// All squares with a value "timestamp+1" have been proven to be frozen.
		// Hence, the timestamp has to be increased by 2 for every run.
		timestamp += 2;

		// Check the horizontal axis.
		// 1. Ist die Kiste horizontal blockiert ? -> Nein, kein Deadlock
		// 2. Sind Kisten oder von ihnen abhängige Kisten auf einem
		// Nichtzielfeld blockiert ?
		if (checkBoxPushability(boxPosition, HORIZONTAL) == true) {
			return false;
		}

		// Diese Variable wird von "checkBoxPushability" gesetzt,
		// falls eine Kiste auf einem nicht Zielfeld ist.
		isABoxFrozenOnANotGoalSquare = false;

		// Check vertical axis.
		if (checkBoxPushability(boxPosition, VERTICAL) == true) {
			return false;
		}

		return true;
	}

	/**
	 * Checks whether the box is movable along the specified axis.
	 * We also take into account that adjacent boxes also may be movable.
	 * <p>
	 * FFS: we should not modify the main board here! the GUI uses it!
	 *
	 * @param boxPosition  	position of the box to be investigated
	 * @param axisdir      axis of movement to be investigated (a direction)
	 * @return whether the box is pushable (not detected to be frozen)
	 */
	private final boolean checkBoxPushability(final int boxPosition, int axisdir) {

		// Hierdrin wird der endgültige Wert ermittelt -> verschiebbar oder nicht
		boolean isBoxPushable = true;

		// change axis of direction
		axisdir = Directions.getOrthogonalDirection(axisdir);

		// If this square has already been investigated along this axis immediately return the result.
		// 如果这个格子已经沿这个轴被调查，立即返回结果。
		if (freezeValues[boxPosition] >= timestamp && axisdir == 0) {
			return freezeValues[boxPosition] > timestamp;
		}

		// Die Positionen der beiden Nachbarfelder auf dieser Achse errechnen
		int boxNeighbor1 = board.getPosition(boxPosition, axisdir);
		int boxNeighbor2 = board.getPositionAtOppositeDirection(boxPosition, axisdir);

		// If there is a wall at either side, the box cannot be pushed
		if (board.isWall(boxNeighbor1) || board.isWall(boxNeighbor2)) {
			isBoxPushable = false;
		}
		// If there is a deadlock square on both sides, the box cannot be pushed
		else if(board.isSimpleDeadlockSquare(boxNeighbor1) && board.isSimpleDeadlockSquare(boxNeighbor2)){
			isBoxPushable = false;
		}

		// Prüfen, ob die Kiste vielleicht durch andere Kisten blockiert ist bzw. diese Kiste eine
		// andere Kiste blockiert. Falls die aktuelle Kiste aber schon als nicht verschiebbar gilt
		// und auf keinem Zielfeld steht ist es auf jeden Fall eine Deadlocksituation und diese
		// Prüfung muss nicht mehr gemacht werden.
		if (isBoxPushable == true || board.isGoal(boxPosition)) {

			// An die aktuelle Position eine MAUER setzen (und die Kiste vom Feld nehmen
			// da sonst sowohl eine Mauer als auch eine Kiste auf dem Feld stehen würden!)
			// Dies ist nötig, da es in dem "if" zu einem rekursiven Aufruf kommen kann.
			// In diesem Fall würde z.B. geprüft: Kann die aktuelle Kiste verschoben werden,
			// indem vorher die Kiste oberhalb verschoben wird. Für die Kiste oberhalb würde geprüft,
			// ob sie verschoben werden kann, indem vorher die Kiste unterhalb verschoben würde ......
			// FFS: modifies main board!
			board.setWall(boxPosition);
			board.removeBox(boxPosition);

			// Falls sich auf einer der beiden Seiten eine Kiste befindet und diese verschiebbar ist,
			// so könnte theoretisch auch die aktuelle Kiste verschoben werden -> prüfen
			// Falls oben aber festgestellt wurde, dass eine Mauer im Weg ist, ist sie definitiv
			// unverschiebbar! => "&="
			if (board.isBox(boxNeighbor1)) {
				isBoxPushable &= checkBoxPushability(boxNeighbor1, axisdir);
			}

			// Falls die Prüfung auf der einen Seite der aktuellen Kiste keine auf einem Nichtzielfeld
			// blockierte Kiste gefunden hat, so könnte aber auf der anderen Seite eine solche Kiste sein.
			// Deshalb muss diese andere Seite auch überprüft werden (unabhängig davon, dass die mögliche
			// Kiste auf der anderen Seite vielleicht bereits blockiert ist!!!)
			if (isABoxFrozenOnANotGoalSquare == false && board.isBox(boxNeighbor2)) {
				// Kiste ist nur verschiebbar, wenn sie auf keiner Seite blockiert ist (=beide Seiten verschiebbar)
				isBoxPushable &= checkBoxPushability(boxNeighbor2, axisdir);
			}

			// Künstlich gesetzte Mauer wieder entfernen und Kiste wieder setzen
			board.removeWall(boxPosition);
			board.setBox(boxPosition);
		}

		// Falls die Kiste bislang als noch verschiebbar gilt, muss geprüft werden,
		// ob sie tatsächlich auf ein Nachbarfeld geschoben werden kann.
		// Dazu müssen beide Seiten frei sein. Der Fall, dass auf einer oder beiden Seiten
		// eine Kiste ist oder auf einer / beiden Seiten eine Mauer ist wurde bereits geprüft!
		// These deadlocks would also be detected by the corral detection! But: When they are detected
		// in this method frozen boxes on goals can be marked as frozen and with this information
		// the lower bound calculation can treat them as walls, which may result in a higher lower bound!
		if (isBoxPushable == true
				&& board.isAccessible(boxNeighbor1)
				&& board.isAccessible(boxNeighbor2)) {
			// Ok, beide Seiten sind frei. Nun muss geprüft werden, auf welche Seite der Kiste der Spieler
			// gelangen kann. Bis zu diesem Zeitpunkt wurden alle blockierten Kisten durch eine Mauer ersetzt!
			// Nun wird auf das Feld der zu untersuchenden Kiste auch eine Mauer gesetzt und dann geprüft,
			// auf welche Seite der Spieler gelangen kann. Dies wird mit "erreichbarFelderSpielerNurMauer" geprüft!
			// FFS: modifies main board!
			board.setWall(boxPosition);
			board.playersReachableSquaresOnlyWalls.update();
			board.removeWall(boxPosition);

			// Da sich die erreichbaren Felder des Spielers während der Deadlockprüfungen ändern,
			// muss gespeichert werden, ob der Spieler die Kiste auch von der zweiten Richtung
			// aus erreichen kann.
			boolean isOtherDirectionAccessible = board.playersReachableSquaresOnlyWalls.isSquareReachable(boxNeighbor1);

			// Prüfen, ob ein Verschieben in die eine Richtung ein "n Kisten für n-y Zielfelder Deadlock"
			// verursacht. Ist dies der Fall, so ist die Kiste für diese Richtung geblockt und es muss auch
			// geprüft werden, ob sie für die andere Richtung geblockt ist. Ist die Kiste in beiden Richtungen
			// geblockt, so ist sie nicht verschiebbar.
			// (Andere Deadlockprüfungen machen keinen Sinn, da andere Kisten mit einbezogen würden,
			// diese aber vorher verschoben hätten werden können)

			// Kistennummer Nummer
			//            int kistennr = g_spielfeld.getKistenNr(KistenPosition);

			// Backup der Spielerposition.
			//            int backupPositionSpieler = g_spielfeld.g_spielerposition;

			// Für die Prüfung auf bipartite Deadlock muss der Spieler auf die alte
			// Kistenposition gesetzt werden. Da dies für beide Richtungen gemacht werden muss,
			// wird dies bereits hier getan und nach den Prüfungen für beide Richtungen
			// wieder rückgängig gemacht.
			//            g_spielfeld.g_spielerposition = KistenPosition;

			// Ausgangszustand ist unverschiebbar! Es muss also geprüft werden,
			// ob die Kiste doch verschiebbar ist!
			isBoxPushable = false;

			// 1. Richtung prüfen:
			// -------------------

			// Nur weiter, falls der Spieler auf die richtige Seite für das Schieben kommen kann und
			// es kein simple Deadlock ist
			if (       board.isSimpleDeadlockSquare(boxNeighbor1) == false
					&& board.playersReachableSquaresOnlyWalls.isSquareReachable(boxNeighbor2)) {
				isBoxPushable = true;
			}

			// 2. Richtung prüfen
			// ------------------

			// Die zweite Richtung muss nur geprüft werden, falls nicht schon vorher
			// bewiesen wurde, dass die Kiste verschiebbar ist.
			// Nur weiter, falls der Spieler der Spieler auf die richtige Seite für das Schieben
			// kommen kann und es kann simple Deadlock ist.
			if (isBoxPushable == false
					&& isOtherDirectionAccessible == true
					&& board.isSimpleDeadlockSquare(boxNeighbor2) == false) {
				isBoxPushable = true;
			}
		}

		// The main computation is done.
		// Now we consider to update several global arrays ...

		if (isBoxPushable == false) {

			// Falls eine Kiste auf einem NichtZielfeld blockiert ist, wird das Flag gesetzt
			if (board.isGoal(boxPosition) == false) {
				isABoxFrozenOnANotGoalSquare = true;
			} else {
				// Falls eine Kiste auf einem Zielfeld geblockt wird, wird ihre Position gespeichert.
				// Falls die Gesamtsituation ebenfalls geblockt ist, aber alle geblockten Kisten auf
				// Zielfeldern stehen, könnnen die geblockten Kisten wie Mauern behandelt werden.
				// Da freeze für beide Achsen aufgerufen wird, kann eine Kiste doppelt
				// vorkommen. Dies muss abgefangen werden.
				int index;
				for (index = 0; index < frozenBoxesOnAGoalCount; index++) {
					if (frozenBoxesOnAGoalPositions[index] == boxPosition) {
						break;
					}
				}

				if (index == frozenBoxesOnAGoalCount) {
					frozenBoxesOnAGoalPositions[frozenBoxesOnAGoalCount++] = boxPosition;
				}
			}
		}

		if (axisdir == 0) {
			freezeValues[boxPosition] = timestamp + (isBoxPushable ? 1 : 0);
		}

		return isBoxPushable;
	}
}