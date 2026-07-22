package vesselreconstruction.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Finds local deviation-score peaks on an implicitly circular ordered boundary.
 */
public class DeviationPeakFinder {

    /**
     * Finds boundary indices whose deviation scores form local maxima after
     * thresholding and non-maximum suppression.
     *
     * <p>Scores are treated as an implicitly closed circular sequence. Candidate
     * peaks must exceed {@code minimumRelativeScore} times the array maximum and
     * be the deterministic local maximum within a circular neighborhood of
     * radius {@code suppressionRadius}. Candidates are considered in descending
     * score order, with ascending index as the tie-break. After a peak is
     * accepted, all indices within the suppression radius are excluded from
     * further selection.
     *
     * @param deviationScores raw non-negative deviation scores indexed by
     *                        boundary position
     * @param minimumRelativeScore relative threshold in {@code [0, 1]} applied
     *                             against the maximum score
     * @param suppressionRadius circular neighborhood radius used for local-max
     *                          tests and peak suppression
     * @return accepted peak boundary indices in ascending numeric order
     * @throws IllegalArgumentException if the input is invalid
     */
    public List<Integer> findPeakIndices(
            double[] deviationScores,
            double minimumRelativeScore,
            int suppressionRadius) {

        validateInputs(
                deviationScores,
                minimumRelativeScore,
                suppressionRadius);

        if (deviationScores.length == 0) {
            return new ArrayList<>();
        }

        double maximumScore = maximumScore(deviationScores);

        if (maximumScore == 0.0) {
            return new ArrayList<>();
        }

        double scoreThreshold = minimumRelativeScore * maximumScore;
        boolean[] suppressed = new boolean[deviationScores.length];
        List<Integer> acceptedPeaks = new ArrayList<>();

        Integer[] candidateOrder = candidateIndices(
                deviationScores,
                scoreThreshold);

        Arrays.sort(
                candidateOrder,
                Comparator
                        .comparingDouble(
                                (Integer index) -> deviationScores[index])
                        .reversed()
                        .thenComparingInt(index -> index));

        for (int candidateIndex : candidateOrder) {

            if (suppressed[candidateIndex]) {
                continue;
            }

            if (!isDeterministicLocalMaximum(
                    deviationScores,
                    candidateIndex,
                    suppressionRadius)) {
                continue;
            }

            acceptedPeaks.add(candidateIndex);
            suppressNeighborhood(
                    suppressed,
                    candidateIndex,
                    suppressionRadius);
        }

        acceptedPeaks.sort(Comparator.naturalOrder());

        return acceptedPeaks;
    }

    private static void validateInputs(
            double[] deviationScores,
            double minimumRelativeScore,
            int suppressionRadius) {

        if (deviationScores == null) {
            throw new IllegalArgumentException(
                    "deviationScores must not be null.");
        }

        if (!Double.isFinite(minimumRelativeScore)
                || minimumRelativeScore < 0.0
                || minimumRelativeScore > 1.0) {
            throw new IllegalArgumentException(
                    "minimumRelativeScore must be finite and within [0, 1].");
        }

        if (suppressionRadius < 1) {
            throw new IllegalArgumentException(
                    "suppressionRadius must be at least 1.");
        }

        if (deviationScores.length == 0) {
            return;
        }

        int minimumLength = 2 * suppressionRadius + 1;

        if (deviationScores.length < minimumLength) {
            throw new IllegalArgumentException(
                    "deviationScores must contain at least "
                            + minimumLength
                            + " points for suppressionRadius="
                            + suppressionRadius
                            + ".");
        }

        for (double deviationScore : deviationScores) {

            if (!Double.isFinite(deviationScore) || deviationScore < 0.0) {
                throw new IllegalArgumentException(
                        "deviationScores must contain only finite, "
                                + "non-negative values.");
            }
        }
    }

    private static double maximumScore(double[] deviationScores) {

        double maximumScore = 0.0;

        for (double deviationScore : deviationScores) {

            if (deviationScore > maximumScore) {
                maximumScore = deviationScore;
            }
        }

        return maximumScore;
    }

    private static Integer[] candidateIndices(
            double[] deviationScores,
            double scoreThreshold) {

        List<Integer> candidates = new ArrayList<>();

        for (int index = 0; index < deviationScores.length; index++) {

            if (deviationScores[index] >= scoreThreshold) {
                candidates.add(index);
            }
        }

        return candidates.toArray(new Integer[0]);
    }

    private static boolean isDeterministicLocalMaximum(
            double[] deviationScores,
            int centerIndex,
            int suppressionRadius) {

        int boundarySize = deviationScores.length;
        double centerScore = deviationScores[centerIndex];
        int smallestTiedIndex = centerIndex;

        for (int offset = -suppressionRadius; offset <= suppressionRadius; offset++) {

            int neighborIndex = circularIndex(
                    centerIndex,
                    offset,
                    boundarySize);
            double neighborScore = deviationScores[neighborIndex];

            if (neighborScore > centerScore) {
                return false;
            }

            if (neighborScore == centerScore && neighborIndex < smallestTiedIndex) {
                smallestTiedIndex = neighborIndex;
            }
        }

        return smallestTiedIndex == centerIndex;
    }

    private static void suppressNeighborhood(
            boolean[] suppressed,
            int centerIndex,
            int suppressionRadius) {

        int boundarySize = suppressed.length;

        for (int offset = -suppressionRadius; offset <= suppressionRadius; offset++) {
            suppressed[circularIndex(centerIndex, offset, boundarySize)] = true;
        }
    }

    private static int circularIndex(int centerIndex, int offset, int boundarySize) {

        int index = (centerIndex + offset) % boundarySize;

        if (index < 0) {
            index += boundarySize;
        }

        return index;
    }
}
