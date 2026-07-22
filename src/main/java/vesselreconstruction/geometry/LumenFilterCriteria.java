package vesselreconstruction.geometry;

/**
 * Immutable threshold configuration for exploratory lumen filtering.
 *
 * <p>These values are diagnostic infrastructure only and are not final
 * scientific thresholds.
 */
public final class LumenFilterCriteria {

    private final int minimumArea;
    private final double minimumCircularity;
    private final double maximumAspectRatio;
    private final double minimumExtent;
    private final double maximumBoundaryRoughness;
    private final boolean rejectBorderTouching;

    public LumenFilterCriteria(
            int minimumArea,
            double minimumCircularity,
            double maximumAspectRatio,
            double minimumExtent,
            double maximumBoundaryRoughness,
            boolean rejectBorderTouching) {

        validate(minimumArea,
                minimumCircularity,
                maximumAspectRatio,
                minimumExtent,
                maximumBoundaryRoughness);

        this.minimumArea = minimumArea;
        this.minimumCircularity = minimumCircularity;
        this.maximumAspectRatio = maximumAspectRatio;
        this.minimumExtent = minimumExtent;
        this.maximumBoundaryRoughness = maximumBoundaryRoughness;
        this.rejectBorderTouching = rejectBorderTouching;
    }

    /**
     * Returns a pass-through configuration that accepts every valid
     * {@link LumenFeatures} object, including border-touching objects.
     */
    public static LumenFilterCriteria passThrough() {

        return new LumenFilterCriteria(
                0,
                0.0,
                Double.MAX_VALUE,
                0.0,
                Double.MAX_VALUE,
                false);
    }

    /**
     * Returns an exploratory diagnostic configuration. These values are not
     * final scientific thresholds and must be tuned against real-image data.
     */
    public static LumenFilterCriteria exploratoryDefaults() {

        return new LumenFilterCriteria(
                250,
                0.30,
                2.00,
                0.40,
                2.50,
                false);
    }

    public int getMinimumArea() {
        return minimumArea;
    }

    public double getMinimumCircularity() {
        return minimumCircularity;
    }

    public double getMaximumAspectRatio() {
        return maximumAspectRatio;
    }

    public double getMinimumExtent() {
        return minimumExtent;
    }

    public double getMaximumBoundaryRoughness() {
        return maximumBoundaryRoughness;
    }

    public boolean isRejectBorderTouching() {
        return rejectBorderTouching;
    }

    private static void validate(
            int minimumArea,
            double minimumCircularity,
            double maximumAspectRatio,
            double minimumExtent,
            double maximumBoundaryRoughness) {

        if (minimumArea < 0) {
            throw new IllegalArgumentException(
                    "minimumArea must be at least 0.");
        }

        if (!Double.isFinite(minimumCircularity) || minimumCircularity < 0.0) {
            throw new IllegalArgumentException(
                    "minimumCircularity must be finite and non-negative.");
        }

        if (!Double.isFinite(maximumAspectRatio) || maximumAspectRatio < 1.0) {
            throw new IllegalArgumentException(
                    "maximumAspectRatio must be finite and at least 1.");
        }

        if (!Double.isFinite(minimumExtent) || minimumExtent < 0.0) {
            throw new IllegalArgumentException(
                    "minimumExtent must be finite and non-negative.");
        }

        if (!Double.isFinite(maximumBoundaryRoughness)
                || maximumBoundaryRoughness < 0.0) {
            throw new IllegalArgumentException(
                    "maximumBoundaryRoughness must be finite and "
                            + "non-negative.");
        }
    }
}
