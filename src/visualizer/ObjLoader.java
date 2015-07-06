package visualizer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ObjLoader {

	private class Vertex {
		float vx, vy, vz;
		float nx, ny, nz;
		float u, v;

		public Vertex(float vx, float vy, float vz, float nx, float ny,
				float nz, float u, float v) {
			this.vx = vx;
			this.vy = vy;
			this.vz = vz;
			this.nx = nx;
			this.ny = ny;
			this.nz = nz;
			this.u = u;
			this.v = v;
		}
	}

	private ArrayList<Float> vertices = new ArrayList<>();
	private ArrayList<Integer> indices = new ArrayList<>();
	private ArrayList<Float> normals = new ArrayList<>();
	private ArrayList<Float> uvs = new ArrayList<>();

	private ArrayList<Vertex> verticesOut = new ArrayList<Vertex>();
	private int numVertices = 0;

	private HashMap<Integer, Vertex> finalVertices = new HashMap<Integer, Vertex>();
	private HashMap<Integer, Integer> verticesMapping = new HashMap<Integer, Integer>();

	private BufferedReader reader = null;

	public Model loadModelFromFile(String filename) {
		FileInputStream fileStream = null;
		InputStreamReader streamReader = null;
		reader = null;
		try {
			fileStream = new FileInputStream(filename);
			streamReader = new InputStreamReader(fileStream);
			reader = new BufferedReader(streamReader);

			// Parse the file in another thread.
			Thread parserThread = new Thread(new Runnable() {
				public void run() {
					try {
						parseFile(reader);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});

			// Start thread and wait for it to finish.
			parserThread.start();
			try {
				parserThread.join();
			} catch (InterruptedException e) {
				parserThread.interrupt();
			}

			System.out.println("Done parsing " + filename + ".");

			streamReader.close();
			reader.close();
			fileStream.close();

		} catch (IOException e) {
			return null;
		}

		prepareArrays();

		return new Model(ArrayHelper.getPrimitiveArrayFloat(vertices
				.toArray(new Float[] {})),
				ArrayHelper.getPrimitiveArrayInteger(indices
						.toArray(new Integer[] {})),
				ArrayHelper.getPrimitiveArrayFloat(normals
						.toArray(new Float[] {})),
				ArrayHelper.getPrimitiveArrayFloat(uvs.toArray(new Float[] {})));

	}

	private void prepareArrays() {
		vertices = new ArrayList<>();
		normals = new ArrayList<>();
		uvs = new ArrayList<>();

		for (int i = 0; i < numVertices; ++i) {

			Vertex vertex = verticesOut.get(i);

			vertices.add(vertex.vx);
			vertices.add(vertex.vy);
			vertices.add(vertex.vz);
			normals.add(vertex.nx);
			normals.add(vertex.ny);
			normals.add(vertex.nz);
			uvs.add(vertex.u);
			uvs.add(vertex.v);
		}
	}

	private void parseFile(BufferedReader reader) throws IOException {
		String line = null;

		Pattern commentPattern = Pattern.compile("(^#)(\\s+.*)?");
		Matcher commentMatcher = commentPattern.matcher("");
		Pattern groupPattern = Pattern.compile("(\\s+)?(o|g)(\\s+.*)?");
		Matcher groupMatcher = groupPattern.matcher("");
		Pattern vertexPattern = Pattern
				.compile("(^v)\\s+(\\S*)\\s+(\\S*)\\s+(\\S*)");
		Matcher vertexMatcher = vertexPattern.matcher("");
		Pattern normalPattern = Pattern
				.compile("(^vn)\\s+(\\S*)\\s+(\\S*)\\s+(\\S*)");
		Matcher normalMatcher = normalPattern.matcher("");
		Pattern uvPattern = Pattern.compile("(^vt)\\s+(\\S*)\\s+(\\S*)");
		Matcher uvMatcher = uvPattern.matcher("");
		Pattern facePattern = Pattern
				.compile("(^f)\\s+(\\d+)+/+(\\d+)+/+(\\d+)\\s+(\\d+)+/+(\\d+)+/+(\\d+)\\s+(\\d+)+/+(\\d+)+/+(\\d+)");
		Matcher faceMatcher = facePattern.matcher("");

		while (reader.ready()) {
			line = reader.readLine();

			commentMatcher.reset(line);
			if (commentMatcher.matches()) {
				// Found comment, ignore.
				continue;
			}

			groupMatcher.reset(line);
			if (groupMatcher.matches()) {
				// Found group of vertices.
				continue;
			}

			vertexMatcher.reset(line);
			if (vertexMatcher.matches()) {
				// Found vertex.
				vertices.add(Float.parseFloat(vertexMatcher.group(2))); // X.
				vertices.add(Float.parseFloat(vertexMatcher.group(3))); // Y.
				vertices.add(Float.parseFloat(vertexMatcher.group(4))); // Z.
				continue;
			}

			normalMatcher.reset(line);
			if (normalMatcher.matches()) {
				// Found normal.
				normals.add(Float.parseFloat(normalMatcher.group(2))); // X.
				normals.add(Float.parseFloat(normalMatcher.group(3))); // Y.
				normals.add(Float.parseFloat(normalMatcher.group(4))); // Z.
				continue;
			}

			uvMatcher.reset(line);
			if (uvMatcher.matches()) {
				// Found UVs.
				uvs.add(Float.parseFloat(uvMatcher.group(2))); // U.
				uvs.add(Float.parseFloat(uvMatcher.group(3))); // V.
				continue;
			}

			faceMatcher.reset(line);
			if (faceMatcher.matches()) {

				// Found face.
				// Vertex at groups 2, 5, 8.
				// UVs at groups 3, 6, 9.
				// Normal at groups 4, 7, 10.

				for (int i = 0; i < 3; ++i) {
					int vertexID = 0;
					int v, t, n;
					v = Integer.parseInt(faceMatcher.group(2 + 3 * i)) - 1;
					t = Integer.parseInt(faceMatcher.group(3 + 3 * i)) - 1;
					n = Integer.parseInt(faceMatcher.group(4 + 3 * i)) - 1;

					vertexID = v * 100 + t * 10 + n;

					Vertex vertex = finalVertices.get(vertexID);

					if (vertex == null) {
						vertex = new Vertex(vertices.get(v * 3),
								vertices.get(v * 3 + 1),
								vertices.get(v * 3 + 2), normals.get(n * 3),
								normals.get(n * 3 + 1), normals.get(n * 3 + 2),
								uvs.get(t * 2), uvs.get(t * 2 + 1));

						finalVertices.put(vertexID, vertex);
						verticesOut.add(vertex);
						verticesMapping.put(vertexID, numVertices);
						indices.add(numVertices);

						numVertices++;
					} else {
						indices.add(verticesMapping.get(vertexID));
					}
				}

				continue;
			}

		}
	}
}
