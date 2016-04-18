package sgraph;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import sgraph.Nodes.INode;
import util.*;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * A specific implementation of this scene graph. This implementation is still independent
 * of the rendering technology (i.e. OpenGL)
 *
 * @author Amit Shesh
 */
public class Scenegraph implements IScenegraph {
  /**
   * The root of the scene graph tree
   */
  protected INode root;

  /**
   * A map to store the (name,mesh) pairs. A map is chosen for efficient search
   */
  protected Map<String, util.PolygonMesh> meshes;

  /**
   * A map to store the (name,textureImage) pairs. A map is chose for efficient search
   */
  protected Map<String, util.TextureImage> textureImages;

  /**
   * A map to store the (name,node) pairs. A map is chosen for efficient search
   */
  protected Map<String, INode> nodes;

  /**
   * The associated renderer for this scene graph. This must be set before attempting to
   * render the scene graph
   */
  protected IScenegraphRenderer renderer;


  public Scenegraph() {
    root = null;
    meshes = new TreeMap<String, util.PolygonMesh>();
    nodes = new TreeMap<String, INode>();
    textureImages = new TreeMap<String, TextureImage>();
  }

  public void dispose() {
    renderer.dispose();
  }

  /**
   * Sets the renderer, and then adds all the meshes to the renderer.
   * This function must be called when the scene graph is complete, otherwise not all of its
   * meshes will be known to the renderer
   *
   * @param renderer The {@link IScenegraphRenderer} object that will act as its renderer
   * @throws Exception
   */
  @Override
  public void setRenderer(IScenegraphRenderer renderer) throws Exception {
    this.renderer = renderer;
    List<Light> lol = this.getRoot().getAllLights(new Matrix4f());
    this.renderer.addLights(lol);

    //now add all the meshes
    for (String meshName : meshes.keySet()) {
      this.renderer.addMesh(meshName, meshes.get(meshName));
    }
    for (String texName : textureImages.keySet()) {
      System.out.println();
      this.renderer.addTexture(texName, textureImages.get(texName));
    }


  }


  /**
   * Set the root of the scenegraph, and then pass a reference to this scene graph object
   * to all its node. This will enable any node to call functions of its associated scene graph
   *
   * @param root
   */

  @Override
  public void makeScenegraph(INode root) {
    this.root = root;
    this.root.setScenegraph(this);

  }

  /**
   * Draw this scene graph. It delegates this operation to the renderer
   *
   * @param modelView
   */
  @Override
  public void draw(Stack<Matrix4f> modelView) {


    if ((root != null) && (renderer != null)) {
      renderer.draw(root, modelView);
    }
  }


  @Override
  public void addPolygonMesh(String name, util.PolygonMesh mesh) {
    meshes.put(name, mesh);
  }

  @Override
  public void addTextureImage(String name, TextureImage textureImage) {
    textureImages.put(name, textureImage);
  }

  boolean trainTransformFlag = true;

  // given time from 0 - 360
  @Override
  public void animate(float time) {

    float radius = 300f;
    //////////////////////////////////////
    // for test head part light
    if (trainTransformFlag) {
      if (time * 2 == 718) {
        trainTransformFlag = false;
      }
      nodes.get("train-transform").setAnimationTransform(
              new Matrix4f().translate(time * 2, 0, 0));
    } else {
      if (time * 2 == 718) {
        trainTransformFlag = true;
      }
      nodes.get("train-transform").setAnimationTransform(
              new Matrix4f().translate(720 - time * 2, 0, 0));
    }

//        nodes.get("train-transform").setAnimationTransform(
//                new Matrix4f().rotate((float) Math.toRadians(time), 0, 1, 0));
//                new Matrix4f().translate(0.5f * time, 0, 0));

    //////////////////////////////////////


    float offset = (float) (Math.PI / 2);

    nodes.get("spiderB-transform").setAnimationTransform(new Matrix4f()
            .rotate((float) Math.toRadians(time) + offset, 0, 1, 0)
            .translate(radius, 0, 0)
            .rotate((float) Math.toRadians(-90), 0, 1, 0));

    nodes.get("spiderA-transform").setAnimationTransform(new Matrix4f()
            .rotate((float) Math.toRadians(time), 0, 1, 0)
            .translate(radius, 0, 0)
            .rotate((float) Math.toRadians(-90), 0, 1, 0));
    // print all the names in the map
//        for(INode n : nodes.values())
//            System.out.println(n.getName());

    animateSpiderLegs();
  }

  // Current time count
  int count = 0;

  // Maximum time count before returning to 0
  int loopLimit = 100;

  // Time offset between each leg of rows
  int offset = 20;

  /**
   * Animate the movement of spider legs
   */
  private void animateSpiderLegs() {

    count++;
    // Animation Percentages from 0 to 1.
    float row0_percentage = (count % loopLimit) / (float) loopLimit;
    float row1_percentage = ((count + offset) % loopLimit) / (float) loopLimit;
    float row2_percentage = ((count + offset * 2) % loopLimit) / (float) loopLimit;
    float row3_percentage = ((count + offset * 3) % loopLimit) / (float) loopLimit;
    // Using equation: rotationAmount = 1/8 * sin(theta * 360 - 180)
    // Both spiders' Left Side
    nodes.get("spiderA-root-legLeft0").setAnimationTransform(new Matrix4f()
            .rotate((float) Math.sin(Math.toRadians(row0_percentage * 360 - 180)) / 8f, 0, 1, 0));
    nodes.get("spiderA-root-legLeft1").setAnimationTransform(new Matrix4f()
            .rotate((float) Math.sin(Math.toRadians(row1_percentage * 360 - 180)) / 8f, 0, 1, 0));
    nodes.get("spiderA-root-legLeft2").setAnimationTransform(new Matrix4f()
            .rotate((float) Math.sin(Math.toRadians(row2_percentage * 360 - 180)) / 8f, 0, 1, 0));
    nodes.get("spiderA-root-legLeft3").setAnimationTransform(new Matrix4f()
            .rotate((float) Math.sin(Math.toRadians(row3_percentage * 360 - 180)) / 8f, 0, 1, 0));

    nodes.get("spiderB-root-legLeft0").setAnimationTransform(new Matrix4f()
            .rotate((float) Math.sin(Math.toRadians(row0_percentage * 360 - 180)) / 8f, 0, 1, 0));
    nodes.get("spiderB-root-legLeft1").setAnimationTransform(new Matrix4f()
            .rotate((float) Math.sin(Math.toRadians(row1_percentage * 360 - 180)) / 8f, 0, 1, 0));
    nodes.get("spiderB-root-legLeft2").setAnimationTransform(new Matrix4f()
            .rotate((float) Math.sin(Math.toRadians(row2_percentage * 360 - 180)) / 8f, 0, 1, 0));
    nodes.get("spiderB-root-legLeft3").setAnimationTransform(new Matrix4f()
            .rotate((float) Math.sin(Math.toRadians(row3_percentage * 360 - 180)) / 8f, 0, 1, 0));

    // Both spiders' Right side
    nodes.get("spiderA-root-legRight0").setAnimationTransform(new Matrix4f()
            .rotate((float) Math.sin(Math.toRadians(row0_percentage * 360 - 180)) / 8f, 0, 1, 0));
    nodes.get("spiderA-root-legRight1").setAnimationTransform(new Matrix4f()
            .rotate((float) Math.sin(Math.toRadians(row1_percentage * 360 - 180)) / 8f, 0, 1, 0));
    nodes.get("spiderA-root-legRight2").setAnimationTransform(new Matrix4f()
            .rotate((float) Math.sin(Math.toRadians(row2_percentage * 360 - 180)) / 8f, 0, 1, 0));
    nodes.get("spiderA-root-legLeft3").setAnimationTransform(new Matrix4f()
            .rotate((float) Math.sin(Math.toRadians(row3_percentage * 360 - 180)) / 8f, 0, 1, 0));

    nodes.get("spiderB-root-legRight0").setAnimationTransform(new Matrix4f()
            .rotate((float) Math.sin(Math.toRadians(row0_percentage * 360 - 180)) / 8f, 0, 1, 0));
    nodes.get("spiderB-root-legRight1").setAnimationTransform(new Matrix4f()
            .rotate((float) Math.sin(Math.toRadians(row1_percentage * 360 - 180)) / 8f, 0, 1, 0));
    nodes.get("spiderB-root-legRight2").setAnimationTransform(new Matrix4f()
            .rotate((float) Math.sin(Math.toRadians(row2_percentage * 360 - 180)) / 8f, 0, 1, 0));
    nodes.get("spiderB-root-legRight3").setAnimationTransform(new Matrix4f()
            .rotate((float) Math.sin(Math.toRadians(row3_percentage * 360 - 180)) / 8f, 0, 1, 0));
  }

  @Override
  public void addNode(String name, INode node) {
    nodes.put(name, node);
  }


  @Override
  public INode getRoot() {
    return root;
  }

  @Override
  public Map<String, PolygonMesh> getPolygonMeshes() {
    Map<String, util.PolygonMesh> meshes = new TreeMap<String, PolygonMesh>();

    meshes.putAll(this.meshes);
    return meshes;
  }

  @Override
  public Map<String, INode> getNodes() {
    Map<String, INode> nodes = new TreeMap<String, INode>();
    nodes.putAll(this.nodes);
    return nodes;
  }

  private Matrix4f worldToView_raytrace = new Matrix4f();
  @Override
  public Color[][] raytrace(int width, int height, Stack<Matrix4f> modelViewStack) {
    worldToView_raytrace = new Matrix4f(modelViewStack.peek());

    System.out.println("Raytrace Begun!");
    Color[][] pixels = new Color[width][height];

    /*
     create ray in view coordinates
     start point: 0,0,0 always!
     going through near plane pixel (i,j)
     So 3D location of that pixel in view coordinates is
     x = i-width/2
     y = j-height/2
     z = -0.5*height/tan(FOVY)
    */
    for (int i = 0; i < width; ++i) {
      for (int j = 0; j < height; ++j) {
        float vX = i - width / 2;
        float vY = j - height / 2;
        float vZ = (float) (-0.5f * height / Math.tan((float) Math.toRadians(60f))); // not 120 because looking for theta / 2

        float sX = 0;
        float sY = 0;
        float sZ = 0;

        // Random Ray
//        Vector4f rayDirection = new Vector4f(
//                2 * (float) Math.random() - 1,
//                2 * (float) Math.random() - 1,
//                2 * (float) Math.random() - 1, 0).normalize();

        Vector4f rayStart = new Vector4f(sX, sY, sZ, 1);
        Vector4f rayDirection = new Vector4f(vX, vY, vZ, 0);
        Ray rayInViewCoord = new Ray(rayStart, rayDirection);

        Vector4f c = new Vector4f(0, 0, 0, 1);
        boolean didRayHit = raycast(rayInViewCoord, modelViewStack, c);
        pixels[i][j] = new Color(c.x, c.y, c.z); // Color needs to be within (0, 1)
      }
    }
    System.out.println("Raytrace Finished!");
    return pixels;
  }

  @Override
  public boolean raycast(Ray viewRay, Stack<Matrix4f> modelViewStack, Vector4f colorRef) {

    // Pass in hitrecord reference
    HitRecord hitRecord = new HitRecord(); // Empty hitrecord
    getRoot().intersect(modelViewStack, viewRay, hitRecord, textureImages);
    boolean hasHit = !hitRecord.isEmpty();

    // Update color reference with HitRecord properties
    if (hasHit) {
      Vector4f shade = shade(hitRecord); // in 0 - 1
      colorRef.set(shade.x, shade.y, shade.z, shade.w);
    } else {
      colorRef.set(0, 0, 0, 1); // Black
    }

    // Return whether it hit or not
    return hasHit;
  }

  private Vector4f shade(HitRecord hitRecord) {
    // Material Factors
    Vector4f materialAmbient = new Vector4f(hitRecord.getMaterial().getAmbient());
    Vector4f materialDiffuse = new Vector4f(hitRecord.getMaterial().getDiffuse());
    Vector4f materialSpecular = new Vector4f(hitRecord.getMaterial().getSpecular());
    float materialShininess = hitRecord.getMaterial().getShininess();

    // Resulting Color
    Vector4f ambient;
    Vector4f diffuse;
    Vector4f specular;
    Vector4f finalColor = new Vector4f(0, 0, 0, 0);

    // Normal at point of intersection
    Vector4f fNormal = new Vector4f(hitRecord.getNormal());
    Vector4f normalized_normalVector_inView = new Vector4f(hitRecord.getNormal());

    // Point of Intersection
    Vector4f fPosition_InView = new Vector4f(hitRecord.getIntersectionPoint());

    // View vector
    Vector4f normalized_viewVector_InView =
            new Vector4f(fPosition_InView.x, fPosition_InView.y, fPosition_InView.z, 0)
              .negate()
              .normalize();

    // Light Vector
    Vector4f normalized_lightVector;
    Vector4f reflectVector;
    Vector4f spotVector;

    // Dot Products
    float nDotL; // normalized_NormalVector_InView.dot(lightVector)
    float rDotV; //

    for (Light l : getRoot().getAllLights(worldToView_raytrace)) {
      if (l.getPosition().w != 0) {
        normalized_lightVector = new Vector4f(l.getPosition()).sub(fPosition_InView).normalize();
      } else {
        normalized_lightVector = new Vector4f(l.getPosition()).negate().normalize();
      }

      Vector4f tNormal = new Vector4f(normalized_normalVector_inView);
      nDotL = normalized_normalVector_inView.dot(normalized_lightVector); // Note: calling 'dot' doesn't mutate

      int s = 1;

      if (l.getPosition().w != 0) {
        if (new Vector4f(normalized_lightVector)
                .negate()
                .dot(new Vector4f(l.getSpotDirection()).normalize()) > l.getSpotCutoff()) {
          s = 1;
        } else {
          s = 0;
        }
      }

      reflectVector = new Vector4f(normalized_lightVector)
              .negate()
              .mul(
              new Matrix4f().reflect(
                new Vector3f(
                        normalized_normalVector_inView.x,
                        normalized_normalVector_inView.y,
                        normalized_normalVector_inView.z),
                new Vector3f(
                        fPosition_InView.x,
                        fPosition_InView.y,
                        fPosition_InView.z)))
              .normalize();

      rDotV = Math.max(reflectVector.dot(normalized_viewVector_InView), 0);

      Vector3f lightAmbient = new Vector3f(l.getAmbient());
      ambient = new Vector4f(materialAmbient).mul(lightAmbient.x, lightAmbient.y, lightAmbient.z, 1);

      Vector3f lightDiffuse = new Vector3f(l.getDiffuse());
      diffuse = new Vector4f(materialDiffuse)
                  .mul(lightDiffuse.x, lightDiffuse.y, lightDiffuse.z, 1)
                  .mul(Math.max(nDotL, 0));

      if (nDotL > 0) {
        Vector3f lightSpecular = new Vector3f(l.getSpecular());
        specular = new Vector4f(materialSpecular)
                    .mul(lightSpecular.x, lightSpecular.y, lightSpecular.z, 1)
                    .mul((float) Math.pow(rDotV, materialShininess));
      } else {
        specular = new Vector4f(0, 0, 0, 1);
      }

      Vector4f ads = new Vector4f(0, 0, 0, 0)
              .add(ambient)
              .add(diffuse)
              .add(specular);
      Vector4f mul = ads.mul(s);
//      Vector4f mul = ads;
      finalColor = finalColor.add(mul.x, mul.y, mul.z, mul.w);
    }
    finalColor = finalColor.mul(hitRecord.getTexture());

    // Truncate to 0 to 1 range
    return new Vector4f(
            Math.min(finalColor.x, 1f) / 1f,
            Math.min(finalColor.y, 1f) / 1f,
            Math.min(finalColor.z, 1f) / 1f,
            Math.min(finalColor.w, 1f) / 1f);
  }
}
