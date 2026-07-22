package vesselreconstruction.geometry;

import java.awt.Point;
import java.util.List;

/**
 * Computes local boundary deviation scores along an ordered vessel-wall contour.
 */
public class ImaginaryRuler {

    private static final double COINCIDENT_CENTROID_TOLERANCE = 1.0e-12;

    /**
     * Calculates a raw deviation score for every point on an ordered,
     * implicitly closed boundary contour.
     *
     * <p>For each boundary point {@code i}, the score measures how far that point
     * deviates from the locally expected vessel-wall direction. Two circular
     * windows of {@code neighbourCount} points on either side of {@code i}
     * (excluding {@code i}) define centroids {@code C_prev} and {@code C_next}.
     * The line through those centroids represents the expected local wall
     * direction. The score is the perpendicular Euclidean distance, in pixels,
     * from point {@code i} to that line. When {@code C_prev} and {@code C_next}
     * coincide, the score is instead the Euclidean distance from point
     * {@code i} to their common location.
     *
     * @param boundaryPoints ordered, implicitly closed boundary coordinates
     * @param neighbourCount number of boundary points on each side of the
     *                       current point used to form the local windows
     * @return one non-negative, finite deviation score per boundary point,
     *         where output index {@code i} corresponds to
     *         {@code boundaryPoints.get(i)}
     * @throws IllegalArgumentException if the input is invalid
     */
    public double[] calculateDeviationScores(
            List<Point> boundaryPoints,
            int neighbourCount) {

        validateInputs(boundaryPoints, neighbourCount);

        int boundarySize = boundaryPoints.size();
        double[] deviationScores = new double[boundarySize];

        for (int index = 0; index < boundarySize; index++) {

            Point currentPoint = boundaryPoints.get(index);

            double[] previousCentroid =
                    windowCentroid(boundaryPoints, index, neighbourCount, true);
            double[] nextCentroid =
                    windowCentroid(boundaryPoints, index, neighbourCount, false);

            deviationScores[index] = deviationScore(
                    currentPoint.x,
                    currentPoint.y,
                    previousCentroid[0],
                    previousCentroid[1],
                    nextCentroid[0],
                    nextCentroid[1]);
        }

        return deviationScores;
    }

    private static void validateInputs(
            List<Point> boundaryPoints,
            int neighbourCount) {

        if (boundaryPoints == null) {
            throw new IllegalArgumentException("boundaryPoints must not be null.");
        }

        if (neighbourCount < 1) {
            throw new IllegalArgumentException(
                    "neighbourCount must be at least 1.");
        }

        int minimumBoundarySize = 2 * neighbourCount + 1;

        if (boundaryPoints.size() < minimumBoundarySize) {
            throw new IllegalArgumentException(
                    "boundaryPoints must contain at least "
                            + minimumBoundarySize
                            + " points for neighbourCount="
                            + neighbourCount
                            + ".");
        }

        for (int index = 0; index < boundaryPoints.size(); index++) {

            if (boundaryPoints.get(index) == null) {
                throw new IllegalArgumentException(
                        "boundaryPoints must not contain null points.");
            }
        }
    }

    private static double[] windowCentroid(
            List<Point> boundaryPoints,
            int centerIndex,
            int neighbourCount,
            boolean previousWindow) {

        double sumX = 0.0;
        double sumY = 0.0;

        for (int offset = 1; offset <= neighbourCount; offset++) {

            int windowOffset = previousWindow ? -offset : offset;
            Point windowPoint = boundaryPoints.get(
                    circularIndex(centerIndex, windowOffset, boundaryPoints.size()));

            sumX += windowPoint.x;
            sumY += windowPoint.y;
        }

        return new double[] {
                sumX / neighbourCount,
                sumY / neighbourCount
        };
    }

    private static int circularIndex(int centerIndex, int offset, int boundarySize) {

        int index = (centerIndex + offset) % boundarySize;

        if (index < 0) {
            index += boundarySize;
        }

        return index;
    }

    private static double deviationScore(
            double pointX,
            double pointY,
            double previousCentroidX,
            double previousCentroidY,
            double nextCentroidX,
            double nextCentroidY) {

        double centroidDeltaX = nextCentroidX - previousCentroidX;
        double centroidDeltaY = nextCentroidY - previousCentroidY;
        double centroidSeparationSquared =
                centroidDeltaX * centroidDeltaX + centroidDeltaY * centroidDeltaY;

        if (centroidSeparationSquared <= COINCIDENT_CENTROID_TOLERANCE) {
            return Math.hypot(
                    pointX - previousCentroidX,
                    pointY - previousCentroidY);
        }

        double crossProduct = Math.abs(
                (pointX - previousCentroidX) * centroidDeltaY
                        - (pointY - previousCentroidY) * centroidDeltaX);

        return crossProduct / Math.sqrt(centroidSeparationSquared);
    }
}
