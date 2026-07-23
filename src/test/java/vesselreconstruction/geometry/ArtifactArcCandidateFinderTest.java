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

public class ArtifactArcCandidateFinderTest {

    private static final double TOLERANCE = 1.0e-9;

    private final ArtifactArcCandidateFinder finder = new ArtifactArcCandidateFinder();

    @Test
    public void oneClearSinglePeakedArcIsDetected() {

        List<Point> boundary = singlePeakedArcBoundary();
        double[] scores = zeroScores(boundary.size());

        List<ArtifactArcCandidate> candidates = finder.findCandidates(
                boundary,
                scores,
                2,
                20,
                1.0,
                100.0,
                1.0,
                1.05,
                1.01,
                2,
                2.0);

        ArtifactArcCandidate selected = findCandidate(candidates, 0, 5, 10);
        assertEquals(5, selected.getPeakBoundaryIndex());
        assertTrue(selected.getPeakDistanceFromClosingLine() > 1.0);
    }

    @Test
    public void peakIsDerivedRatherThanSupplied() {

        List<Point> boundary = singlePeakedArcBoundary();
        double[] scores = zeroScores(boundary.size());

        List<ArtifactArcCandidate> candidates = finder.findCandidates(
                boundary,
                scores,
                2,
                20,
                1.0,
                100.0,
                1.0,
                1.05,
                1.01,
                2,
                2.0);

        ArtifactArcCandidate selected = findCandidate(candidates, 0, 5, 10);
        assertEquals(
                distancePointToSegment(
                        boundary.get(5),
                        boundary.get(0),
                        boundary.get(10)),
                selected.getPeakDistanceFromClosingLine(),
                TOLERANCE);
    }

    @Test
    public void greatestFiniteSegmentDistanceDefinesPeak() {

        List<Point> boundary = asymmetricBulgeBoundary();
        double[] scores = zeroScores(boundary.size());

        List<ArtifactArcCandidate> candidates = finder.findCandidates(
                boundary,
                scores,
                1,
                20,
                1.0,
                100.0,
                1.0,
                1.01,
                1.01,
                1,
                5.0);

        ArtifactArcCandidate selected = findCandidate(candidates, 0, 3, 6);
        assertEquals(3, selected.getPeakBoundaryIndex());
    }

    @Test
    public void finiteSegmentDistanceDiffersFromInfiniteLineDistance() {

        List<Point> boundary = finiteSegmentBoundary();
        double[] scores = zeroScores(boundary.size());
        Point peakPoint = boundary.get(2);
        Point firstPoint = boundary.get(0);
        Point secondPoint = boundary.get(4);
        double finiteDistance = distancePointToSegment(
                peakPoint,
                firstPoint,
                secondPoint);
        double infiniteDistance = distancePointToInfiniteLine(
                peakPoint,
                firstPoint,
                secondPoint);

        List<ArtifactArcCandidate> candidates = finder.findCandidates(
                boundary,
                scores,
                1,
                10,
                1.0,
                100.0,
                finiteDistance - 0.1,
                1.01,
                1.01,
                1,
                5.0);

        ArtifactArcCandidate selected = findCandidate(candidates, 0, 2, 4);
        assertEquals(finiteDistance, selected.getPeakDistanceFromClosingLine(), TOLERANCE);
        assertTrue(infiniteDistance < finiteDistance);
    }

    @Test
    public void uniqueMaximumIsRequired() {

        List<Point> boundary = singlePeakedArcBoundary();
        double[] scores = zeroScores(boundary.size());

        List<ArtifactArcCandidate> candidates = finder.findCandidates(
                boundary,
                scores,
                2,
                20,
                1.0,
                100.0,
                1.0,
                1.05,
                1.01,
                2,
                2.0);

        for (ArtifactArcCandidate candidate : candidates) {
            assertTrue(candidate.getPeakDominanceRatio() >= 1.05 - TOLERANCE);
        }
    }

    @Test
    public void equalMaximumDistancesRejectPair() {

        List<Point> boundary = symmetricBulgeBoundary();
        double[] scores = zeroScores(boundary.size());

        List<ArtifactArcCandidate> candidates = finder.findCandidates(
                boundary,
                scores,
                1,
                20,
                1.0,
                100.0,
                1.0,
                1.01,
                1.01,
                1,
                5.0);

        assertTrue(candidates.isEmpty()
                || candidates.stream().noneMatch(candidate ->
                        candidate.getFirstBoundaryIndex() == 0
                                && candidate.getSecondBoundaryIndex() == 6));
    }

    @Test
    public void insufficientPeakDominanceIsRejected() {

        List<Point> boundary = singlePeakedArcBoundary();
        double[] scores = zeroScores(boundary.size());

        List<ArtifactArcCandidate> candidates = finder.findCandidates(
                boundary,
                scores,
                2,
                20,
                1.0,
                100.0,
                1.0,
                100.0,
                1.01,
                2,
                2.0);

        assertFalse(findCandidateOptional(candidates, 0, 5, 10).isPresent());
    }

    @Test
    public void insufficientPeakDistanceIsRejected() {

        List<Point> boundary = flatBoundary();
        double[] scores = zeroScores(boundary.size());

        List<ArtifactArcCandidate> candidates = finder.findCandidates(
                boundary,
                scores,
                1,
                10,
                1.0,
                100.0,
                5.0,
                1.01,
                1.01,
                1,
                5.0);

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void insufficientFirstFlankIsRejected() {

        List<Point> boundary = singlePeakedArcBoundary();
        double[] scores = zeroScores(boundary.size());

        List<ArtifactArcCandidate> candidates = finder.findCandidates(
                boundary,
                scores,
                2,
                20,
                1.0,
                100.0,
                1.0,
                1.05,
                1.01,
                8,
                2.0);

        assertFalse(findCandidateOptional(candidates, 0, 5, 10).isPresent());
    }

    @Test
    public void insufficientSecondFlankIsRejected() {

        List<Point> boundary = singlePeakedArcBoundary();
        double[] scores = zeroScores(boundary.size());

        List<ArtifactArcCandidate> candidates = finder.findCandidates(
                boundary,
                scores,
                2,
                20,
                1.0,
                100.0,
                1.0,
                1.05,
                1.01,
                6,
                2.0);

        assertFalse(findCandidateOptional(candidates, 0, 5, 10).isPresent());
    }

    @Test
    public void excessiveArcPointCountIsRejected() {

        List<Point> boundary = singlePeakedArcBoundary();
        double[] scores = zeroScores(boundary.size());

        List<ArtifactArcCandidate> candidates = finder.findCandidates(
                boundary,
                scores,
                2,
                8,
                1.0,
                100.0,
                1.0,
                1.05,
                1.01,
                2,
                2.0);

        assertFalse(findCandidateOptional(candidates, 0, 5, 10).isPresent());
    }

    @Test
    public void tooShortClosingLineIsRejected() {

        List<Point> boundary = singlePeakedArcBoundary();
        double[] scores = zeroScores(boundary.size());

        List<ArtifactArcCandidate> candidates = finder.findCandidates(
                boundary,
                scores,
                2,
                20,
                100.0,
                200.0,
                1.0,
                1.05,
                1.01,
                2,
                2.0);

        assertFalse(findCandidateOptional(candidates, 0, 5, 10).isPresent());
    }

    @Test
    public void tooLongClosingLineIsRejected() {

        List<Point> boundary = singlePeakedArcBoundary();
        double[] scores = zeroScores(boundary.size());

        List<ArtifactArcCandidate> candidates = finder.findCandidates(
                boundary,
                scores,
                2,
                20,
                1.0,
                5.0,
                1.0,
                1.05,
                1.01,
                2,
                2.0);

        assertFalse(findCandidateOptional(candidates, 0, 5, 10).isPresent());
    }

    @Test
    public void insufficientArcToChordRatioIsRejected() {

        List<Point> boundary = flatBoundary();
        double[] scores = zeroScores(boundary.size());

        List<ArtifactArcCandidate> candidates = finder.findCandidates(
                boundary,
                scores,
                1,
                10,
                1.0,
                100.0,
                0.0,
                1.01,
                10.0,
                1,
                5.0);

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void monotonicRisingAndFallingProfileIsAccepted() {

        List<Point> boundary = singlePeakedArcBoundary();
        double[] scores = zeroScores(boundary.size());

        ArtifactArcCandidate selected = findCandidate(
                finder.findCandidates(
                        boundary,
                        scores,
                        2,
                        20,
                        1.0,
                        100.0,
                        1.0,
                        1.05,
                        1.01,
                        2,
                        0.0),
                0,
                5,
                10);

        assertEquals(0.0, selected.getMaximumProfileRiseViolation(), TOLERANCE);
        assertEquals(0.0, selected.getMaximumProfileFallViolation(), TOLERANCE);
    }

    @Test
    public void smallProfileViolationsWithinToleranceAreAccepted() {

        List<Point> boundary = noisySinglePeakedArcBoundary();
        double[] scores = zeroScores(boundary.size());

        List<ArtifactArcCandidate> candidates = finder.findCandidates(
                boundary,
                scores,
                2,
                20,
                1.0,
                100.0,
                1.0,
                1.05,
                1.01,
                2,
                1.5);

        assertTrue(findCandidateOptional(candidates, 0, 6, 11).isPresent());
    }

    @Test
    public void excessiveRiseViolationIsRejected() {

        List<Point> boundary = riseViolationBoundary();
        double[] scores = zeroScores(boundary.size());

        List<ArtifactArcCandidate> candidates = finder.findCandidates(
                boundary,
                scores,
                1,
                20,
                1.0,
                100.0,
                1.0,
                1.01,
                1.01,
                1,
                0.5);

        assertFalse(findCandidateOptional(candidates, 0, 4, 8).isPresent());
    }

    @Test
    public void excessiveFallViolationIsRejected() {

        List<Point> boundary = fallViolationBoundary();
        double[] scores = zeroScores(boundary.size());

        List<ArtifactArcCandidate> candidates = finder.findCandidates(
                boundary,
                scores,
                1,
                20,
                1.0,
                100.0,
                1.0,
                1.01,
                1.01,
                1,
                0.5);

        assertFalse(findCandidateOptional(candidates, 0, 4, 8).isPresent());
    }

    @Test
    public void wrapAroundCircularArcIsSupported() {

        List<Point> boundary = wrapAroundBoundary();
        double[] scores = zeroScores(boundary.size());

        List<ArtifactArcCandidate> candidates = finder.findCandidates(
                boundary,
                scores,
                1,
                20,
                1.0,
                100.0,
                1.0,
                1.01,
                1.01,
                1,
                2.0);

        assertTrue(findCandidateOptional(candidates, 7, 9, 4).isPresent());
    }

    @Test
    public void endpointDeviationScoresAreStored() {

        List<Point> boundary = singlePeakedArcBoundary();
        double[] scores = peakScores(boundary.size(), Arrays.asList(0, 5, 10), 4.0, 9.0, 6.0);

        ArtifactArcCandidate selected = findCandidate(
                finder.findCandidates(
                        boundary,
                        scores,
                        2,
                        20,
                        1.0,
                        100.0,
                        1.0,
                        1.05,
                        1.01,
                        2,
                        2.0),
                0,
                5,
                10);

        assertEquals(4.0, selected.getFirstEndpointDeviationScore(), TOLERANCE);
        assertEquals(9.0, selected.getPeakDeviationScore(), TOLERANCE);
        assertEquals(6.0, selected.getSecondEndpointDeviationScore(), TOLERANCE);
    }

    @Test
    public void deterministicOrdering() {

        List<Point> boundary = orderingBoundary();
        double[] scores = zeroScores(boundary.size());
        scores[3] = 20.0;
        scores[6] = 15.0;
        scores[0] = 5.0;
        scores[9] = 7.0;

        List<ArtifactArcCandidate> firstRun = finder.findCandidates(
                boundary,
                scores,
                1,
                20,
                1.0,
                100.0,
                1.0,
                1.01,
                1.01,
                1,
                5.0);
        List<ArtifactArcCandidate> secondRun = finder.findCandidates(
                boundary,
                scores,
                1,
                20,
                1.0,
                100.0,
                1.0,
                1.01,
                1.01,
                1,
                5.0);

        assertEquals(firstRun.size(), secondRun.size());

        if (firstRun.size() >= 2) {
            assertTrue(
                    firstRun.get(0).getPeakDominanceRatio()
                            >= firstRun.get(1).getPeakDominanceRatio() - TOLERANCE);
            assertEquals(
                    firstRun.get(0).getFirstBoundaryIndex(),
                    secondRun.get(0).getFirstBoundaryIndex());
            assertEquals(
                    firstRun.get(0).getPeakBoundaryIndex(),
                    secondRun.get(0).getPeakBoundaryIndex());
            assertEquals(
                    firstRun.get(0).getSecondBoundaryIndex(),
                    secondRun.get(0).getSecondBoundaryIndex());
        }
    }

    @Test
    public void returnedListIsUnmodifiable() {

        List<Point> boundary = singlePeakedArcBoundary();
        double[] scores = zeroScores(boundary.size());

        List<ArtifactArcCandidate> candidates = finder.findCandidates(
                boundary,
                scores,
                2,
                20,
                1.0,
                100.0,
                1.0,
                1.05,
                1.01,
                2,
                2.0);

        expectUnsupportedOperation(() -> candidates.add(candidates.get(0)));
    }

    @Test
    public void defensivePointCopies() {

        List<Point> boundary = new ArrayList<>(singlePeakedArcBoundary());
        double[] scores = zeroScores(boundary.size());

        ArtifactArcCandidate candidate = findCandidate(
                finder.findCandidates(
                        boundary,
                        scores,
                        2,
                        20,
                        1.0,
                        100.0,
                        1.0,
                        1.05,
                        1.01,
                        2,
                        2.0),
                0,
                5,
                10);

        boundary.get(0).translate(10, 10);
        boundary.get(5).translate(10, 10);
        boundary.get(10).translate(10, 10);

        assertEquals(p(0, 0), candidate.getFirstPoint());
        assertEquals(p(10, 8), candidate.getPeakPoint());
        assertEquals(p(20, 0), candidate.getSecondPoint());
        assertNotSame(boundary.get(0), candidate.getFirstPoint());
    }

    @Test
    public void inputsUnchanged() {

        List<Point> boundary = new ArrayList<>(singlePeakedArcBoundary());
        List<Point> expectedBoundary = copyPoints(boundary);
        double[] scores = zeroScores(boundary.size());
        double[] expectedScores = Arrays.copyOf(scores, scores.length);

        finder.findCandidates(
                boundary,
                scores,
                2,
                20,
                1.0,
                100.0,
                1.0,
                1.05,
                1.01,
                2,
                2.0);

        assertEquals(expectedBoundary, boundary);
        assertTrue(Arrays.equals(expectedScores, scores));
    }

    @Test
    public void nullBoundaryReportsNullBoundary() {

        expectIllegalArgumentMessage(
                "NULL_BOUNDARY",
                () -> finder.findCandidates(
                        null,
                        new double[3],
                        1,
                        3,
                        0.0,
                        10.0,
                        0.0,
                        1.0,
                        1.0,
                        1,
                        1.0));
    }

    @Test
    public void shortBoundaryReportsBoundaryTooShort() {

        expectIllegalArgumentMessage(
                "BOUNDARY_TOO_SHORT: size=2",
                () -> finder.findCandidates(
                        Arrays.asList(p(0, 0), p(1, 0)),
                        new double[] {0.0, 0.0},
                        1,
                        3,
                        0.0,
                        10.0,
                        0.0,
                        1.0,
                        1.0,
                        1,
                        1.0));
    }

    @Test
    public void nullBoundaryPointReportsNullBoundaryPoint() {

        List<Point> boundary = new ArrayList<>();
        boundary.add(p(0, 0));
        boundary.add(null);
        boundary.add(p(2, 0));

        expectIllegalArgumentMessage(
                "NULL_BOUNDARY_POINT: index=1",
                () -> finder.findCandidates(
                        boundary,
                        new double[] {0.0, 0.0, 0.0},
                        1,
                        3,
                        0.0,
                        10.0,
                        0.0,
                        1.0,
                        1.0,
                        1,
                        1.0));
    }

    @Test
    public void internalDuplicateReportsDuplicateBoundaryVertex() {

        expectIllegalArgumentMessage(
                "DUPLICATE_BOUNDARY_VERTEX: index=2, firstIndex=1, x=1, y=0",
                () -> finder.findCandidates(
                        Arrays.asList(p(0, 0), p(1, 0), p(1, 0)),
                        new double[] {0.0, 0.0, 0.0},
                        1,
                        3,
                        0.0,
                        10.0,
                        0.0,
                        1.0,
                        1.0,
                        1,
                        1.0));
    }

    @Test
    public void repeatedClosingVertexReportsRepeatedClosingVertex() {

        List<Point> boundary = new ArrayList<>(singlePeakedArcBoundary());
        boundary.add(new Point(boundary.get(0)));

        expectIllegalArgumentMessage(
                "REPEATED_CLOSING_VERTEX: firstIndex=0, lastIndex="
                        + (boundary.size() - 1)
                        + ", x=0, y=0",
                () -> finder.findCandidates(
                        boundary,
                        zeroScores(boundary.size()),
                        1,
                        20,
                        0.0,
                        100.0,
                        0.0,
                        1.0,
                        1.0,
                        1,
                        1.0));
    }

    @Test
    public void deviationLengthMismatchReportsBothSizes() {

        List<Point> boundary = singlePeakedArcBoundary();

        expectIllegalArgumentMessage(
                "DEVIATION_LENGTH_MISMATCH: boundarySize="
                        + boundary.size()
                        + ", deviationCount="
                        + (boundary.size() - 1),
                () -> finder.findCandidates(
                        boundary,
                        new double[boundary.size() - 1],
                        1,
                        3,
                        0.0,
                        10.0,
                        0.0,
                        1.0,
                        1.0,
                        1,
                        1.0));
    }

    @Test
    public void nonFiniteDeviationScoreReportsIndex() {

        List<Point> boundary = singlePeakedArcBoundary();
        double[] scores = zeroScores(boundary.size());
        scores[2] = Double.NaN;

        expectIllegalArgumentMessage(
                "NON_FINITE_DEVIATION_SCORE: index=2",
                () -> finder.findCandidates(
                        boundary,
                        scores,
                        1,
                        3,
                        0.0,
                        10.0,
                        0.0,
                        1.0,
                        1.0,
                        1,
                        1.0));
    }

    @Test
    public void negativeDeviationScoreReportsIndexAndValue() {

        List<Point> boundary = singlePeakedArcBoundary();
        double[] scores = zeroScores(boundary.size());
        scores[2] = -1.0;

        expectIllegalArgumentMessage(
                "NEGATIVE_DEVIATION_SCORE: index=2, value=-1.0",
                () -> finder.findCandidates(
                        boundary,
                        scores,
                        1,
                        3,
                        0.0,
                        10.0,
                        0.0,
                        1.0,
                        1.0,
                        1,
                        1.0));
    }

    @Test
    public void invalidNumericalParametersReportInvalidNumericalParameter() {

        List<Point> boundary = singlePeakedArcBoundary();
        double[] scores = zeroScores(boundary.size());

        expectIllegalArgumentStartsWith(
                "INVALID_NUMERICAL_PARAMETER:",
                () -> finder.findCandidates(
                        boundary, scores, 0, 3, 0.0, 10.0, 0.0, 1.0, 1.0, 1, 1.0));
    }

    @Test
    public void validUniqueContourReachesCandidateGeneration() {

        List<Point> boundary = singlePeakedArcBoundary();
        double[] scores = zeroScores(boundary.size());

        List<ArtifactArcCandidate> candidates = finder.findCandidates(
                boundary,
                scores,
                2,
                20,
                1.0,
                100.0,
                1.0,
                1.05,
                1.01,
                2,
                2.0);

        assertFalse(candidates.isEmpty());
    }

    @Test
    public void nullBoundaryRejected() {

        expectIllegalArgumentMessage(
                "NULL_BOUNDARY",
                () -> finder.findCandidates(
                        null,
                        new double[3],
                        1,
                        3,
                        0.0,
                        10.0,
                        0.0,
                        1.0,
                        1.0,
                        1,
                        1.0));
    }

    @Test
    public void shortBoundaryRejected() {

        expectIllegalArgumentMessage(
                "BOUNDARY_TOO_SHORT: size=2",
                () -> finder.findCandidates(
                        Arrays.asList(p(0, 0), p(1, 0)),
                        new double[] {0.0, 0.0},
                        1,
                        3,
                        0.0,
                        10.0,
                        0.0,
                        1.0,
                        1.0,
                        1,
                        1.0));
    }

    @Test
    public void nullBoundaryPointRejected() {

        List<Point> boundary = new ArrayList<>();
        boundary.add(p(0, 0));
        boundary.add(null);
        boundary.add(p(2, 0));

        expectIllegalArgumentMessage(
                "NULL_BOUNDARY_POINT: index=1",
                () -> finder.findCandidates(
                        boundary,
                        new double[] {0.0, 0.0, 0.0},
                        1,
                        3,
                        0.0,
                        10.0,
                        0.0,
                        1.0,
                        1.0,
                        1,
                        1.0));
    }

    @Test
    public void duplicateBoundaryCoordinateRejected() {

        expectIllegalArgumentMessage(
                "DUPLICATE_BOUNDARY_VERTEX: index=2, firstIndex=1, x=1, y=0",
                () -> finder.findCandidates(
                        Arrays.asList(p(0, 0), p(1, 0), p(1, 0)),
                        new double[] {0.0, 0.0, 0.0},
                        1,
                        3,
                        0.0,
                        10.0,
                        0.0,
                        1.0,
                        1.0,
                        1,
                        1.0));
    }

    @Test
    public void repeatedFinalPointRejected() {

        List<Point> boundary = new ArrayList<>(singlePeakedArcBoundary());
        boundary.add(new Point(boundary.get(0)));

        expectIllegalArgumentMessage(
                "REPEATED_CLOSING_VERTEX: firstIndex=0, lastIndex="
                        + (boundary.size() - 1)
                        + ", x=0, y=0",
                () -> finder.findCandidates(
                        boundary,
                        zeroScores(boundary.size()),
                        1,
                        20,
                        0.0,
                        100.0,
                        0.0,
                        1.0,
                        1.0,
                        1,
                        1.0));
    }

    @Test
    public void nullOrMismatchedDeviationArrayRejected() {

        List<Point> boundary = singlePeakedArcBoundary();

        expectIllegalArgumentMessage(
                "NULL_DEVIATION_SCORE",
                () -> finder.findCandidates(
                        boundary,
                        null,
                        1,
                        3,
                        0.0,
                        10.0,
                        0.0,
                        1.0,
                        1.0,
                        1,
                        1.0));

        expectIllegalArgumentMessage(
                "DEVIATION_LENGTH_MISMATCH: boundarySize="
                        + boundary.size()
                        + ", deviationCount="
                        + (boundary.size() - 1),
                () -> finder.findCandidates(
                        boundary,
                        new double[boundary.size() - 1],
                        1,
                        3,
                        0.0,
                        10.0,
                        0.0,
                        1.0,
                        1.0,
                        1,
                        1.0));
    }

    @Test
    public void invalidDeviationScoreRejected() {

        List<Point> boundary = singlePeakedArcBoundary();
        double[] scores = zeroScores(boundary.size());
        scores[2] = Double.NaN;

        expectIllegalArgumentMessage(
                "NON_FINITE_DEVIATION_SCORE: index=2",
                () -> finder.findCandidates(
                        boundary,
                        scores,
                        1,
                        3,
                        0.0,
                        10.0,
                        0.0,
                        1.0,
                        1.0,
                        1,
                        1.0));

        scores[2] = -1.0;

        expectIllegalArgumentMessage(
                "NEGATIVE_DEVIATION_SCORE: index=2, value=-1.0",
                () -> finder.findCandidates(
                        boundary,
                        scores,
                        1,
                        3,
                        0.0,
                        10.0,
                        0.0,
                        1.0,
                        1.0,
                        1,
                        1.0));
    }

    @Test
    public void invalidNumericalParametersRejected() {

        List<Point> boundary = singlePeakedArcBoundary();
        double[] scores = zeroScores(boundary.size());

        expectIllegalArgumentStartsWith(
                "INVALID_NUMERICAL_PARAMETER:",
                () -> finder.findCandidates(
                        boundary, scores, 0, 3, 0.0, 10.0, 0.0, 1.0, 1.0, 1, 1.0));
        expectIllegalArgumentStartsWith(
                "INVALID_NUMERICAL_PARAMETER:",
                () -> finder.findCandidates(
                        boundary, scores, 1, 2, 0.0, 10.0, 0.0, 1.0, 1.0, 1, 1.0));
        expectIllegalArgumentStartsWith(
                "INVALID_NUMERICAL_PARAMETER:",
                () -> finder.findCandidates(
                        boundary, scores, 1, 3, -1.0, 10.0, 0.0, 1.0, 1.0, 1, 1.0));
        expectIllegalArgumentStartsWith(
                "INVALID_NUMERICAL_PARAMETER:",
                () -> finder.findCandidates(
                        boundary, scores, 1, 3, 5.0, 5.0, 0.0, 1.0, 1.0, 1, 1.0));
        expectIllegalArgumentStartsWith(
                "INVALID_NUMERICAL_PARAMETER:",
                () -> finder.findCandidates(
                        boundary, scores, 1, 3, 0.0, 10.0, -1.0, 1.0, 1.0, 1, 1.0));
        expectIllegalArgumentStartsWith(
                "INVALID_NUMERICAL_PARAMETER:",
                () -> finder.findCandidates(
                        boundary, scores, 1, 3, 0.0, 10.0, 0.0, 0.5, 1.0, 1, 1.0));
        expectIllegalArgumentStartsWith(
                "INVALID_NUMERICAL_PARAMETER:",
                () -> finder.findCandidates(
                        boundary, scores, 1, 3, 0.0, 10.0, 0.0, 1.0, 0.5, 1, 1.0));
        expectIllegalArgumentStartsWith(
                "INVALID_NUMERICAL_PARAMETER:",
                () -> finder.findCandidates(
                        boundary, scores, 1, 3, 0.0, 10.0, 0.0, 1.0, 1.0, 0, 1.0));
        expectIllegalArgumentStartsWith(
                "INVALID_NUMERICAL_PARAMETER:",
                () -> finder.findCandidates(
                        boundary, scores, 1, 3, 0.0, 10.0, 0.0, 1.0, 1.0, 1, -1.0));
    }

    @Test
    public void mouthWidthEqualsEuclideanDistanceBetweenEndpoints() {

        ArtifactArcCandidate selected = selectSinglePeakedCandidate();
        assertEquals(
                euclideanDistance(selected.getFirstPoint(), selected.getSecondPoint()),
                selected.getMouthWidth(),
                TOLERANCE);
        assertEquals(selected.getClosingLineLength(), selected.getMouthWidth(), TOLERANCE);
    }

    @Test
    public void protrusionDepthMatchesFiniteSegmentPeakDistance() {

        ArtifactArcCandidate selected = selectSinglePeakedCandidate();
        assertEquals(
                selected.getPeakDistanceFromClosingLine(),
                selected.getProtrusionDepth(),
                TOLERANCE);
    }

    @Test
    public void depthToMouthRatioIsProtrusionDepthOverMouthWidth() {

        ArtifactArcCandidate selected = selectSinglePeakedCandidate();
        assertEquals(
                selected.getProtrusionDepth() / selected.getMouthWidth(),
                selected.getDepthToMouthRatio(),
                TOLERANCE);
    }

    @Test
    public void arcToMouthRatioIsArcLengthOverMouthWidth() {

        ArtifactArcCandidate selected = selectSinglePeakedCandidate();
        assertEquals(
                selected.getArtifactArcLength() / selected.getMouthWidth(),
                selected.getArcToMouthRatio(),
                TOLERANCE);
    }

    @Test
    public void relativeContourSpanUsesFullContourLength() {

        List<Point> boundary = singlePeakedArcBoundary();
        ArtifactArcCandidate selected = selectSinglePeakedCandidate();
        double totalContourLength = 0.0;

        for (int index = 0; index < boundary.size(); index++) {
            Point current = boundary.get(index);
            Point next = boundary.get((index + 1) % boundary.size());
            totalContourLength += euclideanDistance(current, next);
        }

        assertEquals(
                selected.getArtifactArcLength() / totalContourLength,
                selected.getRelativeContourSpan(),
                TOLERANCE);
    }

    @Test
    public void attachmentAnglesAreFiniteAndNonNegative() {

        ArtifactArcCandidate selected = selectSinglePeakedCandidate();
        assertTrue(selected.getFirstAttachmentTurnDegrees() >= 0.0);
        assertTrue(selected.getSecondAttachmentTurnDegrees() >= 0.0);
        assertEquals(
                Math.min(
                        selected.getFirstAttachmentTurnDegrees(),
                        selected.getSecondAttachmentTurnDegrees()),
                selected.getMinimumAttachmentTurnDegrees(),
                TOLERANCE);
        assertEquals(
                0.5 * (selected.getFirstAttachmentTurnDegrees()
                        + selected.getSecondAttachmentTurnDegrees()),
                selected.getMeanAttachmentTurnDegrees(),
                TOLERANCE);
    }

    @Test
    public void attachmentBalanceIsMinOverMax() {

        ArtifactArcCandidate selected = selectSinglePeakedCandidate();
        double expected = Math.min(
                selected.getFirstAttachmentTurnDegrees(),
                selected.getSecondAttachmentTurnDegrees())
                / Math.max(
                        selected.getFirstAttachmentTurnDegrees(),
                        selected.getSecondAttachmentTurnDegrees());
        assertEquals(expected, selected.getAttachmentTurnBalance(), TOLERANCE);
        assertTrue(selected.getAttachmentTurnBalance() <= 1.0 + TOLERANCE);
    }

    @Test
    public void outsideWallDirectionDifferenceAndContinuityScoreAreConsistent() {

        ArtifactArcCandidate selected = selectSinglePeakedCandidate();
        assertTrue(selected.getOutsideWallDirectionDifferenceDegrees() >= 0.0);
        assertTrue(selected.getOutsideWallDirectionDifferenceDegrees() <= 180.0 + TOLERANCE);
        assertEquals(
                ArtifactArcCandidateFinder.clamp01(
                        1.0 - selected.getOutsideWallDirectionDifferenceDegrees() / 180.0),
                selected.getOutsideWallContinuityScore(),
                TOLERANCE);
    }

    @Test
    public void outsideWallLineDeviationIsNonNegative() {

        ArtifactArcCandidate selected = selectSinglePeakedCandidate();
        assertTrue(selected.getOutsideWallLineDeviation() >= 0.0);
        assertTrue(Double.isFinite(selected.getOutsideWallLineDeviation()));
    }

    @Test
    public void peakArcFractionAndCentralityAreComputedFromArcLengths() {

        ArtifactArcCandidate selected = selectSinglePeakedCandidate();
        assertTrue(selected.getPeakArcFraction() >= 0.0);
        assertTrue(selected.getPeakArcFraction() <= 1.0 + TOLERANCE);
        assertEquals(
                ArtifactArcCandidateFinder.clamp01(
                        1.0 - 2.0 * Math.abs(selected.getPeakArcFraction() - 0.5)),
                selected.getPeakCentrality(),
                TOLERANCE);
    }

    @Test
    public void peakTurnAngleIsFiniteAndNonNegative() {

        ArtifactArcCandidate selected = selectSinglePeakedCandidate();
        assertTrue(selected.getPeakTurnDegrees() >= 0.0);
        assertTrue(selected.getPeakTurnDegrees() <= 180.0 + TOLERANCE);
    }

    @Test
    public void tShapeScoreUsesDocumentedNormalizationAndClamping() {

        double score = ArtifactArcCandidateFinder.computeTShapeDiagnosticScore(
                6.0,
                180.0,
                2.0,
                3.0,
                -1.0);

        assertEquals(
                0.35 * 1.0 + 0.25 * 1.0 + 0.15 * 1.0 + 0.15 * 1.0 + 0.10 * 1.0,
                score,
                TOLERANCE);
        assertEquals(0.0, ArtifactArcCandidateFinder.clamp01(-5.0), TOLERANCE);
        assertEquals(1.0, ArtifactArcCandidateFinder.clamp01(5.0), TOLERANCE);
        assertEquals(0.0, ArtifactArcCandidateFinder.safeDivide(1.0, 0.0), TOLERANCE);
    }

    @Test
    public void tShapeDiagnosticsAreImmutableAndDeterministic() {

        List<Point> boundary = singlePeakedArcBoundary();
        double[] scores = zeroScores(boundary.size());
        List<ArtifactArcCandidate> first = finder.findCandidates(
                boundary, scores, 2, 20, 1.0, 100.0, 1.0, 1.05, 1.01, 2, 2.0);
        List<ArtifactArcCandidate> second = finder.findCandidates(
                boundary, scores, 2, 20, 1.0, 100.0, 1.0, 1.05, 1.01, 2, 2.0);
        ArtifactArcCandidate selected = findCandidate(first, 0, 5, 10);
        ArtifactArcCandidate repeated = findCandidate(second, 0, 5, 10);

        assertEquals(selected.getMouthWidth(), repeated.getMouthWidth(), TOLERANCE);
        assertEquals(
                selected.getTShapeDiagnosticScore(),
                repeated.getTShapeDiagnosticScore(),
                TOLERANCE);
        assertNotSame(selected.getFirstPoint(), selected.getFirstPoint());
        selected.getFirstPoint().translate(9, 9);
        assertEquals(0, findCandidate(first, 0, 5, 10).getFirstPoint().x);
    }

    @Test
    public void tShapeScoreDoesNotInfluenceCandidateOrdering() {

        List<Point> boundary = orderingBoundary();
        double[] scores = zeroScores(boundary.size());
        scores[3] = 20.0;
        scores[6] = 15.0;
        scores[0] = 5.0;
        scores[9] = 7.0;

        List<ArtifactArcCandidate> candidates = finder.findCandidates(
                boundary, scores, 1, 20, 1.0, 100.0, 1.0, 1.01, 1.01, 1, 5.0);

        for (int index = 1; index < candidates.size(); index++) {
            ArtifactArcCandidate previous = candidates.get(index - 1);
            ArtifactArcCandidate current = candidates.get(index);
            assertTrue(
                    previous.getPeakDominanceRatio()
                            >= current.getPeakDominanceRatio() - TOLERANCE);
        }
    }

    @Test
    public void smallerAngleDegreesUsesAcuteOrObtuseSmallerAngle() {

        assertEquals(
                90.0,
                ArtifactArcCandidateFinder.smallerAngleDegrees(1.0, 0.0, 0.0, 1.0),
                TOLERANCE);
        assertEquals(
                0.0,
                ArtifactArcCandidateFinder.smallerAngleDegrees(1.0, 0.0, 2.0, 0.0),
                TOLERANCE);
        assertEquals(
                180.0,
                ArtifactArcCandidateFinder.smallerAngleDegrees(1.0, 0.0, -1.0, 0.0),
                TOLERANCE);
    }

    private ArtifactArcCandidate selectSinglePeakedCandidate() {

        List<Point> boundary = singlePeakedArcBoundary();
        double[] scores = zeroScores(boundary.size());
        return findCandidate(
                finder.findCandidates(
                        boundary,
                        scores,
                        2,
                        20,
                        1.0,
                        100.0,
                        1.0,
                        1.05,
                        1.01,
                        2,
                        2.0),
                0,
                5,
                10);
    }

    private static double euclideanDistance(Point left, Point right) {

        double deltaX = left.x - right.x;
        double deltaY = left.y - right.y;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    private static List<Point> singlePeakedArcBoundary() {

        List<Point> boundary = new ArrayList<>(Arrays.asList(
                p(0, 0),
                p(2, 0),
                p(4, 2),
                p(6, 5),
                p(8, 7),
                p(10, 8),
                p(12, 7),
                p(14, 5),
                p(16, 2),
                p(18, 0),
                p(20, 0)));

        for (int index = 11; index <= 30; index++) {
            boundary.add(p(30 - index, -4));
        }

        return boundary;
    }

    private static List<Point> noisySinglePeakedArcBoundary() {

        List<Point> boundary = new ArrayList<>(Arrays.asList(
                p(0, 0),
                p(2, 0),
                p(4, 1),
                p(6, 4),
                p(7, 6),
                p(8, 7),
                p(10, 8),
                p(12, 7),
                p(14, 5),
                p(16, 2),
                p(18, 0),
                p(20, 0)));

        for (int index = 12; index <= 31; index++) {
            boundary.add(p(31 - index, -4));
        }

        return boundary;
    }

    private static List<Point> riseViolationBoundary() {
        return Arrays.asList(
                p(0, 0),
                p(2, 0),
                p(4, 4),
                p(6, 2),
                p(8, 8),
                p(10, 6),
                p(12, 2),
                p(14, 0),
                p(16, 0));
    }

    private static List<Point> fallViolationBoundary() {
        return Arrays.asList(
                p(0, 0),
                p(2, 0),
                p(4, 2),
                p(6, 6),
                p(8, 8),
                p(10, 6),
                p(12, 7),
                p(14, 0),
                p(16, 0));
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

    private static List<Point> wrapAroundBoundary() {
        return Arrays.asList(
                p(0, 0),
                p(1, 0),
                p(2, 0),
                p(2, 1),
                p(2, 2),
                p(1, 2),
                p(0, 2),
                p(0, 1),
                p(0, 3),
                p(0, 4),
                p(1, 4),
                p(2, 4));
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

    private static double[] zeroScores(int boundarySize) {
        return new double[boundarySize];
    }

    private static double[] peakScores(
            int boundarySize,
            List<Integer> peakIndices,
            double firstScore,
            double peakScore,
            double secondScore) {

        double[] scores = new double[boundarySize];
        scores[peakIndices.get(0)] = firstScore;
        scores[peakIndices.get(1)] = peakScore;
        scores[peakIndices.get(2)] = secondScore;
        return scores;
    }

    private static java.util.Optional<ArtifactArcCandidate> findCandidateOptional(
            List<ArtifactArcCandidate> candidates,
            int firstBoundaryIndex,
            int peakBoundaryIndex,
            int secondBoundaryIndex) {

        for (ArtifactArcCandidate candidate : candidates) {

            if (candidate.getFirstBoundaryIndex() == firstBoundaryIndex
                    && candidate.getPeakBoundaryIndex() == peakBoundaryIndex
                    && candidate.getSecondBoundaryIndex() == secondBoundaryIndex) {
                return java.util.Optional.of(candidate);
            }
        }

        return java.util.Optional.empty();
    }

    private static ArtifactArcCandidate findCandidate(
            List<ArtifactArcCandidate> candidates,
            int firstBoundaryIndex,
            int peakBoundaryIndex,
            int secondBoundaryIndex) {

        return findCandidateOptional(
                candidates,
                firstBoundaryIndex,
                peakBoundaryIndex,
                secondBoundaryIndex)
                .orElseThrow(() -> new AssertionError("Expected candidate not found."));
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

    private static void expectIllegalArgumentMessage(
            String expectedMessage,
            Runnable action) {

        try {
            action.run();
        } catch (IllegalArgumentException exception) {
            assertEquals(expectedMessage, exception.getMessage());
            return;
        }

        throw new AssertionError(
                "Expected IllegalArgumentException with message: "
                        + expectedMessage);
    }

    private static void expectIllegalArgumentStartsWith(
            String expectedPrefix,
            Runnable action) {

        try {
            action.run();
        } catch (IllegalArgumentException exception) {
            assertTrue(
                    exception.getMessage().startsWith(expectedPrefix));
            return;
        }

        throw new AssertionError(
                "Expected IllegalArgumentException starting with: "
                        + expectedPrefix);
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
}
