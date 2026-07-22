package vesselreconstruction.geometry;

import org.junit.Test;
import vesselreconstruction.model.Lumen;

import java.awt.Point;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BoundaryTracerTest {

    private final BoundaryTracer tracer = new BoundaryTracer();

    @Test
    public void emptyLumenReturnsEmptyContour() {

        Lumen lumen = lumenFromPoints(1);

        List<Point> contour = tracer.traceBoundary(lumen);

        assertTrue(contour.isEmpty());
    }

    @Test
    public void singlePixelReturnsOnePoint() {

        Lumen lumen = lumenFromPoints(1, p(5, 5));

        List<Point> contour = tracer.traceBoundary(lumen);

        assertEquals(1, contour.size());
        assertPointEquals(p(5, 5), contour.get(0));
    }

    @Test
    public void twoAdjacentPixelsTraceClosedOuterContour() {

        List<Point> contour = tracer.traceBoundary(
                lumenFromPoints(1, p(0, 0), p(1, 0)));

        assertEquals(2, contour.size());
        assertPointEquals(p(0, 0), contour.get(0));
        assertPointEquals(p(1, 0), contour.get(1));
        assertImplicitlyClosed(contour);
    }

    @Test
    public void horizontalLineTracesWithoutRepeatingStart() {

        List<Point> contour = tracer.traceBoundary(
                lumenFromPoints(1, p(0, 0), p(1, 0), p(2, 0), p(3, 0)));

        assertEquals(6, contour.size());
        assertPointEquals(p(0, 0), contour.get(0));
        assertPointEquals(p(3, 0), contour.get(3));
        assertImplicitlyClosed(contour);
    }

    @Test
    public void verticalLineTracesWithoutRepeatingStart() {

        List<Point> contour = tracer.traceBoundary(
                lumenFromPoints(1, p(0, 0), p(0, 1), p(0, 2), p(0, 3)));

        assertEquals(6, contour.size());
        assertPointEquals(p(0, 0), contour.get(0));
        assertPointEquals(p(0, 3), contour.get(3));
        assertImplicitlyClosed(contour);
    }

    @Test
    public void diagonalLineTracesWithoutHanging() {

        List<Point> contour = tracer.traceBoundary(
                lumenFromPoints(1, p(0, 0), p(1, 1), p(2, 2), p(3, 3)));

        assertEquals(6, contour.size());
        assertPointEquals(p(0, 0), contour.get(0));
        assertPointEquals(p(3, 3), contour.get(3));
        assertImplicitlyClosed(contour);
    }

    @Test
    public void solidRectangleTracesOuterBoundary() {

        List<Point> contour = tracer.traceBoundary(
                lumenFromPoints(1,
                        p(0, 0), p(1, 0), p(2, 0),
                        p(0, 1), p(1, 1), p(2, 1),
                        p(0, 2), p(1, 2), p(2, 2)));

        assertEquals(8, contour.size());
        assertEquals(8, uniquePointCount(contour));
        assertPointEquals(p(0, 0), contour.get(0));
        assertImplicitlyClosed(contour);
    }

    @Test
    public void concaveComponentTracesWithoutHanging() {

        List<Point> contour = tracer.traceBoundary(
                lumenFromPoints(1,
                        p(0, 0), p(0, 1), p(0, 2),
                        p(1, 2), p(2, 2)));

        assertEquals(7, contour.size());
        assertPointEquals(p(0, 0), contour.get(0));
        assertImplicitlyClosed(contour);
    }

    private static Lumen lumenFromPoints(int id, Point... points) {

        Lumen lumen = new Lumen(id);

        for (Point point : points) {
            lumen.addPixel(new Point(point.x, point.y));
        }

        lumen.finish();

        return lumen;
    }

    private static Point p(int x, int y) {
        return new Point(x, y);
    }

    private static void assertPointEquals(Point expected, Point actual) {

        assertEquals(
                "Unexpected point",
                expected.x + "," + expected.y,
                actual.x + "," + actual.y);
    }

    private static void assertImplicitlyClosed(List<Point> contour) {

        assertFalse(
                "Contour must not repeat the start point at the end",
                contour.size() > 1
                        && samePoint(contour.get(0), contour.get(contour.size() - 1)));
    }

    private static boolean samePoint(Point left, Point right) {

        return left.x == right.x && left.y == right.y;
    }

    private static int uniquePointCount(List<Point> contour) {

        Set<String> unique = new HashSet<>();

        for (Point point : contour) {
            unique.add(point.x + "," + point.y);
        }

        return unique.size();
    }
}
