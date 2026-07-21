package vesselreconstruction.model;

import java.awt.Point;

public class Bridge {

    private final Lumen lumenA;
    private final Lumen lumenB;

    private final Point pointA;
    private final Point pointB;

    private final double distance;

    private double score;

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
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}