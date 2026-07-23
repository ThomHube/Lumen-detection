package vesselreconstruction.reconstruction;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Performs non-destructive single-pixel circularity-guided lumen reconstruction
 * after opening a selected bridge.
 */
public class SinglePixelLumenReconstructor {

    private static final int[] EIGHT_CONNECTED_DELTA_X =
            {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] EIGHT_CONNECTED_DELTA_Y =
            {-1, -1, -1, 0, 0, 1, 1, 1};

    private static final int[] MOORE_DELTA_X = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] MOORE_DELTA_Y = {-1, -1, 0, 1, 1, 1, 0, -1};

    private final BridgeEnclosedWoodFinder bridgeEnclosedWoodFinder =
            new BridgeEnclosedWoodFinder();

    /**
     * Simulates single-pixel reconstruction on copied lumen and wood pixels.
     */
    public SinglePixelReconstructionResult reconstructToMaximumCircularity(
            List<Point> originalLumenPixels,
            Set<Point> allowedWoodPixels,
            Point firstBridgePoint,
            Point secondBridgePoint,
            Point seedPoint,
            int bridgeOpeningHalfWidth,
            int maximumReconstructionSteps,
            int nonImprovingStepPatience) {

        validateInputs(
                originalLumenPixels,
                allowedWoodPixels,
                firstBridgePoint,
                secondBridgePoint,
                seedPoint,
                bridgeOpeningHalfWidth,
                maximumReconstructionSteps,
                nonImprovingStepPatience);

        Set<Long> originalKeys = toCoordinateSet(originalLumenPixels);
        Set<Long> removedBridgeKeys = rasterizeBridgeOpening(
                firstBridgePoint,
                secondBridgePoint,
                bridgeOpeningHalfWidth,
                originalKeys);
        Set<Long> openedCandidateKeys = new HashSet<>(originalKeys);
        openedCandidateKeys.removeAll(removedBridgeKeys);

        Set<Long> seedComponentKeys = findComponentContaining(
                openedCandidateKeys,
                seedPoint);

        if (!seedComponentKeys.contains(encodeCoordinate(seedPoint.x, seedPoint.y))) {
            throw new IllegalStateException(
                    "seedPoint was removed by bridge opening.");
        }

        List<Point> openedLumenPixels = keysToSortedPoints(seedComponentKeys);
        double originalCircularity = calculateCircularity(openedLumenPixels);

        Set<Point> bridgeEnclosedWood = bridgeEnclosedWoodFinder.findEnclosedWood(
                originalLumenPixels,
                allowedWoodPixels,
                firstBridgePoint,
                secondBridgePoint,
                bridgeOpeningHalfWidth);
        Set<Long> enclosedWoodKeys = toCoordinateSet(bridgeEnclosedWood);
        List<Point> bridgeEnclosedWoodPixels = keysToSortedPoints(enclosedWoodKeys);

        Set<Long> currentLumenKeys = new HashSet<>(seedComponentKeys);
        Set<Long> bestLumenKeys = new HashSet<>(seedComponentKeys);
        Set<Long> addedWoodKeys = new HashSet<>();

        double bestCircularity = originalCircularity;
        int bestReconstructionStep = 0;
        int performedReconstructionSteps = 0;
        int consecutiveNonImprovingSteps = 0;
        boolean stoppedBecauseNoCandidate = false;
        boolean stoppedBecausePatienceExhausted = false;

        for (int step = 1; step <= maximumReconstructionSteps; step++) {
            Long candidateKey = findNextCandidate(
                    currentLumenKeys,
                    removedBridgeKeys,
                    enclosedWoodKeys,
                    addedWoodKeys,
                    firstBridgePoint,
                    secondBridgePoint);

            if (candidateKey == null) {
                stoppedBecauseNoCandidate = true;
                break;
            }

            currentLumenKeys.add(candidateKey);
            addedWoodKeys.add(candidateKey);
            performedReconstructionSteps = step;

            double currentCircularity = bestCircularity;

            try {
                currentCircularity =
                        calculateCircularity(keysToSortedPoints(currentLumenKeys));
            } catch (IllegalStateException exception) {
                consecutiveNonImprovingSteps++;

                if (consecutiveNonImprovingSteps >= nonImprovingStepPatience) {
                    stoppedBecausePatienceExhausted = true;
                    break;
                }

                continue;
            }

            if (currentCircularity > bestCircularity) {
                bestCircularity = currentCircularity;
                bestLumenKeys = new HashSet<>(currentLumenKeys);
                bestReconstructionStep = step;
                consecutiveNonImprovingSteps = 0;
            } else {
                consecutiveNonImprovingSteps++;

                if (consecutiveNonImprovingSteps >= nonImprovingStepPatience) {
                    stoppedBecausePatienceExhausted = true;
                    break;
                }
            }
        }

        List<Point> bestLumenPixels = keysToSortedPoints(bestLumenKeys);
        List<Point> addedWoodPixels =
                collectAddedWoodPixels(originalKeys, bestLumenKeys);
        List<Point> removedBridgePixels = keysToSortedPoints(removedBridgeKeys);

        return new SinglePixelReconstructionResult(
                seedPoint,
                firstBridgePoint,
                secondBridgePoint,
                originalLumenPixels,
                openedLumenPixels,
                bestLumenPixels,
                removedBridgePixels,
                bridgeEnclosedWoodPixels,
                addedWoodPixels,
                originalCircularity,
                bestCircularity,
                bestReconstructionStep,
                performedReconstructionSteps,
                stoppedBecauseNoCandidate,
                stoppedBecausePatienceExhausted);
    }

    private static Long findNextCandidate(
            Set<Long> currentLumenKeys,
            Set<Long> removedBridgeKeys,
            Set<Long> enclosedWoodKeys,
            Set<Long> addedWoodKeys,
            Point firstBridgePoint,
            Point secondBridgePoint) {

        Long bestCandidate = null;
        Point bestPoint = null;

        for (long woodKey : enclosedWoodKeys) {

            if (addedWoodKeys.contains(woodKey)) {
                continue;
            }

            Point woodPoint = decodeCoordinate(woodKey);

            if (!isCandidateAdjacent(
                    woodPoint,
                    currentLumenKeys,
                    removedBridgeKeys,
                    addedWoodKeys)) {
                continue;
            }

            if (bestPoint == null
                    || compareBridgeDistanceCandidates(
                            woodPoint,
                            bestPoint,
                            firstBridgePoint,
                            secondBridgePoint) < 0) {
                bestPoint = woodPoint;
                bestCandidate = woodKey;
            }
        }

        return bestCandidate;
    }

    /**
     * Returns the smaller squared Euclidean distance from {@code point} to either
     * bridge endpoint. Used only for deterministic candidate ordering.
     */
    private static long minimumSquaredBridgeDistance(
            Point point,
            Point firstBridgePoint,
            Point secondBridgePoint) {

        long deltaXToFirst = (long) point.x - firstBridgePoint.x;
        long deltaYToFirst = (long) point.y - firstBridgePoint.y;
        long squaredDistanceToFirst =
                deltaXToFirst * deltaXToFirst + deltaYToFirst * deltaYToFirst;

        long deltaXToSecond = (long) point.x - secondBridgePoint.x;
        long deltaYToSecond = (long) point.y - secondBridgePoint.y;
        long squaredDistanceToSecond =
                deltaXToSecond * deltaXToSecond + deltaYToSecond * deltaYToSecond;

        return Math.min(squaredDistanceToFirst, squaredDistanceToSecond);
    }

    private static int compareBridgeDistanceCandidates(
            Point left,
            Point right,
            Point firstBridgePoint,
            Point secondBridgePoint) {

        long leftSquaredDistanceScore = minimumSquaredBridgeDistance(
                left,
                firstBridgePoint,
                secondBridgePoint);
        long rightSquaredDistanceScore = minimumSquaredBridgeDistance(
                right,
                firstBridgePoint,
                secondBridgePoint);
        int bySquaredDistanceScore =
                Long.compare(rightSquaredDistanceScore, leftSquaredDistanceScore);

        if (bySquaredDistanceScore != 0) {
            return bySquaredDistanceScore;
        }

        int byY = Integer.compare(left.y, right.y);

        if (byY != 0) {
            return byY;
        }

        return Integer.compare(left.x, right.x);
    }

    private static boolean isCandidateAdjacent(
            Point woodPoint,
            Set<Long> currentLumenKeys,
            Set<Long> removedBridgeKeys,
            Set<Long> addedWoodKeys) {

        if (isEightAdjacentToAny(woodPoint, currentLumenKeys)) {
            return true;
        }

        if (addedWoodKeys.isEmpty()
                && isEightAdjacentToAny(woodPoint, removedBridgeKeys)) {
            return true;
        }

        return false;
    }

    private static boolean isEightAdjacentToAny(
            Point point,
            Set<Long> targetKeys) {

        for (long targetKey : targetKeys) {
            Point targetPoint = decodeCoordinate(targetKey);
            int deltaX = Math.abs(point.x - targetPoint.x);
            int deltaY = Math.abs(point.y - targetPoint.y);

            if (deltaX <= 1 && deltaY <= 1 && (deltaX != 0 || deltaY != 0)) {
                return true;
            }
        }

        return false;
    }

    private static void validateInputs(
            List<Point> originalLumenPixels,
            Set<Point> allowedWoodPixels,
            Point firstBridgePoint,
            Point secondBridgePoint,
            Point seedPoint,
            int bridgeOpeningHalfWidth,
            int maximumReconstructionSteps,
            int nonImprovingStepPatience) {

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

        if (firstBridgePoint == null
                || secondBridgePoint == null
                || seedPoint == null) {
            throw new IllegalArgumentException("points must not be null.");
        }

        if (bridgeOpeningHalfWidth < 0) {
            throw new IllegalArgumentException(
                    "bridgeOpeningHalfWidth must not be negative.");
        }

        if (maximumReconstructionSteps < 1) {
            throw new IllegalArgumentException(
                    "maximumReconstructionSteps must be at least 1.");
        }

        if (nonImprovingStepPatience < 1) {
            throw new IllegalArgumentException(
                    "nonImprovingStepPatience must be at least 1.");
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

        if (!lumenKeys.contains(encodeCoordinate(seedPoint.x, seedPoint.y))) {
            throw new IllegalArgumentException(
                    "seedPoint must be contained in originalLumenPixels.");
        }
    }

    private static Set<Long> rasterizeBridgeOpening(
            Point firstBridgePoint,
            Point secondBridgePoint,
            int bridgeOpeningHalfWidth,
            Set<Long> originalKeys) {

        Set<Long> removedKeys = new HashSet<>();
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
            addOpeningNeighborhood(
                    removedKeys,
                    currentX,
                    currentY,
                    bridgeOpeningHalfWidth,
                    originalKeys);

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

        return removedKeys;
    }

    private static void addOpeningNeighborhood(
            Set<Long> removedKeys,
            int centerX,
            int centerY,
            int bridgeOpeningHalfWidth,
            Set<Long> originalKeys) {

        for (int deltaY = -bridgeOpeningHalfWidth;
                deltaY <= bridgeOpeningHalfWidth;
                deltaY++) {

            for (int deltaX = -bridgeOpeningHalfWidth;
                    deltaX <= bridgeOpeningHalfWidth;
                    deltaX++) {

                long key = encodeCoordinate(centerX + deltaX, centerY + deltaY);

                if (originalKeys.contains(key)) {
                    removedKeys.add(key);
                }
            }
        }
    }

    private static Set<Long> findComponentContaining(
            Set<Long> foregroundKeys,
            Point seedPoint) {

        long seedKey = encodeCoordinate(seedPoint.x, seedPoint.y);
        Set<Long> unvisited = new HashSet<>(foregroundKeys);
        Set<Long> componentKeys = new HashSet<>();
        List<Long> stack = new ArrayList<>();
        stack.add(seedKey);

        while (!stack.isEmpty()) {
            long currentKey = stack.remove(stack.size() - 1);

            if (!unvisited.remove(currentKey)) {
                continue;
            }

            componentKeys.add(currentKey);
            Point currentPoint = decodeCoordinate(currentKey);

            for (int index = 0; index < EIGHT_CONNECTED_DELTA_X.length; index++) {
                long neighborKey = encodeCoordinate(
                        currentPoint.x + EIGHT_CONNECTED_DELTA_X[index],
                        currentPoint.y + EIGHT_CONNECTED_DELTA_Y[index]);

                if (unvisited.contains(neighborKey)) {
                    stack.add(neighborKey);
                }
            }
        }

        return componentKeys;
    }

    private static List<Point> collectAddedWoodPixels(
            Set<Long> originalKeys,
            Set<Long> bestLumenKeys) {

        List<Point> addedWoodPixels = new ArrayList<>();

        for (long key : bestLumenKeys) {

            if (!originalKeys.contains(key)) {
                addedWoodPixels.add(decodeCoordinate(key));
            }
        }

        sortPoints(addedWoodPixels);
        return addedWoodPixels;
    }

    private static double calculateCircularity(List<Point> lumenPixels) {

        if (lumenPixels.isEmpty()) {
            return 0.0;
        }

        if (lumenPixels.size() == 1) {
            return 0.0;
        }

        List<Point> boundaryPixels = traceOuterBoundary(lumenPixels);

        if (boundaryPixels.size() < 2) {
            throw new IllegalStateException(
                    "Unable to produce a valid ordered boundary for circularity.");
        }

        double perimeter = calculatePerimeter(boundaryPixels);

        if (perimeter == 0.0) {
            return 0.0;
        }

        return 4.0 * Math.PI * lumenPixels.size()
                / (perimeter * perimeter);
    }

    private static List<Point> traceOuterBoundary(List<Point> lumenPixels) {

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Point pixel : lumenPixels) {
            minX = Math.min(minX, pixel.x);
            minY = Math.min(minY, pixel.y);
            maxX = Math.max(maxX, pixel.x);
            maxY = Math.max(maxY, pixel.y);
        }

        int gridWidth = maxX - minX + 3;
        int gridHeight = maxY - minY + 3;
        boolean[][] foreground = new boolean[gridHeight][gridWidth];

        for (Point pixel : lumenPixels) {
            foreground[pixel.y - minY + 1][pixel.x - minX + 1] = true;
        }

        Point startPixel = findStartPixel(lumenPixels);
        GridPoint startGrid = toGridPoint(startPixel, minX, minY);
        GridPoint initialBacktrackGrid = new GridPoint(startGrid.x - 1, startGrid.y);

        List<Point> contour = new ArrayList<>();
        contour.add(toImagePoint(startGrid, minX, minY));

        MooreStep firstStep = mooreStep(
                foreground,
                startGrid.x,
                startGrid.y,
                initialBacktrackGrid.x,
                initialBacktrackGrid.y);
        GridPoint firstNextGrid = firstStep.boundary;
        GridPoint currentGrid = firstNextGrid;
        GridPoint backtrackGrid = firstStep.backtrack;

        contour.add(toImagePoint(firstNextGrid, minX, minY));

        Set<DirectedTransition> visitedTransitions = new HashSet<>();
        visitedTransitions.add(new DirectedTransition(startGrid, firstNextGrid));

        int maxIterations = Math.max(32, lumenPixels.size() * 16);

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            MooreStep nextStep = mooreStep(
                    foreground,
                    currentGrid.x,
                    currentGrid.y,
                    backtrackGrid.x,
                    backtrackGrid.y);
            GridPoint nextBoundaryGrid = nextStep.boundary;

            if (currentGrid.equals(startGrid)
                    && nextBoundaryGrid.equals(firstNextGrid)) {
                return contour;
            }

            if (nextBoundaryGrid.equals(startGrid)) {
                currentGrid = startGrid;
                backtrackGrid = nextStep.backtrack;
                continue;
            }

            DirectedTransition transition =
                    new DirectedTransition(currentGrid, nextBoundaryGrid);

            if (!visitedTransitions.add(transition)) {
                throw new IllegalStateException(
                        "Moore boundary tracing failed during circularity "
                                + "calculation.");
            }

            contour.add(toImagePoint(nextBoundaryGrid, minX, minY));
            currentGrid = nextBoundaryGrid;
            backtrackGrid = nextStep.backtrack;
        }

        throw new IllegalStateException(
                "Moore boundary tracing failed during circularity calculation.");
    }

    private static Point findStartPixel(List<Point> pixels) {

        Point start = pixels.get(0);

        for (Point pixel : pixels) {

            if (pixel.y < start.y || (pixel.y == start.y && pixel.x < start.x)) {
                start = pixel;
            }
        }

        return start;
    }

    private static MooreStep mooreStep(
            boolean[][] foreground,
            int boundaryX,
            int boundaryY,
            int backtrackX,
            int backtrackY) {

        int backDirection = directionIndex(
                boundaryX,
                boundaryY,
                backtrackX,
                backtrackY);

        for (int scanOffset = 1; scanOffset <= 8; scanOffset++) {
            int direction = (backDirection + scanOffset) & 7;
            int neighborX = boundaryX + MOORE_DELTA_X[direction];
            int neighborY = boundaryY + MOORE_DELTA_Y[direction];

            if (foreground[neighborY][neighborX]) {
                int precedingDirection = (backDirection + scanOffset - 1) & 7;

                return new MooreStep(
                        new GridPoint(neighborX, neighborY),
                        new GridPoint(
                                boundaryX + MOORE_DELTA_X[precedingDirection],
                                boundaryY + MOORE_DELTA_Y[precedingDirection]));
            }
        }

        throw new IllegalStateException(
                "No foreground Moore neighbor found for boundary pixel.");
    }

    private static int directionIndex(
            int centerX,
            int centerY,
            int neighborX,
            int neighborY) {

        int deltaX = neighborX - centerX;
        int deltaY = neighborY - centerY;

        for (int direction = 0; direction < 8; direction++) {

            if (MOORE_DELTA_X[direction] == deltaX
                    && MOORE_DELTA_Y[direction] == deltaY) {
                return direction;
            }
        }

        throw new IllegalStateException(
                "Backtrack pixel is not a Moore neighbor of boundary pixel.");
    }

    private static double calculatePerimeter(List<Point> boundaryPixels) {

        double perimeter = 0.0;
        int boundarySize = boundaryPixels.size();

        for (int index = 0; index < boundarySize; index++) {
            Point currentPoint = boundaryPixels.get(index);
            Point nextPoint = boundaryPixels.get((index + 1) % boundarySize);
            perimeter += euclideanDistance(currentPoint, nextPoint);
        }

        return perimeter;
    }

    private static double euclideanDistance(Point left, Point right) {

        double deltaX = left.x - right.x;
        double deltaY = left.y - right.y;

        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    private static GridPoint toGridPoint(Point imagePoint, int minX, int minY) {
        return new GridPoint(
                imagePoint.x - minX + 1,
                imagePoint.y - minY + 1);
    }

    private static Point toImagePoint(GridPoint gridPoint, int minX, int minY) {
        return new Point(
                gridPoint.x + minX - 1,
                gridPoint.y + minY - 1);
    }

    private static Set<Long> toCoordinateSet(Iterable<Point> pixels) {

        Set<Long> keys = new HashSet<>();

        for (Point pixel : pixels) {
            keys.add(encodeCoordinate(pixel.x, pixel.y));
        }

        return keys;
    }

    private static List<Point> keysToSortedPoints(Set<Long> keys) {

        List<Point> points = new ArrayList<>(keys.size());

        for (long key : keys) {
            points.add(decodeCoordinate(key));
        }

        sortPoints(points);
        return points;
    }

    private static void sortPoints(List<Point> points) {
        Collections.sort(
                points,
                Comparator.comparingInt((Point point) -> point.y)
                        .thenComparingInt(point -> point.x));
    }

    private static long encodeCoordinate(int x, int y) {
        return (((long) y) << 32) | (x & 0xFFFFFFFFL);
    }

    private static Point decodeCoordinate(long key) {
        int x = (int) key;
        int y = (int) (key >> 32);
        return new Point(x, y);
    }

    private static final class GridPoint {

        private final int x;
        private final int y;

        private GridPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object object) {

            if (this == object) {
                return true;
            }

            if (!(object instanceof GridPoint)) {
                return false;
            }

            GridPoint other = (GridPoint) object;

            return x == other.x && y == other.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    private static final class DirectedTransition {

        private final GridPoint previous;
        private final GridPoint current;

        private DirectedTransition(GridPoint previous, GridPoint current) {
            this.previous = previous;
            this.current = current;
        }

        @Override
        public boolean equals(Object object) {

            if (this == object) {
                return true;
            }

            if (!(object instanceof DirectedTransition)) {
                return false;
            }

            DirectedTransition other = (DirectedTransition) object;

            return previous.equals(other.previous)
                    && current.equals(other.current);
        }

        @Override
        public int hashCode() {
            return Objects.hash(previous, current);
        }
    }

    private static final class MooreStep {

        private final GridPoint boundary;
        private final GridPoint backtrack;

        private MooreStep(GridPoint boundary, GridPoint backtrack) {
            this.boundary = boundary;
            this.backtrack = backtrack;
        }
    }
}
