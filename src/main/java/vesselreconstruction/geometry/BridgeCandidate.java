package vesselreconstruction.geometry;

import java.awt.Point;

/**
 * Immutable bridge-candidate pair of boundary peak points that are spatially
 * close but separated along the ordered contour.
 */
public final class BridgeCandidate {

    private final int firstBoundaryIndex;
    private final int secondBoundaryIndex;
    private final Point firstPoint;
    private final Point secondPoint;
    private final double euclideanDistance;
    private final int circularBoundarySeparation;

    public BridgeCandidate(
            int firstBoundaryIndex,
            int secondBoundaryIndex,
            Point firstPoint,
            Point secondPoint,
            double euclideanDistance,
            int circularBoundarySeparation) {

        if (firstBoundaryIndex >= secondBoundaryIndex) {
            throw new IllegalArgumentException(
                    "firstBoundaryIndex must be smaller than secondBoundaryIndex.");
        }

        if (firstPoint == null || secondPoint == null) {
            throw new IllegalArgumentException("boundary points must not be null.");
        }

        this.firstBoundaryIndex = firstBoundaryIndex;
        this.secondBoundaryIndex = secondBoundaryIndex;
        this.firstPoint = copyPoint(firstPoint);
        this.secondPoint = copyPoint(secondPoint);
        this.euclideanDistance = euclideanDistance;
        this.circularBoundarySeparation = circularBoundarySeparation;
    }

    public int getFirstBoundaryIndex() {
        return firstBoundaryIndex;
    }

    public int getSecondBoundaryIndex() {
        return secondBoundaryIndex;
    }

    public Point getFirstPoint() {
        return copyPoint(firstPoint);
    }

    public Point getSecondPoint() {
        return copyPoint(secondPoint);
    }

    public double getEuclideanDistance() {
        return euclideanDistance;
    }

    public int getCircularBoundarySeparation() {
        return circularBoundarySeparation;
    }

    static BridgeCandidate createCanonical(
            int leftBoundaryIndex,
            int rightBoundaryIndex,
            Point leftPoint,
            Point rightPoint,
            double euclideanDistance,
            int circularBoundarySeparation) {

        if (leftBoundaryIndex < rightBoundaryIndex) {
            return new BridgeCandidate(
                    leftBoundaryIndex,
                    rightBoundaryIndex,
                    leftPoint,
                    rightPoint,
                    euclideanDistance,
                    circularBoundarySeparation);
        }

        return new BridgeCandidate(
                rightBoundaryIndex,
                leftBoundaryIndex,
                rightPoint,
                leftPoint,
                euclideanDistance,
                circularBoundarySeparation);
    }

    private static Point copyPoint(Point point) {
        return new Point(point.x, point.y);
    }
}
