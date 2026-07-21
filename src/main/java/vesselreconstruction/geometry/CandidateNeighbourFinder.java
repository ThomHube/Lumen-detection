package vesselreconstruction.geometry;

import vesselreconstruction.model.Lumen;

import java.util.ArrayList;
import java.util.List;

public class CandidateNeighbourFinder {

    private static final int SEARCH_MARGIN = 30;

    public List<Lumen> findNeighbours(Lumen lumen, List<Lumen> allLumina) {

        List<Lumen> neighbours = new ArrayList<>();

        int minX = lumen.getMinX() - SEARCH_MARGIN;
        int minY = lumen.getMinY() - SEARCH_MARGIN;
        int maxX = lumen.getMaxX() + SEARCH_MARGIN;
        int maxY = lumen.getMaxY() + SEARCH_MARGIN;

        for (Lumen other : allLumina) {

            if (other == lumen) {
                continue;
            }

            boolean overlap =
                    other.getMaxX() >= minX &&
                            other.getMinX() <= maxX &&
                            other.getMaxY() >= minY &&
                            other.getMinY() <= maxY;

            if (overlap) {
                neighbours.add(other);
            }
        }

        return neighbours;
    }
}