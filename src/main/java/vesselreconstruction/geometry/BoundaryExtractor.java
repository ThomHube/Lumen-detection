package vesselreconstruction.geometry;

import vesselreconstruction.model.Lumen;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BoundaryExtractor {

    public List<Point> extractBoundary(Lumen lumen) {

        List<Point> boundaryPixels = new ArrayList<>();

        Set<Point> lumenPixels = new HashSet<>(lumen.getPixels());

        for (Point pixel : lumen.getPixels()) {

            if (isBoundaryPixel(pixel, lumenPixels)) {
                boundaryPixels.add(pixel);
            }
        }

        return boundaryPixels;
    }

    /**
     * A pixel belongs to the boundary if at least one of its
     * four direct neighbours is not part of the lumen.
     */
    private boolean isBoundaryPixel(Point pixel, Set<Point> lumenPixels) {

        return !lumenPixels.contains(new Point(pixel.x + 1, pixel.y))
                || !lumenPixels.contains(new Point(pixel.x - 1, pixel.y))
                || !lumenPixels.contains(new Point(pixel.x, pixel.y + 1))
                || !lumenPixels.contains(new Point(pixel.x, pixel.y - 1));
    }
}