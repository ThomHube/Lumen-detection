package vesselreconstruction.geometry;

import java.awt.Point;

/**
 * Immutable diagnostic description of a valid artifact bridge triplet:
 * first bridge point, bridge peak, and second bridge point along an ordered
 * circular boundary arc.
 */
public final class ArtifactBridgeTriplet {

    private final int firstBridgeBoundaryIndex;
    private final int bridgePeakBoundaryIndex;
    private final int secondBridgeBoundaryIndex;
    private final Point firstBridgePoint;
    private final Point bridgePeakPoint;
    private final Point secondBridgePoint;
    private final int firstToPeakArcPointCount;
    private final int peakToSecondArcPointCount;
    private final int totalArtifactArcPointCount;
    private final double closingLineLength;
    private final double artifactArcLength;
    private final double arcToClosingLineRatio;
    private final double peakDistanceFromClosingLine;
    private final double firstBridgeDeviationScore;
    private final double bridgePeakDeviationScore;
    private final double secondBridgeDeviationScore;

    public ArtifactBridgeTriplet(
            int firstBridgeBoundaryIndex,
            int bridgePeakBoundaryIndex,
            int secondBridgeBoundaryIndex,
            Point firstBridgePoint,
            Point bridgePeakPoint,
            Point secondBridgePoint,
            int firstToPeakArcPointCount,
            int peakToSecondArcPointCount,
            int totalArtifactArcPointCount,
            double closingLineLength,
            double artifactArcLength,
            double arcToClosingLineRatio,
            double peakDistanceFromClosingLine,
            double firstBridgeDeviationScore,
            double bridgePeakDeviationScore,
            double secondBridgeDeviationScore) {

        validateBoundaryIndex(
                firstBridgeBoundaryIndex,
                "firstBridgeBoundaryIndex");
        validateBoundaryIndex(
                bridgePeakBoundaryIndex,
                "bridgePeakBoundaryIndex");
        validateBoundaryIndex(
                secondBridgeBoundaryIndex,
                "secondBridgeBoundaryIndex");

        if (firstBridgeBoundaryIndex == bridgePeakBoundaryIndex
                || firstBridgeBoundaryIndex == secondBridgeBoundaryIndex
                || bridgePeakBoundaryIndex == secondBridgeBoundaryIndex) {
            throw new IllegalArgumentException(
                    "boundary indices must be distinct.");
        }

        if (firstBridgePoint == null
                || bridgePeakPoint == null
                || secondBridgePoint == null) {
            throw new IllegalArgumentException("boundary points must not be null.");
        }

        validateArcPointCount(
                firstToPeakArcPointCount,
                "firstToPeakArcPointCount");
        validateArcPointCount(
                peakToSecondArcPointCount,
                "peakToSecondArcPointCount");
        validateArcPointCount(
                totalArtifactArcPointCount,
                "totalArtifactArcPointCount");

        if (firstToPeakArcPointCount + peakToSecondArcPointCount - 1
                != totalArtifactArcPointCount) {
            throw new IllegalArgumentException(
                    "totalArtifactArcPointCount must equal "
                            + "firstToPeakArcPointCount + peakToSecondArcPointCount - 1.");
        }

        validateNonNegativeFinite(closingLineLength, "closingLineLength");
        validateNonNegativeFinite(artifactArcLength, "artifactArcLength");
        validateNonNegativeFinite(arcToClosingLineRatio, "arcToClosingLineRatio");
        validateNonNegativeFinite(
                peakDistanceFromClosingLine,
                "peakDistanceFromClosingLine");
        validateNonNegativeFinite(
                firstBridgeDeviationScore,
                "firstBridgeDeviationScore");
        validateNonNegativeFinite(
                bridgePeakDeviationScore,
                "bridgePeakDeviationScore");
        validateNonNegativeFinite(
                secondBridgeDeviationScore,
                "secondBridgeDeviationScore");

        this.firstBridgeBoundaryIndex = firstBridgeBoundaryIndex;
        this.bridgePeakBoundaryIndex = bridgePeakBoundaryIndex;
        this.secondBridgeBoundaryIndex = secondBridgeBoundaryIndex;
        this.firstBridgePoint = copyPoint(firstBridgePoint);
        this.bridgePeakPoint = copyPoint(bridgePeakPoint);
        this.secondBridgePoint = copyPoint(secondBridgePoint);
        this.firstToPeakArcPointCount = firstToPeakArcPointCount;
        this.peakToSecondArcPointCount = peakToSecondArcPointCount;
        this.totalArtifactArcPointCount = totalArtifactArcPointCount;
        this.closingLineLength = closingLineLength;
        this.artifactArcLength = artifactArcLength;
        this.arcToClosingLineRatio = arcToClosingLineRatio;
        this.peakDistanceFromClosingLine = peakDistanceFromClosingLine;
        this.firstBridgeDeviationScore = firstBridgeDeviationScore;
        this.bridgePeakDeviationScore = bridgePeakDeviationScore;
        this.secondBridgeDeviationScore = secondBridgeDeviationScore;
    }

    public int getFirstBridgeBoundaryIndex() {
        return firstBridgeBoundaryIndex;
    }

    public int getBridgePeakBoundaryIndex() {
        return bridgePeakBoundaryIndex;
    }

    public int getSecondBridgeBoundaryIndex() {
        return secondBridgeBoundaryIndex;
    }

    public Point getFirstBridgePoint() {
        return copyPoint(firstBridgePoint);
    }

    public Point getBridgePeakPoint() {
        return copyPoint(bridgePeakPoint);
    }

    public Point getSecondBridgePoint() {
        return copyPoint(secondBridgePoint);
    }

    public int getFirstToPeakArcPointCount() {
        return firstToPeakArcPointCount;
    }

    public int getPeakToSecondArcPointCount() {
        return peakToSecondArcPointCount;
    }

    public int getTotalArtifactArcPointCount() {
        return totalArtifactArcPointCount;
    }

    public double getClosingLineLength() {
        return closingLineLength;
    }

    public double getArtifactArcLength() {
        return artifactArcLength;
    }

    public double getArcToClosingLineRatio() {
        return arcToClosingLineRatio;
    }

    public double getPeakDistanceFromClosingLine() {
        return peakDistanceFromClosingLine;
    }

    public double getFirstBridgeDeviationScore() {
        return firstBridgeDeviationScore;
    }

    public double getBridgePeakDeviationScore() {
        return bridgePeakDeviationScore;
    }

    public double getSecondBridgeDeviationScore() {
        return secondBridgeDeviationScore;
    }

    private static void validateBoundaryIndex(int boundaryIndex, String fieldName) {

        if (boundaryIndex < 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be non-negative.");
        }
    }

    private static void validateArcPointCount(int arcPointCount, String fieldName) {

        if (arcPointCount < 2) {
            throw new IllegalArgumentException(
                    fieldName + " must be at least 2.");
        }
    }

    private static void validateNonNegativeFinite(double value, String fieldName) {

        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    fieldName + " must be finite.");
        }

        if (value < 0.0) {
            throw new IllegalArgumentException(
                    fieldName + " must be non-negative.");
        }
    }

    private static Point copyPoint(Point point) {
        return new Point(point.x, point.y);
    }
}
