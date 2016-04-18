package util;

import org.joml.Matrix4f;
import org.joml.Vector4f;

public class Ray {

  private Vector4f startingPoint = new Vector4f(0, 0, 0, 1);
  private Vector4f direction = new Vector4f(0, 0, 0, 0);

  /**
   * Constructs a ray with a starting point of (0, 0, 0, 1) and direction of (0, 0, 0, 0)
   */
  public Ray() {
    // DO NOTHING;
  }

  /**
   * Constructs a ray with given start and direction
   * @param startingPoint the starting position
   * @param direction the directional vector
   */
  public Ray(Vector4f startingPoint, Vector4f direction) {
    this.startingPoint = new Vector4f(startingPoint);
    this.direction = new Vector4f(direction);
  }


  public Vector4f getStartingPoint() {
    return startingPoint;
  }

  public void setStartingPoint(Vector4f startingPoint) {
    this.startingPoint = new Vector4f(startingPoint);
  }

  public Vector4f getDirection() {
    return direction;
  }

  public void setDirection(Vector4f direction) {
    this.direction = new Vector4f(direction);
  }

  /**
   * Given a (matrix new <- current), transform this ray's position and direction
   * @param currentToNew
   * @return a new ray based on this ray's direction, without affecting this original ray
   */
  public Ray changeCoordinateSystem(Matrix4f currentToNew) {
    Ray correctedCoordinateSystem = new Ray(getStartingPoint(), getDirection());

    correctedCoordinateSystem.setStartingPoint(correctedCoordinateSystem.getStartingPoint().mul(currentToNew));
    correctedCoordinateSystem.setDirection(correctedCoordinateSystem.getDirection().mul(currentToNew));

    return correctedCoordinateSystem;
  }

  public Vector4f computeIntersection(float t) {
    return new Vector4f(startingPoint).add(new Vector4f(direction).mul(t));
  }
}
