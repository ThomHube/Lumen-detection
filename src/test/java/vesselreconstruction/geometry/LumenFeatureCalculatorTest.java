package vesselreconstruction.geometry;

import org.junit.Test;
import vesselreconstruction.model.Lumen;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LumenFeatureCalculatorTest {

    private static final double TOLERANCE = 1.0e-9;

    private final LumenFeatureCalculator calculator = new LumenFeatureCalculator();

    @Test
    public void squareProducesExpectedShapeFeatures() {

        Lumen lumen = finishedLumen(
                1,
                squarePixels(0, 0, 3),
                squareBoundary(0, 0, 3));

        LumenFeatures features = calculator.calculate(lumen);

        assertEquals(1, features.getLumenId());
        assertEquals(9, features.getArea());
        assertEquals(8.0, features.getPerimeter(), TOLERANCE);
        assertEquals(1.0, features.getAspectRatio(), TOLERANCE);
        assertEquals(1.0, features.getExtent(), TOLERANCE);
        assertEquals(
                4.0 * Math.PI * 9.0 / (8.0 * 8.0),
                features.getCircularity(),
                TOLERANCE);
        assertEquals(
                8.0 / (2.0 * Math.sqrt(Math.PI * 9.0)),
                features.getBoundaryRoughness(),
                TOLERANCE);
    }

    @Test
    public void rectangleProducesExpectedAspectRatioAndExtent() {

        Lumen lumen = finishedLumen(
                2,
                rectanglePixels(0, 0, 2, 4),
                rectangleBoundary(0, 0, 2, 4));

        LumenFeatures features = calculator.calculate(lumen);

        assertEquals(8, features.getArea());
        assertEquals(2.0, features.getAspectRatio(), TOLERANCE);
        assertEquals(1.0, features.getExtent(), TOLERANCE);
        assertEquals(8.0, features.getPerimeter(), TOLERANCE);
    }

    @Test
    public void diagonalBoundarySegmentsUseSqrtTwoStepLength() {

        Lumen lumen = finishedLumen(
                3,
                diagonalLinePixels(0, 0, 3),
                points(
                        p(0, 0),
                        p(1, 1),
                        p(2, 2),
                        p(3, 3)));

        LumenFeatures features = calculator.calculate(lumen);

        assertEquals(
                6.0 * Math.sqrt(2.0),
                features.getPerimeter(),
                TOLERANCE);
    }

    @Test
    public void translatedShapeProducesIdenticalShapeFeatures() {

        Lumen original = finishedLumen(
                4,
                squarePixels(0, 0, 3),
                squareBoundary(0, 0, 3));
        Lumen translated = finishedLumen(
                5,
                squarePixels(100, 50, 3),
                translateBoundary(squareBoundary(0, 0, 3), 100, 50));

        LumenFeatures originalFeatures = calculator.calculate(original);
        LumenFeatures translatedFeatures = calculator.calculate(translated);

        assertEquals(originalFeatures.getArea(), translatedFeatures.getArea());
        assertEquals(
                originalFeatures.getPerimeter(),
                translatedFeatures.getPerimeter(),
                TOLERANCE);
        assertEquals(
                originalFeatures.getCircularity(),
                translatedFeatures.getCircularity(),
                TOLERANCE);
        assertEquals(
                originalFeatures.getAspectRatio(),
                translatedFeatures.getAspectRatio(),
                TOLERANCE);
        assertEquals(
                originalFeatures.getExtent(),
                translatedFeatures.getExtent(),
                TOLERANCE);
        assertEquals(
                originalFeatures.getBoundaryRoughness(),
                translatedFeatures.getBoundaryRoughness(),
                TOLERANCE);
    }

    @Test
    public void circularityIsZeroWhenPerimeterIsZero() {

        LumenFeatures features = new LumenFeatures(
                1,
                4,
                0.0,
                0.0,
                1.0,
                1.0,
                0.0,
                false);

        assertEquals(0.0, features.getCircularity(), TOLERANCE);
    }

    @Test
    public void closingPerimeterSegmentIsIncluded() {

        Lumen lumen = finishedLumen(
                6,
                points(p(0, 0), p(1, 0)),
                points(p(0, 0), p(1, 0)));

        LumenFeatures features = calculator.calculate(lumen);

        assertEquals(2.0, features.getPerimeter(), TOLERANCE);
    }

    @Test
    public void touchesBorderIsCopiedFromLumen() {

        Lumen touching = finishedLumen(
                7,
                squarePixels(0, 0, 2),
                squareBoundary(0, 0, 2));
        touching.setTouchesBorder(true);

        LumenFeatures features = calculator.calculate(touching);

        assertTrue(features.isTouchesBorder());
    }

    @Test
    public void nullLumenIsRejected() {

        try {
            calculator.calculate(null);
        } catch (IllegalArgumentException exception) {
            assertEquals("lumen must not be null.", exception.getMessage());
            return;
        }

        throw new AssertionError("Expected IllegalArgumentException.");
    }

    @Test
    public void unfinishedLumenIsRejected() {

        Lumen lumen = new Lumen(8);
        lumen.addPixel(p(0, 0));

        try {
            calculator.calculate(lumen);
        } catch (IllegalArgumentException exception) {
            assertEquals(
                    "lumen must have been finished.",
                    exception.getMessage());
            return;
        }

        throw new AssertionError("Expected IllegalArgumentException.");
    }

    @Test
    public void emptyFinishedLumenIsRejected() {

        Lumen lumen = new Lumen(9);
        lumen.finish();

        try {
            calculator.calculate(lumen);
        } catch (IllegalArgumentException exception) {
            assertEquals("lumen must not be empty.", exception.getMessage());
            return;
        }

        throw new AssertionError("Expected IllegalArgumentException.");
    }

    @Test
    public void missingBoundaryIsRejected() {

        Lumen lumen = finishedLumen(10, squarePixels(0, 0, 2), new ArrayList<Point>());

        try {
            calculator.calculate(lumen);
        } catch (IllegalArgumentException exception) {
            assertEquals(
                    "boundary must contain at least 2 ordered points.",
                    exception.getMessage());
            return;
        }

        throw new AssertionError("Expected IllegalArgumentException.");
    }

    @Test
    public void insufficientBoundaryIsRejected() {

        Lumen lumen = finishedLumen(
                11,
                squarePixels(0, 0, 2),
                points(p(0, 0)));

        try {
            calculator.calculate(lumen);
        } catch (IllegalArgumentException exception) {
            assertEquals(
                    "boundary must contain at least 2 ordered points.",
                    exception.getMessage());
            return;
        }

        throw new AssertionError("Expected IllegalArgumentException.");
    }

    @Test
    public void inputLumenAndBoundaryPointsRemainUnchanged() {

        List<Point> boundary = squareBoundary(0, 0, 3);
        int[] boundaryX = boundary.stream().mapToInt(point -> point.x).toArray();
        int[] boundaryY = boundary.stream().mapToInt(point -> point.y).toArray();

        Lumen lumen = finishedLumen(
                12,
                squarePixels(0, 0, 3),
                boundary);
        lumen.setTouchesBorder(false);

        int areaBefore = lumen.getArea();
        boolean touchesBorderBefore = lumen.touchesBorder();

        calculator.calculate(lumen);

        assertEquals(areaBefore, lumen.getArea());
        assertEquals(touchesBorderBefore, lumen.touchesBorder());
        assertEquals(boundaryX.length, lumen.getBoundaryPixels().size());

        for (int index = 0; index < boundaryX.length; index++) {
            Point point = lumen.getBoundaryPixels().get(index);
            assertEquals(boundaryX[index], point.x);
            assertEquals(boundaryY[index], point.y);
        }
    }

    @Test
    public void nullBoundaryPointIsRejected() {

        Lumen lumen = new Lumen(14);

        for (Point pixel : squarePixels(0, 0, 2)) {
            lumen.addPixel(new Point(pixel.x, pixel.y));
        }

        lumen.finish();

        List<Point> boundary = new ArrayList<>();
        boundary.add(p(0, 0));
        boundary.add(null);
        lumen.setBoundaryPixels(boundary);

        try {
            calculator.calculate(lumen);
        } catch (IllegalArgumentException exception) {
            assertEquals(
                    "boundary must not contain null points.",
                    exception.getMessage());
            return;
        }

        throw new AssertionError("Expected IllegalArgumentException.");
    }

    @Test
    public void largeBoundingBoxAreaDoesNotOverflow() {

        Lumen lumen = finishedLumen(
                15,
                points(p(0, 0), p(99999, 99999)),
                points(p(0, 0), p(99999, 99999), p(99999, 0)));

        LumenFeatures features = calculator.calculate(lumen);

        long boundingBoxArea = 100_000L * 100_000L;
        double expectedExtent = 2.0 / (double) boundingBoxArea;
        int overflowedBoundingBoxArea = 100_000 * 100_000;

        assertEquals(expectedExtent, features.getExtent(), TOLERANCE);
        assertTrue(expectedExtent < 1.0 / (double) overflowedBoundingBoxArea);
    }

    @Test
    public void extentUsesCorrectLongBoundingBoxArea() {

        Lumen lumen = finishedLumen(
                16,
                points(p(0, 0), p(49999, 49999)),
                points(p(0, 0), p(49999, 49999), p(49999, 0)));

        LumenFeatures features = calculator.calculate(lumen);

        long boundingBoxArea = 50_000L * 50_000L;

        assertEquals(2, features.getArea());
        assertEquals(
                2.0 / (double) boundingBoxArea,
                features.getExtent(),
                TOLERANCE);
    }

    @Test
    public void allCalculatedValuesAreFinite() {

        Lumen lumen = finishedLumen(
                17,
                squarePixels(0, 0, 2),
                squareBoundary(0, 0, 2));

        LumenFeatures features = calculator.calculate(lumen);

        assertTrue(Double.isFinite(features.getPerimeter()));
        assertTrue(Double.isFinite(features.getCircularity()));
        assertTrue(Double.isFinite(features.getAspectRatio()));
        assertTrue(Double.isFinite(features.getExtent()));
        assertTrue(Double.isFinite(features.getBoundaryRoughness()));
        assertTrue(features.getCircularity() > 1.0);
    }

    @Test
    public void allReturnedNumericValuesAreFiniteAndNonNegative() {

        Lumen lumen = finishedLumen(
                13,
                squarePixels(5, 7, 4),
                squareBoundary(5, 7, 4));

        LumenFeatures features = calculator.calculate(lumen);

        assertTrue(Double.isFinite(features.getPerimeter()));
        assertTrue(features.getPerimeter() >= 0.0);
        assertTrue(Double.isFinite(features.getCircularity()));
        assertTrue(features.getCircularity() >= 0.0);
        assertTrue(Double.isFinite(features.getAspectRatio()));
        assertTrue(features.getAspectRatio() >= 1.0);
        assertTrue(Double.isFinite(features.getExtent()));
        assertTrue(features.getExtent() >= 0.0);
        assertTrue(Double.isFinite(features.getBoundaryRoughness()));
        assertTrue(features.getBoundaryRoughness() >= 0.0);
        assertFalse(features.isTouchesBorder());
    }

    private static Lumen finishedLumen(
            int id,
            List<Point> pixels,
            List<Point> boundary) {

        Lumen lumen = new Lumen(id);

        for (Point pixel : pixels) {
            lumen.addPixel(new Point(pixel.x, pixel.y));
        }

        lumen.finish();
        lumen.setBoundaryPixels(copyPoints(boundary));

        return lumen;
    }

    private static List<Point> squarePixels(int originX, int originY, int size) {

        List<Point> pixels = new ArrayList<>();

        for (int deltaY = 0; deltaY < size; deltaY++) {
            for (int deltaX = 0; deltaX < size; deltaX++) {
                pixels.add(p(originX + deltaX, originY + deltaY));
            }
        }

        return pixels;
    }

    private static List<Point> rectanglePixels(
            int originX,
            int originY,
            int width,
            int height) {

        List<Point> pixels = new ArrayList<>();

        for (int deltaY = 0; deltaY < height; deltaY++) {
            for (int deltaX = 0; deltaX < width; deltaX++) {
                pixels.add(p(originX + deltaX, originY + deltaY));
            }
        }

        return pixels;
    }

    private static List<Point> diagonalLinePixels(
            int originX,
            int originY,
            int length) {

        List<Point> pixels = new ArrayList<>();

        for (int index = 0; index <= length; index++) {
            pixels.add(p(originX + index, originY + index));
        }

        return pixels;
    }

    private static List<Point> squareBoundary(
            int originX,
            int originY,
            int size) {

        int maxOffset = size - 1;

        return points(
                p(originX, originY),
                p(originX + maxOffset, originY),
                p(originX + maxOffset, originY + maxOffset),
                p(originX, originY + maxOffset));
    }

    private static List<Point> rectangleBoundary(
            int originX,
            int originY,
            int width,
            int height) {

        int maxX = originX + width - 1;
        int maxY = originY + height - 1;

        return points(
                p(originX, originY),
                p(maxX, originY),
                p(maxX, maxY),
                p(originX, maxY));
    }

    private static List<Point> translateBoundary(
            List<Point> boundary,
            int deltaX,
            int deltaY) {

        List<Point> translated = new ArrayList<>();

        for (Point point : boundary) {
            translated.add(p(point.x + deltaX, point.y + deltaY));
        }

        return translated;
    }

    private static List<Point> copyPoints(List<Point> points) {

        List<Point> copy = new ArrayList<>();

        for (Point point : points) {
            copy.add(new Point(point.x, point.y));
        }

        return copy;
    }

    private static List<Point> points(Point... points) {
        return new ArrayList<>(Arrays.asList(points));
    }

    private static Point p(int x, int y) {
        return new Point(x, y);
    }
}
