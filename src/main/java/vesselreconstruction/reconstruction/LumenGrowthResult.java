package vesselreconstruction.reconstruction;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable result of a circularity-guided diagnostic lumen growth simulation.
 */
public final class LumenGrowthResult {

    private final Point seedPoint;
    private final Point firstBridgePoint;
    private final Point secondBridgePoint;
    private final List<Point> originalLumenPixels;
    private final List<Point> openedLumenPixels;
    private final List<Point> bestLumenPixels;
    private final List<Point> directlyRemovedBridgePixels;
    private final List<Point> addedWoodPixels;
    private final double originalCircularity;
    private final double bestCircularity;
    private final int bestGrowthIteration;
    private final int performedGrowthIterations;
    private final boolean circularityImproved;
    private final boolean stoppedBecauseNoFrontier;
    private final boolean stoppedBecausePatienceExhausted;

    public LumenGrowthResult(
            Point seedPoint,
            Point firstBridgePoint,
            Point secondBridgePoint,
            List<Point> originalLumenPixels,
            List<Point> openedLumenPixels,
            List<Point> bestLumenPixels,
            List<Point> directlyRemovedBridgePixels,
            List<Point> addedWoodPixels,
            double originalCircularity,
            double bestCircularity,
            int bestGrowthIteration,
            int performedGrowthIterations,
            boolean stoppedBecauseNoFrontier,
            boolean stoppedBecausePatienceExhausted) {

        if (seedPoint == null
                || firstBridgePoint == null
                || secondBridgePoint == null) {
            throw new IllegalArgumentException("points must not be null.");
        }

        if (originalLumenPixels == null
                || openedLumenPixels == null
                || bestLumenPixels == null
                || directlyRemovedBridgePixels == null
                || addedWoodPixels == null) {
            throw new IllegalArgumentException("pixel collections must not be null.");
        }

        validateCircularity(originalCircularity);
        validateCircularity(bestCircularity);

        if (bestGrowthIteration < 0 || performedGrowthIterations < 0) {
            throw new IllegalArgumentException("iteration counts must not be negative.");
        }

        if (bestGrowthIteration > performedGrowthIterations) {
            throw new IllegalArgumentException(
                    "bestGrowthIteration must not exceed performedGrowthIterations.");
        }

        if (directlyRemovedBridgePixels.size()
                > originalLumenPixels.size() - openedLumenPixels.size()) {
            throw new IllegalArgumentException(
                    "directlyRemovedBridgePixels is inconsistent with opened lumen size.");
        }

        if (addedWoodPixels.size()
                != bestLumenPixels.size() - countRetainedOriginalPixels(
                        originalLumenPixels,
                        bestLumenPixels)) {
            throw new IllegalArgumentException(
                    "addedWoodPixels size is inconsistent with best lumen pixels.");
        }

        this.seedPoint = copyPoint(seedPoint);
        this.firstBridgePoint = copyPoint(firstBridgePoint);
        this.secondBridgePoint = copyPoint(secondBridgePoint);
        this.originalLumenPixels = copyPointList(originalLumenPixels);
        this.openedLumenPixels = copyPointList(openedLumenPixels);
        this.bestLumenPixels = copyPointList(bestLumenPixels);
        this.directlyRemovedBridgePixels = copyPointList(directlyRemovedBridgePixels);
        this.addedWoodPixels = copyPointList(addedWoodPixels);
        this.originalCircularity = originalCircularity;
        this.bestCircularity = bestCircularity;
        this.bestGrowthIteration = bestGrowthIteration;
        this.performedGrowthIterations = performedGrowthIterations;
        this.circularityImproved = bestCircularity > originalCircularity;
        this.stoppedBecauseNoFrontier = stoppedBecauseNoFrontier;
        this.stoppedBecausePatienceExhausted = stoppedBecausePatienceExhausted;
    }

    public Point getSeedPoint() {
        return copyPoint(seedPoint);
    }

    public Point getFirstBridgePoint() {
        return copyPoint(firstBridgePoint);
    }

    public Point getSecondBridgePoint() {
        return copyPoint(secondBridgePoint);
    }

    public List<Point> getOriginalLumenPixels() {
        return Collections.unmodifiableList(originalLumenPixels);
    }

    public List<Point> getOpenedLumenPixels() {
        return Collections.unmodifiableList(openedLumenPixels);
    }

    public List<Point> getBestLumenPixels() {
        return Collections.unmodifiableList(bestLumenPixels);
    }

    public List<Point> getDirectlyRemovedBridgePixels() {
        return Collections.unmodifiableList(directlyRemovedBridgePixels);
    }

    public List<Point> getAddedWoodPixels() {
        return Collections.unmodifiableList(addedWoodPixels);
    }

    public double getOriginalCircularity() {
        return originalCircularity;
    }

    public double getBestCircularity() {
        return bestCircularity;
    }

    public int getBestGrowthIteration() {
        return bestGrowthIteration;
    }

    public int getPerformedGrowthIterations() {
        return performedGrowthIterations;
    }

    public boolean isCircularityImproved() {
        return circularityImproved;
    }

    public boolean isStoppedBecauseNoFrontier() {
        return stoppedBecauseNoFrontier;
    }

    public boolean isStoppedBecausePatienceExhausted() {
        return stoppedBecausePatienceExhausted;
    }

    private static void validateCircularity(double circularity) {

        if (!Double.isFinite(circularity) || circularity < 0.0) {
            throw new IllegalArgumentException(
                    "circularity values must be finite and non-negative.");
        }
    }

    private static int countRetainedOriginalPixels(
            List<Point> originalLumenPixels,
            List<Point> bestLumenPixels) {

        java.util.Set<Long> bestKeys = new java.util.HashSet<>();

        for (Point pixel : bestLumenPixels) {
            bestKeys.add(encodeCoordinate(pixel.x, pixel.y));
        }

        int retainedCount = 0;

        for (Point pixel : originalLumenPixels) {

            if (bestKeys.contains(encodeCoordinate(pixel.x, pixel.y))) {
                retainedCount++;
            }
        }

        return retainedCount;
    }

    private static long encodeCoordinate(int x, int y) {
        return (((long) y) << 32) | (x & 0xFFFFFFFFL);
    }

    private static Point copyPoint(Point point) {
        return new Point(point.x, point.y);
    }

    private static List<Point> copyPointList(List<Point> points) {

        List<Point> copies = new ArrayList<>(points.size());

        for (Point point : points) {
            copies.add(copyPoint(point));
        }

        return copies;
    }
}
