package vesselreconstruction.geometry;

import vesselreconstruction.model.Lumen;

import java.awt.Point;

public class BoundaryDistanceCalculator {

    public BoundaryDistanceResult calculate(Lumen lumenA, Lumen lumenB) {

        double minimumDistanceSquared = Double.MAX_VALUE;

        Point closestPointOnLumenA = null;
        Point closestPointOnLumenB = null;

        for (Point pointA : lumenA.getBoundaryPixels()) {

            for (Point pointB : lumenB.getBoundaryPixels()) {

                double distanceSquared = squaredDistance(pointA, pointB);

                if (distanceSquared < minimumDistanceSquared) {

                    minimumDistanceSquared = distanceSquared;
                    closestPointOnLumenA = pointA;
                    closestPointOnLumenB = pointB;
                }
            }
        }

        return new BoundaryDistanceResult(
                Math.sqrt(minimumDistanceSquared),
                closestPointOnLumenA,
                closestPointOnLumenB
        );
    }

    private double squaredDistance(Point pointA, Point pointB) {

        double dx = pointA.x - pointB.x;
        double dy = pointA.y - pointB.y;

        return dx * dx + dy * dy;
    }
}