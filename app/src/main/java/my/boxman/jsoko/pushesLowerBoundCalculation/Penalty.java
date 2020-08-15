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
package my.boxman.jsoko.pushesLowerBoundCalculation;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;

import my.boxman.jsoko.board.Board;

/**
 * This class computes "simple" penalty situations.
 * Example:<pre>
 *   #####
 *   #   #
 * ###   #
 * #..$$ #
 * ### @ #
 *   #####
 * </pre>
 * Such situations increase the lower bound estimation.
 */
public class Penalty {

	 // Direction constants.
	private final byte UP    = 0;
	private final byte DOWN  = 1;
	private final byte LEFT  = 2;
	private final byte RIGHT = 3;

	/** Reference to the board object */
	private Board board;

    // ArrayList, die alle Penaltysituationen für das aktuelle Level aufnimmt.
    // Diese Stellungen können mit einer Stellung verglichen werden und der Lowerbound
    // entsprechend angepasst werden.
    private ArrayList<BitSet> penaltySituations;

    // Hierdrin wird gespeichert, wieviele Kistenfelder es gibt.
    // -> um Platz zu sparen, werden nur die Felder berücksichtigt, auf denen auch eine Kiste steht.
    // Es gibt also ggf. weniger Kistenfelder als Spielfeldfelder.
    private short boxSquaresCount;

	// Array, in dem die für Kisten betretbaren Felder (Keine Mauer, kein Außenfeld und kein Deadlockfeld)
	// durchnummeriert sind.
	private short[] boardSquaresToBoxSquares;

	// Array, in dem für jedes kistenbetretbare Feld gespeichert ist, welche Position im Spielfeld
	// dieses Feld entspricht.
	private short[] boxSquaresToBoardSquares;

    /**
     * Creates an object for calculating the penalty of a board position.
     *
     * @param board {@code Board} to calculate the penlaties for
     */
    public Penalty(Board board) {

		this.board = board;

    	// Identify the positions accessible for a box.
		identifyBoxRelevantPositions();

    	penaltySituations = new ArrayList<BitSet>(10);

    	identifyPotentialPenaltySquares();
    }


    /**
     * Identifies those board squares, which may cause a simple penalty situation.
	 * Example:<pre>
	 *   #####
	 *   #   #
	 * ###   #
	 * #..$$ #
	 * ### @ #
	 *   #####
	 * </pre>
	 * Here we would detect a penalty (and increase the lower bound by 2).
	 */
    final private void identifyPotentialPenaltySquares(){

        // Gibt an, ob eine Suche weitergeführt werden soll.
        boolean isSearchToBeContinued = false;

        // Nimmt eine Stellung der Kisten auf.
        BitSet boardPosition;

        // Gibt an, ob eine Stellung von mindestens einer Spielerposition aus keine Penaltystellung ist.
        boolean isBoardPositionAtLeastOneTimeNoPenalty;

        // Aktuelle Spielerposition sichern, da sie während der Penaltyfelderermittlung verändert wird.
        int playerPositionBackup = board.playerPosition;

        // Potentielle Penaltyfelder ermitteln
        for(int position = board.firstRelevantSquare; position < board.lastRelevantSquare; position++) {

            // Alle Felder, auf denen keine Kiste stehen kann, ausschließen
            if(board.isWallOrIllegalSquare(position) || board.isOuterSquareOrWall(position)) {
                continue;
            }

            /*
             *  Prüfen, ob bei einem horizontalen Block ein Penalty erforderlich wäre.
             *
             *  Ein Penalty wäre gegeben, falls auf der aktuellen Position und auf der
             *  rechten Nachbarposition eine Kiste stehen würden und eine vertikale Verschiebung beider
             *  Kisten den Lowerbound erhöhen würde.
             */
            // Freezesituationen werden nicht als Penaltysituationen gewertet
            // -> Mindestens eine der beiden Kisten muss auch verschiebbar sein.
            // Außerdem ist es keine Penaltysituation, wenn beide Felder Zielfelder sind.
            int neighborSquarePosition = position + 1;

            while(true) {

                // Es kann nur ein Penalty sein, wenn es kein Freeze ist und
                // nicht beide Kisten auf Zielfeldern stehen.
                if(!board.isWallOrIllegalSquare(neighborSquarePosition)
                   &&
                   (!board.isGoal(position) ||
                    !board.isGoal(neighborSquarePosition))
                    &&
                    (!board.isWallOrIllegalSquare(board.getPosition(neighborSquarePosition, UP)) &&
                     !board.isWall(board.getPosition(neighborSquarePosition, DOWN))
                     ||
                     !board.isWallOrIllegalSquare(board.getPosition(neighborSquarePosition, DOWN)) &&
                     !board.isWall(board.getPosition(neighborSquarePosition, UP))
                     ||
                     !board.isWallOrIllegalSquare(board.getPosition(position, UP)) &&
                     !board.isWall(board.getPosition(position, DOWN))
                     ||
                     !board.isWallOrIllegalSquare(board.getPosition(position, DOWN)) &&
                     !board.isWall(board.getPosition(position, UP)))) {


                    // Auf die Kistenfelder eine Mauer setzen, um die erreichbaren Felder des
                    // Spielers ermitteln zu können.
                    board.setWall(position);
                    board.setWall(neighborSquarePosition);

                    // Erreichbare Felder des Spielers kennzeichen
                    board.playersReachableSquaresOnlyWalls.update();

                    // Zunächst ist keine Stellung gespeichert.
                    boardPosition = null;

                    // Initialisieren
                    isBoardPositionAtLeastOneTimeNoPenalty = false;

                    do {
                         // Nun kommt die eigentliche Prüfung:
                        if(isPushWithoutLowerBoundIncreasePossible(position, UP) == false &&
                         !isPushWithoutLowerBoundIncreasePossible(neighborSquarePosition, UP)) {

                            // Die aktuelle Penaltysituation speichern, falls dies noch nicht geschehen ist.
                            if(boardPosition == null) {
                                boardPosition = new BitSet(boxSquaresToBoardSquares.length + 1);
                                boardPosition.set(boardSquaresToBoxSquares[position]);
                                boardPosition.set(boardSquaresToBoxSquares[neighborSquarePosition]);
                                penaltySituations.add(boardPosition);
                            }
                        } else {
                            // Die aktuelle Stellung ist mindestens von dieser Spielerposition aus kein Penalty.
                            isBoardPositionAtLeastOneTimeNoPenalty = true;
                        }

                        // Es wird davon ausgegangen, dass kein neuer Durchgang notwendig ist.
                        isSearchToBeContinued = false;

                        // Den Spieler auf ein Feld setzen, das er bisher nicht erreichen konnte. Falls es ein
                        // solches Feld gibt, so wird von dieser Position ebenfalls überprüft, ob die aktuelle
                        // Stellung eventuell eine Penaltystellung ist.
                        for(int playerPosition = board.firstRelevantSquare; playerPosition < board.lastRelevantSquare; playerPosition++) {
                            if(board.playersReachableSquaresOnlyWalls.isSquareReachable(playerPosition) == false &&
                               board.isOuterSquareOrWall(playerPosition) == false) {
                                board.setPlayerPosition(playerPosition);
                                board.playersReachableSquaresOnlyWalls.enlarge(playerPosition);
                                isSearchToBeContinued = true;
                                break;
                            }
                        }

                    }while(isSearchToBeContinued == true);

                    // Falls die Stellung eine Penaltystellung ist, aber mindestens von einer Spielerposition aus
                    // keine Penaltystellung ist, so ist sie spielerpositionsabhängig eine Penaltystellung.
                    // In diesem Fall wird das letzte Bit in der Stellung gesetzt, damit später immer
                    // spielerpositionsabhängig auf Penalty geprüft wird.
                    if(boardPosition != null && isBoardPositionAtLeastOneTimeNoPenalty == true) {
                        boardPosition.set(boardPosition.size()-1);
                    }

                    // Mauern wieder entfernen
                    board.removeWall(position);
                    board.removeWall(neighborSquarePosition);
                }

                // Falls das Nachbarfeld in einem Tunnel ist muss auch das rechte Nachbarfeld von diesem
                // Feld geprüft werden.
                if(board.isWall( board.getPosition(neighborSquarePosition, UP)   ) &&
                   board.isWall( board.getPosition(neighborSquarePosition, DOWN) ) &&
                   board.isGoal(neighborSquarePosition) == false &&
                   !board.isWallOrIllegalSquare(neighborSquarePosition)) {
                    neighborSquarePosition++;
                } else {
                    break;
                }
            }


            /*
             *  Prüfen, ob bei einem vertikalen Block ein Penalty erforderlich wäre.
             *
             *  Ein Penalty wäre ebenfalls gegeben, falls auf der aktuellen Position und auf der
             *  unteren Nachbarposition eine Kiste stehen würden und eine horizontale Verschiebung beider
             *  Kisten den Lowerbound erhöhen würde.
             */
            // Freezesituationen werden nicht als Penaltysituationen gewertet
            // -> Mindestens eine der beiden Kisten muss auch verschiebbar sein.
            // Außerdem ist es keine Penaltysituation, wenn beide Felder Zielfelder sind.
            neighborSquarePosition = board.getPosition(position, DOWN);

            while(true) {

                    // Es kann nur ein Penalty sein, wenn es kein Freeze ist und
                    // nicht beide Kisten auf Zielfeldern stehen.
                    if(!board.isWallOrIllegalSquare(neighborSquarePosition)
                       &&
                       (!board.isGoal(position) ||
                        !board.isGoal(neighborSquarePosition))
                        &&
                        (!board.isWallOrIllegalSquare(board.getPosition(neighborSquarePosition, RIGHT)) &&
                         !board.isWall(board.getPosition(neighborSquarePosition, LEFT))
                         ||
                        !board.isWallOrIllegalSquare(board.getPosition(neighborSquarePosition, LEFT)) &&
                         !board.isWall(board.getPosition(neighborSquarePosition, RIGHT))
                         ||
                        !board.isWallOrIllegalSquare(board.getPosition(position, RIGHT)) &&
                         !board.isWall(board.getPosition(position, LEFT))
                         ||
                        !board.isWallOrIllegalSquare(board.getPosition(position, LEFT)) &&
                         !board.isWall(board.getPosition(position, RIGHT)))) {

                        // Auf die Kistenfelder eine Mauer setzen, um die erreichbaren Felder des
                        // Spielers ermitteln zu können.
                        board.setWall(position);
                        board.setWall(neighborSquarePosition);

                        // Erreichbare Felder des Spielers kennzeichen
                        board.playersReachableSquaresOnlyWalls.update();

                        // Zunächst ist keine Stellung gespeichert.
                        boardPosition = null;

                        // Initialisieren
                        isBoardPositionAtLeastOneTimeNoPenalty = false;

                        do {
                             // Nun kommt die eigentliche Prüfung:
                            if(!isPushWithoutLowerBoundIncreasePossible(position, RIGHT) &&
                             !isPushWithoutLowerBoundIncreasePossible(neighborSquarePosition, RIGHT)) {

                                // Die aktuelle Penaltysituation speichern, falls dies noch nicht geschehen ist.
                                if(boardPosition == null) {
                                    boardPosition = new BitSet(boxSquaresToBoardSquares.length + 1);
                                    boardPosition.set(boardSquaresToBoxSquares[position]);
                                    boardPosition.set(boardSquaresToBoxSquares[neighborSquarePosition]);
                                    penaltySituations.add(boardPosition);
                                }
                            } else {
                                // Die aktuelle Stellung ist mindestens von dieser Spielerposition aus kein Penalty.
                                isBoardPositionAtLeastOneTimeNoPenalty = true;
                            }

                            // Es wird davon ausgegangen, dass kein neuer Durchgang notwendig ist.
                            isSearchToBeContinued = false;

                            // Den Spieler auf ein Feld setzen, das er bisher nicht erreichen konnte. Falls es ein
                            // solches Feld gibt, so wird von dieser Position ebenfalls überprüft, ob die aktuelle
                            // Stellung eventuell eine Penaltystellung ist.
                            for(int playerPosition = board.firstRelevantSquare; playerPosition < board.lastRelevantSquare; playerPosition++) {
                                if(board.playersReachableSquaresOnlyWalls.isSquareReachable(playerPosition) == false &&
                                   board.isOuterSquareOrWall(playerPosition) == false) {
                                    board.setPlayerPosition(playerPosition);
                                    board.playersReachableSquaresOnlyWalls.enlarge(playerPosition);
                                    isSearchToBeContinued = true;
                                    break;
                                }
                            }

                        }while(isSearchToBeContinued == true);

                        // Falls die Stellung eine Penaltystellung ist, aber mindestens von einer Spielerposition aus
                        // keine Penaltystellung ist, so ist sie spielerpositionsabhängig eine Penaltystellung.
                        // In diesem Fall wird das letzte Bit in der Stellung gesetzt, damit später immer
                        // spielerpositionsabhängig auf Penalty geprüft wird.
                        if(boardPosition != null && isBoardPositionAtLeastOneTimeNoPenalty == true) {
                            boardPosition.set(boardPosition.size()-1);
                        }

                        // Mauern wieder entfernen
                        board.removeWall(position);
                        board.removeWall(neighborSquarePosition);
                    }

                    // Falls das Nachbarfeld in einem Tunnel ist muss auch das untere Nachbarfeld von diesem
                    // Feld geprüft werden.
                    if(board.isWall( board.getPosition(neighborSquarePosition, RIGHT) ) &&
                       board.isWall( board.getPosition(neighborSquarePosition, LEFT)  ) &&
                       board.isGoal(neighborSquarePosition) == false &&
                       board.isWallOrIllegalSquare(neighborSquarePosition) == false) {
                        neighborSquarePosition = board.getPosition(neighborSquarePosition, DOWN);
                    } else {
                        break;
                    }
                }

            // Spieler wieder auf die ursprüngliche Position setzen
            board.setPlayerPosition(playerPositionBackup);

            /*
             * Hinweis:
             * Es müssen nur die Felder rechts und unterhalb geprüft werden, da die Felder links und
             * oberhalb ja bereits in einem früheren Durchlauf geprüft wurden.
             */
        }
    }


	/**
	 * This method checks, whether a box at the passed position, can be pushed along the
	 * specified axis, without increasing its distance to all goals.
	 * If that is not possible, any push of this box along that axis would increase
	 * the lower bound of the board configuration by 2.
	 * <p>
	 * While we do not have a notion of "axis", we specify it by one of the associated
	 * directions, e.g. LEFT or RIGHT for "horizontal".
	 *
	 * @param boxPosition position of the box to consider
	 * @param direction	  direction which implies the axis of the pushes to consider
	 *
	 * @return <code>true</code> if the box can be pushed along the axis without causing
	 *         an overall increase of the lower bound, and<br>
	 *         <code>false</code>, if any push of the box along the axis would increase
	 *         the overall lower bound
	 */
	final private boolean isPushWithoutLowerBoundIncreasePossible(final int boxPosition, final int direction) {

	    // Spielerposition sichern.
	    int playerPositionBackup = board.playerPosition;

	    int boxPositionInDirection 		   = board.getPosition(boxPosition, direction);
	    int boxPositionInOppositeDirection = board.getPositionAtOppositeDirection(boxPosition, direction);

	    // Prüfen, ob ein Verschieben einer Kiste auf dem aktuellen Feld auf der angegebenen Achse
        // zwingend eine Lowerbounderhöhung zur Folge hätte.
        if(board.isWall(boxPositionInDirection) ||
           board.isWall(boxPositionInOppositeDirection) ||
           (board.isSimpleDeadlockSquare(boxPositionInDirection) &&
            board.isSimpleDeadlockSquare(boxPositionInOppositeDirection))) {
			return false;
		}

        for(int goalNo = 0 ; goalNo < board.goalsCount; goalNo++) {

           // Entfernung vom aktuellen Feld zum Zielfeld ermitteln
           int distance = board.distances.getBoxDistanceForwardsPosition(boxPosition, board.getGoalPosition(goalNo));

           // Prüfen, ob bei einem Verschieben der Kiste in eine Richtung die Distanz zu einem Zielfeld geringer wird.
           board.setPlayerPosition(boxPosition);
           if(board.distances.getBoxDistanceForwardsPosition(boxPositionInDirection, board.getGoalPosition(goalNo))         < distance ||
              board.distances.getBoxDistanceForwardsPosition(boxPositionInOppositeDirection, board.getGoalPosition(goalNo)) < distance) {
               board.setPlayerPosition(playerPositionBackup);
               return true;
           }

           // Spieler wieder auf seine alte Position zurücksetzen.
           board.setPlayerPosition(playerPositionBackup);
        }

        // Falls ein Verschieben nach beiden Seiten eine Erhöhung des Lowerbounds zur Folge hätte,
        // so ist überhaupt keine Verschiebung auf der vorgegebenen Achse möglich.
        return false;
	}


	/**
	 * Penalty situations are stored in bit vectors ({@link BitSet}), where each bit
	 * corresponds to a position.  To save memory we do not use a bit for <em>each</em>
	 * square, but rather only for those squares, which may contain a box (no wall, and
	 * no deadlock).
	 * <p>
	 * In this method we compute the arrays we need for this, mapping potential box
	 * square indexes to normal board positions and vice versa.
	 */
	final private void identifyBoxRelevantPositions() {

	    // Array, das speichert, welches Kistenfeld einem bestimmten Spielfeldfeld entspricht.
	    boardSquaresToBoxSquares = new short[board.size];
	    for(int position = 0; position < board.size; position++) {
	        boardSquaresToBoxSquares[position] = -1;
	    }

	    // Array, das speichert, welches Kistenfeld welchem Spielfeldfeld entspricht.
	    short[] tempBoxSquaresToBoardSquares = new short[board.size];

	    for(int position = board.firstRelevantSquare; position < board.lastRelevantSquare; position++) {
	        if(board.isOuterSquareOrWall(position) == false && board.isWallOrIllegalSquare(position) == false) {
	            boardSquaresToBoxSquares[position] 			    = boxSquaresCount;
	            tempBoxSquaresToBoardSquares[boxSquaresCount++] = (short) position;
	        }
	    }

	    // Lokales Array in ein globales Array der optimalen Länge kopieren.
	    boxSquaresToBoardSquares = new short[boxSquaresCount];
	    for(int position = 0; position < boxSquaresCount; position++) {
	        boxSquaresToBoardSquares[position] = tempBoxSquaresToBoardSquares[position];
	    }
	}


	/**
	 * Calculates the penalty value of the current board.
	 *
	 * @return penalty value of the current board
	 */
	final public int calculatePenalty() {

	    // Penaltywert der aktuellen Stellung
	    int penalty = 0;

	    // BitSet der aktuellen Stellung.
	    BitSet currentSituation;

	    // BitSet der Penaltysituation.
	    BitSet penaltySituation;

	    // Position der beiden Penaltyfelder
	    int[] penaltySquaresPositions;

	    // Gibt an, ob es Blockerkisten in der aktuellen Stellung gibt.
	    boolean isAFrozenBoxInSituation = false;

	    // BitSet anlegen, das alle Kistenfelder verwalten kann.
	    currentSituation = new BitSet(boxSquaresCount);

	    // Array anlegen, in dem die beiden Penaltyfelder gespeichert werden
	    penaltySquaresPositions = new int[2];

	    // Aus Platzgründen werden die Spielfeldfelder auf die "von Kisten betretbaren Felder" gemapped.
	    for(int boxNo=0; boxNo < board.boxCount; boxNo++) {
	        int boxPosition = board.boxData.getBoxPosition(boxNo);
	        // Falls es ein unlösbares Level ist wird 0 zurückgegeben.
	        if(board.isSimpleDeadlockSquare(boxPosition)) {
				return 0;
			}
	        currentSituation.set(boardSquaresToBoxSquares[boxPosition]);
	    }

	    // Alle Penaltysituationen durchgehen und für jede Situation, die sich auf dem Spielfeld
	    // befindet den Lowerbound erhöhen.
	    nextPenaltySituation:
	    for(Iterator<BitSet> i = penaltySituations.iterator(); i.hasNext();) {
	        penaltySituation = i.next();

	        // Die erste Penaltyposition wird bei jeder neuen Situation initialisiert,
	        // um die Positionen korrekt im Array speichern zu können.
	        penaltySquaresPositions[0] = 0;

	        // Falls die Penaltysituation in der aktuellen Stellung nicht vorkommt
	        // wird sofort zurückgesprungen ("-1", da das letzte Bit angibt, ob es ein spielerpositions-
	        // abhängiges Penalty ist)
	        for(int index = 0; index < penaltySituation.size()-1; index++) {
	            if(penaltySituation.get(index) == false) {
	                continue;
	            }
	            if(currentSituation.get(index) == false) {
	                continue nextPenaltySituation;
	            }

	            // Position der beiden Penaltyfelder im Spielfeld merken
	            if(penaltySquaresPositions[0] == 0) {
	                penaltySquaresPositions[0] = boxSquaresToBoardSquares[index];
	            } else {
	                penaltySquaresPositions[1] = boxSquaresToBoardSquares[index];
	            }
	        }

	        // Prüfen, ob es Blockerkisten gibt.
	        for(int boxNo = 0 ; boxNo<board.boxCount; boxNo++) {
	            if(board.boxData.isBoxFrozen(boxNo)) {
	                isAFrozenBoxInSituation = true;
	                break;
	            }
	        }

	        // Falls das Penalty von der Spielerposition abhängig ist, so muss es erneut auf
	        // Gültigkeit überprüft werden. Das letzte Bit gibt an, ob es spielerpositionsabhängig ist.
	        // Außerdem muss die Gültigkeit überprüft werden, falls es Blockerkisten gibt.
	        // (Durch geblockte Kisten auf Zielfeldern kann der Lowerbound bereits erhöht worden sein.
		    // Die Penaltys wurden aber bereits vorberechnet als es noch keine geblockten Kisten gab)
	        if(penaltySituation.get(penaltySituation.size()-1) == true ||
	           isAFrozenBoxInSituation == true) {

	            // Prüfen, ob es ein horizontales Penalty ist, welches aufgelöst werden kann.
	            if(penaltySquaresPositions[1] - penaltySquaresPositions[0] < board.width) {
	                if(isPushWithoutLowerBoundIncreasePossible(penaltySquaresPositions[0], UP) == true ||
		               isPushWithoutLowerBoundIncreasePossible(penaltySquaresPositions[1], UP) == true) {
						continue nextPenaltySituation;
					}
		        } else {
			        // Prüfen, ob das vertikale Penalty aufgelöst werden kann.
	                if(isPushWithoutLowerBoundIncreasePossible(penaltySquaresPositions[0], RIGHT) == true ||
		               isPushWithoutLowerBoundIncreasePossible(penaltySquaresPositions[1], RIGHT) == true) {
		               	continue nextPenaltySituation;
	                }
			    }
			}

	        // Das Penalty ist in der aktuellen Stellung vorhanden und erhöht damit den Lowerbound.
	        penalty += 2;

	        // Alle Kisten in der Penaltysituation, die verschiebbar sind können nicht mehr für weitere
	        // Penaltysituationen benutzt werden und werden daher logisch gelöscht.
	        for(int index = 0; index < 2; index++) {

	            // Horizontales Penalty -> prüfen, ob die Kisten vertikal verschoben werden können
	            if(penaltySquaresPositions[1] - penaltySquaresPositions[0] < board.width) {
	            	int penaltySquareAbove = board.getPosition(penaltySquaresPositions[index], UP);
	            	int penaltySquareBelow = board.getPosition(penaltySquaresPositions[index], DOWN);
	                if(board.isWall(penaltySquareAbove) ||
	                   board.isWall(penaltySquareBelow) ||
	 	               board.isSimpleDeadlockSquare(penaltySquareAbove) &&
	 	               board.isSimpleDeadlockSquare(penaltySquareBelow)) {
						continue;
					}
	            }
	            else {
	            	int penaltySquareLeft  = board.getPosition(penaltySquaresPositions[index], LEFT);
	            	int penaltySquareRight = board.getPosition(penaltySquaresPositions[index], RIGHT);
	                if(board.isWall(penaltySquareLeft) ||
	                   board.isWall(penaltySquareRight) ||
	 	               board.isSimpleDeadlockSquare(penaltySquareLeft) &&
	 	               board.isSimpleDeadlockSquare(penaltySquareRight)) {
						continue;
					}
	            }

	            // Die Kiste kann wahrscheinlich verschoben werden, so dass das Penalty durch diese
	            // Kiste aufgelöst werden kann. Diese Kiste darf nicht gleichzeitig für ein anderes
	            // Penalty benutzt werden und wird daher aus der aktuellen Stellung entfernt.
	            currentSituation.clear(boardSquaresToBoxSquares[penaltySquaresPositions[index]]);
	        }
	    }

	    return penalty;
	}


	/**
	 * Displays the penalty situations of the current level (for debugging purposes).
	 */
	public void debugShowPenaltySituations() {

	    // Nimmt die Stellung einer Penaltysituation auf.
	    BitSet penaltySituation;

	    // Alle bestehenden Markierungen löschen.
	    board.removeAllMarking();

	    // Alle Penaltysituationen durchgehen und die Felder markieren
	    for(Iterator<BitSet> i = penaltySituations.iterator(); i.hasNext();) {
	        penaltySituation = i.next();
	        for(int boxSquareNo = 0; boxSquareNo < boxSquaresCount; boxSquareNo++) {
	            if(penaltySituation.get(boxSquareNo)) {
	                board.setMarking(boxSquaresToBoardSquares[boxSquareNo]);
	            }
	        }
	    }

	}
}