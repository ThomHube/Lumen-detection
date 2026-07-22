package vesselreconstruction.geometry;

/**
 * Evaluates {@link LumenFeatures} against configurable diagnostic criteria.
 *
 * <p>This class performs calculation only. It does not modify or remove any
 * lumen objects.
 */
public class LumenFilter {

    /**
     * Evaluates one feature set against the supplied criteria.
     *
     * @param features calculated lumen features
     * @param criteria immutable filter thresholds
     * @return immutable evaluation outcome with all rejection flags populated
     * @throws IllegalArgumentException if {@code features} or {@code criteria}
     *                                  is null
     */
    public LumenFilterResult evaluate(
            LumenFeatures features,
            LumenFilterCriteria criteria) {

        if (features == null) {
            throw new IllegalArgumentException("features must not be null.");
        }

        if (criteria == null) {
            throw new IllegalArgumentException("criteria must not be null.");
        }

        boolean rejectedByArea =
                features.getArea() < criteria.getMinimumArea();
        boolean rejectedByCircularity =
                features.getCircularity() < criteria.getMinimumCircularity();
        boolean rejectedByAspectRatio =
                features.getAspectRatio() > criteria.getMaximumAspectRatio();
        boolean rejectedByExtent =
                features.getExtent() < criteria.getMinimumExtent();
        boolean rejectedByBoundaryRoughness =
                features.getBoundaryRoughness()
                        > criteria.getMaximumBoundaryRoughness();
        boolean rejectedByBorderContact =
                criteria.isRejectBorderTouching()
                        && features.isTouchesBorder();

        return new LumenFilterResult(
                rejectedByArea,
                rejectedByCircularity,
                rejectedByAspectRatio,
                rejectedByExtent,
                rejectedByBoundaryRoughness,
                rejectedByBorderContact);
    }
}
