package vesselreconstruction.geometry;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeviationPeakFinderTest {

    private final DeviationPeakFinder peakFinder = new DeviationPeakFinder();

    @Test
    public void oneIsolatedPeakIsDetected() {

        double[] scores = {1.0, 1.0, 10.0, 1.0, 1.0};

        List<Integer> peaks = peakFinder.findPeakIndices(scores, 0.5, 1);

        assertEquals(Arrays.asList(2), peaks);
    }

    @Test
    public void multipleSeparatedPeaksAreDetected() {

        double[] scores = {10.0, 1.0, 1.0, 1.0, 10.0, 1.0, 1.0, 1.0, 1.0};

        List<Integer> peaks = peakFinder.findPeakIndices(scores, 0.5, 1);

        assertEquals(Arrays.asList(0, 4), peaks);
    }

    @Test
    public void nearbyPeaksAreSuppressed() {

        double[] scores = {10.0, 8.0, 1.0, 1.0, 1.0};

        List<Integer> peaks = peakFinder.findPeakIndices(scores, 0.5, 1);

        assertEquals(Arrays.asList(0), peaks);
    }

    @Test
    public void higherNearbyPeakWins() {

        double[] scores = {4.0, 10.0, 6.0, 1.0, 1.0};

        List<Integer> peaks = peakFinder.findPeakIndices(scores, 0.5, 1);

        assertEquals(Arrays.asList(1), peaks);
    }

    @Test
    public void equalNearbyPeaksChooseLowestIndex() {

        double[] scores = {10.0, 10.0, 1.0, 1.0, 1.0};

        List<Integer> peaks = peakFinder.findPeakIndices(scores, 0.5, 1);

        assertEquals(Arrays.asList(0), peaks);
    }

    @Test
    public void circularSuppressionWorksAcrossBoundaryEnds() {

        double[] scores = {10.0, 1.0, 1.0, 1.0, 8.0};

        List<Integer> peaks = peakFinder.findPeakIndices(scores, 0.5, 2);

        assertEquals(Arrays.asList(0), peaks);
    }

    @Test
    public void thresholdInclusionAtExactEquality() {

        double[] scores = {1.0, 1.0, 2.5, 1.0, 1.0};

        List<Integer> peaks = peakFinder.findPeakIndices(scores, 1.0, 1);

        assertEquals(Arrays.asList(2), peaks);
    }

    @Test
    public void belowThresholdPointsAreExcluded() {

        double[] scores = {10.0, 1.0, 4.0, 1.0, 1.0};

        List<Integer> peaks = peakFinder.findPeakIndices(scores, 0.5, 1);

        assertEquals(Arrays.asList(0), peaks);
    }

    @Test
    public void allZeroArrayReturnsEmpty() {

        double[] scores = {0.0, 0.0, 0.0, 0.0, 0.0};

        List<Integer> peaks = peakFinder.findPeakIndices(scores, 0.5, 1);

        assertTrue(peaks.isEmpty());
    }

    @Test
    public void emptyArrayReturnsEmpty() {

        List<Integer> peaks = peakFinder.findPeakIndices(
                new double[0],
                0.5,
                1);

        assertTrue(peaks.isEmpty());
    }

    @Test
    public void returnedIndicesAreAscending() {

        double[] scores = {10.0, 1.0, 1.0, 10.0, 1.0, 1.0, 10.0, 1.0, 1.0};

        List<Integer> peaks = peakFinder.findPeakIndices(scores, 0.5, 1);

        assertEquals(Arrays.asList(0, 3, 6), peaks);
    }

    @Test
    public void inputArrayRemainsUnchanged() {

        double[] scores = {1.0, 10.0, 1.0, 1.0, 1.0};
        double[] snapshot = scores.clone();

        peakFinder.findPeakIndices(scores, 0.5, 1);

        assertArrayEquals(snapshot, scores, 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullInputRejected() {

        peakFinder.findPeakIndices(null, 0.5, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nanScoreRejected() {

        peakFinder.findPeakIndices(
                new double[] {1.0, Double.NaN, 1.0, 1.0, 1.0},
                0.5,
                1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void infinityScoreRejected() {

        peakFinder.findPeakIndices(
                new double[] {1.0, Double.POSITIVE_INFINITY, 1.0, 1.0, 1.0},
                0.5,
                1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeScoreRejected() {

        peakFinder.findPeakIndices(
                new double[] {1.0, -1.0, 1.0, 1.0, 1.0},
                0.5,
                1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidRelativeThresholdRejected() {

        peakFinder.findPeakIndices(
                new double[] {1.0, 2.0, 1.0, 1.0, 1.0},
                1.5,
                1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidSuppressionRadiusRejected() {

        peakFinder.findPeakIndices(
                new double[] {1.0, 2.0, 1.0, 1.0, 1.0},
                0.5,
                0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insufficientNonEmptyArrayLengthRejected() {

        peakFinder.findPeakIndices(
                new double[] {1.0, 2.0, 1.0},
                0.5,
                2);
    }
}
