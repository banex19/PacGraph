package visualizer;

public class GraphicsMath {

	public static float getBoundingSphereRadius(float[] vertexArray) {
		float radius = 0.0f;
		float[] center = { 0.0f, 0.0f, 0.0f };

		int count = vertexArray.length / 3;

		for (int i = 0; i < count; ++i) {
			center[0] += vertexArray[i * 3];
			center[1] += vertexArray[i * 3 + 1];
			center[2] += vertexArray[i * 3 + 2];
		}

		center[0] /= count;
		center[1] /= count;
		center[2] /= count;

		float[] distVector = { 0.0f, 0.0f, 0.0f };
		float distSq = 0.0f;
		for (int i = 0; i < count; ++i) {

			distVector[0] = vertexArray[i * 3] - center[0];
			distVector[1] = vertexArray[i * 3 + 1] - center[1];
			distVector[2] = vertexArray[i * 3 + 2] - center[2];

			distSq = lengthSquared(distVector);
			System.out.println(distSq);
			if (distSq > radius)
				radius = distSq;
		}

		return (float) Math.sqrt(radius);
	}

	public static float lengthSquared(float[] vec) {
		return vec[0] * vec[0] + vec[1] * vec[1] + vec[2] * vec[2];
	}

	public static void crossProduct(float a[], float b[], float res[]) {
		res[0] = a[1] * b[2] - b[1] * a[2];
		res[1] = a[2] * b[0] - b[2] * a[0];
		res[2] = a[0] * b[1] - b[0] * a[1];
	}

	public static void normalize(float a[]) {

		float mag = (float) Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);

		a[0] /= mag;
		a[1] /= mag;
		a[2] /= mag;
	}

	public static void setIdentityMatrix(float[] mat, int size) {
		// Fill matrix with 0s.
		for (int i = 0; i < size * size; ++i)
			mat[i] = 0.0f;

		// Fill diagonal with 1s.
		for (int i = 0; i < size; ++i)
			mat[i + i * size] = 1.0f;
	}

	public static void setScaleMatrix(float[] mat, float scaleX, float scaleY,
			float scaleZ) {
		setIdentityMatrix(mat, 4);
		mat[0] = scaleX;
		mat[5] = scaleY;
		mat[10] = scaleZ;
	}

	public static void multMatrix(float[] a, float[] b) {

		float[] res = new float[16];

		for (int i = 0; i < 4; ++i) {
			for (int j = 0; j < 4; ++j) {
				res[j * 4 + i] = 0.0f;
				for (int k = 0; k < 4; ++k) {
					res[j * 4 + i] += a[k * 4 + i] * b[j * 4 + k];
				}
			}
		}
		System.arraycopy(res, 0, a, 0, 16);
	}

	public static void setTranslationMatrix(float[] mat, float x, float y,
			float z) {

		setIdentityMatrix(mat, 4);

		mat[12] = x;
		mat[13] = y;
		mat[14] = z;
	}

	public static void setRotationMatrixX(float[] mat, float angle) {
		setIdentityMatrix(mat, 4);

		float cos = (float) Math.cos(angle);
		float sin = (float) Math.sin(angle);

		mat[5] = cos;
		mat[6] = -sin;
		mat[9] = sin;
		mat[10] = cos;
	}

	public static void setRotationMatrixY(float[] mat, float angle) {
		setIdentityMatrix(mat, 4);

		float cos = (float) Math.cos(angle);
		float sin = (float) Math.sin(angle);

		mat[0] = cos;
		mat[2] = sin;
		mat[8] = -sin;
		mat[10] = cos;
	}

	public static void setRotationMatrixZ(float[] mat, float angle) {
		setIdentityMatrix(mat, 4);

		float cos = (float) Math.cos(angle);
		float sin = (float) Math.sin(angle);

		mat[0] = cos;
		mat[1] = -sin;
		mat[4] = sin;
		mat[5] = cos;
	}

	public static float[] buildProjectionMatrix(float fov, float ratio,
			float nearP, float farP, float[] projMatrix) {

		float f = 1.0f / (float) Math.tan(fov * (Math.PI / 360.0));

		setIdentityMatrix(projMatrix, 4);

		projMatrix[0] = f / ratio;
		projMatrix[1 * 4 + 1] = f;
		projMatrix[2 * 4 + 2] = (farP + nearP) / (nearP - farP);
		projMatrix[3 * 4 + 2] = (2.0f * farP * nearP) / (nearP - farP);
		projMatrix[2 * 4 + 3] = -1.0f;
		projMatrix[3 * 4 + 3] = 0.0f;

		return projMatrix;
	}

	public static float[] setCamera(float posX, float posY, float posZ,
			float lookAtX, float lookAtY, float lookAtZ, float[] viewMatrix) {

		float[] dir = new float[3];
		float[] right = new float[3];
		float[] up = new float[3];

		up[0] = 0.0f;
		up[1] = 1.0f;
		up[2] = 0.0f;

		dir[0] = (lookAtX - posX);
		dir[1] = (lookAtY - posY);
		dir[2] = (lookAtZ - posZ);
		normalize(dir);

		crossProduct(dir, up, right);
		normalize(right);

		crossProduct(right, dir, up);
		normalize(up);

		float[] aux = new float[16];

		viewMatrix[0] = right[0];
		viewMatrix[4] = right[1];
		viewMatrix[8] = right[2];
		viewMatrix[12] = 0.0f;

		viewMatrix[1] = up[0];
		viewMatrix[5] = up[1];
		viewMatrix[9] = up[2];
		viewMatrix[13] = 0.0f;

		viewMatrix[2] = -dir[0];
		viewMatrix[6] = -dir[1];
		viewMatrix[10] = -dir[2];
		viewMatrix[14] = 0.0f;

		viewMatrix[3] = 0.0f;
		viewMatrix[7] = 0.0f;
		viewMatrix[11] = 0.0f;
		viewMatrix[15] = 1.0f;

		setTranslationMatrix(aux, -posX, -posY, -posZ);

		multMatrix(viewMatrix, aux);

		return viewMatrix;
	}
}
