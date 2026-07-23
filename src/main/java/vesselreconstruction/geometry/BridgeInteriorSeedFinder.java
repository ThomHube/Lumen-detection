package vesselreconstruction.geometry;

import java.awt.Point;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds the interior lumen seed maximally distant from both bridge endpoints.
 */
public class BridgeInteriorSeedFinder {

    private static final Comparator<SeedCandidate> SEED_ORDER =
            Comparator
                    .comparingDouble(SeedCandidate::getSeedScore)
                    .reversed()
                    .thenComparingInt(SeedCandidate::getY)
                    .thenComparingInt(SeedCandidate::getX);

    /**
     * Finds the lumen pixel with the greatest minimum distance to both bridge
     * endpoints.
     *
     * @param lumenPixels complete foreground pixels of one lumen
     * @param firstBridgePoint first selected bridge endpoint
     * @param secondBridgePoint second selected bridge endpoint
     * @return defensive copy of the selected seed pixel
     */
    public Point findSeed(
            List<Point> lumenPixels,
            Point firstBridgePoint,
            Point secondBridgePoint) {

        validateInputs(lumenPixels, firstBridgePoint, secondBridgePoint);

        SeedCandidate bestCandidate = null;

        for (Point pixel : lumenPixels) {
            SeedCandidate candidate = new SeedCandidate(
                    pixel,
                    seedScore(pixel, firstBridgePoint, secondBridgePoint));

            if (bestCandidate == null
                    || SEED_ORDER.compare(candidate, bestCandidate) < 0) {
                bestCandidate = candidate;
            }
        }

        return new Point(bestCandidate.getX(), bestCandidate.getY());
    }

    private static double seedScore(
            Point pixel,
            Point firstBridgePoint,
            Point secondBridgePoint) {

        double distanceToFirst = euclideanDistance(pixel, firstBridgePoint);
        double distanceToSecond = euclideanDistance(pixel, secondBridgePoint);

        return Math.min(distanceToFirst, distanceToSecond);
    }

    private static void validateInputs(
            List<Point> lumenPixels,
            Point firstBridgePoint,
            Point secondBridgePoint) {

        if (lumenPixels == null) {
            throw new IllegalArgumentException("lumenPixels must not be null.");
        }

        if (lumenPixels.isEmpty()) {
            throw new IllegalArgumentException("lumenPixels must not be empty.");
        }

        if (firstBridgePoint == null || secondBridgePoint == null) {
            throw new IllegalArgumentException("bridge points must not be null.");
        }

        Set<Long> seenCoordinates = new HashSet<>();

        for (Point pixel : lumenPixels) {

            if (pixel == null) {
                throw new IllegalArgumentException(
                        "lumenPixels must not contain null values.");
            }

            if (!seenCoordinates.add(encodeCoordinate(pixel.x, pixel.y))) {
                throw new IllegalArgumentException(
                        "lumenPixels must not contain duplicate coordinates.");
            }
        }

        if (!seenCoordinates.contains(
                encodeCoordinate(firstBridgePoint.x, firstBridgePoint.y))) {
            throw new IllegalArgumentException(
                    "firstBridgePoint must be contained in lumenPixels.");
        }

        if (!seenCoordinates.contains(
                encodeCoordinate(secondBridgePoint.x, secondBridgePoint.y))) {
            throw new IllegalArgumentException(
                    "secondBridgePoint must be contained in lumenPixels.");
        }
    }

    private static double euclideanDistance(Point left, Point right) {

        double deltaX = left.x - right.x;
        double deltaY = left.y - right.y;

        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    private static long encodeCoordinate(int x, int y) {
        return (((long) y) << 32) | (x & 0xFFFFFFFFL);
    }

    private static final class SeedCandidate {

        private final int x;
        private final int y;
        private final double seedScore;

        private SeedCandidate(Point pixel, double seedScore) {
            this.x = pixel.x;
            this.y = pixel.y;
            this.seedScore = seedScore;
        }

        private int getX() {
            return x;
        }

        private int getY() {
            return y;
        }

        private double getSeedScore() {
            return seedScore;
        }
    }
}
