package sgraph.Nodes;

import com.jogamp.opengl.util.texture.Texture;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import sgraph.IScenegraph;
import util.HitRecord;
import util.Light;
import util.Ray;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * This abstract class implements the {@link INode} interface. It provides default methods
 * for many of the methods, especially the ones that could throw an exception
 * Child classes that do not want these exceptions throws should override these methods
 *
 * @author Amit Shesh
 */
public abstract class AbstractNode implements INode {
  /**
   * The name given to this node
   */
  protected String name;
  /**
   * The parent of this node. Each node except the root has a parent. The root's parent is null
   */
  protected INode parent;
  /**
   * A reference to the {@link sgraph.IScenegraph} object that this is part of
   */
  protected IScenegraph scenegraph;

  /**
   * A list of light objects
   */
  protected ArrayList<Light> listOfLights;

  public AbstractNode(IScenegraph graph, String name) {
    this.parent = null;
    scenegraph = graph;
    listOfLights = new ArrayList<Light>();
    setName(name);
  }

  /**
   * By default, this method checks only itself. Nodes that have children should override this
   * method and navigate to children to find the one with the correct name
   *
   * @param name name of node to be searched
   * @return the node whose name this is, null otherwise
   */
  public INode getNode(String name) {
    if (this.name.equals(name))
      return this;

    return null;
  }

  /**
   * Sets the parent of this node
   *
   * @param parent the node that is to be the parent of this node
   */

  public void setParent(INode parent) {
    this.parent = parent;
  }

  /**
   * Sets the scene graph object whose part this node is and then adds itself
   * to the scenegraph (in case the scene graph ever needs to directly access this node)
   *
   * @param graph a reference to the scenegraph object of which this tree is a part
   */
  public void setScenegraph(IScenegraph graph) {
    this.scenegraph = graph;
    graph.addNode(this.name, this);
  }

  /**
   * Sets the name of this node
   *
   * @param name the name of this node
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the name of this node
   *
   * @return the name of this node
   */
  public String getName() {
    return name;
  }


  public abstract INode clone();

  /**
   * By default, throws an exception. Any nodes that can have children should override this
   * method
   *
   * @param child
   * @throws IllegalArgumentException
   */
  @Override
  public void addChild(INode child) throws IllegalArgumentException {
    throw new IllegalArgumentException("Not a composite node");
  }

  /**
   * By default, throws an exception. Any nodes that are capable of storing transformations
   * should override this method
   *
   * @param t
   */

  @Override
  public void setTransform(Matrix4f t) {
    throw new IllegalArgumentException(getName() + " is not a transform node");
  }

  @Override
  public void setAnimationTransform(Matrix4f t) {
    throw new IllegalArgumentException(getName() + " is not a transform node");
  }

  /**
   * By default, throws an exception. Any nodes that are capable of storing material should
   * override this method
   *
   * @param m the material object to be associated with this node
   */
  @Override
  public void setMaterial(util.Material m) {
    throw new IllegalArgumentException(getName() + " is not a leaf node");
  }

  @Override
  public void addLight(util.Light light) {
    listOfLights.add(light.clone());
  }

  @Override
  public ArrayList<util.Light> getLights() {
    return this.listOfLights;
  }

  @Override
  public List<Light> getAllLights(Matrix4f worldToView) {
    return getAllLightsHelp(new ArrayList<Light>(), new ArrayList<INode>(), worldToView);
  }

  @Override
  public void transformLightsPassedIn(Matrix4f toView, List<Light> listOfLightsTransformed) {
    for (Light l : listOfLightsTransformed) {
      // if the light's position is a vector…
      // it means that it is a directional light
      // no transformations on position,


      if (l.getPosition().w == 1) {
        l.setPosition(new Vector4f(l.getPosition())
                .mul(toView));

        l.setSpotDirection(
                new Vector4f(l.getSpotDirection())
                        .mul(toView));


      } else if (l.getPosition().w == 0) {
        l.setDirection(new Vector4f(l.getPosition())
                .mul(toView));
      }
    }
  }

}
