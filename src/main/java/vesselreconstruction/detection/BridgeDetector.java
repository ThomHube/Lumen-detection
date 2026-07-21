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

        for (Lumen lumen : lumina) {

            List<Lumen> neighbours =
                    neighbourFinder.findNeighbours(lumen, lumina);

            for (Lumen neighbour : neighbours) {

                if (lumen.getId() >= neighbour.getId()) {
                    continue;
                }

                BoundaryDistanceResult result =
                        distanceCalculator.calculate(lumen, neighbour);

                Bridge bridge = new Bridge(
                        lumen,
                        neighbour,
                        result.getPointOnLumenA(),
                        result.getPointOnLumenB(),
                        result.getDistance()
                );

                bridge.setScore(1.0 / (1.0 + result.getDistance()));

                bridges.add(bridge);
            }
        }

        return bridges;
    }
}