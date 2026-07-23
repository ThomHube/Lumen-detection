package vesselreconstruction.geometry;

import org.junit.Test;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class ArtifactBridgeTripletDetectorTest {

    private static final double TOLERANCE = 1.0e-9;

    private final ArtifactBridgeTripletDetector detector =
            new ArtifactBridgeTripletDetector();

    @Test
    public void clearStartPeakEndTripletIsDetected() {

        List<Point> boundary = artifactBoundary();
        List<Integer> peaks = Arrays.asList(0, 4, 8);
        double[] scores = peakScores(boundary.size(), peaks, 10.0);

        List<ArtifactBridgeTriplet> candidates = detector.findCandidates(
                boundary,
                scores,
                peaks,
                2,
                20,
                20.0,
                1.0,
                1.05);

        assertFalse(candidates.isEmpty());
        ArtifactBridgeTriplet triplet = candidates.get(0);
        assertEquals(0, triplet.getFirstBridgeBoundaryIndex());
        assertEquals(4, triplet.getBridgePeakBoundaryIndex());
        assertEquals(8, triplet.getSecondBridgeBoundaryIndex());
    }

    @Test
    public void peakLiesBetweenEndpointsOnForwardCircularArc() {

        List<Point> boundary = artifactBoundary();
        List<Integer> peaks = Arrays.asList(0, 4, 8);
        double[] scores = peakScores(boundary.size(), peaks, 10.0);

        List<ArtifactBridgeTriplet> candidates = detector.findCandidates(
                boundary,
                scores,
                peaks,
                2,
                20,
                20.0,
                1.0,
                1.05);

        ArtifactBridgeTriplet triplet = candidates.get(0);
        assertTrue(isStrictlyOnForwardArc(
                triplet.getFirstBridgeBoundaryIndex(),
                triplet.getSecondBridgeBoundaryIndex(),
                triplet.getBridgePeakBoundaryIndex(),
                boundary.size()));
    }

    @Test
    public void wrapAroundCircularTripletIsDetected() {

        List<Point> boundary = squareBoundary();
        List<Integer> peaks = Arrays.asList(1, 4, 7);
        double[] scores = peakScores(boundary.size(), peaks, 8.0);

        List<ArtifactBridgeTriplet> candidates = detector.findCandidates(
                boundary,
                scores,
                peaks,
                1,
                10,
                10.0,
                0.5,
                1.01);

        assertFalse(candidates.isEmpty());
        boolean foundWrapTriplet = false;

        for (ArtifactBridgeTriplet triplet : candidates) {

            if (triplet.getFirstBridgeBoundaryIndex() == 7
                    && triplet.getSecondBridgeBoundaryIndex() == 4
                    && triplet.getBridgePeakBoundaryIndex() == 1) {
                foundWrapTriplet = true;
                break;
            }
        }

        assertTrue(foundWrapTriplet);
    }

    @Test
    public void peakWithGreatestSegmentDistanceIsSelected() {

        List<Point> boundary = asymmetricBulgeBoundary();
        List<Integer> peaks = Arrays.asList(0, 3, 6);
        double[] scores = peakScores(boundary.size(), peaks, 12.0);

        List<ArtifactBridgeTriplet> candidates = detector.findCandidates(
                boundary,
                scores,
                peaks,
                1,
                20,
                20.0,
                1.0,
                1.01);

        ArtifactBridgeTriplet selected = findTriplet(candidates, 0, 3, 6);
        assertEquals(3, selected.getBridgePeakBoundaryIndex());
        assertTrue(
                selected.getPeakDistanceFromClosingLine()
                        > distancePointToSegment(
                                boundary.get(1),
                                boundary.get(0),
                                boundary.get(6)));
    }

    @Test
    public void distanceIsMeasuredToFiniteSegment() {

        List<Point> boundary = finiteSegmentBoundary();
        List<Integer> peaks = Arrays.asList(0, 2, 4);
        double[] scores = peakScores(boundary.size(), peaks, 5.0);
        Point peakPoint = boundary.get(2);
        Point firstPoint = boundary.get(0);
        Point secondPoint = boundary.get(4);
        double finiteDistance = distancePointToSegment(
                peakPoint,
                firstPoint,
                secondPoint);
        double infiniteLineDistance = distancePointToInfiniteLine(
                peakPoint,
                firstPoint,
                secondPoint);

        List<ArtifactBridgeTriplet> candidates = detector.findCandidates(
                boundary,
                scores,
                peaks,
                1,
                10,
                20.0,
                finiteDistance - 0.1,
                1.01);

        ArtifactBridgeTriplet triplet = findTriplet(candidates, 0, 2, 4);
        assertEquals(finiteDistance, triplet.getPeakDistanceFromClosingLine(), TOLERANCE);
        assertTrue(infiniteLineDistance < finiteDistance);
    }

    @Test
    public void insufficientFlankSeparationIsRejected() {

        List<Point> boundary = artifactBoundary();
        List<Integer> peaks = Arrays.asList(0, 4, 8);
        double[] scores = peakScores(boundary.size(), peaks, 10.0);

        List<ArtifactBridgeTriplet> candidates = detector.findCandidates(
                boundary,
                scores,
                peaks,
                5,
                20,
                20.0,
                1.0,
                1.05);

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void excessiveArtifactArcLengthIsRejected() {

        List<Point> boundary = artifactBoundary();
        List<Integer> peaks = Arrays.asList(0, 4, 8);
        double[] scores = peakScores(boundary.size(), peaks, 10.0);

        List<ArtifactBridgeTriplet> candidates = detector.findCandidates(
                boundary,
                scores,
                peaks,
                2,
                5,
                20.0,
                1.0,
                1.05);

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void excessiveClosingLineLengthIsRejected() {

        List<Point> boundary = artifactBoundary();
        List<Integer> peaks = Arrays.asList(0, 4, 8);
        double[] scores = peakScores(boundary.size(), peaks, 10.0);

        List<ArtifactBridgeTriplet> candidates = detector.findCandidates(
                boundary,
                scores,
                peaks,
                2,
                20,
                0.9,
                0.1,
                1.01);

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void insufficientPeakDistanceIsRejected() {

        List<Point> boundary = flatBoundary();
        List<Integer> peaks = Arrays.asList(0, 2, 4);
        double[] scores = peakScores(boundary.size(), peaks, 10.0);

        List<ArtifactBridgeTriplet> candidates = detector.findCandidates(
                boundary,
                scores,
                peaks,
                1,
                10,
                10.0,
                5.0,
                1.01);

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void insufficientArcToClosingLineRatioIsRejected() {

        List<Point> boundary = flatBoundary();
        List<Integer> peaks = Arrays.asList(0, 2, 4);
        double[] scores = peakScores(boundary.size(), peaks, 10.0);

        List<ArtifactBridgeTriplet> candidates = detector.findCandidates(
                boundary,
                scores,
                peaks,
                1,
                10,
                10.0,
                0.0,
                10.0);

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void noInteriorAcceptedPeakReturnsNoCandidate() {

        List<Point> boundary = flatBoundary();
        List<Integer> peaks = Arrays.asList(0, 4);
        double[] scores = peakScores(boundary.size(), peaks, 10.0);

        expectIllegalArgument(() -> detector.findCandidates(
                boundary,
                scores,
                peaks,
                1,
                10,
                10.0,
                0.0,
                1.01));
    }

    @Test
    public void equalCompetingApexDistancesRejectEndpointPair() {

        List<Point> boundary = symmetricBulgeBoundary();
        List<Integer> peaks = Arrays.asList(0, 2, 4, 6);
        double[] scores = peakScores(boundary.size(), peaks, 10.0);

        List<ArtifactBridgeTriplet> candidates = detector.findCandidates(
                boundary,
                scores,
                peaks,
                1,
                10,
                20.0,
                1.0,
                1.01);

        for (ArtifactBridgeTriplet candidate : candidates) {
            assertFalse(
                    candidate.getFirstBridgeBoundaryIndex() == 0
                            && candidate.getSecondBridgeBoundaryIndex() == 6);
        }
    }

    @Test
    public void noValidPeakMeansEmptyResult() {

        List<Point> boundary = flatBoundary();
        List<Integer> peaks = Arrays.asList(0, 1, 4);
        double[] scores = peakScores(boundary.size(), peaks, 10.0);

        List<ArtifactBridgeTriplet> candidates = detector.findCandidates(
                boundary,
                scores,
                peaks,
                1,
                10,
                10.0,
                100.0,
                1.01);

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void deterministicCandidateOrdering() {

        List<Point> boundary = orderingBoundary();
        List<Integer> peaks = Arrays.asList(0, 3, 6, 9);
        double[] scores = peakScores(boundary.size(), peaks, 10.0);
        scores[3] = 20.0;
        scores[6] = 15.0;

        List<ArtifactBridgeTriplet> candidates = detector.findCandidates(
                boundary,
                scores,
                peaks,
                1,
                20,
                30.0,
                0.5,
                1.01);

        assertTrue(candidates.size() >= 2);
        assertTrue(
                candidates.get(0).getPeakDistanceFromClosingLine()
                        >= candidates.get(1).getPeakDistanceFromClosingLine()
                                - TOLERANCE);
    }

    @Test
    public void returnedListIsUnmodifiable() {

        List<Point> boundary = artifactBoundary();
        List<Integer> peaks = Arrays.asList(0, 4, 8);
        double[] scores = peakScores(boundary.size(), peaks, 10.0);

        List<ArtifactBridgeTriplet> candidates = detector.findCandidates(
                boundary,
                scores,
                peaks,
                2,
                20,
                20.0,
                1.0,
                1.05);

        expectUnsupportedOperation(() -> candidates.add(candidates.get(0)));
    }

    @Test
    public void returnedPointsAreDefensiveCopies() {

        List<Point> boundary = artifactBoundary();
        List<Integer> peaks = Arrays.asList(0, 4, 8);
        double[] scores = peakScores(boundary.size(), peaks, 10.0);

        ArtifactBridgeTriplet triplet = detector.findCandidates(
                boundary,
                scores,
                peaks,
                2,
                20,
                20.0,
                1.0,
                1.05).get(0);

        Point firstPoint = triplet.getFirstBridgePoint();
        Point peakPoint = triplet.getBridgePeakPoint();
        Point secondPoint = triplet.getSecondBridgePoint();

        firstPoint.translate(10, 10);
        peakPoint.translate(10, 10);
        secondPoint.translate(10, 10);

        assertNotSame(boundary.get(0), triplet.getFirstBridgePoint());
        assertEquals(boundary.get(0), triplet.getFirstBridgePoint());
        assertEquals(boundary.get(4), triplet.getBridgePeakPoint());
        assertEquals(boundary.get(8), triplet.getSecondBridgePoint());
    }

    @Test
    public void inputBoundaryIsUnchanged() {

        List<Point> boundary = new ArrayList<>(artifactBoundary());
        List<Point> expected = copyPoints(boundary);
        List<Integer> peaks = Arrays.asList(0, 4, 8);
        double[] scores = peakScores(boundary.size(), peaks, 10.0);

        detector.findCandidates(
                boundary,
                scores,
                peaks,
                2,
                20,
                20.0,
                1.0,
                1.05);

        assertEquals(expected, boundary);
    }

    @Test
    public void inputPeakListIsUnchanged() {

        List<Point> boundary = artifactBoundary();
        List<Integer> peaks = new ArrayList<>(Arrays.asList(0, 4, 8));
        List<Integer> expected = new ArrayList<>(peaks);
        double[] scores = peakScores(boundary.size(), peaks, 10.0);

        detector.findCandidates(
                boundary,
                scores,
                peaks,
                2,
                20,
                20.0,
                1.0,
                1.05);

        assertEquals(expected, peaks);
    }

    @Test
    public void nullBoundaryRejected() {

        expectIllegalArgument(() -> detector.findCandidates(
                null,
                new double[3],
                Arrays.asList(0, 1, 2),
                1,
                3,
                10.0,
                0.0,
                1.0));
    }

    @Test
    public void shortBoundaryRejected() {

        expectIllegalArgument(() -> detector.findCandidates(
                Arrays.asList(p(0, 0), p(1, 0)),
                new double[] {0.0, 0.0},
                Arrays.asList(0, 1, 0),
                1,
                3,
                10.0,
                0.0,
                1.0));
    }

    @Test
    public void nullBoundaryPointRejected() {

        List<Point> boundary = new ArrayList<>();
        boundary.add(p(0, 0));
        boundary.add(null);
        boundary.add(p(2, 0));

        expectIllegalArgument(() -> detector.findCandidates(
                boundary,
                new double[] {0.0, 0.0, 0.0},
                Arrays.asList(0, 1, 2),
                1,
                3,
                10.0,
                0.0,
                1.0));
    }

    @Test
    public void duplicateBoundaryCoordinateRejected() {

        expectIllegalArgument(() -> detector.findCandidates(
                Arrays.asList(p(0, 0), p(1, 0), p(1, 0)),
                new double[] {0.0, 0.0, 0.0},
                Arrays.asList(0, 1, 2),
                1,
                3,
                10.0,
                0.0,
                1.0));
    }

    @Test
    public void internalDuplicateBoundaryCoordinateRejected() {

        expectIllegalArgument(() -> detector.findCandidates(
                repeatedInternalBoundary(),
                peakScores(6, Arrays.asList(0, 2, 4), 10.0),
                Arrays.asList(0, 2, 4),
                1,
                10,
                10.0,
                0.0,
                1.01));
    }

    @Test
    public void conventionalClosingDuplicateBoundaryCoordinateRejected() {

        expectIllegalArgument(() -> detector.findCandidates(
                closedArtifactBoundary(),
                peakScores(closedArtifactBoundary().size(), Arrays.asList(0, 4, 8), 10.0),
                Arrays.asList(0, 4, 8),
                2,
                20,
                20.0,
                1.0,
                1.05));
    }

    @Test
    public void mismatchedDeviationArrayLengthRejected() {

        List<Point> closedBoundary = closedArtifactBoundary();
        List<Point> openBoundary = artifactBoundary();
        List<Integer> peaks = Arrays.asList(0, 4, 8);
        double[] normalizedScores = peakScores(openBoundary.size(), peaks, 10.0);

        expectIllegalArgument(() -> detector.findCandidates(
                closedBoundary,
                normalizedScores,
                peaks,
                2,
                20,
                20.0,
                1.0,
                1.05));
    }

    @Test
    public void duplicateSuppliedPeakIndicesAreRejected() {

        List<Point> boundary = artifactBoundary();

        expectIllegalArgument(() -> detector.findCandidates(
                boundary,
                peakScores(boundary.size(), Arrays.asList(0, 4, 8), 10.0),
                Arrays.asList(0, 0, 4),
                2,
                20,
                20.0,
                1.0,
                1.05));
    }

    @Test
    public void inputCollectionsRemainUnchanged() {

        List<Point> boundary = new ArrayList<>(artifactBoundary());
        List<Point> expectedBoundary = copyPoints(boundary);
        List<Integer> peaks = new ArrayList<>(Arrays.asList(0, 4, 8));
        List<Integer> expectedPeaks = new ArrayList<>(peaks);
        double[] scores = peakScores(boundary.size(), peaks, 10.0);
        double[] expectedScores = Arrays.copyOf(scores, scores.length);

        detector.findCandidates(
                boundary,
                scores,
                peaks,
                2,
                20,
                20.0,
                1.0,
                1.05);

        assertEquals(expectedBoundary, boundary);
        assertEquals(expectedPeaks, peaks);
        assertTrue(Arrays.equals(expectedScores, scores));
    }

    private static List<Point> repeatedInternalBoundary() {
        return Arrays.asList(
                p(0, 0),
                p(1, 0),
                p(1, 1),
                p(1, 0),
                p(0, 1),
                p(0, 0));
    }

    private static List<Point> closedArtifactBoundary() {

        List<Point> boundary = new ArrayList<>(artifactBoundary());
        boundary.add(new Point(boundary.get(0)));
        return boundary;
    }

    @Test
    public void nullOrMismatchedDeviationArrayRejected() {

        List<Point> boundary = artifactBoundary();

        expectIllegalArgument(() -> detector.findCandidates(
                boundary,
                null,
                Arrays.asList(0, 4, 8),
                1,
                3,
                10.0,
                0.0,
                1.0));

        expectIllegalArgument(() -> detector.findCandidates(
                boundary,
                new double[boundary.size() - 1],
                Arrays.asList(0, 4, 8),
                1,
                3,
                10.0,
                0.0,
                1.0));
    }

    @Test
    public void nonFiniteOrNegativeDeviationScoreRejected() {

        List<Point> boundary = artifactBoundary();
        double[] scores = peakScores(boundary.size(), Arrays.asList(0, 4, 8), 10.0);
        scores[2] = Double.NaN;

        expectIllegalArgument(() -> detector.findCandidates(
                boundary,
                scores,
                Arrays.asList(0, 4, 8),
                1,
                3,
                10.0,
                0.0,
                1.0));

        scores[2] = -1.0;

        expectIllegalArgument(() -> detector.findCandidates(
                boundary,
                scores,
                Arrays.asList(0, 4, 8),
                1,
                3,
                10.0,
                0.0,
                1.0));
    }

    @Test
    public void nullPeakListRejected() {

        expectIllegalArgument(() -> detector.findCandidates(
                artifactBoundary(),
                peakScores(9, Arrays.asList(0, 4, 8), 10.0),
                null,
                1,
                3,
                10.0,
                0.0,
                1.0));
    }

    @Test
    public void nullPeakIndexRejected() {

        List<Integer> peaks = new ArrayList<>(Arrays.asList(0, 4, 8));
        peaks.set(1, null);

        expectIllegalArgument(() -> detector.findCandidates(
                artifactBoundary(),
                peakScores(9, Arrays.asList(0, 4, 8), 10.0),
                peaks,
                1,
                3,
                10.0,
                0.0,
                1.0));
    }

    @Test
    public void duplicatePeakIndexRejected() {

        expectIllegalArgument(() -> detector.findCandidates(
                artifactBoundary(),
                peakScores(9, Arrays.asList(0, 4, 8), 10.0),
                Arrays.asList(0, 4, 4),
                1,
                3,
                10.0,
                0.0,
                1.0));
    }

    @Test
    public void outOfRangePeakIndexRejected() {

        expectIllegalArgument(() -> detector.findCandidates(
                artifactBoundary(),
                peakScores(9, Arrays.asList(0, 4, 8), 10.0),
                Arrays.asList(0, 4, 99),
                1,
                3,
                10.0,
                0.0,
                1.0));
    }

    @Test
    public void fewerThanThreeAcceptedPeaksRejected() {

        expectIllegalArgument(() -> detector.findCandidates(
                artifactBoundary(),
                peakScores(9, Arrays.asList(0, 4), 10.0),
                Arrays.asList(0, 4),
                1,
                3,
                10.0,
                0.0,
                1.0));
    }

    @Test
    public void invalidNumericalParametersRejected() {

        List<Point> boundary = artifactBoundary();
        List<Integer> peaks = Arrays.asList(0, 4, 8);
        double[] scores = peakScores(boundary.size(), peaks, 10.0);

        expectIllegalArgument(() -> detector.findCandidates(
                boundary, scores, peaks, 0, 3, 10.0, 0.0, 1.0));
        expectIllegalArgument(() -> detector.findCandidates(
                boundary, scores, peaks, 1, 2, 10.0, 0.0, 1.0));
        expectIllegalArgument(() -> detector.findCandidates(
                boundary, scores, peaks, 1, 3, 0.0, 0.0, 1.0));
        expectIllegalArgument(() -> detector.findCandidates(
                boundary, scores, peaks, 1, 3, Double.NaN, 0.0, 1.0));
        expectIllegalArgument(() -> detector.findCandidates(
                boundary, scores, peaks, 1, 3, 10.0, -1.0, 1.0));
        expectIllegalArgument(() -> detector.findCandidates(
                boundary, scores, peaks, 1, 3, 10.0, 0.0, 0.5));
        expectIllegalArgument(() -> detector.findCandidates(
                boundary, scores, peaks, 1, 3, 10.0, Double.POSITIVE_INFINITY, 1.0));
    }

    private static List<Point> artifactBoundary() {
        return Arrays.asList(
                p(0, 0),
                p(1, 0),
                p(2, 0),
                p(3, 0),
                p(3, 1),
                p(3, 2),
                p(2, 3),
                p(1, 2),
                p(0, 1));
    }

    private static List<Point> squareBoundary() {
        return Arrays.asList(
                p(0, 0),
                p(1, 0),
                p(2, 0),
                p(2, 1),
                p(2, 2),
                p(1, 2),
                p(0, 2),
                p(0, 1));
    }

    private static List<Point> asymmetricBulgeBoundary() {
        return Arrays.asList(
                p(0, 0),
                p(1, 0),
                p(2, 0),
                p(3, 3),
                p(2, 1),
                p(1, 1),
                p(0, 1));
    }

    private static List<Point> symmetricBulgeBoundary() {
        return Arrays.asList(
                p(0, 0),
                p(1, 0),
                p(2, 3),
                p(3, 0),
                p(4, 3),
                p(5, 0),
                p(6, 0));
    }

    private static List<Point> finiteSegmentBoundary() {
        return Arrays.asList(
                p(0, 0),
                p(10, 0),
                p(12, 4),
                p(8, 2),
                p(6, 0));
    }

    private static List<Point> flatBoundary() {
        return Arrays.asList(
                p(0, 0),
                p(1, 0),
                p(2, 0),
                p(3, 0),
                p(4, 0));
    }

    private static List<Point> orderingBoundary() {
        return Arrays.asList(
                p(0, 0),
                p(1, 0),
                p(2, 0),
                p(2, 3),
                p(2, 6),
                p(1, 6),
                p(0, 6),
                p(0, 3),
                p(0, 2),
                p(0, 1));
    }

    private static double[] peakScores(
            int boundarySize,
            List<Integer> peakIndices,
            double peakScore) {

        double[] scores = new double[boundarySize];

        for (int peakIndex : peakIndices) {
            scores[peakIndex] = peakScore;
        }

        return scores;
    }

    private static boolean isStrictlyOnForwardArc(
            int firstBoundaryIndex,
            int secondBoundaryIndex,
            int candidateBoundaryIndex,
            int boundarySize) {

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

    private static double distancePointToSegment(
            Point point,
            Point segmentStart,
            Point segmentEnd) {

        double deltaX = segmentEnd.x - segmentStart.x;
        double deltaY = segmentEnd.y - segmentStart.y;
        double segmentLengthSquared = deltaX * deltaX + deltaY * deltaY;

        if (segmentLengthSquared == 0.0) {
            return Math.hypot(
                    point.x - segmentStart.x,
                    point.y - segmentStart.y);
        }

        double projectionNumerator =
                (point.x - segmentStart.x) * deltaX
                        + (point.y - segmentStart.y) * deltaY;
        double projectionParameter = projectionNumerator / segmentLengthSquared;
        projectionParameter = Math.max(0.0, Math.min(1.0, projectionParameter));

        double projectedX = segmentStart.x + projectionParameter * deltaX;
        double projectedY = segmentStart.y + projectionParameter * deltaY;

        return Math.hypot(point.x - projectedX, point.y - projectedY);
    }

    private static double distancePointToInfiniteLine(
            Point point,
            Point lineStart,
            Point lineEnd) {

        double deltaX = lineEnd.x - lineStart.x;
        double deltaY = lineEnd.y - lineStart.y;
        double length = Math.hypot(deltaX, deltaY);

        if (length == 0.0) {
            return Math.hypot(point.x - lineStart.x, point.y - lineStart.y);
        }

        return Math.abs(
                deltaX * (lineStart.y - point.y)
                        - (lineStart.x - point.x) * deltaY)
                / length;
    }

    private static List<Point> copyPoints(List<Point> points) {

        List<Point> copies = new ArrayList<>(points.size());

        for (Point point : points) {
            copies.add(new Point(point));
        }

        return copies;
    }

    private static Point p(int x, int y) {
        return new Point(x, y);
    }

    private static void expectIllegalArgument(Runnable action) {

        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }

        throw new AssertionError("Expected IllegalArgumentException.");
    }

    private static void expectUnsupportedOperation(Runnable action) {

        try {
            action.run();
        } catch (UnsupportedOperationException expected) {
            return;
        }

        throw new AssertionError("Expected UnsupportedOperationException.");
    }

    private static ArtifactBridgeTriplet findTriplet(
            List<ArtifactBridgeTriplet> candidates,
            int firstBoundaryIndex,
            int bridgePeakBoundaryIndex,
            int secondBoundaryIndex) {

        for (ArtifactBridgeTriplet candidate : candidates) {

            if (candidate.getFirstBridgeBoundaryIndex() == firstBoundaryIndex
                    && candidate.getBridgePeakBoundaryIndex() == bridgePeakBoundaryIndex
                    && candidate.getSecondBridgeBoundaryIndex() == secondBoundaryIndex) {
                return candidate;
            }
        }

        throw new AssertionError("Expected triplet not found.");
    }
}
