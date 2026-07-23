package vesselreconstruction.reconstruction;

import org.junit.Test;

import java.awt.Point;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class BridgeEnclosedWoodFinderTest {

    private final BridgeEnclosedWoodFinder finder = new BridgeEnclosedWoodFinder();

    @Test
    public void exteriorFloodFillTraversesNonWoodCoordinates() {

        List<Point> lumenPixels = cupLumen();
        Set<Point> woodPixels = new HashSet<>(Collections.singletonList(p(2, 0)));

        Set<Point> enclosed = finder.findEnclosedWood(
                lumenPixels,
                woodPixels,
                p(1, 1),
                p(3, 1),
                0);

        assertTrue(enclosed.isEmpty());
    }

    @Test
    public void otherTissueClassesDoNotCreateFalseEnclosedRegions() {

        List<Point> lumenPixels = cupLumen();
        Set<Point> woodPixels = new HashSet<>(Collections.singletonList(p(2, 0)));

        Set<Point> enclosed = finder.findEnclosedWood(
                lumenPixels,
                woodPixels,
                p(1, 1),
                p(3, 1),
                0);

        assertFalse(enclosed.contains(p(2, 0)));
    }

    @Test
    public void exteriorWoodConnectedToGridEdgeThroughNonWoodSpaceIsExcluded() {

        List<Point> lumenPixels = cupLumen();
        Set<Point> woodPixels = new HashSet<>(Arrays.asList(
                p(2, 2),
                p(5, 2)));

        Set<Point> enclosed = finder.findEnclosedWood(
                lumenPixels,
                woodPixels,
                p(1, 1),
                p(3, 1),
                0);

        assertTrue(enclosed.contains(p(2, 2)));
        assertFalse(enclosed.contains(p(5, 2)));
    }

    @Test
    public void genuinelyBridgeEnclosedWoodIsIncluded() {

        List<Point> lumenPixels = cupLumen();
        Set<Point> woodPixels = new HashSet<>(Collections.singletonList(p(2, 2)));

        Set<Point> enclosed = finder.findEnclosedWood(
                lumenPixels,
                woodPixels,
                p(1, 1),
                p(3, 1),
                0);

        assertTrue(enclosed.contains(p(2, 2)));
    }

    @Test
    public void removingBridgeLineMakesRegionExterior() {

        List<Point> lumenPixels = cupLumen();
        Set<Point> woodPixels = new HashSet<>(Collections.singletonList(p(2, 2)));

        Set<Point> enclosedWithBridge = finder.findEnclosedWood(
                lumenPixels,
                woodPixels,
                p(1, 1),
                p(3, 1),
                0);
        Set<Point> enclosedWithoutClosingBridge = finder.findEnclosedWood(
                lumenPixels,
                woodPixels,
                p(1, 1),
                p(1, 2),
                0);

        assertTrue(enclosedWithBridge.contains(p(2, 2)));
        assertTrue(enclosedWithoutClosingBridge.isEmpty());
    }

    @Test
    public void diagonalExteriorTravelUsesEightConnectivity() {

        List<Point> lumenPixels = sorted(new HashSet<>(Arrays.asList(
                p(1, 1),
                p(1, 2))));
        Set<Point> woodPixels = new HashSet<>(Collections.singletonList(p(2, 2)));

        Set<Point> enclosed = finder.findEnclosedWood(
                lumenPixels,
                woodPixels,
                p(1, 1),
                p(1, 2),
                0);

        assertTrue(enclosed.isEmpty());
    }

    @Test
    public void onlyLumenPixelsAndBridgeLineAreBarriers() {

        List<Point> lumenPixels = cupLumen();
        Set<Point> woodPixels = new HashSet<>(Arrays.asList(
                p(2, 2),
                p(0, 2)));

        Set<Point> enclosed = finder.findEnclosedWood(
                lumenPixels,
                woodPixels,
                p(1, 1),
                p(3, 1),
                0);

        assertTrue(enclosed.contains(p(2, 2)));
        assertFalse(enclosed.contains(p(0, 2)));
    }

    @Test
    public void woodConnectedInsideEnclosedRegionIsIncluded() {

        List<Point> lumenPixels = tallCupLumen();
        Set<Point> woodPixels = new HashSet<>(Arrays.asList(
                p(2, 2),
                p(2, 3)));

        Set<Point> enclosed = finder.findEnclosedWood(
                lumenPixels,
                woodPixels,
                p(1, 1),
                p(3, 1),
                0);

        assertTrue(enclosed.contains(p(2, 2)));
        assertTrue(enclosed.contains(p(2, 3)));
    }

    @Test
    public void woodTouchingExteriorSideOfLumenIsExcluded() {

        List<Point> lumenPixels = cupLumen();
        Set<Point> woodPixels = new HashSet<>(Arrays.asList(
                p(2, 2),
                p(0, 3),
                p(4, 3)));

        Set<Point> enclosed = finder.findEnclosedWood(
                lumenPixels,
                woodPixels,
                p(1, 1),
                p(3, 1),
                0);

        assertTrue(enclosed.contains(p(2, 2)));
        assertFalse(enclosed.contains(p(0, 3)));
        assertFalse(enclosed.contains(p(4, 3)));
    }

    @Test
    public void noEnclosedWoodWhenAllowedWoodIsExteriorOnly() {

        List<Point> lumenPixels = cupLumen();

        Set<Point> enclosed = finder.findEnclosedWood(
                lumenPixels,
                Collections.singleton(p(2, 0)),
                p(1, 1),
                p(3, 1),
                0);

        assertTrue(enclosed.isEmpty());
    }

    @Test
    public void returnedSetIsDefensive() {

        List<Point> lumenPixels = cupLumen();
        Set<Point> woodPixels = new HashSet<>(Collections.singletonList(p(2, 2)));

        Set<Point> enclosed = finder.findEnclosedWood(
                lumenPixels,
                woodPixels,
                p(1, 1),
                p(3, 1),
                0);

        Point first = enclosed.iterator().next();
        first.translate(5, 5);

        Set<Point> second = finder.findEnclosedWood(
                lumenPixels,
                woodPixels,
                p(1, 1),
                p(3, 1),
                0);

        assertNotSame(first, second.iterator().next());
    }

    @Test
    public void inputsAreNotMutated() {

        List<Point> lumenPixels = cupLumen();
        Set<Point> woodPixels = new HashSet<>(Collections.singletonList(p(2, 2)));
        int originalWoodSize = woodPixels.size();

        finder.findEnclosedWood(
                lumenPixels,
                woodPixels,
                p(1, 1),
                p(3, 1),
                0);

        assertEquals(7, lumenPixels.size());
        assertEquals(originalWoodSize, woodPixels.size());
    }

    @Test
    public void nullLumenInputRejected() {
        expectIllegalArgument(
                () -> finder.findEnclosedWood(
                        null,
                        Collections.<Point>emptySet(),
                        p(0, 0),
                        p(1, 0),
                        0),
                "originalLumenPixels must not be null.");
    }

    @Test
    public void emptyLumenInputRejected() {
        expectIllegalArgument(
                () -> finder.findEnclosedWood(
                        Collections.<Point>emptyList(),
                        Collections.<Point>emptySet(),
                        p(0, 0),
                        p(1, 0),
                        0),
                "originalLumenPixels must not be empty.");
    }

    @Test
    public void nullAllowedWoodSetRejected() {
        expectIllegalArgument(
                () -> finder.findEnclosedWood(
                        cupLumen(),
                        null,
                        p(1, 1),
                        p(3, 1),
                        0),
                "allowedWoodPixels must not be null.");
    }

    @Test
    public void overlapBetweenLumenAndAllowedWoodRejected() {

        Set<Point> woodPixels = new HashSet<>(Collections.singletonList(p(1, 1)));

        expectIllegalArgument(
                () -> finder.findEnclosedWood(
                        cupLumen(),
                        woodPixels,
                        p(1, 1),
                        p(3, 1),
                        0),
                "allowedWoodPixels must not overlap originalLumenPixels.");
    }

    @Test
    public void endpointOutsideLumenRejected() {
        expectIllegalArgument(
                () -> finder.findEnclosedWood(
                        cupLumen(),
                        Collections.<Point>emptySet(),
                        p(99, 99),
                        p(3, 1),
                        0),
                "firstBridgePoint must be contained in originalLumenPixels.");
    }

    @Test
    public void negativeOpeningWidthRejected() {
        expectIllegalArgument(
                () -> finder.findEnclosedWood(
                        cupLumen(),
                        Collections.<Point>emptySet(),
                        p(1, 1),
                        p(3, 1),
                        -1),
                "bridgeOpeningHalfWidth must not be negative.");
    }

    private static List<Point> tallCupLumen() {

        return sorted(new HashSet<>(Arrays.asList(
                p(1, 1),
                p(3, 1),
                p(1, 2),
                p(3, 2),
                p(1, 3),
                p(3, 3),
                p(1, 4),
                p(2, 4),
                p(3, 4))));
    }

    private static List<Point> cupLumen() {

        return sorted(new HashSet<>(Arrays.asList(
                p(1, 1),
                p(3, 1),
                p(1, 2),
                p(3, 2),
                p(1, 3),
                p(2, 3),
                p(3, 3))));
    }

    private static List<Point> sorted(Set<Point> pixels) {

        List<Point> sorted = new java.util.ArrayList<>(pixels);
        Collections.sort(
                sorted,
                (left, right) -> {
                    int byY = Integer.compare(left.y, right.y);

                    if (byY != 0) {
                        return byY;
                    }

                    return Integer.compare(left.x, right.x);
                });
        return sorted;
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
