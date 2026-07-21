package vesselreconstruction.geometry;

import vesselreconstruction.model.Lumen;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BoundaryExtractor {

    public List<Point> extractBoundary(Lumen lumen) {

        List<Point> boundary = new ArrayList<>();

        Set<Point> pixels = new HashSet<>(lumen.getPixels());

        for (Point p : lumen.getPixels()) {

            if (isBoundaryPixel(p, pixels)) {
                boundary.add(p);
            }
        }

        return boundary;
    }

    private boolean isBoundaryPixel(Point p, Set<Point> pixels) {

        return !pixels.contains(new Point(p.x + 1, p.y)) ||
                !pixels.contains(new Point(p.x - 1, p.y)) ||
                !pixels.contains(new Point(p.x, p.y + 1)) ||
                !pixels.contains(new Point(p.x, p.y - 1));
    }
}