package visualizer;

public class ArrayHelper {

	public static int[] getPrimitiveArrayInteger(Integer[] array) {
		int[] primitive = new int[array.length];

		for (int i = 0; i < array.length; ++i)
			primitive[i] = array[i];

		return primitive;
	}
	
	public static float[] getPrimitiveArrayFloat(Float[] array) {
		float[] primitive = new float[array.length];

		for (int i = 0; i < array.length; ++i)
			primitive[i] = array[i];

		return primitive;
	}
}
