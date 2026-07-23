package vesselreconstruction.geometry;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds artifact arc candidates by searching endpoint pairs on a circular contour
 * and deriving the bridge peak from perpendicular distance to the closing line.
 *
 * <p>T-shape diagnostic measurements are attached to every accepted candidate for
 * inspection only. They must not influence ranking, acceptance, or reconstruction.
 */
public class ArtifactArcCandidateFinder {

    private static final double AMBIGUOUS_PEAK_DISTANCE_TOLERANCE = 1.0e-9;
    private static final double MINIMUM_SECOND_PEAK_DISTANCE = 1.0e-9;

    /** Contour-step offset used only for T-shape attachment/wall/peak turn diagnostics. */
    private static final int ATTACHMENT_TANGENT_OFFSET = 5;

    /**
     * Finds valid artifact arc candidates on the supplied circular boundary.
     */
    public List<ArtifactArcCandidate> findCandidates(
            List<Point> boundaryVertices,
            double[] deviationScores,
            int minimumEndpointArcSeparation,
            int maximumArtifactArcPointCount,
            double minimumClosingLineLength,
            double maximumClosingLineLength,
            double minimumPeakDistanceFromClosingLine,
            double minimumPeakDominanceRatio,
            double minimumArcToClosingLineRatio,
            int minimumPeakFlankArcSeparation,
            double maximumProfileViolation) {

        validateNumericalParameters(
                minimumEndpointArcSeparation,
                maximumArtifactArcPointCount,
                minimumClosingLineLength,
                maximumClosingLineLength,
                minimumPeakDistanceFromClosingLine,
                minimumPeakDominanceRatio,
                minimumArcToClosingLineRatio,
                minimumPeakFlankArcSeparation,
                maximumProfileViolation);

        validateInputs(boundaryVertices, deviationScores);

        int boundarySize = boundaryVertices.size();
        List<ArtifactArcCandidate> candidates = new ArrayList<>();

        for (int firstBoundaryIndex = 0;
                firstBoundaryIndex < boundarySize;
                firstBoundaryIndex++) {

            for (int secondBoundaryIndex = 0;
                    secondBoundaryIndex < boundarySize;
                    secondBoundaryIndex++) {

                if (firstBoundaryIndex == secondBoundaryIndex) {
                    continue;
                }

                ArtifactArcCandidate candidate = buildCandidateIfValid(
                        boundaryVertices,
                        deviationScores,
                        firstBoundaryIndex,
                        secondBoundaryIndex,
                        boundarySize,
                        minimumEndpointArcSeparation,
                        maximumArtifactArcPointCount,
                        minimumClosingLineLength,
                        maximumClosingLineLength,
                        minimumPeakDistanceFromClosingLine,
                        minimumPeakDominanceRatio,
                        minimumArcToClosingLineRatio,
                        minimumPeakFlankArcSeparation,
                        maximumProfileViolation);

                if (candidate != null) {
                    candidates.add(candidate);
                }
            }
        }

        candidates.sort(candidateOrder());

        return Collections.unmodifiableList(candidates);
    }

    private static ArtifactArcCandidate buildCandidateIfValid(
            List<Point> boundaryVertices,
            double[] deviationScores,
            int firstBoundaryIndex,
            int secondBoundaryIndex,
            int boundarySize,
            int minimumEndpointArcSeparation,
            int maximumArtifactArcPointCount,
            double minimumClosingLineLength,
            double maximumClosingLineLength,
            double minimumPeakDistanceFromClosingLine,
            double minimumPeakDominanceRatio,
            double minimumArcToClosingLineRatio,
            int minimumPeakFlankArcSeparation,
            double maximumProfileViolation) {

        int forwardArcPointCount = forwardArcPointCount(
                firstBoundaryIndex,
                secondBoundaryIndex,
                boundarySize);

        if (forwardArcPointCount < minimumEndpointArcSeparation + 2) {
            return null;
        }

        if (forwardArcPointCount > maximumArtifactArcPointCount) {
            return null;
        }

        Point firstPoint = boundaryVertices.get(firstBoundaryIndex);
        Point secondPoint = boundaryVertices.get(secondBoundaryIndex);
        double closingLineLength = euclideanDistance(firstPoint, secondPoint);

        if (!Double.isFinite(closingLineLength)
                || closingLineLength < minimumClosingLineLength
                || closingLineLength > maximumClosingLineLength) {
            return null;
        }

        double artifactArcLength = forwardArcPolylineLength(
                boundaryVertices,
                firstBoundaryIndex,
                secondBoundaryIndex,
                boundarySize);
        double arcToClosingLineRatio = artifactArcLength / closingLineLength;

        if (arcToClosingLineRatio < minimumArcToClosingLineRatio) {
            return null;
        }

        PeakSelection peakSelection = selectDerivedPeak(
                boundaryVertices,
                firstBoundaryIndex,
                secondBoundaryIndex,
                firstPoint,
                secondPoint,
                boundarySize,
                minimumPeakDistanceFromClosingLine,
                minimumPeakDominanceRatio);

        if (peakSelection == null) {
            return null;
        }

        int firstFlankArcPointCount = forwardArcPointCount(
                firstBoundaryIndex,
                peakSelection.peakBoundaryIndex,
                boundarySize);
        int secondFlankArcPointCount = forwardArcPointCount(
                peakSelection.peakBoundaryIndex,
                secondBoundaryIndex,
                boundarySize);

        if (firstFlankArcPointCount - 1 < minimumPeakFlankArcSeparation
                || secondFlankArcPointCount - 1 < minimumPeakFlankArcSeparation) {
            return null;
        }

        ProfileViolations profileViolations = calculateProfileViolations(
                boundaryVertices,
                firstBoundaryIndex,
                secondBoundaryIndex,
                peakSelection.peakBoundaryIndex,
                firstPoint,
                secondPoint,
                boundarySize);

        if (profileViolations.maximumProfileRiseViolation > maximumProfileViolation
                || profileViolations.maximumProfileFallViolation > maximumProfileViolation) {
            return null;
        }

        Point peakPoint = boundaryVertices.get(peakSelection.peakBoundaryIndex);
        TShapeDiagnostics tShapeDiagnostics = buildTShapeDiagnostics(
                boundaryVertices,
                firstBoundaryIndex,
                peakSelection.peakBoundaryIndex,
                secondBoundaryIndex,
                firstPoint,
                peakPoint,
                secondPoint,
                closingLineLength,
                artifactArcLength,
                peakSelection.greatestPeakDistance,
                peakSelection.peakDominanceRatio,
                boundarySize);

        return new ArtifactArcCandidate(
                firstBoundaryIndex,
                peakSelection.peakBoundaryIndex,
                secondBoundaryIndex,
                firstPoint,
                peakPoint,
                secondPoint,
                forwardArcPointCount,
                closingLineLength,
                artifactArcLength,
                arcToClosingLineRatio,
                peakSelection.greatestPeakDistance,
                peakSelection.secondGreatestPeakDistance,
                peakSelection.peakDominanceRatio,
                firstFlankArcPointCount,
                secondFlankArcPointCount,
                deviationScores[firstBoundaryIndex],
                deviationScores[peakSelection.peakBoundaryIndex],
                deviationScores[secondBoundaryIndex],
                profileViolations.maximumProfileRiseViolation,
                profileViolations.maximumProfileFallViolation,
                tShapeDiagnostics.mouthWidth,
                tShapeDiagnostics.protrusionDepth,
                tShapeDiagnostics.depthToMouthRatio,
                tShapeDiagnostics.arcToMouthRatio,
                tShapeDiagnostics.relativeContourSpan,
                tShapeDiagnostics.firstAttachmentTurnDegrees,
                tShapeDiagnostics.secondAttachmentTurnDegrees,
                tShapeDiagnostics.minimumAttachmentTurnDegrees,
                tShapeDiagnostics.meanAttachmentTurnDegrees,
                tShapeDiagnostics.attachmentTurnBalance,
                tShapeDiagnostics.outsideWallDirectionDifferenceDegrees,
                tShapeDiagnostics.outsideWallContinuityScore,
                tShapeDiagnostics.outsideWallLineDeviation,
                tShapeDiagnostics.peakArcFraction,
                tShapeDiagnostics.peakCentrality,
                tShapeDiagnostics.peakTurnDegrees,
                tShapeDiagnostics.tShapeDiagnosticScore);
    }

    /**
     * Builds experimental T-shape diagnostic measurements. These values never
     * affect candidate ordering or acceptance.
     */
    static TShapeDiagnostics buildTShapeDiagnostics(
            List<Point> boundaryVertices,
            int firstBoundaryIndex,
            int peakBoundaryIndex,
            int secondBoundaryIndex,
            Point firstPoint,
            Point peakPoint,
            Point secondPoint,
            double closingLineLength,
            double artifactArcLength,
            double protrusionDepth,
            double peakDominanceRatio,
            int boundarySize) {

        double mouthWidth = closingLineLength;
        double depthToMouthRatio = safeDivide(protrusionDepth, mouthWidth);
        double arcToMouthRatio = safeDivide(artifactArcLength, mouthWidth);
        double totalContourLength = closedContourPolylineLength(boundaryVertices);
        double relativeContourSpan = safeDivide(artifactArcLength, totalContourLength);

        int aOutsideIndex = stepBoundaryIndex(
                firstBoundaryIndex,
                -ATTACHMENT_TANGENT_OFFSET,
                boundarySize);
        int aInsideIndex = stepBoundaryIndex(
                firstBoundaryIndex,
                ATTACHMENT_TANGENT_OFFSET,
                boundarySize);
        int bInsideIndex = stepBoundaryIndex(
                secondBoundaryIndex,
                -ATTACHMENT_TANGENT_OFFSET,
                boundarySize);
        int bOutsideIndex = stepBoundaryIndex(
                secondBoundaryIndex,
                ATTACHMENT_TANGENT_OFFSET,
                boundarySize);

        Point aOutside = boundaryVertices.get(aOutsideIndex);
        Point aInside = boundaryVertices.get(aInsideIndex);
        Point bInside = boundaryVertices.get(bInsideIndex);
        Point bOutside = boundaryVertices.get(bOutsideIndex);

        double firstAttachmentTurnDegrees = smallerAngleDegrees(
                firstPoint.x - aOutside.x,
                firstPoint.y - aOutside.y,
                aInside.x - firstPoint.x,
                aInside.y - firstPoint.y);
        double secondAttachmentTurnDegrees = smallerAngleDegrees(
                secondPoint.x - bInside.x,
                secondPoint.y - bInside.y,
                bOutside.x - secondPoint.x,
                bOutside.y - secondPoint.y);
        double minimumAttachmentTurnDegrees = Math.min(
                firstAttachmentTurnDegrees,
                secondAttachmentTurnDegrees);
        double meanAttachmentTurnDegrees =
                0.5 * (firstAttachmentTurnDegrees + secondAttachmentTurnDegrees);
        double attachmentTurnBalance = safeDivide(
                Math.min(firstAttachmentTurnDegrees, secondAttachmentTurnDegrees),
                Math.max(firstAttachmentTurnDegrees, secondAttachmentTurnDegrees));

        if (firstAttachmentTurnDegrees == 0.0 && secondAttachmentTurnDegrees == 0.0) {
            attachmentTurnBalance = 1.0;
        }

        double wallBeforeAx = firstPoint.x - aOutside.x;
        double wallBeforeAy = firstPoint.y - aOutside.y;
        double wallAfterBx = bOutside.x - secondPoint.x;
        double wallAfterBy = bOutside.y - secondPoint.y;

        double[] orientedWallAfter = orientDirectionConsistently(
                wallBeforeAx,
                wallBeforeAy,
                wallAfterBx,
                wallAfterBy);
        double outsideWallDirectionDifferenceDegrees = smallerAngleDegrees(
                wallBeforeAx,
                wallBeforeAy,
                orientedWallAfter[0],
                orientedWallAfter[1]);
        double outsideWallContinuityScore = clamp01(
                1.0 - outsideWallDirectionDifferenceDegrees / 180.0);

        double averageWallDx = 0.5 * (wallBeforeAx + orientedWallAfter[0]);
        double averageWallDy = 0.5 * (wallBeforeAy + orientedWallAfter[1]);
        double midX = 0.5 * (firstPoint.x + secondPoint.x);
        double midY = 0.5 * (firstPoint.y + secondPoint.y);
        double outsideWallLineDeviation = Math.max(
                distancePointToInfiniteLine(
                        aOutside.x,
                        aOutside.y,
                        midX,
                        midY,
                        averageWallDx,
                        averageWallDy),
                distancePointToInfiniteLine(
                        bOutside.x,
                        bOutside.y,
                        midX,
                        midY,
                        averageWallDx,
                        averageWallDy));

        double peakArcLength = forwardArcPolylineLength(
                boundaryVertices,
                firstBoundaryIndex,
                peakBoundaryIndex,
                boundarySize);
        double peakArcFraction = safeDivide(peakArcLength, artifactArcLength);
        double peakCentrality = clamp01(
                1.0 - 2.0 * Math.abs(peakArcFraction - 0.5));

        int peakBeforeIndex = stepBoundaryIndex(
                peakBoundaryIndex,
                -ATTACHMENT_TANGENT_OFFSET,
                boundarySize);
        int peakAfterIndex = stepBoundaryIndex(
                peakBoundaryIndex,
                ATTACHMENT_TANGENT_OFFSET,
                boundarySize);
        Point peakBefore = boundaryVertices.get(peakBeforeIndex);
        Point peakAfter = boundaryVertices.get(peakAfterIndex);
        double peakTurnDegrees = smallerAngleDegrees(
                peakPoint.x - peakBefore.x,
                peakPoint.y - peakBefore.y,
                peakAfter.x - peakPoint.x,
                peakAfter.y - peakPoint.y);

        double tShapeDiagnosticScore = computeTShapeDiagnosticScore(
                depthToMouthRatio,
                minimumAttachmentTurnDegrees,
                outsideWallContinuityScore,
                peakDominanceRatio,
                relativeContourSpan);

        return new TShapeDiagnostics(
                mouthWidth,
                protrusionDepth,
                depthToMouthRatio,
                arcToMouthRatio,
                relativeContourSpan,
                firstAttachmentTurnDegrees,
                secondAttachmentTurnDegrees,
                minimumAttachmentTurnDegrees,
                meanAttachmentTurnDegrees,
                attachmentTurnBalance,
                outsideWallDirectionDifferenceDegrees,
                outsideWallContinuityScore,
                outsideWallLineDeviation,
                peakArcFraction,
                peakCentrality,
                peakTurnDegrees,
                tShapeDiagnosticScore);
    }

    /**
     * Experimental weighted T-shape score. Diagnostic only; never used for
     * ranking or selection.
     */
    static double computeTShapeDiagnosticScore(
            double depthToMouthRatio,
            double minimumAttachmentTurnDegrees,
            double outsideWallContinuityScore,
            double peakDominanceRatio,
            double relativeContourSpan) {

        double depthComponent = clamp01(depthToMouthRatio / 3.0);
        double attachmentComponent = clamp01(minimumAttachmentTurnDegrees / 90.0);
        double wallComponent = clamp01(outsideWallContinuityScore);
        double peakComponent = clamp01(peakDominanceRatio / 1.5);
        double localityComponent = clamp01(1.0 - relativeContourSpan / 0.30);

        return 0.35 * depthComponent
                + 0.25 * attachmentComponent
                + 0.15 * wallComponent
                + 0.15 * peakComponent
                + 0.10 * localityComponent;
    }

    static double safeDivide(double numerator, double denominator) {

        if (!Double.isFinite(numerator)
                || !Double.isFinite(denominator)
                || denominator == 0.0) {
            return 0.0;
        }

        double ratio = numerator / denominator;
        return Double.isFinite(ratio) ? ratio : 0.0;
    }

    static double clamp01(double value) {

        if (!Double.isFinite(value)) {
            return 0.0;
        }

        return Math.max(0.0, Math.min(1.0, value));
    }

    static double smallerAngleDegrees(
            double firstDeltaX,
            double firstDeltaY,
            double secondDeltaX,
            double secondDeltaY) {

        double firstLength = Math.hypot(firstDeltaX, firstDeltaY);
        double secondLength = Math.hypot(secondDeltaX, secondDeltaY);

        if (firstLength == 0.0 || secondLength == 0.0) {
            return 0.0;
        }

        double cosine =
                (firstDeltaX * secondDeltaX + firstDeltaY * secondDeltaY)
                        / (firstLength * secondLength);
        cosine = Math.max(-1.0, Math.min(1.0, cosine));
        return Math.toDegrees(Math.acos(cosine));
    }

    static double[] orientDirectionConsistently(
            double referenceDeltaX,
            double referenceDeltaY,
            double candidateDeltaX,
            double candidateDeltaY) {

        double referenceLength = Math.hypot(referenceDeltaX, referenceDeltaY);
        double candidateLength = Math.hypot(candidateDeltaX, candidateDeltaY);

        if (referenceLength == 0.0 || candidateLength == 0.0) {
            return new double[] {candidateDeltaX, candidateDeltaY};
        }

        double dot =
                referenceDeltaX * candidateDeltaX
                        + referenceDeltaY * candidateDeltaY;

        if (dot < 0.0) {
            return new double[] {-candidateDeltaX, -candidateDeltaY};
        }

        return new double[] {candidateDeltaX, candidateDeltaY};
    }

    static double distancePointToInfiniteLine(
            double pointX,
            double pointY,
            double linePointX,
            double linePointY,
            double directionX,
            double directionY) {

        double directionLength = Math.hypot(directionX, directionY);

        if (directionLength == 0.0) {
            double deltaX = pointX - linePointX;
            double deltaY = pointY - linePointY;
            return Math.hypot(deltaX, deltaY);
        }

        double unitX = directionX / directionLength;
        double unitY = directionY / directionLength;
        double relativeX = pointX - linePointX;
        double relativeY = pointY - linePointY;
        return Math.abs(relativeX * unitY - relativeY * unitX);
    }

    private static int stepBoundaryIndex(
            int currentBoundaryIndex,
            int stepCount,
            int boundarySize) {

        int wrapped = (currentBoundaryIndex + stepCount) % boundarySize;

        if (wrapped < 0) {
            wrapped += boundarySize;
        }

        return wrapped;
    }

    private static double closedContourPolylineLength(List<Point> boundaryVertices) {

        double contourLength = 0.0;
        int boundarySize = boundaryVertices.size();

        for (int boundaryIndex = 0; boundaryIndex < boundarySize; boundaryIndex++) {
            Point currentPoint = boundaryVertices.get(boundaryIndex);
            Point nextPoint = boundaryVertices.get((boundaryIndex + 1) % boundarySize);
            contourLength += euclideanDistance(currentPoint, nextPoint);
        }

        return contourLength;
    }

    private static PeakSelection selectDerivedPeak(
            List<Point> boundaryVertices,
            int firstBoundaryIndex,
            int secondBoundaryIndex,
            Point firstPoint,
            Point secondPoint,
            int boundarySize,
            double minimumPeakDistanceFromClosingLine,
            double minimumPeakDominanceRatio) {

        double greatestPeakDistance = Double.NEGATIVE_INFINITY;
        double secondGreatestPeakDistance = Double.NEGATIVE_INFINITY;
        Integer greatestPeakBoundaryIndex = null;
        int competingPeakCount = 0;

        int currentBoundaryIndex = forwardBoundaryIndex(
                firstBoundaryIndex,
                boundarySize);

        while (currentBoundaryIndex != secondBoundaryIndex) {

            Point interiorPoint = boundaryVertices.get(currentBoundaryIndex);
            double peakDistance = distancePointToSegment(
                    interiorPoint,
                    firstPoint,
                    secondPoint);

            if (peakDistance > greatestPeakDistance + AMBIGUOUS_PEAK_DISTANCE_TOLERANCE) {
                secondGreatestPeakDistance = greatestPeakDistance;
                greatestPeakDistance = peakDistance;
                greatestPeakBoundaryIndex = currentBoundaryIndex;
                competingPeakCount = 1;
            } else if (Math.abs(peakDistance - greatestPeakDistance)
                    <= AMBIGUOUS_PEAK_DISTANCE_TOLERANCE) {
                competingPeakCount++;
            } else if (peakDistance > secondGreatestPeakDistance) {
                secondGreatestPeakDistance = peakDistance;
            }

            currentBoundaryIndex = forwardBoundaryIndex(
                    currentBoundaryIndex,
                    boundarySize);
        }

        if (greatestPeakBoundaryIndex == null
                || greatestPeakDistance < minimumPeakDistanceFromClosingLine
                || competingPeakCount > 1) {
            return null;
        }

        double peakDominanceRatio =
                greatestPeakDistance
                        / Math.max(secondGreatestPeakDistance, MINIMUM_SECOND_PEAK_DISTANCE);

        if (peakDominanceRatio < minimumPeakDominanceRatio) {
            return null;
        }

        return new PeakSelection(
                greatestPeakBoundaryIndex,
                greatestPeakDistance,
                Math.max(secondGreatestPeakDistance, 0.0),
                peakDominanceRatio);
    }

    private static ProfileViolations calculateProfileViolations(
            List<Point> boundaryVertices,
            int firstBoundaryIndex,
            int secondBoundaryIndex,
            int peakBoundaryIndex,
            Point firstPoint,
            Point secondPoint,
            int boundarySize) {

        double maximumProfileRiseViolation = 0.0;
        double maximumProfileFallViolation = 0.0;
        double previousDistance = 0.0;
        int currentBoundaryIndex = firstBoundaryIndex;
        int firstToPeakSteps = forwardBoundarySteps(
                firstBoundaryIndex,
                peakBoundaryIndex,
                boundarySize);
        int firstToSecondSteps = forwardBoundarySteps(
                firstBoundaryIndex,
                secondBoundaryIndex,
                boundarySize);

        while (true) {
            Point currentPoint = boundaryVertices.get(currentBoundaryIndex);
            double currentDistance = distancePointToSegment(
                    currentPoint,
                    firstPoint,
                    secondPoint);
            int firstToCurrentSteps = forwardBoundarySteps(
                    firstBoundaryIndex,
                    currentBoundaryIndex,
                    boundarySize);

            if (currentBoundaryIndex != firstBoundaryIndex) {

                if (firstToCurrentSteps > 0
                        && firstToCurrentSteps < firstToPeakSteps) {
                    maximumProfileRiseViolation = Math.max(
                            maximumProfileRiseViolation,
                            Math.max(0.0, previousDistance - currentDistance));
                }

                if (firstToCurrentSteps > firstToPeakSteps
                        && firstToCurrentSteps < firstToSecondSteps) {
                    maximumProfileFallViolation = Math.max(
                            maximumProfileFallViolation,
                            Math.max(0.0, currentDistance - previousDistance));
                }
            }

            if (currentBoundaryIndex == secondBoundaryIndex) {
                break;
            }

            previousDistance = currentDistance;
            currentBoundaryIndex = forwardBoundaryIndex(
                    currentBoundaryIndex,
                    boundarySize);
        }

        return new ProfileViolations(
                maximumProfileRiseViolation,
                maximumProfileFallViolation);
    }

    private static void validateInputs(
            List<Point> boundaryVertices,
            double[] deviationScores) {

        if (boundaryVertices == null) {
            throw new IllegalArgumentException("NULL_BOUNDARY");
        }

        if (boundaryVertices.size() < 3) {
            throw new IllegalArgumentException(
                    "BOUNDARY_TOO_SHORT: size=" + boundaryVertices.size());
        }

        for (int boundaryIndex = 0;
                boundaryIndex < boundaryVertices.size();
                boundaryIndex++) {

            if (boundaryVertices.get(boundaryIndex) == null) {
                throw new IllegalArgumentException(
                        "NULL_BOUNDARY_POINT: index=" + boundaryIndex);
            }
        }

        Point firstVertex = boundaryVertices.get(0);
        Point lastVertex = boundaryVertices.get(boundaryVertices.size() - 1);

        if (firstVertex.x == lastVertex.x && firstVertex.y == lastVertex.y) {
            throw new IllegalArgumentException(
                    "REPEATED_CLOSING_VERTEX: firstIndex=0, lastIndex="
                            + (boundaryVertices.size() - 1)
                            + ", x="
                            + firstVertex.x
                            + ", y="
                            + firstVertex.y);
        }

        java.util.Map<Long, Integer> firstIndexByCoordinate = new java.util.HashMap<>();

        for (int boundaryIndex = 0;
                boundaryIndex < boundaryVertices.size();
                boundaryIndex++) {

            Point boundaryVertex = boundaryVertices.get(boundaryIndex);
            long coordinateKey = encodeCoordinate(
                    boundaryVertex.x,
                    boundaryVertex.y);

            if (firstIndexByCoordinate.containsKey(coordinateKey)) {
                throw new IllegalArgumentException(
                        "DUPLICATE_BOUNDARY_VERTEX: index="
                                + boundaryIndex
                                + ", firstIndex="
                                + firstIndexByCoordinate.get(coordinateKey)
                                + ", x="
                                + boundaryVertex.x
                                + ", y="
                                + boundaryVertex.y);
            }

            firstIndexByCoordinate.put(coordinateKey, boundaryIndex);
        }

        if (deviationScores == null) {
            throw new IllegalArgumentException("NULL_DEVIATION_SCORE");
        }

        if (deviationScores.length != boundaryVertices.size()) {
            throw new IllegalArgumentException(
                    "DEVIATION_LENGTH_MISMATCH: boundarySize="
                            + boundaryVertices.size()
                            + ", deviationCount="
                            + deviationScores.length);
        }

        for (int scoreIndex = 0; scoreIndex < deviationScores.length; scoreIndex++) {
            double deviationScore = deviationScores[scoreIndex];

            if (!Double.isFinite(deviationScore)) {
                throw new IllegalArgumentException(
                        "NON_FINITE_DEVIATION_SCORE: index=" + scoreIndex);
            }

            if (deviationScore < 0.0) {
                throw new IllegalArgumentException(
                        "NEGATIVE_DEVIATION_SCORE: index="
                                + scoreIndex
                                + ", value="
                                + deviationScore);
            }
        }
    }

    private static void validateNumericalParameters(
            int minimumEndpointArcSeparation,
            int maximumArtifactArcPointCount,
            double minimumClosingLineLength,
            double maximumClosingLineLength,
            double minimumPeakDistanceFromClosingLine,
            double minimumPeakDominanceRatio,
            double minimumArcToClosingLineRatio,
            int minimumPeakFlankArcSeparation,
            double maximumProfileViolation) {

        if (minimumEndpointArcSeparation < 1) {
            throw new IllegalArgumentException(
                    "INVALID_NUMERICAL_PARAMETER: minimumEndpointArcSeparation "
                            + "must be at least 1.");
        }

        if (maximumArtifactArcPointCount < 3) {
            throw new IllegalArgumentException(
                    "INVALID_NUMERICAL_PARAMETER: maximumArtifactArcPointCount "
                            + "must be at least 3.");
        }

        if (!Double.isFinite(minimumClosingLineLength)
                || minimumClosingLineLength < 0.0) {
            throw new IllegalArgumentException(
                    "INVALID_NUMERICAL_PARAMETER: minimumClosingLineLength "
                            + "must be finite and non-negative.");
        }

        if (!Double.isFinite(maximumClosingLineLength)
                || maximumClosingLineLength <= minimumClosingLineLength) {
            throw new IllegalArgumentException(
                    "INVALID_NUMERICAL_PARAMETER: maximumClosingLineLength "
                            + "must be finite and greater than "
                            + "minimumClosingLineLength.");
        }

        if (!Double.isFinite(minimumPeakDistanceFromClosingLine)
                || minimumPeakDistanceFromClosingLine < 0.0) {
            throw new IllegalArgumentException(
                    "INVALID_NUMERICAL_PARAMETER: minimumPeakDistanceFromClosingLine "
                            + "must be finite and non-negative.");
        }

        if (!Double.isFinite(minimumPeakDominanceRatio)
                || minimumPeakDominanceRatio < 1.0) {
            throw new IllegalArgumentException(
                    "INVALID_NUMERICAL_PARAMETER: minimumPeakDominanceRatio "
                            + "must be finite and at least 1.");
        }

        if (!Double.isFinite(minimumArcToClosingLineRatio)
                || minimumArcToClosingLineRatio < 1.0) {
            throw new IllegalArgumentException(
                    "INVALID_NUMERICAL_PARAMETER: minimumArcToClosingLineRatio "
                            + "must be finite and at least 1.");
        }

        if (minimumPeakFlankArcSeparation < 1) {
            throw new IllegalArgumentException(
                    "INVALID_NUMERICAL_PARAMETER: minimumPeakFlankArcSeparation "
                            + "must be at least 1.");
        }

        if (!Double.isFinite(maximumProfileViolation)
                || maximumProfileViolation < 0.0) {
            throw new IllegalArgumentException(
                    "INVALID_NUMERICAL_PARAMETER: maximumProfileViolation "
                            + "must be finite and non-negative.");
        }
    }

    private static int forwardBoundaryIndex(int currentBoundaryIndex, int boundarySize) {
        return (currentBoundaryIndex + 1) % boundarySize;
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
            List<Point> boundaryVertices,
            int fromBoundaryIndex,
            int toBoundaryIndex,
            int boundarySize) {

        double arcLength = 0.0;
        int currentBoundaryIndex = fromBoundaryIndex;

        while (currentBoundaryIndex != toBoundaryIndex) {
            int nextBoundaryIndex = forwardBoundaryIndex(
                    currentBoundaryIndex,
                    boundarySize);
            arcLength += euclideanDistance(
                    boundaryVertices.get(currentBoundaryIndex),
                    boundaryVertices.get(nextBoundaryIndex));
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

    private static long encodeCoordinate(int x, int y) {
        return (((long) y) << 32) | (x & 0xFFFFFFFFL);
    }

    private static Comparator<ArtifactArcCandidate> candidateOrder() {

        return Comparator
                .comparingDouble(ArtifactArcCandidate::getPeakDominanceRatio)
                .reversed()
                .thenComparing(
                        ArtifactArcCandidate::getPeakDistanceFromClosingLine,
                        Comparator.reverseOrder())
                .thenComparingDouble(candidate ->
                        candidate.getMaximumProfileRiseViolation()
                                + candidate.getMaximumProfileFallViolation())
                .thenComparing(
                        ArtifactArcCandidate::getArcToClosingLineRatio,
                        Comparator.reverseOrder())
                .thenComparing(
                        (ArtifactArcCandidate candidate) ->
                                candidate.getFirstEndpointDeviationScore()
                                        + candidate.getSecondEndpointDeviationScore(),
                        Comparator.reverseOrder())
                .thenComparingDouble(ArtifactArcCandidate::getClosingLineLength)
                .thenComparingInt(ArtifactArcCandidate::getFirstBoundaryIndex)
                .thenComparingInt(ArtifactArcCandidate::getPeakBoundaryIndex)
                .thenComparingInt(ArtifactArcCandidate::getSecondBoundaryIndex);
    }

    private static final class PeakSelection {

        private final int peakBoundaryIndex;
        private final double greatestPeakDistance;
        private final double secondGreatestPeakDistance;
        private final double peakDominanceRatio;

        private PeakSelection(
                int peakBoundaryIndex,
                double greatestPeakDistance,
                double secondGreatestPeakDistance,
                double peakDominanceRatio) {
            this.peakBoundaryIndex = peakBoundaryIndex;
            this.greatestPeakDistance = greatestPeakDistance;
            this.secondGreatestPeakDistance = secondGreatestPeakDistance;
            this.peakDominanceRatio = peakDominanceRatio;
        }
    }

    private static final class ProfileViolations {

        private final double maximumProfileRiseViolation;
        private final double maximumProfileFallViolation;

        private ProfileViolations(
                double maximumProfileRiseViolation,
                double maximumProfileFallViolation) {
            this.maximumProfileRiseViolation = maximumProfileRiseViolation;
            this.maximumProfileFallViolation = maximumProfileFallViolation;
        }
    }

    /**
     * Immutable experimental T-shape diagnostic bundle. Not used for ranking.
     */
    static final class TShapeDiagnostics {

        private final double mouthWidth;
        private final double protrusionDepth;
        private final double depthToMouthRatio;
        private final double arcToMouthRatio;
        private final double relativeContourSpan;
        private final double firstAttachmentTurnDegrees;
        private final double secondAttachmentTurnDegrees;
        private final double minimumAttachmentTurnDegrees;
        private final double meanAttachmentTurnDegrees;
        private final double attachmentTurnBalance;
        private final double outsideWallDirectionDifferenceDegrees;
        private final double outsideWallContinuityScore;
        private final double outsideWallLineDeviation;
        private final double peakArcFraction;
        private final double peakCentrality;
        private final double peakTurnDegrees;
        private final double tShapeDiagnosticScore;

        private TShapeDiagnostics(
                double mouthWidth,
                double protrusionDepth,
                double depthToMouthRatio,
                double arcToMouthRatio,
                double relativeContourSpan,
                double firstAttachmentTurnDegrees,
                double secondAttachmentTurnDegrees,
                double minimumAttachmentTurnDegrees,
                double meanAttachmentTurnDegrees,
                double attachmentTurnBalance,
                double outsideWallDirectionDifferenceDegrees,
                double outsideWallContinuityScore,
                double outsideWallLineDeviation,
                double peakArcFraction,
                double peakCentrality,
                double peakTurnDegrees,
                double tShapeDiagnosticScore) {

            this.mouthWidth = mouthWidth;
            this.protrusionDepth = protrusionDepth;
            this.depthToMouthRatio = depthToMouthRatio;
            this.arcToMouthRatio = arcToMouthRatio;
            this.relativeContourSpan = relativeContourSpan;
            this.firstAttachmentTurnDegrees = firstAttachmentTurnDegrees;
            this.secondAttachmentTurnDegrees = secondAttachmentTurnDegrees;
            this.minimumAttachmentTurnDegrees = minimumAttachmentTurnDegrees;
            this.meanAttachmentTurnDegrees = meanAttachmentTurnDegrees;
            this.attachmentTurnBalance = attachmentTurnBalance;
            this.outsideWallDirectionDifferenceDegrees =
                    outsideWallDirectionDifferenceDegrees;
            this.outsideWallContinuityScore = outsideWallContinuityScore;
            this.outsideWallLineDeviation = outsideWallLineDeviation;
            this.peakArcFraction = peakArcFraction;
            this.peakCentrality = peakCentrality;
            this.peakTurnDegrees = peakTurnDegrees;
            this.tShapeDiagnosticScore = tShapeDiagnosticScore;
        }
    }
}
