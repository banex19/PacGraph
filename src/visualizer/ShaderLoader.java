package visualizer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import com.jogamp.opengl.GL2;

public class ShaderLoader {

	private enum ShaderType {
		VertexShader, FragmentShader
	}

	static private String readEntireFileAsString(String fileName) {
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(fileName);
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}

		BufferedReader reader = new BufferedReader(
				new InputStreamReader(stream));

		StringBuilder strBuilder = new StringBuilder();

		try {
			String line = reader.readLine();

			while (line != null) {
				strBuilder.append(line + "\n");
				line = reader.readLine();
			}

			reader.close();
			stream.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return strBuilder.toString();
	}

	static int createProgram(GL2 gl, String vertexShaderFilename,
			String fragmentShaderFilename) {
		int v = loadShaderFromFile(gl, vertexShaderFilename,
				ShaderType.VertexShader);
		int f = loadShaderFromFile(gl, fragmentShaderFilename,
				ShaderType.FragmentShader);

		int p = linkProgram(gl, v, f);

		String vertexShaderLog = getShaderInfoLog(gl, v);
		String fragmentShaderLog = getShaderInfoLog(gl, f);
		String programLog = getProgramInfoLog(gl, p);

		if (!vertexShaderLog.isEmpty())
			System.out.println(vertexShaderLog);
		if (!fragmentShaderLog.isEmpty())
			System.out.println(fragmentShaderLog);
		if (!programLog.isEmpty())
			System.out.println(programLog);

		return p;
	}

	static private int linkProgram(GL2 gl, int vertexShaderId,
			int fragmentShaderId) {

		int programId = gl.glCreateProgram();

		gl.glAttachShader(programId, vertexShaderId);
		gl.glAttachShader(programId, fragmentShaderId);

		gl.glBindAttribLocation(programId, RenderingGlobals.positionAttribute,
				"position");
		gl.glBindAttribLocation(programId, RenderingGlobals.uvAttribute,
				"texCoordIn");
		gl.glBindAttribLocation(programId, RenderingGlobals.normalAttribute,
				"normal");

		gl.glLinkProgram(programId);

		return programId;
	}

	static private int loadShaderFromFile(GL2 gl, String fileName,
			ShaderType type) {
		String shaderSource = readEntireFileAsString(fileName);
		int shaderType = type == ShaderType.VertexShader ? GL2.GL_VERTEX_SHADER
				: GL2.GL_FRAGMENT_SHADER;

		int id = gl.glCreateShader(shaderType);

		gl.glShaderSource(id, 1, new String[] { shaderSource }, null);
		gl.glCompileShader(id);

		return id;
	}

	static private String getShaderInfoLog(GL2 gl, int obj) {

		final int logLen = getShaderParameter(gl, obj, GL2.GL_INFO_LOG_LENGTH);
		if (logLen <= 0)
			return "";

		final int[] retLength = new int[1];
		final byte[] bytes = new byte[logLen + 1];
		gl.glGetShaderInfoLog(obj, logLen, retLength, 0, bytes, 0);
		final String logMessage = new String(bytes);

		if (logMessage.length() > 3)
			return String.format("ShaderLog: %s", logMessage);
		else
			return "";
	}

	static private int getProgramParameter(GL2 gl, int obj, int paramName) {
		final int params[] = new int[1];
		gl.glGetProgramiv(obj, paramName, params, 0);
		return params[0];
	}

	static private int getShaderParameter(GL2 gl, int obj, int paramName) {
		final int params[] = new int[1];
		gl.glGetShaderiv(obj, paramName, params, 0);
		return params[0];
	}

	static private String getProgramInfoLog(GL2 gl, int obj) {

		final int logLen = getProgramParameter(gl, obj, GL2.GL_INFO_LOG_LENGTH);
		if (logLen <= 0)
			return "";

		final int[] retLength = new int[1];
		final byte[] bytes = new byte[logLen + 1];
		gl.glGetProgramInfoLog(obj, logLen, retLength, 0, bytes, 0);
		final String logMessage = new String(bytes);

		if (logMessage.length() > 3)
			return String.format("ShaderLog: %s", logMessage);
		else
			return "";
	}
}
