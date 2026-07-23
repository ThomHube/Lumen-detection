package vesselreconstruction.geometry;

/**
 * Immutable geometric feature summary for a finished lumen candidate.
 *
 * <p>All numeric values are finite and non-negative except where noted in the
 * calculator validation rules.
 */
public final class LumenFeatures {

    private final int lumenId;
    private final int area;
    private final double perimeter;
    private final double circularity;
    private final double aspectRatio;
    private final double extent;
    private final double boundaryRoughness;
    private final boolean touchesBorder;

    public LumenFeatures(
            int lumenId,
            int area,
            double perimeter,
            double circularity,
            double aspectRatio,
            double extent,
            double boundaryRoughness,
            boolean touchesBorder) {

        this.lumenId = lumenId;
        this.area = area;
        this.perimeter = perimeter;
        this.circularity = circularity;
        this.aspectRatio = aspectRatio;
        this.extent = extent;
        this.boundaryRoughness = boundaryRoughness;
        this.touchesBorder = touchesBorder;
    }

    public int getLumenId() {
        return lumenId;
    }

    /**
     * Pixel count of the lumen foreground region.
     */
    public int getArea() {
        return area;
    }

    /**
     * Closed boundary length in pixels. Each consecutive ordered boundary point
     * contributes its Euclidean step length, including the closing segment from
     * the last point back to the first.
     */
    public double getPerimeter() {
        return perimeter;
    }

    /**
     * {@code 4 * PI * area / (perimeter * perimeter)}. Equals zero when
     * {@code perimeter == 0}.
     */
    public double getCircularity() {
        return circularity;
    }

    /**
     * {@code max(boundingBoxWidth, boundingBoxHeight)
     * / min(boundingBoxWidth, boundingBoxHeight)}. Always at least 1.
     */
    public double getAspectRatio() {
        return aspectRatio;
    }

    /**
     * {@code area / (boundingBoxWidth * boundingBoxHeight)}.
     */
    public double getExtent() {
        return extent;
    }

    /**
     * {@code perimeter / (2 * sqrt(PI * area))}, comparing the measured closed
     * boundary length to the circumference of a circle with equal area. Compact
     * smooth objects are near 1; irregular regions are generally larger.
     */
    public double getBoundaryRoughness() {
        return boundaryRoughness;
    }

    public boolean isTouchesBorder() {
        return touchesBorder;
    }
}
