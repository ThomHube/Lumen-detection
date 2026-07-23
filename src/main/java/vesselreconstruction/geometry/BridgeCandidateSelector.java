package vesselreconstruction.geometry;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Selects one deterministic best bridge candidate from a candidate list.
 */
public class BridgeCandidateSelector {

    private static final Comparator<BridgeCandidate> BEST_CANDIDATE_ORDER =
            Comparator
                    .comparingDouble(BridgeCandidate::getEuclideanDistance)
                    .thenComparing(
                            BridgeCandidate::getCircularBoundarySeparation,
                            Comparator.reverseOrder())
                    .thenComparingInt(BridgeCandidate::getFirstBoundaryIndex)
                    .thenComparingInt(BridgeCandidate::getSecondBoundaryIndex);

    /**
     * Selects the best bridge candidate using deterministic geometric ordering.
     *
     * @param candidates bridge candidates, not necessarily pre-sorted
     * @return the selected candidate, or empty when none are available
     * @throws IllegalArgumentException if {@code candidates} or any entry is
     *                                  null
     */
    public Optional<BridgeCandidate> selectBest(List<BridgeCandidate> candidates) {

        if (candidates == null) {
            throw new IllegalArgumentException("candidates must not be null.");
        }

        for (BridgeCandidate candidate : candidates) {

            if (candidate == null) {
                throw new IllegalArgumentException(
                        "candidates must not contain null values.");
            }
        }

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        BridgeCandidate bestCandidate = candidates.get(0);

        for (int index = 1; index < candidates.size(); index++) {

            BridgeCandidate candidate = candidates.get(index);

            if (BEST_CANDIDATE_ORDER.compare(candidate, bestCandidate) < 0) {
                bestCandidate = candidate;
            }
        }

        return Optional.of(bestCandidate);
    }
}
