package vesselreconstruction.detection;

import vesselreconstruction.geometry.BoundaryDistanceCalculator;
import vesselreconstruction.geometry.BoundaryDistanceResult;
import vesselreconstruction.geometry.CandidateNeighbourFinder;
import vesselreconstruction.model.Bridge;
import vesselreconstruction.model.Lumen;

import java.util.ArrayList;
import java.util.List;

public class BridgeDetector {

    private final CandidateNeighbourFinder neighbourFinder =
            new CandidateNeighbourFinder();

    private final BoundaryDistanceCalculator distanceCalculator =
            new BoundaryDistanceCalculator();

    public List<Bridge> detectBridges(List<Lumen> lumina) {

        List<Bridge> bridges = new ArrayList<>();

        for (Lumen referenceLumen : lumina) {

            List<Lumen> neighbours =
                    neighbourFinder.findNeighbours(referenceLumen, lumina);

            for (Lumen candidateLumen : neighbours) {

                /*
                 * Avoid creating the same bridge twice.
                 */
                if (referenceLumen.getId() >= candidateLumen.getId()) {
                    continue;
                }

                bridges.add(
                        createBridge(referenceLumen, candidateLumen)
                );
            }
        }

        return bridges;
    }

    private Bridge createBridge(Lumen lumenA, Lumen lumenB) {

        BoundaryDistanceResult distanceResult =
                distanceCalculator.calculate(lumenA, lumenB);

        Bridge bridge = new Bridge(
                lumenA,
                lumenB,
                distanceResult.getPointOnLumenA(),
                distanceResult.getPointOnLumenB(),
                distanceResult.getDistance()
        );

        /*
         * Temporary scoring.
         * Will later be replaced by the BridgeValidator.
         */
        bridge.setScore(
                1.0 / (1.0 + distanceResult.getDistance())
        );

        return bridge;
    }
}