package my.boxman.jsoko;

import my.boxman.myGameView;

// 这个类，用于检查“冻结”死锁
public final class FreezeLock {

	private myGameView board;
	private char[][] level;                                // 地图指针
	private int nWidth, nHeight;                           // 地图的宽、高
	private boolean isBoxOnNotGoal;                        // 是否遇到了不在目标位的箱子
	private int[] pt;
	IntStack intStack;

	public FreezeLock(myGameView board) {
		this.board = board;
		// 使用最大尺寸定义内存，减少死锁检查时，频繁的内存申请及释放（因为死锁检查太过频繁）
		pt       = new int[8];                     // “小四块”需要入列待检的箱子，最多 5 个
		intStack = new IntStack(8);          // 挨着“小四块”的没有构成“四块”的箱子，最多 6 个
	}

	public boolean isDeadlock(int boxRow, int boxCol) {

		level   = board.m_cArray;
		nHeight = level.length;
		nWidth  = level[0].length;

        // 第一步，先检查当前箱子构成“四块”死锁
        if (isBlock4(boxRow, boxCol)) {
            return true;
        }

        // 第二步，标记关卡中所有的“四块”位置，以便检查“Z型”死锁
		for (int i = 0; i < nHeight; i++) {
			for (int j = 0; j < nWidth; j++) {
				if (isBoxOrWall(i, j) && isBoxOrWall(i, j-1) && isBoxOrWall(i-1, j) && isBoxOrWall(i-1, j-1)) {
					if (isBox2(i, j)) board.m_Freeze[i][j] = 4;
					if (isBox2(i, j-1)) board.m_Freeze[i][j-1] = 4;
					if (isBox2(i-1, j)) board.m_Freeze[i-1][j] = 4;
					if (isBox2(i-1, j-1)) board.m_Freeze[i-1][j-1] = 4;
				}
				else board.m_Freeze[i][j] = 0;
			}
		}

		// 第三步，若当前箱子处于“四块”冻结，一定没有引发“四块”死锁
		// 那么，收集挨着此“四块”的箱子，并检查它们是否引发了“Z型”死锁
		if (board.m_Freeze[boxRow][boxCol] == 4) {
			intStack.clear();
			findAroundBoxs(intStack, boxRow, boxCol);  // 收集“四块”周围的箱子

			// 检查“四块”周围的箱子是否存在“Z型”死锁
			int pos;
			while (!intStack.isEmpty()) {
				pos = intStack.remove();
				if (isZLock(pos >>> 16, pos & 0x0000ffff)) return true;
			}
			return false;  // 此时，当前箱子没有引发死锁
		}

		// 第四步，当前箱子没有形成“四块”，那么检查其是否引发“Z型”死锁
		return isZLock(boxRow, boxCol);
	}

	// 是否引发了“四块”死锁
	private boolean isBlock4(int row, int col) {
		// 左上
		if (isBoxOrWall(row-1, col) && isBoxOrWall(row, col-1) && isBoxOrWall(row-1, col-1) &&
		   (isBox(row, col) || isBox(row-1, col) || isBox(row, col-1) || isBox(row-1, col-1))) {
			return true;
		}
		// 左下
		if (isBoxOrWall(row+1, col) && isBoxOrWall(row, col-1) && isBoxOrWall(row+1, col-1) &&
		   (isBox(row, col) || isBox(row+1, col) || isBox(row, col-1) || isBox(row+1, col-1))) {
			return true;
		}
		// 右上
		if (isBoxOrWall(row-1, col) && isBoxOrWall(row, col+1) && isBoxOrWall(row-1, col+1) &&
		   (isBox(row, col) || isBox(row-1, col) || isBox(row, col+1) || isBox(row-1, col+1))) {
			return true;
		}
		// 右下
		if (isBoxOrWall(row+1, col) && isBoxOrWall(row, col+1) && isBoxOrWall(row+1, col+1) &&
		   (isBox(row, col) || isBox(row+1, col) || isBox(row, col+1) || isBox(row+1, col+1))) {
			return true;
		}
		return false;
	}

	// 搜集挨着当前“四块”的箱子
	//四邻：左、右、上、下
	private final byte[] dr4 = {0, 0, -1, 1};
	private final byte[] dc4 = {-1, 1, 0, 0};
	private void findAroundBoxs(IntStack intStack, int boxRow, int boxCol) {

		//排查可达点的四邻（用循环取代递归）
		int i1, j1, i0, j0;
		int p = 0, tail = 0;
		board.m_Freeze[boxRow][boxCol] += 10;
		pt[0] = boxRow << 16 | boxCol;
		while (p <= tail) {
			while (p <= tail) {
				i0 = pt[p] >>> 16;
				j0 = pt[p] & 0x0000ffff;
				// 此时，board.m_Freeze[i0][j0] == 14，检查其邻居
				for (int k = 0; 4 > k; k++) {
					i1 = i0 + dr4[k];
					j1 = j0 + dc4[k];
					if (i1 < 0 || j1 < 0 || i1 >= nHeight || j1 >= nWidth || isDistanceOut(boxRow, boxCol, i1, j1, 2)) {  // 超出关联“范围”
						continue;
					} else if (isBox2(i1, j1)) {  // 仅检查范围内有关的箱子
//						System.out.println("==================== " + i1 + ", " + j1 + ": " + level[i1][j1] + " - " + board.m_Freeze[i1][j1]);
						if (isDistanceOut(boxRow, boxCol, i1, j1, 1)) {   // 当前“小四块”紧挨着的位置
							if (board.m_Freeze[i1][j1] == 0) {  // 此箱子未在任何“四块”之内，需要检查其是否“Z型”冻结死锁
								intStack.add(i1 << 16 | j1);
//								System.out.println("==================== " + intStack.size());
							}
						} else {  // 这里是当前“小四块”内的箱子
							if (board.m_Freeze[i1][j1] == 0) {  // 此箱子未构成“小四块”，也要检查其是否“Z型”冻结死锁
								intStack.add(i1 << 16 | j1);
//                              System.out.println("==================== " + intStack.size());
							} else if (board.m_Freeze[i1][j1] == 4) {  // 当前“小四块”内构成“四块”的箱子，因为未曾入列检查过，所以，入列待检
								if (i1 == boxRow || j1 == boxCol || isDistanceOut(board.m_nRow, board.m_nCol, i1, j1, 1)) {  // 挨着当前箱子的箱子或与人的距离大于 1 的箱子
									tail++;
									pt[tail] = i1 << 16 | j1;
									board.m_Freeze[i1][j1] += 10;
//								    System.out.println("==================== " + tail);
								}
							}
						}
					}
				}
				p++;
			}
		}
	}

	// 检查两个位置间的距离是否 > v
	private boolean isDistanceOut(int r1, int c1, int r2, int c2, int v) {
		if (r1 - r2 < -v || r1 - r2 > v || c1 - c2 < -v || c1 - c2 > v) {
			return true;
		} else {
			return false;
		}
	}

	// 是否引发了“Z型”死锁
	private final int[][] dA = {
			{ 0, -1, -1, 0 },  // 先左后上
			{ 0, -1,  1, 0 },  // 先左后下
			{ 0,  1, -1, 0 },  // 先右后上
			{ 0,  1,  1, 0 }   // 先右后下
	};
	private final int[][] dB = {
			{  1, 0, 0,  1 },  // 先下后右
			{ -1, 0, 0,  1 },  // 先上后右
			{  1, 0, 0, -1 },  // 先下后左
			{ -1, 0, 0, -1 }   // 先上后左
	};

	private boolean isZLock(int row, int col) {
		isBoxOnNotGoal = isBox(row, col);
		if (isZ(row, col, dA[0]) && isZ(row, col, dB[0]) && isBoxOnNotGoal) return true;  // 左上 - 下右
		isBoxOnNotGoal = isBox(row, col);
		if (isZ(row, col, dA[1]) && isZ(row, col, dB[1]) && isBoxOnNotGoal) return true;  // 左下 - 上右
		isBoxOnNotGoal = isBox(row, col);
		if (isZ(row, col, dA[2]) && isZ(row, col, dB[2]) && isBoxOnNotGoal) return true;  // 右上 - 下左
		isBoxOnNotGoal = isBox(row, col);
		if (isZ(row, col, dA[3]) && isZ(row, col, dB[3]) && isBoxOnNotGoal) return true;  // 右下 - 上左

		return false;
	}

	// 是否形成单方向上的“Z型”冻结，同时记录是否包含不在目标位的箱子
	private boolean isZ(int row, int col, int[] dDir) {
		int r = row, c = col;
		boolean flg = true;

		while (true) {
			if (flg) { r += dDir[0]; c += dDir[1]; }
			else     { r += dDir[2]; c += dDir[3]; }

			if (isWall(r, c)) return true;                                 // 遇到墙壁、“四块”、或界外
			if (isPass(r, c)) break;                                       // 遇到通道，没有冻结
			if (isBox(r, c) && !isBoxOnNotGoal) isBoxOnNotGoal = true;     // 遇到不在目标位的箱子
			flg = !flg;                                                    // 折转
		}
		return false;
	}

    // 是否不在目标位的箱子
    private boolean isBox(int row, int col) {
        if (row < 0 || col < 0 || row >= nHeight || col >= nWidth) return false;
        return level[row][col] == '$';
    }

    // 是否箱子
    private boolean isBox2(int row, int col) {
        if (row < 0 || col < 0 || row >= nHeight || col >= nWidth) return false;
        return level[row][col] == '$'|| level[row][col] == '*';
    }

    // 是否箱子或墙壁
    private boolean isBoxOrWall(int row, int col) {
        if (row < 0 || col < 0 || row >= nHeight || col >= nWidth ||
            level[row][col] == '#' || level[row][col] == '_' ||
            level[row][col] == '$' || level[row][col] == '*') return true;
        return false;
    }

    // 是否通道
    private boolean isPass(int row, int col) {
        if (row < 0 || col < 0 || row >= nHeight || col >= nWidth) return false;

        return  level[row][col] == '-' || level[row][col] == '.' ||
            	level[row][col] == '@' || level[row][col] == '+';
    }

    // 是否墙壁、墙外或界外
	private boolean isWall(int row, int col) {
		if (row < 0 || col < 0 || row >= nHeight || col >= nWidth ||
			board.m_Freeze[row][col] == 4 || board.m_Freeze[row][col] == 14 ||
            level[row][col] == '#' || level[row][col] == '_') {
			return true;
		}
		return false;
	}
}