package visualizer;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import visualizer.Graph.Vertex;
import visualizer.Visualizer3D.MOVE_DIRECTION;

public class PacmanGame {

	private Clip audioClip = null;
	private Clip dotClip = null;
	private Clip dotClip2 = null;
	private Clip deathClip = null;
	private Clip winClip = null;
	private Clip energizerClip = null;
	private Clip eatGhostClip = null;

	private final int NUM_GHOSTS = 4;

	private Sprite playerSprite;
	private Sprite playerSpriteClosed;
	private Sprite levelSprite;
	private Sprite blackSprite;
	private Sprite dotSprite;
	private Sprite energizerSprite;
	private Sprite ghostScaredSprite;
	private Sprite eyesSprite;

	private Sprite[] ghosts = new Sprite[NUM_GHOSTS];
	private final int spriteDimension = 20;

	private ArrayList<PointFloat> dotPositions = new ArrayList<PointFloat>();
	private HashMap<Rectangle, PointFloat> dotAreas = new HashMap<Rectangle, PointFloat>();
	private ArrayList<PointFloat> energizerPositions = new ArrayList<PointFloat>();
	private HashMap<Rectangle, PointFloat> energizerAreas = new HashMap<Rectangle, PointFloat>();
	private int numRemainingDots = 0;

	private PointInt playerLastNode = null;
	private PointInt playerTargetNode = null;
	private int playerLastNodeId = 29;
	private int playerTargetNodeId = -1;
	private int[] cageNodes = { 68, 69, 70, 71 };
	private LinkedList<Integer> lastNodeIds = new LinkedList<Integer>();
	private boolean isPlayerDying = false;
	private int score = 0;
	private int highScore = 0;

	private PointInt[] ghostLastNodes = new PointInt[NUM_GHOSTS];
	private PointInt[] ghostTargetNodes = new PointInt[NUM_GHOSTS];
	private int[] ghostLastNodeIds = new int[NUM_GHOSTS];
	private int[] ghostTargetNodeIds = new int[NUM_GHOSTS];
	private boolean[] areGhostsTeleporting = new boolean[NUM_GHOSTS];
	private boolean[] areGhostsExiting = new boolean[NUM_GHOSTS];
	private boolean[] areGhostsDead = new boolean[NUM_GHOSTS];
	private MOVE_DIRECTION[] ghostDirections = new MOVE_DIRECTION[NUM_GHOSTS];
	private int ghostSpeed = 100;
	private int numActiveGhosts = 2;

	private int playerSpeed = 100;
	private boolean playerMoving = false;
	private boolean overrideDirection = false;
	private MOVE_DIRECTION newDirection = MOVE_DIRECTION.MOVE_LEFT;
	private MOVE_DIRECTION playerDirection = MOVE_DIRECTION.MOVE_LEFT;
	private boolean hasMouthOpen = true;
	private int numMouthFrames = 0;

	private boolean playingClipOne = true;
	private boolean gameStarted = false;
	private long gameStartTime = 0;
	private long ghostSpawnInterval = 5000;
	private long energizerStartTime = 0;

	private Graph finalGraph = null;

	private final int areaSize = 40;
	private final int dotAreaSize = 4;

	private Random random = new Random();

	private HashMap<Rectangle, PointInt> nodeAreas = new HashMap<Rectangle, PointInt>();

	private void computePointAreasFromGraph(Graph g) {

		for (Vertex v : g.getVertices()) {
			nodeAreas.put(getPointArea(v.data), v.data);
		}
	}

	private void loadHighScore() {
		try {
			Scanner scanner = new Scanner(new File("high_score.txt"));

			String toDecode = scanner.next();
			byte[] valueDecoded = Base64.getDecoder().decode(
					toDecode.getBytes());

			highScore = Integer.parseInt(new String(valueDecoded));
			scanner.close();
		} catch (IOException e) {
			highScore = 0;
		}
	}

	private void saveHighScore() throws IOException {
		FileWriter wr = new FileWriter(new File("high_score.txt"));
		String scoreString = Integer.toString(highScore);

		byte[] bytesEncoded = Base64.getEncoder()
				.encode(scoreString.getBytes());

		wr.write(new String(bytesEncoded));
		wr.close();
	}

	private Rectangle getDotArea(PointInt p) {
		Rectangle r = new Rectangle();

		r.x = (p.x / dotAreaSize) * dotAreaSize;
		r.y = (p.y / dotAreaSize) * dotAreaSize;
		r.width = dotAreaSize;
		r.height = dotAreaSize;
		return r;
	}

	private Rectangle getPointArea(PointInt data) {
		Rectangle r = new Rectangle();

		r.x = (data.x / areaSize) * areaSize;
		r.y = (data.y / areaSize) * areaSize;
		r.width = areaSize;
		r.height = areaSize;
		return r;
	}

	private static final int PROXIMTY_TRESHOLD = 4;

	private boolean arePointsNear(PointInt a, PointInt b) {
		boolean xNear = false, yNear = false;

		if (Math.abs(a.x - b.x) <= PROXIMTY_TRESHOLD)
			xNear = true;
		if (Math.abs(a.y - b.y) <= PROXIMTY_TRESHOLD)
			yNear = true;

		return xNear && yNear;
	}

	private void updateGhosts(float elapsedTime) {

		for (int i = 0; i < NUM_GHOSTS; ++i) {
			if (!isPlayerDying
					&& arePointsNear(ghosts[i].getPosition(),
							playerSprite.getPosition())) {

				if (energizerStartTime == 0) {
					isPlayerDying = true;

					deathClip.setFramePosition(0);
					deathClip.start();
					audioClip.stop();
					gameStarted = false;
				} else {
					if (!areGhostsDead[i]) {
						areGhostsDead[i] = true;
						score += 250;
						eatGhostClip.setFramePosition(0);
						eatGhostClip.start();
					}

				}
			} else if (arePointsNear(ghosts[i].getPosition(),
					ghostTargetNodes[i])) {

				if (i > numActiveGhosts - 1) {

					ghosts[i].updatePosition(ghostTargetNodes[i]);
					ghostLastNodeIds[i] = ghostTargetNodeIds[i];
					ghostLastNodes[i] = ghostTargetNodes[i];

					long deltaTime = System.currentTimeMillis() - gameStartTime;

					if (!areGhostsExiting[i]
							&& (deltaTime / ghostSpawnInterval >= i - 1)) {
						areGhostsExiting[i] = true;
					}

					if (areGhostsExiting[i]) {

						if (ghostLastNodeIds[i] == 71
								|| ghostLastNodeIds[i] == 70) {
							ghostTargetNodeIds[i] = 69;
							ghostTargetNodes[i] = finalGraph.getVertex(69).data;
						} else if (ghostLastNodeIds[i] == 69) {
							ghostTargetNodeIds[i] = 68;
							ghostTargetNodes[i] = finalGraph.getVertex(68).data;
						} else if (ghostLastNodeIds[i] == 68) {
							numActiveGhosts++;
							areGhostsExiting[i] = false;
						}
					} else {

						if (ghostLastNodeIds[i] == 71
								|| ghostLastNodeIds[i] == 69) {
							ghostTargetNodeIds[i] = 70;
							ghostTargetNodes[i] = finalGraph.getVertex(70).data;
						} else if (ghostLastNodeIds[i] == 70) {
							ghostTargetNodeIds[i] = 71;
							ghostTargetNodes[i] = finalGraph.getVertex(71).data;
						}
					}
				} else {

					if (ghostTargetNodeIds[i] == 68 && areGhostsDead[i]) {

						ghostTargetNodeIds[i] = 69;
						ghostTargetNodes[i] = finalGraph.getVertex(69).data;
						ghostLastNodeIds[i] = 68;
						ghostLastNodes[i] = finalGraph.getVertex(68).data;

					} else if (ghostTargetNodeIds[i] == 69 && areGhostsDead[i]) {

						areGhostsDead[i] = false;
						ghostTargetNodeIds[i] = 68;
						ghostTargetNodes[i] = finalGraph.getVertex(68).data;
						ghostLastNodeIds[i] = 69;
						ghostLastNodes[i] = finalGraph.getVertex(69).data;
					} else {

						areGhostsTeleporting[i] = false;
						if (ghostTargetNodeIds[i] == 66) {
							ghostTargetNodeIds[i] = 67;
							ghostTargetNodes[i] = finalGraph.getVertex(67).data;
							ghostLastNodeIds[i] = 66;
							ghostLastNodes[i] = finalGraph.getVertex(66).data;

							areGhostsTeleporting[i] = true;
						} else if (ghostTargetNodeIds[i] == 67) {
							ghostTargetNodeIds[i] = 66;
							ghostTargetNodes[i] = finalGraph.getVertex(66).data;
							ghostLastNodeIds[i] = 67;
							ghostLastNodes[i] = finalGraph.getVertex(67).data;
							areGhostsTeleporting[i] = true;
						}

						ghosts[i].updatePosition(ghostTargetNodes[i]);

						ArrayList<Vertex> path = null;

						int numTries = 0;

						int t = 0;

						while (path == null || path.isEmpty()
								|| (!areGhostsDead[i] && path.size() < 2)) {
							if (numTries < 5)
								t = calculateGhostNextTarget(i);
							else if (numTries != 0)
								t = random.nextInt(2) == 0 ? playerTargetNodeId
										: playerLastNodeId;
							numTries++;

							try {
								path = finalGraph
										.shortestPath(
												finalGraph
														.getVertex(ghostTargetNodeIds[i]),
												finalGraph.getVertex(t),
												(!areGhostsDead[i] && energizerStartTime != 0) ? null
														: finalGraph
																.getVertex(ghostLastNodeIds[i]));
							} catch (Exception e) {
								System.out.println("Exception in pathfinding algorithm: "
										+ e.getMessage());
								System.out.println("Target was: " + t);
								System.out.println(finalGraph
										.getVertex(ghostTargetNodeIds[i]).id);
								System.out.println(finalGraph.getVertex(t).id);
								System.out.println(finalGraph
										.getVertex(ghostLastNodeIds[i]).id);
							}

						}
						Vertex target;

						if (areGhostsDead[i] && path.size() == 1) {
							target = path.get(0);
						} else
							target = path.get(1);

						ghostLastNodes[i] = ghostTargetNodes[i];
						ghostLastNodeIds[i] = ghostTargetNodeIds[i];

						ghostTargetNodes[i] = target.data;
						ghostTargetNodeIds[i] = target.id;
					}
				}
			} else {
				ghostDirections[i] = getDirectionNodes(ghostLastNodes[i],
						ghostTargetNodes[i]);

				float speedMultiplier = 1.0f;
				if (areGhostsTeleporting[i] || energizerStartTime != 0)
					speedMultiplier = 0.8f;

				if (areGhostsDead[i])
					speedMultiplier = 2.0f;

				moveGhost(i, elapsedTime, speedMultiplier);
			}
		}
	}

	private int calculateGhostNextTarget(int ghostId) {

		if (areGhostsDead[ghostId]) {
			return finalGraph.getVertex(68).id;
		}

		if (energizerStartTime != 0) {
			return finalGraph.getFarVertex(playerTargetNodeId, 10).id;
		}

		int t = lastNodeIds.size() > 0 ? lastNodeIds.get(random
				.nextInt(lastNodeIds.size())) : playerLastNodeId;

		if (ghostId == 1 && random.nextInt(2) == 0 && playerTargetNodeId != -1)
			t = playerTargetNodeId;
		else if (ghostId == 2 && random.nextInt(2) == 0) {
			t = playerLastNodeId;
		} else if (ghostId == 3 || (ghostId == 0 && random.nextInt(2) == 0)) {
			while (t == ghostTargetNodeIds[ghostId]
					|| t == ghostLastNodeIds[ghostId]
					|| Arrays.binarySearch(cageNodes, t) > 0) {
				t = random.nextInt(finalGraph.numVertices + 1);
			}
		}

		return t;
	}

	public void moveGhost(int ghostId, float elapsedTime, float speedMultiplier) {
		float delta = elapsedTime * ghostSpeed * speedMultiplier;

		PointInt p = ghosts[ghostId].getPosition();

		if (ghostDirections[ghostId] == MOVE_DIRECTION.MOVE_RIGHT) {
			p.x += delta;
			if (p.x > ghostTargetNodes[ghostId].x)
				p.x = ghostTargetNodes[ghostId].x;
		} else if (ghostDirections[ghostId] == MOVE_DIRECTION.MOVE_LEFT) {
			p.x -= Math.floor(delta);
			if (p.x < ghostTargetNodes[ghostId].x)
				p.x = ghostTargetNodes[ghostId].x;
		} else if (ghostDirections[ghostId] == MOVE_DIRECTION.MOVE_DOWN) {
			p.y += delta;
			if (p.y > ghostTargetNodes[ghostId].y)
				p.y = ghostTargetNodes[ghostId].y;
		} else if (ghostDirections[ghostId] == MOVE_DIRECTION.MOVE_UP) {
			p.y -= Math.floor(delta);
			if (p.y < ghostTargetNodes[ghostId].y)
				p.y = ghostTargetNodes[ghostId].y;
		}

		ghosts[ghostId].updatePosition(p);
	}

	public void update(float elapsedTime) {
		if (playerTargetNodeId != -1 && gameStarted && !isPlayerDying)
			updateGhosts(elapsedTime);
	}

	public void render(float elapsedTime) {
		if (energizerStartTime != 0) {
			if (System.currentTimeMillis() - energizerStartTime > 8000) {
				energizerStartTime = 0;
				energizerClip.stop();
				audioClip.loop(Clip.LOOP_CONTINUOUSLY);
				audioClip.start();

			}
		}
		if (levelSprite != null)
			levelSprite.render();

		if (isPlayerDying) {
			playerSprite.changeOverrideColor(1.0f, 0.0f, 0.0f);
			playerSprite.setOverrideColor(true);
			playerSprite.render();
			playerSprite.setOverrideColor(false);
		}

		else {
			if (gameStarted) {
				if (playerMoving) {
					PointInt p = playerSprite.getPosition();

					Rectangle pArea = getDotArea(p);

					PointFloat dot = dotAreas.get(pArea);

					if (dot != null) {
						dotPositions.remove(dot);
						dotAreas.remove(pArea);
						numRemainingDots--;
						if (playingClipOne) {
							dotClip.setFramePosition(0);
							dotClip.start();
							playingClipOne = false;
						} else {
							dotClip2.setFramePosition(0);
							dotClip2.start();
							playingClipOne = true;
						}

						score += 100;

						if (numRemainingDots == 0) {
							gameStarted = false;
							winClip.setFramePosition(0);
							winClip.start();

						}
					} else {
						PointFloat energizer = energizerAreas.get(pArea);
						if (energizer != null) {
							energizerPositions.remove(energizer);
							energizerAreas.remove(pArea);

							score += 300;

							energizerStartTime = System.currentTimeMillis();

							energizerClip.setFramePosition(0);
							energizerClip.loop(Clip.LOOP_CONTINUOUSLY);
							energizerClip.start();
							audioClip.stop();
						}
					}

					boolean waitToMove = false;

					if (arePointsNear(p, playerTargetNode)) {
						playerMoving = false;
						playerSprite.updatePosition(playerTargetNode);

						lastNodeIds.add(playerLastNodeId);

						if (lastNodeIds.size() > 10)
							lastNodeIds.remove();

						playerLastNode = playerTargetNode;
						playerLastNodeId = playerTargetNodeId;

						if (playerLastNodeId == 66) {
							playerLastNode = finalGraph.getVertex(67).data;
							playerLastNodeId = 67;
						} else if (playerLastNodeId == 67) {
							playerLastNode = finalGraph.getVertex(66).data;
							playerLastNodeId = 66;
						}

						if (overrideDirection) {
							playerDirection = newDirection;
							overrideDirection = false;
						}
						movePlayer(playerDirection);
						playerSprite.updatePosition(playerLastNode);

						waitToMove = true;

					}
					if (!waitToMove && playerMoving) {
						float delta = elapsedTime * playerSpeed;

						if (playerDirection == MOVE_DIRECTION.MOVE_RIGHT) {
							p.x += delta;
							if (p.x > playerTargetNode.x)
								p.x = playerTargetNode.x;
						} else if (playerDirection == MOVE_DIRECTION.MOVE_LEFT) {
							p.x -= Math.floor(delta);
							if (p.x < playerTargetNode.x)
								p.x = playerTargetNode.x;
						} else if (playerDirection == MOVE_DIRECTION.MOVE_DOWN) {
							p.y += delta;
							if (p.y > playerTargetNode.y)
								p.y = playerTargetNode.y;
						} else if (playerDirection == MOVE_DIRECTION.MOVE_UP) {
							p.y -= Math.floor(delta);
							if (p.y < playerTargetNode.y)
								p.y = playerTargetNode.y;
						}

						playerSprite.updatePosition(p);
						playerSprite.updateTextureCoordinates(playerDirection);
						playerSpriteClosed
								.updateTextureCoordinates(playerDirection);
					}

				} else {
					playerSprite.updatePosition(playerLastNode);
				}
			}

			blackSprite.updatePosition(finalGraph.getVertex(67).data);
			blackSprite.render();
			blackSprite.updatePosition(finalGraph.getVertex(66).data);
			blackSprite.render();

			if (!playerMoving || hasMouthOpen) {
				playerSprite.render();
				if (numMouthFrames > 5) {
					hasMouthOpen = false;
					numMouthFrames = 0;
				} else
					numMouthFrames++;
			} else {
				playerSpriteClosed.updatePosition(playerSprite.getPosition());
				playerSpriteClosed.render();
				if (numMouthFrames > 5) {
					hasMouthOpen = true;
					numMouthFrames = 0;
				} else
					numMouthFrames++;
			}
		}

		for (int i = 0; i < NUM_GHOSTS; ++i) {

			if (areGhostsDead[i]) {
				eyesSprite.updatePosition(ghosts[i].getPosition());
				eyesSprite.render();
			} else if (energizerStartTime != 0) {
				float diff = System.currentTimeMillis() - energizerStartTime;
				if ((diff > 6500 && diff < 7000)
						|| (diff > 7500 && diff < 8000)) {
					ghostScaredSprite.setOverrideColor(true);
					ghostScaredSprite.changeOverrideColor(1.0f, 1.0f, 1.0f);
				}
				ghostScaredSprite.updatePosition(ghosts[i].getPosition());
				ghostScaredSprite.render();
				ghostScaredSprite.setOverrideColor(false);
			} else {
				ghosts[i].render();
			}
		}

		for (PointFloat p : dotPositions) {
			dotSprite.updatePosition(p);
			dotSprite.render();
		}

		for (PointFloat p : energizerPositions) {
			energizerSprite.updatePosition(p);
			energizerSprite.render();
		}
	}

	public int getScore() {
		return score;
	}

	public int getHighScore() {
		return highScore;
	}

	public void initialize(int width, int height) {

		try {
			finalGraph = new Graph();
			finalGraph.loadFromFile("graphNodes_final.txt");

			computePointAreasFromGraph(finalGraph);
		} catch (IOException e) {
			System.err.println("Cannot load movement nodes graph.");
			return;
		}

		loadHighScore();
		levelSprite = Sprite.getNewSprite("sprites/level.png", width, height,
				width, height);
		levelSprite.updatePosition(0, 0);
		levelSprite.setLayer(1);
		playerSprite = Sprite.getNewSprite("sprites/pacman_o.png",
				spriteDimension, spriteDimension, width, height);
		playerSprite.updatePosition(playerLastNode = finalGraph
				.getVertex(playerLastNodeId).data);

		playerSprite.setLayer(15);
		playerSpriteClosed = Sprite.getNewSprite("sprites/pacman_c.png",
				spriteDimension, spriteDimension, width, height);
		playerSpriteClosed.setLayer(15);

		ghosts[0] = Sprite.getNewSprite("sprites/blue.png", spriteDimension,
				spriteDimension, width, height);

		ghosts[0].setLayer(6);
		ghosts[1] = Sprite.getNewSprite("sprites/red.png", spriteDimension,
				spriteDimension, width, height);

		ghosts[1].setLayer(7);
		ghosts[2] = Sprite.getNewSprite("sprites/pink.png", spriteDimension,
				spriteDimension, width, height);
		ghosts[2].setLayer(8);

		ghosts[3] = Sprite.getNewSprite("sprites/orange.png", spriteDimension,
				spriteDimension, width, height);
		ghosts[3].setLayer(9);

		ghostScaredSprite = Sprite.getNewSprite("sprites/scared.png",
				spriteDimension, spriteDimension, width, height);
		ghostScaredSprite.setLayer(14);

		eyesSprite = Sprite.getNewSprite("sprites/eyes.png", spriteDimension,
				spriteDimension, width, height);
		eyesSprite.setLayer(3);

		blackSprite = Sprite.getNewSprite("sprites/black_texture.png",
				spriteDimension, spriteDimension, width, height);
		blackSprite.setLayer(4);

		dotSprite = Sprite.getNewSprite("sprites/dot.png",
				(int) (spriteDimension / 3), (int) (spriteDimension / 3),
				width, height);
		dotSprite.setLayer(4);
		energizerSprite = Sprite.getNewSprite("sprites/dot.png",
				(int) (spriteDimension / 1.5), (int) (spriteDimension / 1.5),
				width, height);
		energizerSprite.setLayer(4);

		numActiveGhosts = 2;

		loadSounds();
		resetPacman();
	}

	private void resetPacman() {

		try {
			if (score > highScore) {
				highScore = score;
				saveHighScore();
			}
		} catch (IOException e) {

		}

		lastNodeIds.clear();
		score = 0;

		dotAreas.clear();
		dotPositions.clear();
		energizerAreas.clear();
		energizerPositions.clear();
		computeDotPositions();

		playerLastNodeId = 29;
		playerLastNode = finalGraph.getVertex(playerLastNodeId).data;
		playerTargetNodeId = -1;
		playerMoving = false;
		playerSprite.updatePosition(playerLastNode);

		ghostLastNodeIds[0] = 69;
		ghostTargetNodeIds[0] = 69;
		ghosts[0]
				.updatePosition(ghostLastNodes[0] = ghostTargetNodes[0] = finalGraph
						.getVertex(ghostLastNodeIds[0]).data);
		ghosts[0]
				.updatePosition(finalGraph.getVertex(ghostLastNodeIds[0]).data);

		ghostLastNodeIds[1] = 69;
		ghostTargetNodeIds[1] = 69;
		ghosts[1]
				.updatePosition(ghostLastNodes[1] = ghostTargetNodes[1] = finalGraph
						.getVertex(ghostLastNodeIds[1]).data);
		ghosts[1]
				.updatePosition(finalGraph.getVertex(ghostLastNodeIds[1]).data);

		ghostLastNodeIds[2] = 70;
		ghostTargetNodeIds[2] = 70;
		ghosts[2]
				.updatePosition(ghostLastNodes[2] = ghostTargetNodes[2] = finalGraph
						.getVertex(ghostLastNodeIds[2]).data);
		ghosts[2]
				.updatePosition(finalGraph.getVertex(ghostLastNodeIds[2]).data);
		ghostLastNodeIds[3] = 71;
		ghostTargetNodeIds[3] = 71;
		ghosts[3]
				.updatePosition(ghostLastNodes[3] = ghostTargetNodes[3] = finalGraph
						.getVertex(ghostLastNodeIds[3]).data);
		ghosts[3]
				.updatePosition(finalGraph.getVertex(ghostLastNodeIds[3]).data);

		numActiveGhosts = 2;
	}

	private MOVE_DIRECTION getDirectionNodes(PointInt a, PointInt b) {
		if (a.x == b.x) {
			if (a.y > b.y)
				return MOVE_DIRECTION.MOVE_UP;
			else
				return MOVE_DIRECTION.MOVE_DOWN;
		} else if (a.y == b.y) {
			if (a.x > b.x)
				return MOVE_DIRECTION.MOVE_LEFT;
			else
				return MOVE_DIRECTION.MOVE_RIGHT;
		}

		return MOVE_DIRECTION.MOVE_RIGHT;
	}

	private boolean areDirectionsAgainst(MOVE_DIRECTION a, MOVE_DIRECTION b) {
		if ((a == MOVE_DIRECTION.MOVE_LEFT && b == MOVE_DIRECTION.MOVE_RIGHT)
				|| (b == MOVE_DIRECTION.MOVE_LEFT && a == MOVE_DIRECTION.MOVE_RIGHT))
			return true;
		if ((a == MOVE_DIRECTION.MOVE_UP && b == MOVE_DIRECTION.MOVE_DOWN)
				|| (b == MOVE_DIRECTION.MOVE_UP && a == MOVE_DIRECTION.MOVE_DOWN))
			return true;

		return false;
	}

	private boolean playerReversed(MOVE_DIRECTION current, MOVE_DIRECTION updated) {

		if (playerMoving && areDirectionsAgainst(current, updated)) {
			playerDirection = updated;
		} else
			return false;

		PointInt temp = playerLastNode;
		playerLastNode = playerTargetNode;
		playerTargetNode = temp;

		int x = playerLastNodeId;
		playerLastNodeId = playerTargetNodeId;
		playerTargetNodeId = x;

		return true;
	}

	private void movePlayer(MOVE_DIRECTION dir) {
		if (dir == MOVE_DIRECTION.MOVE_RIGHT) {
			playerMoveRight();
		} else if (dir == MOVE_DIRECTION.MOVE_LEFT) {
			playerMoveLeft();
		} else if (dir == MOVE_DIRECTION.MOVE_DOWN) {
			playerMoveDown();
		} else if (dir == MOVE_DIRECTION.MOVE_UP) {
			playerMoveUp();
		}
	}

	private void playerMoveRight() {
		Vertex vertex = finalGraph.getVertex(playerLastNodeId);

		ArrayList<Vertex> adjList = vertex.adjacencyList;

		for (Vertex adj : adjList) {
			if (Arrays.binarySearch(cageNodes, adj.id) < 0
					&& adj.data.x > playerLastNode.x) {
				playerTargetNode = adj.data;
				playerTargetNodeId = adj.id;
				playerMoving = true;
				playerDirection = MOVE_DIRECTION.MOVE_RIGHT;

				break;
			}
		}
	}

	private void playerMoveLeft() {
		Vertex vertex = finalGraph.getVertex(playerLastNodeId);

		ArrayList<Vertex> adjList = vertex.adjacencyList;

		for (Vertex adj : adjList) {
			if (Arrays.binarySearch(cageNodes, adj.id) < 0
					&& adj.data.x < playerLastNode.x) {
				playerTargetNode = adj.data;
				playerTargetNodeId = adj.id;
				playerMoving = true;
				playerDirection = MOVE_DIRECTION.MOVE_LEFT;

				break;
			}
		}

	}

	private void playerMoveDown() {

		Vertex vertex = finalGraph.getVertex(playerLastNodeId);

		ArrayList<Vertex> adjList = vertex.adjacencyList;

		for (Vertex adj : adjList) {
			if (Arrays.binarySearch(cageNodes, adj.id) < 0
					&& adj.data.y > playerLastNode.y) {
				playerTargetNode = adj.data;
				playerTargetNodeId = adj.id;
				playerMoving = true;
				playerDirection = MOVE_DIRECTION.MOVE_DOWN;

				break;
			}
		}

	}

	private void playerMoveUp() {

		Vertex vertex = finalGraph.getVertex(playerLastNodeId);

		ArrayList<Vertex> adjList = vertex.adjacencyList;

		for (Vertex adj : adjList) {
			if (Arrays.binarySearch(cageNodes, adj.id) < 0
					&& adj.data.y < playerLastNode.y) {
				playerTargetNode = adj.data;
				playerTargetNodeId = adj.id;
				playerMoving = true;
				playerDirection = MOVE_DIRECTION.MOVE_UP;

				break;
			}
		}

	}

	private void computeDotPositions() {

		ArrayList<PointFloat> dotsConnections = new ArrayList<PointFloat>();

		ArrayList<Vertex> vertices = finalGraph.getVertices();

		// Vertical.
		for (Vertex v : vertices) {
			if (Arrays.binarySearch(cageNodes, v.id) >= 0)
				continue;

			for (Vertex v2 : v.adjacencyList) {
				if (v2.data.y > v.data.y) {
					dotsConnections.add(new PointFloat(v.data.x, v.data.y));
					dotsConnections.add(new PointFloat(v2.data.x, v2.data.y));
					break;
				}
			}
		}

		for (int i = 0; i < dotsConnections.size(); i += 2) {
			PointFloat p1 = dotsConnections.get(i);
			PointFloat p2 = dotsConnections.get(i + 1);

			float x1 = p1.x, y1 = p1.y;
			float y2 = p2.y;

			float dist = y2 - y1;
			int n = (int) dist / 17;

			float space = dist / n;

			int j = 0;

			while (j < n - 1) {
				y1 += space;
				PointFloat pf = new PointFloat(x1, y1);
				dotPositions.add(pf);
				dotAreas.put(getDotArea(new PointInt((int) x1, (int) y1)), pf);
				j++;
			}
		}
		dotsConnections.clear();

		// Horizontal.
		for (Vertex v : vertices) {
			if (Arrays.binarySearch(cageNodes, v.id) >= 0 || v.id == 37
					|| v.id == 67)
				continue;

			for (Vertex v2 : v.adjacencyList) {
				if (v2.data.x > v.data.x) {
					dotsConnections.add(new PointFloat(v.data.x, v.data.y));
					dotsConnections.add(new PointFloat(v2.data.x, v2.data.y));
					break;
				}
			}
		}

		for (int i = 0; i < dotsConnections.size(); i += 2) {
			PointFloat p1 = dotsConnections.get(i);
			PointFloat p2 = dotsConnections.get(i + 1);

			float x1 = p1.x, y1 = p1.y;
			float x2 = p2.x;

			float dist = x2 - x1;
			int n = (int) dist / 17;

			float space = dist / n;

			int j = 0;

			while (j < n - 1) {

				x1 += space;
				PointFloat pf = new PointFloat(x1, y1);
				dotPositions.add(pf);
				dotAreas.put(getDotArea(new PointInt((int) x1, (int) y1)), pf);
				j++;
			}
		}
		dotsConnections.clear();

		for (Vertex v : vertices) {
			if (Arrays.binarySearch(cageNodes, v.id) >= 0 || v.id == 37
					|| v.id == 66 || v.id == 23 || v.id == 67)
				continue;

			float x1 = v.data.x, y1 = v.data.y;
			PointFloat pf = new PointFloat(x1, y1);
			if (v.id == 26 || v.id == 59 || v.id == 7 || v.id == 22) {

				energizerPositions.add(pf);
				energizerAreas.put(
						getDotArea(new PointInt((int) x1, (int) y1)), pf);

			} else {
				dotPositions.add(pf);
				dotAreas.put(getDotArea(new PointInt((int) x1, (int) y1)), pf);
			}
		}

		numRemainingDots = dotPositions.size();
	}

	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();

		if (gameStarted || (audioClip == null || !audioClip.isRunning())) {
			if (!gameStarted
					&& ((audioClip == null || !audioClip.isRunning())
							&& (deathClip == null || !deathClip.isRunning()) && ((winClip == null || !winClip
							.isRunning())))) {
				gameStarted = true;
				isPlayerDying = false;
				resetPacman();

				gameStartTime = System.currentTimeMillis();
				AudioInputStream audioIn;
				try {
					if (audioClip == null || !audioClip.isRunning()) {
						audioIn = AudioSystem.getAudioInputStream(new File(
								"sounds/siren.wav"));

						audioClip = AudioSystem.getClip();

						audioClip.open(audioIn);

						FloatControl gainControl = (FloatControl) audioClip
								.getControl(FloatControl.Type.MASTER_GAIN);
						gainControl.setValue(-5.0f); // Reduce volume.

						audioClip.loop(Clip.LOOP_CONTINUOUSLY);
						audioClip.start();
					}

				} catch (UnsupportedAudioFileException | IOException
						| LineUnavailableException e1) {

					e1.printStackTrace();
				}
			}

			if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_LEFT
					|| key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN) {
				PointInt p = playerSprite.getPosition();

				PointInt prox = nodeAreas.get(getPointArea(p));

				if (key == KeyEvent.VK_RIGHT) {
					newDirection = MOVE_DIRECTION.MOVE_RIGHT;
				}
				if (key == KeyEvent.VK_LEFT) {
					newDirection = MOVE_DIRECTION.MOVE_LEFT;
				}
				if (key == KeyEvent.VK_DOWN) {
					newDirection = MOVE_DIRECTION.MOVE_DOWN;
				}
				if (key == KeyEvent.VK_UP) {
					newDirection = MOVE_DIRECTION.MOVE_UP;
				}

				boolean reverseDirection = false;

				reverseDirection = playerReversed(playerDirection, newDirection);

				if (prox != null) {
					if (!reverseDirection) {
						if (!playerMoving) {
							playerDirection = newDirection;
							movePlayer(playerDirection);
						} else {
							overrideDirection = true;
						}
					}
				}
			}
		}
	}

	private void loadSounds() {
		AudioInputStream audioIn;
		try {
			if (audioClip == null) {
				audioIn = AudioSystem.getAudioInputStream(new File(
						"sounds/begin.wav"));

				audioClip = AudioSystem.getClip();

				audioClip.open(audioIn);

				FloatControl gainControl = (FloatControl) audioClip
						.getControl(FloatControl.Type.MASTER_GAIN);
				gainControl.setValue(-5.0f); // Reduce volume.

				audioClip.start();

				audioIn = AudioSystem.getAudioInputStream(new File(
						"sounds/chomp.wav"));
				dotClip = AudioSystem.getClip();
				dotClip.open(audioIn);
				gainControl = (FloatControl) dotClip
						.getControl(FloatControl.Type.MASTER_GAIN);
				gainControl.setValue(-5.0f); // Reduce volume.

				audioIn = AudioSystem.getAudioInputStream(new File(
						"sounds/chomp2.wav"));
				dotClip2 = AudioSystem.getClip();
				dotClip2.open(audioIn);
				gainControl = (FloatControl) dotClip2
						.getControl(FloatControl.Type.MASTER_GAIN);
				gainControl.setValue(-5.0f); // Reduce volume.

				audioIn = AudioSystem.getAudioInputStream(new File(
						"sounds/death.wav"));
				deathClip = AudioSystem.getClip();
				deathClip.open(audioIn);
				gainControl = (FloatControl) deathClip
						.getControl(FloatControl.Type.MASTER_GAIN);
				gainControl.setValue(-5.0f); // Reduce volume.

				audioIn = AudioSystem.getAudioInputStream(new File(
						"sounds/win.wav"));
				winClip = AudioSystem.getClip();
				winClip.open(audioIn);
				gainControl = (FloatControl) winClip
						.getControl(FloatControl.Type.MASTER_GAIN);
				gainControl.setValue(-5.0f); // Reduce volume.

				audioIn = AudioSystem.getAudioInputStream(new File(
						"sounds/energizer.wav"));
				energizerClip = AudioSystem.getClip();
				energizerClip.open(audioIn);
				gainControl = (FloatControl) energizerClip
						.getControl(FloatControl.Type.MASTER_GAIN);
				gainControl.setValue(-5.0f); // Reduce volume.

				audioIn = AudioSystem.getAudioInputStream(new File(
						"sounds/eat_ghost.wav"));
				eatGhostClip = AudioSystem.getClip();
				eatGhostClip.open(audioIn);
				gainControl = (FloatControl) eatGhostClip
						.getControl(FloatControl.Type.MASTER_GAIN);
				gainControl.setValue(-5.0f); // Reduce volume.
			}
		} catch (UnsupportedAudioFileException | IOException
				| LineUnavailableException e1) {
			System.out.println("Error in loading sounds.");
		}
	}

}
