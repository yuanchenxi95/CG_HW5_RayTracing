import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.texture.Texture;
import org.joml.*;
import sgraph.Nodes.INode;
import util.HitRecord;
import util.Ray;
import util.RaytraceUtil;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

/**
 * Created by ashesh on 9/18/2015.
 * <p>
 * The View class is the "controller" of all our OpenGL stuff. It cleanly encapsulates all our OpenGL functionality from the rest of Java GUI, managed
 * by the JOGLFrame class.
 */
public class View {
  private int WINDOW_WIDTH, WINDOW_HEIGHT;
  private Stack<Matrix4f> modelView;
  private Matrix4f projection, trackballTransform;
  private Matrix3f keyBoardTransform;
  private float trackballRadius;
  private Vector2f mousePos;
  private util.ObjectInstance meshObject;


  private util.ShaderProgram program;
  private int projectionLocation;
  private sgraph.IScenegraph scenegraph;
  private int angleOfRotation;

  private HashMap<Character, Boolean> cameraMoveCharMap;

  //INVARIANT lookAtPositionInit.equals(cameraPositionInit)
  private final Vector3f cameraPositionInit = new Vector3f(0, 300, 400);

  private Vector3f cameraPosition = new Vector3f(cameraPositionInit);
  Vector3f upVector;


  private enum CameraState {
    STATIONARY, ON_NODE, KEY_CONTROL
  }
  private CameraState cameraState = CameraState.STATIONARY;

  private enum RenderMode {
    RAYTRACE, LIVE
  }
  private RenderMode renderMode = RenderMode.LIVE;

  public View() {
    projection = new Matrix4f();
    modelView = new Stack<>();
    angleOfRotation = 0;
    trackballRadius = 300;

    resetMoveCamera();
  }

  private void resetMoveCamera() {
    // initialize keyboard control
    cameraMoveCharMap = new HashMap<Character, Boolean>();
    cameraMoveCharMap.put('w', false);
    cameraMoveCharMap.put('s', false);
    cameraMoveCharMap.put('a', false);
    cameraMoveCharMap.put('d', false);
    keyBoardTransform = new Matrix3f();
    upVector = new Vector3f(0, 1, 0);
    trackballTransform = new Matrix4f();
  }

  public void initScenegraph(GLAutoDrawable gla, InputStream in) throws Exception {
    GL3 gl = gla.getGL().getGL3();

    if (scenegraph != null)
      scenegraph.dispose();

    scenegraph = sgraph.SceneXMLReader.importScenegraph(in);

    sgraph.IScenegraphRenderer renderer = new sgraph.GL3ScenegraphRenderer();
    renderer.setContext(gla);
    renderer.initShaderProgram(program);
    scenegraph.setRenderer(renderer);
  }

  public void init(GLAutoDrawable gla) throws Exception {
    GL3 gl = gla.getGL().getGL3();


    //compile and make our shader program. Look at the ShaderProgram class for details on how this is done
    program = new util.ShaderProgram();

    program.createProgram(gl, "shaders/phong-multiple.vert", "shaders/phong-multiple.frag");


    //get input variables that need to be given to the shader program
    projectionLocation = program.getUniformLocation(gl, "projection");
  }


  public void draw(GLAutoDrawable gla) {

    // Clear ModelView stack; init with one matrix (identity)
    while (!modelView.empty()) {
      modelView.pop();
    }
    modelView.push(new Matrix4f());

    // ============================================
    // Animate Scenegraph (regardless of draw mode)
    // ============================================
//    scenegraph.animate(angleOfRotation);
//    angleOfRotation = (angleOfRotation + 1) % 360;

    // ===================================
    // Set camera matrix (M view <- world)
    // ===================================
    switch (cameraState) {
      case STATIONARY:
//        modelView.peek()
//                .lookAt(new Vector3f(0, 500, 500),
//                        new Vector3f(0, 50, 0),
//                        new Vector3f(0, 1, 0))
//                .mul(trackballTransform);

        // Sanity Check
        modelView.peek()
                .lookAt(new Vector3f(1.571E+2f, 3.681E+2f, 2.656E+2f),
                        new Vector3f(1.567E+2f, 3.674E+2f, 2.649E+2f),
                        new Vector3f(-1.860E-1f, 7.854E-1f, -5.903E-1f)).mul(trackballTransform);
        break;
      case ON_NODE:
        String cameraNodeName = "spiderA-root-spiderEye";

        INode cameraNode = scenegraph.getNodes().get(cameraNodeName);

        if (cameraNode == null) {
          break;
//          throw new NullPointerException(cameraNodeName + " is not in the scenegraph!");
        }
        Matrix4f objectToView = cameraNode.getObjectToViewTransform(new Matrix4f(), new Matrix4f(modelView.peek()));
        Matrix4f viewToObject = new Matrix4f(objectToView).invert();

        modelView.peek()
                .lookAt(new Vector3f(-1, 0, 0),
                        new Vector3f(-2, 0, 0),
                        new Vector3f(0, 1, 0))
                .mul(viewToObject);
        break;
      case KEY_CONTROL:
        moveCamera();

        Vector3f rotate = new Vector3f(0, 0, -1).mul(new Matrix3f(keyBoardTransform));
        upVector = new Vector3f(0, 1, 0).mul(new Matrix3f(keyBoardTransform));

        modelView.peek().lookAt(
                cameraPosition,
                new Vector3f(cameraPosition).add(rotate),
                upVector);
        break;
      default:
        throw new IllegalArgumentException("No such camera state!");
    }

    switch (renderMode) {
      case RAYTRACE:
        outputRaytraceResult(WINDOW_WIDTH, WINDOW_HEIGHT, modelView);
        renderMode = RenderMode.LIVE;
        break;
      case LIVE:
        drawOpenGL(gla);
        break;
      default:
        throw new RuntimeException("Render mode is invalid!");
    }

  }

  // Renders the scene graph with raytracing, write results to an image file
  private void outputRaytraceResult(int width, int height, Stack<Matrix4f> modelViewStack) {

    BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

    Color[][] traceResult = scenegraph.raytrace(width, height, modelViewStack);

    for (int i = 0; i < traceResult.length; i++) {
      for (int j = 0; j < traceResult[i].length; j++) {
        Color c = traceResult[i][j];
        output.setRGB(i, height - j - 1, c.getRGB());
      }
    }

    OutputStream outStream = null;

    try {
      outStream = new FileOutputStream("output/raytrace.png");
    } catch (FileNotFoundException e) {
      throw new IllegalArgumentException("Could not write raytraced image!");
    }

    try {
      ImageIO.write(output, "png", outStream);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not write raytraced image!");
    }
  }

  // Renders the scene graph in real time
  private void drawOpenGL(GLAutoDrawable gla) {
    GL3 gl = gla.getGL().getGL3();
    gl.glClearColor(0, 0, 0, 1);
    gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
    gl.glEnable(GL.GL_DEPTH_TEST);
    program.enable(gl);

    // ==============================
    // SHADER: Pass Model View Matrix
    // ==============================
    FloatBuffer fb = Buffers.newDirectFloatBuffer(16);
    gl.glUniformMatrix4fv(projectionLocation, 1, false, projection.get(fb));

//    gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL3.GL_LINE); //OUTLINES
    gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL3.GL_FILL); //FILLED

    // =================
    // Render Scenegraph
    // =================
    scenegraph.draw(modelView);

    /*
     * OpenGL batch-processes all its OpenGL commands.
     *  *The next command asks OpenGL to "empty" its batch of issued commands, i.e. draw
     *
     * This a non-blocking function. That is, it will signal OpenGL to draw, but won't wait for it to
     * finish drawing.
     *
     * If you would like OpenGL to start drawing and wait until it is done, call glFinish() instead.
     */
    gl.glFlush();

    program.disable(gl);
  }

  public void keyTyped(char c) {
    switch (c) {
      case 'r':
        trackballTransform = new Matrix4f().identity();
        cameraPosition = new Vector3f(cameraPositionInit);
        resetMoveCamera();
        break;
      case 'c':
        if (cameraState == cameraState.ON_NODE) {
          cameraState = cameraState.KEY_CONTROL;

        } else if (cameraState == cameraState.STATIONARY) {
          cameraState = cameraState.ON_NODE;
        } else if (cameraState == cameraState.KEY_CONTROL) {
          cameraState = cameraState.STATIONARY;
        }
        break;
      case KeyEvent.VK_ENTER:
        renderMode = RenderMode.RAYTRACE;
        break;
    }
  }

  public void keyPressed(char c) {
    if (cameraState.KEY_CONTROL == cameraState) {
      if (c == 'w' || c == 's' || c == 'a' || c == 'd') {
        cameraMoveCharMap.put(c, true);
      }
    }
  }

  public void keyReleased(char c) {
    if (cameraState.KEY_CONTROL == cameraState) {
      if (c == 'w' || c == 's' || c == 'a' || c == 'd') {
        cameraMoveCharMap.put(c, false);
      }
    }
  }

  private void moveCamera() {

    if (cameraState != cameraState.KEY_CONTROL) return;

    for (Character cameraMoveChar : cameraMoveCharMap.keySet()) {

      if (cameraMoveCharMap.get('w')) {
        Vector3f temp = new Vector3f(0, 0, -4);
        temp = temp.mul(keyBoardTransform);

        cameraPosition.add(temp);
      }
      if (cameraMoveCharMap.get('s')) {
        Vector3f temp = new Vector3f(0, 0, 4);
        temp = temp.mul(keyBoardTransform);

        cameraPosition.add(temp);
      }
      if (cameraMoveCharMap.get('a')) {
        Vector3f temp = new Vector3f(-4, 0, 0);
        temp = temp.mul(keyBoardTransform);

        cameraPosition.add(temp);
      }
      if (cameraMoveCharMap.get('d')) {
        Vector3f temp = new Vector3f(4, 0, 0);
        temp = temp.mul(keyBoardTransform);

        cameraPosition.add(temp);
      }
    }
  }


  public void mousePressed(int x, int y) {
    mousePos = new Vector2f(x, y);
  }

  public void mouseReleased(int x, int y) {
    // DO NOTHING
  }

  public void mouseDragged(int x, int y) {
    Vector2f delta;

    switch (cameraState) {
      case ON_NODE:
        // DO NOTHING
        break;
      case STATIONARY:
        delta = new Vector2f(x - mousePos.x, y - mousePos.y);
        mousePos = new Vector2f(x, y);

        trackballTransform = new Matrix4f()
                .rotate(delta.x / trackballRadius, 0, 1, 0)
                .rotate(delta.y / trackballRadius, 1, 0, 0).mul(trackballTransform);
        break;
      case KEY_CONTROL:
        delta = new Vector2f(x - mousePos.x, y - mousePos.y);
        mousePos = new Vector2f(x, y);

        keyBoardTransform =
                new Matrix3f().mul(keyBoardTransform).rotate(-delta.x / trackballRadius, 0, 1, 0)
                        .rotate(-delta.y / trackballRadius, 1, 0, 0);
        break;
    }
  }

  public void reshape(GLAutoDrawable gla, int x, int y, int width, int height) {
    GL gl = gla.getGL();
    WINDOW_WIDTH = width;
    WINDOW_HEIGHT = height;
    gl.glViewport(0, 0, width, height);

    projection = new Matrix4f().perspective((float) Math.toRadians(120.0f), (float) width / height, 0.1f, 10000.0f);
//        projection = new Matrix4f().ortho(-400,400,-400,400,0.1f,10000.0f);
  }

  public void dispose(GLAutoDrawable gla) {
    GL3 gl = gla.getGL().getGL3();
  }


}
