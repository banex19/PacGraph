package visualizer;

public class PointInt implements Point {

	public int x, y;

	public PointInt() {

	}

	public PointInt(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public String toString() {
		return "Point [" + x + ", " + y + "]";
	}

}
