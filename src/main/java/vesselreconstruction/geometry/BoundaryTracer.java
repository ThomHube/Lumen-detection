package vesselreconstruction.geometry;

import vesselreconstruction.model.Lumen;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * First implementation of a Moore-neighbor boundary tracer.
 *
 * This version is intended as the initial working implementation for
 * project verification. It will be refined after testing.
 */
public class BoundaryTracer {

    private static final int[] DX = {0,1,1,1,0,-1,-1,-1};
    private static final int[] DY = {-1,-1,0,1,1,1,0,-1};

    public List<Point> traceBoundary(Lumen lumen) {

        List<Point> pixels = lumen.getPixels();
        List<Point> contour = new ArrayList<>();

        if (pixels.isEmpty()) {
            return contour;
        }

        int minX = lumen.getMinX();
        int minY = lumen.getMinY();
        int maxX = lumen.getMaxX();
        int maxY = lumen.getMaxY();

        int w = maxX - minX + 3;
        int h = maxY - minY + 3;

        boolean[][] fg = new boolean[h][w];

        for (Point p : pixels) {
            fg[p.y - minY + 1][p.x - minX + 1] = true;
        }

        Point start = null;
        for (int y = 1; y < h - 1 && start == null; y++) {
            for (int x = 1; x < w - 1; x++) {
                if (fg[y][x] && isBoundary(fg, x, y)) {
                    start = new Point(x, y);
                    break;
                }
            }
        }

        if (start == null) {
            return contour;
        }

        Point current = new Point(start);
        Point backtrack = new Point(start.x - 1, start.y);
        Point initialBacktrack = new Point(backtrack);

        do {
            contour.add(new Point(current.x + minX - 1, current.y + minY - 1));

            int backDir = direction(current, backtrack);
            boolean found = false;

            for (int i = 1; i <= 8; i++) {
                int dir = (backDir + i) % 8;
                int nx = current.x + DX[dir];
                int ny = current.y + DY[dir];

                if (fg[ny][nx]) {
                    backtrack = new Point(
                            current.x + DX[(dir + 7) % 8],
                            current.y + DY[(dir + 7) % 8]
                    );
                    current = new Point(nx, ny);
                    found = true;
                    break;
                }
            }

            if (!found) {
                break;
            }

        } while (!(current.equals(start) && backtrack.equals(initialBacktrack)));

        return contour;
    }

    private boolean isBoundary(boolean[][] fg, int x, int y) {
        for (int i = 0; i < 8; i++) {
            if (!fg[y + DY[i]][x + DX[i]]) {
                return true;
            }
        }
        return false;
    }

    private int direction(Point center, Point neighbour) {
        int dx = neighbour.x - center.x;
        int dy = neighbour.y - center.y;

        for (int i = 0; i < 8; i++) {
            if (DX[i] == dx && DY[i] == dy) {
                return i;
            }
        }
        return 6;
    }
}