package vesselreconstruction.geometry;

import vesselreconstruction.model.Lumen;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

/**
 * Calculates geometric shape features for finished lumen candidates.
 */
public class LumenFeatureCalculator {

    /**
     * Calculates geometric features for a finished lumen with an ordered
     * boundary.
     *
     * @param lumen finished lumen with at least two ordered boundary points
     * @return immutable feature summary
     * @throws IllegalArgumentException if the lumen or its geometry is invalid
     */
    public LumenFeatures calculate(Lumen lumen) {

        validateLumen(lumen);

        int area = lumen.getArea();
        List<Point> boundaryPixels = lumen.getBoundaryPixels();
        Rectangle boundingBox = lumen.getBoundingBox();
        long boundingBoxArea = boundingBoxArea(boundingBox);

        double perimeter = calculatePerimeter(boundaryPixels);
        double circularity = calculateCircularity(area, perimeter);
        double aspectRatio = calculateAspectRatio(boundingBox);
        double extent = calculateExtent(area, boundingBoxArea);
        double boundaryRoughness = calculateBoundaryRoughness(perimeter, area);

        validateCalculatedFeatures(
                perimeter,
                circularity,
                aspectRatio,
                extent,
                boundaryRoughness);

        return new LumenFeatures(
                lumen.getId(),
                area,
                perimeter,
                circularity,
                aspectRatio,
                extent,
                boundaryRoughness,
                lumen.touchesBorder());
    }

    private static void validateLumen(Lumen lumen) {

        if (lumen == null) {
            throw new IllegalArgumentException("lumen must not be null.");
        }

        if (lumen.getCentroid() == null || lumen.getBoundingBox() == null) {
            throw new IllegalArgumentException("lumen must have been finished.");
        }

        if (lumen.getArea() <= 0) {
            throw new IllegalArgumentException("lumen must not be empty.");
        }

        List<Point> boundaryPixels = lumen.getBoundaryPixels();

        if (boundaryPixels == null || boundaryPixels.size() < 2) {
            throw new IllegalArgumentException(
                    "boundary must contain at least 2 ordered points.");
        }

        for (Point boundaryPoint : boundaryPixels) {

            if (boundaryPoint == null) {
                throw new IllegalArgumentException(
                        "boundary must not contain null points.");
            }
        }

        Rectangle boundingBox = lumen.getBoundingBox();

        if (boundingBox.width <= 0 || boundingBox.height <= 0) {
            throw new IllegalArgumentException(
                    "lumen bounding box must have positive width and height.");
        }

        if (boundingBoxArea(boundingBox) <= 0L) {
            throw new IllegalArgumentException(
                    "lumen bounding box area must be positive.");
        }
    }

    private static long boundingBoxArea(Rectangle boundingBox) {

        return (long) boundingBox.width * (long) boundingBox.height;
    }

    private static void validateCalculatedFeatures(
            double perimeter,
            double circularity,
            double aspectRatio,
            double extent,
            double boundaryRoughness) {

        if (!Double.isFinite(perimeter) || perimeter < 0.0) {
            throw new IllegalStateException(
                    "calculated perimeter must be finite and non-negative.");
        }

        if (!Double.isFinite(circularity) || circularity < 0.0) {
            throw new IllegalStateException(
                    "calculated circularity must be finite and non-negative.");
        }

        if (!Double.isFinite(aspectRatio) || aspectRatio < 1.0) {
            throw new IllegalStateException(
                    "calculated aspectRatio must be finite and at least 1.");
        }

        if (!Double.isFinite(extent) || extent < 0.0) {
            throw new IllegalStateException(
                    "calculated extent must be finite and non-negative.");
        }

        if (!Double.isFinite(boundaryRoughness) || boundaryRoughness < 0.0) {
            throw new IllegalStateException(
                    "calculated boundaryRoughness must be finite and "
                            + "non-negative.");
        }
    }

    /**
     * Sums Euclidean distances between consecutive ordered boundary points,
     * including the closing segment from the last point back to the first.
     * Horizontal and vertical steps have length 1; diagonal steps have length
     * {@code sqrt(2)} when boundary points are 8-connected neighbors.
     */
    private static double calculatePerimeter(List<Point> boundaryPixels) {

        int boundarySize = boundaryPixels.size();
        double perimeter = 0.0;

        for (int index = 0; index < boundarySize; index++) {

            Point currentPoint = boundaryPixels.get(index);
            Point nextPoint = boundaryPixels.get((index + 1) % boundarySize);

            perimeter += euclideanDistance(currentPoint, nextPoint);
        }

        return perimeter;
    }

    private static double euclideanDistance(Point left, Point right) {

        double deltaX = left.x - right.x;
        double deltaY = left.y - right.y;

        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    /**
     * {@code 4 * PI * area / (perimeter * perimeter)} with zero perimeter mapped
     * to zero circularity.
     */
    private static double calculateCircularity(int area, double perimeter) {

        if (perimeter == 0.0) {
            return 0.0;
        }

        return 4.0 * Math.PI * area / (perimeter * perimeter);
    }

    /**
     * {@code max(width, height) / min(width, height)} using the lumen bounding
     * box dimensions.
     */
    private static double calculateAspectRatio(Rectangle boundingBox) {

        int width = boundingBox.width;
        int height = boundingBox.height;
        int largerDimension = Math.max(width, height);
        int smallerDimension = Math.min(width, height);

        return (double) largerDimension / (double) smallerDimension;
    }

    /**
     * {@code area / (boundingBoxWidth * boundingBoxHeight)} using a long-based
     * bounding-box area to avoid integer overflow.
     */
    private static double calculateExtent(int area, long boundingBoxArea) {

        return (double) area / (double) boundingBoxArea;
    }

    /**
     * {@code perimeter / (2 * sqrt(PI * area))}, the ratio of the measured
     * closed boundary length to the circumference of a circle with equal area.
     */
    private static double calculateBoundaryRoughness(
            double perimeter,
            int area) {

        double equivalentCircleCircumference =
                2.0 * Math.sqrt(Math.PI * area);

        return perimeter / equivalentCircleCircumference;
    }
}
