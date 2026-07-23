package vesselreconstruction.reconstruction;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds allowed wood pixels that are enclosed by the lumen and a temporary
 * bridge-closing line, using an exterior flood fill through all non-blocked
 * coordinates in a local search grid.
 */
public class BridgeEnclosedWoodFinder {

    private static final int[] EIGHT_CONNECTED_DELTA_X =
            {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] EIGHT_CONNECTED_DELTA_Y =
            {-1, -1, -1, 0, 0, 1, 1, 1};

    /**
     * Returns allowed wood pixels that lie inside the local grid, are not
     * blocked, and were not reached by the exterior flood fill.
     */
    public Set<Point> findEnclosedWood(
            List<Point> originalLumenPixels,
            Set<Point> allowedWoodPixels,
            Point firstBridgePoint,
            Point secondBridgePoint,
            int bridgeOpeningHalfWidth) {

        validateInputs(
                originalLumenPixels,
                allowedWoodPixels,
                firstBridgePoint,
                secondBridgePoint,
                bridgeOpeningHalfWidth);

        Set<Long> blockedKeys = new HashSet<>(toCoordinateSet(originalLumenPixels));
        blockedKeys.addAll(rasterizeBridgeLine(
                firstBridgePoint,
                secondBridgePoint));

        int[] gridBounds = computeLocalGridBounds(
                originalLumenPixels,
                allowedWoodPixels);
        int gridMinX = gridBounds[0];
        int gridMinY = gridBounds[1];
        int gridMaxX = gridBounds[2];
        int gridMaxY = gridBounds[3];

        Set<Long> exteriorKeys = floodFillExterior(
                gridMinX,
                gridMinY,
                gridMaxX,
                gridMaxY,
                blockedKeys);

        Set<Long> enclosedWoodKeys = new HashSet<>();

        for (Point woodPixel : allowedWoodPixels) {
            long woodKey = encodeCoordinate(woodPixel.x, woodPixel.y);

            if (woodPixel.x < gridMinX
                    || woodPixel.x >= gridMaxX
                    || woodPixel.y < gridMinY
                    || woodPixel.y >= gridMaxY) {
                continue;
            }

            if (blockedKeys.contains(woodKey)) {
                continue;
            }

            if (!exteriorKeys.contains(woodKey)) {
                enclosedWoodKeys.add(woodKey);
            }
        }

        return keysToSortedSet(enclosedWoodKeys);
    }

    private static void validateInputs(
            List<Point> originalLumenPixels,
            Set<Point> allowedWoodPixels,
            Point firstBridgePoint,
            Point secondBridgePoint,
            int bridgeOpeningHalfWidth) {

        if (originalLumenPixels == null) {
            throw new IllegalArgumentException(
                    "originalLumenPixels must not be null.");
        }

        if (originalLumenPixels.isEmpty()) {
            throw new IllegalArgumentException(
                    "originalLumenPixels must not be empty.");
        }

        if (allowedWoodPixels == null) {
            throw new IllegalArgumentException(
                    "allowedWoodPixels must not be null.");
        }

        if (firstBridgePoint == null || secondBridgePoint == null) {
            throw new IllegalArgumentException("bridge points must not be null.");
        }

        if (bridgeOpeningHalfWidth < 0) {
            throw new IllegalArgumentException(
                    "bridgeOpeningHalfWidth must not be negative.");
        }

        Set<Long> lumenKeys = new HashSet<>();

        for (Point pixel : originalLumenPixels) {

            if (pixel == null) {
                throw new IllegalArgumentException(
                        "originalLumenPixels must not contain null values.");
            }

            if (!lumenKeys.add(encodeCoordinate(pixel.x, pixel.y))) {
                throw new IllegalArgumentException(
                        "originalLumenPixels must not contain duplicate "
                                + "coordinates.");
            }
        }

        for (Point woodPixel : allowedWoodPixels) {

            if (woodPixel == null) {
                throw new IllegalArgumentException(
                        "allowedWoodPixels must not contain null values.");
            }

            long woodKey = encodeCoordinate(woodPixel.x, woodPixel.y);

            if (lumenKeys.contains(woodKey)) {
                throw new IllegalArgumentException(
                        "allowedWoodPixels must not overlap originalLumenPixels.");
            }
        }

        if (!lumenKeys.contains(
                encodeCoordinate(firstBridgePoint.x, firstBridgePoint.y))) {
            throw new IllegalArgumentException(
                    "firstBridgePoint must be contained in originalLumenPixels.");
        }

        if (!lumenKeys.contains(
                encodeCoordinate(secondBridgePoint.x, secondBridgePoint.y))) {
            throw new IllegalArgumentException(
                    "secondBridgePoint must be contained in originalLumenPixels.");
        }
    }

    private static int[] computeLocalGridBounds(
            List<Point> originalLumenPixels,
            Set<Point> allowedWoodPixels) {

        int lumenMinX = Integer.MAX_VALUE;
        int lumenMinY = Integer.MAX_VALUE;
        int lumenMaxX = Integer.MIN_VALUE;
        int lumenMaxY = Integer.MIN_VALUE;

        for (Point pixel : originalLumenPixels) {
            lumenMinX = Math.min(lumenMinX, pixel.x);
            lumenMinY = Math.min(lumenMinY, pixel.y);
            lumenMaxX = Math.max(lumenMaxX, pixel.x);
            lumenMaxY = Math.max(lumenMaxY, pixel.y);
        }

        int searchMargin = 0;

        for (Point woodPixel : allowedWoodPixels) {
            searchMargin = Math.max(searchMargin, lumenMinX - woodPixel.x);
            searchMargin = Math.max(searchMargin, woodPixel.x - lumenMaxX);
            searchMargin = Math.max(searchMargin, lumenMinY - woodPixel.y);
            searchMargin = Math.max(searchMargin, woodPixel.y - lumenMaxY);
        }

        return new int[] {
                lumenMinX - searchMargin,
                lumenMinY - searchMargin,
                lumenMaxX + searchMargin + 1,
                lumenMaxY + searchMargin + 1
        };
    }

    private static Set<Long> floodFillExterior(
            int gridMinX,
            int gridMinY,
            int gridMaxX,
            int gridMaxY,
            Set<Long> blockedKeys) {

        Set<Long> exteriorKeys = new HashSet<>();
        List<Long> stack = new ArrayList<>();

        for (int x = gridMinX; x < gridMaxX; x++) {
            enqueueExteriorCandidate(
                    stack,
                    x,
                    gridMinY,
                    gridMinX,
                    gridMinY,
                    gridMaxX,
                    gridMaxY,
                    blockedKeys,
                    exteriorKeys);
            enqueueExteriorCandidate(
                    stack,
                    x,
                    gridMaxY - 1,
                    gridMinX,
                    gridMinY,
                    gridMaxX,
                    gridMaxY,
                    blockedKeys,
                    exteriorKeys);
        }

        for (int y = gridMinY; y < gridMaxY; y++) {
            enqueueExteriorCandidate(
                    stack,
                    gridMinX,
                    y,
                    gridMinX,
                    gridMinY,
                    gridMaxX,
                    gridMaxY,
                    blockedKeys,
                    exteriorKeys);
            enqueueExteriorCandidate(
                    stack,
                    gridMaxX - 1,
                    y,
                    gridMinX,
                    gridMinY,
                    gridMaxX,
                    gridMaxY,
                    blockedKeys,
                    exteriorKeys);
        }

        while (!stack.isEmpty()) {
            long currentKey = stack.remove(stack.size() - 1);

            if (!exteriorKeys.add(currentKey)) {
                continue;
            }

            Point currentPoint = decodeCoordinate(currentKey);

            for (int index = 0; index < EIGHT_CONNECTED_DELTA_X.length; index++) {
                int neighborX =
                        currentPoint.x + EIGHT_CONNECTED_DELTA_X[index];
                int neighborY =
                        currentPoint.y + EIGHT_CONNECTED_DELTA_Y[index];

                enqueueExteriorCandidate(
                        stack,
                        neighborX,
                        neighborY,
                        gridMinX,
                        gridMinY,
                        gridMaxX,
                        gridMaxY,
                        blockedKeys,
                        exteriorKeys);
            }
        }

        return exteriorKeys;
    }

    private static void enqueueExteriorCandidate(
            List<Long> stack,
            int x,
            int y,
            int gridMinX,
            int gridMinY,
            int gridMaxX,
            int gridMaxY,
            Set<Long> blockedKeys,
            Set<Long> exteriorKeys) {

        if (x < gridMinX || x >= gridMaxX || y < gridMinY || y >= gridMaxY) {
            return;
        }

        long key = encodeCoordinate(x, y);

        if (blockedKeys.contains(key) || exteriorKeys.contains(key)) {
            return;
        }

        stack.add(key);
    }

    private static Set<Long> rasterizeBridgeLine(
            Point firstBridgePoint,
            Point secondBridgePoint) {

        Set<Long> bridgeLineKeys = new HashSet<>();
        int startX = firstBridgePoint.x;
        int startY = firstBridgePoint.y;
        int endX = secondBridgePoint.x;
        int endY = secondBridgePoint.y;
        int deltaX = Math.abs(endX - startX);
        int deltaY = Math.abs(endY - startY);
        int stepX = startX < endX ? 1 : -1;
        int stepY = startY < endY ? 1 : -1;
        int error = deltaX - deltaY;
        int currentX = startX;
        int currentY = startY;

        while (true) {
            bridgeLineKeys.add(encodeCoordinate(currentX, currentY));

            if (currentX == endX && currentY == endY) {
                break;
            }

            int doubledError = 2 * error;

            if (doubledError > -deltaY) {
                error -= deltaY;
                currentX += stepX;
            }

            if (doubledError < deltaX) {
                error += deltaX;
                currentY += stepY;
            }
        }

        return bridgeLineKeys;
    }

    private static Set<Long> toCoordinateSet(Iterable<Point> pixels) {

        Set<Long> keys = new HashSet<>();

        for (Point pixel : pixels) {
            keys.add(encodeCoordinate(pixel.x, pixel.y));
        }

        return keys;
    }

    private static Set<Point> keysToSortedSet(Set<Long> keys) {

        List<Point> points = new ArrayList<>(keys.size());

        for (long key : keys) {
            points.add(decodeCoordinate(key));
        }

        Collections.sort(
                points,
                Comparator.comparingInt((Point point) -> point.y)
                        .thenComparingInt(point -> point.x));

        Set<Point> sortedSet = new LinkedHashSet<>();

        for (Point point : points) {
            sortedSet.add(new Point(point.x, point.y));
        }

        return sortedSet;
    }

    private static long encodeCoordinate(int x, int y) {
        return (((long) y) << 32) | (x & 0xFFFFFFFFL);
    }

    private static Point decodeCoordinate(long key) {
        int x = (int) key;
        int y = (int) (key >> 32);
        return new Point(x, y);
    }
}
