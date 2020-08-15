package my.boxman.jsoko.board;

//有关方向操作的静态方法
public final class Directions implements DirectionConstants {
	// 计算某方向的反方向
	public static final int getOppositeDirection(int dir) {
		return dir ^ 01;
	}

	// 根据一个方向计算另一个方向，使得它们相交（可做反复切换）
	public static final int getOrthogonalDirection( int dir ) {
		return (dir ^ 02) & 03;
	}
}
