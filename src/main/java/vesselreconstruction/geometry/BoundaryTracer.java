package vesselreconstruction.geometry;

import vesselreconstruction.model.Lumen;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Moore-neighbor outer boundary tracer for lumen foreground pixels.
 *
 * <p>Uses Jacob Eliosoff's directed-entry stopping criterion: tracing completes
 * when the tracer is at the start pixel and the next boundary step would move
 * to the same first-next boundary point recorded after the start.
 */
public class BoundaryTracer {

    /**
     * Clockwise Moore offsets from north in ImageJ image coordinates (y down).
     */
    private static final int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] DY = {-1, -1, 0, 1, 1, 1, 0, -1};

    public List<Point> traceBoundary(Lumen lumen) {

        List<Point> pixels = lumen.getPixels();
        List<Point> contour = new ArrayList<>();

        if (pixels.isEmpty()) {
            return contour;
        }

        if (pixels.size() == 1) {
            Point only = pixels.get(0);
            contour.add(new Point(only.x, only.y));
            return contour;
        }

        int minX = lumen.getMinX();
        int minY = lumen.getMinY();
        int maxX = lumen.getMaxX();
        int maxY = lumen.getMaxY();

        int gridWidth = maxX - minX + 3;
        int gridHeight = maxY - minY + 3;

        boolean[][] foreground = new boolean[gridHeight][gridWidth];

        for (Point pixel : pixels) {
            foreground[pixel.y - minY + 1][pixel.x - minX + 1] = true;
        }

        GridPoint startGrid = toGridPoint(findStartPixel(pixels), minX, minY);
        GridPoint initialBacktrackGrid = new GridPoint(startGrid.x - 1, startGrid.y);

        contour.add(toImagePoint(startGrid, minX, minY));

        MooreStep firstStep = mooreStep(
                foreground,
                startGrid.x,
                startGrid.y,
                initialBacktrackGrid.x,
                initialBacktrackGrid.y);

        GridPoint firstNextGrid = firstStep.boundary;
        GridPoint currentGrid = firstNextGrid;
        GridPoint backtrackGrid = firstStep.backtrack;

        contour.add(toImagePoint(firstNextGrid, minX, minY));

        Set<DirectedTransition> visitedTransitions = new HashSet<>();
        visitedTransitions.add(new DirectedTransition(startGrid, firstNextGrid));

        int maxIterations = Math.max(32, lumen.getArea() * 16);

        for (int iteration = 0; iteration < maxIterations; iteration++) {

            MooreStep nextStep = mooreStep(
                    foreground,
                    currentGrid.x,
                    currentGrid.y,
                    backtrackGrid.x,
                    backtrackGrid.y);

            GridPoint nextBoundaryGrid = nextStep.boundary;

            if (currentGrid.equals(startGrid)
                    && nextBoundaryGrid.equals(firstNextGrid)) {
                return contour;
            }

            if (nextBoundaryGrid.equals(startGrid)) {
                currentGrid = startGrid;
                backtrackGrid = nextStep.backtrack;
                continue;
            }

            DirectedTransition transition =
                    new DirectedTransition(currentGrid, nextBoundaryGrid);

            if (!visitedTransitions.add(transition)) {
                throw tracingFailure(
                        lumen,
                        iteration,
                        toImagePoint(currentGrid, minX, minY),
                        toImagePoint(backtrackGrid, minX, minY),
                        contour.size());
            }

            contour.add(toImagePoint(nextBoundaryGrid, minX, minY));

            currentGrid = nextBoundaryGrid;
            backtrackGrid = nextStep.backtrack;
        }

        throw tracingFailure(
                lumen,
                maxIterations,
                toImagePoint(currentGrid, minX, minY),
                toImagePoint(backtrackGrid, minX, minY),
                contour.size());
    }

    private static Point findStartPixel(List<Point> pixels) {

        Point start = pixels.get(0);

        for (Point pixel : pixels) {

            if (pixel.y < start.y || (pixel.y == start.y && pixel.x < start.x)) {
                start = pixel;
            }
        }

        return start;
    }

    /**
     * Performs one atomic Moore-neighbor step from boundary pixel
     * ({@code boundaryX}, {@code boundaryY}) with background/backtrack pixel
     * ({@code backtrackX}, {@code backtrackY}).
     */
    private static MooreStep mooreStep(
            boolean[][] foreground,
            int boundaryX,
            int boundaryY,
            int backtrackX,
            int backtrackY) {

        int backDirection = directionIndex(
                boundaryX, boundaryY, backtrackX, backtrackY);

        for (int scanOffset = 1; scanOffset <= 8; scanOffset++) {

            int direction = (backDirection + scanOffset) & 7;
            int neighborX = boundaryX + DX[direction];
            int neighborY = boundaryY + DY[direction];

            if (foreground[neighborY][neighborX]) {

                int precedingDirection = (backDirection + scanOffset - 1) & 7;

                return new MooreStep(
                        new GridPoint(neighborX, neighborY),
                        new GridPoint(
                                boundaryX + DX[precedingDirection],
                                boundaryY + DY[precedingDirection]));
            }
        }

        throw new IllegalStateException(
                "No foreground Moore neighbor found for boundary pixel ("
                        + boundaryX + ", " + boundaryY + ").");
    }

    private static int directionIndex(
            int centerX,
            int centerY,
            int neighborX,
            int neighborY) {

        int deltaX = neighborX - centerX;
        int deltaY = neighborY - centerY;

        for (int direction = 0; direction < 8; direction++) {

            if (DX[direction] == deltaX && DY[direction] == deltaY) {
                return direction;
            }
        }

        throw new IllegalStateException(
                "Backtrack pixel ("
                        + neighborX + ", " + neighborY
                        + ") is not a Moore neighbor of boundary pixel ("
                        + centerX + ", " + centerY + ").");
    }

    private static GridPoint toGridPoint(Point imagePoint, int minX, int minY) {

        return new GridPoint(
                imagePoint.x - minX + 1,
                imagePoint.y - minY + 1);
    }

    private static Point toImagePoint(GridPoint gridPoint, int minX, int minY) {

        return new Point(
                gridPoint.x + minX - 1,
                gridPoint.y + minY - 1);
    }

    private static IllegalStateException tracingFailure(
            Lumen lumen,
            int iterationCount,
            Point currentPoint,
            Point backtrackPoint,
            int contourSize) {

        return new IllegalStateException(
                "Moore boundary tracing failed for lumen "
                        + lumen.getId()
                        + ": iterationCount="
                        + iterationCount
                        + ", currentPoint="
                        + currentPoint
                        + ", backtrackPoint="
                        + backtrackPoint
                        + ", contourSize="
                        + contourSize);
    }

    private static final class GridPoint {

        private final int x;
        private final int y;

        private GridPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object object) {

            if (this == object) {
                return true;
            }

            if (!(object instanceof GridPoint)) {
                return false;
            }

            GridPoint other = (GridPoint) object;

            return x == other.x && y == other.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    private static final class DirectedTransition {

        private final GridPoint previous;
        private final GridPoint current;

        private DirectedTransition(GridPoint previous, GridPoint current) {
            this.previous = previous;
            this.current = current;
        }

        @Override
        public boolean equals(Object object) {

            if (this == object) {
                return true;
            }

            if (!(object instanceof DirectedTransition)) {
                return false;
            }

            DirectedTransition other = (DirectedTransition) object;

            return previous.equals(other.previous)
                    && current.equals(other.current);
        }

        @Override
        public int hashCode() {
            return Objects.hash(previous, current);
        }
    }

    private static final class MooreStep {

        private final GridPoint boundary;
        private final GridPoint backtrack;

        private MooreStep(GridPoint boundary, GridPoint backtrack) {
            this.boundary = boundary;
            this.backtrack = backtrack;
        }
    }
}
