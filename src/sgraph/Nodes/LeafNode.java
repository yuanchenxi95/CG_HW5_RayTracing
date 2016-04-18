package sgraph.Nodes;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import sgraph.IScenegraph;
import sgraph.IScenegraphRenderer;
import util.*;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * This node represents the leaf of a scene graph. It is the only type of node that has
 * actual geometry to render.
 *
 * @author Amit Shesh
 */
public class LeafNode extends AbstractNode {
  /**
   * The name of the object instance that this leaf contains. All object instances are stored
   * in the scene graph itself, so that an instance can be reused in several leaves
   */
  protected String objInstanceName;
  /**
   * The material associated with the object instance at this leaf
   */
  protected util.Material material;

  private String texturename;

  public LeafNode(String instanceOf, IScenegraph graph, String name, String texturename) {
    super(graph, name);
    this.objInstanceName = instanceOf;
    this.texturename = texturename;
  }


  /*
 *Set the material of each vertex in this object
 */
  @Override
  public void setMaterial(util.Material mat) {
    material = new util.Material(mat);
  }

  @Override
  public Matrix4f getObjectToViewTransform(Matrix4f accumulator, Matrix4f worldToView) {

    if (this.parent == null) {
      return new Matrix4f().mul(worldToView).mul(accumulator);
    }

    // M v <- w     M w <- train    M train <- animation    M animation <- transform    M transform <- light
    return parent.getObjectToViewTransform(accumulator, worldToView);
  }

  /*
   * gets the material
   */
  public util.Material getMaterial() {
    return material;
  }

  @Override
  public INode clone() {
    LeafNode newclone = new LeafNode(this.objInstanceName, scenegraph, name, this.texturename);
    newclone.setMaterial(this.getMaterial());

    for (Light l : listOfLights) {
      newclone.addLight(l.clone());
    }
    return newclone;
  }


  /**
   * Delegates to the scene graph for rendering. This has two advantages:
   * <ul>
   * <li>It keeps the leaf light.</li>
   * <li>It abstracts the actual drawing to the specific implementation of the scene graph renderer</li>
   * </ul>
   *
   * @param context   the generic renderer context {@link sgraph.IScenegraphRenderer}
   * @param modelView the stack of modelview matrices
   * @throws IllegalArgumentException
   */
  @Override
  public void draw(IScenegraphRenderer context, Stack<Matrix4f> modelView) throws IllegalArgumentException {
    if (objInstanceName.length() > 0) {
      context.drawMesh(objInstanceName, material, modelView.peek(), this.texturename);
    }
  }

  @Override
  public List<Light> getAllLightsHelp(List<Light> accLights, List<INode> unvisited, Matrix4f worldToView) {
    // Transform New Lights in this node
//        System.out.println("Leaf getAllLights");
    Matrix4f toView = getObjectToViewTransform(new Matrix4f(), new Matrix4f(worldToView));
    List<Light> listOfLightsTransformed = new ArrayList<>();
    for (Light l : listOfLights) {
      listOfLightsTransformed.add(l.clone());
    }

    transformLightsPassedIn(toView, listOfLightsTransformed);

    // New Acc
    List<Light> transformedLights = new ArrayList<>(accLights);
    transformedLights.addAll(listOfLightsTransformed);

    if (!unvisited.isEmpty()) {
      unvisited.remove(0);
    }

    if (unvisited.isEmpty()) {
      return transformedLights;
    }
    return unvisited.get(0).getAllLightsHelp(transformedLights, unvisited, worldToView);
  }

  @Override
  public void intersect(Stack<Matrix4f> currentTransformationStack, Ray viewRay, HitRecord hit, Map<String, TextureImage> textureImages) {
//    System.out.println("Testing intersection of leaf object: " + objInstanceName);

//    System.out.println(textureImages.size());
    Optional<Float> newT = RaytraceUtil.intersect(
            viewRay.changeCoordinateSystem(new Matrix4f(currentTransformationStack.peek()).invert()),
            objInstanceName);

    // Did it hit this object?
    if (!newT.isPresent()) {
      // DO NOTHING
    } else {
      // Hit: override existing hit?
      if (hit.isEmpty() || newT.get() < hit.getT()) {
          // New t is smaller, use the new hit
          hit.changeHitRecord(getHitRecord(currentTransformationStack.peek(), viewRay, newT.get(), textureImages));
      }
    }
  }

  /**
   * Pre-condition: checked that the hit record exists and should be constructed
   * Creates a new hit record instance with the given parameters of THIS leaf object, material, texture etc
   * @param fromObjToView
   * @param newT
   * @param viewRay
   */
  private HitRecord getHitRecord(Matrix4f fromObjToView, Ray viewRay, float newT, Map<String, TextureImage> textureImages) {

    // Inverse
    Matrix4f fromViewToObject = new Matrix4f(fromObjToView).invert();

    // Inverse Inverse Transpose
    Matrix4f objectToViewNormal = new Matrix4f(fromObjToView).invert().transpose();

    // Object Ray
    Ray objectRay = viewRay.changeCoordinateSystem(fromViewToObject);

    // Inverse-Transpose
    Matrix4f inverseT = new Matrix4f(fromObjToView).invert().transpose();

    // Compute Intersection
    Vector4f pointOfIntersection = viewRay.computeIntersection(newT);

    // Compute Intersection in Object Coords
    Vector4f pointOfIntersection_obj = objectRay.computeIntersection(newT);

    Vector4f textureColor = new Vector4f(1,1,1,1);

    // Compute Normal by:
    // 1) Computing normal in obj coordinate system
    // 2) Then converting from obj to the view coordinate system
    Vector4f normalized_normal_inView = null;
    switch (objInstanceName) {
      case "box":
      case "box-outside":
      case "box-inside":
        float normalX = 0f;
        float normalY = 0f;
        float normalZ = 0f;
        if (Math.abs(pointOfIntersection_obj.z - 0.5f) < 0.001f) {
          // Front face?
          normalZ = 1;
        } else if (Math.abs(pointOfIntersection_obj.z + 0.5f) < 0.001f) {
          // Back face?
          normalZ = -1;
        } else {
          normalZ = 0;
        }

        if (Math.abs(pointOfIntersection_obj.y - 0.5f) < 0.001f) {
          // Top face?
          normalY = 1;
        } else if (Math.abs(pointOfIntersection_obj.y + 0.5f) < 0.001f) {
          // Bottom face?
          normalY = -1;
        } else {
          normalY = 0;
        }

        if (Math.abs(pointOfIntersection_obj.x - 0.5f) < 0.001f) {
          // Right face?
          normalX = 1;
        } else if (Math.abs(pointOfIntersection_obj.x + 0.5f) < 0.001f) {
          // Left face?
          normalX = -1;
        } else {
          normalX = 0;
        }

        Vector4f normal_inObj = new Vector4f(normalX, normalY, normalZ, 0);
        normalized_normal_inView = normal_inObj.mul(objectToViewNormal).normalize(); // view coord

        break;
      case "sphere":
        normalized_normal_inView = new Vector4f(
                pointOfIntersection_obj.x,
                pointOfIntersection_obj.y,
                pointOfIntersection_obj.z,
                0).mul(objectToViewNormal).normalize();

        break;
      case "cylinder":
        if (Math.abs(pointOfIntersection_obj.y - 0f) < 0.1f) {
          normalized_normal_inView = (new Vector4f(0, -1, 0, 0).mul(objectToViewNormal)).normalize();
        } else if (Math.abs(pointOfIntersection_obj.y - 1f) < 0.1f){
          normalized_normal_inView = (new Vector4f(0, 1, 0, 0).mul(objectToViewNormal)).normalize();
        } else if (pointOfIntersection_obj.y > 1.1f || pointOfIntersection_obj.y < -0.1f) {
          // give 0.001 calculation errors.
//          System.out.println(pointOfIntersection_obj.y);
          normalized_normal_inView = (new Vector4f(0, 1, 0, 0).mul(objectToViewNormal)).normalize();
          throw new RuntimeException("Cylinder intersection point is illegal.");
        } else {
          normalized_normal_inView = new Vector4f(pointOfIntersection_obj.x, 0, pointOfIntersection_obj.z, 0).mul(objectToViewNormal).normalize();
        }


        break;
      default:
        throw new RuntimeException("Object Instance: " + objInstanceName + " is not supported!");
    }

    // normalize the normals
    normalized_normal_inView = new Vector4f(normalized_normal_inView.x, normalized_normal_inView.y, normalized_normal_inView.z, 0f).normalize();

    //        if (Math.abs(pointOfIntersection_obj.y - 0f) < 0.1f) {
//          System.out.println(normalized_normal_inView.x + ", " + normalized_normal_inView.y + ", " + normalized_normal_inView.z + ", " + normalized_normal_inView.w);
//        } else if (Math.abs(pointOfIntersection_obj.y - 0f) < 0.1f){
//          System.out.println(normalized_normal_inView.x + ", " + normalized_normal_inView.y + ", " + normalized_normal_inView.z + ", " + normalized_normal_inView.w);
//        }


    TextureImage ti = textureImages.get(texturename);

    if (ti != null) {

      // Calculate Texture Color
      switch (objInstanceName) {
        case "box": {
          float bx = pointOfIntersection_obj.x;
          float by = pointOfIntersection_obj.y;
          float bz = pointOfIntersection_obj.z;

          // front: z == 0.5
          if (Math.abs(bz - 0.5f) <= 0.001f) {
            // (x, y)
            float imgx = bx + 0.5f;
            float imgy = by + 0.5f;
            textureColor = ti.getColor(imgx, imgy);
          }

          // back: z == -0.5
          if (Math.abs(bz + 0.5f) <= 0.001f) {
            // (x, y)
            // flip over x
            float imgx = 1f - (bx + 0.5f);
            float imgy = by + 0.5f;
            textureColor = ti.getColor(imgx, imgy);
          }

          // right: x == 0.5
          if (Math.abs(bx - 0.5f) <= 0.001f) {
            // (z, y)
            // flip over z
            float imgx = 1f - (bz + 0.5f);
            float imgy = by + 0.5f;
            textureColor = ti.getColor(imgx, imgy);
          }

          // left: x == 0.5
          if (Math.abs(bx + 0.5f) <= 0.001f) {
            // (z, y)
            float imgx = bz + 0.5f;
            float imgy = by + 0.5f;
            textureColor = ti.getColor(imgx, imgy);
          }

          // top: y == 0.5
          if (Math.abs(by - 0.5f) <= 0.001f) {
            // flip over z
            // (x, z)
            float imgx = bx + 0.5f;
            float imgy = 1f - (bz + 0.5f);
            textureColor = ti.getColor(imgx, imgy);
          }

          // bottom: y == -0.5
          if (Math.abs(by + 0.5f) <= 0.001f) {
            // (x, z)
            float imgx = bx + 0.5f;
            float imgy = bz + 0.5f;
            textureColor = ti.getColor(imgx, imgy);
          }


        }
        break;
        case "box-outside":
        case "box-inside": {
          float bx = pointOfIntersection_obj.x;
          float by = pointOfIntersection_obj.y;
          float bz = pointOfIntersection_obj.z;

          // break if the x, y, or z, are outside(-0.5,0.5), give 0.01 accuracy error
          if (Math.abs(bx) > 0.51f || Math.abs(by) > 0.51f || Math.abs(bz) > 0.51f) {
            break;
          }


//        f4...........f1
//        .            .
//        .            .
//        .            .
//        .            .
//        .            .
//        f3...........f2


//                  0.00      0.25      0.50      0.75      1.00
//
//
//        0.00
//
//
//        0.25
//                               top
//
//        0.50
//                      left     back       right      front
//
//        0.75
//                              bottom
//
//        1.00


          // f: front, r: right, ba: back, l: left, t: top, b: bottom

          // front: z == 0.5
          // f1(0.5, 0.5, 0.5)
          // f2(0.5, -0.5, 0.5)
          // f3(-0.5, -0.5, 0.5)
          // f4(-0.5, 0.5, 0.5)

          // back: z == -0.5
          // ba1(-0.5,0.5,-0.5)
          // ba2(-0.5,-0.5,-0.5)
          // ba3(0.5,-0.5,-0.5)
          // ba4(0.5,0.5,-0.5)

          // right: x == 0.5
          // r1(0.5, 0.5, -0.5)
          // r2(0.5, -0.5, -0.5)
          // r3(0.5, 0.5, 0.5)
          // r4(0.5, -0.5, 0.5)

          // left: x == -0.5
          // l1(-0.5,0.5,0.5)
          // l2(-0.5,0.5,-0.5)
          // l3(-0.5,-0.5,0.5)
          // l4(-0.5,-0.5,-0.5)

          // top: y = 0.5
          // t1(0.5, 0.5, -0.5)
          // t2(0.5, 0.5, 0.5)
          // t3(-0.5, 0.5, 0.5)
          // t4(-0.5, 0.5, -0.5)

          // bottom: y == -0.5
          // b1(0.5, -0.5, 0.5)
          // b2(0.5, -0.5, -0.5)
          // b3(-0.5, -0.5, -0.5)
          // b4(-0.5, -0.5, 0.5)


          // front: z == 0.5
          if (Math.abs(bz - 0.5f) <= 0.001f) {
            // (x, y)
            float imgx = 0.75f + (bx + 0.5f) * 0.25f;
            float imgy = 0.25f + (by + 0.5f) * 0.25f;
            textureColor = ti.getColor(imgx, imgy);
          }

          // back: z == -0.5
          if (Math.abs(bz + 0.5f) <= 0.001f) {
            // (x, y)
            // flip over x
            float imgx = 0.25f + (1f - (bx + 0.5f)) * 0.25f;
            float imgy = 0.25f + (by + 0.5f) * 0.25f;
            textureColor = ti.getColor(imgx, imgy);
          }

          // right: x == 0.5
          if (Math.abs(bx - 0.5f) <= 0.001f) {
            // (z, y)
            // flip over z
            float imgx = 0.50f + (1f - (bz + 0.5f)) * 0.25f;
            float imgy = 0.25f + (by + 0.5f) * 0.25f;
            textureColor = ti.getColor(imgx, imgy);
          }

          // left: x == 0.5
          if (Math.abs(bx + 0.5f) <= 0.001f) {
            // (z, y)
            float imgx = 0.00f + (bz + 0.5f) * 0.25f;
            float imgy = 0.25f + (by + 0.5f) * 0.25f;
            textureColor = ti.getColor(imgx, imgy);
          }

          // top: y == 0.5
          if (Math.abs(by - 0.5f) <= 0.001f) {
            // flip over z
            float imgx = 0.25f + (bx + 0.5f) * 0.25f;
            float imgy = 0.50f + (1f - (bz + 0.5f)) * 0.25f;
            textureColor = ti.getColor(imgx, imgy);
          }

          // bottom: y == -0.5
          if (Math.abs(by + 0.5f) <= 0.001f) {
            float imgx = 0.25f + (bx + 0.5f) * 0.25f;
            float imgy = 0.00f + (bz + 0.5f) * 0.25f;
            textureColor = ti.getColor(imgx, imgy);
          }


        }
        break;
        case "sphere": {
          float sx = pointOfIntersection_obj.x;
          float sy = pointOfIntersection_obj.y;
          float sz = pointOfIntersection_obj.z;
          float phi = (float) Math.asin(sy);
          phi += Math.PI / 2;
          float theta = (float) Math.atan2((double) sz, (double) sx);
          theta += Math.PI;

          float s = (float) (theta / (2f * Math.PI));

          // flip the t
          float t = (1 - (phi / ((float) Math.PI)));
          textureColor = textureImages.get(texturename).getColor(s, t);
//        if (textureColor.x < 0 || textureColor.y < 0 || textureColor.z < 0) {
//          System.out.println();
//        }
        }
        break;
        case "cylinder":
          // DO NOTHING
          break;
        default:
          throw new RuntimeException("Object Instance: " + objInstanceName + " is not supported!");
      }
    }


    return new HitRecord(
            newT,
            pointOfIntersection,
            normalized_normal_inView,
            material,
            textureColor); // TODO add texture
  }
}
