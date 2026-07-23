package vesselreconstruction.geometry;

import org.junit.Test;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BridgeCandidateFinderTest {

    private static final double TOLERANCE = 1.0e-9;

    private final BridgeCandidateFinder finder = new BridgeCandidateFinder();

    @Test
    public void spatiallyCloseAndContourDistantPeaksProduceCandidate() {

        List<Point> boundary = dumbbellBoundary();
        List<Integer> peakIndices = Arrays.asList(0, 6);

        List<BridgeCandidate> candidates = finder.findCandidates(
                boundary,
                peakIndices,
                4,
                5.0);

        assertEquals(1, candidates.size());
        assertEquals(0, candidates.get(0).getFirstBoundaryIndex());
        assertEquals(6, candidates.get(0).getSecondBoundaryIndex());
        assertPointEquals(p(0, 0), candidates.get(0).getFirstPoint());
        assertPointEquals(p(4, 2), candidates.get(0).getSecondPoint());
        assertEquals(6, candidates.get(0).getCircularBoundarySeparation());
        assertEquals(
                Math.sqrt(20.0),
                candidates.get(0).getEuclideanDistance(),
                TOLERANCE);
    }

    @Test
    public void spatiallyCloseButContourNearPeaksAreRejected() {

        List<BridgeCandidate> candidates = finder.findCandidates(
                dumbbellBoundary(),
                Arrays.asList(0, 1),
                2,
                10.0);

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void contourDistantButSpatiallyTooFarPeaksAreRejected() {

        List<BridgeCandidate> candidates = finder.findCandidates(
                dumbbellBoundary(),
                Arrays.asList(0, 6),
                4,
                3.0);

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void circularContourSeparationWorksAcrossIndexZero() {

        List<Point> boundary = squareBoundary();
        List<Integer> peakIndices = Arrays.asList(1, 7);

        List<BridgeCandidate> candidates = finder.findCandidates(
                boundary,
                peakIndices,
                2,
                6.0);

        assertEquals(1, candidates.size());
        assertEquals(2, candidates.get(0).getCircularBoundarySeparation());
        assertPointEquals(p(4, 0), candidates.get(0).getFirstPoint());
        assertPointEquals(p(0, 4), candidates.get(0).getSecondPoint());
    }

    @Test
    public void exactMaximumBridgeLengthIsIncluded() {

        List<BridgeCandidate> candidates = finder.findCandidates(
                dumbbellBoundary(),
                Arrays.asList(1, 5),
                3,
                Math.sqrt(10.0));

        assertEquals(1, candidates.size());
        assertEquals(
                Math.sqrt(10.0),
                candidates.get(0).getEuclideanDistance(),
                TOLERANCE);
    }

    @Test
    public void exactMinimumCircularBoundarySeparationIsIncluded() {

        List<BridgeCandidate> candidates = finder.findCandidates(
                squareBoundary(),
                Arrays.asList(1, 7),
                2,
                6.0);

        assertEquals(1, candidates.size());
        assertEquals(2, candidates.get(0).getCircularBoundarySeparation());
    }

    @Test
    public void duplicatePeakIndicesDoNotCreateDuplicateCandidates() {

        List<BridgeCandidate> candidates = finder.findCandidates(
                dumbbellBoundary(),
                Arrays.asList(0, 6, 0, 6),
                4,
                5.0);

        assertEquals(1, candidates.size());
    }

    @Test
    public void candidateIndicesAndPointsUseCanonicalOrdering() {

        BridgeCandidate candidate = BridgeCandidate.createCanonical(
                6,
                0,
                p(4, 2),
                p(0, 0),
                Math.sqrt(20.0),
                6);

        assertEquals(0, candidate.getFirstBoundaryIndex());
        assertEquals(6, candidate.getSecondBoundaryIndex());
        assertPointEquals(p(0, 0), candidate.getFirstPoint());
        assertPointEquals(p(4, 2), candidate.getSecondPoint());
    }

    @Test
    public void deterministicCandidateSorting() {

        List<BridgeCandidate> candidates = finder.findCandidates(
                dumbbellBoundary(),
                Arrays.asList(0, 1, 5, 6),
                3,
                10.0);

        assertTrue(candidates.size() >= 2);
        assertTrue(
                candidates.get(0).getEuclideanDistance()
                        <= candidates.get(1).getEuclideanDistance());

        for (int index = 1; index < candidates.size(); index++) {
            BridgeCandidate previous = candidates.get(index - 1);
            BridgeCandidate current = candidates.get(index);

            assertTrue(
                    previous.getEuclideanDistance()
                            < current.getEuclideanDistance()
                            || (Math.abs(previous.getEuclideanDistance()
                                    - current.getEuclideanDistance()) <= TOLERANCE
                            && (previous.getCircularBoundarySeparation()
                                    > current.getCircularBoundarySeparation()
                                    || (previous.getCircularBoundarySeparation()
                                            == current.getCircularBoundarySeparation()
                                            && (previous.getFirstBoundaryIndex()
                                                    < current.getFirstBoundaryIndex()
                                                    || (previous.getFirstBoundaryIndex()
                                                            == current.getFirstBoundaryIndex()
                                                            && previous.getSecondBoundaryIndex()
                                                                    <= current.getSecondBoundaryIndex()))))));
        }
    }

    @Test
    public void emptyPeakListReturnsEmpty() {

        List<BridgeCandidate> candidates = finder.findCandidates(
                squareBoundary(),
                new ArrayList<Integer>(),
                2,
                10.0);

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void onePeakReturnsEmpty() {

        List<BridgeCandidate> candidates = finder.findCandidates(
                squareBoundary(),
                Arrays.asList(2),
                2,
                10.0);

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void returnedListIsUnmodifiable() {

        List<BridgeCandidate> candidates = finder.findCandidates(
                dumbbellBoundary(),
                Arrays.asList(0, 6),
                4,
                5.0);

        try {
            candidates.add(candidates.get(0));
        } catch (UnsupportedOperationException exception) {
            return;
        }

        throw new AssertionError("Expected UnsupportedOperationException.");
    }

    @Test
    public void inputListsAndPointsRemainUnchanged() {

        List<Point> boundary = mutableCopy(dumbbellBoundary());
        int[] boundaryX = boundary.stream().mapToInt(point -> point.x).toArray();
        int[] boundaryY = boundary.stream().mapToInt(point -> point.y).toArray();
        List<Integer> peakIndices = new ArrayList<>(Arrays.asList(0, 6));

        finder.findCandidates(boundary, peakIndices, 4, 5.0);

        assertEquals(2, peakIndices.size());
        assertEquals(Integer.valueOf(0), peakIndices.get(0));
        assertEquals(Integer.valueOf(6), peakIndices.get(1));

        for (int index = 0; index < boundary.size(); index++) {
            assertEquals(boundaryX[index], boundary.get(index).x);
            assertEquals(boundaryY[index], boundary.get(index).y);
        }
    }

    @Test
    public void nullBoundaryRejected() {

        assertInvalid(
                null,
                Arrays.asList(0, 1),
                2,
                5.0,
                "boundaryPoints must not be null.");
    }

    @Test
    public void nullBoundaryPointRejected() {

        List<Point> boundary = dumbbellBoundary();
        boundary.set(2, null);

        assertInvalid(
                boundary,
                Arrays.asList(0, 6),
                4,
                5.0,
                "boundaryPoints must not contain null points.");
    }

    @Test
    public void nullPeakListRejected() {

        assertInvalid(
                dumbbellBoundary(),
                null,
                4,
                5.0,
                "peakIndices must not be null.");
    }

    @Test
    public void nullPeakIndexRejected() {

        List<Integer> peakIndices = new ArrayList<>();
        peakIndices.add(0);
        peakIndices.add(null);

        assertInvalid(
                dumbbellBoundary(),
                peakIndices,
                4,
                5.0,
                "peakIndices must not contain null values.");
    }

    @Test
    public void outOfRangeNegativePeakIndexRejected() {

        assertInvalid(
                dumbbellBoundary(),
                Arrays.asList(-1, 6),
                4,
                5.0,
                "peakIndices must be within [0, 11].");
    }

    @Test
    public void outOfRangeHighPeakIndexRejected() {

        assertInvalid(
                dumbbellBoundary(),
                Arrays.asList(0, 12),
                4,
                5.0,
                "peakIndices must be within [0, 11].");
    }

    @Test
    public void invalidMinimumSeparationRejected() {

        assertInvalid(
                dumbbellBoundary(),
                Arrays.asList(0, 6),
                0,
                5.0,
                "minimumCircularBoundarySeparation must be at least 1.");

        assertInvalid(
                dumbbellBoundary(),
                Arrays.asList(0, 6),
                7,
                5.0,
                "minimumCircularBoundarySeparation must be at most 6.");
    }

    @Test
    public void nonFiniteMaximumLengthRejected() {

        assertInvalid(
                dumbbellBoundary(),
                Arrays.asList(0, 6),
                4,
                Double.NaN,
                "maximumBridgeLength must be finite.");
    }

    @Test
    public void negativeMaximumLengthRejected() {

        assertInvalid(
                dumbbellBoundary(),
                Arrays.asList(0, 6),
                4,
                -1.0,
                "maximumBridgeLength must be non-negative.");
    }

    private static List<Point> dumbbellBoundary() {

        return points(
                p(0, 0),
                p(1, 0),
                p(2, 0),
                p(3, 0),
                p(4, 0),
                p(4, 1),
                p(4, 2),
                p(3, 2),
                p(2, 2),
                p(1, 2),
                p(0, 2),
                p(0, 1));
    }

    private static List<Point> squareBoundary() {

        return points(
                p(0, 0),
                p(4, 0),
                p(8, 0),
                p(8, 4),
                p(8, 8),
                p(4, 8),
                p(0, 8),
                p(0, 4));
    }

    private static List<Point> mutableCopy(List<Point> boundary) {

        List<Point> copy = new ArrayList<>();

        for (Point point : boundary) {
            copy.add(new Point(point.x, point.y));
        }

        return copy;
    }

    private void assertInvalid(
            List<Point> boundary,
            List<Integer> peakIndices,
            int minimumCircularBoundarySeparation,
            double maximumBridgeLength,
            String expectedMessage) {

        try {
            finder.findCandidates(
                    boundary,
                    peakIndices,
                    minimumCircularBoundarySeparation,
                    maximumBridgeLength);
        } catch (IllegalArgumentException exception) {
            assertEquals(expectedMessage, exception.getMessage());
            return;
        }

        throw new AssertionError("Expected IllegalArgumentException.");
    }

    private static List<Point> points(Point... points) {
        return new ArrayList<>(Arrays.asList(points));
    }

    private static Point p(int x, int y) {
        return new Point(x, y);
    }

    private static void assertPointEquals(Point expected, Point actual) {

        assertEquals(
                expected.x + "," + expected.y,
                actual.x + "," + actual.y);
    }
}
