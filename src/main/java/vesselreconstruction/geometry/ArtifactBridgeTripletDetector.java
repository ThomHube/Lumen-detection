package vesselreconstruction.geometry;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Finds ordered artifact bridge triplets along an implicitly circular boundary.
 */
public class ArtifactBridgeTripletDetector {

    private static final double AMBIGUOUS_PEAK_DISTANCE_TOLERANCE = 1.0e-9;

    /**
     * Finds valid artifact bridge triplets among accepted boundary peaks.
     */
    public List<ArtifactBridgeTriplet> findCandidates(
            List<Point> boundaryPixels,
            double[] deviationScores,
            List<Integer> acceptedPeakIndices,
            int minimumFlankArcSeparation,
            int maximumArtifactArcPointCount,
            double maximumClosingLineLength,
            double minimumPeakDistanceFromClosingLine,
            double minimumArcToClosingLineRatio) {

        validateNumericalParameters(
                minimumFlankArcSeparation,
                maximumArtifactArcPointCount,
                maximumClosingLineLength,
                minimumPeakDistanceFromClosingLine,
                minimumArcToClosingLineRatio);

        validateInputs(
                boundaryPixels,
                deviationScores,
                acceptedPeakIndices);

        List<Integer> uniquePeakIndices = uniqueSortedPeakIndices(acceptedPeakIndices);

        if (uniquePeakIndices.size() < 3) {
            return Collections.emptyList();
        }

        int boundarySize = boundaryPixels.size();
        Set<Long> seenTriplets = new HashSet<>();
        List<ArtifactBridgeTriplet> candidates = new ArrayList<>();

        for (int firstPeakPosition = 0;
                firstPeakPosition < uniquePeakIndices.size();
                firstPeakPosition++) {

            for (int secondPeakPosition = 0;
                    secondPeakPosition < uniquePeakIndices.size();
                    secondPeakPosition++) {

                if (firstPeakPosition == secondPeakPosition) {
                    continue;
                }

                int firstBoundaryIndex = uniquePeakIndices.get(firstPeakPosition);
                int secondBoundaryIndex = uniquePeakIndices.get(secondPeakPosition);
                ArtifactBridgeTriplet triplet = buildTripletIfValid(
                        boundaryPixels,
                        deviationScores,
                        uniquePeakIndices,
                        firstBoundaryIndex,
                        secondBoundaryIndex,
                        boundarySize,
                        minimumFlankArcSeparation,
                        maximumArtifactArcPointCount,
                        maximumClosingLineLength,
                        minimumPeakDistanceFromClosingLine,
                        minimumArcToClosingLineRatio);

                if (triplet == null) {
                    continue;
                }

                long tripletKey = encodeTripletKey(
                        triplet.getFirstBridgeBoundaryIndex(),
                        triplet.getBridgePeakBoundaryIndex(),
                        triplet.getSecondBridgeBoundaryIndex());

                if (!seenTriplets.add(tripletKey)) {
                    continue;
                }

                candidates.add(triplet);
            }
        }

        candidates.sort(candidateOrder());

        return Collections.unmodifiableList(candidates);
    }

    private static void validateInputs(
            List<Point> boundaryPixels,
            double[] deviationScores,
            List<Integer> acceptedPeakIndices) {

        if (boundaryPixels == null) {
            throw new IllegalArgumentException("boundaryPixels must not be null.");
        }

        if (boundaryPixels.size() < 3) {
            throw new IllegalArgumentException(
                    "boundaryPixels must contain at least 3 points.");
        }

        Set<Long> boundaryCoordinateKeys = new HashSet<>();

        for (Point boundaryPixel : boundaryPixels) {

            if (boundaryPixel == null) {
                throw new IllegalArgumentException(
                        "boundaryPixels must not contain null points.");
            }

            if (!boundaryCoordinateKeys.add(
                    encodeCoordinate(boundaryPixel.x, boundaryPixel.y))) {
                throw new IllegalArgumentException(
                        "boundaryPixels must not contain duplicate coordinates.");
            }
        }

        if (deviationScores == null) {
            throw new IllegalArgumentException("deviationScores must not be null.");
        }

        if (deviationScores.length != boundaryPixels.size()) {
            throw new IllegalArgumentException(
                    "deviationScores length must match boundaryPixels size.");
        }

        for (double deviationScore : deviationScores) {

            if (!Double.isFinite(deviationScore) || deviationScore < 0.0) {
                throw new IllegalArgumentException(
                        "deviationScores must contain finite non-negative values.");
            }
        }

        if (acceptedPeakIndices == null) {
            throw new IllegalArgumentException(
                    "acceptedPeakIndices must not be null.");
        }

        if (acceptedPeakIndices.size() < 3) {
            throw new IllegalArgumentException(
                    "acceptedPeakIndices must contain at least 3 indices.");
        }

        int maximumBoundaryIndex = boundaryPixels.size() - 1;
        Set<Integer> uniquePeakIndices = new HashSet<>();

        for (Integer peakIndex : acceptedPeakIndices) {

            if (peakIndex == null) {
                throw new IllegalArgumentException(
                        "acceptedPeakIndices must not contain null values.");
            }

            if (peakIndex < 0 || peakIndex > maximumBoundaryIndex) {
                throw new IllegalArgumentException(
                        "acceptedPeakIndices must be within [0, "
                                + maximumBoundaryIndex
                                + "].");
            }

            if (!uniquePeakIndices.add(peakIndex)) {
                throw new IllegalArgumentException(
                        "acceptedPeakIndices must not contain duplicate indices.");
            }
        }
    }

    private static ArtifactBridgeTriplet buildTripletIfValid(
            List<Point> boundaryPixels,
            double[] deviationScores,
            List<Integer> acceptedPeakIndices,
            int firstBoundaryIndex,
            int secondBoundaryIndex,
            int boundarySize,
            int minimumFlankArcSeparation,
            int maximumArtifactArcPointCount,
            double maximumClosingLineLength,
            double minimumPeakDistanceFromClosingLine,
            double minimumArcToClosingLineRatio) {

        int totalArtifactArcPointCount =
                forwardArcPointCount(
                        firstBoundaryIndex,
                        secondBoundaryIndex,
                        boundarySize);

        if (totalArtifactArcPointCount > maximumArtifactArcPointCount) {
            return null;
        }

        Point firstBridgePoint = boundaryPixels.get(firstBoundaryIndex);
        Point secondBridgePoint = boundaryPixels.get(secondBoundaryIndex);
        double closingLineLength = euclideanDistance(
                firstBridgePoint,
                secondBridgePoint);

        if (closingLineLength == 0.0
                || !Double.isFinite(closingLineLength)
                || closingLineLength > maximumClosingLineLength) {
            return null;
        }

        double artifactArcLength = forwardArcPolylineLength(
                boundaryPixels,
                firstBoundaryIndex,
                secondBoundaryIndex,
                boundarySize);
        double arcToClosingLineRatio = artifactArcLength / closingLineLength;

        if (arcToClosingLineRatio < minimumArcToClosingLineRatio) {
            return null;
        }

        Integer bridgePeakBoundaryIndex = selectBridgePeakBoundaryIndex(
                boundaryPixels,
                acceptedPeakIndices,
                firstBoundaryIndex,
                secondBoundaryIndex,
                firstBridgePoint,
                secondBridgePoint,
                boundarySize,
                minimumFlankArcSeparation,
                minimumPeakDistanceFromClosingLine);

        if (bridgePeakBoundaryIndex == null) {
            return null;
        }

        int firstToPeakArcPointCount = forwardArcPointCount(
                firstBoundaryIndex,
                bridgePeakBoundaryIndex,
                boundarySize);
        int peakToSecondArcPointCount = forwardArcPointCount(
                bridgePeakBoundaryIndex,
                secondBoundaryIndex,
                boundarySize);
        Point bridgePeakPoint = boundaryPixels.get(bridgePeakBoundaryIndex);
        double peakDistanceFromClosingLine = distancePointToSegment(
                bridgePeakPoint,
                firstBridgePoint,
                secondBridgePoint);

        return new ArtifactBridgeTriplet(
                firstBoundaryIndex,
                bridgePeakBoundaryIndex,
                secondBoundaryIndex,
                firstBridgePoint,
                bridgePeakPoint,
                secondBridgePoint,
                firstToPeakArcPointCount,
                peakToSecondArcPointCount,
                totalArtifactArcPointCount,
                closingLineLength,
                artifactArcLength,
                arcToClosingLineRatio,
                peakDistanceFromClosingLine,
                deviationScores[firstBoundaryIndex],
                deviationScores[bridgePeakBoundaryIndex],
                deviationScores[secondBoundaryIndex]);
    }

    private static Integer selectBridgePeakBoundaryIndex(
            List<Point> boundaryPixels,
            List<Integer> acceptedPeakIndices,
            int firstBoundaryIndex,
            int secondBoundaryIndex,
            Point firstBridgePoint,
            Point secondBridgePoint,
            int boundarySize,
            int minimumFlankArcSeparation,
            double minimumPeakDistanceFromClosingLine) {

        Integer bestPeakBoundaryIndex = null;
        double bestPeakDistance = Double.NEGATIVE_INFINITY;
        int competingPeakCount = 0;

        for (int peakBoundaryIndex : acceptedPeakIndices) {

            if (peakBoundaryIndex == firstBoundaryIndex
                    || peakBoundaryIndex == secondBoundaryIndex) {
                continue;
            }

            if (!isStrictlyOnForwardArc(
                    firstBoundaryIndex,
                    secondBoundaryIndex,
                    peakBoundaryIndex,
                    boundarySize)) {
                continue;
            }

            int firstToPeakSteps = forwardBoundarySteps(
                    firstBoundaryIndex,
                    peakBoundaryIndex,
                    boundarySize);
            int peakToSecondSteps = forwardBoundarySteps(
                    peakBoundaryIndex,
                    secondBoundaryIndex,
                    boundarySize);

            if (firstToPeakSteps < minimumFlankArcSeparation
                    || peakToSecondSteps < minimumFlankArcSeparation) {
                continue;
            }

            Point peakPoint = boundaryPixels.get(peakBoundaryIndex);
            double peakDistance = distancePointToSegment(
                    peakPoint,
                    firstBridgePoint,
                    secondBridgePoint);

            if (peakDistance > bestPeakDistance + AMBIGUOUS_PEAK_DISTANCE_TOLERANCE) {
                bestPeakDistance = peakDistance;
                bestPeakBoundaryIndex = peakBoundaryIndex;
                competingPeakCount = 1;
            } else if (Math.abs(peakDistance - bestPeakDistance)
                    <= AMBIGUOUS_PEAK_DISTANCE_TOLERANCE) {
                competingPeakCount++;
            }
        }

        if (bestPeakBoundaryIndex == null
                || bestPeakDistance < minimumPeakDistanceFromClosingLine
                || competingPeakCount > 1) {
            return null;
        }

        return bestPeakBoundaryIndex;
    }

    private static void validateNumericalParameters(
            int minimumFlankArcSeparation,
            int maximumArtifactArcPointCount,
            double maximumClosingLineLength,
            double minimumPeakDistanceFromClosingLine,
            double minimumArcToClosingLineRatio) {

        if (minimumFlankArcSeparation < 1) {
            throw new IllegalArgumentException(
                    "minimumFlankArcSeparation must be at least 1.");
        }

        if (maximumArtifactArcPointCount < 3) {
            throw new IllegalArgumentException(
                    "maximumArtifactArcPointCount must be at least 3.");
        }

        if (!Double.isFinite(maximumClosingLineLength)
                || maximumClosingLineLength <= 0.0) {
            throw new IllegalArgumentException(
                    "maximumClosingLineLength must be finite and positive.");
        }

        if (!Double.isFinite(minimumPeakDistanceFromClosingLine)
                || minimumPeakDistanceFromClosingLine < 0.0) {
            throw new IllegalArgumentException(
                    "minimumPeakDistanceFromClosingLine must be finite "
                            + "and non-negative.");
        }

        if (!Double.isFinite(minimumArcToClosingLineRatio)
                || minimumArcToClosingLineRatio < 1.0) {
            throw new IllegalArgumentException(
                    "minimumArcToClosingLineRatio must be finite and at least 1.");
        }
    }

    private static List<Integer> uniqueSortedPeakIndices(List<Integer> peakIndices) {
        return new ArrayList<>(new TreeSet<>(peakIndices));
    }

    private static boolean isStrictlyOnForwardArc(
            int firstBoundaryIndex,
            int secondBoundaryIndex,
            int candidateBoundaryIndex,
            int boundarySize) {

        if (candidateBoundaryIndex == firstBoundaryIndex
                || candidateBoundaryIndex == secondBoundaryIndex) {
            return false;
        }

        int forwardSteps = forwardBoundarySteps(
                firstBoundaryIndex,
                candidateBoundaryIndex,
                boundarySize);
        int arcSteps = forwardBoundarySteps(
                firstBoundaryIndex,
                secondBoundaryIndex,
                boundarySize);

        return forwardSteps > 0 && forwardSteps < arcSteps;
    }

    private static int forwardBoundarySteps(
            int fromBoundaryIndex,
            int toBoundaryIndex,
            int boundarySize) {

        if (toBoundaryIndex >= fromBoundaryIndex) {
            return toBoundaryIndex - fromBoundaryIndex;
        }

        return (boundarySize - fromBoundaryIndex) + toBoundaryIndex;
    }

    private static int forwardArcPointCount(
            int fromBoundaryIndex,
            int toBoundaryIndex,
            int boundarySize) {

        return forwardBoundarySteps(fromBoundaryIndex, toBoundaryIndex, boundarySize) + 1;
    }

    private static double forwardArcPolylineLength(
            List<Point> boundaryPixels,
            int fromBoundaryIndex,
            int toBoundaryIndex,
            int boundarySize) {

        double arcLength = 0.0;
        int currentBoundaryIndex = fromBoundaryIndex;

        while (currentBoundaryIndex != toBoundaryIndex) {
            int nextBoundaryIndex = (currentBoundaryIndex + 1) % boundarySize;
            arcLength += euclideanDistance(
                    boundaryPixels.get(currentBoundaryIndex),
                    boundaryPixels.get(nextBoundaryIndex));
            currentBoundaryIndex = nextBoundaryIndex;
        }

        return arcLength;
    }

    private static double euclideanDistance(Point left, Point right) {

        double deltaX = left.x - right.x;
        double deltaY = left.y - right.y;

        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    private static double distancePointToSegment(
            Point point,
            Point segmentStart,
            Point segmentEnd) {

        double deltaX = segmentEnd.x - segmentStart.x;
        double deltaY = segmentEnd.y - segmentStart.y;
        double segmentLengthSquared = deltaX * deltaX + deltaY * deltaY;

        if (segmentLengthSquared == 0.0) {
            return euclideanDistance(point, segmentStart);
        }

        double projectionNumerator =
                (point.x - segmentStart.x) * deltaX
                        + (point.y - segmentStart.y) * deltaY;
        double projectionParameter = projectionNumerator / segmentLengthSquared;
        projectionParameter = Math.max(0.0, Math.min(1.0, projectionParameter));

        double projectedX = segmentStart.x + projectionParameter * deltaX;
        double projectedY = segmentStart.y + projectionParameter * deltaY;
        double distanceX = point.x - projectedX;
        double distanceY = point.y - projectedY;

        return Math.sqrt(distanceX * distanceX + distanceY * distanceY);
    }

    private static long encodeTripletKey(
            int firstBoundaryIndex,
            int bridgePeakBoundaryIndex,
            int secondBoundaryIndex) {

        return (((long) firstBoundaryIndex) << 42)
                | (((long) bridgePeakBoundaryIndex) << 21)
                | (long) secondBoundaryIndex;
    }

    private static long encodeCoordinate(int x, int y) {
        return (((long) y) << 32) | (x & 0xFFFFFFFFL);
    }

    private static Comparator<ArtifactBridgeTriplet> candidateOrder() {

        return Comparator
                .comparingDouble(ArtifactBridgeTriplet::getPeakDistanceFromClosingLine)
                .reversed()
                .thenComparing(
                        ArtifactBridgeTriplet::getArcToClosingLineRatio,
                        Comparator.reverseOrder())
                .thenComparing(
                        ArtifactBridgeTriplet::getBridgePeakDeviationScore,
                        Comparator.reverseOrder())
                .thenComparingDouble(ArtifactBridgeTriplet::getClosingLineLength)
                .thenComparingInt(ArtifactBridgeTriplet::getFirstBridgeBoundaryIndex)
                .thenComparingInt(ArtifactBridgeTriplet::getBridgePeakBoundaryIndex)
                .thenComparingInt(ArtifactBridgeTriplet::getSecondBridgeBoundaryIndex);
    }
}
