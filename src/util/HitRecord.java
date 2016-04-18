package util;

import org.joml.Vector4f;

/**
 Stores all the information that you will need to determine the closest object that was hit, and
 information about that object to calculate shading.

 More specifically, it will contain:
 (a) the time ‘t’ on the ray where it intersects an object
 (b) the 3D point of intersection in view coordinates
 (c) the 3D normal of the object at that point in view coordinates
 (d) The material properties.
 (e) Texture coordinates and a Texture object, if applicable
 */
public class HitRecord {

  private float t = 0;
  private Vector4f intersectionPoint = new Vector4f(0, 0, 0, 1);
  private Vector4f normal = new Vector4f(0, 0, 0, 0);
  private util.Material material = new Material();
  private Vector4f texture = null;

  public HitRecord() {
    // Keep isEmpty == true
  }

  public HitRecord(float t,
                   Vector4f intersectionPoint,
                   Vector4f normal,
                   Material material,
                   Vector4f texture) {
    this.t = t;
    this.intersectionPoint = intersectionPoint;
    this.normal = normal;
    this.material = material;
    this.texture = texture;
    this.isEmpty = false;
  }

  public float getT() {
    return t;
  }

  public Vector4f getIntersectionPoint() {
    return intersectionPoint;
  }

  public Vector4f getNormal() {
    return normal;
  }

  public Material getMaterial() {
    return material;
  }

  public Vector4f getTexture() {
    return texture;
  }

  public void changeHitRecord(HitRecord newHit) {
    changeHitRecord(newHit.getT(), newHit.getIntersectionPoint(), newHit.getNormal(), newHit.getMaterial(), newHit.getTexture());
    isEmpty = false;
  }

  private void changeHitRecord(float t, Vector4f intersectionPoint, Vector4f normal, Material material, Vector4f texture) {
    setT(t);
    setIntersectionPoint(intersectionPoint);
    setNormal(normal);
    setMaterial(material);
    setTexture(texture);
  }

  private void setT(float t) {
    this.t = t;
  }

  private void setIntersectionPoint(Vector4f intersectionPoint) {
    this.intersectionPoint = new Vector4f(intersectionPoint);
  }

  private void setNormal(Vector4f normal) {
    this.normal = new Vector4f(normal);
  }

  private void setMaterial(Material material) {
    this.material = new Material(material);
  }

  private void setTexture(Vector4f texture) {
    this.texture = new Vector4f(texture);
  }

  boolean isEmpty = true;
  public boolean isEmpty() {
    return isEmpty;
  }
}
