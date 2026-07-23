package vesselreconstruction.geometry;

import java.awt.Point;

/**
 * Immutable diagnostic description of a valid artifact arc candidate with
 * endpoint pair A and B and a derived bridge peak P on the forward contour arc.
 *
 * <p>T-shape diagnostic fields are experimental geometry descriptors only. They
 * do not influence candidate ranking, acceptance, or reconstruction.
 */
public final class ArtifactArcCandidate {

    private final int firstBoundaryIndex;
    private final int peakBoundaryIndex;
    private final int secondBoundaryIndex;
    private final Point firstPoint;
    private final Point peakPoint;
    private final Point secondPoint;
    private final int forwardArcPointCount;
    private final double closingLineLength;
    private final double artifactArcLength;
    private final double arcToClosingLineRatio;
    private final double peakDistanceFromClosingLine;
    private final double secondGreatestPeakDistance;
    private final double peakDominanceRatio;
    private final int firstFlankArcPointCount;
    private final int secondFlankArcPointCount;
    private final double firstEndpointDeviationScore;
    private final double peakDeviationScore;
    private final double secondEndpointDeviationScore;
    private final double maximumProfileRiseViolation;
    private final double maximumProfileFallViolation;

    /** Euclidean distance between bridgepoints A and B (attachment mouth width). */
    private final double mouthWidth;

    /** Perpendicular distance from peak P to the finite segment A-B. */
    private final double protrusionDepth;

    /** protrusionDepth / mouthWidth (safe when mouthWidth is zero). */
    private final double depthToMouthRatio;

    /** artifactArcLength / mouthWidth (safe when mouthWidth is zero). */
    private final double arcToMouthRatio;

    /** artifactArcLength / totalContourLength (safe when totalContourLength is zero). */
    private final double relativeContourSpan;

    private final double firstAttachmentTurnDegrees;
    private final double secondAttachmentTurnDegrees;
    private final double minimumAttachmentTurnDegrees;
    private final double meanAttachmentTurnDegrees;
    private final double attachmentTurnBalance;

    private final double outsideWallDirectionDifferenceDegrees;
    private final double outsideWallContinuityScore;

    /**
     * Maximum perpendicular distance of outside tangent samples (aOutside,
     * bOutside) from the infinite wall line through the midpoint of A-B oriented
     * by the average consistently oriented outside-wall direction.
     */
    private final double outsideWallLineDeviation;

    private final double peakArcFraction;
    private final double peakCentrality;
    private final double peakTurnDegrees;

    /**
     * Experimental T-shape resemblance score. Must not be used for ranking,
     * acceptance, reconstruction, or selection decisions.
     */
    private final double tShapeDiagnosticScore;

    public ArtifactArcCandidate(
            int firstBoundaryIndex,
            int peakBoundaryIndex,
            int secondBoundaryIndex,
            Point firstPoint,
            Point peakPoint,
            Point secondPoint,
            int forwardArcPointCount,
            double closingLineLength,
            double artifactArcLength,
            double arcToClosingLineRatio,
            double peakDistanceFromClosingLine,
            double secondGreatestPeakDistance,
            double peakDominanceRatio,
            int firstFlankArcPointCount,
            int secondFlankArcPointCount,
            double firstEndpointDeviationScore,
            double peakDeviationScore,
            double secondEndpointDeviationScore,
            double maximumProfileRiseViolation,
            double maximumProfileFallViolation,
            double mouthWidth,
            double protrusionDepth,
            double depthToMouthRatio,
            double arcToMouthRatio,
            double relativeContourSpan,
            double firstAttachmentTurnDegrees,
            double secondAttachmentTurnDegrees,
            double minimumAttachmentTurnDegrees,
            double meanAttachmentTurnDegrees,
            double attachmentTurnBalance,
            double outsideWallDirectionDifferenceDegrees,
            double outsideWallContinuityScore,
            double outsideWallLineDeviation,
            double peakArcFraction,
            double peakCentrality,
            double peakTurnDegrees,
            double tShapeDiagnosticScore) {

        validateBoundaryIndex(firstBoundaryIndex, "firstBoundaryIndex");
        validateBoundaryIndex(peakBoundaryIndex, "peakBoundaryIndex");
        validateBoundaryIndex(secondBoundaryIndex, "secondBoundaryIndex");

        if (firstBoundaryIndex == peakBoundaryIndex
                || firstBoundaryIndex == secondBoundaryIndex
                || peakBoundaryIndex == secondBoundaryIndex) {
            throw new IllegalArgumentException(
                    "boundary indices must be distinct.");
        }

        if (firstPoint == null || peakPoint == null || secondPoint == null) {
            throw new IllegalArgumentException("boundary points must not be null.");
        }

        validateArcPointCount(forwardArcPointCount, "forwardArcPointCount");
        validateArcPointCount(firstFlankArcPointCount, "firstFlankArcPointCount");
        validateArcPointCount(secondFlankArcPointCount, "secondFlankArcPointCount");

        if (firstFlankArcPointCount + secondFlankArcPointCount - 1 != forwardArcPointCount) {
            throw new IllegalArgumentException(
                    "forwardArcPointCount must equal "
                            + "firstFlankArcPointCount + secondFlankArcPointCount - 1.");
        }

        validateNonNegativeFinite(closingLineLength, "closingLineLength");
        validateNonNegativeFinite(artifactArcLength, "artifactArcLength");
        validateNonNegativeFinite(arcToClosingLineRatio, "arcToClosingLineRatio");
        validateNonNegativeFinite(
                peakDistanceFromClosingLine,
                "peakDistanceFromClosingLine");
        validateNonNegativeFinite(
                secondGreatestPeakDistance,
                "secondGreatestPeakDistance");
        validateNonNegativeFinite(peakDominanceRatio, "peakDominanceRatio");
        validateNonNegativeFinite(
                firstEndpointDeviationScore,
                "firstEndpointDeviationScore");
        validateNonNegativeFinite(peakDeviationScore, "peakDeviationScore");
        validateNonNegativeFinite(
                secondEndpointDeviationScore,
                "secondEndpointDeviationScore");
        validateNonNegativeFinite(
                maximumProfileRiseViolation,
                "maximumProfileRiseViolation");
        validateNonNegativeFinite(
                maximumProfileFallViolation,
                "maximumProfileFallViolation");
        validateNonNegativeFinite(mouthWidth, "mouthWidth");
        validateNonNegativeFinite(protrusionDepth, "protrusionDepth");
        validateNonNegativeFinite(depthToMouthRatio, "depthToMouthRatio");
        validateNonNegativeFinite(arcToMouthRatio, "arcToMouthRatio");
        validateNonNegativeFinite(relativeContourSpan, "relativeContourSpan");
        validateNonNegativeFinite(
                firstAttachmentTurnDegrees,
                "firstAttachmentTurnDegrees");
        validateNonNegativeFinite(
                secondAttachmentTurnDegrees,
                "secondAttachmentTurnDegrees");
        validateNonNegativeFinite(
                minimumAttachmentTurnDegrees,
                "minimumAttachmentTurnDegrees");
        validateNonNegativeFinite(
                meanAttachmentTurnDegrees,
                "meanAttachmentTurnDegrees");
        validateNonNegativeFinite(attachmentTurnBalance, "attachmentTurnBalance");
        validateNonNegativeFinite(
                outsideWallDirectionDifferenceDegrees,
                "outsideWallDirectionDifferenceDegrees");
        validateNonNegativeFinite(
                outsideWallContinuityScore,
                "outsideWallContinuityScore");
        validateNonNegativeFinite(
                outsideWallLineDeviation,
                "outsideWallLineDeviation");
        validateNonNegativeFinite(peakArcFraction, "peakArcFraction");
        validateNonNegativeFinite(peakCentrality, "peakCentrality");
        validateNonNegativeFinite(peakTurnDegrees, "peakTurnDegrees");
        validateNonNegativeFinite(tShapeDiagnosticScore, "tShapeDiagnosticScore");

        this.firstBoundaryIndex = firstBoundaryIndex;
        this.peakBoundaryIndex = peakBoundaryIndex;
        this.secondBoundaryIndex = secondBoundaryIndex;
        this.firstPoint = copyPoint(firstPoint);
        this.peakPoint = copyPoint(peakPoint);
        this.secondPoint = copyPoint(secondPoint);
        this.forwardArcPointCount = forwardArcPointCount;
        this.closingLineLength = closingLineLength;
        this.artifactArcLength = artifactArcLength;
        this.arcToClosingLineRatio = arcToClosingLineRatio;
        this.peakDistanceFromClosingLine = peakDistanceFromClosingLine;
        this.secondGreatestPeakDistance = secondGreatestPeakDistance;
        this.peakDominanceRatio = peakDominanceRatio;
        this.firstFlankArcPointCount = firstFlankArcPointCount;
        this.secondFlankArcPointCount = secondFlankArcPointCount;
        this.firstEndpointDeviationScore = firstEndpointDeviationScore;
        this.peakDeviationScore = peakDeviationScore;
        this.secondEndpointDeviationScore = secondEndpointDeviationScore;
        this.maximumProfileRiseViolation = maximumProfileRiseViolation;
        this.maximumProfileFallViolation = maximumProfileFallViolation;
        this.mouthWidth = mouthWidth;
        this.protrusionDepth = protrusionDepth;
        this.depthToMouthRatio = depthToMouthRatio;
        this.arcToMouthRatio = arcToMouthRatio;
        this.relativeContourSpan = relativeContourSpan;
        this.firstAttachmentTurnDegrees = firstAttachmentTurnDegrees;
        this.secondAttachmentTurnDegrees = secondAttachmentTurnDegrees;
        this.minimumAttachmentTurnDegrees = minimumAttachmentTurnDegrees;
        this.meanAttachmentTurnDegrees = meanAttachmentTurnDegrees;
        this.attachmentTurnBalance = attachmentTurnBalance;
        this.outsideWallDirectionDifferenceDegrees =
                outsideWallDirectionDifferenceDegrees;
        this.outsideWallContinuityScore = outsideWallContinuityScore;
        this.outsideWallLineDeviation = outsideWallLineDeviation;
        this.peakArcFraction = peakArcFraction;
        this.peakCentrality = peakCentrality;
        this.peakTurnDegrees = peakTurnDegrees;
        this.tShapeDiagnosticScore = tShapeDiagnosticScore;
    }

    public int getFirstBoundaryIndex() {
        return firstBoundaryIndex;
    }

    public int getPeakBoundaryIndex() {
        return peakBoundaryIndex;
    }

    public int getSecondBoundaryIndex() {
        return secondBoundaryIndex;
    }

    public Point getFirstPoint() {
        return copyPoint(firstPoint);
    }

    public Point getPeakPoint() {
        return copyPoint(peakPoint);
    }

    public Point getSecondPoint() {
        return copyPoint(secondPoint);
    }

    public int getForwardArcPointCount() {
        return forwardArcPointCount;
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

    public double getSecondGreatestPeakDistance() {
        return secondGreatestPeakDistance;
    }

    public double getPeakDominanceRatio() {
        return peakDominanceRatio;
    }

    public int getFirstFlankArcPointCount() {
        return firstFlankArcPointCount;
    }

    public int getSecondFlankArcPointCount() {
        return secondFlankArcPointCount;
    }

    public double getFirstEndpointDeviationScore() {
        return firstEndpointDeviationScore;
    }

    public double getPeakDeviationScore() {
        return peakDeviationScore;
    }

    public double getSecondEndpointDeviationScore() {
        return secondEndpointDeviationScore;
    }

    public double getMaximumProfileRiseViolation() {
        return maximumProfileRiseViolation;
    }

    public double getMaximumProfileFallViolation() {
        return maximumProfileFallViolation;
    }

    public double getMouthWidth() {
        return mouthWidth;
    }

    public double getProtrusionDepth() {
        return protrusionDepth;
    }

    public double getDepthToMouthRatio() {
        return depthToMouthRatio;
    }

    public double getArcToMouthRatio() {
        return arcToMouthRatio;
    }

    public double getRelativeContourSpan() {
        return relativeContourSpan;
    }

    public double getFirstAttachmentTurnDegrees() {
        return firstAttachmentTurnDegrees;
    }

    public double getSecondAttachmentTurnDegrees() {
        return secondAttachmentTurnDegrees;
    }

    public double getMinimumAttachmentTurnDegrees() {
        return minimumAttachmentTurnDegrees;
    }

    public double getMeanAttachmentTurnDegrees() {
        return meanAttachmentTurnDegrees;
    }

    public double getAttachmentTurnBalance() {
        return attachmentTurnBalance;
    }

    public double getOutsideWallDirectionDifferenceDegrees() {
        return outsideWallDirectionDifferenceDegrees;
    }

    public double getOutsideWallContinuityScore() {
        return outsideWallContinuityScore;
    }

    public double getOutsideWallLineDeviation() {
        return outsideWallLineDeviation;
    }

    public double getPeakArcFraction() {
        return peakArcFraction;
    }

    public double getPeakCentrality() {
        return peakCentrality;
    }

    public double getPeakTurnDegrees() {
        return peakTurnDegrees;
    }

    public double getTShapeDiagnosticScore() {
        return tShapeDiagnosticScore;
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
