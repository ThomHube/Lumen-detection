package vesselreconstruction.geometry;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LumenFilterTest {

    private static final double TOLERANCE = 1.0e-9;

    private final LumenFilter filter = new LumenFilter();

    @Test
    public void resultWithNoRejectionFlagsIsAccepted() {

        LumenFilterResult result = new LumenFilterResult(
                false,
                false,
                false,
                false,
                false,
                false);

        assertTrue(result.isAccepted());
    }

    @Test
    public void resultWithAnyRejectionFlagIsRejected() {

        assertFalse(new LumenFilterResult(
                true, false, false, false, false, false).isAccepted());
        assertFalse(new LumenFilterResult(
                false, true, false, false, false, false).isAccepted());
        assertFalse(new LumenFilterResult(
                false, false, true, false, false, false).isAccepted());
        assertFalse(new LumenFilterResult(
                false, false, false, true, false, false).isAccepted());
        assertFalse(new LumenFilterResult(
                false, false, false, false, true, false).isAccepted());
        assertFalse(new LumenFilterResult(
                false, false, false, false, false, true).isAccepted());
    }

    @Test
    public void passThroughAcceptsValidFeatureSet() {

        LumenFeatures features = features(
                1,
                100,
                0.50,
                3.00,
                0.20,
                4.00,
                true);
        LumenFilterCriteria criteria = LumenFilterCriteria.passThrough();

        LumenFilterResult result = filter.evaluate(features, criteria);

        assertTrue(result.isAccepted());
        assertTrue(result.getRejectionReasons().isEmpty());
    }

    @Test
    public void exploratoryDefaultsAcceptObjectPassingAllCriteria() {

        LumenFeatures features = features(
                2,
                300,
                0.35,
                1.80,
                0.50,
                2.00,
                false);
        LumenFilterCriteria criteria = LumenFilterCriteria.exploratoryDefaults();

        LumenFilterResult result = filter.evaluate(features, criteria);

        assertTrue(result.isAccepted());
        assertTrue(result.getRejectionReasons().isEmpty());
    }

    @Test
    public void areaCriterionRejectsIndependently() {

        LumenFilterCriteria criteria = singleCriterionCriteria(
                minimumArea(300),
                minimumCircularity(0.0),
                maximumAspectRatio(Double.MAX_VALUE),
                minimumExtent(0.0),
                maximumBoundaryRoughness(Double.MAX_VALUE),
                false);

        LumenFilterResult result = filter.evaluate(
                features(3, 299, 1.0, 1.0, 1.0, 1.0, false),
                criteria);

        assertFalse(result.isAccepted());
        assertTrue(result.isRejectedByArea());
        assertEquals(Arrays.asList("AREA"), result.getRejectionReasons());
    }

    @Test
    public void circularityCriterionRejectsIndependently() {

        LumenFilterCriteria criteria = singleCriterionCriteria(
                minimumArea(0),
                minimumCircularity(0.40),
                maximumAspectRatio(Double.MAX_VALUE),
                minimumExtent(0.0),
                maximumBoundaryRoughness(Double.MAX_VALUE),
                false);

        LumenFilterResult result = filter.evaluate(
                features(4, 500, 0.39, 1.0, 1.0, 1.0, false),
                criteria);

        assertFalse(result.isAccepted());
        assertTrue(result.isRejectedByCircularity());
        assertEquals(Arrays.asList("CIRCULARITY"), result.getRejectionReasons());
    }

    @Test
    public void aspectRatioCriterionRejectsIndependently() {

        LumenFilterCriteria criteria = singleCriterionCriteria(
                minimumArea(0),
                minimumCircularity(0.0),
                maximumAspectRatio(2.0),
                minimumExtent(0.0),
                maximumBoundaryRoughness(Double.MAX_VALUE),
                false);

        LumenFilterResult result = filter.evaluate(
                features(5, 500, 1.0, 2.01, 1.0, 1.0, false),
                criteria);

        assertFalse(result.isAccepted());
        assertTrue(result.isRejectedByAspectRatio());
        assertEquals(
                Arrays.asList("ASPECT_RATIO"),
                result.getRejectionReasons());
    }

    @Test
    public void extentCriterionRejectsIndependently() {

        LumenFilterCriteria criteria = singleCriterionCriteria(
                minimumArea(0),
                minimumCircularity(0.0),
                maximumAspectRatio(Double.MAX_VALUE),
                minimumExtent(0.50),
                maximumBoundaryRoughness(Double.MAX_VALUE),
                false);

        LumenFilterResult result = filter.evaluate(
                features(6, 500, 1.0, 1.0, 0.49, 1.0, false),
                criteria);

        assertFalse(result.isAccepted());
        assertTrue(result.isRejectedByExtent());
        assertEquals(Arrays.asList("EXTENT"), result.getRejectionReasons());
    }

    @Test
    public void boundaryRoughnessCriterionRejectsIndependently() {

        LumenFilterCriteria criteria = singleCriterionCriteria(
                minimumArea(0),
                minimumCircularity(0.0),
                maximumAspectRatio(Double.MAX_VALUE),
                minimumExtent(0.0),
                maximumBoundaryRoughness(2.0),
                false);

        LumenFilterResult result = filter.evaluate(
                features(7, 500, 1.0, 1.0, 1.0, 2.01, false),
                criteria);

        assertFalse(result.isAccepted());
        assertTrue(result.isRejectedByBoundaryRoughness());
        assertEquals(
                Arrays.asList("BOUNDARY_ROUGHNESS"),
                result.getRejectionReasons());
    }

    @Test
    public void multipleRejectionReasonsAreAllReported() {

        LumenFilterCriteria criteria = LumenFilterCriteria.exploratoryDefaults();

        LumenFilterResult result = filter.evaluate(
                features(8, 100, 0.10, 3.00, 0.10, 4.00, true),
                criteria);

        assertFalse(result.isAccepted());
        assertTrue(result.isRejectedByArea());
        assertTrue(result.isRejectedByCircularity());
        assertTrue(result.isRejectedByAspectRatio());
        assertTrue(result.isRejectedByExtent());
        assertTrue(result.isRejectedByBoundaryRoughness());
        assertFalse(result.isRejectedByBorderContact());
        assertEquals(
                Arrays.asList(
                        "AREA",
                        "CIRCULARITY",
                        "ASPECT_RATIO",
                        "EXTENT",
                        "BOUNDARY_ROUGHNESS"),
                result.getRejectionReasons());
    }

    @Test
    public void rejectionReasonOrderIsDeterministic() {

        LumenFilterCriteria criteria = new LumenFilterCriteria(
                500,
                0.80,
                1.50,
                0.90,
                1.20,
                true);

        LumenFilterResult result = filter.evaluate(
                features(9, 100, 0.10, 3.00, 0.10, 4.00, true),
                criteria);

        assertEquals(
                Arrays.asList(
                        "AREA",
                        "CIRCULARITY",
                        "ASPECT_RATIO",
                        "EXTENT",
                        "BOUNDARY_ROUGHNESS",
                        "BORDER_CONTACT"),
                result.getRejectionReasons());
    }

    @Test
    public void exactThresholdEqualityPasses() {

        LumenFilterCriteria criteria = LumenFilterCriteria.exploratoryDefaults();

        LumenFilterResult result = filter.evaluate(
                features(10, 250, 0.30, 2.00, 0.40, 2.50, false),
                criteria);

        assertTrue(result.isAccepted());
        assertTrue(result.getRejectionReasons().isEmpty());
    }

    @Test
    public void borderContactIgnoredWhenDisabled() {

        LumenFilterCriteria criteria = new LumenFilterCriteria(
                0,
                0.0,
                Double.MAX_VALUE,
                0.0,
                Double.MAX_VALUE,
                false);

        LumenFilterResult result = filter.evaluate(
                features(11, 500, 1.0, 1.0, 1.0, 1.0, true),
                criteria);

        assertTrue(result.isAccepted());
        assertFalse(result.isRejectedByBorderContact());
    }

    @Test
    public void borderContactRejectsWhenEnabled() {

        LumenFilterCriteria criteria = new LumenFilterCriteria(
                0,
                0.0,
                Double.MAX_VALUE,
                0.0,
                Double.MAX_VALUE,
                true);

        LumenFilterResult result = filter.evaluate(
                features(12, 500, 1.0, 1.0, 1.0, 1.0, true),
                criteria);

        assertFalse(result.isAccepted());
        assertTrue(result.isRejectedByBorderContact());
        assertEquals(
                Arrays.asList("BORDER_CONTACT"),
                result.getRejectionReasons());
    }

    @Test
    public void nullFeaturesRejected() {

        try {
            filter.evaluate(null, LumenFilterCriteria.passThrough());
        } catch (IllegalArgumentException exception) {
            assertEquals("features must not be null.", exception.getMessage());
            return;
        }

        throw new AssertionError("Expected IllegalArgumentException.");
    }

    @Test
    public void nullCriteriaRejected() {

        LumenFeatures features = features(
                13,
                500,
                1.0,
                1.0,
                1.0,
                1.0,
                false);

        try {
            filter.evaluate(features, null);
        } catch (IllegalArgumentException exception) {
            assertEquals("criteria must not be null.", exception.getMessage());
            return;
        }

        throw new AssertionError("Expected IllegalArgumentException.");
    }

    @Test
    public void invalidCriteriaValuesRejected() {

        assertInvalidCriteria(
                -1,
                0.0,
                2.0,
                0.0,
                2.0,
                "minimumArea must be at least 0.");
        assertInvalidCriteria(
                0,
                Double.NaN,
                2.0,
                0.0,
                2.0,
                "minimumCircularity must be finite and non-negative.");
        assertInvalidCriteria(
                0,
                0.0,
                0.5,
                0.0,
                2.0,
                "maximumAspectRatio must be finite and at least 1.");
        assertInvalidCriteria(
                0,
                0.0,
                2.0,
                Double.POSITIVE_INFINITY,
                2.0,
                "minimumExtent must be finite and non-negative.");
        assertInvalidCriteria(
                0,
                0.0,
                2.0,
                0.0,
                -1.0,
                "maximumBoundaryRoughness must be finite and non-negative.");
    }

    @Test
    public void resultAndReasonListAreImmutable() {

        LumenFilterResult result = filter.evaluate(
                features(14, 100, 0.10, 3.00, 0.10, 4.00, true),
                LumenFilterCriteria.exploratoryDefaults());
        List<String> rejectionReasons = result.getRejectionReasons();

        try {
            rejectionReasons.add("MUTATION");
        } catch (UnsupportedOperationException exception) {
            assertEquals(
                    Arrays.asList(
                            "AREA",
                            "CIRCULARITY",
                            "ASPECT_RATIO",
                            "EXTENT",
                            "BOUNDARY_ROUGHNESS"),
                    result.getRejectionReasons());
            return;
        }

        throw new AssertionError(
                "Expected UnsupportedOperationException for reason list.");
    }

    @Test
    public void inputObjectsRemainUnchanged() {

        LumenFeatures features = features(
                15,
                300,
                0.35,
                1.80,
                0.50,
                2.00,
                false);
        LumenFilterCriteria criteria = LumenFilterCriteria.exploratoryDefaults();

        int areaBefore = features.getArea();
        double circularityBefore = features.getCircularity();
        double aspectRatioBefore = features.getAspectRatio();
        double extentBefore = features.getExtent();
        double boundaryRoughnessBefore = features.getBoundaryRoughness();
        boolean touchesBorderBefore = features.isTouchesBorder();
        int minimumAreaBefore = criteria.getMinimumArea();
        double minimumCircularityBefore = criteria.getMinimumCircularity();
        double maximumAspectRatioBefore = criteria.getMaximumAspectRatio();
        double minimumExtentBefore = criteria.getMinimumExtent();
        double maximumBoundaryRoughnessBefore =
                criteria.getMaximumBoundaryRoughness();
        boolean rejectBorderTouchingBefore = criteria.isRejectBorderTouching();

        filter.evaluate(features, criteria);

        assertEquals(areaBefore, features.getArea());
        assertEquals(circularityBefore, features.getCircularity(), TOLERANCE);
        assertEquals(aspectRatioBefore, features.getAspectRatio(), TOLERANCE);
        assertEquals(extentBefore, features.getExtent(), TOLERANCE);
        assertEquals(
                boundaryRoughnessBefore,
                features.getBoundaryRoughness(),
                TOLERANCE);
        assertEquals(touchesBorderBefore, features.isTouchesBorder());
        assertEquals(minimumAreaBefore, criteria.getMinimumArea());
        assertEquals(
                minimumCircularityBefore,
                criteria.getMinimumCircularity(),
                TOLERANCE);
        assertEquals(
                maximumAspectRatioBefore,
                criteria.getMaximumAspectRatio(),
                TOLERANCE);
        assertEquals(minimumExtentBefore, criteria.getMinimumExtent(), TOLERANCE);
        assertEquals(
                maximumBoundaryRoughnessBefore,
                criteria.getMaximumBoundaryRoughness(),
                TOLERANCE);
        assertEquals(
                rejectBorderTouchingBefore,
                criteria.isRejectBorderTouching());
    }

    private static LumenFeatures features(
            int lumenId,
            int area,
            double circularity,
            double aspectRatio,
            double extent,
            double boundaryRoughness,
            boolean touchesBorder) {

        return new LumenFeatures(
                lumenId,
                area,
                10.0,
                circularity,
                aspectRatio,
                extent,
                boundaryRoughness,
                touchesBorder);
    }

    private static LumenFilterCriteria singleCriterionCriteria(
            int minimumArea,
            double minimumCircularity,
            double maximumAspectRatio,
            double minimumExtent,
            double maximumBoundaryRoughness,
            boolean rejectBorderTouching) {

        return new LumenFilterCriteria(
                minimumArea,
                minimumCircularity,
                maximumAspectRatio,
                minimumExtent,
                maximumBoundaryRoughness,
                rejectBorderTouching);
    }

    private static int minimumArea(int value) {
        return value;
    }

    private static double minimumCircularity(double value) {
        return value;
    }

    private static double maximumAspectRatio(double value) {
        return value;
    }

    private static double minimumExtent(double value) {
        return value;
    }

    private static double maximumBoundaryRoughness(double value) {
        return value;
    }

    private static void assertInvalidCriteria(
            int minimumArea,
            double minimumCircularity,
            double maximumAspectRatio,
            double minimumExtent,
            double maximumBoundaryRoughness,
            String expectedMessage) {

        try {
            new LumenFilterCriteria(
                    minimumArea,
                    minimumCircularity,
                    maximumAspectRatio,
                    minimumExtent,
                    maximumBoundaryRoughness,
                    false);
        } catch (IllegalArgumentException exception) {
            assertEquals(expectedMessage, exception.getMessage());
            return;
        }

        throw new AssertionError(
                "Expected IllegalArgumentException: " + expectedMessage);
    }
}
