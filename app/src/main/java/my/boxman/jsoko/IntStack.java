package my.boxman.jsoko;

// 此类，用于模拟整数栈操作（移植于 JSoko）
public class IntStack {

	private final int[] data;

	private int topOfStack;

	public IntStack (int size) {
		data = new int[size];
		topOfStack = -1;
	}

	public void add(int value) {
		data[++topOfStack] = value;
	}

	public int remove() {
		return data[topOfStack--];
	}

	public int size() {
		return topOfStack+1;
	}

	public boolean isEmpty() {
		return topOfStack == -1;
	}

	public void clear() {
		topOfStack = -1;
	}
}
