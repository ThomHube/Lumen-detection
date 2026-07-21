package vesselreconstruction.model;

import java.awt.Point;

/**
 * Represents a potential bridge between two neighbouring lumen objects.
 * A bridge is defined by the closest pair of boundary points and the
 * Euclidean distance between them.
 */
public class Bridge {

    private final Lumen lumenA;
    private final Lumen lumenB;

    private final Point pointA;
    private final Point pointB;

    private final double distance;

    /**
     * Confidence score assigned by the bridge detection/validation pipeline.
     */
    private double bridgeScore;

    public Bridge(Lumen lumenA,
                  Lumen lumenB,
                  Point pointA,
                  Point pointB,
                  double distance) {

        this.lumenA = lumenA;
        this.lumenB = lumenB;
        this.pointA = pointA;
        this.pointB = pointB;
        this.distance = distance;
    }

    public Lumen getLumenA() {
        return lumenA;
    }

    public Lumen getLumenB() {
        return lumenB;
    }

    public Point getPointA() {
        return pointA;
    }

    public Point getPointB() {
        return pointB;
    }

    public double getDistance() {
        return distance;
    }

    public double getScore() {
        return bridgeScore;
    }

    public void setScore(double score) {
        this.bridgeScore = score;
    }
}