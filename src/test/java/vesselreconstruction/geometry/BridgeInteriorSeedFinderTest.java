package vesselreconstruction.geometry;

import org.junit.Test;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class BridgeInteriorSeedFinderTest {

    private final BridgeInteriorSeedFinder seedFinder = new BridgeInteriorSeedFinder();

    @Test
    public void maximumMinimumDistancePixelIsSelected() {

        List<Point> lumenPixels = Arrays.asList(
                p(0, 0),
                p(10, 0),
                p(5, 10));

        Point seed = seedFinder.findSeed(lumenPixels, p(0, 0), p(10, 0));

        assertEquals(new Point(5, 10), seed);
    }

    @Test
    public void unequalDistancesUseSmallerDistanceAsScore() {

        List<Point> lumenPixels = Arrays.asList(
                p(0, 0),
                p(10, 0),
                p(4, 1));

        Point seed = seedFinder.findSeed(lumenPixels, p(0, 0), p(10, 0));

        assertEquals(new Point(4, 1), seed);
    }

    @Test
    public void smallestYBreaksTie() {

        List<Point> lumenPixels = Arrays.asList(
                p(0, 0),
                p(0, 4),
                p(5, 1),
                p(5, 3));

        Point seed = seedFinder.findSeed(lumenPixels, p(0, 0), p(0, 4));

        assertEquals(new Point(5, 1), seed);
    }

    @Test
    public void smallestXBreaksRemainingTie() {

        List<Point> lumenPixels = Arrays.asList(
                p(0, 0),
                p(10, 0),
                p(4, 5),
                p(6, 5));

        Point seed = seedFinder.findSeed(lumenPixels, p(0, 0), p(10, 0));

        assertEquals(new Point(4, 5), seed);
    }

    @Test
    public void returnedPointIsDefensiveCopy() {

        List<Point> lumenPixels = Arrays.asList(p(0, 0), p(10, 0), p(5, 5));
        Point seed = seedFinder.findSeed(lumenPixels, p(0, 0), p(10, 0));

        seed.translate(3, 3);

        Point secondSeed = seedFinder.findSeed(lumenPixels, p(0, 0), p(10, 0));

        assertEquals(new Point(5, 5), secondSeed);
        assertNotSame(seed, secondSeed);
    }

    @Test
    public void inputsRemainUnchanged() {

        List<Point> lumenPixels = new ArrayList<>(Arrays.asList(
                p(0, 0),
                p(10, 0),
                p(5, 5)));
        Point first = p(0, 0);
        Point second = p(10, 0);
        Point firstSnapshot = new Point(first);
        Point secondSnapshot = new Point(second);

        seedFinder.findSeed(lumenPixels, first, second);

        assertEquals(firstSnapshot, first);
        assertEquals(secondSnapshot, second);
        assertEquals(Arrays.asList(p(0, 0), p(10, 0), p(5, 5)), lumenPixels);
    }

    @Test
    public void nullLumenListRejected() {
        expectIllegalArgument(
                () -> seedFinder.findSeed(null, p(0, 0), p(1, 0)),
                "lumenPixels must not be null.");
    }

    @Test
    public void emptyLumenListRejected() {
        expectIllegalArgument(
                () -> seedFinder.findSeed(new ArrayList<Point>(), p(0, 0), p(1, 0)),
                "lumenPixels must not be empty.");
    }

    @Test
    public void nullLumenPixelRejected() {

        List<Point> lumenPixels = new ArrayList<>();
        lumenPixels.add(p(0, 0));
        lumenPixels.add(null);

        expectIllegalArgument(
                () -> seedFinder.findSeed(lumenPixels, p(0, 0), p(0, 0)),
                "lumenPixels must not contain null values.");
    }

    @Test
    public void duplicateLumenCoordinateRejected() {
        expectIllegalArgument(
                () -> seedFinder.findSeed(
                        Arrays.asList(p(0, 0), p(0, 0)),
                        p(0, 0),
                        p(0, 0)),
                "lumenPixels must not contain duplicate coordinates.");
    }

    @Test
    public void nullBridgePointRejected() {
        expectIllegalArgument(
                () -> seedFinder.findSeed(
                        Arrays.asList(p(0, 0), p(1, 0)),
                        null,
                        p(1, 0)),
                "bridge points must not be null.");
    }

    @Test
    public void bridgePointOutsideLumenRejected() {
        expectIllegalArgument(
                () -> seedFinder.findSeed(
                        Arrays.asList(p(0, 0), p(1, 0)),
                        p(9, 9),
                        p(1, 0)),
                "firstBridgePoint must be contained in lumenPixels.");
    }

    private static Point p(int x, int y) {
        return new Point(x, y);
    }

    private static void expectIllegalArgument(Runnable action, String message) {

        try {
            action.run();
        } catch (IllegalArgumentException exception) {
            assertEquals(message, exception.getMessage());
            return;
        }

        throw new AssertionError("Expected IllegalArgumentException.");
    }
}
