package my.boxman.jsoko.board;

import java.util.Arrays;

// 这个类用于管理地图上的所有箱子
final public class BoxData implements Cloneable {

	private Board board;              // 地图的引用指针
	private int boxCount;             // 箱子的个数
	private int[] boxPositions;       // 箱子的位置
	private boolean[] isBoxFrozen;    // 不能再推的箱子
	private boolean[] isBoxInCorral;  // 围栏上的箱子
	private boolean[] isBoxInactive;  // 未动过的箱子

	// 指向游戏中的地图，并创建箱子相关的对象
	public BoxData(Board board) {
		this.board = board;

		boxCount = board.boxCount;
		boxPositions = new int[boxCount];

		isBoxFrozen   = new boolean[boxCount];
		isBoxInCorral = new boolean[boxCount];
		isBoxInactive = new boolean[boxCount];
	}

	// 复制地图相关的箱子数据
	private BoxData(BoxData boxData) {
		boxPositions  = boxData.getBoxPositionsClone();
		isBoxInactive = boxData.isBoxInactive.clone();
		isBoxInCorral = boxData.isBoxInCorral.clone();
		isBoxFrozen   = boxData.isBoxFrozen.clone();
		board		  = boxData.board;
		boxCount	  = boxData.boxCount;
	}

	// 通过克隆的方式，创建地图中箱子数据的副本
	@Override
	final public Object clone() {
		return new BoxData(this);
	}

	// 为箱子指定位置
	final public void setBoxPosition(int boxNo, int boxPosition) {
		boxPositions[boxNo] = boxPosition;
	}

	// 通过复制的方式，为所有箱子指定位置
	final public void setBoxPositions(int[] newBoxPositions) {
		boxPositions = newBoxPositions.clone();
	}

	// 标记箱子被移动过
	final public void setBoxActive(int boxNo) {
		isBoxInactive[boxNo] = false;
	}

	// 取消箱子被移动过的标记
	final public void setBoxInactive(int boxNo) {
		isBoxInactive[boxNo] = true;

		// 未动的箱子，不能成为围栏的一部分
		isBoxInCorral[boxNo] = false;
	}

	// 检查箱子是否被移动过
	final public boolean isBoxActive(int boxNo) {
		return isBoxInactive[boxNo] == false;
	}

	// 检查箱子是否没有被移动过
	final public boolean isBoxInactive(int boxNo) {
		return isBoxInactive[boxNo];
	}

	// 标记箱子为不可再推状态
	final public void setBoxFrozen(int boxNo) {
		isBoxFrozen[boxNo] = true;
	}

	// 检查箱子是否可以再推
	final public boolean isBoxFrozen(int boxNo) {
		return isBoxFrozen[boxNo];
	}

	// 标记箱子为可以再推
	final public void setBoxUnfrozen(int boxNo) {
		isBoxFrozen[boxNo] = false;
	}

	// 标记箱子成为围栏的一部分
	final public void setBoxInCorral(int boxNo) {
		isBoxInCorral[boxNo] = true;
	}

	// 从围栏中剔除箱子
	final public void removeBoxFromCorral(int boxNo) {
		isBoxInCorral[boxNo] = false;
	}

	// 检查箱子是否为围栏的一部分
	final public boolean isBoxInCorral(int boxNo) {
		return isBoxInCorral[boxNo];
	}

	// 取得箱子号取得箱子位置
	final public int getBoxPosition(int boxNo) {
		return boxPositions[boxNo];
	}

	// 取得一个克隆的所有箱子位置的数组
	final public int[] getBoxPositionsClone() {
		return boxPositions.clone();
	}

	// 检查所有动过的箱子，是否均在目标点位上
	final public boolean isEveryBoxOnAGoal() {

		for (int boxNo = 0; boxNo < boxCount; boxNo++) {
			if (!isBoxInactive(boxNo) && !board.isBoxOnGoal(boxPositions[boxNo])) {
				return false;
			}
		}
		return true;
	}

	// 取得已在点位上的箱子的个数
	public int getBoxesOnGoalsCount() {
		int boxesOnGoalsCount = 0;
		for(int boxNo=0; boxNo<boxCount; boxNo++) {
			if(isBoxActive(boxNo) && board.isBoxOnGoal(boxPositions[boxNo])) {
				boxesOnGoalsCount++;
			}
		}
		return boxesOnGoalsCount;
	}

	// 检查所有围栏上的箱子是否均在点位上
	final public boolean isEveryCorralBoxOnAGoal() {

		for (int boxNo = 0; boxNo < boxCount; boxNo++) {
			// 忽略未动和非围栏的箱子
			if (isBoxInactive(boxNo) || isBoxInCorral(boxNo) == false) {
				continue;
			}
			if (board.isBoxOnGoal(boxPositions[boxNo]) == false) {
				return false;
			}
		}
		return true;
	}

	// 标记所有箱子可以再推
	final public void setAllBoxesNotFrozen() {
		Arrays.fill(isBoxFrozen, false);
	}
}