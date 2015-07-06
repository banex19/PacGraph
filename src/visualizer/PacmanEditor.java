package visualizer;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import javax.swing.SwingUtilities;

import visualizer.Graph.Vertex;

public class PacmanEditor {

	private Sprite nodeSprite = null;
	private Sprite levelSprite = null;
	private int spriteDimension = 20;

	public PacmanEditor() {

	}

	public PacmanEditor(int width, int height) {
		nodeSprite = Sprite.getNewSprite("sprites/node.png", spriteDimension,
				spriteDimension, width, height);
		nodeSprite.setLayer(6);
		nodeSprite.changeOverrideColor(1.0f, 0.0f, 0.0f);

		levelSprite = Sprite.getNewSprite("sprites/level_guide.png", width,
				height, width, height);
		levelSprite.setLayer(1);
		levelSprite.updatePosition(0, 0);
	}

	private ArrayList<PointInt> nodePoints = new ArrayList<PointInt>();
	private HashMap<PointInt, Integer> nodeIds = new HashMap<PointInt, Integer>();

	private HashMap<PointInt, Boolean> coloredNodes = new HashMap<PointInt, Boolean>();
	private HashMap<PointInt, Boolean> selectedNodes = new HashMap<PointInt, Boolean>();
	private HashMap<PointInt, Integer> connectedNodes = new HashMap<PointInt, Integer>();
	private ArrayList<PointInt> selectedNodesArray = new ArrayList<PointInt>();

	private HashMap<Rectangle, PointInt> nodeAreas = new HashMap<Rectangle, PointInt>();

	private Graph nodeGraph = new Graph();

	private boolean selectionMode = false;

	public void render() {

		levelSprite.render();

		for (PointInt p : nodePoints) {
			if (nodeSprite != null) {
				nodeSprite.updatePosition(p.x - spriteDimension / 2, p.y
						- spriteDimension / 2);

				if (!selectionMode) {
					if (coloredNodes.get(p) != null) {
						nodeSprite.changeOverrideColor(1, 0, 0);
						nodeSprite.setOverrideColor(true);
					}
				}

				Integer numConnections = connectedNodes.get(p);

				if (numConnections != null) {

					float color = 1.0f / numConnections;
					if (numConnections < 4)
						nodeSprite.changeOverrideColor(color, color, color);
					else
						nodeSprite.changeOverrideColor(1, 0, 0);

					nodeSprite.setOverrideColor(true);
				}
				if (selectedNodes.get(p) != null) {
					nodeSprite.changeOverrideColor(0, 0, 1);
					nodeSprite.setOverrideColor(true);
				}

				nodeSprite.render();
				nodeSprite.setOverrideColor(false);
			}
		}
	}

	public void keyPressed(KeyEvent e) {

		int key = e.getKeyCode();

		if (key == KeyEvent.VK_A) {
			selectedNodes.clear();
			selectedNodesArray.clear();
		} else if (key == KeyEvent.VK_R) {
			if (selectionMode) {
				try {
					if (e.isControlDown()) {
						nodeGraph.loadEdgesFromFile("graphNodes.txt");
						getConnectedFromGraph();
					} else {
						nodeGraph.loadFromFile("graphNodes.txt");
						getConnectedFromGraph();
					}
				} catch (IOException e1) {

					e1.printStackTrace();
				}
			}
		} else if (key == KeyEvent.VK_C) {
			if (selectedNodesArray.size() == 2) {

				PointInt s0 = selectedNodesArray.get(0);
				PointInt s1 = selectedNodesArray.get(1);

				int v = nodeIds.get(s0);
				int w = nodeIds.get(s1);
				boolean newEdge = nodeGraph.addEdge(v, w);

				if (newEdge) {
					Integer c0 = connectedNodes.get(s0);
					Integer c1 = connectedNodes.get(s1);
					if (c0 != null)
						connectedNodes.put(s0, c0 + 1);
					else
						connectedNodes.put(s0, 1);
					if (c1 != null)
						connectedNodes.put(s1, c1 + 1);
					else
						connectedNodes.put(s1, 1);
				}

				selectedNodes.clear();
				selectedNodesArray.clear();
			}
		} else if (key == KeyEvent.VK_B) {

			if (selectedNodesArray.size() == 2) {

				PointInt s0 = selectedNodesArray.get(0);
				PointInt s1 = selectedNodesArray.get(1);

				int v = nodeIds.get(s0);
				int w = nodeIds.get(s1);
				boolean removed = nodeGraph.removeEdge(v, w);

				if (removed) {
					Integer c0 = connectedNodes.get(s0);
					Integer c1 = connectedNodes.get(s1);
					if (c0 != null) {
						if (c0 - 1 > 0)
							connectedNodes.put(s0, c0 - 1);
						else
							connectedNodes.remove(s0);
					}
					if (c1 != null) {
						if (c1 - 1 > 0)
							connectedNodes.put(s1, c1 - 1);
						else
							connectedNodes.remove(s1);

					}
				}

				selectedNodes.clear();
				selectedNodesArray.clear();
			}
		} else if (key == KeyEvent.VK_I) {
			if (selectedNodesArray.size() == 1) {

				PointInt s0 = selectedNodesArray.get(0);

				int v = nodeIds.get(s0);

				Vertex vertex = nodeGraph.getVertices().get(v);

				System.out.println("Vertex id: " + v);
				System.out.println("Vertex point: " + vertex.data);

				ArrayList<Vertex> adjList = vertex.adjacencyList;

				System.out.print("Vertex adjacency list: ");

				for (int i = 0; i < adjList.size(); ++i) {
					System.out.print(adjList.get(i).id + " ");
				}

				System.out.println();

			}
		} else if (key == KeyEvent.VK_E) {
			selectionMode = !selectionMode;
			coloredNodes.clear();

			if (selectionMode) {
				connectedNodes.clear();
				nodeGraph = new Graph();

				for (PointInt p : nodePoints) {
					int id = nodeGraph.addVertex(p);
					nodeIds.put(p, id);
				}
			}
		} else if (key == KeyEvent.VK_MINUS) {
			if (nodePoints.size() > 0)
				nodePoints.remove(nodePoints.size() - 1);
			System.out.println("Number of nodes: " + nodePoints.size());
		} else if (key == KeyEvent.VK_T) {
			try {

				if (e.isControlDown() && nodeGraph != null) {
					nodeGraph.saveToFile("graphNodes.txt");
					System.out.println("Saved connections to file.");
				}

			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else if (key == KeyEvent.VK_S) {

			if (nodePoints.size() == 0) {
				System.out.println("No nodes to save.");
				return;
			}
			try {
				FileOutputStream stream = new FileOutputStream("nodes.txt");

				BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(stream));

				for (PointInt p : nodePoints) {
					writer.write(p.x + " " + p.y + "\r\n");
				}

				writer.flush();
				writer.close();
				stream.close();

			} catch (IOException ex) {
			}

			System.out.println("Saved nodes to file.");
		}

		else if (key == KeyEvent.VK_L) {

			try {

				FileInputStream stream = new FileInputStream("nodes.txt");

				BufferedReader reader = new BufferedReader(
						new InputStreamReader(stream));

				nodePoints = new ArrayList<PointInt>();

				while (reader.ready()) {

					String line = reader.readLine();

					Scanner tokenizer = new Scanner(line);

					int x = tokenizer.nextInt();
					int y = tokenizer.nextInt();

					PointInt p = new PointInt(x, y);
					nodePoints.add(p);

					tokenizer.close();

				}

				reader.close();
				stream.close();

				computePointAreas();

			} catch (IOException ex) {
				System.out.println("Cannot load nodes from file.");
			}

			System.out.println("Loaded nodes from file.");
		}
	}

	private void computePointAreas() {
		for (PointInt p : nodePoints) {
			nodeAreas.put(getPointArea(p), p);
		}
	}

	private final int areaSize = 40;

	public Rectangle getPointArea(PointInt p) {
		Rectangle r = new Rectangle();

		r.x = (p.x / areaSize) * areaSize;
		r.y = (p.y / areaSize) * areaSize;
		r.width = areaSize;
		r.height = areaSize;
		return r;
	}

	private void getConnectedFromGraph() {
		if (nodeGraph != null) {
			ArrayList<Vertex> vertices = nodeGraph.getVertices();
			for (int i = 0; i < vertices.size(); ++i) {
				Vertex vertex = vertices.get(i);
				ArrayList<Vertex> adjList = vertex.adjacencyList;
				if (adjList.size() > 0)
					connectedNodes.put(vertex.data, adjList.size());

			}
		}
	}

	public void mousePressed(MouseEvent e) {

		if (!selectionMode) {
			if (SwingUtilities.isLeftMouseButton(e)) {
				PointInt p = new PointInt(e.getPoint().x, e.getPoint().y);
				nodePoints.add(p);
				nodeAreas.put(getPointArea(p), p);
				System.out.println(e.getPoint());
			} else if (SwingUtilities.isRightMouseButton(e)) {
				PointInt p = new PointInt(e.getPoint().x, e.getPoint().y);

				PointInt actual = nodeAreas.get(getPointArea(p));

				if (actual != null) {
					nodePoints.remove(actual);
					nodeAreas.remove(getPointArea(actual));
				}
			}
		} else {
			if (SwingUtilities.isLeftMouseButton(e)) {
				PointInt p = new PointInt(e.getPoint().x, e.getPoint().y);

				if (selectedNodesArray.size() < 2) {
					Rectangle area = getPointArea(p);
					PointInt node = nodeAreas.get(area);
					if (node != null) {
						selectedNodes.put(node, true);
						selectedNodesArray.add(node);
					}
				}
			}
		}

	}

	public void mouseMoved(MouseEvent e) {

		if (!selectionMode) {
			coloredNodes.clear();

			int x = e.getX(), y = e.getY();
			for (PointInt p : nodePoints) {
				if (p.x == x || p.y == y) {
					coloredNodes.put(p, true);

				}
			}
		}
	}
}
