package visualizer;

import java.io.File;
import java.io.IOException;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

public class Model {

	static final int SIZE_OF_FLOAT = 4;
	static final int SIZE_OF_INT = 4;

	// Vertex attributes arrays.
	protected float[] vertexArray = null;
	private int[] indexArray = null;
	private float[] normalArray = null;
	protected float[] uvArray = null;

	private Texture texture;

	protected boolean textureSprite = false;

	// Model VBOs.
	protected int[] modelVBO = { 0, 0, 0, 0 };

	public Model(float[] vertices, int[] indices, float[] normals, float[] uvs) {
		createNew(vertices, indices, normals, uvs);
	}

	public void createNew(float[] vertices, int[] indices, float[] normals,
			float[] uvs) {
		vertexArray = vertices;
		indexArray = indices;
		normalArray = normals;
		uvArray = uvs;
		createOpenGLArrays();
	}

	public void loadAndBindTexture(String filename) {
		try {
			texture = TextureIO.newTexture(new File(filename), true);
		} catch (GLException | IOException e) {
			texture = null;
			System.out.println("Warning: cannot load texture from file "
					+ filename);
			return;
		}
	}

	public void render() {
		GL2 gl = Visualizer3D.getGLPointer();

		// Bind buffers.
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, modelVBO[0]);
		gl.glVertexAttribPointer(RenderingGlobals.positionAttribute, 3,
				GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(RenderingGlobals.positionAttribute);

		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, modelVBO[1]);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, modelVBO[3]);
		gl.glVertexAttribPointer(RenderingGlobals.uvAttribute, 2, GL.GL_FLOAT,
				false, 0, 0);
		gl.glEnableVertexAttribArray(RenderingGlobals.uvAttribute);

		/*
		 * gl.glBindBuffer(GL.GL_ARRAY_BUFFER, modelVBO[2]);
		 * gl.glVertexAttribPointer(RenderingGlobals.normalAttribute, 3,
		 * GL.GL_FLOAT, false, 0, 0);
		 * gl.glEnableVertexAttribArray(RenderingGlobals.normalAttribute);
		 */

		// Bind texture.
		if (texture != null) {
			gl.glActiveTexture(GL.GL_TEXTURE1);
			texture.bind(gl);

			if (!textureSprite)
				gl.glUniform1i(RenderingGlobals.textureLocModel, 1);
			else {
				gl.glUniform1i(RenderingGlobals.textureLocSprite, 1);
			}
		}

		// Render.
		gl.glDrawElements(GL.GL_TRIANGLES, indexArray.length,
				GL.GL_UNSIGNED_INT, 0);

		gl.glActiveTexture(GL.GL_TEXTURE0);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);

		gl.glDisableVertexAttribArray(RenderingGlobals.normalAttribute);
		gl.glDisableVertexAttribArray(RenderingGlobals.uvAttribute);
		gl.glDisableVertexAttribArray(RenderingGlobals.positionAttribute);
	}

	public void dispose() {
		GL2 gl = Visualizer3D.getGLPointer();

		gl.glDeleteBuffers(4, modelVBO, 0);

		if (texture != null) {
			texture.disable(gl);
			texture.destroy(gl);
		}
	}

	private void createOpenGLArrays() {
		GL2 gl = Visualizer3D.getGLPointer();

		gl.glGenBuffers(4, modelVBO, 0);

		// Vertex array.
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, modelVBO[0]);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, SIZE_OF_FLOAT * vertexArray.length,
				Buffers.newDirectFloatBuffer(vertexArray), GL.GL_STATIC_DRAW);

		// Index array.
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, modelVBO[1]);
		gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, SIZE_OF_INT
				* indexArray.length, Buffers.newDirectIntBuffer(indexArray),
				GL.GL_STATIC_DRAW);

		// Normal array.
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, modelVBO[2]);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, SIZE_OF_FLOAT * normalArray.length,
				Buffers.newDirectFloatBuffer(normalArray), GL.GL_STATIC_DRAW);

		// Texture coordinates.
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, modelVBO[3]);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, SIZE_OF_FLOAT * uvArray.length,
				Buffers.newDirectFloatBuffer(uvArray), GL.GL_STATIC_DRAW);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);

	}
}
