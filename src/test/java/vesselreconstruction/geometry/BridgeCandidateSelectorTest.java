package vesselreconstruction.geometry;

import org.junit.Test;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BridgeCandidateSelectorTest {

    private final BridgeCandidateSelector selector = new BridgeCandidateSelector();

    @Test
    public void emptyListReturnsOptionalEmpty() {

        Optional<BridgeCandidate> selected = selector.selectBest(
                new ArrayList<BridgeCandidate>());

        assertFalse(selected.isPresent());
    }

    @Test
    public void oneCandidateIsSelected() {

        BridgeCandidate candidate = candidate(0, 10, p(0, 0), p(5, 0), 5.0, 10);

        Optional<BridgeCandidate> selected = selector.selectBest(
                Collections.singletonList(candidate));

        assertTrue(selected.isPresent());
        assertSame(candidate, selected.get());
    }

    @Test
    public void shortestDistanceWins() {

        BridgeCandidate farther = candidate(0, 10, p(0, 0), p(10, 0), 10.0, 10);
        BridgeCandidate closer = candidate(1, 11, p(1, 0), p(6, 0), 5.0, 10);

        Optional<BridgeCandidate> selected = selector.selectBest(
                Arrays.asList(farther, closer));

        assertSame(closer, selected.get());
    }

    @Test
    public void greaterCircularSeparationBreaksEqualDistanceTie() {

        BridgeCandidate lowerSeparation = candidate(
                0, 10, p(0, 0), p(5, 0), 5.0, 8);
        BridgeCandidate higherSeparation = candidate(
                2, 12, p(2, 0), p(7, 0), 5.0, 10);

        Optional<BridgeCandidate> selected = selector.selectBest(
                Arrays.asList(lowerSeparation, higherSeparation));

        assertSame(higherSeparation, selected.get());
    }

    @Test
    public void lowerFirstIndexBreaksNextTie() {

        BridgeCandidate laterFirstIndex = candidate(
                5, 15, p(5, 0), p(10, 0), 5.0, 10);
        BridgeCandidate earlierFirstIndex = candidate(
                1, 11, p(1, 0), p(6, 0), 5.0, 10);

        Optional<BridgeCandidate> selected = selector.selectBest(
                Arrays.asList(laterFirstIndex, earlierFirstIndex));

        assertSame(earlierFirstIndex, selected.get());
    }

    @Test
    public void lowerSecondIndexBreaksFinalTie() {

        BridgeCandidate laterSecondIndex = candidate(
                1, 12, p(1, 0), p(7, 0), 5.0, 10);
        BridgeCandidate earlierSecondIndex = candidate(
                1, 11, p(1, 0), p(6, 0), 5.0, 10);

        Optional<BridgeCandidate> selected = selector.selectBest(
                Arrays.asList(laterSecondIndex, earlierSecondIndex));

        assertSame(earlierSecondIndex, selected.get());
    }

    @Test
    public void selectorDoesNotDependOnInputOrdering() {

        BridgeCandidate best = candidate(1, 11, p(1, 0), p(6, 0), 4.0, 12);
        BridgeCandidate middle = candidate(2, 12, p(2, 0), p(8, 0), 6.0, 10);
        BridgeCandidate worst = candidate(0, 10, p(0, 0), p(10, 0), 10.0, 10);

        Optional<BridgeCandidate> selectedFromShuffledOrder = selector.selectBest(
                Arrays.asList(worst, best, middle));
        Optional<BridgeCandidate> selectedFromSortedOrder = selector.selectBest(
                Arrays.asList(middle, worst, best));

        assertSame(best, selectedFromShuffledOrder.get());
        assertSame(best, selectedFromSortedOrder.get());
    }

    @Test
    public void inputListRemainsUnchanged() {

        List<BridgeCandidate> candidates = new ArrayList<>(Arrays.asList(
                candidate(0, 10, p(0, 0), p(10, 0), 10.0, 10),
                candidate(1, 11, p(1, 0), p(6, 0), 5.0, 10)));
        BridgeCandidate first = candidates.get(0);
        BridgeCandidate second = candidates.get(1);

        selector.selectBest(candidates);

        assertEquals(2, candidates.size());
        assertSame(first, candidates.get(0));
        assertSame(second, candidates.get(1));
    }

    @Test
    public void nullListRejected() {

        try {
            selector.selectBest(null);
        } catch (IllegalArgumentException exception) {
            assertEquals("candidates must not be null.", exception.getMessage());
            return;
        }

        throw new AssertionError("Expected IllegalArgumentException.");
    }

    @Test
    public void nullCandidateRejected() {

        List<BridgeCandidate> candidates = new ArrayList<>();
        candidates.add(candidate(0, 10, p(0, 0), p(5, 0), 5.0, 10));
        candidates.add(null);

        try {
            selector.selectBest(candidates);
        } catch (IllegalArgumentException exception) {
            assertEquals(
                    "candidates must not contain null values.",
                    exception.getMessage());
            return;
        }

        throw new AssertionError("Expected IllegalArgumentException.");
    }

    private static BridgeCandidate candidate(
            int firstBoundaryIndex,
            int secondBoundaryIndex,
            Point firstPoint,
            Point secondPoint,
            double euclideanDistance,
            int circularBoundarySeparation) {

        return new BridgeCandidate(
                firstBoundaryIndex,
                secondBoundaryIndex,
                firstPoint,
                secondPoint,
                euclideanDistance,
                circularBoundarySeparation);
    }

    private static Point p(int x, int y) {
        return new Point(x, y);
    }
}
