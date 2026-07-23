package vesselreconstruction.reconstruction;

import org.junit.Test;

import java.awt.Point;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SinglePixelLumenReconstructorTest {

    private final SinglePixelLumenReconstructor reconstructor =
            new SinglePixelLumenReconstructor();

    @Test
    public void bridgeOpeningRetainsComponentContainingSeed() {

        List<Point> lumenPixels = dumbbellLumen();

        SinglePixelReconstructionResult result =
                reconstructor.reconstructToMaximumCircularity(
                        lumenPixels,
                        Collections.<Point>emptySet(),
                        p(2, 1),
                        p(4, 1),
                        p(1, 0),
                        0,
                        5,
                        2);

        assertTrue(result.getOpenedLumenPixels().size() < lumenPixels.size());
        assertTrue(containsPoint(result.getOpenedLumenPixels(), p(1, 0)));
    }

    @Test
    public void onePixelIsAddedPerReconstructionStep() {

        List<Point> lumenPixels = cupLumen();
        Set<Point> woodPixels = new HashSet<>(Collections.singletonList(p(2, 2)));

        SinglePixelReconstructionResult result =
                reconstructor.reconstructToMaximumCircularity(
                        lumenPixels,
                        woodPixels,
                        p(1, 1),
                        p(3, 1),
                        p(1, 2),
                        0,
                        1,
                        5);

        assertEquals(1, result.getPerformedReconstructionSteps());
    }

    @Test
    public void selectsEligiblePixelWithGreatestMinimumBridgeDistanceFirst() {

        List<Point> lumenPixels = tallCupLumen();
        Set<Point> woodPixels = tallCupEnclosedWood();

        SinglePixelReconstructionResult result =
                reconstructor.reconstructToMaximumCircularity(
                        lumenPixels,
                        woodPixels,
                        p(1, 1),
                        p(3, 1),
                        p(1, 2),
                        0,
                        1,
                        10);

        assertEquals(new Point(2, 3), result.getAddedWoodPixels().get(0));
    }

    @Test
    public void minimumBridgeDistanceScoreUsesCloserEndpoint() {

        List<Point> lumenPixels = wideCupLumen(10);
        Set<Point> woodPixels = new HashSet<>(Arrays.asList(
                p(9, 2),
                p(5, 4)));

        SinglePixelReconstructionResult result =
                reconstructor.reconstructToMaximumCircularity(
                        lumenPixels,
                        woodPixels,
                        p(0, 0),
                        p(10, 0),
                        p(0, 1),
                        0,
                        1,
                        10);

        assertEquals(new Point(5, 4), result.getAddedWoodPixels().get(0));
    }

    @Test
    public void equalDistanceScoreTieBreaksOnSmallestY() throws Exception {

        Point firstBridgePoint = p(0, 0);
        Point secondBridgePoint = p(6, 4);
        Point lowerY = p(2, 1);
        Point higherY = p(1, 2);

        assertEquals(
                minimumSquaredBridgeDistance(
                        lowerY,
                        firstBridgePoint,
                        secondBridgePoint),
                minimumSquaredBridgeDistance(
                        higherY,
                        firstBridgePoint,
                        secondBridgePoint));
        assertTrue(
                compareBridgeDistanceCandidates(
                        lowerY,
                        higherY,
                        firstBridgePoint,
                        secondBridgePoint)
                        < 0);
    }

    @Test
    public void equalDistanceScoreAndYTieBreaksOnSmallestX() {

        List<Point> lumenPixels = wideCupLumen(8);
        Set<Point> woodPixels = new HashSet<>(Arrays.asList(
                p(5, 3),
                p(1, 3)));

        SinglePixelReconstructionResult result =
                reconstructor.reconstructToMaximumCircularity(
                        lumenPixels,
                        woodPixels,
                        p(0, 0),
                        p(8, 0),
                        p(0, 1),
                        0,
                        1,
                        10);

        assertEquals(new Point(1, 3), result.getAddedWoodPixels().get(0));
    }

    @Test
    public void closerPixelWithSmallerYDoesNotBeatFartherPixel() {

        List<Point> lumenPixels = tallCupLumen();
        Set<Point> woodPixels = tallCupEnclosedWood();

        SinglePixelReconstructionResult firstStepResult =
                reconstructor.reconstructToMaximumCircularity(
                        lumenPixels,
                        woodPixels,
                        p(1, 1),
                        p(3, 1),
                        p(1, 2),
                        0,
                        1,
                        10);
        SinglePixelReconstructionResult fullResult =
                reconstructor.reconstructToMaximumCircularity(
                        lumenPixels,
                        woodPixels,
                        p(1, 1),
                        p(3, 1),
                        p(1, 2),
                        0,
                        2,
                        10);

        assertEquals(new Point(2, 3), firstStepResult.getAddedWoodPixels().get(0));
        assertEquals(2, fullResult.getPerformedReconstructionSteps());
        assertEquals(
                new HashSet<>(Arrays.asList(new Point(2, 2), new Point(2, 3))),
                new HashSet<>(fullResult.getAddedWoodPixels()));
    }

    @Test
    public void squaredDistanceArithmeticDoesNotOverflowForLargeCoordinates() {

        int offset = 100_000;
        List<Point> lumenPixels = offsetPoints(tallCupLumen(), offset, offset);
        Set<Point> woodPixels = offsetPoints(tallCupEnclosedWood(), offset, offset);

        SinglePixelReconstructionResult firstStepResult =
                reconstructor.reconstructToMaximumCircularity(
                        lumenPixels,
                        woodPixels,
                        p(1 + offset, 1 + offset),
                        p(3 + offset, 1 + offset),
                        p(1 + offset, 2 + offset),
                        0,
                        1,
                        10);
        SinglePixelReconstructionResult fullResult =
                reconstructor.reconstructToMaximumCircularity(
                        lumenPixels,
                        woodPixels,
                        p(1 + offset, 1 + offset),
                        p(3 + offset, 1 + offset),
                        p(1 + offset, 2 + offset),
                        0,
                        2,
                        10);

        assertEquals(
                new Point(2 + offset, 3 + offset),
                firstStepResult.getAddedWoodPixels().get(0));
        assertEquals(2, fullResult.getPerformedReconstructionSteps());
        assertEquals(
                new HashSet<>(Arrays.asList(
                        new Point(2 + offset, 2 + offset),
                        new Point(2 + offset, 3 + offset))),
                new HashSet<>(fullResult.getAddedWoodPixels()));
    }

    @Test
    public void onlyBridgeEnclosedWoodCanBeAdded() {

        List<Point> lumenPixels = cShapeLumen();
        Set<Point> woodPixels = cShapePortalRegressionWood();

        SinglePixelReconstructionResult result =
                reconstructor.reconstructToMaximumCircularity(
                        lumenPixels,
                        woodPixels,
                        p(2, 2),
                        p(6, 2),
                        p(0, 2),
                        0,
                        20,
                        10);

        for (Point addedWood : result.getAddedWoodPixels()) {
            assertTrue(containsPoint(result.getBridgeEnclosedWoodPixels(), addedWood));
        }

        assertFalse(containsPoint(result.getAddedWoodPixels(), p(-1, 2)));
        assertFalse(containsPoint(result.getAddedWoodPixels(), p(0, -1)));
    }

    @Test
    public void noEnclosedWoodProducesNoCandidateWithZeroSteps() {

        List<Point> lumenPixels = block(0, 0, 2, 2);

        SinglePixelReconstructionResult result =
                reconstructor.reconstructToMaximumCircularity(
                        lumenPixels,
                        Collections.<Point>singleton(p(10, 10)),
                        p(0, 0),
                        p(1, 0),
                        p(0, 1),
                        0,
                        10,
                        5);

        assertEquals(0, result.getPerformedReconstructionSteps());
        assertTrue(result.isStoppedBecauseNoCandidate());
        assertTrue(result.getAddedWoodPixels().isEmpty());
    }

    @Test
    public void eightConnectedDiagonalCandidateWorks() {

        List<Point> lumenPixels = ringLumen();
        Set<Point> woodPixels = Collections.singleton(p(1, 1));

        SinglePixelReconstructionResult result =
                reconstructor.reconstructToMaximumCircularity(
                        lumenPixels,
                        woodPixels,
                        p(0, 0),
                        p(2, 0),
                        p(0, 1),
                        0,
                        1,
                        1);

        assertEquals(1, result.getPerformedReconstructionSteps());
        assertEquals(new Point(1, 1), result.getAddedWoodPixels().get(0));
    }

    @Test
    public void bridgeLocalReconstructionCanImproveCircularity() {

        List<Point> lumenPixels = cupLumen();
        Set<Point> woodPixels = new HashSet<>(Collections.singletonList(p(2, 2)));

        SinglePixelReconstructionResult result =
                reconstructor.reconstructToMaximumCircularity(
                        lumenPixels,
                        woodPixels,
                        p(1, 1),
                        p(3, 1),
                        p(1, 2),
                        0,
                        20,
                        5);

        assertFalse(result.getAddedWoodPixels().isEmpty());
        assertTrue(result.getBestCircularity() >= result.getOriginalCircularity());
    }

    @Test
    public void bestCircularityStateIsRetainedRatherThanFinalExploredState() {

        List<Point> lumenPixels = cShapeLumen();
        Set<Point> woodPixels = cShapeWood();

        SinglePixelReconstructionResult result =
                reconstructor.reconstructToMaximumCircularity(
                        lumenPixels,
                        woodPixels,
                        p(2, 2),
                        p(6, 2),
                        p(0, 2),
                        0,
                        20,
                        3);

        assertTrue(result.getBestReconstructionStep()
                <= result.getPerformedReconstructionSteps());
    }

    @Test
    public void reconstructionStopsAfterPatienceIsExhausted() {

        List<Point> lumenPixels = tallCupLumen();
        Set<Point> woodPixels = tallCupEnclosedWood();

        SinglePixelReconstructionResult result =
                reconstructor.reconstructToMaximumCircularity(
                        lumenPixels,
                        woodPixels,
                        p(1, 1),
                        p(3, 1),
                        p(1, 2),
                        0,
                        50,
                        2);

        assertTrue(
                result.isStoppedBecausePatienceExhausted()
                        || result.getPerformedReconstructionSteps() >= 2);
    }

    @Test
    public void reconstructionStopsAtMaximumStepLimit() {

        List<Point> lumenPixels = tallCupLumen();
        Set<Point> woodPixels = tallCupEnclosedWood();

        SinglePixelReconstructionResult result =
                reconstructor.reconstructToMaximumCircularity(
                        lumenPixels,
                        woodPixels,
                        p(1, 1),
                        p(3, 1),
                        p(1, 2),
                        0,
                        2,
                        100);

        assertEquals(2, result.getPerformedReconstructionSteps());
        assertFalse(result.isStoppedBecauseNoCandidate());
        assertFalse(result.isStoppedBecausePatienceExhausted());
    }

    @Test
    public void bridgeEnclosedWoodPixelsAreReported() {

        List<Point> lumenPixels = tallCupLumen();
        Set<Point> woodPixels = tallCupEnclosedWood();

        SinglePixelReconstructionResult result =
                reconstructor.reconstructToMaximumCircularity(
                        lumenPixels,
                        woodPixels,
                        p(1, 1),
                        p(3, 1),
                        p(1, 2),
                        0,
                        1,
                        5);

        assertEquals(2, result.getBridgeEnclosedWoodPixels().size());
    }

    @Test
    public void returnedCollectionsAreUnmodifiable() {

        SinglePixelReconstructionResult result =
                reconstructor.reconstructToMaximumCircularity(
                        dumbbellLumen(),
                        Collections.<Point>emptySet(),
                        p(2, 1),
                        p(4, 1),
                        p(1, 0),
                        0,
                        1,
                        1);

        expectUnsupportedOperation(
                () -> result.getBestLumenPixels().add(p(0, 0)));
        expectUnsupportedOperation(
                () -> result.getBridgeEnclosedWoodPixels().add(p(0, 0)));
    }

    @Test
    public void inputsAreNotMutated() {

        List<Point> lumenPixels = new ArrayList<>(dumbbellLumen());
        Set<Point> woodPixels = new HashSet<>(cShapeWood());
        int originalWoodSize = woodPixels.size();

        reconstructor.reconstructToMaximumCircularity(
                lumenPixels,
                woodPixels,
                p(2, 1),
                p(4, 1),
                p(1, 0),
                0,
                2,
                2);

        assertEquals(dumbbellLumen().size(), lumenPixels.size());
        assertEquals(originalWoodSize, woodPixels.size());
    }

    @Test
    public void nullLumenInputRejected() {
        expectIllegalArgument(
                () -> reconstructor.reconstructToMaximumCircularity(
                        null,
                        Collections.<Point>emptySet(),
                        p(0, 0),
                        p(1, 0),
                        p(0, 0),
                        0,
                        1,
                        1),
                "originalLumenPixels must not be null.");
    }

    @Test
    public void maximumStepsBelowOneRejected() {
        expectIllegalArgument(
                () -> reconstructor.reconstructToMaximumCircularity(
                        dumbbellLumen(),
                        Collections.<Point>emptySet(),
                        p(2, 1),
                        p(4, 1),
                        p(1, 0),
                        0,
                        0,
                        1),
                "maximumReconstructionSteps must be at least 1.");
    }

    @Test
    public void patienceBelowOneRejected() {
        expectIllegalArgument(
                () -> reconstructor.reconstructToMaximumCircularity(
                        dumbbellLumen(),
                        Collections.<Point>emptySet(),
                        p(2, 1),
                        p(4, 1),
                        p(1, 0),
                        0,
                        1,
                        0),
                "nonImprovingStepPatience must be at least 1.");
    }

    private static List<Point> wideCupLumen(int width) {

        Set<Point> pixels = new HashSet<>();

        for (int y = 0; y <= 5; y++) {
            pixels.add(p(0, y));
            pixels.add(p(width, y));
        }

        for (int x = 0; x <= width; x++) {
            pixels.add(p(x, 5));
        }

        return sorted(pixels);
    }

    private static Set<Point> offsetPoints(Set<Point> pixels, int offsetX, int offsetY) {

        Set<Point> offsetPixels = new HashSet<>();

        for (Point pixel : pixels) {
            offsetPixels.add(p(pixel.x + offsetX, pixel.y + offsetY));
        }

        return offsetPixels;
    }

    private static List<Point> offsetPoints(List<Point> pixels, int offsetX, int offsetY) {

        List<Point> offsetPixels = new ArrayList<>(pixels.size());

        for (Point pixel : pixels) {
            offsetPixels.add(p(pixel.x + offsetX, pixel.y + offsetY));
        }

        return offsetPixels;
    }

    private static long minimumSquaredBridgeDistance(
            Point point,
            Point firstBridgePoint,
            Point secondBridgePoint) throws Exception {

        Method scoreMethod = SinglePixelLumenReconstructor.class.getDeclaredMethod(
                "minimumSquaredBridgeDistance",
                Point.class,
                Point.class,
                Point.class);
        scoreMethod.setAccessible(true);

        return (Long) scoreMethod.invoke(
                null,
                point,
                firstBridgePoint,
                secondBridgePoint);
    }

    private static int compareBridgeDistanceCandidates(
            Point left,
            Point right,
            Point firstBridgePoint,
            Point secondBridgePoint) throws Exception {

        Method compareMethod = SinglePixelLumenReconstructor.class.getDeclaredMethod(
                "compareBridgeDistanceCandidates",
                Point.class,
                Point.class,
                Point.class,
                Point.class);
        compareMethod.setAccessible(true);

        return (Integer) compareMethod.invoke(
                null,
                left,
                right,
                firstBridgePoint,
                secondBridgePoint);
    }

    private static List<Point> cupLumen() {

        Set<Point> pixels = new HashSet<>(Arrays.asList(
                p(1, 1),
                p(3, 1),
                p(1, 2),
                p(3, 2),
                p(1, 3),
                p(2, 3),
                p(3, 3)));
        return sorted(pixels);
    }

    private static List<Point> tallCupLumen() {

        Set<Point> pixels = new HashSet<>(Arrays.asList(
                p(1, 1),
                p(3, 1),
                p(1, 2),
                p(3, 2),
                p(1, 3),
                p(3, 3),
                p(1, 4),
                p(2, 4),
                p(3, 4)));
        return sorted(pixels);
    }

    private static Set<Point> tallCupEnclosedWood() {

        return new HashSet<>(Arrays.asList(p(2, 2), p(2, 3)));
    }

    private static List<Point> ringLumen() {

        Set<Point> pixels = new HashSet<>();

        for (int x = 0; x < 3; x++) {
            pixels.add(p(x, 0));
            pixels.add(p(x, 2));
        }

        pixels.add(p(0, 1));
        pixels.add(p(2, 1));

        return sorted(pixels);
    }

    private static List<Point> dumbbellLumen() {

        Set<Point> pixels = new HashSet<>();
        addBlock(pixels, 0, 0, 3, 2);
        pixels.add(p(3, 1));
        addBlock(pixels, 4, 0, 3, 2);
        return sorted(pixels);
    }

    private static List<Point> cShapeLumen() {

        Set<Point> pixels = new HashSet<>();
        addBlock(pixels, 0, 0, 7, 3);
        addBlock(pixels, 0, 3, 3, 4);
        addBlock(pixels, 4, 3, 3, 4);
        pixels.remove(p(3, 2));
        pixels.remove(p(4, 2));
        return sorted(pixels);
    }

    private static Set<Point> cShapeWood() {

        Set<Point> wood = new HashSet<>();
        addBlock(wood, 3, 2, 2, 1);
        return wood;
    }

    private static Set<Point> cShapePortalRegressionWood() {

        Set<Point> wood = new HashSet<>();
        wood.add(p(3, 2));
        wood.add(p(4, 2));
        wood.add(p(-1, 2));
        wood.add(p(0, -1));
        wood.add(p(3, 5));
        return wood;
    }

    private static Set<Point> portalWoodChain() {

        Set<Point> wood = new HashSet<>();

        for (int x = 0; x <= 5; x++) {
            wood.add(p(x, -1));
        }

        return wood;
    }

    private static List<Point> block(int startX, int startY, int width, int height) {
        Set<Point> pixels = new HashSet<>();
        addBlock(pixels, startX, startY, width, height);
        return sorted(pixels);
    }

    private static void addBlock(
            Set<Point> pixels,
            int startX,
            int startY,
            int width,
            int height) {

        for (int y = startY; y < startY + height; y++) {

            for (int x = startX; x < startX + width; x++) {
                pixels.add(p(x, y));
            }
        }
    }

    private static List<Point> sorted(Set<Point> pixels) {

        List<Point> sorted = new ArrayList<>(pixels);
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

    private static boolean containsPoint(List<Point> pixels, Point target) {

        for (Point pixel : pixels) {

            if (pixel.x == target.x && pixel.y == target.y) {
                return true;
            }
        }

        return false;
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

    private static void expectUnsupportedOperation(Runnable action) {

        try {
            action.run();
        } catch (UnsupportedOperationException exception) {
            return;
        }

        throw new AssertionError("Expected UnsupportedOperationException.");
    }
}
