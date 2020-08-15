package my.boxman.jsoko.board;

/**
 * 方向常量
 *    U            0
 *    |            |
 * L -+- R  ==  2 -+- 3
 *    |            |
 *    D            1
 */
public interface DirectionConstants {
	byte UP     = 0;
	byte DOWN   = 1;
	byte LEFT   = 2;
	byte RIGHT  = 3;
	byte NO_DIR = -1;                // 暂无方向时的常量

	byte DIRS_COUNT  = 4;            // 有效方向的个数
}
