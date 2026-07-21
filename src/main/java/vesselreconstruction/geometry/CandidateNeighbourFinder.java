package vesselreconstruction.geometry;

import vesselreconstruction.model.Lumen;

import java.util.ArrayList;
import java.util.List;

public class CandidateNeighbourFinder {

    /**
     * Margin (in pixels) added around each lumen bounding box when searching
     * for neighbouring lumen candidates.
     */
    private static final int SEARCH_MARGIN = 30;

    public List<Lumen> findNeighbours(Lumen referenceLumen, List<Lumen> allLumina) {

        List<Lumen> neighbours = new ArrayList<>();

        for (Lumen candidate : allLumina) {

            if (candidate == referenceLumen) {
                continue;
            }

            if (boundingBoxesOverlap(referenceLumen, candidate)) {
                neighbours.add(candidate);
            }
        }

        return neighbours;
    }

    private boolean boundingBoxesOverlap(Lumen reference, Lumen candidate) {

        int minX = reference.getMinX() - SEARCH_MARGIN;
        int minY = reference.getMinY() - SEARCH_MARGIN;
        int maxX = reference.getMaxX() + SEARCH_MARGIN;
        int maxY = reference.getMaxY() + SEARCH_MARGIN;

        return candidate.getMaxX() >= minX
                && candidate.getMinX() <= maxX
                && candidate.getMaxY() >= minY
                && candidate.getMinY() <= maxY;
    }
}