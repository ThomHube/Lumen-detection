package vesselreconstruction.geometry;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Finds bridge-candidate pairs among selected boundary peak indices.
 */
public class BridgeCandidateFinder {

    /**
     * Finds bridge candidates among peak boundary indices.
     *
     * @param boundaryPoints ordered, implicitly closed contour points
     * @param peakIndices selected peak boundary indices
     * @param minimumCircularBoundarySeparation minimum inclusive circular
     *                                          contour separation
     * @param maximumBridgeLength maximum inclusive Euclidean bridge length
     * @return unmodifiable bridge candidates sorted deterministically
     * @throws IllegalArgumentException if the input is invalid
     */
    public List<BridgeCandidate> findCandidates(
            List<Point> boundaryPoints,
            List<Integer> peakIndices,
            int minimumCircularBoundarySeparation,
            double maximumBridgeLength) {

        validateInputs(
                boundaryPoints,
                peakIndices,
                minimumCircularBoundarySeparation,
                maximumBridgeLength);

        if (peakIndices.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> uniquePeakIndices = uniqueSortedPeakIndices(peakIndices);

        if (uniquePeakIndices.size() < 2) {
            return Collections.emptyList();
        }

        int boundarySize = boundaryPoints.size();
        List<BridgeCandidate> candidates = new ArrayList<>();

        for (int leftPeakPosition = 0;
                leftPeakPosition < uniquePeakIndices.size() - 1;
                leftPeakPosition++) {

            for (int rightPeakPosition = leftPeakPosition + 1;
                    rightPeakPosition < uniquePeakIndices.size();
                    rightPeakPosition++) {

                int leftBoundaryIndex = uniquePeakIndices.get(leftPeakPosition);
                int rightBoundaryIndex = uniquePeakIndices.get(rightPeakPosition);
                int circularBoundarySeparation = circularBoundarySeparation(
                        leftBoundaryIndex,
                        rightBoundaryIndex,
                        boundarySize);

                if (circularBoundarySeparation
                        < minimumCircularBoundarySeparation) {
                    continue;
                }

                Point leftPoint = boundaryPoints.get(leftBoundaryIndex);
                Point rightPoint = boundaryPoints.get(rightBoundaryIndex);
                double euclideanDistance = euclideanDistance(leftPoint, rightPoint);

                if (euclideanDistance > maximumBridgeLength) {
                    continue;
                }

                candidates.add(BridgeCandidate.createCanonical(
                        leftBoundaryIndex,
                        rightBoundaryIndex,
                        leftPoint,
                        rightPoint,
                        euclideanDistance,
                        circularBoundarySeparation));
            }
        }

        candidates.sort(candidateOrder());

        return Collections.unmodifiableList(candidates);
    }

    private static void validateInputs(
            List<Point> boundaryPoints,
            List<Integer> peakIndices,
            int minimumCircularBoundarySeparation,
            double maximumBridgeLength) {

        if (boundaryPoints == null) {
            throw new IllegalArgumentException("boundaryPoints must not be null.");
        }

        if (boundaryPoints.size() < 3) {
            throw new IllegalArgumentException(
                    "boundaryPoints must contain at least 3 points.");
        }

        for (Point boundaryPoint : boundaryPoints) {

            if (boundaryPoint == null) {
                throw new IllegalArgumentException(
                        "boundaryPoints must not contain null points.");
            }
        }

        if (peakIndices == null) {
            throw new IllegalArgumentException("peakIndices must not be null.");
        }

        int maximumBoundaryIndex = boundaryPoints.size() - 1;

        for (Integer peakIndex : peakIndices) {

            if (peakIndex == null) {
                throw new IllegalArgumentException(
                        "peakIndices must not contain null values.");
            }

            if (peakIndex < 0 || peakIndex > maximumBoundaryIndex) {
                throw new IllegalArgumentException(
                        "peakIndices must be within [0, "
                                + maximumBoundaryIndex
                                + "].");
            }
        }

        int maximumMinimumCircularSeparation = boundaryPoints.size() / 2;

        if (minimumCircularBoundarySeparation < 1) {
            throw new IllegalArgumentException(
                    "minimumCircularBoundarySeparation must be at least 1.");
        }

        if (minimumCircularBoundarySeparation > maximumMinimumCircularSeparation) {
            throw new IllegalArgumentException(
                    "minimumCircularBoundarySeparation must be at most "
                            + maximumMinimumCircularSeparation
                            + ".");
        }

        if (!Double.isFinite(maximumBridgeLength)) {
            throw new IllegalArgumentException(
                    "maximumBridgeLength must be finite.");
        }

        if (maximumBridgeLength < 0.0) {
            throw new IllegalArgumentException(
                    "maximumBridgeLength must be non-negative.");
        }
    }

    private static List<Integer> uniqueSortedPeakIndices(List<Integer> peakIndices) {

        Set<Integer> uniquePeakIndices = new TreeSet<>(peakIndices);

        return new ArrayList<>(uniquePeakIndices);
    }

    private static int circularBoundarySeparation(
            int leftBoundaryIndex,
            int rightBoundaryIndex,
            int boundarySize) {

        int directSeparation = Math.abs(leftBoundaryIndex - rightBoundaryIndex);

        return Math.min(directSeparation, boundarySize - directSeparation);
    }

    private static double euclideanDistance(Point left, Point right) {

        double deltaX = left.x - right.x;
        double deltaY = left.y - right.y;

        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    private static Comparator<BridgeCandidate> candidateOrder() {

        return Comparator
                .comparingDouble(BridgeCandidate::getEuclideanDistance)
                .thenComparing(
                        BridgeCandidate::getCircularBoundarySeparation,
                        Comparator.reverseOrder())
                .thenComparingInt(BridgeCandidate::getFirstBoundaryIndex)
                .thenComparingInt(BridgeCandidate::getSecondBoundaryIndex);
    }
}
