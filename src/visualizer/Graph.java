package visualizer;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

public class Graph {

	public class Vertex {

		public PointInt data = null;
		public int id = -1;
		public ArrayList<Vertex> adjacencyList = null;

		public Vertex(PointInt data, int id) {
			this.data = data;
			this.id = id;
			adjacencyList = new ArrayList<Vertex>();
		}
	}

	int numVertices = 0;

	int numEdges = 0;

	ArrayList<Vertex> vertices = null;

	public Graph() {
		vertices = new ArrayList<Vertex>();
	}

	public void loadEdgesFromFile(String filename) throws IOException {
		FileInputStream stream = new FileInputStream(filename);

		BufferedReader reader = new BufferedReader(
				new InputStreamReader(stream));

		String line = reader.readLine();

		Scanner tokenizer = new Scanner(line);

		int nVerts = tokenizer.nextInt();
		// numEdges = tokenizer.nextInt();

		int i = 0;

		while (i < nVerts) {
			// Skip vertices.
			line = reader.readLine();

			i++;
		}

		i = 0;

		while (reader.ready()) {
			line = reader.readLine();
			tokenizer = new Scanner(line);

			while (tokenizer.hasNextInt()) {
				int w = tokenizer.nextInt();
				addEdge(i, w);
			}

			i++;
		}

		tokenizer.close();

		reader.close();
		stream.close();
	}

	public void loadFromFile(String filename) throws IOException {
		FileInputStream stream = new FileInputStream(filename);

		BufferedReader reader = new BufferedReader(
				new InputStreamReader(stream));

		String line = reader.readLine();

		Scanner tokenizer = new Scanner(line);

		int nVerts = tokenizer.nextInt();
		// numEdges = tokenizer.nextInt();

		int i = 0;

		while (i < nVerts) {

			// Read data.
			line = reader.readLine();
			tokenizer = new Scanner(line);
			int x = tokenizer.nextInt();
			int y = tokenizer.nextInt();

			PointInt data = new PointInt(x, y);

			addVertex(data);

			i++;
		}

		i = 0;

		while (reader.ready()) {
			line = reader.readLine();
			tokenizer = new Scanner(line);

			while (tokenizer.hasNextInt()) {
				int w = tokenizer.nextInt();
				addEdge(i, w);
			}

			i++;
		}

		tokenizer.close();

		reader.close();
		stream.close();
	}

	public void saveToFile(String filename) throws IOException {
		FileOutputStream stream = new FileOutputStream(filename);

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				stream));

		String nl = "\r\n";

		writer.write(numVertices + " " + numEdges + nl);

		for (int i = 0; i < numVertices; ++i) {
			Vertex vertex = vertices.get(i);
			writer.write(vertex.data.x + " " + vertex.data.y + nl);
		}

		if (numEdges > 0) {
			for (int i = 0; i < numVertices; ++i) {
				Vertex vertex = vertices.get(i);
				ArrayList<Vertex> adjList = vertex.adjacencyList;
				for (int j = 0; j < adjList.size(); ++j) {
					writer.write(adjList.get(j).id + " ");
				}
				writer.write(nl);
			}
		}

		writer.flush();
		writer.close();
		stream.close();
	}

	public ArrayList<Vertex> getVertices() {
		return vertices;
	}

	public int addVertex(PointInt v) {
		for (int i = 0; i < vertices.size(); ++i) {
			Vertex vertex = vertices.get(i);
			if (vertex != null && vertex.data == v) {
				return i;
			}
		}

		vertices.add(new Vertex(v, numVertices));
		numVertices++;
		return numVertices - 1;
	}

	public ArrayList<Vertex> shortestPath(Vertex a, Vertex b,
			Vertex toIgnoreFirst) {
		ArrayList<Vertex> path = new ArrayList<Graph.Vertex>();

		// null is white, 1 is gray, 2 is black.

		HashMap<Vertex, Integer> vertexColors = new HashMap<Graph.Vertex, Integer>();
		HashMap<Vertex, Vertex> predecessors = new HashMap<Graph.Vertex, Vertex>();

		LinkedList<Vertex> queue = new LinkedList<Graph.Vertex>();

		vertexColors.put(a, 1);

		queue.add(a);

		boolean foundB = false;

		while (!queue.isEmpty()) {
			Vertex current = queue.remove();

			ArrayList<Vertex> adj = current.adjacencyList;

			for (Vertex v : adj) {
				if (current.id == a.id && toIgnoreFirst != null
						&& toIgnoreFirst.id == v.id)
					continue;

				Integer color = vertexColors.get(v);
				if (color == null) {
					vertexColors.put(a, 1);
					predecessors.put(v, current);

					queue.add(v);

					if (v == b) {
						foundB = true;
						break;
					}
				}

			}

			if (foundB)
				break;

			vertexColors.put(current, 2);

		}

		Vertex pred = b;

		while (pred.id != a.id) {

			path.add(pred);
			pred = predecessors.get(pred);
		}
		path.add(a);

		Collections.reverse(path);

		return path;
	}

	public int getNumVertices() {
		return numVertices;
	}

	public int getNumEdges() {
		return numEdges;
	}

	public Vertex getVertex(int id) {
		if (vertices.size() > id)
			return vertices.get(id);
		else
			return null;
	}

	public Vertex getFarVertex(int v, int howFar) {
		Vertex vertex = vertices.get(v);
		Vertex last = vertex;

		for (int i = 0; i < howFar; ++i) {
			vertex = vertex.adjacencyList.get(0);
			if (vertex.id == last.id)
				vertex = vertex.adjacencyList.get(1);

			last = vertex;
		}

		return vertex;
	}

	public boolean removeEdge(int v, int w) {
		if (v > vertices.size() || w > vertices.size())
			return false;

		if (v == w)
			return false;

		Vertex vertexV = vertices.get(v);
		Vertex vertexW = vertices.get(w);

		boolean foundW = vertexV.adjacencyList.remove(vertexW);
		boolean foundV = vertexW.adjacencyList.remove(vertexV);
		return foundW && foundV;
	}

	public boolean addEdge(int v, int w) {

		if (v > vertices.size() || w > vertices.size())
			return false;

		Vertex vertexV = vertices.get(v);
		Vertex vertexW = vertices.get(w);

		boolean found = false;
		for (int i = 0; i < vertexV.adjacencyList.size(); ++i) {
			if (vertexV.adjacencyList.get(i) == vertexW)
				found = true;
		}

		if (!found) {
			vertexV.adjacencyList.add(vertexW);
			numEdges++;
		}

		for (int i = 0; i < vertexW.adjacencyList.size(); ++i) {
			if (vertexW.adjacencyList.get(i) == vertexV)
				found = true;
		}

		if (!found) {
			vertexW.adjacencyList.add(vertexV);
			numEdges++;
		}

		return !found;

	}

}
