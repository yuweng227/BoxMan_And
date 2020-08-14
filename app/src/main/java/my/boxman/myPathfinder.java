package my.boxman;

/*  特别感谢！！！
    下面的功能函数移植于杨超教授的 SokoPlayer.js
    public void manReachable( ... )  //计算仓管员的可达位置
	public String manTo( ... )  //仓管员寻径
	public boolean manTo2( ... )  //检查仓管员是否可从位置 1 到达位置 2
	public boolean manTo2b( ... )  //利用“割点”、“图块”检查仓管员是否可从位置 1 到达位置 2
	public void boxReachable( ... )  //计算箱子的可达位置
	public void boxTo( ... )  //箱子寻径，用 anian 老师提出的转弯累计值作为优先队列比较器的因子，使箱子能够尽量推直线，效果不错。
	public void FindBlock( ... )  //计算“割点”，标识“图块”
	public void Block( ... )  //标识“图块”
	public void CutVertex( ... )  //计算“割点”

	注意：
	1、该类在实例化时，接收地图尺寸作为其参数
 */

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

//探路单元，提供各种寻径功能
public class myPathfinder {

    //供外部使用的各标志数组：1、2为仓管员的可达点；3、4为箱子的可达点；5、6为穿越点
    public boolean[][] mark1, mark2, mark3, mark4, mark5, mark6, mark;  //mark 识别穿越可达用；

    char[][] tmpLevel, tmpLevel1, tmpLevel2;  //地图副本，割点和寻径使用时，会更改其数据
    int mMapRows, mMapCols;  //当前地图尺寸

    boolean[][][] mark0;
    boolean[][] cut;  //割点

    short[][] children, //记录当前节点的子树节点方向：1--左，2--右，4--上，8--下，用位运算操作
             parent, parent2; //记录当前节点的父节点方向：0--左，1--右，2--上，3--下

    int[][] depth_tag, low_tag;
    int depth; //DFS 深度
    int b_count;  //计数“块 ”，块序号从 -1递减标注
    bc_Node[][] block;
    int[] ptBlock;  //[h*w]，登记块使用，替代“队列”

    int[] pt, pt0;  //[h*w]，开集

    //四邻：左、右、上、下
    final byte[] dr4 = {0, 0, -1, 1};
    final byte[] dc4 = {-1, 1, 0, 0};
    final byte[] bt = {1, 3, 2, 4, 5, 7, 6, 8};  //对应动作：l u r d L U R D

    //构造函数，参数为地图尺寸
    public myPathfinder(int row, int col) {
        //地图尺寸
        mMapRows = row;
        mMapCols = col;

        //供外部使用的各标志数组：正推中网锁识别用；1、2为人可达；3、4为箱子可达；5、6为穿越点
        mark = new boolean[mMapRows][mMapCols];
        mark1 = new boolean[mMapRows][mMapCols];
        mark2 = new boolean[mMapRows][mMapCols];
        mark3 = new boolean[mMapRows][mMapCols];
        mark4 = new boolean[mMapRows][mMapCols];
        mark5 = new boolean[mMapRows][mMapCols];
        mark6 = new boolean[mMapRows][mMapCols];

        //正逆推地图副本，箱子寻径使用
        tmpLevel  = new char[mMapRows][mMapCols];
        tmpLevel1 = new char[mMapRows][mMapCols];
        tmpLevel2 = new char[mMapRows][mMapCols];

        //割点算法使用的各标志数组，仅内部使用
        depth_tag = new int[mMapRows][mMapCols];
        low_tag = new int[mMapRows][mMapCols];
        cut = new boolean[mMapRows][mMapCols];
        block = new bc_Node[mMapRows][mMapCols];
        mark0 = new boolean[mMapRows][mMapCols][4];  //箱子寻径时，记录新节点某方向是否已推过
        parent = new short[mMapRows][mMapCols];
        parent2 = new short[mMapRows][mMapCols];
        children = new short[mMapRows][mMapCols];
        ptBlock = new int[mMapRows * mMapCols];
        pt = new int[mMapRows * mMapCols];
        pt0 = new int[mMapRows * mMapCols];
    }

    //计算仓管员的可达位置
    //传参：flg -- 是否逆推，level -- 地图现场，m_nRow、m_nCol -- 仓管员坐标
    public void manReachable(boolean flg, char[][] level, int m_nRow, int m_nCol) {
        boolean[][] mark = flg ? mark2 : mark1, mark9 = flg ? mark6 : mark5;

        for (int i = 0; i < mMapRows; i++) {
            for (int j = 0; j < mMapCols; j++) {
                if (level[i][j] == '-' || level[i][j] == '.' || level[i][j] == '@' || level[i][j] == '+') tmpLevel[i][j] = '-';
                else if (level[i][j] == '*') tmpLevel[i][j] = '$';
                else tmpLevel[i][j] = level[i][j];

                mark[i][j] = false;
                mark9[i][j] = false;
            }
        }

        //排查可达点的四邻（用循环取代递归）
        int i1, i2,  i3, j1, j2, j3;
        int p = 0, tail = 0;

        mark[m_nRow][m_nCol] = true;
        pt[0] = m_nRow << 16 | m_nCol;
        while (p <= tail) {
            while (p <= tail) {
                for (int k = 0; 4 > k; k++) {
                    i1 = (pt[p] >>> 16) + dr4[k];
                    j1 = (pt[p] & 0x0000ffff) + dc4[k];
                    if (i1 < 0 || j1 < 0 || i1 >= mMapRows || j1 >= mMapCols) {  // 界外
                        continue;
                    } else if ('-' == tmpLevel[i1][j1] && !mark[i1][j1]) {
                        tail++;
                        pt[tail] = i1 << 16 | j1;   //新的足迹
                        mark[i1][j1] = true;
                    }
                }
                p++;
            }

            //检查穿越情况
            if (myMaps.m_Sets[17] == 1) {
                for (int i = 1; i < mMapRows - 1; i++) {
                    for (int j = 1; j < mMapCols - 1; j++) {
                        if ('-' == tmpLevel[i][j] && !mark[i][j]) {
                            for (int k = 0; 4 > k; k++) { //排查四向之双侧
                                deep_Thur = 0;
                                if (flg) {  //逆推
                                    i1 = i + 3 * dr4[k];
                                    j1 = j + 3 * dc4[k];
                                    i2 = i + 2 * dr4[k];
                                    j2 = j + 2 * dc4[k];
                                    i3 = i + dr4[k];
                                    j3 = j + dc4[k];
                                    if (i1 < 0 || j1 < 0 || i1 >= mMapRows || j1 >= mMapCols ||
                                            i2 < 0 || j2 < 0 || i2 >= mMapRows || j2 >= mMapCols ||
                                            i3 < 0 || j3 < 0 || i3 >= mMapRows || j3 >= mMapCols) {  // 界外
                                        continue;
                                    } else if ('$' == tmpLevel[i3][j3] && mark[i1][j1] && mark[i2][j2]) {
                                        tmpLevel[i3][j3] = '-';
                                        if (isChuanYue(true, tmpLevel, i2, j2, i1, j1, i3, j3, k)) {
                                            mark[i][j] = true;
                                            mark9[i3][j3] = true;
                                            tail++;
                                            pt[tail] = i << 16 | j;
                                            tmpLevel[i3][j3] = '$';
//                                            break;  //直接跳出，穿越点提示不全，实际走的路径，可能不是提示的穿越点
                                        }
                                        tmpLevel[i3][j3] = '$';
                                    }
                                } else {  //正推
                                    i1 = i + dr4[k];
                                    j1 = j + dc4[k];
                                    i2 = i - dr4[k];
                                    j2 = j - dc4[k];
                                    i3 = i - 2 * dr4[k];
                                    j3 = j - 2 * dc4[k];
                                    if (i1 < 0 || j1 < 0 || i1 >= mMapRows || j1 >= mMapCols ||
                                            i2 < 0 || j2 < 0 || i2 >= mMapRows || j2 >= mMapCols ||
                                            i3 < 0 || j3 < 0 || i3 >= mMapRows || j3 >= mMapCols) {  // 界外
                                        continue;
                                    } else if ('$' == tmpLevel[i2][j2] && '-' == tmpLevel[i1][j1] && mark[i3][j3]) {
                                        tmpLevel[i2][j2] = '-';
                                        if (isChuanYue(false, tmpLevel, i, j, i2, j2, i1, j1, k)) {
                                            mark[i][j] = true;
                                            mark9[i2][j2] = true;
                                            tail++;
                                            pt[tail] = i << 16 | j;
                                            tmpLevel[i2][j2] = '$';
//                                            break;  //直接跳出，穿越点提示不全，实际走的路径，可能不是提示的穿越点
                                        }
                                        tmpLevel[i2][j2] = '$';
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    //仓管员寻径，返回路径的 Byte 链表
    //参数：flg -- 是否逆推，level -- 地图现场，from_row、from_col -- 仓管员原位置，to_row、to_col -- 仓管员目的位置
    static int deep_Thur = 0;  //穿越前后的直推次数
    public LinkedList<Byte> manTo(boolean flg, char[][] level, int from_row, int from_col, int to_row, int to_col) {
        pt[0] = from_row << 16 | from_col;
        boolean[][] mark = mark1;  //已访问标记

        for (int i = 0; i < mMapRows; i++) {
            for (int j = 0; j < mMapCols; j++) {
                if (level[i][j] == '-' || level[i][j] == '.' || level[i][j] == '@' || level[i][j] == '+') tmpLevel[i][j] = '-';
                else if (level[i][j] == '*') tmpLevel[i][j] = '$';
                else tmpLevel[i][j] = level[i][j];

                mark[i][j] = false;
                parent[i][j] = -1;
            }
        }

        int i1, i2, i3, j1, j2, j3;
        boolean isFound = false;
        mark[from_row][from_col] = true;
        int p = 0, tail = 0;
        while (p <= tail) {  //先做四邻排查
            while (p <= tail) {
                for (int k = 0; 4 > k; k++) {
                    i1 = (pt[p] >>> 16) + dr4[k];
                    j1 = (pt[p] & 0x0000ffff) + dc4[k];
                    if (i1 < 0 || j1 < 0 || i1 >= mMapRows || j1 >= mMapCols) {  // 界外
                        continue;
                    } else if ('-' == tmpLevel[i1][j1] && !mark[i1][j1]) {
                        tail++;
                        pt[tail] = i1 << 16 | j1;   //新的足迹
                        mark[i1][j1] = true;
                        parent[i1][j1] = (short) k;  //父节点到此点的方向
                        if (to_row == i1 && to_col == j1) {  //到达目标
                            isFound = true;
                            break;
                        }
                    }
                }
                p++;
            }
            if (isFound) break;

            //四邻排查后，对不可达的特殊点，进行穿越排查
            if (myMaps.m_Sets[17] == 1) {
                for (int i = 1; i < mMapRows - 1; i++) {
                    for (int j = 1; j < mMapCols - 1; j++) {
                        if ('-' == tmpLevel[i][j] && !mark[i][j]) {
                            for (int k = 0; 4 > k; k++) {
                                deep_Thur = 1;
                                if (flg) {  //逆推
                                    i1 = i + 3 * dr4[k];
                                    j1 = j + 3 * dc4[k];
                                    i2 = i + 2 * dr4[k];
                                    j2 = j + 2 * dc4[k];
                                    i3 = i + dr4[k];
                                    j3 = j + dc4[k];
                                    if (i1 < 0 || j1 < 0 || i1 >= mMapRows || j1 >= mMapCols ||
                                            i2 < 0 || j2 < 0 || i2 >= mMapRows || j2 >= mMapCols ||
                                            i3 < 0 || j3 < 0 || i3 >= mMapRows || j3 >= mMapCols) {  // 界外
                                        continue;
                                    } else if ('$' == tmpLevel[i3][j3] && mark[i1][j1] && mark[i2][j2]) {
                                        tmpLevel[i3][j3] = '-';
                                        if (isChuanYue(true, tmpLevel, i2, j2, i1, j1, i3, j3, k)) {
                                            mark[i][j] = true;
                                            tail++;
                                            pt[tail] = i << 16 | j;
                                            parent[i][j] = ( short ) (10 * deep_Thur + k); //穿越走法（变通的方向）

                                            if (i == to_row && j == to_col) {  //到达目标
                                                isFound = true;
                                                tmpLevel[i3][j3] = '$';
                                                break;
                                            }
                                        }
                                        tmpLevel[i3][j3] = '$';
                                    }
                                } else {  //正推
                                    i1 = i + dr4[k];
                                    j1 = j + dc4[k];
                                    i2 = i - dr4[k];
                                    j2 = j - dc4[k];
                                    i3 = i - 2 * dr4[k];
                                    j3 = j - 2 * dc4[k];
                                    if (i1 < 0 || j1 < 0 || i1 >= mMapRows || j1 >= mMapCols ||
                                            i2 < 0 || j2 < 0 || i2 >= mMapRows || j2 >= mMapCols ||
                                            i3 < 0 || j3 < 0 || i3 >= mMapRows || j3 >= mMapCols) {  // 界外
                                        continue;
                                    } else if ('$' == tmpLevel[i2][j2] && '-' == tmpLevel[i1][j1] && mark[i3][j3]) {
                                        tmpLevel[i2][j2] = '-';
                                        if (isChuanYue(false, tmpLevel, i, j, i2, j2, i1, j1, k)) {
                                            mark[i][j] = true;
                                            tail++;
                                            pt[tail] = i << 16 | j;
                                            parent[i][j] = ( short ) (10 * deep_Thur + k); //穿越走法（变通的方向）
                                            if (i == to_row && j == to_col) {  //到达目标
                                                isFound = true;
                                                tmpLevel[i2][j2] = '$';
                                                break;
                                            }
                                        }
                                        tmpLevel[i2][j2] = '$';
                                    }
                                }
                            }
                            if (isFound) break;
                        }
                    }
                    if (isFound) break;
                }
            }
            if (isFound) break;
        }

        LinkedList<Byte> path_Link = new LinkedList<Byte>();
        LinkedList<Byte> path_Link2 = new LinkedList<Byte>();
        if (isFound) {  //拼接路径————从止点到起点（反向字符串，无路径时长度为 0）
            int t_er = to_row, t_ec = to_col, t1, t2;
            while (t_er != from_row || t_ec != from_col) {
                if (parent[t_er][t_ec] < 4) {
                    path_Link.offer(bt[parent[t_er][t_ec]]);
                    t1 = t_er - dr4[parent[t_er][t_ec]];
                    t2 = t_ec - dc4[parent[t_er][t_ec]];
                    t_er = t1;
                    t_ec = t2;
                } else {
                    if (flg) { //逆推
                        i1 = t_er + 3 * dr4[parent[t_er][t_ec] % 10];
                        j1 = t_ec + 3 * dc4[parent[t_er][t_ec] % 10];
                        i2 = t_er + 2 * dr4[parent[t_er][t_ec] % 10];
                        j2 = t_ec + 2 * dc4[parent[t_er][t_ec] % 10];
                        i3 = t_er + dr4[parent[t_er][t_ec] % 10];
                        j3 = t_ec + dc4[parent[t_er][t_ec] % 10];
                        tmpLevel[i3][j3] = '-';
                        getChuanYue(tmpLevel, i2, j2, i1, j1, i3, j3, (byte) (parent[t_er][t_ec] % 10), parent[t_er][t_ec] / 10 - 1, path_Link2);
                        tmpLevel[i3][j3] = '$';
                        while (!path_Link2.isEmpty()) {
                            path_Link.offer(path_Link2.removeFirst());
                        }
                    } else {
                        i1 = t_er + dr4[parent[t_er][t_ec] % 10];
                        j1 = t_ec + dc4[parent[t_er][t_ec] % 10];
                        i2 = t_er - dr4[parent[t_er][t_ec] % 10];
                        j2 = t_ec - dc4[parent[t_er][t_ec] % 10];
                        tmpLevel[i2][j2] = '-';
                        getChuanYue(tmpLevel, t_er, t_ec, i2, j2, i1, j1, (byte) (parent[t_er][t_ec] % 10), parent[t_er][t_ec] / 10 - 1, path_Link2);
                        tmpLevel[i2][j2] = '$';
                        while (!path_Link2.isEmpty()) {
                            path_Link.offer(path_Link2.removeFirst());
                        }
                    }
                    if (flg) {
                        t1 = t_er + 2 * dr4[parent[t_er][t_ec] % 10];
                        t2 = t_ec + 2 * dc4[parent[t_er][t_ec] % 10];
                    } else {
                        t1 = t_er - 2 * dr4[parent[t_er][t_ec] % 10];
                        t2 = t_ec - 2 * dc4[parent[t_er][t_ec] % 10];
                    }
                    t_er = t1;
                    t_ec = t2;
                }
            }
        }
        return path_Link;
    }

    // 检查 nRow1, nCol1 与 nRow2, nCol2 两点是否穿越可达, 点 nRow2, nCol2 是被穿越的箱子，点 nRow, nCol 是穿越时箱子的临时移动位置
    public boolean isChuanYue(boolean is_BK, char[][] level, int nRow, int nCol, int nRow1, int nCol1, int nRow2, int nCol2, int dir) {
        for (int i = 0; i < mMapRows; i++) {
            for (int j = 0; j < mMapCols; j++) {
                mark[i][j] = false;
            }
        }

        //排查可达点的四邻（用循环取代递归）
        int i1, j1;
        int p = 0, tail = 0;
        mark[nRow1][nCol1] = true;
        pt0[0] = nRow1 << 16 | nCol1;
        while (p <= tail) {
            for (int k = 0; 4 > k; k++) {
                i1 = (pt0[p] >>> 16) + dr4[k];
                j1 = (pt0[p] & 0x0000ffff) + dc4[k];
                if (i1 < 0 || j1 < 0 || i1 >= mMapRows || j1 >= mMapCols || i1 == nRow && j1 == nCol) {  // 界外，或遇到箱子临时位置
                    continue;
                } else if (i1 == nRow2 && j1 == nCol2) {  // 穿越可达
                    return true;
                } else if ('-' == level[i1][j1] && !mark[i1][j1]) {
                    tail++;
                    pt0[tail] = i1 << 16 | j1;   //新的足迹
                    mark[i1][j1] = true;
                }
            }
            p++;
        }

        if (is_BK) {
            i1 = nRow1 + dr4[dir];
            j1 = nCol1 + dc4[dir];
        } else {
            i1 = nRow2 + dr4[dir];
            j1 = nCol2 + dc4[dir];
        }
        if (i1 < 0 || j1 < 0 || i1 >= mMapRows || j1 >= mMapCols || level[i1][j1] != '-') {  // 界外
            return false;
        } else {
            deep_Thur++;
            if (is_BK) {
                return isChuanYue(is_BK, level, nRow1, nCol1, i1, j1, nRow, nCol, dir);  //再前进一步检查
            } else {
                return isChuanYue(is_BK, level, nRow2, nCol2, nRow, nCol, i1, j1, dir);  //再前进一步检查
            }
        }
    }

    // 取得 nRow1, nCol1 与 nRow2, nCol2 两点是否穿越路径, 点 nRow2, nCol2 是被穿越的箱子，点 nRow, nCol 是穿越时箱子的临时移动位置
    public void getChuanYue(char[][] level, int nRow, int nCol, int nRow1, int nCol1, int nRow2, int nCol2, byte dir, int num, LinkedList<Byte> path) {

        for (int i = 0; i < mMapRows; i++) {
            for (int j = 0; j < mMapCols; j++) {
                parent2[i][j] = -1;
                mark[i][j] = false;
            }
        }

        //排查可达点的四邻（用循环取代递归）
        int i1, j1;
        int p = 0, tail = 0;
        boolean isFound = false;

        //根据直推次数，调整计算位置
        nRow1 += dr4[dir] * num;
        nCol1 += dc4[dir] * num;
        nRow2 += dr4[dir] * num;
        nCol2 += dc4[dir] * num;
        nRow += dr4[dir] * num;
        nCol += dc4[dir] * num;

        mark[nRow1][nCol1] = true;
        pt0[0] = nRow1 << 16 | nCol1;
        while (p <= tail) {
            for (int k = 0; 4 > k; k++) {
                i1 = (pt0[p] >>> 16) + dr4[k];
                j1 = (pt0[p] & 0x0000ffff) + dc4[k];
                if (i1 < 0 || j1 < 0 || i1 >= mMapRows || j1 >= mMapCols || i1 == nRow && j1 == nCol) {  // 界外，或遇到箱子临时位置
                    continue;
                } else if (nRow2 == i1 && nCol2 == j1) {  //到达目标
                    tail++;
                    pt0[tail] = i1 << 16 | j1;   //新的足迹
                    parent2[i1][j1] = (short) k;  //父节点到此点的方向
                    isFound = true;
                    break;
                } else if ('-' == level[i1][j1] && !mark[i1][j1]) {
                    tail++;
                    pt0[tail] = i1 << 16 | j1;   //新的足迹
                    mark[i1][j1] = true;
                    parent2[i1][j1] = (short) k;  //父节点到此点的方向
                }
            }
            if (isFound) break;
            p++;
        }

        if (isFound) {  // 拼接穿越路径
            //穿越中，人的移动
            int t_er = nRow2, t_ec = nCol2, t1, t2;
            while (t_er != nRow1 || t_ec != nCol1) {
                path.offer(bt[parent2[t_er][t_ec]]);
                t1 = t_er - dr4[parent2[t_er][t_ec]];
                t2 = t_ec - dc4[parent2[t_er][t_ec]];
                t_er = t1;
                t_ec = t2;
            }
            //穿越中，路径两端的推动（出于效率及简化算法考虑，仅支持直推穿越）
            for (int k = 0; k <= num; k++) {
                //反向推回原位
                switch (dir) {
                    case 0: path.offerFirst(bt[5]); break;
                    case 1: path.offerFirst(bt[4]); break;
                    case 2: path.offerFirst(bt[7]); break;
                    case 3: path.offerFirst(bt[6]); break;
                }
                //正向推至可穿越位置
                path.offer(bt[dir+4]);
            }
        }
    }

    //查看仓管员是否可以从第一点 [firR][firC] 到达第二点 [secR][setC]
    public boolean manTo2(boolean flg, char[][] level, int boxR, int boxC, int firR, int firC, int secR, int secC) {

        if (firR == secR && firC == secC) return true;

        pt[0] = firR << 16 | firC;

        boolean[][] mark = mark1, mark9 = flg ? mark6 : mark5;

        for (int i = 0; i < mMapRows; i++) {
            for (int j = 0; j < mMapCols; j++) {
                if (level[i][j] == '-' || level[i][j] == '.' || level[i][j] == '@' || level[i][j] == '+') tmpLevel[i][j] = '-';
                else if (level[i][j] == '*') tmpLevel[i][j] = '$';
                else tmpLevel[i][j] = level[i][j];
                mark[i][j] = false;
            }
        }

        int i1, i2, i3, j1, j2, j3;
        mark[firR][firC] = true;
        int p = 0, tail = 0;
        while (p <= tail) {  //先做四邻排查
            while (p <= tail) {
                for (int k = 0; 4 > k; k++) {
                    i1 = (pt[p] >>> 16) + dr4[k];
                    j1 = (pt[p] & 0x0000ffff) + dc4[k];
                    if (i1 < 0 || j1 < 0 || i1 >= mMapRows || j1 >= mMapCols) {  // 界外
                        continue;
                    } else if ('-' == tmpLevel[i1][j1] && !mark[i1][j1]) {
                        tail++;
                        pt[tail] = i1 << 16 | j1;   //新的足迹
                        mark[i1][j1] = true;  //新足迹离起点的距离

                        if (secR == (pt[tail] >>> 16) && secC == (pt[tail] & 0x0000ffff)) {  //到达目标
                            return true;
                        }
                    }
                }
                p++;
            }

            //四邻排查后，对不可达的特殊点，进行穿越排查
            if (myMaps.m_Sets[17] == 1) {
                for (int i = 1; i < mMapRows - 1; i++) {
                    for (int j = 1; j < mMapCols - 1; j++) {
                        if ('-' == tmpLevel[i][j] && !mark[i][j]) {
                            for (int k = 0; 4 > k; k++) {
                                i1 = i - dr4[k];
                                j1 = j - dc4[k];
                                if (boxR >= 0 && i1 == boxR && j1 == boxC) {
                                    continue;  //很重要，点击的箱子不可作为穿越点
                                }
                                deep_Thur = 0;
                                if (flg) {  //逆推
                                    i1 = i + 3 * dr4[k];
                                    j1 = j + 3 * dc4[k];
                                    i2 = i + 2 * dr4[k];
                                    j2 = j + 2 * dc4[k];
                                    i3 = i + dr4[k];
                                    j3 = j + dc4[k];
                                    if (i1 < 0 || j1 < 0 || i1 >= mMapRows || j1 >= mMapCols ||
                                            i2 < 0 || j2 < 0 || i2 >= mMapRows || j2 >= mMapCols ||
                                            i3 < 0 || j3 < 0 || i3 >= mMapRows || j3 >= mMapCols) {  // 界外
                                        continue;
                                    } else if ('$' == tmpLevel[i3][j3] && mark[i1][j1] && mark[i2][j2]) {
                                        tmpLevel[i3][j3] = '-';
                                        if (isChuanYue(true, tmpLevel, i2, j2, i1, j1, i3, j3, k)) {
                                            mark[i][j] = true;
                                            mark9[i3][j3] = true;
                                            tail++;
                                            pt[tail] = i << 16 | j;

                                            if (i == secR && j == secC) {  //到达目标
                                                tmpLevel[i3][j3] = '$';
                                                return true;
                                            }
                                        }
                                        tmpLevel[i3][j3] = '$';
                                    }
                                } else {  //正推
                                    i1 = i + dr4[k];
                                    j1 = j + dc4[k];
                                    i2 = i - dr4[k];
                                    j2 = j - dc4[k];
                                    i3 = i - 2 * dr4[k];
                                    j3 = j - 2 * dc4[k];
                                    if (i1 < 0 || j1 < 0 || i1 >= mMapRows || j1 >= mMapCols ||
                                            i2 < 0 || j2 < 0 || i2 >= mMapRows || j2 >= mMapCols ||
                                            i3 < 0 || j3 < 0 || i3 >= mMapRows || j3 >= mMapCols) {  // 界外
                                        continue;
                                    } else if ('$' == tmpLevel[i2][j2] && '-' == tmpLevel[i1][j1] && mark[i3][j3]) {
                                        tmpLevel[i2][j2] = '-';
                                        if (isChuanYue(false, tmpLevel, i, j, i2, j2, i1, j1, k)) {
                                            mark[i][j] = true;
                                            mark9[i2][j2] = true;
                                            tail++;
                                            pt[tail] = i << 16 | j;

                                            if (i == secR && j == secC) {  //到达目标
                                                tmpLevel[i2][j2] = '$';
                                                return true;
                                            }
                                        }
                                        tmpLevel[i2][j2] = '$';
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    //查看两点[firR][firC]和[secR][setC]是否可达；[boxR][boxC]为所推的箱子的位置，其 boxR < 0 时，则不查看它
    int len1, len2;
    boolean ls_flg;
    private boolean manTo2b(boolean flg, char[][] tmpLevel, int boxR, int boxC, int firR, int firC, int secR, int secC) {

        //点1 == 点2
        if (firR == secR && firC == secC) return true;

        //点2 不是空地
        try {
            if (tmpLevel[secR][secC] != '-') return false;
        } catch (ArrayIndexOutOfBoundsException ex) {
            return false;
        }

        len1 = block[firR][firC].Length();  //点1 占据的图块数
        if (!cut[boxR][boxC] && len1 > 0) return true;  //目标箱子不在割点上
        else {
            len2 = block[secR][secC].Length();  //点2 占据的图块数
            for (int i = 0; i < len1; i++) {  //两点在同一块内，必定可达
                for (int j = 0; j < len2; j++) {
                    if (block[firR][firC].Get(i) == block[secR][secC].Get(j)) return true;
                }
            }

            //当割点法不可达时，对于被点击的箱子做穿越检查
            if (myMaps.m_Sets[17] == 1 || tmpLevel[boxR][boxC] == '-') {
                tmpLevel[boxR][boxC] = '$';
                ls_flg = manTo2(flg, tmpLevel, boxR, boxC, firR, firC, secR, secC);
                tmpLevel[boxR][boxC] = '-';
            }
            return ls_flg;
        }
    }

    //在lvl[][]中(tmpLevel[][])，仓管员已经被置换成 '-'，方便利用此函数及其它函数中查看是否为通道
    private boolean isPass(char[][] lvl, int rr, int cc) {
        try {
            if (lvl[rr][cc] == '-') return true;  //通道
        } catch (ArrayIndexOutOfBoundsException ex) { }
        return false;
    }

    //计算箱子的可达位置
    public void boxReachable(boolean flg, int boxR, int boxC, int manR, int manC) {
        int newR, newC, mToR, mToC;

        boolean[][] mark = flg ? mark4 : mark3, mark9 = flg ? mark6 : mark5;
        char[][] tmpLevel = flg ? tmpLevel2 : tmpLevel1;  //临时关卡指针

        Queue<bm_Node> Q = new LinkedList<bm_Node>();

        //各标志数组初始化
        for (int i = 0; i < mMapRows; i++)
            for (int j = 0; j < mMapCols; j++) {
                mark[i][j] = false;  //是否可达
                mark0[i][j][0] = false;  //新节点的四方向（反向）是否已推
                mark0[i][j][1] = false;
                mark0[i][j][2] = false;
                mark0[i][j][3] = false;
            }

        //初始位置检测
        Q.offer(new bm_Node(boxR, boxC, manR, manC));  //初始位置入队列，待查其四邻
        mark[boxR][boxC] = true;  //被点箱子

        bm_Node f;
        while (!Q.isEmpty()) {
            f = Q.poll();//出队列
            for (int k = 0; k < 4; k++) {//检查f的四邻
                if (mark0[f.box_R][f.box_C][k]) {  //该节点的此方向（反）已推
                    continue;
                }
                newR = f.box_R + dr4[k];  //箱子新位置
                newC = f.box_C + dc4[k];
                if (flg) {  //逆推
                    mToR = newR;  //箱子至新位置，人需到的位置
                    mToC = newC;
                    //界外，不计算
                    if (!isPass(tmpLevel, newR + dr4[k], newC + dc4[k]) || !isPass(tmpLevel, mToR, mToC)) {
                        continue;
                    }
                } else {
                    mToR = f.box_R - dr4[k];  //箱子至新位置，人需到的位置
                    mToC = f.box_C - dc4[k];
                    //界外，不计算
                    if (!isPass(tmpLevel, newR, newC) || !isPass(tmpLevel, mToR, mToC)) {
                        continue;
                    }
                }
                if (manTo2b(flg, tmpLevel, f.box_R, f.box_C, f.man_R, f.man_C, mToR, mToC)) {//人能否过来
                    if (flg)  //逆推
                        Q.offer(new bm_Node(newR, newC, newR + dr4[k], newC + dc4[k]));//新可达点入队列待查
                    else
                        Q.offer(new bm_Node(newR, newC, f.box_R, f.box_C));//新可达点入队列待查
                    mark[newR][newC] = true;  //新的可达点
                    mark0[f.box_R][f.box_C][k] = true;  //该节点的此方向（反）已推
                }
            }
        }

        //新穿越算法，逆推的穿越提示点在被电击的箱子上有些异常，清理一下，具体原因待查
        for (int i = 1; i < mMapRows - 1; i++) {
            for (int j = 1; j < mMapCols - 1; j++) {
                if ('$' != tmpLevel[i][j] && mark9[i][j]) mark9[i][j] = false;
            }
        }
    }

    //计算箱子从 [boxR][boxC] 到达 [toR][toC] 的最短路径
    public void boxTo(boolean flg, int boxR, int boxC, int toR, int toC, int manR, int manC, LinkedList<Byte> pathByteList3) {
        pathByteList3.clear();

        if (boxR == toR && boxC == toC) return;

        int newR, newC, mFromR = manR, mFromC = manC, mToR, mToC, H, T;
        char[][] tmpLevel = flg ? tmpLevel2 : tmpLevel1;  //临时关卡指针

        Comparator<bm_Node2> comparator = new bmComparator();
        PriorityQueue<bm_Node2> PQ = new PriorityQueue<bm_Node2>(mMapRows * mMapCols, comparator);
        LinkedList<path_Node> Path = new LinkedList<path_Node>();  //闭集

        for (int i = 0; i < mMapRows; i++)
            for (int j = 0; j < mMapCols; j++) {
                mark0[i][j][0] = false;  //节点的四方向（反向）是否已推
                mark0[i][j][1] = false;
                mark0[i][j][2] = false;
                mark0[i][j][3] = false;
            }

        boolean isFound = false;  //是否找到了有路径
        PQ.offer(new bm_Node2(boxR, boxC, mFromR, mFromC, Math.abs(boxR - toR) + Math.abs(boxC - toC), 0, -1, -1));

        bm_Node2 f;
        while (!isFound && !PQ.isEmpty()) {
            f = PQ.poll();//出队列
            for (int k = 0; k < 4; k++) { //检查f的四邻
                if (mark0[f.box_R][f.box_C][k]) {  //该节点的此方向（反）已推
                    continue;
                }
                newR = f.box_R + dr4[k];  //箱子新位置
                newC = f.box_C + dc4[k];
                if (flg) {  //逆推
                    mToR = newR;  //箱子至新位置，人需到的位置
                    mToC = newC;
                    if (!isPass(tmpLevel, newR + dr4[k], newC + dc4[k]) || !isPass(tmpLevel, mToR, mToC)) {
                        continue;
                    }
                } else {
                    mToR = f.box_R - dr4[k];  //箱子至新位置，人需到的位置
                    mToC = f.box_C - dc4[k];
                    if (!isPass(tmpLevel, newR, newC) || !isPass(tmpLevel, mToR, mToC)) {
                        continue;
                    }
                }
                if (manTo2b(flg, tmpLevel, f.box_R, f.box_C, f.man_R, f.man_C, mToR, mToC)) {//人能否过来
                    mark0[f.box_R][f.box_C][k] = true;  //该节点的此方向（反）已推
                    H = Math.abs(newR - toR) + Math.abs(newC - toC);  //评估值，尽量向目标点靠拢
                    if (f.D == k) T = f.T;  //转弯累计值，以此作为优先队列中节点比较的主力
                    else T = f.T + 1;
                    if (flg)  //逆推
                        PQ.offer(new bm_Node2(newR, newC, newR + dr4[k], newC + dc4[k], H, f.G + 1, T, k));//新可达点入队列待查
                    else
                        PQ.offer(new bm_Node2(newR, newC, f.box_R, f.box_C, H, f.G + 1, T, k));//新可达点入队列待查

                    Path.offer(new path_Node(newR, newC, (byte) k, (byte) f.D));

                    if (newR == toR && newC == toC) {  //到达目标点
                        isFound = true;
                        break;
                    }
                }
            }
        }

        if (isFound) {  //找到了路径
            LinkedList<Byte> pathByteList = new LinkedList<Byte>();
            LinkedList<Byte> pathByteList2;

            path_Node phNode;
            mToR = toR;  //箱子目标
            mToC = toC;
            byte mDir, mDir0 = -1;
            while (!Path.isEmpty()) {  //取得箱子路径保存到 List
                phNode = Path.pollLast();
                if (phNode.box_R != mToR || phNode.box_C != mToC || mDir0 >= 0 && mDir0 != phNode.dir) {
                    continue;
                }
                pathByteList.offer(phNode.dir);
                mToR = phNode.box_R - dr4[phNode.dir];
                mToC = phNode.box_C - dc4[phNode.dir];
                mDir0 = phNode.dir2;  //父节点的父节点
            }
            mDir0 = -1;
            //箱子和人推动前的位置
            newR = boxR;
            newC = boxC;
            mFromR = manR;
            mFromC = manC;
            while (!pathByteList.isEmpty()) {  //取得包括人移动的完整路径 String
                mDir = pathByteList.pollLast();
                if (flg) {  //逆推
                    mToR = newR + dr4[mDir];
                    mToC = newC + dc4[mDir];
                } else {
                    mToR = newR - dr4[mDir];
                    mToC = newC - dc4[mDir];
                }
                if (mDir == mDir0) {  //箱子移动方向没有改变
                    pathByteList3.offerFirst(bt[mDir + 4]);
                } else {  //箱子改变了移动方向
                    tmpLevel[newR][newC] = '$';
                    pathByteList2 = manTo(flg, tmpLevel, mFromR, mFromC, mToR, mToC);  //计算人移位路径
                    while (!pathByteList2.isEmpty()) {
                        pathByteList3.offerFirst(pathByteList2.pollLast());
                    }
                    pathByteList3.offerFirst(bt[mDir + 4]);
                    tmpLevel[newR][newC] = '-';
                    mDir0 = mDir;
                }
                if (flg) {  //逆推
                    newR = mToR;  //箱子进一位（人在箱子前面）
                    newC = mToC;
                    mFromR = newR + dr4[mDir];  //人进一位（人在箱子前面）
                    mFromC = newC + dc4[mDir];
                } else {
                    mFromR = newR;  //人到箱子的位置（人在箱子后面）
                    mFromC = newC;
                    newR = newR + dr4[mDir];  //箱子进一位
                    newC = newC + dc4[mDir];
                }
            }
        }
    }

    final byte[] mByte = {1, 2, 4, 8};  //便于“块”的查找
    int Box_Row, Box_Col;  //临时记录被点击箱子位置，计算割点 CutVertex() 时使用
    //参数：flg -- 是否逆推，level -- 地图现场，boxR、boxC -- 被点击的箱子，manR、manC -- 仓管员坐标
    public void FindBlock(boolean flg, char[][] level, int boxR, int boxC) {
        char[][] tmpLevel = flg ? tmpLevel2 : tmpLevel1;  //临时关卡指针
        boolean[][] mark = flg ? mark4 : mark3, mark9 = flg ? mark6 : mark5;

        Box_Row = boxR;  //CutVertex()使用
        Box_Col = boxC;

        depth = 0;
        b_count = -1;  //块号有从 -1 递减来标示

        for (int i = 0; i < myMaps.curMap.Rows; i++) {
            for (int j = 0; j < myMaps.curMap.Cols; j++) {
                //生成关卡副本
                if (level[i][j] == '-' || level[i][j] == '.' || level[i][j] == '@' || level[i][j] == '+') tmpLevel[i][j] = '-';
                else if (level[i][j] == '*') tmpLevel[i][j] = '$';
                else tmpLevel[i][j] = level[i][j];
                mark[i][j] = false;  //可达点
                mark9[i][j] = false;  //穿越点
                cut[i][j] = false;
                parent[i][j] = -1;
                block[i][j] = new bc_Node();
                children[i][j] = 0;
                depth_tag[i][j] = 0;
                low_tag[i][j] = 0;
            }
        }

        //计算割点时，需要先去掉被点击的箱子
        tmpLevel[boxR][boxC] = '-';
        CutVertex(tmpLevel, boxR, boxC, mark);  //递归计算割点，块

        //检查 DFS 的根节点
        int j = 0; //计数根节点的子树

        for (int i = 0; i < 4; i++) {
            if ((children[boxR][boxC] & mByte[i]) > 0) {
                j++;
                block[boxR][boxC].Push(b_count);
                Block(boxR + dr4[i], boxC + dc4[i]);
                b_count--;
            }
        }
        if (j >= 2) { //若根节点有两个以上的子树, 则根节点也是“割点”
            cut[boxR][boxC] = true;
        }
    }

    //循环法为“块”内的节点做标识
    private void Block(int r, int c) {
        int rr, cc;

        //将坐标用一个 int 存储
        ptBlock[0] = r << 16 | c;

        int p = 0, tail = 0;
        while (p <= tail) {
            rr = ptBlock[p] >>> 16;
            cc = ptBlock[p] & 0x0000ffff;
            block[rr][cc].Push(b_count);
            for (int i = 0; i < 4; i++) {
                if ((children[rr][cc] & mByte[i]) > 0) {
                    tail++;
                    ptBlock[tail] = (rr + dr4[i]) << 16 | (cc + dc4[i]);
                }
            }
            p++;
        }
    }

    //计算“割点”、“块”，从目标箱子位置 [row][col] 开始
    final byte[] dir = {1, 0, 3, 2};  //换算父节点方向用

    //mark与FindBlock()中的复位相关联，记录箱子可达点
    private void CutVertex(char[][] tmpLevel, int row, int col, boolean[][] mark) {

        mark[row][col] = true; //已访问标记

        depth++;
        depth_tag[row][col] = depth;
        low_tag[row][col] = depth;  //标记 low 点

        try {
            byte i;
            for (i = 0; i < 4; i++) {
                if (mark[row + dr4[i]][col + dc4[i]] == true) {//节点被访问过
                    //若非父节点, 那么标记其为“返祖边”
                    if (parent[row][col] != i && depth_tag[row + dr4[i]][col + dc4[i]] < low_tag[row][col])
                        low_tag[row][col] = depth_tag[row + dr4[i]][col + dc4[i]];
                } else if (tmpLevel[row + dr4[i]][col + dc4[i]] == '-') { //新的子节点
                    parent[row + dr4[i]][col + dc4[i]] = dir[i];  //标示父节点的动作方向
                    children[row][col] = (short) (children[row][col] | mByte[i]);  //增加子树

                    CutVertex(tmpLevel, row + dr4[i], col + dc4[i], mark);

                    if (low_tag[row + dr4[i]][col + dc4[i]] < low_tag[row][col]) { //若子节点的 low值小于其父节点的low值
                        low_tag[row][col] = low_tag[row + dr4[i]][col + dc4[i]];  //重置其父节点的 low值
                    } else if (low_tag[row + dr4[i]][col + dc4[i]] >= depth_tag[row][col]) { //若子节点的 low值大于其父节点的low值，则父节点为“割点”
                        if (Box_Row != row || Box_Col != col) {
                            if (!cut[row][col]) {
                                cut[row][col] = true;
                            }
                            //标记“块”
                            block[row][col].Push(b_count); //标记割点自身
                            Block((row + dr4[i]), col + dc4[i]);  //标记此割点的子树
                            b_count--;        //块号减一
                            children[row][col] = (short) (children[row][col] & (~mByte[i])); //移除此“割点”及其子树
                        }
                    }
                }
            }
        } catch (Throwable ex) {  //ArrayIndexOutOfBoundsException
        }
        depth--;
    }


    ////////////////////////////////////////////////////////////////////////
    //辅助类定义
    ////////////////////////////////////////////////////////////////////////

    //计算图块用节点
    class bc_Node {
        ArrayList<Integer> bc_Num;

        public bc_Node() {
            bc_Num = new ArrayList<Integer>();
        }

        public void Push(int bc_num) {
            bc_Num.add(bc_num);
        }

        public int Length() {
            return bc_Num.size();
        }

        public int Get(int i) {
            return bc_Num.get(i);
        }
    }

    //搜询箱子可达队列中，人与箱子组合节点
    class bm_Node {
        int box_R;
        int box_C;
        int man_R;
        int man_C;

        public bm_Node(int box_R, int box_C, int man_R, int man_C) {
            this.box_R = box_R;
            this.box_C = box_C;
            this.man_R = man_R;
            this.man_C = man_C;
        }
    }

    //箱子寻径中，人与箱子组合节点
    class bm_Node2 {
        int box_R;
        int box_C;
        int man_R;
        int man_C;
        int H;  //评估值
        int G;  //累计步数
        int T;  //累计转弯次数
        int D;  //父节点方向

        public bm_Node2(int box_R, int box_C, int man_R, int man_C, int H, int G, int T, int D) {
            this.box_R = box_R;
            this.box_C = box_C;
            this.man_R = man_R;
            this.man_C = man_C;
            this.H = H;
            this.G = G;
            this.T = T;
            this.D = D;
        }
    }

    //拼接路径队列中，箱子与方向节点
    class path_Node {
        int box_R;
        int box_C;
        byte dir;  //用以指向父节点
        byte dir2;  //用以指向父节点的父节点

        public path_Node(int box_R, int box_C, byte dir, byte dir2) {
            this.box_R = box_R;
            this.box_C = box_C;
            this.dir = dir;
            this.dir2 = dir2;
        }
    }

    class bmComparator implements Comparator<bm_Node2> {
        @Override  //经测试，按以下次序做优先队列的比较函数，箱子推直线的机会最多
        public int compare(bm_Node2 x, bm_Node2 y) {
            if (x.T < y.T) return -1;  //先比较转弯数
            else if (x.T > y.T) return 1;
            else {
                if (x.H < y.H) return -1;  //次比较评估值
                else if (x.H > y.H) return 1;
                else {
                    if (x.G < y.G) return -1;  //最后比较推动消耗（步数）
                    else if (x.G > y.G) return 1;
                }
            }
            return 0;
        }
    }
}
