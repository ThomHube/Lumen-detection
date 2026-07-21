package vesselreconstruction.geometry;

import vesselreconstruction.model.Lumen;

import java.awt.Point;

public class BoundaryDistanceCalculator {

    public BoundaryDistanceResult calculate(Lumen lumenA, Lumen lumenB) {

        double minDistanceSquared = Double.MAX_VALUE;

        Point bestPointA = null;
        Point bestPointB = null;

        for (Point pointA : lumenA.getBoundaryPixels()) {

            for (Point pointB : lumenB.getBoundaryPixels()) {

                double dx = pointA.x - pointB.x;
                double dy = pointA.y - pointB.y;

                double distanceSquared = dx * dx + dy * dy;

                if (distanceSquared < minDistanceSquared) {

                    minDistanceSquared = distanceSquared;
                    bestPointA = pointA;
                    bestPointB = pointB;
                }
            }
        }

        double distance = Math.sqrt(minDistanceSquared);

        return new BoundaryDistanceResult(
                distance,
                bestPointA,
                bestPointB
        );
    }
}