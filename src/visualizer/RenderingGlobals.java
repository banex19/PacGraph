package visualizer;

public class RenderingGlobals {

	static public final int positionAttribute = 0, normalAttribute = 2,
			uvAttribute = 1;

	final static public int[] staticIndices = { 0, 1, 2, 1, 3, 2 };
	final static public float[] staticNormals = new float[12];
	final static public float[] staticUVsRight = { 1, 1, 0, 1, 1, 0, 0, 0 };
	final static public float[] staticUVsLeft = { 0, 1, 1, 1, 0, 0, 1, 0 };
	final static public float[] staticUVsUp = { 1, 0, 1, 1, 0, 0, 0, 1 };
	final static public float[] staticUVsDown = { 0, 1, 0, 0, 1, 1, 1, 0 };
	
	// Uniform variables locations.
	static public int projMatrixLoc, viewMatrixLoc, modelMatrixLoc, scaleLoc,
			textureLocModel, textureLocSprite;

	static public int traslMatrixLoc, scaleMatrixLoc;

	static public int overrideColorLoc, overrideColorVecLoc;
}
