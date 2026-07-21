package vesselreconstruction.geometry;

import java.awt.Point;

/**
 * Stores the shortest boundary-to-boundary distance between two lumen objects.
 */
public class BoundaryDistanceResult {

    private final double distance;
    private final Point pointOnLumenA;
    private final Point pointOnLumenB;

    public BoundaryDistanceResult(double distance,
                                  Point pointOnLumenA,
                                  Point pointOnLumenB) {

        this.distance = distance;
        this.pointOnLumenA = pointOnLumenA;
        this.pointOnLumenB = pointOnLumenB;
    }

    /**
     * Returns the shortest Euclidean distance.
     */
    public double getDistance() {
        return distance;
    }

    /**
     * Returns the closest boundary point on the first lumen.
     */
    public Point getPointOnLumenA() {
        return pointOnLumenA;
    }

    /**
     * Returns the closest boundary point on the second lumen.
     */
    public Point getPointOnLumenB() {
        return pointOnLumenB;
    }
}