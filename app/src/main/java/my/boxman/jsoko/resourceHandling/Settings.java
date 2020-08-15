package my.boxman.jsoko.resourceHandling;

public final class Settings {
	/** Flag specifying whether the simple deadlock detection is activated. */
	public static boolean detectSimpleDeadlocks = true;

	/** Flag specifying whether the freeze deadlock detection is activated. */
	public static boolean detectFreezeDeadlocks = true;

	/** Flag specifying whether the corral deadlock detection is activated. */
	public static boolean detectCorralDeadlocks = true;

	/** Flag specifying whether the bipartite deadlock detection is activated. */
	public static boolean detectBipartiteDeadlocks = true;

	/** Flag specifying whether the bipartite deadlock detection is activated. */
	public static boolean detectClosedDiagonalDeadlocks = true;

	/** Direction of the solver search. */
	public enum SearchDirection { FORWARD, BACKWARD, BACKWARD_GOAL_ROOM, UNKNOWN }
}