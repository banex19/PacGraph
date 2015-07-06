package visualizer;

import visualizer.Visualizer3D.MOVE_DIRECTION;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

public class Sprite extends Model {

	private int screenHeight = 0, screenWidth = 0;
	private int spriteHeight = 20, spriteWidth = 20;

	private boolean updatedUvs = false;
	private int currentX = -1, currentY = -1;
	private float finalX = 0, finalY = 0, finalZ = 0, w = 0, h = 0;
	private boolean useOverrideColor = false;
	private float r = 1.0f, g = 0, b = 0;

	private float[] traslMatrix = new float[16];
	private float[] scaleMatrix = new float[16];

	private MOVE_DIRECTION currentDir = MOVE_DIRECTION.MOVE_RIGHT;

	public static Sprite getNewSprite(String filename, int spriteWidth,
			int spriteHeight, int screenWidth, int screenHeight) {
		float[] vertices = new float[] { 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0 };
		Sprite sprite = new Sprite(vertices, RenderingGlobals.staticIndices,
				RenderingGlobals.staticNormals,
				RenderingGlobals.staticUVsRight, spriteWidth, spriteHeight,
				screenWidth, screenHeight);
		sprite.loadAndBindTexture(filename);
		return sprite;
	}

	public Sprite(float[] vertices, int[] indices, float[] normals,
			float[] uvs, int spriteWidth, int spriteHeight, int screenWidth,
			int screenHeight) {
		super(vertices, indices, normals, uvs);
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
		this.spriteWidth = spriteWidth;
		this.spriteHeight = spriteHeight;

		GraphicsMath.setIdentityMatrix(traslMatrix, 4);
		GraphicsMath.setIdentityMatrix(scaleMatrix, 4);

		textureSprite = true;
	}

	public void setLayer(int l) {
		if (l < 0)
			return;

		float newL = 1.0f / (l + 1);
		finalZ = newL;
	}

	public void setOverrideColor(boolean override) {
		useOverrideColor = override;
	}

	public void changeOverrideColor(float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}

	public PointInt getPosition() {
		return new PointInt(currentX + spriteWidth / 2, currentY + spriteHeight
				/ 2);
	}

	public void updatePosition(Point p) {
		if (p instanceof PointInt) {
			updatePositionInt((PointInt) p);
		} else if (p instanceof PointFloat) {
			updatePositionFloat((PointFloat) p);
		}
	}

	private void updatePositionInt(PointInt p) {

		if (p == null)
			return;

		int x = p.x;
		int y = p.y;

		x -= spriteWidth / 2;
		y -= spriteHeight / 2;

		updatePosition(x, y);
	}

	private void updatePositionFloat(PointFloat p) {

		if (p == null)
			return;

		float x = p.x;
		float y = p.y;

		x -= spriteWidth / 2;
		y -= spriteHeight / 2;

		updatePosition(x, y);
	}

	public void updateTextureCoordinates(MOVE_DIRECTION dir) {

		if (currentDir == dir)
			return;

		if (dir == MOVE_DIRECTION.MOVE_RIGHT) {
			uvArray = RenderingGlobals.staticUVsRight;
		} else if (dir == MOVE_DIRECTION.MOVE_LEFT) {
			uvArray = RenderingGlobals.staticUVsLeft;
		} else if (dir == MOVE_DIRECTION.MOVE_UP) {
			uvArray = RenderingGlobals.staticUVsUp;
		} else if (dir == MOVE_DIRECTION.MOVE_DOWN) {
			uvArray = RenderingGlobals.staticUVsDown;
		}

		currentDir = dir;

		updatedUvs = true;
	}

	public void updatePosition(float x, float y) {

		currentX = (int) x;
		currentY = (int) y;

		y = screenHeight - y;

		float newX = ((x - 0.0f) / (screenWidth - 0.0f)) * (1.0f - (-1.0f))
				+ (-1.0f);
		float newY = ((y - 0.0f) / (screenHeight - 0.0f)) * (1.0f - (-1.0f))
				+ (-1.0f);

		updateCoords(newX, newY);
	}

	public void updatePosition(int x, int y) {

		currentX = x;
		currentY = y;

		y = screenHeight - y;

		float newX = ((x - 0.0f) / (screenWidth - 0.0f)) * (1.0f - (-1.0f))
				+ (-1.0f);
		float newY = ((y - 0.0f) / (screenHeight - 0.0f)) * (1.0f - (-1.0f))
				+ (-1.0f);

		updateCoords(newX, newY);

	}

	private void updateCoords(float newX, float newY) {
		float newWidth = (float) spriteWidth / (float) screenWidth * 2.0f;
		float newHeight = (float) spriteHeight
				/ (float) (screenHeight - MenuBar.getMenuBarHeight()) * 2.0f;

		this.finalX = newX;
		this.finalY = newY;
		w = newWidth;
		h = newHeight;
	}

	public void render() {
		GL2 gl = Visualizer3D.getGLPointer();

		GraphicsMath.setTranslationMatrix(traslMatrix, finalX, finalY - 1 * h,
				finalZ);
		GraphicsMath.setScaleMatrix(scaleMatrix, w, h, 1.0f);

		if (updatedUvs == true) {
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, modelVBO[3]);

			gl.glBufferData(GL.GL_ARRAY_BUFFER, SIZE_OF_FLOAT * uvArray.length,
					Buffers.newDirectFloatBuffer(uvArray), GL.GL_STATIC_DRAW);
			updatedUvs = false;
		}

		gl.glUniformMatrix4fv(RenderingGlobals.traslMatrixLoc, 1, false,
				traslMatrix, 0);
		gl.glUniformMatrix4fv(RenderingGlobals.scaleMatrixLoc, 1, false,
				scaleMatrix, 0);

		if (useOverrideColor) {
			gl.glUniform1i(RenderingGlobals.overrideColorLoc, 1);

			gl.glUniform3f(RenderingGlobals.overrideColorVecLoc, r, g, b);
		} else {
			gl.glUniform1i(RenderingGlobals.overrideColorLoc, 0);
		}

		super.render();
	}
}
