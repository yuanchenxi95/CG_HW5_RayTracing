package util;

import org.joml.Vector4f;

import java.util.Optional;

/**
 * Created by jeffrey02px2014 on 4/9/16.
 */
public class RaytraceUtil {

  public static Optional<Float> intersect(Ray rayInObjectCoord, String objectType) {
    switch (objectType) {
      case "box":
        return intersectBox(rayInObjectCoord);
      case "box-outside":
        return intersectBox(rayInObjectCoord);
      case "box-inside":
        return intersectBox(rayInObjectCoord);
      case "sphere":
        return intersectSphere(rayInObjectCoord);
      case "cylinder":
        return intersectCylinder(rayInObjectCoord);
      default:
        throw new RuntimeException("Object Instance: " + objectType + " is not supported!");
    }
  }

  /**
   * Box: center @0, 0, 0; size: 1
   * @param rayInObjectCoord ray must be in object coordinate system
   * @return t of intersection
   */
  public static Optional<Float> intersectBox(Ray rayInObjectCoord) {
    Vector4f start = rayInObjectCoord.getStartingPoint();
    Vector4f direction = rayInObjectCoord.getDirection();

    // Set range initially as tXMin to tXMax range.
    float cubeMin = -0.5f;
    float cubeMax = 0.5f;
    float tMin = (cubeMin - start.x) / direction.x;
    float tMax = (cubeMax - start.x) / direction.x;

    // Flip if tMin is greater than tMax
    if (tMin > tMax) {
      float temptMin = tMin;
      tMin = tMax;
      tMax = temptMin;
    }

    // Find t ranges in the y direction
    float tYMin = (cubeMin - start.y) / direction.y;
    float tYMax = (cubeMax - start.y) / direction.y;

    // Flip if tYMin is greater than tYMax
    if (tYMin > tYMax) {
      float temptYMin = tYMin;
      tYMin = tYMax;
      tYMax = temptYMin;
    }

    // if x-interval is completely left of y-interval... OR if x-interval is completely right of y-interval
    if (tMin > tYMax || tYMin > tMax) {
      return Optional.empty();
    }

    // Shrink left-hand-range to overlap
    if (tYMin > tMin) {
      tMin = tYMin;
    }

    // Shrink right-hand-range to overlap
    if (tYMax < tMax) {
      tMax =tYMax;
    }

    // Calculate t-range for z direction
    float tZMin = (cubeMin - start.z) / direction.z;
    float tZMax = (cubeMax - start.z) / direction.z;

    if (tZMin > tZMax) {
      float temptZMin = tZMin;
      tZMin = tZMax;
      tZMax = temptZMin;
    }

    // if current range completely left of z range:
    //    [...]
    //          [...]
    //
    // if current range completely right of z range:
    //          [...]
    //    [...]
    if (tMin > tZMax || tZMin > tMax)
      return Optional.empty();
    if (tZMin > tMin)
      tMin = tZMin;
    if (tZMax < tMax)
      tMax = tZMax;

    if (tMin < 0f) {
      if (tMax < 0f) {
        return Optional.empty();
      } else {
        return Optional.of(tMax);
      }
    } else {
      if (tMax < 0f) {
        return Optional.of(tMin);
      } else {
        return Optional.of(Math.min(tMin, tMax));
      }
    }
  }

  /**
   * Sphere: center @0, 0, 0; size: 1
   * @param rayInObjectCoord ray must be in object coordinate system
   * @return did it hit TODO change to actual t
   */
  public static Optional<Float> intersectSphere(Ray rayInObjectCoord) {
    Vector4f start = rayInObjectCoord.getStartingPoint();
    Vector4f direction = rayInObjectCoord.getDirection();

    float xc, yc, zc;
    xc = yc = zc = 0f;

    float r = 1f;

    float a = (float) (Math.pow(direction.x, 2f) + Math.pow(direction.y, 2f) + Math.pow(direction.z, 2f));
    float b = (2 * direction.x * (start.x - xc)
            + 2 * direction.y * (start.y - yc)
            + 2 * direction.z * (start.z - zc));
    float c = (float) (Math.pow((start.x - xc), 2f) + Math.pow((start.y - yc), 2f) + Math.pow((start.z - zc), 2f) - Math.pow(r, 2f));

    if (b * b - 4 * a * c >= 0f) {
      float t1 = (float) ((-b - Math.sqrt(Math.pow(b, 2f) - 4f * a * c)) / (2f * a));
      float t2 = (float) ((-b + Math.sqrt(Math.pow(b, 2f) - 4f * a * c)) / (2f * a));

      if (t1 < 0) {
        if (t2 < 0) {
          return Optional.empty();
        } else {
          return Optional.of(t2);
        }
      } else {
        if (t2 < 0) {
          return Optional.of(t1);
        } else {
          return Optional.of(Math.min(t1, t2));
        }
      }
    } else {
      return Optional.empty();
    }
  }

  /**
   * Cylinder: center @0, 0, 0; size: 1
   * @param rayInObjectCoord ray must be in object coordinate system
   * @return did it hit TODO change to actual t
   */
  public static Optional<Float> intersectCylinder(Ray rayInObjectCoord) {


    boolean flagStep1 = false;
    boolean flagStep2 = false;
    boolean flagStepUp = false;
    boolean flagStepDown = false;
    Vector4f start = rayInObjectCoord.getStartingPoint();
    Vector4f direction = rayInObjectCoord.getDirection();

    float radius = 1;
    float height = 1;

    // on the infinite cylinder
    // (v.x^2 + v.z^2) * t^2 + 2 * (v.x*p.x + v.z*p.z) * t + p.x^2 + p.z^2 - r^2 = 0
    // -1 * height / 2 <= v.y*t + p.y <= height /2
    // in the obj case: 0 <= v.y*t + p.y <= height

    // on the cap plane
    // in the obj case: v.y * t + p.y = 0 || v.y * t + p.y = height
    // v.y*t + p.y = height/2
    // v.y*t + p.y = -1 * height/2
    // (v.x*t + p.x)^2 + (v.y*t + p.y)^2 <= r^2;


    float vx = direction.x;
    float vy = direction.y;
    float vz = direction.z;

    float px = start.x;
    float py = start.y;
    float pz = start.z;

    float t1 = -1;
    float t2 = -1;

    // step 1. find the intersection of the infinite cylinder
    {
      // (v.x^2 + v.z^2) * t^2 + 2 * (v.x*p.x + v.z*p.z) * t + p.x^2 + p.z^2 - r^2 = 0
      // -1 * height / 2 <= v.y*t + p.y <= height /2
      // in the obj case: 0 <= v.y*t + p.y <= height


      float a = vx * vx + vz * vz;
      float b = 2 * (vx * px + vz * pz);
      float c = px * px + pz * pz - radius * radius;

      // if a == 0, the ray is shooting straight down or straight up.
      if (Math.abs(a) != 0) {


        // initialize with negative value, which is invalid.

        // if b^2 - 4ac < 0, no intersection
        float delta = b * b - 4 * a * c;
        if (delta >= 0) {
          t1 = (-1 * b + (float) Math.sqrt(delta)) / (a * 2);
          t2 = (-1 * b - (float) Math.sqrt(delta)) / (a * 2);
        }


        if (t1 >= 0) {
          // cylinder is from 0 to height
          // -1 * height / 2 <= v.y*t + p.y <= height /2
          if (0 <= (vy * t1 + py) && (vy * t1 + py) <= height) {
            flagStep1 = true;
          }
        }

        if (t2 >= 0) {
          if (0 <= vy * t2 + py && vy * t2 + py <= height) {
            flagStep2 = true;
          }
        }
      }
    }

    float tUp = -1;
    float tDown = -1;

    // step 2. find the intersection of the two cap planes.
    {
      // in the obj case: v.y * t + p.y = 0 || v.y * t + p.y = height
      // v.y*t + p.y = height/2
      // v.y*t + p.y = -1 * height/2
      // (v.x*t + p.x)^2 + (v.y*t + p.y)^2 <= r^2;

      // if vy = 0, the ray is shooting horizontally to the x-z plane
      if (Math.abs(vy) != 0) {
        // initialize with -1 which is invalid


        tUp = (height - py) / vy;
        tDown = (0 - py) / vy;

        if (tUp >= 0) {
          if (Math.pow((vx * tUp + px), 2) + Math.pow((vz * tUp + pz), 2) <= radius * radius) {
            flagStepUp = true;
          }
        }

        if (tDown > 0) {
          if (Math.pow((vx * tDown + px), 2) + Math.pow((vz * tDown + pz), 2) <= radius * radius) {
            flagStepDown = true;
          }

        }

      }

    }

    if(flagStep1 || flagStep2 || flagStepDown || flagStepUp) {
      FloatValue fv1 = new FloatValue(t1, flagStep1);
      FloatValue fv2 = new FloatValue(t2, flagStep2);
      FloatValue fvUp = new FloatValue(tUp, flagStepUp);
      FloatValue fvDown = new FloatValue(tDown, flagStepDown);

//      if (myMin(fv1, fv2, fvUp, fvDown) * vy + py > 1)
//      System.out.println(myMin(fv1, fv2, fvUp, fvDown) * vy + py);

      return  Optional.of(RaytraceUtil.myMin(fv1, fv2, fvUp, fvDown));

    } else {
      return Optional.empty();
    }

  }

  /**
   * return the minimum non-negative number of a list of floats.
   * @param fv list of floats
   * @return the minimum non-negative number of a list of floats.
   */
  private static float myMin(FloatValue ... fv) {
    float min = Float.MAX_VALUE;
    for (FloatValue f: fv) {
      if (f.b && f.t >= 0 && f.t < min) {
        min = f.t;
      }
    }
    return min;
  }
}

class FloatValue {
  float t;
  boolean b;
  FloatValue(float t, boolean b) {
    this.t = t;
    this.b = b;
  }
}