/**
 *  JSoko - A Java implementation of the game of Sokoban
 *  Copyright (c) 2017 by Matthias Meger, Germany
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

import java.util.Arrays;

import my.boxman.jsoko.board.Board;
import my.boxman.jsoko.pushesLowerBoundCalculation.LowerBoundCalculation;
import my.boxman.jsoko.resourceHandling.IntStack;
import my.boxman.jsoko.resourceHandling.Settings;

/**
 * Class for calculating minimum number of pushes needed to push every boxes to a goal.
 * 用于计算将每个箱子推向目标所需的最小推动数。
 * <p>
 * A Sokoban level can only be solved when every box can be pushed to a goal 
 * with the constraint that one goal can only be occupied by one box. <br>
 * This class provides methods for calculating box<->goal matchings that 
 * assigns every box to a specific goal so that the total number of pushes 
 * needed to push every box to its goal is minimal.
 * Sokoban 关卡只有在每个箱子都可以被推到一个目标时才能被解决，而约束条件是一个目标只能被一个箱子占据。
 * 这个类提供了用于计算箱子与目标匹配的方法，该方法将每个箱子分配给特定的目标，以便将每个箱子推到其目标所需的推动总数最小。
 * <p>
 * The matchings are calculated using the "Auction algorithm" using epsilon scaling.
 * 按最小“拍卖算法”进行匹配？
 */
public class BipartiteMatchings {

	/** Indicator value for a box that can't reach a specific goal. */ 
	private final int NO_BENEFIT = Integer.MIN_VALUE;

	/** Constant used when NONE must be represented as value. */
	private final int NONE = -1;

	/** Constant representing "infinity". */
	private final long INFINITY  = Long.MAX_VALUE/4;

	/** The board this class operates on. */
	private final Board board;

	/** Contains the information which box is assigned to which goal. */
	private final int[] matchedBoxForGoal;

	/** These arrays hold the prices and the benefits for the auction algorithm. */
	private final long[] prices;
	private int[][] benefits;

	/** LiFo queue for storing the numbers of the boxes that have to be matched with a goal. */
	private final IntStack boxesToBeMatched;

	/** 
	 * To ensure the EPSILON value doesn't interfere with the benefits of the boxes
	 * during the auctions the benefits have to be scaled with this number ("number of boxes + 1").
	 */
	private final int BENEFITS_SCALING_FACTOR;

	/** Note: the actual benefits are negated because we like to find the minimum, however this value is the highest absolute value for a benefit. */
	private int highestAbsoluteBenefit = Integer.MIN_VALUE;

	/** If a "value" (benefit - price) is lower than this value no perfect matching can be found anymore. */
	private long minimumNoDeadlockValue;


	/**
	 * Instantiates a new object for calculating the pushes lower bound for the
	 * board position represented by the passed board.
	 * <p>Note: the number of boxes and goals on the board must be identical.
	 *
	 * @param board  the board the pushes lower bound is to be calculated for
	 */
	public BipartiteMatchings(Board board) {

		this.board		  = board;

		prices			  = new long[board.boxCount];
		benefits		  = new int[board.boxCount][board.goalsCount];
		matchedBoxForGoal = new int[board.goalsCount];
		boxesToBeMatched  = new IntStack(board.boxCount);

		BENEFITS_SCALING_FACTOR = board.boxCount + 1;
	}

	/**
	 * Returns whether the current board contains a deadlock, because not all boxes can reach an own goal.<br>
	 * This method must only be called after the "freeze" status of every box on the board has been updated
	 * since this status is used in this method!<br>
	 * If a deadlock is detected than the level is definitely not solvable anymore. 
	 * However, not all deadlocks can be detected. Hence, a return value of <code>false</code> doesn't
	 * neccessarly mean there is no deadlock.
	 * 返回当前地图是否包含死锁，因为并非所有的箱子都能达到自己的目标。
	 * 此方法必须只在更新了地图上每个箱子的“冻结”状态之后调用，因为在此方法中使用了该状态！
	 * 如果检测到死锁，则关卡肯定不再可解。
	 * 然而，并非所有死锁都可以被检测到。因此，false 的返回值并不意味着没有死锁。
	 *
	 * Example:<pre>
	 * #######
	 * # $.$ #
	 * #@ .  #
	 * #######</pre>
	 *
	 * The two boxes can both only reach the same goal. Hence, the situation is a deadlock, 
	 * because not every box can reach an own goal.
	 * 这两个盒子都只能达到相同的目标。因此，情况是一个僵局，因为不是每一个盒子都能达到自己的目标。
	 *
	 * @param searchDirection	direction of the search:  forwards (-> pushes) or backwards (->pulls)
	 * @return <code>true</code> if the board is a deadlock, or <code>false</code> if no deadlock has been found
	 */
	public boolean isDeadlock(Settings.SearchDirection searchDirection) {
		return isDeadlock(searchDirection, null);
	}

	/**
	 * Returns whether the current board contains a deadlock, because not all boxes can reach an own goal.<br>
	 * This method must only be called after the "freeze" status of every box on the board has been updated
	 * since this status is used in this method!<br>
	 * If a deadlock is detected than the level is definitely not solvable anymore. 
	 * However, not all deadlocks can be detected. Hence, a return value of <code>false</code> doesn't
	 * neccessarly mean there is no deadlock.
	 *
	 * Example:<pre>
	 * #######
	 * # $.$ #
	 * #@ .  #
	 * #######</pre>
	 *
	 * The two boxes can both only reach the same goal. Hence, the situation is a deadlock, 
	 * because not every box can reach an own goal.
	 *
	 * @param searchDirection	direction of the search (forwards or backwards)
	 * @param isGoalExcluded boolean array indicating which goals are to be excluded by the bipartite matching. {@code true} means "exclude goal"
	 * @return <code>true</code> if the board is a deadlock, or <code>false</code> if no deadlock has been found
	 */
	public boolean isDeadlock(Settings.SearchDirection searchDirection, boolean[] isGoalExcluded) {

		// The box distances to the goals must be updated because there may be new frozen boxes on the board.
		board.distances.updateBoxDistances(searchDirection, true);

		calculateBenefitsForDeadlockDetection(searchDirection, isGoalExcluded);

		return !searchPerfectBipartiteMatchingWithHighestBenefit();
	}

	/**
	 * Computes the minimum number of pushes needed to push every box to a goal on the current board.
	 * The calculated number of pushes is never higher than the real minimum number of needed pushes, 
	 * but usually lower.<br>
	 * This method must only be called after the "freeze" status of every box on the board has been 
	 * updated since this status is used in this method.
	 * <p>
	 * Note: the method temporarily changes the board while trying to find a deadlock. 
	 * However, after the method is finished the board is set back to the original board.
	 *
	 * @param searchDirection  direction of the search (forwards or backwards)
	 * @return pushes lower bound of the current board or {@code LowerBoundCalculation.DEADLOCK}
	 */
	public int calculatePushesLowerBound(Settings.SearchDirection searchDirection){
		
		// The box distances to the goals must be updated because there may be new frozen boxes on the board.
		board.distances.updateBoxDistances(searchDirection, true);
		
		boolean isPerfectMatchingPossible = calculateBenefits(searchDirection, false);
		if(!isPerfectMatchingPossible) {
			return LowerBoundCalculation.DEADLOCK;
		}	
		
		boolean matchingFound = searchPerfectBipartiteMatchingWithHighestBenefit();
		if(!matchingFound) {
			return LowerBoundCalculation.DEADLOCK;
		}
			
		// Calculate the sum of all distances of the boxes to their assigned goals.
		// See method "calculateBenefits" for details about how to extract the distances.
		int lowerBound = 0;
		for(int goalNo = 0; goalNo < board.boxCount; goalNo++) {
			int boxNo = matchedBoxForGoal[goalNo];
			lowerBound -= benefits[boxNo][goalNo] / BENEFITS_SCALING_FACTOR;
		}
				
		return lowerBound;
	}
	
	/**
	 * Calculates the benefits of every box for every goal for the auction algorithm. <br>
	 * This also checks whether any goal isn't reachable anymore and returns a deadlock status accordingly.
	 *
	 * @param searchDirection  direction of the search (forwards or backwards)
	 * @param isDeadlockDetection  flag indicating whether the benefits are only used for detecting a deadlock
	 * @return {@code true} perfect matching might be possible, {@code false} no perfect matching possible => deadlock
	 */
	private boolean calculateBenefits(Settings.SearchDirection searchDirection, boolean isDeadlockDetection) {

		int highestBoxDistance = 0;

		// The auction algorithm takes the distances of a box to the goals as "benefits". 
		// Since the auction algorithm is maximizing the total benefits value we have to 
		// set a higher benefit the lower the distance is.
		// To avoid circular matchings the algorithm must add an EPSILON value to the prices of the goals.
		// These additions mustn't interfere with the normal benefit values. Since epsilon
		// can only be added boxCount times we multiply every benefit with "number of boxes + 1".
		for(int goalNo = 0; goalNo < board.goalsCount; goalNo++) {

			boolean isGoalReachable = false;
			for(int boxNo = 0; boxNo < board.boxCount; boxNo++) {			
				int boxDistance = getDistance(boxNo, goalNo, searchDirection);
				if(boxDistance == Board.UNREACHABLE) {
					benefits[boxNo][goalNo] = NO_BENEFIT;
				} else {
					if(isDeadlockDetection) {			// for a deadlock detection it's only
						benefits[boxNo][goalNo] = 0;	// relevant if the goal is reachable or not
					} else {							    
						benefits[boxNo][goalNo] = - boxDistance * BENEFITS_SCALING_FACTOR;
						if(boxDistance > highestBoxDistance) {
							highestBoxDistance = boxDistance;
						}
					}
					isGoalReachable = true;
				}
			}
			if(!isGoalReachable) {
				return false;
			}
		}

		highestAbsoluteBenefit = highestBoxDistance * BENEFITS_SCALING_FACTOR; // highest absolute benefit	

		return true;
	}

	/**
	 * Returns the distance of the box to the goal regarding the specified search direction.
	 * 
	 * @param boxNo  the boxNo
	 * @param goalNo the goalNo
	 * @param searchDirection	direction of the search:  forwards (-> pushes) or backwards (->pulls)
	 * @return the distance from the box to the goal
	 */
	private int getDistance(int boxNo, int goalNo, Settings.SearchDirection searchDirection) {
	
		// Note: to use epsilon scaling the number of boxes and goals must be equal. 
		// Hence, for inactive boxes a dummy distance of 0 is returned instead of ignoring them.
		// This means, that inactive boxes can reach every goal. For more info see: 
		// "Auction Algorithms for Network Flow Problems: A Tutorial Introduction" by Dimitri P. Bertsekas, page 36.
		return !isBoxOnBoard(boxNo) ?  0 :	
				 searchDirection == Settings.SearchDirection.FORWARD ?
					board.distances.getBoxDistanceForwardsNo(boxNo, goalNo) :
					board.distances.getBoxDistanceBackwardsNo(boxNo, goalNo);		
	}
	
	/**
	 * Calculates the benefits of every box for every goal for the auction algorithm. <br>
	 * For the deadlock detection the real distances are irrelevant. 
	 * Hence, the benefit is either 0 or NOT_REACHABLE depending on whether a box can reach 
	 * a specific goal or not.
	 * These benefits are used in the auction algorithm to pair every box with a goal.
	 *
	 * @param searchDirection  direction of the search (forwards or backwards)
	 * @param isGoalExcluded   boolean array indicating which goals are to be excluded by the bipartite matching. {@code true} means "exclude goal"
	 * @return {@code true} perfect matching might be possible, {@code false} no perfect matching possible => deadlock
	 */
	private boolean calculateBenefitsForDeadlockDetection(Settings.SearchDirection searchDirection, boolean[] isGoalExcluded) {
	
		if(!calculateBenefits(searchDirection, true)) {
			return false;
		}
	
		// If a goal has to be ignored treat it as unreachable.
		// This array is "null" in most cases, hence excluded goals are handled here instead of 
		// being taken care of in method "calculateBenefits".
		// Note: this means that some of the boxes need to be inactive in order to still allow a perfect matching.
		if (isGoalExcluded != null) {	
			for(int goalNo = 0; goalNo < isGoalExcluded.length; goalNo++) {
				if(isGoalExcluded[goalNo]) {
					for(int boxNo = 0; boxNo < board.boxCount; boxNo++) {
						if(isBoxOnBoard(boxNo)) {					// inactive boxes can reach every goal
							benefits[boxNo][goalNo] = NO_BENEFIT;
						}
					}
				}
			}
		}
		
		return true;
	}

	private boolean isBoxOnBoard(int boxNo) {
		return board.boxData.isBoxActive(boxNo) && board.boxData.getBoxPosition(boxNo) != 0;
	}
	
	/**
	 * An auction algorithm is performed, where the boxes "buy" the goals. Each box tries to buy the goal,
	 * which is most favorable for it (-> highest benefit).<br>
	 * In the end we get a perfect bipartite matching where the total sum of benefits is maximized.<br>
	 * If no perfect matching can be found the board is a deadlock and can't be solved anymore.<br>
	 * The found matchings are stored in {@link #matchedBoxForGoal}.
	 *
	 * @return {@code true} when a perfect bipartite matching has been found, <code>false</code> otherwise
	 */
	private boolean searchPerfectBipartiteMatchingWithHighestBenefit() {

		int EPSILON_SCALING_DIVISOR = 4;   				 		//  4 is a "magic number" found by extensive testing different values
		int epsilon = Math.max(highestAbsoluteBenefit / 20, 1);	// 20 is a "magic number" found by extensive testing different values

		// If a value is lower than this value no perfect matching can be found anymore.
		// (see: "Auction Algorithms for Network Flow Problems: A Tutorial Introduction" by Dimitri P. Bertsekas, page 12)
		// Initial prices must be 0 for this formula to work.
		minimumNoDeadlockValue = - (2 * board.boxCount - 1) * highestAbsoluteBenefit - ( board.boxCount - 1) * epsilon;

		Arrays.fill(prices, 0);
		Arrays.fill(matchedBoxForGoal, NONE);

		boxesToBeMatched.clear();
		for (int boxNo = 0; boxNo < board.boxCount; boxNo++) {
			boxesToBeMatched.add(boxNo);
		}

		while(searchPerfectMatching(epsilon)) {							// Search perfect matching using the current epsilon value

			if(epsilon == 1) {											// A perfect matching found with epsilon value = 1 
				return true; 											// means minimum matching found!
			}

			epsilon = Math.max( epsilon / EPSILON_SCALING_DIVISOR,  1); // last run must start with epsilon = 1

			// Add the boxes for the next round. 
			for (int goalNo = 0; goalNo < board.goalsCount; goalNo++) {	
				if(prices[goalNo] < INFINITY-highestAbsoluteBenefit) {	// Goals that have a price >= INFINITY can't be bought
					boxesToBeMatched.add(matchedBoxForGoal[goalNo]);	// by any other box than the currently assigned one
					matchedBoxForGoal[goalNo] = NONE;					// => these boxes needn't to be matched again.
				}													
			}

			// In the next cycle the prices aren't 0 anymore. Hence, the calculated value for
			// minimumNoDeadlockValue isn't valid anymore.
			// However, a deadlock would already have been detected in the first run.
			minimumNoDeadlockValue = -INFINITY; 
		}

		return false;	// no perfect matching found => deadlock
	}

	/**
	 * An auction is performed where the boxes are assigned to goals so that the sum of all benefits
	 * is maximal(with respect to the passed epsilon value).
	 * Each cycle uses the prices from the previous cycle.
	 * Epsilon must be reduced for every new cycle until the last cycle is performed with epsilon = 1.
	 * 
	 * @param epsilon value to be used during this auction
	 * @return {@code true} if a perfect bipartite matching has been found, {@code false} otherwise
	 */
	private boolean searchPerfectMatching(int epsilon) {

		while(!boxesToBeMatched.isEmpty()) {

			int boxNo = boxesToBeMatched.remove();

			int bestGoalNo          = NONE;
			long highestValue       = -2*INFINITY;	
			long secondHighestValue = -INFINITY; 	

			// Search the goal with the highest "value".
			for(int goalNo = 0; goalNo < board.goalsCount; goalNo++) {
				if(benefits[boxNo][goalNo] != NO_BENEFIT) {
					long value = benefits[boxNo][goalNo] - prices[goalNo];
					if(value > secondHighestValue) {
						if(value > highestValue) {
							secondHighestValue = highestValue;
							highestValue       = value;
							bestGoalNo         = goalNo;
						} else {
							secondHighestValue = value;
						}
					}
				}
			}

			if(highestValue < minimumNoDeadlockValue) {
				return false;  // box can't be assigned to any goal => deadlock
			}

			int currentlyMatchedBox = matchedBoxForGoal[bestGoalNo];
			if(currentlyMatchedBox != NONE) {						
				boxesToBeMatched.add(currentlyMatchedBox);
			}

			matchedBoxForGoal[bestGoalNo] = boxNo;

			// Another box has to pay at least "the difference between the highest and the second highest value + epsilon"
			// for buying the goal from the current box. If there is only one reachable goal for the box the price is set 
			// to a value higher than INFINITY (this is the reason why the initial value for highestValue is -2*INFINITY (instead of -INFINITY).
			prices[bestGoalNo] += highestValue - secondHighestValue + epsilon;
		}

		return true;
	}
}