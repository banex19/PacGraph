package visualizer;

public class PointFloat implements Point {
	public float x, y;

	public PointFloat() {

	}

	public PointFloat(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public String toString() {
		return "Point [" + x + ", " + y + "]";
	}
}
