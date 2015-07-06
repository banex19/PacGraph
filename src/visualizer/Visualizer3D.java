package visualizer;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;

import javax.swing.JFrame;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;

public class Visualizer3D extends GLJPanel implements GLEventListener,
		KeyListener, MouseListener, MouseMotionListener {

	private static final long serialVersionUID = 1L;

	private static GL2 gl = null;

	private float scaleValue = 1.0f;

	private static String osName = null;

	private static JFrame frame = null;

	// Program
	private int programModel = 0;
	private int programQuad = 0;

	private float projMatrix[] = new float[16];
	private float viewMatrix[] = new float[16];
	private float modelMatrix[] = new float[16];

	private Model model = null;

	private TextRenderer textRenderer = null;
	private TextRenderer scoreRenderer = null;

	private boolean renderingModel = true;
	private boolean funkyColors = false;
	private boolean loadNewModel = false;
	private boolean modelRenderingMode = true;
	private boolean activeRotation = true;
	private boolean editorMode = false;
	private PacmanEditor editor = null;
	private String[] newModelPaths = { null, null };

	private long textStartTime = 0;

	private long lastTime = 0;
	private long startTime = 0;

	private float rotationAmount = 0;

	public enum MOVE_DIRECTION {
		MOVE_LEFT, MOVE_RIGHT, MOVE_UP, MOVE_DOWN
	}

	private PacmanGame pacmanGame = null;

	private int screenWidth, screenHeight;

	private boolean loadPacmanFlag = false;
	private boolean loadEditorFlag = false;

	public static GL2 getGLPointer() {
		return gl;
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();

		if (key == KeyEvent.VK_P) {
			modelRenderingMode = !modelRenderingMode;
			editorMode = false;

			if (!e.isControlDown() && !modelRenderingMode) {
				if (pacmanGame == null) {
					loadPacmanFlag = true;
				}
			} else if (e.isControlDown() && !modelRenderingMode) {
				editorMode = true;
				loadEditorFlag = true;
			}

			return;
		}

		if (modelRenderingMode) {
			if (key == KeyEvent.VK_I) {
				scaleValue += 0.25f;
				if (scaleValue > 4.0f)
					scaleValue = 4.0f;
			} else if (key == KeyEvent.VK_K) {
				scaleValue -= 0.25f;
				if (scaleValue < 0.1f)
					scaleValue = 0.1f;
			} else if (key == KeyEvent.VK_N) {
				scaleValue = 0.01f;
			} else if (key == KeyEvent.VK_M) {
				scaleValue = 15.0f;
			} else if (key == KeyEvent.VK_ENTER) {
				renderingModel = !renderingModel;
			} else if (key == KeyEvent.VK_C) {
				funkyColors = !funkyColors;
			} else if (key == KeyEvent.VK_R) {
				activeRotation = !activeRotation;
			}
		} else {
			if (editorMode) {

				// Pass down to the editor the current key pressed.
				editor.keyPressed(e);

			} else {
				if (pacmanGame != null)
					pacmanGame.keyPressed(e);
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {

	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	public void update(float elapsedTime) {
		if (loadNewModel) {

			loadModel(newModelPaths[0], newModelPaths[1]);

			loadNewModel = false;
		}

		// Model matrix.
		if (activeRotation) {
			rotationAmount += ((elapsedTime / 2) * 2 * Math.PI);

			if (rotationAmount > (float) (10000 * Math.PI))
				rotationAmount = 0.0f;
			GraphicsMath.setRotationMatrixY(modelMatrix, rotationAmount);
		}

		if (!modelRenderingMode) {
			if (pacmanGame != null)
				pacmanGame.update(elapsedTime);
		}
	}

	@Override
	public void display(GLAutoDrawable glDrawable) {

		GL2 gl = glDrawable.getGL().getGL2();

		long currentTime = System.currentTimeMillis();
		float elapsedTime = (currentTime - lastTime) * 0.001f;

		update(elapsedTime);

		if (loadPacmanFlag) {
			pacmanGame = new PacmanGame();

			pacmanGame.initialize(frame.getWidth(), 710);

			loadPacmanFlag = false;
		} else if (loadEditorFlag) {
			editor = new PacmanEditor(screenWidth, screenHeight);
			loadEditorFlag = false;
		}

		// Clear background.
		if (funkyColors) {
			float r = (float) (Math.sin((currentTime - startTime) / 1000.0f));
			float g = (float) (Math.cos((currentTime - startTime) / 1000.0f));
			float b = (float) (Math.sin(r + g));

			gl.glClearColor(r, g, b, 1.0f);
		} else {
			gl.glClearColor(0.55f, 0.55f, 0.55f, 1.0f);
		}

		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

		// Update camera.
		if (modelRenderingMode) {
			GraphicsMath.setCamera(5.5f, 5.5f, 5, 0.0f, 0.0f, 0,
					this.viewMatrix);

			gl.glUseProgram(programModel);

			// Set uniforms.
			gl.glUniformMatrix4fv(RenderingGlobals.projMatrixLoc, 1, false,
					this.projMatrix, 0);
			gl.glUniformMatrix4fv(RenderingGlobals.viewMatrixLoc, 1, false,
					this.viewMatrix, 0);
			gl.glUniformMatrix4fv(RenderingGlobals.modelMatrixLoc, 1, false,
					this.modelMatrix, 0);
			gl.glUniform1f(RenderingGlobals.scaleLoc, scaleValue);

			if (renderingModel) {
				if (model != null)
					model.render();
			}

			float diffTime = (System.currentTimeMillis() - textStartTime) / 1000.0f;
			if (diffTime < 5.0f) {

				gl.glUseProgram(0);
				textRenderer.beginRendering(frame.getWidth(), 710, true);
				textRenderer.setUseVertexArrays(true);
				float alpha = 1.0f - diffTime / 2.5f;

				textRenderer.setColor(1.0f, 1.0f, 1.0f, alpha);
				PointInt p = getPointBottomLeft(frame.getWidth(), 710, 10, 30);
				textRenderer.draw("Loaded model " + newModelPaths[0], p.x, p.y);

				textRenderer.endRendering();
				gl.glUseProgram(programModel);

			}
		} else {

			GraphicsMath.setCamera(5.5f, 5.5f, 5, 0.0f, 0.0f, 0,
					this.viewMatrix);

			gl.glUseProgram(programQuad);

			if (pacmanGame != null)
				pacmanGame.render(elapsedTime);

			if (editorMode) {
				editor.render();
			}

			// Render score as text.
			if (pacmanGame != null) {
				gl.glUseProgram(0);
				scoreRenderer.beginRendering(frame.getWidth(), 710, true);
				scoreRenderer.setUseVertexArrays(true);

				scoreRenderer.setColor(1.0f, 1.0f, 1.0f, 1.0f);
				PointInt p1 = getPointBottomLeft(frame.getWidth(), 710, 35, 140);
				scoreRenderer.draw("" + pacmanGame.getScore(), p1.x, p1.y);
				p1 = getPointBottomLeft(frame.getWidth(), 710, 900, 140);
				scoreRenderer.draw("" + pacmanGame.getHighScore(), p1.x, p1.y);

				scoreRenderer.endRendering();
				gl.glUseProgram(programQuad);
			}
		}

		lastTime = currentTime;

		int error = gl.glGetError();

		while (error != 0) {
			System.out.println("Error: " + error);
			error = gl.glGetError();
		}
	}

	@Override
	public void dispose(GLAutoDrawable glDrawable) {
	}

	@Override
	public void init(GLAutoDrawable glDrawable) {
		GL2 gl = glDrawable.getGL().getGL2();

		Visualizer3D.gl = gl;

		textRenderer = new TextRenderer(new Font("Arial", Font.PLAIN, 18));
		scoreRenderer = new TextRenderer(new Font("Arial", Font.BOLD, 20));

		loadModel("models/ship.obj", "models/ship.png");
		newModelPaths[0] = new File("models/ship.obj").getAbsolutePath();

		screenWidth = glDrawable.getSurfaceWidth();
		screenHeight = glDrawable.getSurfaceHeight();

		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);

		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

		if (!osName.equals("mac")) {
			programModel = ShaderLoader.createProgram(gl,
					"simpleVertexShader.txt", "simplePixelShader.txt");
			programQuad = ShaderLoader.createProgram(gl, "quadVertex.txt",
					"quadPixel.txt");
		} else {
			programModel = ShaderLoader.createProgram(gl,
					"simpleVertexShader_mac.txt", "simplePixelShader_mac.txt");
			programQuad = ShaderLoader.createProgram(gl, "quadVertex_mac.txt",
					"quadPixel_mac.txt");
		}

		RenderingGlobals.overrideColorLoc = gl.glGetUniformLocation(
				programQuad, "overrideColor");
		RenderingGlobals.overrideColorVecLoc = gl.glGetUniformLocation(
				programQuad, "overrideColorRGB");

		RenderingGlobals.textureLocModel = gl.glGetUniformLocation(
				programModel, "texUnit");
		RenderingGlobals.textureLocSprite = gl.glGetUniformLocation(
				programQuad, "texUnit");

		RenderingGlobals.projMatrixLoc = gl.glGetUniformLocation(programModel,
				"projMatrix");
		RenderingGlobals.viewMatrixLoc = gl.glGetUniformLocation(programModel,
				"viewMatrix");
		RenderingGlobals.modelMatrixLoc = gl.glGetUniformLocation(programModel,
				"modelMatrix");
		RenderingGlobals.scaleLoc = gl.glGetUniformLocation(programModel,
				"scale");

		RenderingGlobals.traslMatrixLoc = gl.glGetUniformLocation(programQuad,
				"traslMatrix");
		RenderingGlobals.scaleMatrixLoc = gl.glGetUniformLocation(programQuad,
				"scaleMatrix");

		if (!osName.equals("mac")) {
			gl.glBindFragDataLocation(programModel, 0, "outColor");
			gl.glBindFragDataLocation(programQuad, 0, "outColor");
		}

		gl.glUseProgram(programModel);

		startTime = System.currentTimeMillis();

		int error = gl.glGetError();

		while (error != 0) {
			System.out.println("Error: " + error);
			error = gl.glGetError();
		}
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {

		float ratio;

		if (height == 0)
			height = 1;
		if (width == 0)
			width = 1;

		ratio = (1.0f * width) / height;
		this.projMatrix = GraphicsMath.buildProjectionMatrix(60.0f, ratio,
				1.0f, 30.0f, this.projMatrix);

	}

	private PointInt getPointBottomLeft(int width, int height, int x, int y) {
		return new PointInt(x, height - y);
	}

	public void issueLoadModel(String modelFilename, String textureFilename) {
		loadNewModel = true;
		newModelPaths[0] = modelFilename;
		newModelPaths[1] = textureFilename;
	}

	private void loadModel(String modelFilename, String textureFilename) {

		if (model != null)
			model.dispose();

		ObjLoader loader = new ObjLoader();
		model = null;
		model = loader.loadModelFromFile(modelFilename);
		model.loadAndBindTexture(textureFilename);

		textStartTime = System.currentTimeMillis();
	}

	public Visualizer3D(GLCapabilities capabilities) {
		super(capabilities);
		setPreferredSize(new Dimension(1024, 768));
		addGLEventListener(this);
		addKeyListener(this);
		addMouseListener(this);
	}

	public static void main(String[] args) {

		GLProfile glp = GLProfile.get(GLProfile.GL2);

		osName = System.getProperty("os.name");

		osName = osName.substring(0, 3).toLowerCase();

		if (args.length > 0) {
			if (args[0].toLowerCase().equals("mac"))
				osName = "mac";
		}

		JFrame window = new JFrame("Qualità");
		GLCapabilities caps = new GLCapabilities(glp);

		caps.setSampleBuffers(true);
		caps.setNumSamples(2);

		GLCanvas canvas = new GLCanvas(caps);
		Visualizer3D mainApplication = new Visualizer3D(caps);

		FPSAnimator animator = new FPSAnimator(60);
		animator.add(canvas);
		animator.start();

		canvas.addGLEventListener(mainApplication);
		canvas.addKeyListener(mainApplication);
		canvas.addMouseListener(mainApplication);
		canvas.addMouseMotionListener(mainApplication);
		canvas.requestFocus();

		new MenuBar(mainApplication, window);

		window.pack();
		window.setLocation(50, 50);
		window.setSize(1024, 768);
		window.setResizable(false);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setVisible(true);
		window.add(canvas);

		frame = window;

		canvas.requestFocusInWindow();

	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (!modelRenderingMode) {

			if (editorMode) {
				editor.mousePressed(e);
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {

		if (!modelRenderingMode) {

			if (editorMode) {
				editor.mouseMoved(e);
			}
		}
	}
}
