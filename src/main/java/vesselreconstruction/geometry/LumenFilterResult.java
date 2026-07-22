package vesselreconstruction.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable evaluation outcome for one {@link LumenFeatures} object against
 * {@link LumenFilterCriteria}.
 */
public final class LumenFilterResult {

    private final boolean accepted;
    private final boolean rejectedByArea;
    private final boolean rejectedByCircularity;
    private final boolean rejectedByAspectRatio;
    private final boolean rejectedByExtent;
    private final boolean rejectedByBoundaryRoughness;
    private final boolean rejectedByBorderContact;

    public LumenFilterResult(
            boolean rejectedByArea,
            boolean rejectedByCircularity,
            boolean rejectedByAspectRatio,
            boolean rejectedByExtent,
            boolean rejectedByBoundaryRoughness,
            boolean rejectedByBorderContact) {

        this.rejectedByArea = rejectedByArea;
        this.rejectedByCircularity = rejectedByCircularity;
        this.rejectedByAspectRatio = rejectedByAspectRatio;
        this.rejectedByExtent = rejectedByExtent;
        this.rejectedByBoundaryRoughness = rejectedByBoundaryRoughness;
        this.rejectedByBorderContact = rejectedByBorderContact;
        this.accepted = !rejectedByArea
                && !rejectedByCircularity
                && !rejectedByAspectRatio
                && !rejectedByExtent
                && !rejectedByBoundaryRoughness
                && !rejectedByBorderContact;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public boolean isRejectedByArea() {
        return rejectedByArea;
    }

    public boolean isRejectedByCircularity() {
        return rejectedByCircularity;
    }

    public boolean isRejectedByAspectRatio() {
        return rejectedByAspectRatio;
    }

    public boolean isRejectedByExtent() {
        return rejectedByExtent;
    }

    public boolean isRejectedByBoundaryRoughness() {
        return rejectedByBoundaryRoughness;
    }

    public boolean isRejectedByBorderContact() {
        return rejectedByBorderContact;
    }

    /**
     * Returns deterministic reason strings for failed criteria in the order:
     * AREA, CIRCULARITY, ASPECT_RATIO, EXTENT, BOUNDARY_ROUGHNESS,
     * BORDER_CONTACT.
     */
    public List<String> getRejectionReasons() {

        List<String> rejectionReasons = new ArrayList<>();

        if (rejectedByArea) {
            rejectionReasons.add("AREA");
        }

        if (rejectedByCircularity) {
            rejectionReasons.add("CIRCULARITY");
        }

        if (rejectedByAspectRatio) {
            rejectionReasons.add("ASPECT_RATIO");
        }

        if (rejectedByExtent) {
            rejectionReasons.add("EXTENT");
        }

        if (rejectedByBoundaryRoughness) {
            rejectionReasons.add("BOUNDARY_ROUGHNESS");
        }

        if (rejectedByBorderContact) {
            rejectionReasons.add("BORDER_CONTACT");
        }

        return Collections.unmodifiableList(rejectionReasons);
    }
}
