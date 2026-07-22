package vesselreconstruction.geometry;

import org.junit.Test;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ImaginaryRulerTest {

    private static final double TOLERANCE = 1.0e-9;

    private final ImaginaryRuler ruler = new ImaginaryRuler();

    @Test
    public void horizontalStraightBoundarySectionProducesZeroDeviation() {

        List<Point> boundary = points(
                p(0, 0),
                p(1, 0),
                p(2, 0),
                p(3, 0),
                p(4, 0));

        double[] scores = ruler.calculateDeviationScores(boundary, 1);

        assertEquals(0.0, scores[2], TOLERANCE);
    }

    @Test
    public void verticalStraightBoundarySectionProducesZeroDeviation() {

        List<Point> boundary = points(
                p(0, 0),
                p(0, 1),
                p(0, 2),
                p(0, 3),
                p(0, 4));

        double[] scores = ruler.calculateDeviationScores(boundary, 1);

        assertEquals(0.0, scores[2], TOLERANCE);
    }

    @Test
    public void displacedPointProducesExpectedPerpendicularDistance() {

        List<Point> boundary = points(
                p(0, 0),
                p(1, 0),
                p(2, 3),
                p(3, 0),
                p(4, 0));

        double[] scores = ruler.calculateDeviationScores(boundary, 1);

        assertEquals(3.0, scores[2], TOLERANCE);
    }

    @Test
    public void translatedCoordinatesProduceSameScore() {

        List<Point> boundary = points(
                p(0, 0),
                p(1, 0),
                p(2, 3),
                p(3, 0),
                p(4, 0));
        List<Point> translatedBoundary = points(
                p(100, 50),
                p(101, 50),
                p(102, 53),
                p(103, 50),
                p(104, 50));

        double[] originalScores = ruler.calculateDeviationScores(boundary, 1);
        double[] translatedScores =
                ruler.calculateDeviationScores(translatedBoundary, 1);

        assertArrayEquals(originalScores, translatedScores, TOLERANCE);
    }

    @Test
    public void circularIndexingWorksAtIndexZero() {

        List<Point> boundary = points(
                p(0, 0),
                p(1, 0),
                p(2, 0),
                p(3, 0),
                p(4, 0));

        double[] scores = ruler.calculateDeviationScores(boundary, 1);

        assertEquals(0.0, scores[0], TOLERANCE);
    }

    @Test
    public void returnedArrayLengthEqualsBoundaryLength() {

        List<Point> boundary = points(
                p(0, 0),
                p(1, 0),
                p(2, 0),
                p(3, 0),
                p(4, 0));

        double[] scores = ruler.calculateDeviationScores(boundary, 1);

        assertEquals(boundary.size(), scores.length);
    }

    @Test
    public void coincidentWindowCentroidsProduceFiniteResult() {

        List<Point> boundary = points(
                p(0, 0),
                p(1, 0),
                p(0, 0),
                p(2, 0),
                p(3, 0));

        double[] scores = ruler.calculateDeviationScores(boundary, 1);

        assertEquals(1.0, scores[1], TOLERANCE);
        assertTrue(Double.isFinite(scores[1]));
        assertTrue(scores[1] >= 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullBoundaryRejected() {

        ruler.calculateDeviationScores(null, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullPointRejected() {

        List<Point> boundary = new ArrayList<>(Arrays.asList(
                p(0, 0),
                null,
                p(2, 0),
                p(3, 0),
                p(4, 0)));

        ruler.calculateDeviationScores(boundary, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void neighbourCountZeroRejected() {

        ruler.calculateDeviationScores(points(p(0, 0), p(1, 0), p(2, 0)), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void neighbourCountNegativeRejected() {

        ruler.calculateDeviationScores(points(p(0, 0), p(1, 0), p(2, 0)), -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insufficientBoundaryLengthRejected() {

        ruler.calculateDeviationScores(points(p(0, 0), p(1, 0)), 1);
    }

    @Test
    public void inputListAndPointsRemainUnchanged() {

        Point first = p(0, 0);
        Point second = p(1, 0);
        Point third = p(2, 3);
        Point fourth = p(3, 0);
        Point fifth = p(4, 0);

        List<Point> boundary = points(first, second, third, fourth, fifth);
        List<Point> snapshot = points(
                p(first.x, first.y),
                p(second.x, second.y),
                p(third.x, third.y),
                p(fourth.x, fourth.y),
                p(fifth.x, fifth.y));

        ruler.calculateDeviationScores(boundary, 1);

        assertEquals(snapshot.size(), boundary.size());

        for (int index = 0; index < snapshot.size(); index++) {
            assertPointEquals(snapshot.get(index), boundary.get(index));
        }
    }

    private static List<Point> points(Point... boundaryPoints) {

        return new ArrayList<>(Arrays.asList(boundaryPoints));
    }

    private static Point p(int x, int y) {
        return new Point(x, y);
    }

    private static void assertPointEquals(Point expected, Point actual) {

        assertEquals(expected.x, actual.x);
        assertEquals(expected.y, actual.y);
    }
}
