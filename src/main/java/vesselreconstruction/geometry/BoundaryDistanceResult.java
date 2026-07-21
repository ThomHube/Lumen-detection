package vesselreconstruction.geometry;

import java.awt.Point;

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

    public double getDistance() {
        return distance;
    }

    public Point getPointOnLumenA() {
        return pointOnLumenA;
    }

    public Point getPointOnLumenB() {
        return pointOnLumenB;
    }
}