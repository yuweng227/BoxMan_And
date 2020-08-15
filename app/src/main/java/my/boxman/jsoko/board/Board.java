package my.boxman.jsoko.board;

import java.util.Arrays;
import java.util.BitSet;

import my.boxman.jsoko.boardpositions.IBoardPosition;
import my.boxman.jsoko.deadlockdetection.ClosedDiagonalDeadlock;
import my.boxman.jsoko.deadlockdetection.FreezeDeadlockDetection;
import my.boxman.jsoko.resourceHandling.IntStack;
import my.boxman.jsoko.resourceHandling.LruCache;
import my.boxman.jsoko.resourceHandling.Utilities;
import my.boxman.jsoko.resourceHandling.Settings;

// 地图数据及其更新方法
public class Board implements DirectionConstants
{
	// 地图最大尺寸
	public static final int MAXIMUM_BOARDSIZE = 100;

	// 距离无限远，即：不可达。
	public static final short UNREACHABLE = Short.MAX_VALUE;

	// 此表标识所有可能成为围栏的局面
	// 其索引是该格子周边的 3 * 3 区域中标记的墙
	// 忽略中心的格子后: (9-1) bits <==> 256 允许的全部索引值
	final protected boolean[] corralForcerSituations = new boolean[] { false,
			false, false, false, false, true, false, false, false, false,
			false, false, true, true, false, false, false, true, false, false,
			false, true, false, false, true, true, false, false, true, true,
			false, false, false, true, true, true, true, true, true, true,
			false, false, false, false, true, true, false, false, true, true,
			true, true, true, true, true, true, true, true, false, false, true,
			true, false, false, false, true, true, true, true, true, true,
			true, false, false, false, false, true, true, false, false, false,
			true, false, false, false, true, false, false, false, false, false,
			false, false, false, false, false, false, true, true, true, true,
			true, true, true, false, false, false, false, true, true, false,
			false, false, true, false, false, false, true, false, false, false,
			false, false, false, false, false, false, false, false, true, true,
			true, true, true, true, true, true, true, true, true, true, true,
			true, true, false, true, false, false, false, true, false, false,
			true, true, false, false, true, true, false, false, true, true,
			true, true, true, true, true, true, true, true, true, true, true,
			true, true, true, true, true, true, true, true, true, true, true,
			true, true, false, false, true, true, false, false, false, true,
			true, true, true, true, true, true, false, false, false, false,
			true, true, false, false, false, true, false, false, false, true,
			false, false, false, false, false, false, false, false, false,
			false, false, true, true, true, true, true, true, true, false,
			false, false, false, true, true, false, false, false, true, false,
			false, false, true, false, false, false, false, false, false,
			false, false, false, false };

	// 表示关卡缺少仓管员的常量
	private final int NO_PLAYER = -1;

	// 目标点位的序号
	protected int[] goalsNumbers;

	// 全部目标点的位置
	protected int[] goalsPositions;

	//箱子的序号
	private int[] boxNumbers;

	//两个格子间，仓管员需要移动的距离
	protected short[][] playerDistances;

	private byte[] wallsArray;
	private boolean[] goalsArray;
	private boolean[] boxesArray;
	protected boolean[] simpleDeadlockSquareForwards;  //正推中的简单死锁点
	protected boolean[] simpleDeadlockSquareBackwards;  //逆推中的简单死锁点
	private boolean[] advancedSimpleDeadlockSquareForwards;  //正推中的高级简单死锁点
	private boolean[] marked;

	// 正推搜索的目标点数组，求解器中，为正推搜索的箱子所在的目标位置
	protected boolean[] goalSquareBackwardsSearch;
	protected int[] goalPositionsBackwardsSearch;

	// 仅仅含有墙壁的关卡初态中，设置为“真”的格子是仓管员的可达区域
	private boolean[] playersReachableSquaresOnlyWallsAtLevelStart;

	// 为“真”时，表示若箱子放在这里，将形成一个仓管员不可达的小封闭空间
	protected boolean[] corralForcer;

	// 辨认简单和高级死锁的对象
	protected BadSquares badSquares;

	public int width;
	public int height;

	// 方便方向计算的数组
	public int[] offset;

	// 地图的格子总数
	public int size;

	// 仓管员可达位置中的第一个
	public int firstRelevantSquare;

	// 仓管员可达位置中的最后一个
	public int lastRelevantSquare;

	// 指示仓管员位置的变量，默认值是“还没有”
	public int playerPosition = NO_PLAYER;

	// 箱子数，一些计算可能临时从地图中去掉箱子，但是，此值不变
	public int boxCount;

	// 点位数
	public int goalsCount;

	// 保存实际箱子数据的对象
	public BoxData boxData;

	// 标识仓管员可达位置的对象
	public PlayersReachableSquares playersReachableSquares;

	// 计算仓管员到达指定位置的路径
//	public PlayerPathCalculation playerPath;

	// 去掉箱子仅剩墙壁时，标识仓管员可以到达的位置的对象
	public PlayersReachableSquaresOnlyWalls playersReachableSquaresOnlyWalls;

	// 点击箱子时，标识箱子的可达位置的对象
//	public BoxReachableSquares boxReachableSquares;

	//标识箱子逆推的可达位置的对象
//	public BoxReachableSquaresBackwards boxReachableSquaresBackwards;

	// 当仅剩这个箱子时，标识其正推可达位置的对象
	private BoxReachableSquaresOnlyWalls boxReachableSquaresOnlyWalls;

	// 当仅剩这个箱子时，标识其逆推可达位置的对象
//	private BoxReachableSquaresBackwardsOnlyWalls boxReachableSquaresBackwardsOnlyWalls;

	// 计算仓管员和箱子移动距离的对象
	public Distances distances;

	public Board() {
	}

	// 快手添加：根据字符串，创建地图
	public void setBoardFromArray(char[][] arr) {

		int newBoardHeight = arr.length, newBoardWidth = arr[0].length;

		// 根据加载的地图尺寸，创建所有相关对象（关于路径、死锁等相关计算用的所有数组）
		newBoard(newBoardWidth, newBoardHeight);

		// 解析地图相关的所有数据（箱子、墙壁、目标点、人的位置等数组）
		char squareCharacter;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {

				squareCharacter = arr[y][x];

				switch (squareCharacter) {
				case ' ':
				case '-':  // 增强对其它“地板”字符的支持
					break;

				case '.':
					goalsCount++;
					setGoal(x, y);
					break;

				case '$':
					boxCount++;
					setBox(x, y);
					break;

				case '*':
					goalsCount++;
					boxCount++;
					setBoxOnGoal(x, y);
					break;

				case '@':
					setPlayerPosition(x, y);
					break;

				case '+':
					goalsCount++;
					setGoal(x, y);
					setPlayerPosition(x, y);
					break;

				default:
					setWall(x, y);
				}
			}
		}
	}

	// 根据字符串，创建地图
	public void setBoardFromString(String boardAsString) throws Exception {

		int newBoardWidth  = 0, newBoardHeight;

		// 尺寸的确定
		String[] boardRows = boardAsString.split("\n|\r|\r\n|\n\r|\\|");  // 允许更多的字符分行
		for (String row : boardRows) {
			if (newBoardWidth < row.length()) {
				newBoardWidth = row.length();		// 以最宽的行作为地图的宽
			}
		}
		newBoardHeight = boardRows.length;

		// 根据加载的地图尺寸，创建所有相关对象（关于路径、死锁等相关计算用的所有数组）
		newBoard(newBoardWidth, newBoardHeight);

		// 解析地图相关的所有数据（箱子、墙壁、目标点、人的位置等数组）
		char squareCharacter;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {

				String row = boardRows[y];
				squareCharacter = (x < row.length()) ? row.charAt(x) : ' ';

				switch (squareCharacter) {
				case ' ':
				case '-':  // 增强对其它“地板”字符的支持
					break;

				case '.':
					goalsCount++;
					setGoal(x, y);
					break;

				case '$':
					boxCount++;
					setBox(x, y);
					break;

				case '*':
					goalsCount++;
					boxCount++;
					setBoxOnGoal(x, y);
					break;

				case '@':
					setPlayerPosition(x, y);
					break;

				case '+':
					goalsCount++;
					setGoal(x, y);
					setPlayerPosition(x, y);
					break;

				default:
					setWall(x, y);
				}
			}
		}
		if (newBoardWidth > MAXIMUM_BOARDSIZE || newBoardHeight > MAXIMUM_BOARDSIZE) {
			throw new Exception( "关卡尺寸超限: " + MAXIMUM_BOARDSIZE + " * " + MAXIMUM_BOARDSIZE );
		}
	}

	// 当地图宽度确定下来后，填充这个数组
	private final void makeOffsets() {
		offset = new int[] { -width, width, -1, 1 };
	}

	// 根据两个格子，计算移动方向向量，若两个格子“不相关”，将返回一个越界的方向向量，即：4
	public final byte getMoveDirection(int srcPosition, int dstPosition) {
		int  stepdelta = dstPosition - srcPosition;
		byte direction = 0;
		while (offset[direction] != stepdelta) {
			++direction;
		}
		return direction;
	}

	// 功能同上，属于重构
	public final byte getMoveDirectionNumber(int srcPosition, int dstPosition) {
		int  stepdelta = dstPosition - srcPosition;
		byte direction = 0;
		while (offset[direction] != stepdelta) {
			++direction;
		}
		return direction;
	}


	// 创建指定尺寸的地图
	public void newBoard(int width, int height) {
		this.width  = width;
		this.height = height;

		size = width * height;
		wallsArray = new byte[size];
		goalsArray = new boolean[size];
		boxesArray = new boolean[size];

		simpleDeadlockSquareForwards = new boolean[size];
		simpleDeadlockSquareBackwards = new boolean[size];
		advancedSimpleDeadlockSquareForwards = new boolean[size];
		marked = new boolean[size];

		makeOffsets();

		playersReachableSquares = new PlayersReachableSquares();
//		playerPath = new PlayerPathCalculation();
		playersReachableSquaresOnlyWalls = new PlayersReachableSquaresOnlyWalls();
//		boxReachableSquares = new BoxReachableSquares();
//		boxReachableSquaresBackwards = new BoxReachableSquaresBackwards();
		boxReachableSquaresOnlyWalls = new BoxReachableSquaresOnlyWalls();
//		boxReachableSquaresBackwardsOnlyWalls = new BoxReachableSquaresBackwardsOnlyWalls();

		// 创建用于简单和高级死锁计算的对象
		badSquares = new BadSquares();

		boxCount   = 0;
		goalsCount = 0;

		playerPosition = NO_PLAYER;
	}


	// 检查地图是否有效
	public boolean isValid(StringBuilder message) {

		int currentPlayerPosition;

		message.setLength(0);

		playersReachableSquaresOnlyWallsAtLevelStart = new boolean[size];

		// 有没有仓管员
		if (playerPosition == NO_PLAYER) {
			message.append("没有仓管员！");
			return false;
		}

		IntStack squaresToBeAnalyzed = new IntStack(size);

		// 检查仓管员的达到位置，若可达地图的边界，那么地图是无效的
		squaresToBeAnalyzed.add(playerPosition);
		playersReachableSquaresOnlyWallsAtLevelStart[playerPosition] = true;

		while (!squaresToBeAnalyzed.isEmpty()) {
			currentPlayerPosition = squaresToBeAnalyzed.remove();

			if (       currentPlayerPosition < width
					|| currentPlayerPosition > size - width
					|| currentPlayerPosition % width == 0
					|| currentPlayerPosition % width == width - 1) {
				if (message.length() == 0) {
					message.append("无效的仓管员位置！");
				}

				continue;
			}

			for (int direction = 0; direction < DIRS_COUNT; direction++) {
				int nextPlayerPosition = currentPlayerPosition + offset[direction];
				if (       isWall(nextPlayerPosition) == false
						&& playersReachableSquaresOnlyWallsAtLevelStart[nextPlayerPosition] == false) {
					squaresToBeAnalyzed.add(nextPlayerPosition);
					playersReachableSquaresOnlyWallsAtLevelStart[nextPlayerPosition] = true;
				}
			}
		}

		boxCount   = 0;
		goalsCount = 0;
		for (int position = 0; position < size; position++) {
			if (isBox(position)) {
				if (playersReachableSquaresOnlyWallsAtLevelStart[position]) {
					boxCount++;
				}
			}
			if (isGoal(position)) {
				if (playersReachableSquaresOnlyWallsAtLevelStart[position]) {
					goalsCount++;
				}
			}
		}

		if (message.length() > 0) {
			return false;
		}

		// 箱子数 == 目标数？
		if (boxCount != goalsCount) {
			message.append("箱子与目标位不符");
			return false;
		}

		// 有没有箱子或目标点？
		if (boxCount == 0) {
			message.append("没有箱子或目标点");
			return false;
		}

		return true;
	}

	// 地图加载成功后的各项准备工作
	public void prepareBoard() {

		boxData = new BoxData(this);
		goalsPositions = new int[goalsCount];
		goalSquareBackwardsSearch = new boolean[size];
		goalPositionsBackwardsSearch = new int[goalsCount];

		distances = new Distances();

		goalsNumbers = new int[size];
		boxNumbers   = new int[size];

		int boxNo  = 0;
		int goalNo = 0;

		firstRelevantSquare = 0;

		for (int position = 0; position < size; position++) {

			if (isOuterSquareOrWall(position)) {
				continue;
			}

			if (isBox(position)) {
				boxNumbers[position] = boxNo;
				boxData.setBoxPosition(boxNo, position);

				goalSquareBackwardsSearch[position] = true;
				goalPositionsBackwardsSearch[boxNo] = position;

				boxNo++;
			} else {
				goalSquareBackwardsSearch[position] = false;
			}

			if (isGoal(position)) {
				goalsNumbers[position] = goalNo;
				goalsPositions[goalNo++] = position;
			}

			if (firstRelevantSquare == 0 && ! isOuterSquareOrWall(position)) {
				firstRelevantSquare = position;
			}

			lastRelevantSquare = position + 1;
		}

		distances.updateBoxDistances(Settings.SearchDirection.FORWARD, true );
		distances.updateBoxDistances(Settings.SearchDirection.BACKWARD, true);

		distances.calculatePlayerDistances();

		badSquares.identifySimpleDeadlockSquaresForwards();
		badSquares.identifySimpleDeadlockSquaresBackwards();
		badSquares.identifyAdvancedSimpleDeadlockSquaresForwards();
	}


	public void removeBox(int position) {
		boxesArray[position] = false;
	}

	public void removeBoxByNumber(int boxNo) {
		boxesArray[boxData.getBoxPosition(boxNo)] = false;
	}

	public void removeBox(int x, int y) {
		boxesArray[x + width * y] = false;
	}

	public void removeWall(int position) {
		wallsArray[position] -= ((wallsArray[position] > 0) ? 1 : 0);
	}

	public void removeWall(int x, int y) {
		wallsArray[x + width * y] -= ((wallsArray[x + width * y] > 0) ? 1 : 0);
	}

	public void removeGoal(int position) {
		goalsArray[position] = false;
	}

	public void removeGoal(int x, int y) {
		goalsArray[x + width * y] = false;
	}

	public void removePlayer() {
		playerPosition = NO_PLAYER;
	}

	public void setBox(int position) {
		boxesArray[position] = true;
	}

	public void setBox(int x, int y) {
		boxesArray[x + width * y] = true;
	}

	public void setBoxWithNo(int boxNo, int position) {
		boxesArray[position] = true;
		boxNumbers[position] = boxNo;
	}

	public void setBoxWithNo(int boxNo, int x, int y) {
		boxesArray[x + width * y] = true;
		boxNumbers[x + width * y] = boxNo;
	}

	public void setBoxOnGoal(int position) {
		boxesArray[position] = true;
		goalsArray[position] = true;
	}

	public void setBoxOnGoal(int x, int y) {
		boxesArray[x + width * y] = true;
		goalsArray[x + width * y] = true;
	}

	public void setGoal(int position) {
		goalsArray[position] = true;
	}

	public void setGoal(int x, int y) {
		goalsArray[x + width * y] = true;
	}

	public void setWall(int position) {
		wallsArray[position]++;
	}

	public void setWall(int x, int y) {
		wallsArray[x + width * y]++;
	}

	public void setBoxNo(int boxNo, int position) {
		boxNumbers[position] = boxNo;
	}

	public void setBoxNo(int boxNo, int x, int y) {
		boxNumbers[x + width * y] = boxNo;
	}

	public void setAdvancedSimpleDeadlock(int position) {
		advancedSimpleDeadlockSquareForwards[position] = true;
	}

	public void setPlayerPosition(int position) {
		playerPosition = position;
	}

	public void setPlayerPosition(int x, int y) {
		playerPosition = x + width * y;
	}

	public boolean isCorralForcerSquare(int position) {
		return corralForcer[position];
	}

	public boolean isCorralForcerSquare(int x, int y) {
		return corralForcer[x + width * y];
	}

	public boolean isBox(int position) {
		return boxesArray[position];
	}

	public boolean isBox(int x, int y) {
		return boxesArray[x + width * y];
	}

	public boolean isWall(int position) {
		return wallsArray[position] > 0;
	}

	public boolean isWall(int x, int y) {
		return wallsArray[x + width * y] > 0;
	}

	public boolean isGoal(int position) {
		return goalsArray[position];
	}

	public boolean isGoal(int x, int y) {
		return goalsArray[x + width * y];
	}

	public boolean isBoxOrWall(int position) {
		return wallsArray[position] > 0 || boxesArray[position];
	}

	public boolean isGoalOrWall(int position) {
		return goalsArray[position] || wallsArray[position] > 0;
	}

	public boolean isBoxOrWall(int x, int y) {
		return wallsArray[x + width * y] > 0 || boxesArray[x + width * y];
	}

	public boolean isCorralForcer(int position) {
		return corralForcer[position] == true;
	}

	public boolean isGoalBackwardsSearch(int position) {
		return goalSquareBackwardsSearch[position];
	}

	public boolean isGoalBackwardsSearch(int x, int y) {
		return goalSquareBackwardsSearch[x + width * y];
	}

	public boolean isEmptySquare(int position) {
		return ! (boxesArray[position] || wallsArray[position] > 0 || goalsArray[position]);
	}

	public boolean isEmptySquare(int x, int y) {
		return ! (   boxesArray[x + width * y]
		          || wallsArray[x + width * y] > 0
		          || goalsArray[x + width * y]     );
	}

	public boolean isAccessible(int position) {
		return ! (wallsArray[position] > 0 || boxesArray[position]);
	}

	public boolean isAccessible(int x, int y) {
		return ! (wallsArray[x + width * y] > 0 || boxesArray[x + width * y]);
	}

	public boolean isAccessibleBox(int position) {
		return ! (   wallsArray[position] > 0
				  || boxesArray[position]
				  || simpleDeadlockSquareForwards[position]
				  || advancedSimpleDeadlockSquareForwards[position] );
	}

	public boolean isAccessibleBox(int x, int y) {
		return ! (   wallsArray[x + width * y] > 0
				  || boxesArray[x + width * y]
				  || simpleDeadlockSquareForwards[x + width * y]
				  || advancedSimpleDeadlockSquareForwards[x + width * y] );
	}

	public boolean isWallOrIllegalSquare(int position) {
		return     wallsArray[position] > 0
				|| simpleDeadlockSquareForwards[position]
				|| advancedSimpleDeadlockSquareForwards[position];
	}

	public boolean isWallOrIllegalSquare(int x, int y) {
		return     wallsArray[x + width * y] > 0
				|| simpleDeadlockSquareForwards[x + width * y]
				|| advancedSimpleDeadlockSquareForwards[x + width * y];
	}

	public boolean isOuterSquareOrWall(int position) {
		return    ! playersReachableSquaresOnlyWallsAtLevelStart[position]
		       || wallsArray[position] > 0;
	}

	public boolean isSimpleDeadlockSquare(int position) {
		return     simpleDeadlockSquareForwards[position]
				|| advancedSimpleDeadlockSquareForwards[position]
				|| simpleDeadlockSquareBackwards[position];
	}

	public boolean isSimpleDeadlockSquare(int x, int y) {
		return     simpleDeadlockSquareForwards[x + width * y]
				|| advancedSimpleDeadlockSquareForwards[x + width * y]
				|| simpleDeadlockSquareBackwards[x + width * y];
	}

	public boolean isAdvancedSimpleDeadlockSquareForwards(int position) {
		return advancedSimpleDeadlockSquareForwards[position];
	}

	public boolean isAdvancedSimpleDeadlockSquareForwards(int x, int y) {
		return advancedSimpleDeadlockSquareForwards[x + width * y];
	}

	public boolean isBoxOnGoal(int position) {
		return boxesArray[position] && goalsArray[position];
	}

	public boolean isBoxOnGoal(int x, int y) {
		return boxesArray[x + width * y] && goalsArray[x + width * y];
	}

	public boolean isPlayerInLevel() {
		return playerPosition != NO_PLAYER;
	}

	//---------- Methods for "marking" (debug only) --------------------------------------

	public boolean isMarked(int position) {
		return marked[position];
	}

	public boolean isMarked(int x, int y) {
		return marked[x + width * y];
	}

	public void removeMarking(int position) {
		marked[position] = false;
	}

	public void removeMarking(int x, int y) {
		marked[x + width * y] = false;
	}

	public void removeAllMarking() {
		Arrays.fill(marked, false);			// this is not time critical
	}

	public void setMarking(int position) {
		marked[position] = true;
	}

	public void setMarking(int x, int y) {
		marked[x + width * y] = true;
	}

	public void assignMarking(int position, boolean markValue) {
		marked[position] = markValue;
	}

	public void flipMarking(int position) {
		marked[position] = ! marked[position];
	}

	//------------------------------------------------------------------------------------

	public int getBoxNo(int position) {
		return boxNumbers[position];
	}

	public int getGoalNo(int position) {
		return goalsNumbers[position];
	}

	public int getGoalPosition(int goalNo) {
		return goalsPositions[goalNo];
	}

	public int[] getGoalPositions() {
		return goalsPositions.clone();
	}

	public int[] getGoalPositionsBackward() {
		return goalPositionsBackwardsSearch;
	}

	public int getPosition(int position, int direction) {
		return position + offset[direction];
	}

	public int getPositionAtOppositeDirection(int position, int direction) {
		return position - offset[direction];
	}

	private int justPushBox(int fromSquare, int toSquare, String msgSuff) {

		if (isBox(fromSquare) == false) {
			return -1;
		}

		if (fromSquare == toSquare) {
			return -1;
		}

		if (isAccessible(toSquare) == false) {
			return -1;
		}

		final int boxNo = getBoxNo(fromSquare);
		removeBox(fromSquare);
		setBoxWithNo(boxNo, toSquare);

		boxData.setBoxPosition(boxNo, toSquare);

		return boxNo;
	}

	public int pushBox(int fromSquare, int toSquare) {
		return justPushBox(fromSquare, toSquare, "");
	}

	public int pushBoxUndo(int fromSquare, int toSquare) {
		final int boxNo = justPushBox(fromSquare, toSquare, " (undo)");

		if (boxNo >= 0) {
			if (boxData.isBoxFrozen(boxNo)) {
				boxData.setBoxUnfrozen(boxNo);
			}
		}

		return boxNo;
	}

	// 快手添加：仅为死锁检查
	public void moveBox(int fromSquare, int toSquare) {
		final int boxNo = getBoxNo(fromSquare);
		removeBox(fromSquare);
		setBoxWithNo(boxNo, toSquare);

		boxData.setBoxPosition(boxNo, toSquare);
	}

	public int getPlayerDistance(int fromSquare, int toSquare) {
		return playerDistances[fromSquare][toSquare];
	}

	public void setBoardPosition(IBoardPosition position) {
		setBoardPosition(position.getPositions());
	}

	public void setBoardPosition(int[] positions) {

		removeAllBoxes();

		boxData.setBoxPositions(positions);

		for (int boxNo = 0; boxNo < boxCount; boxNo++) {
			setBoxWithNo(boxNo, positions[boxNo]);
		}

		playerPosition = positions[boxCount];
	}

	public void removeAllBoxes() {
		for (int boxNo = 0; boxNo < boxCount; boxNo++) {
			removeBox(boxData.getBoxPosition(boxNo));
		}
	}

	public void setGoalsBackwardsSearch() {

		int goalNo = 0;

		for (int position = firstRelevantSquare; position < lastRelevantSquare; position++) {
			if (isBox(position) && isOuterSquareOrWall(position) == false) {
				goalSquareBackwardsSearch[position] = true;
				goalPositionsBackwardsSearch[goalNo++] = position;
			} else {
				goalSquareBackwardsSearch[position] = false;
			}
		}
	}

	@Override
	public Board clone() {

		Board newBoard = new Board();

		try {
			newBoard.setBoardFromString(getBoardDataAsString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		newBoard.isValid(new StringBuilder());
		newBoard.prepareBoard();

		return newBoard;
	}

	public String getBoardDataAsString() {

		StringBuilder boardAsString = new StringBuilder(size+height+2);

		for (int position = 0; position < size; position++) {

			if (position > 0 && position % width == 0) {
				Utilities.removeTrailingSpaces(boardAsString);
				boardAsString.append('\n');
			}

			if (isWall(position)) {
				boardAsString.append('#');
				continue;
			}
			if (isBoxOnGoal(position)) {
				boardAsString.append('*');
				continue;
			}
			if (isBox(position)) {
				boardAsString.append('$');
				continue;
			}
			if (isGoal(position)) {
				if (playerPosition == position) {
					boardAsString.append('+');
				} else {
					boardAsString.append('.');
				}
				continue;
			}
			if (playerPosition == position) {
				boardAsString.append('@');
				continue;
			}

			boardAsString.append(' ');
		}

		Utilities.removeTrailingSpaces(boardAsString);

		boardAsString.append('\n');
		return boardAsString.toString();
	}

	public int getBoxesOnGoalsCount() {
		int boxOnGoalCounter = 0;
		for (int boxNo = 0; boxNo < boxCount; boxNo++) {
			if (isGoal(boxData.getBoxPosition(boxNo))) {
				boxOnGoalCounter++;
			}
		}

		return boxOnGoalCounter;
	}

	public class PlayerPathCalculation {

		/** Marker for the start position of the player. Used for indicating */
		private final static int START_POSITION_MARKER = Integer.MIN_VALUE;

		/** Indicating that a position hasn't been reached from any direction, yet. */
		private final static int NONE = -1;

		/** All reached positions are stored in this queue. */
		private final int[] queue;

		/** Storage for saving which position has been reached from which previous position. */
		private final int[] reachedFromPosition;

		/**
		 * Creates a new object for calculatin the player path.
		 */
		public PlayerPathCalculation() {
			queue = new int[size];
			reachedFromPosition = new int[size];
		}

		/**
		 * Returns the positions of the shortest path to be gone by the player
		 * to get from the current player position to the passed target position.
		 *
		 * @param targetPosition  target position of the player
		 * @return array of positions to be gone to reach the target position
		 * including the start position and the target position<br>.
		 * If the start position is the target position then the array is empty.<br>
		 * If there is no path to the target position <code>null</code> is returned.
		 */
		public int[] getPathTo(final int targetPosition) {
			return getPath(playerPosition, targetPosition);
		}

		/**
		 * Returns the positions of the shortest path to be gone by the player
		 * to get from the passed start position to the passed target position.
		 *
		 * @param startPosition   current position of the player
		 * @param targetPosition  target position of the player
		 * @return array of positions to be gone to reach the target position
		 * including the start position and the target position<br>.
		 * If the start position is the target position then the array only
		 * contains the target position. Hence, the length of the array is
		 * always "moves to be done + 1".<br>
		 * If there is no path to the target position <code>null</code> is returned.
		 */
		public int[] getPath(final int startPosition, final int targetPosition) {

			// Quick check whether the target position is already been reached.
			if(startPosition == targetPosition) {
				return new int[] { targetPosition };//TODO: FFS/mm: isn't it better to return new int[0]?
			}

			// Initialize the reachedFrom array using invalid positions.
			for(int i=0; i<reachedFromPosition.length; i++) {
				reachedFromPosition[i] = NONE;
			}

			// Initialization of the queue indices.
			int indexToWrite = 0;
			int indexToTake  = 0;

			// Mark the start position as reached by setting an invalid previous position.
			reachedFromPosition[startPosition] = START_POSITION_MARKER;

			int currentPlayerPosition = startPosition;

			// Do a breadth first search to find the shortest path to the target position.
			do {
				// Move the player to all four directions.
				for(int directionOffset : offset) {

					// Calculate the new player position when moving to the direction.
					final int newPlayerPosition = currentPlayerPosition + directionOffset;

					// Check whether the player can move to the new position and has never moved there before.
					if(!isBoxOrWall(newPlayerPosition) && reachedFromPosition[newPlayerPosition] == NONE) {

						// Check whether the target position has been reached.
						if (newPlayerPosition == targetPosition) {

							indexToWrite = queue.length-1;

							// Target position must be included.
							queue[indexToWrite] = newPlayerPosition;

							// Go backwards from the target position to the start position and
							// save the path gone.
							do {
								queue[--indexToWrite] = currentPlayerPosition;
								currentPlayerPosition = reachedFromPosition[currentPlayerPosition];
							}while(currentPlayerPosition != START_POSITION_MARKER);

							// Copy the path into an array having just the right size and return it.
							return Arrays.copyOfRange(queue, indexToWrite, queue.length);
						}

						// Save from which position the new position has been reached.
						reachedFromPosition[newPlayerPosition] = currentPlayerPosition;

						// It must be checked if any neighbor of the new
						// position can be reached, too => queue the new
						// position for further processing.
						queue[indexToWrite++] = newPlayerPosition;
					}
				}

				// Get the next position from the queue.
				currentPlayerPosition = queue[indexToTake++];

			}while(indexToTake <= indexToWrite); // As long as there are positions in the queue

			// Return null since no path to the target position has been found.
			return null;
		}
	}


	/**
	 * This class combines the 3 arrays which form an entry
	 * of a {@link DistCache} as used in {@link Distances}.
	 */
	public class DistCacheElement {
		short[][][] boxDistances;
		boolean[]   corralForcer;
		short[][]   playerDistances;

		/**
		 * Object for storing box distances, corral forcer positions and player distances.
		 *
		 * @param boxDistances
		 * @param corralForcer
		 * @param playerDistances
		 */
		public DistCacheElement(
				short[][][] boxDistances,
				boolean[]   corralForcer,
				short[][]   playerDistances )
		{
			this.boxDistances    = boxDistances;
			this.corralForcer    = corralForcer;
			this.playerDistances = playerDistances;
		}
	}

	/**
	 * A cache for the computation of box distances based on the current
	 * box freeze situation.
	 * Forward and backward search use seperate cache objects.
	 *
	 * @author Heiner Marxen
	 */
	public class DistCache extends LruCache<BitSet, DistCacheElement> {
		/*
		 * We take advantage of:
		 * - BitSet (the key) has a good hashCode() (data based)
		 * - BitSet (the key) has a deep clone()
		 * - all the remembered arrays get used by reference (not by copy),
		 *   since their contents are never changed after computation.
		 */

		/**
		 * Creates a new cache for box distances.
		 *
		 * @param initialCapacity  initial capacity
		 */
		public DistCache(final int initialCapacity) {
			super(initialCapacity);
			this.setMinRAMinMiB(10);
		}
	}

	/**
	 * Computes the box distances from any square to any square,
	 * depending on the player position.
	 */
	public class Distances {

		// [Richtung][vonPosition][nachPosition]
		// Richtung = wo der Spieler relativ zur vonPosition steht. Ein Eintrag im Array steht jeweils für
		// die minimale Kistendistanz einer Kiste, die von der "vonPosition" zur "nachPosition" geschoben
		// wird (wird in "berechneKistenentfernungen" berechnet)
		protected short[][][] boxDistancesForwards;
		protected short[][][] boxDistancesBackwards;

		/**
		 * For performance reasons we want to cache the results of box
		 * distance computations (which change each time a box is frozen).
		 */
		private DistCache distancesForwCache;
		private DistCache distancesBackCache;
		private int[] goalsPositionsBackwardsSearchUsedForCaching = null;

		// Inhalt der Variablen = auf welchem Zielfeld steht eine geblockte Kiste.
		// Steht z.B. auf dem 5. Zielfeld eine geblockte Kiste ist Bit Nr. 5 gesetzt.
		private BitSet currentFreezeSituationForwards;
		private BitSet currentFreezeSituationBackwards;

		/**
		 * Maximally necessary size of a Q to contain one entry for each
		 * possible pair of (boxPosition, playerPosition), for a maximally
		 * large level.
		 */
		public static final int BOX_PL_Q_SIZE = MAXIMUM_BOARDSIZE * MAXIMUM_BOARDSIZE * 4;

		/**
		 * This array is part of a queue (Q), used to perform a reachability analysis.
		 * Each entry in the Q consists of 3 components, each of which has its own array.
		 * This one stores positions of boxes.
		 * The other 2 components are {@link #playerPositionsQ} for player positions,
		 * and {@link #distancesQ} for distances.
		 * <p>
		 * Implemented globally for performance reasons.
		 */
		short[] boxPositionsQ    = new short[BOX_PL_Q_SIZE];
		/** Second part of the Q described at {@link #boxPositionsQ} */
		short[] playerPositionsQ = new short[BOX_PL_Q_SIZE];
		/** Third  part of the Q described at {@link #boxPositionsQ} */
		short[] distancesQ       = new short[BOX_PL_Q_SIZE];

		/**
		 * Constructor
		 */
		public Distances() {
			distancesForwCache = new DistCache(5);	// FFS/hm capacity
			distancesBackCache = new DistCache(5);
			currentFreezeSituationForwards  = new BitSet(goalsCount);
			currentFreezeSituationBackwards = new BitSet(goalsCount);
		}

		/**
		 * Returns the push distance of a specific box to a specific goal.
		 * <p>
		 * The distance is calculated under the assumption that:
		 * <ol>
		 *  <li> the box is the only one on the whole board
		 *  <li> the player can reach every side of the box at the moment
		 * </ol>
		 *
		 * @param boxNo number of the relevant box
		 * @param goalNo number of the relevant goal
		 * @return push distance or #
		 */
		public int getBoxDistanceForwardsPlayerPositionIndependentNo(int boxNo, int goalNo) {
			return getBoxDistanceForwardsPlayerPositionIndependent(boxData.getBoxPosition(boxNo), goalsPositions[goalNo]);
		}

		/**
		 * Returns the push distance needed for pushing a box from the passed
		 * start position to the passed target position.
		 * <p>
		 * The distance is calculated under the assumption that:
		 * <ol>
		 *  <li> the box is the only one on the whole board
		 *  <li> the player can reach every side of the box at the moment
		 * </ol>
		 * @param startPosition  position the pushing begins
		 * @param targetPosition position the pushing ends
		 * @return push distance or {@link Board#UNREACHABLE}
		 */
		public int getBoxDistanceForwardsPlayerPositionIndependent(int startPosition, int targetPosition) {

			// If it isn't a corral forcer square then the position of the player doesn't matter because the
			// player can reach ever side of the box. Hence the precalculated distances are equally high for
			// every player position.
			if (isCorralForcerSquare(startPosition) == false) {
				return boxDistancesForwards[UP][startPosition][targetPosition];
			}

			// Determine the minimum push distance assuming the player can reach every side of the box.
			int minimalDistance = UNREACHABLE;
			for (int direction = 0; direction < DIRS_COUNT; direction++) {
				if (minimalDistance > boxDistancesForwards[direction][startPosition][targetPosition]) {
					minimalDistance = boxDistancesForwards[direction][startPosition][targetPosition];
				}
			}
			// Return the determined distance.
			return minimalDistance;

		}

		/**
		 * Gibt die Kistendistanz zu einem bestimmten Zielfeld zurück, wenn nur diese Kiste
		 * auf dem Feld stehen würde und der Spieler an der aktuellen Position steht.
		 * Vorwärts = die Kiste wird geschoben und nicht gezogen.
		 *
		 * @param boxNo		Nummer der Kiste, für die die Distanz berechnet werden soll
		 * @param goalNo	Nummer des Zielfeldes, das die Kiste erreichen soll
		 *
		 * @return the distance of the box, {@link Board#UNREACHABLE} if the box can't reach the goal
		 */
		public int getBoxDistanceForwardsNo(int boxNo, int goalNo) {
			return getBoxDistanceForwardsPosition(boxData.getBoxPosition(boxNo), goalsPositions[goalNo]);
		}

		/**
		 * Gibt die Kistendistanz zurück, die eine Kiste zurücklegen muss, um von der Startposition
		 * auf die Zielposition geschoben zu werden.
		 * Annahmen: Es steht nur diese eine Kiste auf dem Feld und der Spieler steht an der
		 * aktuellen Position.
		 * Vorwärts = die Kiste wird geschoben und nicht gezogen.
		 *
		 * @param fromSquare 	Startposition von wo aus die Distanz berechnet werden soll
		 * @param toSquare	Position des Zielfeldes, das die Kiste erreichen soll
		 * @return return Kistendistanz vom Start- zum Zielfeld, UNENDLICH = Kiste kann Ziel nicht erreichen
		 */
		public int getBoxDistanceForwardsPosition(int fromSquare, int toSquare) {

			// Only for influence calculations all distances are calculated. Hence, it's most likey a bug that a distance to a non goal
			// is to be returned.

            // System.out.println("box distances have only been calculated to goal positions!");

			// Falls es sich nicht um ein Corralerzwingerfeld handelt ist die Position des Spielers
			// egal und es kann eine beliebige Richtung angenommen werden.
			// (bei einem NichtCorralfeld ist die Distanz immer gleich groß, egal auf welchem Nachbar-
			// feld der Kiste der Spieler steht. Selbst wenn der Spieler auf einer Mauer steht!
			// -> siehe dazu "berechneKistenentfernungen...")
			if (isCorralForcerSquare(fromSquare) == false) {
				return boxDistancesForwards[UP][fromSquare][toSquare];
			}

			// Falls der Spieler "auf" der Kiste steht kann er auf alle Seiten der Kiste gelangen
			// und es muss die kleinste Distanz aller Richtungen zurückgegeben werden.
			// Dieser Fall kann auftreten, da während der Corralanalyse Kisten vom Feld genommen
			// werden. Wird nun z.B. in Freeze der Spieler auf diese "freie" Position gestellt,
			// so steht der Spieler auf einer Kiste, da der Lowerbound alle Kisten berücksichtigt,
			// auch die deaktiven! (Lowerbound bekommt die Kistenpositionen aus dem
			// boxData-Object in board)
			if (playerPosition == fromSquare) {
				int minimalDistance = UNREACHABLE;
				for (int direction = 0; direction < DIRS_COUNT; direction++) {
					if (minimalDistance > boxDistancesForwards[direction][fromSquare][toSquare]) {
						minimalDistance = boxDistancesForwards[direction][fromSquare][toSquare];
					}
				}
				return minimalDistance;
			}

			/*
			 * Die Kiste trennt das Spielfeld in Bereiche, die durch den Spieler erreichbar sind
			 * und die nicht durch den Spieler erreichbar sind. In diesem Fall ist es also
			 * wichtig zu wissen, auf welche Seite der Kiste der Spieler gelangen kann.
			 * Dazu wird folgendes gemacht:
			 * Es wird zunächst die Distanz ermittelt, die der Spieler benötigen würde die
			 * Startposition direkt zu erreichen. Um diese Startposition direkt zu erreichen,
			 * muss der Spieler über eines der 4 Nachbarfelder laufen. Für das Erreichen eines
			 * der Nachbarfelder benötigt er weniger Bewegungen. Es muss also nur ermittelt
			 * werden, welches Nachbarfeld er mit weniger Bewegungen erreichen kann.
			 */
			int playerDistance = getPlayerDistance(playerPosition, fromSquare);

			for (int direction = 0; direction < DIRS_COUNT; direction++) {
				if (getPlayerDistance(playerPosition, fromSquare + offset[direction]) < playerDistance) {
					return boxDistancesForwards[direction][fromSquare][toSquare];
				}
			}

			// Falls die Startposition der Zielposition entspricht ist die Distanz 0.
			// Dies muss extra abgefangen werden, da es sein kann, dass eine Kiste auf einem
			// Zielfeld für den Spieler aufgrund von Blockerkisten unerreichbar wird.
			if (fromSquare == toSquare) {
				return 0;
			}

			// Die Kiste ist nicht durch den Spieler erreichbar. Dies kann sein, wenn eine Blockerkiste
			// wie eine Mauer behandelt wird und dadurch eine andere Kiste vom Spielerbereich abtrennt.
			return UNREACHABLE;
		}

		/**
		 * Gibt die Kistendistanz zu einem bestimmten Zielfeld zurück, wenn nur diese Kiste
		 * auf dem Feld stehen würde und der Spieler an der aktuellen Position steht.
		 * Rückwärts = die Kiste wird gezogen und nicht geschoben.
		 *
		 * @param boxNo		Nummer der Kiste, für die die Distanz berechnet werden soll
		 * @param goalNo	Nummer des Zielfeldes, das die Kiste erreichen soll
		 *
		 * @return the distance of the box, infinite if the box can't reach the goal
		 */
		public int getBoxDistanceBackwardsNo(int boxNo, int goalNo) {

			// Achtung! Die Rückwärtssuche hat eigene Zielfelder (nämlich die Startpositionen der Kisten)
			return getBoxDistanceBackwardPosition(boxData.getBoxPosition(boxNo), goalPositionsBackwardsSearch[goalNo]);
		}

		/**
		 * Gibt die Kistendistanz zurück, die eine Kiste zurücklegen muss, um von der Startposition
		 * auf die Zielposition gezogen zu werden.
		 * Annahmen: Es steht nur diese eine Kiste auf dem Feld und der Spieler steht an der
		 * aktuellen Position.
		 * Vorwärts = die Kiste wird geschoben und nicht gezogen.
		 *
		 * @param fromSquare	Position der Kiste, für die die Distanz berechnet werden soll
		 * @param toSquare	Position des Zielfeldes, das die Kiste erreichen soll
		 *
		 * @return the distance of the box, infinite if the box can't reach the toSquare
		 */
		public int getBoxDistanceBackwardPosition(int fromSquare, int toSquare) {

			// Falls es sich nicht um ein Corralerzwingerfeld handelt ist die Position des Spielers
			// egal und es kann eine beliebige Richtung angenommen werden.
			// (bei einem NichtCorralfeld ist die Distanz immer gleich groß, egal auf welchem Nachbar-
			// feld der Kiste der Spieler steht. Selbst wenn der Spieler auf einer Mauer steht!
			// -> siehe dazu "berechneKistenentfernungen...")
			if (isCorralForcerSquare(fromSquare) == false) {
				return boxDistancesBackwards[UP][fromSquare][toSquare];
			}

			// Falls der Spieler "auf" der Kiste steht kann er auf alle Seiten der Kiste gelangen
			// und es muss die kleinste Distanz aller Richtungen zurückgegeben werden.
			// Dieser Fall kann auftreten, da während der Corralanalyse Kisten vom Feld genommen
			// werden. Wird nun z.B. in Freeze der Spieler auf diese "freie" Position gestellt,
			// so steht der Spieler auf einer Kiste, da der Lowerbound alle Kisten berücksichtigt,
			// auch die deaktiven! (Lowerbound bekommt die Kistenpositionen aus dem
			// boxData-Objekt in board)
			// Bei der Durchsuchung eines Zielfeldraumes werden Kisten vom Feld genommen, die den
			// Zielraum bereits verlassen haben. In diesem Fall werden die Kiste so gezählt, als wenn
			// sie mit der minimalen Distanz ein Zielfeld erreichen können.
			if (playerPosition == fromSquare || isBox(fromSquare) == false) {
				int minimalDistance = UNREACHABLE;
				for (int direction = 0; direction < DIRS_COUNT; direction++) {
					if (minimalDistance > boxDistancesBackwards[direction][fromSquare][toSquare]) {
						minimalDistance = boxDistancesBackwards[direction][fromSquare][toSquare];
					}
				}
				return minimalDistance;
			}

			/*
			 * Die Kiste trennt das Spielfeld in Bereiche, die durch den Spieler erreichbar sind
			 * und die nicht durch den Spieler erreichbar sind. In diesem Fall ist es also
			 * wichtig zu wissen, auf welche Seite der Kiste der Spieler gelangen kann.
			 * Dazu wird folgendes gemacht:
			 * Es wird zunächst die Distanz ermittelt, die der Spieler benötigen würde die
			 * Startposition direkt zu erreichen. Um diese Startposition direkt zu erreichen,
			 * muss der Spieler über eines der 4 Nachbarfelder laufen. Für das Erreichen eines
			 * der Nachbarfelder benötigt er weniger Bewegungen. Es muss also nur ermittelt
			 * werden, welches Nachbarfeld er mit weniger Bewegungen erreichen kann.
			 */
			int playerDistance = getPlayerDistance(playerPosition, fromSquare);

			for (byte direction = 0; direction < DIRS_COUNT; direction++) {
				if (getPlayerDistance(playerPosition, fromSquare + offset[direction]) < playerDistance) {
					return boxDistancesBackwards[direction][fromSquare][toSquare];
				}
			}

			// Falls die Startposition der Zielposition entspricht ist die Distanz 0.
			// Dies muss extra abgefangen werden, da es sein kann, dass eine Kiste auf einem
			// Zielfeld für den Spieler aufgrund von Blockerkisten unerreichbar wird.
			if (fromSquare == toSquare) {
				return 0;
			}

			// Die Kiste ist nicht durch den Spieler erreichbar. Dies kann sein, wenn eine Blockerkiste
			// wie eine Mauer behandelt wird und dadurch eine andere Kiste vom Spielerbereich abtrennt.
			return UNREACHABLE;
		}

		/**
		 * Determines the squares on which a box would induce a closed area,
		 * i.e. a corral with just one box.
		 * Example:<pre>
		 *  ###
		 * XX #
		 *  ###</pre>
		 * The squares marked with <code>X</code> are such corral forcer
		 * squares, since a box on one of them would create a closed area,
		 * which is unreachable for the player, even if there were no other
		 * box on the board.
		 * <p>
		 * Please note: we do not imply that such a corral cannot be resolved!
		 * A box on the first <code>X</code>, e.g. could be pushed upwards
		 * and resolve the corral.
		 * To be more precise: these squares create an area, which the player
		 * cannot reach without at least pushing some box.
		 */
		private void identifyCorralForcerSquares() {

			// Position
			int neighbor;

			// We have to create a new array for each call, since these arrays
			// are going to be buffered in a vector.
			corralForcer = new boolean[size];

			// Scan all squares
			PositionsLoop:
			for (int center = firstRelevantSquare; center < lastRelevantSquare; center++) {

				// skip outer squares and walls
				if (isOuterSquareOrWall(center)) {
					continue;
				}

				int wallMask = 0;
				int startPosition = center + offset[UP] + offset[LEFT];
				int bitNo = 0;
				for (int x = 0; x < 3; x++) {
					for (int y = 0; y < 3; y++) {
						neighbor = startPosition + x + y * offset[DOWN];
						if (neighbor != center) {
							if (isWall(neighbor)) {
								wallMask |= (1 << bitNo);
							}
							bitNo++;
						}
					}
				}

				// When the current neighborhood is a potential corral forcer,
				// we determine, whether it is a real corral forcer.
				if (corralForcerSituations[wallMask]) {

					// We search for two non-wall neighbor squares, such that
					// one is not player reachable from the other.
					// To enumerate neighbors we enumerate ordered pairs
					// of different directions.
					// FFS/hm: we could still reduce the call to "update"
					for (int dir1 = 0; dir1 < (DIRS_COUNT-1); dir1++) {
						neighbor = center + offset[dir1];

						// Skip neighbors blocked by walls
						if (isWall(neighbor)) {
							continue;
						}

						// Determine the reachable squares for the neighbor
						// square, with a wall at the center.
						setWall(center);
						playersReachableSquaresOnlyWalls.update(neighbor);
						removeWall(center);

						// Search for a different neighbor, which is unreachable
						for (int dir2 = dir1 + 1; dir2 < DIRS_COUNT; dir2++) {
							neighbor = center + offset[dir2];

							// Skip neighbors blocked by walls
							if (isWall(neighbor)) {
								continue;
							}

							if (playersReachableSquaresOnlyWalls.isSquareReachable(neighbor) == false) {
								// Placing a wall on the "center" makes this
								// neighbor unreachable from the other one:
								// that splits the formerly connected area.
								corralForcer[center] = true;
								continue PositionsLoop;
							}
						}
					}
				}
			}
		}

		/**
		 * Calculates the box distances from all squares to the target square, with respect
		 * to all possible player positions.  We do that backwards, i.e. from any square
		 * we pull a box to all reachable squares to determine the distances.
		 *
		 * @param targetPosition the position the distances have to be calculated for (any square to this target position)
		 */
		public void calculateBoxDistancesForwards(int targetPosition) {

			int oppositeDirectionOfPull;
			short next_in = 0;
			short next_out = 0;
			short playerPosition;
			short distance;
			short boxPosition = 0;

			// Alle Außenfelder und Mauern überspringen.
			// Achtung: Auf die Positionen, auf denen eine geblockte Kiste auf einem Zielfeld
			// steht wurde auch eine Mauer gesetzt. Diese Positionen müssen zu sich selbst
			// eine Entfernung von 0 besitzen, damit sie bei der Lowerboundberechnung nicht
			// als Deadlock gelten!
			if (isOuterSquareOrWall(targetPosition)) {
				if (isBoxOnGoal(targetPosition)) {
					for (byte direction = 0; direction < DIRS_COUNT; direction++) {
						boxDistancesForwards[direction][targetPosition][targetPosition] = 0;
					}
				}
				return;
			}

			next_in = 0;
			next_out = 0;

			// Versuchen, eine Kiste vom relevanten Feld in alle möglichen Richtungen zu ziehen
			// und damit Startdaten für die eigentliche Schleife (die "while-Schleife") erzeugen.
			for (byte direction = 0; direction < DIRS_COUNT; direction++) {

				boxPosition    = (short) (targetPosition +     offset[direction]);
				playerPosition = (short) (targetPosition + 2 * offset[direction]);

				// Die Entfernung des Feldes zu sich selbst ist 0.
				boxDistancesForwards[direction][targetPosition][targetPosition] = 0;

				// Falls auf das neue Feld gezogen werden kann, kommt es in den Stack
				if (isWall(boxPosition) == false && isWall(playerPosition) == false) {
					boxPositionsQ   [next_in] = boxPosition;
					playerPositionsQ[next_in] = playerPosition;
					distancesQ      [next_in] = 1;
					next_in++;
				}
			}

			// Ausgehend von den Startpositionen wird nun ermittelt auf welche Felder
			// die Kiste noch gezogen werden kann und welche Entfernungen diese weiteren
			// Felder zu dem Feld "position" haben.
			while (next_out < next_in) {
				boxPosition    = boxPositionsQ   [next_out];
				playerPosition = playerPositionsQ[next_out];
				distance       = distancesQ      [next_out];
				next_out++;

				// Prüfen, ob der Spieler auf alle freien Seiten der Kiste gelangen kann (= Kein Corralerzwingerfeld)
				if (isCorralForcerSquare(boxPosition) == false) {

					// Wenn die neue ermittelte Distanz nicht geringer ist als die bereits vorher
					// ermittelte, dann kann gleich das nächste Feld verarbeitet werden.
					if (boxDistancesForwards[UP][boxPosition][targetPosition] <= distance) {
						continue;
					}

					// Distanzen vom aktuellen Feld zum "Hauptfeld" eintragen
					boxDistancesForwards[UP   ][boxPosition][targetPosition] = distance;
					boxDistancesForwards[DOWN ][boxPosition][targetPosition] = distance;
					boxDistancesForwards[LEFT ][boxPosition][targetPosition] = distance;
					boxDistancesForwards[RIGHT][boxPosition][targetPosition] = distance;

					// Prüfen, auf welche Nachbarfelder die Kiste vom aktuellen Feld
					// gezogen werden kann. Dieses Nachbarfeld kann dann mit
					// Distanz+1 Pulls erreicht werden.
					// Falls das aktuelle Feld und das vorige Feld beide keine Corralerzwingerfelder
					// sind, kann ein Zurückschieben verhindert werden, da in diesem Fall die alte
					// Stellung erneut erreicht würde nur mit einer höheren Distanz.
					// (Falls die Kiste auf dem Ursprungsfeld steht (positionKiste == position)
					// könnte ein Zurückschieben auch vermieden werden, da für die Ursprungsposition
					// der Spieler auf alle möglichen Nachbarpositionen gesetzt wurde und somit
					// schon jede Richtung untersucht wird. Diese zusätzliche Abrage kostet aber
					// mehr Zeit, als die Verhinderung des zusätzlichen Pulls in die Gegenrichtung
					// gewinnen würde ...)
					if (isCorralForcerSquare(2 * boxPosition - playerPosition)) {
						// Das alte Feld war ein Corralerzwinger. Es muss auf jeden Fall ein
						// Zurückschieben geprüft werden. Deshalb Gegenrichtung auf irgend einen
						// ungültigen Wert setzen.  FFS/hm: 1000 ist "unguenstig"
						oppositeDirectionOfPull = 1000;
					} else {
						// Sowohl das alte als auch das aktuelle Feld der Kiste sind NichtCorralerzwinger.
						// Der Spieler kann also bei beiden Feldern auf alle freien Seiten kommen.
						// Ein Zurückschieben ist deshalb unnötig.
						oppositeDirectionOfPull = boxPosition - playerPosition;
					}

					for (byte direction = 0; direction < DIRS_COUNT; direction++) {
						if (offset[direction] != oppositeDirectionOfPull
								&& isWall(boxPosition + offset[direction]) == false
								&& isWall(boxPosition + 2 * offset[direction]) == false) {
							boxPositionsQ   [next_in] = (short) (boxPosition +     offset[direction]);
							playerPositionsQ[next_in] = (short) (boxPosition + 2 * offset[direction]);
							distancesQ      [next_in] = (short) (distance + 1);
							next_in++;
						}
					}
				} else {
					// erreichbare Felder des Spielers ermitteln, wobei nur auf der
					// aktuellen Position eine Kiste im Weg steht.
					// (Die Kiste wird durch eine Mauer simuliert)
					setWall(boxPosition);
					playersReachableSquaresOnlyWalls.update(playerPosition);
					removeWall(boxPosition);

					// Bei Corralerzwingerfeldern reicht es nicht zu prüfen, ob die beiden für ein Ziehen
					// relevanten Felder frei sind (= keine Mauer), sondern es muss auch noch geprüft werden,
					// ob der Spieler tatsächlich auf die jeweilige Seite gelangen kann. Ein Ziehen macht
					// natürlich auch nur dann Sinn, wenn die Situation nicht bereits vorher mit einer
					// kleineren oder genau so hohen Distanz erzeugt wurde.
					for (byte direction = 0; direction < DIRS_COUNT; direction++) {
						if (playersReachableSquaresOnlyWalls.isSquareReachable(boxPosition + offset[direction]) == false
								|| boxDistancesForwards[direction][boxPosition][targetPosition] <= distance) {
							continue;
						}

						// Es wurde ein Weg mit einer geringeren Distanz gefunden.
						boxDistancesForwards[direction][boxPosition][targetPosition] = distance;

						// Prüfen, auf welche Nachbarfelder die Kiste vom aktuellen Feld gezogen werden kann.
						// Das direkte Nachbarfeld wurde ja bereits durch den obigen "if" auf frei (=erreichbar)
						// geprüft. Es muss nun geprüft werden, ob das zweite für ein Ziehen notwendige Feld
						// ebenfalls frei ist. (man könnte hier genau so gut auf board.isWall(...) == false
						// prüfen, das bewirkt das gleiche ...)
						if (playersReachableSquaresOnlyWalls.isSquareReachable(boxPosition + 2 * offset[direction])) {
							boxPositionsQ   [next_in] = (short) (boxPosition +     offset[direction]);
							playerPositionsQ[next_in] = (short) (boxPosition + 2 * offset[direction]);
							distancesQ      [next_in] = (short) (distance + 1);
							next_in++;
						}
					}
				}
			}
		}

		/**
		 * Calculates the box distances from all squares to all squares, with respect
		 * to all possible player positions for pulls (backward pushes).
		 * We do that forwards, i.e. from any square we push a box to all
		 * reachable squares to determine the distances.
		 */
		private void calculateBoxDistancesBackwards() {

			int oppositeDirectionOfPush;
			int next_in = 0;
			int next_out = 0;
			short playerPosition;
			short distance;
			short boxPosition = 0;

			// Array, das die Kistendistanzen aufnimmt. Dieses Array muss bei jedem Aufruf
			// neu angelegt werden, da es später in einem Vector gepuffert wird.
			boxDistancesBackwards = new short[DIRS_COUNT][size][size];

			// Alle Kistendistanzen mit "unendlich" initialisieren
			Utilities.fillArray(boxDistancesBackwards, UNREACHABLE);

			// CorralerzwingerFelder ermitteln
			identifyCorralForcerSquares();

			// Alle Rückwärtszielfelder durchgehen und die Entfernung von jedem anderen Feld zu diesem Feld ermitteln
			for (int goalNo=0; goalNo<goalsCount;goalNo++) {

				int position = goalPositionsBackwardsSearch[goalNo];

				// Alle Außenfelder und Mauern überspringen
				if (isOuterSquareOrWall(position)) {
					continue;
				}

				next_in  = 0;
				next_out = 0;

				// Versuchen, eine Kiste vom relevanten Feld in alle möglichen Richtungen zu verschieben
				// und damit Startdaten für die eigentliche Schleife (die "while-Schleife") erzeugen.
				for (byte dir = 0; dir < DIRS_COUNT; dir++) {

					// Die Entfernung des Feldes zu sich selbst ist 0.
					boxDistancesBackwards[dir][position][position] = 0;

					// Falls auf das neue Feld verschoben werden kann, kommt es in den Stack
					if (       !isWall(position + offset[dir])
							&& !isWall(position - offset[dir]) ) {
						boxPositionsQ   [next_in] = (short) (position + offset[dir]);
						playerPositionsQ[next_in] = (short) (position);
						distancesQ      [next_in] = 1;
						next_in++;
					}
				}

				// Ausgehend von den Startpositionen wird nun ermittelt auf welche Felder
				// die Kiste noch verschoben werden kann und welche Entfernungen diese weiteren
				// Felder zu dem Feld "position" haben.
				while (next_out < next_in) {
					boxPosition    = boxPositionsQ   [next_out];
					playerPosition = playerPositionsQ[next_out];
					distance       = distancesQ      [next_out];
					next_out++;

					// Prüfen, ob der Spieler auf alle freien Seiten der Kiste gelangen kann
					// (= Kein Corralerzwingerfeld)
					if (isCorralForcerSquare(boxPosition) == false) {

						// Wenn die neue ermittelte Distanz nicht geringer ist als die bereits vorher
						// ermittelte, dann kann gleich das nächste Feld verarbeitet werden.
						if (boxDistancesBackwards[UP][boxPosition][position] <= distance) {
							continue;
						}

						// Distanzen vom aktuellen Feld zum "Hauptfeld" eintragen
						boxDistancesBackwards[UP   ][boxPosition][position] = distance;
						boxDistancesBackwards[DOWN ][boxPosition][position] = distance;
						boxDistancesBackwards[LEFT ][boxPosition][position] = distance;
						boxDistancesBackwards[RIGHT][boxPosition][position] = distance;

						// Prüfen, auf welche Nachbarfelder die Kiste vom aktuellen Feld
						// gezogen werden kann. Dieses Nachbarfeld kann dann mit
						// Distanz+1 Pulls erreicht werden.
						// Falls das aktuelle Feld und das vorige Feld beide keine Corralerzwingerfelder
						// sind, kann ein Zurückschieben verhindert werden, da in diesem Fall die alte
						// Stellung erneut erreicht würde nur mit einer höheren Distanz.
						// (Falls die Kiste auf dem Ursprungsfeld steht (positionKiste == position)
						// könnte ein Zurückschieben auch vermieden werden, da für die Ursprungsposition
						// der Spieler auf alle möglichen Nachbarpositionen gesetzt wurde und somit
						// schon jede Richtung untersucht wird. Diese zusätzliche Abrage kostet aber
						// mehr Zeit, als die Verhinderung des zusätzlichen Pushes in die Gegenrichtung
						// gewinnen würde ...)
						if (isCorralForcerSquare(playerPosition)) {
							// Das alte Feld war ein Corralerzwinger. Es muss auf jeden Fall ein
							// Zurückschieben geprüft werden. Deshalb Gegenrichtung auf irgend einen
							// ungültigen Wert setzen.
							oppositeDirectionOfPush = 1000;
						} else {
							// Sowohl das alte als auch das aktuelle Feld der Kiste sind NichtCorralerzwinger.
							// Der Spieler kann also bei beiden Feldern auf alle freien Seiten kommen.
							// Ein Zurückschieben ist deshalb unnötig.
							oppositeDirectionOfPush = playerPosition - boxPosition;
						}

						for (byte direction = 0; direction < DIRS_COUNT; direction++) {
							if (offset[direction] != oppositeDirectionOfPush
									&& isWall(boxPosition + offset[direction]) == false
									&& isWall(boxPosition - offset[direction]) == false) {
								boxPositionsQ   [next_in] = (short) (boxPosition + offset[direction]);
								playerPositionsQ[next_in] = boxPosition;
								distancesQ      [next_in] = (short) (distance + 1);
								next_in++;
							}
						}
					} else {
						// erreichbare Felder des Spielers ermitteln, wobei nur auf der
						// aktuellen Position eine Kiste im Weg steht.
						// (Die Kiste wird durch eine Mauer simuliert)
						setWall(boxPosition);
						playersReachableSquaresOnlyWalls.update(playerPosition);
						removeWall(boxPosition);

						// Bei Corralerzwingerfeldern reicht es nicht zu prüfen, ob die beiden für ein Verschieben
						// relevanten Felder frei sind (= keine Mauer), sondern es muss auch noch geprüft werden,
						// ob der Spieler tatsächlich auf die jeweilige Seite gelangen kann. Ein Verschieben macht
						// natürlich auch nur dann Sinn, wenn die Situation nicht bereits vorher mit einer
						// kleineren oder genau so hohen Distanz erzeugt wurde.
						for (byte direction = 0; direction < DIRS_COUNT; direction++) {
							if (playersReachableSquaresOnlyWalls.isSquareReachable(boxPosition + offset[direction]) == false
									|| boxDistancesBackwards[direction][boxPosition][position] <= distance) {
								continue;
							}

							// Es wurde ein Weg mit einer geringeren Distanz gefunden.
							boxDistancesBackwards[direction][boxPosition][position] = distance;

							// Prüfen, auf welche Nachbarfelder die Kiste vom aktuellen Feld gezogen werden kann.
							// Das direkte Nachbarfeld wurde ja bereits durch den obigen "if" auf frei (=erreichbar)
							// geprüft. Es muss nun geprüft werden, ob das zweite für ein Ziehen notwendige Feld
							// ebenfalls frei ist. (man könnte hier genau so gut auf board.isWall(...) == false
							// prüfen, das bewirkt das gleiche ...)
							if (isWall(boxPosition - offset[direction]) == false) {

								// - offset, damit oben boxDistancesBackwards[direction] stehen kann.
								// würde man + offset[direction] benutzen, müsste man die
								// Gegenrichtung in das Distanzenarray benutzen ...
								boxPositionsQ   [next_in] = (short) (boxPosition - offset[direction]);
								playerPositionsQ[next_in] = boxPosition;
								distancesQ      [next_in] = (short) (distance + 1);
								next_in++;
							}
						}
					}
				}
			}
		}

		/**
		 * Calculates the distance of the player to any square: ({@link Board#playerDistances}
		 * Frozen boxes are recognized as obstacles, provided the caller has converted
		 * the frozen boxes into walls, temporarily. All other boxes are ignored.
		 *
		 * @see_#updateBoxDistances(SearchDirection, boolean)
		 */
		public void calculatePlayerDistances() {

			// Here we use (short) to be memory efficient.  Also, such arrays may
			// eventually be saved in a buffer like "distancesBufferForwards".

			// Hold distance and player position from the respective stacks
			short distance;
			short playerPosition;

			// stack indices
			int next_in;
			int next_out;

			// a possible new player position
			short newPosition;

			// This is the array storing the distances between any two squares [fromSquare][toSquare]
			// We need a new instance, each time, since we later buffer it in a vector.
			playerDistances = new short[size][size];

			// Initialize the complete array(s) with "infinite" (a large value)
			Utilities.fillArray(playerDistances, UNREACHABLE);

			// Calculate distances for all squares
			for (int position = firstRelevantSquare; position < lastRelevantSquare; position++) {

				// ignore outer squares and walls ...
				if (isOuterSquareOrWall(position)) {
					continue;
				}

				// Now we calculate distances for the square "position".  For that we
				// fill their distances into the reachable squares, as we detect them.
				playerPositionsQ[0] = (short) position;
				distancesQ      [0] = 0;
				next_in  = 1;
				next_out = 0;
				while (next_out < next_in) {
					// dequeue next item: square and distance
					playerPosition = playerPositionsQ[next_out];
					distance       = distancesQ      [next_out];
					next_out++;

					// In case this square had already been reached with smaller distance
					// (than what we are now going to grant), then we immediately
					// continue with the next square from the queue.
					// Take care: We must not move this check into the loop below,
					// since the stack (at that time) may still contain other squares
					// which may reach our square with a smaller distance.
					if (playerDistances[playerPosition][position] <= distance) {
						continue;
					}
					playerDistances[playerPosition][position] = distance;

					// Now we enter all squares, which are directly reachable from the
					// current one, with their distance (one larger than the current one)
					for (int direction = 0; direction < DIRS_COUNT; direction++) {

						newPosition = (short) (playerPosition + offset[direction]);

						if (isWall(newPosition)) {
							continue;
						}

						// The new square is reachable with (distance+1) from the start square
						playerPositionsQ[next_in] = newPosition;
						distancesQ      [next_in] = (short) (distance + 1);
						next_in++;
					}
				}
			}
		}

		/**
		 * Fill the current freeze situation into the specified object
		 * (set a bit for each frozen box).
		 * Used for forward and backward search.
		 *
		 * @param freezeSet object to be filled
		 */
		private void fillCurrentFreeze(BitSet freezeSet) {
			freezeSet.clear();
			for (int boxNo = 0; boxNo < boxCount; boxNo++) {
				if (boxData.isBoxFrozen(boxNo)) {
					freezeSet.set(goalsNumbers[boxData.getBoxPosition(boxNo)]); // boxes can only freeze on goals (otherwise-> deadlock) but the same box number can be frozen on different positions
				}
			}
		}

		/**
		 * Recalculates box distances, taking into account frozen boxes
		 * (those which cannot be moved anymore, except by "undo").
		 * In this method we mainly handle the caching of the results of
		 * former calculations.  The calculations can become expensive,
		 * and the key for the calculation is more often already done,
		 * than not.  Hence a cache is important for efficiency.
		 *
		 * @param searchDirection the direction of the search (push or pull)
		 * @param onlyDistancesToGoals <code>true</code> calculates ony the distances to the goal positions,
		 * 		  <code>false</code> calculates the distances to all positions
		 *
		 * @see #calculateBoxDistancesForwards(int)
		 */
		public void updateBoxDistances(Settings.SearchDirection searchDirection, boolean onlyDistancesToGoals) {

			// Take care: Even if the current freeze state is identical to
			// the last freeze state from the same search direction,
			// we still cannot just "return" (do nothing). The box distances
			// would be correct, but the player distances might still
			// be from/for a former search in the opposite direction!
			// Hence, even for an optimal match we have to access the data
			// we find for our key.

			if (searchDirection == Settings.SearchDirection.FORWARD) {

				// Copy current freeze state into a bit vector
				fillCurrentFreeze(currentFreezeSituationForwards);

				// Check whether the current situation is known, already
				DistCacheElement distData = distancesForwCache.getV(currentFreezeSituationForwards);
				if (distData != null) {
					// Fetch the references from the found value
					boxDistancesForwards = distData.boxDistances;
					corralForcer         = distData.corralForcer;
					playerDistances      = distData.playerDistances;

					return;
				}

				/*
				 * Did not find the current freeze state in the buffer.
				 * Hence we have to calculate it.
				 */
				// Temporarily place a wall for all frozen boxes.
				for (int boxNo = 0; boxNo < boxCount; boxNo++) {
					if (boxData.isBoxFrozen(boxNo)) {
						setWall(boxData.getBoxPosition(boxNo));
					}
				}

				/**
				 * Update the distances.
				 */
				// Array, das die Kistendistanzen aufnimmt. Dieses Array muss bei jedem Aufruf
				// neu angelegt werden, da es später in einem Vector gepuffert wird.
				boxDistancesForwards = new short[DIRS_COUNT][size][size];

				// Alle Kistendistanzen mit "unendlich" initialisieren
				Utilities.fillArray(boxDistancesForwards, UNREACHABLE);

				// Spielerdistanzen und Kistendistanzen neu berechnen (Corralerzwinger werden
				// automatisch bei der Berechnung der neuen Kistendistanzen berechnet)
				calculatePlayerDistances();
				identifyCorralForcerSquares();      // for better performance the corral forcer squares are identified

				// Calculate the distance from every position to every other position.
				for (int position = firstRelevantSquare; position < lastRelevantSquare; position++) {
					if(isGoal(position) || onlyDistancesToGoals == false) {
						calculateBoxDistancesForwards(position);
					}
				}

				// Remove the temporary walls
				for (int boxNo = 0; boxNo < boxCount; boxNo++) {
					if (boxData.isBoxFrozen(boxNo)) {
						removeWall(boxData.getBoxPosition(boxNo));
					}
				}

				if(distancesForwCache.size() > 3)
					distancesForwCache.clear(); // avoid too much RAM usage
				
				// NB: We must clone the key we enter to the cache, since that
				// object is reused for multiple computations, while the
				// associated data is always freshly allocated,
				// and directly entered to the cache.
				distancesForwCache.add(
						(BitSet) currentFreezeSituationForwards.clone(),
						new DistCacheElement( boxDistancesForwards,
								              corralForcer,
								              playerDistances      )    );

				return;
			}

			/*
			 * Search direction is "backwards"
			 */

			// Currently, no freeze conditions are recognized for backwards search.
			// Hence, in backwards search we never really have frozen boxes.
			// But the coding for them is already there.
			// TODO: recognize frozen boxes for backwards search
			// IMPORTANT: this means that frotzen boxes from the forward search
			// may still be on the board! Hence, we have to set all boxes "not frozen"
			// for the backward search.
			boxData.setAllBoxesNotFrozen();

			// Fill a BitSet with the current freeze situation...
			fillCurrentFreeze(currentFreezeSituationBackwards);

			// The solver can be started after some pushes have been made. In this situation
			// the new box positions are used as backward goals. Hence, for the caching the
			// backward goal positions are also relevant!
			if(!Arrays.equals(goalPositionsBackwardsSearch, goalsPositionsBackwardsSearchUsedForCaching)) {
				goalsPositionsBackwardsSearchUsedForCaching = Arrays.copyOf(goalPositionsBackwardsSearch, goalPositionsBackwardsSearch.length);
				distancesBackCache.clear();
			}

			// Check whether the current situation is already known.
			DistCacheElement distData = distancesBackCache.getV(currentFreezeSituationBackwards);
			if (distData != null) {
				// Fetch the references from the found value
				boxDistancesBackwards = distData.boxDistances;
				corralForcer          = distData.corralForcer;
				playerDistances       = distData.playerDistances;

				return;
			}

			/*
			 * Die aktuelle Blockersituation war nicht im Puffer vorhanden. Sie muss deshalb neu
			 * berechnet werden.
			 */
			// Auf alle Positionen, auf denen eine geblockte Kiste steht eine Mauer setzen.
			for (int boxNo = 0; boxNo < boxCount; boxNo++) {
				if (boxData.isBoxFrozen(boxNo)) {
					setWall(boxData.getBoxPosition(boxNo));
				}
			}

			// Spielerdistanzen und Kistendistanzen neu berechnen (Corralerzwinger werden
			// automatisch bei der Berechnung der neuen Kistendistanzen berechnet)
			calculatePlayerDistances();
			calculateBoxDistancesBackwards();

			// Die zusätzlichen Mauern wieder entfernen
			for (int boxNo = 0; boxNo < boxCount; boxNo++) {
				if (boxData.isBoxFrozen(boxNo)) {
					removeWall(boxData.getBoxPosition(boxNo));
				}
			}

			if(distancesBackCache.size() > 3)
				distancesBackCache.clear(); // avoid too much RAM usage

			// NB: We must clone the key we enter to the cache, since that
			// object is reused for multiple computations, while the
			// associated data is always freshly allocated,
			// and directly entered to the cache.
			distancesBackCache.add(
					(BitSet) currentFreezeSituationBackwards.clone(),
					new DistCacheElement( boxDistancesBackwards,
							              corralForcer,
							              playerDistances       )    );
		}
	}

	/**
	 * This class holds an array, in which the player reachable squares are marked.
	 * For each call of the <code>update</code> method the currently reachable
	 * squares are calculated and marked.
	 * 这个类持有一个数组，用其标记玩家可到达的格子
	 * 每次调用update()方法，将计算并标记当前可访问的格子
	 */
	public class PlayersReachableSquares {

		// Hierdrin wird der Wert gespeichert, den ein durch den Spieler erreichbares
		// Feld kennzeichnt. Dieser Wert wird bei jedem Durchlauf hochgezählt, so dass
		// immer das gleiche Array zur Ermittlung der erreichbaren Felder genutzt werden
		// kann, ohne dieses Array vor jedem Durchlauf initialisieren zu müssen.
		// 仓管员每通过一个格子，此值递增
		// 不需要在每一次迭代时初始化这个数组
		int indicatorReachableSquare;  //指针

		// Array, in dem die bereits erreichten Felder gekennzeichnet werden.
		int[] playersReachableSquaresArray;

		// Stack, in dem die Positionen der noch zu analysierenden Felder gespeichert werden
		// analysieren = ist Feld erreichbar durch den Spieler oder nicht
		IntStack positionsToBeAnalyzed = new IntStack(size);

		/**
		 * Constructor, just allocating array of size {@link #size}.
		 */
		public PlayersReachableSquares() {
			playersReachableSquaresArray = new int[size];
		}

		/**
		 * Constructor for cloning.
		 * Returns a PlayersReachableSquare object containing the passed values.
		 *
		 * @param reachableSquares	the reachable squares array to be set
		 * @param indicatorValue	the indicator for reachable squares to be set
		 */
		public PlayersReachableSquares(int[] reachableSquares, int indicatorValue) {
			playersReachableSquaresArray = reachableSquares.clone();
			indicatorReachableSquare = indicatorValue;
		}

		/**
		 * Returns whether the player can reach the passed position.
		 *
		 * @param position the position to be tested for reachability
		 *
		 * @return <code>true</code> the position is reachable by the player
		 * 			<code>false</code> the position isn't reachable by the player
		 */
		 public boolean isSquareReachable(int position) {
			return playersReachableSquaresArray[position] == indicatorReachableSquare;
		}

		/**
		 * Returns a clone of the current object.
		 *
		 * @return a PlayersReachableSquares object identical to this object
		 */
		 public PlayersReachableSquares getClone() {
			return new PlayersReachableSquares(playersReachableSquaresArray, indicatorReachableSquare);
		}


		/**
		 * Updates the reachable squares of the player assuming the player at the passed position.
		 *
		 * @param xPlayerPosition the x coordinate of the player position
		 * @param yPlayerPosition the y coordinate of the player position
		 */
		public void update(int xPlayerPosition, int yPlayerPosition) {
			update(xPlayerPosition + width * yPlayerPosition);
		}

		/**
		 * Updates the reachable squares of the player.
		 * <p>
		 * The squares then can be tested for reachability calling the method <code>isSquareReachable()</code>
		 */
		public void update() {
			update(playerPosition);
		}

		/**
		 * Updates the reachable squares of the player assuming the player at the passed position.
		 *
		 * @param playerPosition the position of the player
		 */
		public void update(int playerPosition) {

			// Bei jedem Aufruf einen neuen Wert als Indikator für ein erreichbares Feld
			// verwenden, so dass das Array nicht immer vorgelöscht werden muss!
			indicatorReachableSquare++;

			positionsToBeAnalyzed.add(playerPosition);
			playersReachableSquaresArray[playerPosition] = indicatorReachableSquare;

			while (!positionsToBeAnalyzed.isEmpty()) {
				playerPosition = positionsToBeAnalyzed.remove();

				for( int direction = 0; direction<DIRS_COUNT ; ++direction) {
					int newPosition = playerPosition + offset[direction];
					if (isAccessible(newPosition) && playersReachableSquaresArray[newPosition] != indicatorReachableSquare) {
						positionsToBeAnalyzed.add(newPosition);
						playersReachableSquaresArray[newPosition] = indicatorReachableSquare;
					}
				}
			}
		}

		/**
		 * Die derzeit im Array als erreichbar gekennzeichneten Felder behalten ihre Kennzeichnung.
		 * Der Spieler wird auf die übergebenen Koordinaten gesetzt und dann werden zusätzlich
		 * zu den bereits gekennzeichneten Felder alle Felder gekennzeichnet, die nun ebenfalls
		 * erreichbar sind. Diese Methode geht davon aus, dass eine ERWEITERUNG der erreichbaren
		 * Felder vorliegt (also Kisten vom Feld genommen). D.h. sobald ein bereits als erreichbar
		 * gekennzeichnetes Feld erreicht wird, wird an dieser Stelle nicht weitergesucht!
		 *
		 * @param xPlayerPosition the x coordinate of the player position
		 * @param yPlayerPosition the y coordinate of the player position
		 */
		public void enlarge(int xPlayerPosition, int yPlayerPosition) {
			// Der Spieler startet quasi noch einmal von einer anderen Position aus. Der
			// Wert, der ein erreichtes Feld kennzeichnet muss also gleich bleiben.
			// Da er in der Methode automatisch erhöht wird, wird er hier um eins erniedrigt.
			indicatorReachableSquare--;
			update(xPlayerPosition, yPlayerPosition);
		}

		/**
		 * Die derzeit im Array als erreichbar gekennzeichneten Felder behalten ihre Kennzeichnung.
		 * Der Spieler wird auf die übergebenen Koordinaten gesetzt und dann werden zusätzlich
		 * zu den bereits gekennzeichneten Felder alle Felder gekennzeichnet, die nun ebenfalls
		 * erreichbar sind. Diese Methode geht davon aus, dass eine ERWEITERUNG der erreichbaren
		 * Felder vorliegt (also Kisten vom Feld genommen). D.h. sobald ein bereits als erreichbar
		 * gekennzeichnetes Feld erreicht wird, wird an dieser Stelle nicht weitergesucht!
		 *
		 * @param playerPosition the player position
		 */
		public void enlarge(int playerPosition) {
			// Der Spieler startet quasi noch einmal von einer anderen Position aus. Der
			// Wert, der ein erreichtes Feld kennzeichnet muss also gleich bleiben.
			// Da er in der Methode automatisch erhöht wird, wird er hier um eins erniedrigt.
			indicatorReachableSquare--;
			update(playerPosition);
		}

		/**
		 * Ermittelt die vom Spieler erreichbaren Felder und kennzeichnet sie im übergebenen Array.
		 * Normalerweise wird das Array in dieser Klasse genutzt (playersReachableSquares),
		 * aber da dieses globale Array bei jedem Aufruf überschrieben wird, ist es nur für
		 * eine gewisse Zeit aktuell. Sollen die erreichbaren Spielerfelder einer bestimmten
		 * Stellung für einen langen Zeitraum abfragbar sein, so kann diese Methode benutzt
		 * werden, die die erreichbaren Felder in dem übergebenen Array kennzeichnet.
		 * Als Indikatorwert wird einfach 1 angenommen. Somit darf diese Methode nicht
		 * noch einmal für das gleiche Array mit dieser Methodensignatur aufgerufen werden,
		 * da ja schon Felder mit 1 gekennzeichnet sein würden, was zu Fehlern führen würde!
		 *
		 * @param reachableSquares			Array, in dem alle erreichbaren Felder gekennzeichnet werden
		 */
		public void update(byte[] reachableSquares) {
			update(reachableSquares, (byte) 1, playerPosition);
		}

		/**
		 * Ermittelt die vom Spieler erreichbaren Felder und kennzeichnet sie im übergebenen Array.
		 * Normalerweise wird das Array in dieser Klasse genutzt (playersReachableSquaresArray),
		 * aber da dieses globale Array bei jedem Aufruf überschrieben wird, ist es nur für
		 * eine gewisse Zeit aktuell. Sollen die erreichbaren Spielerfelder einer bestimmten
		 * Stellung für einen langen Zeitraum abfragbar sein, so kann diese Methode benutzt
		 * werden, die die erreichbaren Felder in dem übergebenen Array kennzeichnet.
		 *
		 * @param reachableSquares			Array, in dem alle erreichbaren Felder gekennzeichnet werden
		 * @param reachableIndicatorValue	Wert, mit dem erreichbare Felder gekennzeichnet werden
		 */
		public void update(byte[] reachableSquares, byte reachableIndicatorValue) {
			update(reachableSquares, reachableIndicatorValue, playerPosition);
		}

		/**
		 * Ermittelt die vom Spieler erreichbaren Felder und kennzeichnet sie im übergebenen Array.
		 * Normalerweise wird das Array in dieser Klasse genutzt (playersReachableSquares),
		 * aber da dieses globale Array bei jedem Aufruf überschrieben wird, ist es nur für
		 * eine gewisse Zeit aktuell. Sollen die erreichbaren Spielerfelder einer bestimmten
		 * Stellung für einen langen Zeitraum abfragbar sein, so kann diese Methode benutzt
		 * werden, die die erreichbaren Felder in dem übergebenen Array kennzeichnet.
		 *
		 * @param reachableSquares			Array, in dem alle erreichbaren Felder gekennzeichnet werden
		 * @param reachableIndicatorValue	Wert, mit dem erreichbare Felder gekennzeichnet werden
		 * @param playerPosition		 	Spielerposition
		 */
		public void update(byte[] reachableSquares, byte reachableIndicatorValue, int playerPosition) {

			positionsToBeAnalyzed.add(playerPosition);
			reachableSquares[playerPosition] = reachableIndicatorValue;

			while (!positionsToBeAnalyzed.isEmpty()) {
				playerPosition = positionsToBeAnalyzed.remove();

				// Alle von der aktuellen Position erreichbaren Felder in den Stack aufnehmen,
				// falls sie vorher nicht bereits erreicht wurden.
				for( int direction = 0; direction<DIRS_COUNT ; ++direction) {
					int newPosition = playerPosition + offset[direction];
					if (isAccessible(newPosition) && reachableSquares[newPosition] != reachableIndicatorValue) {
						positionsToBeAnalyzed.add(newPosition);
						reachableSquares[newPosition] = reachableIndicatorValue;
					}
				}
			}
		}

		/**
		 * Die derzeit im Array als erreichbar gekennzeichneten Felder werden um die Felder
		 * reduziert, die der Spieler jetzt erreichen kann.
		 *
		 * @param reachableSquares			Array, in dem alle erreichbaren Felder gekennzeichnet sind
		 * @param playerPosition		 	Spielerposition
		 */
		public void reduce(byte[] reachableSquares, int playerPosition) {

			positionsToBeAnalyzed.add(playerPosition);
			reachableSquares[playerPosition] = -1;

			while (!positionsToBeAnalyzed.isEmpty()) {
				playerPosition = positionsToBeAnalyzed.remove();

				// Durch Setzen von -1 wird auf jeden Fall ein neuer Wert der unterschiedlich
				// von reachableIndicatorValue ist gesetzt, wodurch dieses Feld automatisch
				// als nicht mehr erreichbar gesetzt gilt, wenn mit reachableIndicatorValue
				// geprüft wird !!!)

				// Alle von der aktuellen Position erreichbaren Felder in den Stack aufnehmen,
				// falls sie vorher nicht bereits erreicht wurden.
				for (int dir = 0; dir < DIRS_COUNT; dir++) {
					int newPosition = playerPosition + offset[dir];
					if (isAccessible(newPosition) && reachableSquares[newPosition] != -1) {
						positionsToBeAnalyzed.add(newPosition);
						reachableSquares[newPosition] = -1;
					}
				}
			}
		}

		/**
		 * Die derzeit im globalen Array als erreichbar gekennzeichneten Felder werden um die Felder
		 * reduziert, die der Spieler jetzt erreichen kann.
		 *
		 * @param playerPosition		 	Spielerposition
		 */
		public void reduce(int playerPosition) {

			positionsToBeAnalyzed.add(playerPosition);
			playersReachableSquaresArray[playerPosition] = -1;

			while (!positionsToBeAnalyzed.isEmpty()) {
				playerPosition = positionsToBeAnalyzed.remove();

				// Durch Setzen von -1 wird auf jeden Fall ein neuer Wert der unterschiedlich
				// von reachableIndicatorValue ist gesetzt, wodurch dieses Feld automatisch
				// als nicht mehr erreichbar gesetzt gilt, wenn mit reachableIndicatorValue
				// geprüft wird !!!)

				// Alle von der aktuellen Position erreichbaren Felder in den Stack aufnehmen,
				// falls sie vorher nicht bereits erreicht wurden.
				for (int dir = 0; dir < DIRS_COUNT; dir++) {
					int newPosition = playerPosition + offset[dir];
					if (isAccessible(newPosition) && playersReachableSquaresArray[newPosition] != -1) {
						positionsToBeAnalyzed.add(newPosition);
						playersReachableSquaresArray[newPosition] = -1;
					}
				}
			}
		}

		/**
		 * Returns the position reachable of the player that is
		 * the most top left one.
		 * This is a normalization of the player position, used, where the
		 * exact player position is not relevant, but its reachable area is.
		 *
		 * @return the position top left
		 */
		public int getPlayerPositionTopLeft() {

			// Calculate squares reachable by the player.
			update();

			// This is just the square with the smallest index.
			for (int position = firstRelevantSquare; position < lastRelevantSquare; position++) {
				if (isSquareReachable(position)) {
					return position;
				}
			}

			// The player can't reach any square.
			return -1;
		}
	}

	/**
	 * Diese Klasse hält ein Array, in dem die durch den Spieler erreichbaren Felder gekennzeichnet
	 * werden. Bei jedem Aufruf der "aktualisiere"-Methode werden die aktuell erreichbaren Felder
	 * des Spielers gekennzeichnet.
	 * Besonderheit: Der Spieler kann bei der Ermittlung der erreichbaren Felder durch
	 * Kisten hindurchgehen!
	 */
	public class PlayersReachableSquaresOnlyWalls {

		// Hierdrin wird der Wert gespeichert, den ein durch den Spieler erreichbares
		// Feld kennzeichnt. Dieser Wert wird bei jedem Durchlauf hochgezählt, so dass
		// immer das gleiche Array zur Ermittlung der erreichbaren Felder genutzt werden
		// kann, ohne dieses Array vor jedem Durchlauf initialisieren zu müssen.
		int reachableSquareIndicatorOnlyWalls = 1;

		// Array, in dem die bereits erreichten Felder gekennzeichnet werden.
		int[] playersReachableSquaresOnlyWallsArray;

		// Stack, in dem die Positionen der noch zu analysierenden Felder gespeichert werden
		// analysieren = ist Feld erreichbar durch den Spieler oder nicht
		final IntStack positionsToBeAnalyzed;

		/**
		 * Constructor, just allocating arrays of size {@link Board#size}.
		 */
		public PlayersReachableSquaresOnlyWalls() {
			playersReachableSquaresOnlyWallsArray = new int[size];
			positionsToBeAnalyzed = new IntStack(size);
		}

		/**
		 * Constructor for cloning.
		 *
		 * @param reachableSquares	the array of reachable squares to be set
		 * @param reachableIndicatorValue	the value indicating a square to be reachable
		 */
		public PlayersReachableSquaresOnlyWalls(int[] reachableSquares,
				int reachableIndicatorValue) {
			playersReachableSquaresOnlyWallsArray = reachableSquares.clone();
			reachableSquareIndicatorOnlyWalls = reachableIndicatorValue;
			positionsToBeAnalyzed = new IntStack(size); // needn't be cloned
		}

		/**
		 * Returns a (deep) clone of this object.
		 *
		 * @return a (deep) clone of this object
		 */
		public PlayersReachableSquaresOnlyWalls getClone() {
			return new PlayersReachableSquaresOnlyWalls(playersReachableSquaresOnlyWallsArray, reachableSquareIndicatorOnlyWalls);
		}

		/**
		 * Returns whether a specific Square is reachable by the player.
		 *
		 * @param position Position which is checked for being reachable by the player
		 * @return true = Square is reachable; false = Square is not reachable
		 */
		public boolean isSquareReachable(int position) {
			return playersReachableSquaresOnlyWallsArray[position] == reachableSquareIndicatorOnlyWalls;
		}

		/**
		 * Returns if a specific Square is reachable by the player.
		 *
		 * @param x xPosition of square which is checked for being reachable by the player
		 * @param y yPosition of square which is checked for being reachable by the player
		 * @return true = Square is reachable; false = Square is not reachable
		 */
		public boolean isSquareReachable(int x, int y) {
			return playersReachableSquaresOnlyWallsArray[x + width * y] == reachableSquareIndicatorOnlyWalls;
		}

		/**
		 * Identifies the reachable squares of the player regarding only walls as obstacles.
		 */
		public void update() {
			update(playerPosition);
		}

		/**
		 * Identifies the reachable squares of the player regarding only walls as obstacles.
		 * The player is set to the passed position to itentify the reachable squares.
		 *
		 * @param xPlayerPosition the x coordinate of the player position
		 * @param yPlayerPosition the y coordinate of the player position
		 */
		public void update(int xPlayerPosition, int yPlayerPosition) {
			update(xPlayerPosition + width * yPlayerPosition);
		}

		/**
		 * Identifies the reachable squares of the player regarding only walls
		 * as obstacles. The player is considered to be at the passed position
		 * to identify the reachable squares.
		 *
		 * @param playerPosition the position of the player
		 */
		public void update(int playerPosition) {

			// Bei jedem Aufruf einen neuen Wert als Indikator für ein erreichbares Feld
			// verwenden, so dass das Array nicht immer vorgelöscht werden muss!
			reachableSquareIndicatorOnlyWalls++;

			positionsToBeAnalyzed.add(playerPosition);
			playersReachableSquaresOnlyWallsArray[playerPosition] = reachableSquareIndicatorOnlyWalls;

			while (!positionsToBeAnalyzed.isEmpty()) {
				playerPosition = positionsToBeAnalyzed.remove();

				// Alle von der aktuellen Position erreichbaren Felder in den Stack aufnehmen,
				// falls sie vorher nicht bereits erreicht wurden.
				for (int directionOffset : offset) {
					int newPosition = playerPosition + directionOffset;
					if (isWall(newPosition) == false && playersReachableSquaresOnlyWallsArray[newPosition] != reachableSquareIndicatorOnlyWalls) {
						positionsToBeAnalyzed.add(newPosition);
						playersReachableSquaresOnlyWallsArray[newPosition] = reachableSquareIndicatorOnlyWalls;
					}
				}
			}
		}

		/**
		 * Die derzeit im Array als erreichbar gekennzeichneten Felder behalten ihre Kennzeichnung.
		 * Der Spieler wird auf die übergebenen Koordinaten gesetzt und dann werden zusätzlich
		 * zu den bereits gekennzeichneten Felder alle Felder gekennzeichnet, die nun ebenfalls
		 * erreichbar sind. Sobald ein bereits als erreichbar gekennzeichnetes Feld erreicht wird,
		 * wird an dieser Stelle nicht weitergesucht. Der Spieler muss für diese Methode also
		 * in verschiedene, durch Mauern vollständig eingegrenzte Bereiche gesetzt werden und dann
		 * jeweils diese Methode aufgerufen werden.
		 *
		 * @param playerPosition the player position
		 */
		public void enlarge(int playerPosition) {
			// Der Spieler startet quasi noch einmal von einer anderen Position aus. Der
			// Wert, der ein erreichtes Feld kennzeichnet muss also gleich bleiben.
			// Da er in der Methode automatisch erhöht wird, wird er hier um eins erniedrigt.
			reachableSquareIndicatorOnlyWalls--;
			update(playerPosition);
		}
	}

	/**
	 * Diese Klasse hält ein Array, in dem die durch den Spieler erreichbaren Felder gekennzeichnet
	 * 	 * werden. Bei jedem Aufruf der "aktualisiere"-Methode werden die aktuell erreichbaren Felder
	 * 	 * des Spielers gekennzeichnet. Im Gegensatz zur Klasse "ErreichbareFelderSpieler" werden nicht
	 * 	 * nur die erreichbaren Felder ermittelt, sondern auch die Distanz des Spielers zu jedem Feld
	 * 	 * ermittelt.
	 */
	public class PlayersReachableSquaresMoves {

		// Array, in dem die bereits erreichten Felder gekennzeichnet werden.
		short[] playersReachableSquaresMoves;

		// Mit diesem Array wird das obige Array bei jedem Aufruf wieder initialisiert.
		// (Falls diese Klasse öfter instanziiert wird sollte dieses Array in die Spielfeldklasse
		// verschoben und static gesetzt werden)
		short[] initializationArray;

		// Stack, in dem die Positionen der noch zu analysierenden Felder gespeichert werden
		// analysieren = ist Feld erreichbar durch den Spieler oder nicht
		int[] positionsToBeAnalyzed = new int[size];

		/**
		 * Konstruktor
		 */
		public PlayersReachableSquaresMoves() {
			playersReachableSquaresMoves = new short[size];
			initializationArray = new short[size];
			Arrays.fill(initializationArray, UNREACHABLE);
		}

		/**
		 * Gibt zurück, ob ein Feld durch den Spieler erreichbar ist.
		 *
		 * @param position  Position, die auf Erreichbarkeit geprüft wird
		 * @return			true = Feld ist erreichbar; false = Feld ist nicht erreichbar
		 */
		public boolean isSquareReachable(int position) {
			return playersReachableSquaresMoves[position] != UNREACHABLE;
		}

		/**
		 * Gibt die Distanz zur übergebenen Position zurück.
		 *
		 * @param position	Position, zu der die Distanz des Spielers zurückgegeben wird.
		 * @return			Distanz des Spielers zur übergebenen Position
		 */
		public short getDistance(int position) {
			return playersReachableSquaresMoves[position];
		}

		/**
		 * Ermittelt die vom Spieler erreichbaren Felder und kennzeichnet sie im entsprechenden Array.
		 */
		public void update() {
			update(playerPosition);
		}

		/**
		 * Ermittelt die Distanz des Spielers zu jedem erreichbaren Feld des Spielers.
		 *
		 * @param playerPosition Position, auf der der Spieler zu Beginn steht.
		 */
		public void update(int playerPosition) {

			// Index auf den höchsten Index der Queue und Index auf den als nächstes zu verarbeitenden
			// Eintrag.
			int highestIndex = 0;
			int currentIndex = 0;
			int newPosition  = 0;

			// Distanz eines Feldes
			short newDistance = 0;

			// Array mit einer "unendlichen" Distanz vorbelegen.
			System.arraycopy(initializationArray, 0, playersReachableSquaresMoves, 0, size);

			// Die aktuelle Spielerposition als Ausgangsfeld nehmen. Es kann mit 0 Moves erreicht werden.
			positionsToBeAnalyzed[0] = playerPosition;
			playersReachableSquaresMoves[playerPosition] = 0;

			while (currentIndex <= highestIndex) {
				playerPosition = positionsToBeAnalyzed[currentIndex++];

				// Distanz der jetzigen Spielerposition zur ursprünglichen Position + 1 =
				// Distanz für die umliegenden Felder
				newDistance = (short) (playersReachableSquaresMoves[playerPosition] + 1);

				// Alle von der aktuellen Position erreichbaren Felder als neue Ausgangsbasis aufnehmen,
				// falls sie vorher nicht bereits erreicht wurden.
				for (int directionOffset : offset) {
					newPosition = playerPosition + directionOffset;
					if (isAccessible(newPosition) && playersReachableSquaresMoves[newPosition] == UNREACHABLE) {
						positionsToBeAnalyzed[++highestIndex] = newPosition;
						playersReachableSquaresMoves[newPosition] = newDistance;
					}
				}
			}
		}
	}

	/**
	 * This class identifies and marks the reachable squares of a box.
	 * The current player position and the positions of the other boxes are considered when
	 * identifying these squares.
	 * 这个类识别并标记一个箱子的可达位置。当识别这些位置时，将考虑当前玩家的位置和其他箱子的位置。
	 */
	public class BoxReachableSquares {

		// Indicator for a reachable square.可到达位置的指示器。
		private int indicatorReachableSquare = 1;

		// Array, where all reachable squares are marked with the indicator value.数组，其中所有可到达的方块都用指示符值标记。
		private int[] boxReachableSquaresArray;

		// Queue holding the positions which still have to be analyzed.保持仍然需要分析的位置的队列。
		private final IntStack positionsStack;

		// Array holding the information which square has already been reached from which direction.数组，保存已经从哪个方向到达哪个位置的信息
		private int[][] alreadyReachedSquares;

		/** Frozen boxes deadlock detection.（阻滞型死锁） */
		private final FreezeDeadlockDetection freezeDeadlockDetection;

		/** Deadlockdetection for closed diaginal deadlocks.（对角型死锁） */
		private final ClosedDiagonalDeadlock closedDiagonalDeadlockDetection;

		/**
		 * Constructor.构造函数
		 */
		public BoxReachableSquares() {
			boxReachableSquaresArray		= new int[size];
			positionsStack          		= new IntStack(4 * size);
			alreadyReachedSquares    		= new int[size][DIRS_COUNT];
	 		freezeDeadlockDetection  		= new FreezeDeadlockDetection(Board.this);
	 		closedDiagonalDeadlockDetection = new ClosedDiagonalDeadlock(Board.this);
		}

		/**
		 * Returns whether the given position has been marked as reachable.
		 * 返回给定位置是否已标记为可到达。
		 * @param position  Position to be checked to be reachable.
		 * @return	<code>true</code> if position is reachable,
		 *         <code>false</code> if position isn't reachable
		 */
		public boolean isSquareReachable(int position) {
			return boxReachableSquaresArray[position] == indicatorReachableSquare;
		}

		/**
		 * Identifies and marks the reachable squares of a box located at the passed position.
		 * The current player position and the positions of the other boxes are considered when
		 * identifying these squares.
		 * 识别和标记位于通过位置的盒子的可到达的方块。当识别这些方块时，要考虑当前玩家的位置和其他盒子的位置。
		 * Simple deadlocks and freeze deadlocks are taken account of, too.
		 * （简单死锁和不可再推死锁将被考虑在内）
		 *
		 * @param boxPosition the  box position
		 * @param markCurrentPosition specifies whether the current position has also to be marked
		 */
		public void markReachableSquares(int boxPosition, boolean markCurrentPosition) {

			// Backup the current positions
			int playerPositionBackup = playerPosition;
			int boxPositionBackup = boxPosition;

			// Timestamp when this method has to stop reducing the reachable squares
			// because of deadlocks. (200 milli seconds after this method has started)
			long timestampWhenToStop = System.currentTimeMillis() + 200;

			// Increase indicator every time this method is called for avoiding having to erase
			// the array every time.
			indicatorReachableSquare++;

			// Push the current positions to the stack.
			positionsStack.add(playerPosition);
			positionsStack.add(boxPosition);

			// Remove the box from the board, because it is set within the while-loop.
			removeBox(boxPosition);

			// Loop until no more positions can be reached
			while (!positionsStack.isEmpty()) {
				boxPosition    = positionsStack.remove();
				playerPosition = positionsStack.remove();

				// Set the board position that was saved in the stack and
				// update the array holding the reachable squares of the player.
				setBox(boxPosition);
				playersReachableSquares.update();

				// Push the box to every direction possible. If the the box has never been pushed
				// to the new position with this direction before the situation is added to the stack.
				for (int direction = 0; direction < DIRS_COUNT; direction++) {
					int newBoxPosition = boxPosition + offset[direction];
					if (isAccessible(newBoxPosition) && playersReachableSquares.isSquareReachable(boxPosition - offset[direction])
							&& alreadyReachedSquares[newBoxPosition][direction] != indicatorReachableSquare) {

						// Skip simple deadlocks if simple deadlocks are to be detected.
						if (isSimpleDeadlockSquare(newBoxPosition)) {
							continue;
						}

						// Mark the square as reachable for the box and save the status that it has
						// been reached with the current direction. Then add the new position to the stack.
						alreadyReachedSquares[newBoxPosition][direction] = indicatorReachableSquare;
						boxReachableSquaresArray[newBoxPosition] = indicatorReachableSquare;
						positionsStack.add(boxPosition);
						positionsStack.add(newBoxPosition);
					}
				}
				removeBox(boxPosition);
			}

			// Mark the current position of the box as reachable as requested.
			boxReachableSquaresArray[boxPositionBackup] = (markCurrentPosition ? indicatorReachableSquare : indicatorReachableSquare - 1);

			// Remove as many squares creating a freeze deadlock as the timelimit allows if the freeze test is enabled.
			for (int position = firstRelevantSquare; position < lastRelevantSquare && System.currentTimeMillis() < timestampWhenToStop; position++) {

				// Skip positions that are already marked as not reachable
				if (boxReachableSquaresArray[position] != indicatorReachableSquare) {
					continue;
				}

				// Set the box to the relevant position. The freeze detection takes the
				// player position into account. At this moment we don't know where the player
				// would be after having pushed the box so we set him at the same position as the box
				// in order to be sure he has access to every area.
				setBox(position);
				setPlayerPosition(position);
				if (freezeDeadlockDetection.isDeadlock(position, false)) {
					boxReachableSquaresArray[position] = 0;
				}
				removeBox(position);
			}

			// Remove as many squares creating a closed diagonal deadlock as the timelimit allows.
			for (int position = firstRelevantSquare; position < lastRelevantSquare && System.currentTimeMillis() < timestampWhenToStop; position++) {

				// Skip positions that are already marked as not reachable
				if (boxReachableSquaresArray[position] != indicatorReachableSquare) {
					continue;
				}

				setBox(position);
				if(closedDiagonalDeadlockDetection.isDeadlock(position) == true) {
					boxReachableSquaresArray[position] = 0;
				}
				removeBox(position);
			}

			// Reset the original board position.
			playerPosition = playerPositionBackup;
			setBox(boxPositionBackup);
		}

		/**
		 * Unmarks all reachable squares.
		 * After this method is called <code>isSquareReachable()</code> will return <code>false</code>
		 * for every position.
		 */
		public void unmarkReachableSquares() {
			indicatorReachableSquare++;
		}
	}

	/**
	 * This class identifies and marks the reachable squares of a box.
	 * The current player position but NOT the other boxes are considered
	 * when identifying these squares.
	 * 这个类识别并标记一个箱子的可达格子。
	 * 当识别这些格子时，将考虑当前玩家的位置，但不考虑其他箱子。
	 */
	public class BoxReachableSquaresOnlyWalls {

		/** Indicator for a reachable square. */
		private int indicatorReachableSquare = 1;

		/** Array, where all reachable squares are marked with the
		 *  indicator value.
		 */
		private int[] boxReachableSquaresArray;

		/** Queue holding the positions which still have to be analyzed. */
		private int[] positionsQueue;

		/** Array holding the information which square has already been
		 *  reached from which direction
		 */
		private int[][] alreadyReachedSquares;

		/**
		 * Constructor
		 */
		public BoxReachableSquaresOnlyWalls() {
			boxReachableSquaresArray = new int[size];
			positionsQueue           = new int[DIRS_COUNT * size];
			alreadyReachedSquares    = new int[size][DIRS_COUNT];
		}

		/**
		 * Returns whether the given position has been marked as reachable.
		 *
		 * @param position  position to be checked to be reachable.
		 * @return	<code>true</code> position is reachable
		 *          <code>false</code> position isn't reachable
		 */
		public boolean isSquareReachable(int position) {
			return boxReachableSquaresArray[position] == indicatorReachableSquare;
		}

		/**
		 * Identifies and marks the reachable squares of a box located at the passed position.
		 * The current player position is considered when identifying these squares BUT NOT the other boxes.
		 * 标识并标记位于通过位置的箱子的可到达的格子。当识别这些格子而不是其他箱子时，将考虑当前玩家的位置。
		 * Simple deadlocks are taken account of, too.
		 * Simple deadlocks are taken account of, too.
		 * @param boxPosition the  box position
		 * @param markCurrentPosition specifies whether the current position has also to be marked
		 */
		public void markReachableSquares(int boxPosition, boolean markCurrentPosition) {

			// Backup the current positions
			int playerPositionBackup = playerPosition;
			int boxPositionBackup    = boxPosition;

			int topOfQueue = 0;
			int newBoxPosition = 0;

			// Increase indicator every time this method is called for avoiding having to erase
			// the array every time.
			indicatorReachableSquare++;

			// Add the current positions to the queue.
			positionsQueue[topOfQueue++] = playerPosition;
			positionsQueue[topOfQueue++] = boxPosition;

			// Loop until no more positions can be reached
			while (topOfQueue > 1) {

				boxPosition    = positionsQueue[--topOfQueue];
				playerPosition = positionsQueue[--topOfQueue];

				// Set the board position that was saved in the queue and update the array holding the reachable squares of the player.
				// A wall is set because boxes are ignored as obstacles.
				setWall(boxPosition);
				playersReachableSquaresOnlyWalls.update();

				// Push the box to every direction possible. If the the box has never been pushed
				// to the new position with this direction before the situation is added to the queue.
				for (int direction = 0; direction < DIRS_COUNT; direction++) {
					newBoxPosition = boxPosition + offset[direction];
					if (isWall(newBoxPosition) == false && playersReachableSquaresOnlyWalls.isSquareReachable(boxPosition - offset[direction])
							&& alreadyReachedSquares[newBoxPosition][direction] != indicatorReachableSquare) {

						// Skip simple deadlocks.
						if (isSimpleDeadlockSquare(newBoxPosition)) {
							continue;
						}

						// Mark the square as reachable for the box and save the status that it has
						// been reached with the current direction. Then add the new position to the queue.
						alreadyReachedSquares[newBoxPosition][direction] = indicatorReachableSquare;
						boxReachableSquaresArray[newBoxPosition] = indicatorReachableSquare;
						positionsQueue[topOfQueue++] = boxPosition;
						positionsQueue[topOfQueue++] = newBoxPosition;
					}
				}
				removeWall(boxPosition);
			}

			// Mark the current position of the box as reachable as requested.
			boxReachableSquaresArray[boxPositionBackup] = indicatorReachableSquare - (markCurrentPosition ? 0 : 1);

			// Reset the original board position.
			playerPosition = playerPositionBackup;
		}

		/**
		 * Unmarks all reachable squares.
		 * After this method is called <code>isSquareReachable()</code> will return <code>false</code>
		 * for every position.
		 */
		public void unmarkReachableSquares() {
			indicatorReachableSquare++;
		}
	}

	/**
	 * This class identifies and marks the reachable squares of a box when pulling a box.
	 * The current player position and the positions of the other boxes are considered when
	 * identifying these squares.
	 */
	public class BoxReachableSquaresBackwards {

		// Indicator for a reachable square.
		private int indicatorReachableSquare = 1;

		// Array, where all reachable squares are marked with the indicator value.
		private int[] boxReachableSquaresArray;

		// Stack holding the positions which still have to be analyzed.
		private final IntStack positionsStack;

		// Array holding the information which square has already been reached from which direction.
		private int[][] alreadyReachedSquares;

		/**
		 * Constructor
		 */
		public BoxReachableSquaresBackwards() {
			boxReachableSquaresArray = new int[size];
			positionsStack           = new IntStack(3 * size);
			alreadyReachedSquares    = new int[size][DIRS_COUNT];
		}

		/**
		 * Returns whether the given position has been marked as reachable.
		 *
		 * @param position  Position to be checked to be reachable.
		 * @return	<code>true</code> position is reachable
		 *         <code>false</code> position isn't reachable
		 */
		public boolean isSquareReachable(int position) {
			return boxReachableSquaresArray[position] == indicatorReachableSquare;
		}

		/**
		 * Identifies and marks the backwards reachable squares of a box
		 * located at the passed position.
		 * The current player position and the positions of the other boxes
		 * are considered when identifying these squares.
		 *
		 * @param boxPosition  the box position
		 * @param markCurrentPosition specifies whether the current position has also to be marked
		 */
		public void markReachableSquares(int boxPosition, boolean markCurrentPosition) {

			// Backup the current positions
			int playerPositionBackup = playerPosition;
			int boxPositionBackup = boxPosition;

			// Increase indicator every time this method is called for avoiding having to erase
			// the array every time.
			indicatorReachableSquare++;

			// Push the current positions to the stack.
			positionsStack.add(playerPosition);
			positionsStack.add(boxPosition);

			// Remove the box from the board, because it is set within the while-loop.
			removeBox(boxPosition);

			// Loop until no more positions can be reached
			while (!positionsStack.isEmpty()) {
				boxPosition    = positionsStack.remove();
				playerPosition = positionsStack.remove();

				// Set the board position that was saved in the queue and
				// update the array holding the reachable squares of the player.
				setBox(boxPosition);
				playersReachableSquares.update();

				// Pull the box to every direction possible. If the the box has never been pulled
				// to the new position from this direction before the situation is added to the queue.
				for (int direction = 0; direction < DIRS_COUNT; direction++) {
					int newBoxPosition = boxPosition + offset[direction];

					// Skip simple deadlocks.
					if (isSimpleDeadlockSquare(newBoxPosition)) {
						continue;
					}

					if (isAccessible(newBoxPosition)
							&& playersReachableSquares.isSquareReachable(newBoxPosition + offset[direction])
							&& alreadyReachedSquares[newBoxPosition][direction] != indicatorReachableSquare) {

						// Mark the square as reachable for the box and save the status that it has
						// been reached with the current direction. Then add the new position to the queue.
						alreadyReachedSquares[newBoxPosition][direction] = indicatorReachableSquare;
						boxReachableSquaresArray[newBoxPosition] = indicatorReachableSquare;
						positionsStack.add(newBoxPosition + offset[direction]);
						positionsStack.add(newBoxPosition);
					}
				}
				removeBox(boxPosition);
			}

			// Mark the current position of the box as reachable as requested.
			boxReachableSquaresArray[boxPositionBackup] = (markCurrentPosition ? indicatorReachableSquare : indicatorReachableSquare - 1);

			// Reset the original board position.
			playerPosition = playerPositionBackup;
			setBox(boxPositionBackup);
		}

		/**
		 * Unmarks all reachable squares.
		 * After this method is called <code>isSquareReachable()</code> will return <code>false</code>
		 * for every position.
		 */
		public void unmarkReachableSquares() {
			indicatorReachableSquare++;
		}
	}

	/**
	 * This class identifies and marks the reachable squares of a box when pulling a box.
	 * The current player position BUT NOT other boxes are considered when identifying these squares.
	 */
	public class BoxReachableSquaresBackwardsOnlyWalls {

		// Indicator for a reachable square.
		private int indicatorReachableSquare = 1;

		// Array, where all reachable squares are marked with the indicator value.
		private int[] boxReachableSquaresArray;

		// Queue holding the positions which still have to be analyzed.
		private int[] positionsQueue;

		// Array holding the information which square has already been reached from which direction.
		private int[][] alreadyReachedSquares;

		/**
		 * Constructor
		 */
		public BoxReachableSquaresBackwardsOnlyWalls() {
			boxReachableSquaresArray = new int[size];
			positionsQueue           = new int[DIRS_COUNT * size];
			alreadyReachedSquares    = new int[size][DIRS_COUNT];
		}

		/**
		 * Returns whether the given position has been marked as reachable.
		 *
		 * @param position  Position to be checked to be reachable.
		 * @return	<code>true</code> position is reachable
		 *          <code>false</code> position isn't reachable
		 */
		public boolean isSquareReachable(int position) {
			return boxReachableSquaresArray[position] == indicatorReachableSquare;
		}

		/**
		 * Identifies and marks the backwards reachable squares of a box located at the passed position.
		 * The current player position BUT NOT other boxes are considered when identifying these squares.
		 * 标识并标记位于通过位置的箱子的逆推可到达的格子。
		 * 当识别这些方块时，不会考虑其他方块。
		 * @param boxPosition  the box position
		 * @param markCurrentPosition specifies whether the current position has also to be marked
		 */
		public void markReachableSquares(int boxPosition, boolean markCurrentPosition) {

			// Backup the current positions
			int playerPositionBackup = playerPosition;
			int boxPositionBackup = boxPosition;

			int topOfQueue = 0;
			int newBoxPosition = 0;

			// Increase（增加） indicator（指针） every time this method is called
			// for avoiding（避免） having to erase（抹去） the array every time.
			indicatorReachableSquare++;

			// Push the current positions to the queue.
			positionsQueue[topOfQueue++] = playerPosition;
			positionsQueue[topOfQueue++] = boxPosition;

			// Loop until no more positions can be reached
			while (topOfQueue > 1) {

				boxPosition    = positionsQueue[--topOfQueue];
				playerPosition = positionsQueue[--topOfQueue];

				// Set the board position that was saved in the queue（队列） and
				// update the array holding（包含、容纳） the reachable squares of the player.
				// A wall is set instead of a box because boxes are ignored in this class.
				setWall(boxPosition);
				playersReachableSquaresOnlyWalls.update();

				// Pull（拉） the box to every direction possible（可能的）. If the the box has never been pulled
				// to the new position from this direction before the situation（情况、局面） is added（加入） to the queue（队列）.
				for (int direction = 0; direction < DIRS_COUNT; direction++) {
					newBoxPosition = boxPosition + offset[direction];

					// Skip simple deadlocks.
					if (isSimpleDeadlockSquare(newBoxPosition)) {
						continue;
					}

					if (isWall(newBoxPosition) == false
							&& playersReachableSquaresOnlyWalls.isSquareReachable(newBoxPosition + offset[direction])
							&& alreadyReachedSquares[newBoxPosition][direction] != indicatorReachableSquare) {

						// Mark the square as reachable for the box and save the status that it has
						// been reached with the current direction. Then add the new position to the queue.
						alreadyReachedSquares[newBoxPosition][direction] = indicatorReachableSquare;
						boxReachableSquaresArray[newBoxPosition] = indicatorReachableSquare;
						positionsQueue[topOfQueue++] = newBoxPosition + offset[direction];
						positionsQueue[topOfQueue++] = newBoxPosition;
					}
				}
				removeWall(boxPosition);
			}

			// Mark the current position of the box as reachable as requested（请求的）.
			boxReachableSquaresArray[boxPositionBackup] = (markCurrentPosition ? indicatorReachableSquare : indicatorReachableSquare - 1);

			// Reset the original（起初的） board position.
			playerPosition = playerPositionBackup;
		}

		/**
		 * Unmarks all reachable squares.
		 * After this method is called <code>isSquareReachable()</code>
		 * will return <code>false</code> for every position.
		 */
		public void unmarkReachableSquares() {
			indicatorReachableSquare++;
		}
	}

	protected class BadSquares {

		/**
		 * Diese Methode ermittelt alle SimpleDeadlockfelder bezüglich des Vorwärts-
		 * schiebens von Kisten. Dazu wird für jedes Feld geprüft, ob von ihm aus ein
		 * Zielfeld erreichbar ist. SimpleDeadlockfelder sind Felder, die ein Level
		 * unlösbar machen, wenn eine Kiste auf ihnen steht - unabhängig davon,
		 * auf welche Seite der Kiste der Spieler dabei gelangen kann.
		 */
		protected void identifySimpleDeadlockSquaresForwards() {

			// Zähler für die Zielfelder
			int goalNo;

			// Alle Felder überprüfen: Kann von dem Feld aus eine Kiste auf irgend ein
			// Zielfeld geschoben werden ? Falls nein => SimpleDeadlockfeld.
			for (int position = firstRelevantSquare; position < lastRelevantSquare; position++) {

				// Alle Außenfelder und Mauern können überspringen werden（墙或墙外）
				if (isOuterSquareOrWall(position)) {
					continue;
				}

				// Bei Corralerzwingerfeldern spielt die Spielerposition eine Rolle.
				if (isCorralForcerSquare(position)) {
					// Prüfen, ob von diesem Feld irgend ein Zielfeld erreichbar ist.
					// (Da es kein Corralerzwingerfeld ist, ist die Spielerposition egal.
					// Es wird OBEN angenommen.)
					endOfLoop:
					for (goalNo = 0; goalNo < goalsCount; goalNo++) {
						for (int direction = 0; direction < DIRS_COUNT; direction++) {
							if (distances.boxDistancesForwards[direction][position][goalsPositions[goalNo]] != UNREACHABLE) {
								break endOfLoop;
							}
						}
					}
				} else {
					// Prüfen, ob von diesem Feld irgend ein Zielfeld erreichbar ist.
					// (Da es kein Corralerzwingerfeld ist, ist die Spielerposition egal.
					// Es wird OBEN angenommen.)
					for (goalNo = 0; goalNo < goalsCount; goalNo++) {
						if (distances.boxDistancesForwards[UP][position][goalsPositions[goalNo]] != UNREACHABLE) {
							break;
						}
					}
				}

				// Falls kein Zielfeld erreichbar ist, handelt es sich um ein SimpleDeadlockfeld.
				simpleDeadlockSquareForwards[position] = (goalNo == goalsCount);
			}


			/**
			 * Dead corridor detection.
			 * Some squares are deadlock squares because pushing a box to them ends
			 * in a one way street. Example:<pre>
			 *     ####
			 * #####  #
			 * #DDDD  #    D = dead end squares which are deadlock squares
			 * #####$ #
			 *     #+ #
			 *     ####</pre>
			 */

			// Backup the player position.
//			int playerPositionBackup = playerPosition;
//
//			 Check every square for detecting dead corridors.
//			for (int position = firstRelevantSquare; position < lastRelevantSquare; position++) {
//				// Jump over squares that are outside the player accessible area and squares that don't split the level into several parts (corral forcer).
//				if(isOuterSquareOrWall(position) || isCorralForcer(position) == false)
//					continue;
//
//				// Determine the number of sides from which the player can push a box from that position to any goal.
//				for(int direction=0; direction<DIRS; direction++) {
//
//					// Caculate the player position next to the box for every direction.
//					playerPosition = position + offset[direction];
//
//					// Jump over walls.
//					if(isWall(playerPosition))
//						continue;
//
//
//					for(goalNo=0; goalNo<goalsCount; goalNo++) {
//
//					}
//				}
//			}
		}

		/**
		 * Diese Methode ermittelt alle SimpleDeadlockfelder bezüglich des Rückwärts-
		 * ziehens von Kisten. Dazu wird für jedes Zielfeld geprüft, ob von ihm aus ein
		 * Kistenfeld erreichbar ist. SimpleDeadlockfelder sind Felder, die ein Level
		 * unlösbar machen, wenn eine Kiste auf ihnen steht - unabhängig davon,
		 * auf welche Seite der Kiste der Spieler dabei gelangen kann.
		 * Achtung: Es muss sozusagen das Spielfeld invertiert werden: Alle Zielfelder
		 * erhalten eine Kiste und alle Felder mit einer Kiste werden zu Zielfeldern.
		 * Deswegen muss geprüft werden, von welchem Zielfeld aus eine Kiste ein aktuelles
		 * Kistenfeld erreichen könnte (durch Ziehen!)
		 */
		protected void identifySimpleDeadlockSquaresBackwards() {

			int boxNo = -1;

			// Alle Felder überprüfen: Kann von dem Feld aus eine Kiste auf irgend ein
			// aktuelles Kistenfeld gezogen werden ? Falls nein => SimpleDeadlockfeld.
			for (int position = firstRelevantSquare; position < lastRelevantSquare; position++) {

				// Alle Außenfelder und Mauern können überspringen werden
				if (isOuterSquareOrWall(position)) {
					continue;
				}

				// Bei Corralerzwingerfeldern spielt die Spielerposition eine Rolle.
				if (isCorralForcerSquare(position)) {
					// Prüfen, ob von diesem Feld irgend ein Kistenfeld erreichbar ist.
					// (Da es kein Corralerzwingerfeld ist, ist die Spielerposition egal.
					// Es wird OBEN angenommen.)
					endOfLoop:
					for (boxNo = 0; boxNo < boxCount; boxNo++) {
						for (int direction = 0; direction < DIRS_COUNT; direction++) {
							if (distances.boxDistancesBackwards[direction][position][boxData.getBoxPosition(boxNo)] != UNREACHABLE) {
								break endOfLoop;
							}
						}
					}
				} else {
					// Prüfen, ob von diesem Feld irgend ein Kistenfeld erreichbar ist.
					// (Da es kein Corralerzwingerfeld ist, ist die Spielerposition egal.
					// Es wird OBEN angenommen.)
					for (boxNo = 0; boxNo < boxCount; boxNo++) {
						if (distances.boxDistancesBackwards[UP][position][boxData.getBoxPosition(boxNo)] != UNREACHABLE) {
							break;
						}
					}
				}

				// Falls kein Zielfeld erreichbar ist, handelt es sich um ein SimpleDeadlockfeld.
				simpleDeadlockSquareBackwards[position] = (boxNo == boxCount);
			}
		}

		/**
		 * Detect <em>advanced simple deadlocks</em>.
		 * Advanced simple deadlocks are squares, which generate a bipartite
		 * deadlock, when a box is located on them -
		 * independently from the location of the other boxes.
		 * Example:<pre>
		 *   ######
		 *   #.$ @#
		 *   ##A$.#
		 *    #   #
		 *    #####</pre>
		 *
		 * The square marked "A" is an advanced simple deadlock square.
		 * <p>
		 * Our advanced simple deadlocks always occur with boxes which can
		 * reach just a single goal square.  We do not check any other cases
		 * for performance reasons (no real lower bound calculation).
		 * <p>
		 * The result of this computation is stuffed into the Board.
		 * @see Board#setAdvancedSimpleDeadlock(int)
		 */
		protected void identifyAdvancedSimpleDeadlockSquaresForwards() {

			// In order to have any effect at all, we need at least 2 boxes
			// For speed up see e.g. Microban III, 101.
			if (boxCount < 2) {
				return;
			}

			// Loop over all boxes.
			for (int boxNo = 0; boxNo < boxCount; boxNo++) {

				// Number of goals this box can reach.
				int reachableGoalsCount = 0;

				// Number of the reachable goal.
				int nrReachableGoal = 0;

				// Calculate the number of reachable goals of the box.计算箱子的可达目标的数量。
				for (int goalNo = 0; goalNo < goalsCount && reachableGoalsCount < 2; goalNo++) {
					if (distances.getBoxDistanceForwardsPlayerPositionIndependentNo(boxNo, goalNo) != UNREACHABLE) {
						nrReachableGoal = goalNo;
						reachableGoalsCount++;
					}
				}

				// If the box can reach more than just one goal
				// immediately continue with the next box.
				if (reachableGoalsCount > 1) {
					continue;
				}

				// Box position.
				final int boxPosition;

				// Whether "markReachableSquares" done for "boxPosition"
				boolean boxPositionMarked;

				// Get the position of the box which can only reach one goal.
				boxPosition       = boxData.getBoxPosition(boxNo);
				boxPositionMarked = false;

				// Check every square of the board whether a box from
				// this square can also only reach this goal.
				loopLabel:
				for (int position = firstRelevantSquare; position < lastRelevantSquare; position++) {

					// Jump over all known deadlock squares
					if (isOuterSquareOrWall(position) || !isAccessibleBox(position)) {
						continue;
					}

					// Check whether a box on this position can also only
					// reach this goal. If yes, two boxes share only one goal.
					// 检查这个位置上的盒子是否也只能达到这个目标点。如果是，两个盒子只有一个目标。
					for (int goalNo = 0; goalNo < goalsCount; goalNo++) {
						if (distances.getBoxDistanceForwardsPlayerPositionIndependent(position, goalsPositions[goalNo]) != UNREACHABLE) {
							if (goalNo != nrReachableGoal) {
								continue loopLabel;
							}
						}
					}

					// It's only an advanced simple deadlock，这只是一个高级的简单死锁
					// if the current box can't reach the position, too.如果当前箱子也无法到达该位置。
					if ( ! boxPositionMarked) {
						boxReachableSquaresOnlyWalls.markReachableSquares(boxPosition, true);
						boxPositionMarked = true;
					}
					if(boxReachableSquaresOnlyWalls.isSquareReachable(position) == false) {
						// A box on this square can only reach a goal
						// which is already reserved for another box
						// => deadlock square.
						setAdvancedSimpleDeadlock(position);
					}
				}
			}
		}
	}
}