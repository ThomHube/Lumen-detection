package vesselreconstruction.geometry;

import vesselreconstruction.model.Lumen;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Traces the outer pixel-edge boundary of a lumen as an ordered loop of lattice
 * vertices with the lumen interior kept on the right.
 */
public final class PixelEdgeBoundaryTracer {

    private static final int DIRECTION_EAST = 0;
    private static final int DIRECTION_SOUTH = 1;
    private static final int DIRECTION_WEST = 2;
    private static final int DIRECTION_NORTH = 3;

    private static final String DUPLICATE_CLASSIFICATION_SAME_DIRECTED_EDGE =
            "SAME_DIRECTED_EDGE_REVISITED";
    private static final String DUPLICATE_CLASSIFICATION_DIFFERENT_EDGES =
            "DIFFERENT_EDGES_SHARED_VERTEX";
    private static final String DUPLICATE_CLASSIFICATION_CONVENTIONAL_CLOSURE =
            "CONVENTIONAL_CLOSURE_RECORDED";
    private static final String DUPLICATE_CLASSIFICATION_HIGH_DEGREE =
            "AMBIGUOUS_HIGH_DEGREE_VERTEX";
    private static final String DUPLICATE_CLASSIFICATION_UNKNOWN = "UNKNOWN";

    private static final String EDGE_ID_CLASSIFICATION_SAME_EDGE_ID_REVISITED =
            "SAME_EDGE_ID_REVISITED";
    private static final String EDGE_ID_CLASSIFICATION_SAME_COORDINATES_DIFFERENT_OBJECT =
            "SAME_COORDINATES_DIFFERENT_EDGE_OBJECT";
    private static final String EDGE_ID_CLASSIFICATION_DIFFERENT_EDGE_IDS_SHARED_VERTEX =
            "DIFFERENT_EDGE_IDS_SHARED_VERTEX";
    private static final String EDGE_ID_CLASSIFICATION_CONVENTIONAL_CLOSURE =
            "CONVENTIONAL_CLOSURE";
    private static final String EDGE_ID_CLASSIFICATION_HIGH_DEGREE_VERTEX =
            "HIGH_DEGREE_VERTEX";
    private static final String EDGE_ID_CLASSIFICATION_UNKNOWN = "UNKNOWN";

    private static final String INVARIANT_CURRENT_EDGE_NOT_EXPOSED =
            "CURRENT_EDGE_NOT_EXPOSED";
    private static final String INVARIANT_CURRENT_EDGE_ALREADY_USED_BEFORE_TRAVERSAL =
            "CURRENT_EDGE_ALREADY_USED_BEFORE_TRAVERSAL";
    private static final String INVARIANT_CURRENT_VERTEX_NOT_CURRENT_EDGE_END =
            "CURRENT_VERTEX_NOT_CURRENT_EDGE_END";
    private static final String INVARIANT_SUCCESSOR_DOES_NOT_START_AT_CURRENT_VERTEX =
            "SUCCESSOR_DOES_NOT_START_AT_CURRENT_VERTEX";
    private static final String INVARIANT_SUCCESSOR_NOT_EXPOSED =
            "SUCCESSOR_NOT_EXPOSED";
    private static final String INVARIANT_SUCCESSOR_ALREADY_USED =
            "SUCCESSOR_ALREADY_USED";
    private static final String INVARIANT_SUCCESSOR_NOT_IN_EXPOSED_EDGE_SET =
            "SUCCESSOR_NOT_IN_EXPOSED_EDGE_SET";
    private static final String INVARIANT_SUCCESSOR_BREAKS_UNIT_LATTICE_STEP =
            "SUCCESSOR_BREAKS_UNIT_LATTICE_STEP";
    private static final String INVARIANT_USED_EDGE_COUNT_DID_NOT_INCREMENT_BY_ONE =
            "USED_EDGE_COUNT_DID_NOT_INCREMENT_BY_ONE";
    private static final String INVARIANT_CURRENT_VERTEX_NOT_UPDATED_TO_SUCCESSOR_END =
            "CURRENT_VERTEX_NOT_UPDATED_TO_SUCCESSOR_END";
    private static final String INVARIANT_SAME_DIRECTED_EDGE_SELECTED_AGAIN =
            "SAME_DIRECTED_EDGE_SELECTED_AGAIN";
    private static final String INVARIANT_NO_VALID_UNUSED_SUCCESSOR =
            "NO_VALID_UNUSED_SUCCESSOR";
    private static final String INVARIANT_INVALID_LOOP_CLOSURE =
            "INVALID_LOOP_CLOSURE";
    private static final String INVARIANT_UNKNOWN_TRAVERSAL_STATE =
            "UNKNOWN_TRAVERSAL_STATE";
    private static final String INVARIANT_DUPLICATE_TRACED_VERTEX =
            "DUPLICATE_TRACED_VERTEX";

    private static final String CLOSURE_CONDITION_SUCCESSOR_NOT_NULL =
            "CLOSURE_SUCCESSOR_NOT_NULL";
    private static final String CLOSURE_CONDITION_SUCCESSOR_STARTS_AT_CURRENT_VERTEX =
            "CLOSURE_SUCCESSOR_STARTS_AT_CURRENT_VERTEX";
    private static final String CLOSURE_CONDITION_SUCCESSOR_MATCHES_START_EDGE_COORDINATES =
            "CLOSURE_SUCCESSOR_MATCHES_START_EDGE_COORDINATES";
    private static final String CLOSURE_CONDITION_SUCCESSOR_EDGE_ID_MATCHES_START_EDGE_ID =
            "CLOSURE_SUCCESSOR_EDGE_ID_MATCHES_START_EDGE_ID";
    private static final String CLOSURE_CONDITION_START_EDGE_NOT_IN_UNUSED_SET =
            "CLOSURE_START_EDGE_NOT_IN_UNUSED_SET";
    private static final String CLOSURE_CONDITION_START_EDGE_USED_EXACTLY_ONCE =
            "CLOSURE_START_EDGE_USED_EXACTLY_ONCE";

    /** Documented exposed-edge orientation: lumen interior is on the right when traversing edge direction. */
    public static final String EXPOSED_EDGE_LUMEN_SIDE_CONVENTION = "LUMEN_ON_RIGHT";

    private static final String TOPOLOGY_ALL_BACKGROUND = "ALL_BACKGROUND";
    private static final String TOPOLOGY_SINGLE_FOREGROUND_QUADRANT = "SINGLE_FOREGROUND_QUADRANT";
    private static final String TOPOLOGY_TWO_ADJACENT_FOREGROUND = "TWO_ADJACENT_FOREGROUND";
    private static final String TOPOLOGY_TWO_DIAGONAL_FOREGROUND_NW_SE =
            "TWO_DIAGONAL_FOREGROUND_NW_SE";
    private static final String TOPOLOGY_TWO_DIAGONAL_FOREGROUND_NE_SW =
            "TWO_DIAGONAL_FOREGROUND_NE_SW";
    private static final String TOPOLOGY_THREE_FOREGROUND_QUADRANTS = "THREE_FOREGROUND_QUADRANTS";
    private static final String TOPOLOGY_ALL_FOREGROUND = "ALL_FOREGROUND";
    private static final String TOPOLOGY_OUT_OF_BOUNDS = "OUT_OF_BOUNDS";
    private static final String TOPOLOGY_UNKNOWN = "UNKNOWN";

    private static final String PAIRING_VALID_FACE_CONTINUATION = "VALID_FACE_CONTINUATION";
    private static final String PAIRING_CHANGES_FOREGROUND_FACE = "CHANGES_FOREGROUND_FACE";
    private static final String PAIRING_REUSES_DIRECTED_EDGE = "REUSES_DIRECTED_EDGE";
    private static final String PAIRING_RETURNS_TO_SHARED_LATTICE_VERTEX =
            "RETURNS_TO_SHARED_LATTICE_VERTEX";
    private static final String PAIRING_INVALID_GEOMETRY = "INVALID_GEOMETRY";
    private static final String PAIRING_UNKNOWN = "UNKNOWN";

    public static final String SELECTION_REASON_GENERIC_TURN_PRIORITY =
            "GENERIC_TURN_PRIORITY";
    public static final String SELECTION_REASON_DIAGONAL_JUNCTION_MAPPING =
            "DIAGONAL_JUNCTION_MAPPING";
    public static final String SELECTION_REASON_EXACT_START_EDGE_CLOSURE =
            "EXACT_START_EDGE_CLOSURE";
    public static final String DIAGONAL_JUNCTION_SUCCESSOR_INVALID =
            "DIAGONAL_JUNCTION_SUCCESSOR_INVALID";

    public static final String TOPOLOGY_CLASSIFICATION_TWO_DIAGONAL_FOREGROUND_NW_SE =
            TOPOLOGY_TWO_DIAGONAL_FOREGROUND_NW_SE;
    public static final String TOPOLOGY_CLASSIFICATION_TWO_DIAGONAL_FOREGROUND_NE_SW =
            TOPOLOGY_TWO_DIAGONAL_FOREGROUND_NE_SW;

    /**
     * Immutable diagnostic facts from a boundary trace attempt.
     */
    public static final class TraceDiagnostics {

        private final int lumenId;
        private final int lumenPixelCount;
        private final int exposedDirectedEdgeCount;
        private final int tracedLoopCount;
        private final int selectedOuterLoopNumber;
        private final int selectedOuterLoopVertexCount;
        private final int selectedOuterLoopDistinctVertexCount;
        private final double selectedOuterLoopSignedArea;
        private final int repeatedVertexOccurrenceCount;
        private final Point firstRepeatedVertex;
        private final int firstRepeatedVertexFirstIndex;
        private final int firstRepeatedVertexRepeatedIndex;
        private final String firstRepeatedVertexClassification;
        private final String failureReason;
        private final List<Point> selectedOuterLoopVertices;
        private final DuplicateVertexDetail firstDuplicateDetail;

        private TraceDiagnostics(
                int lumenId,
                int lumenPixelCount,
                int exposedDirectedEdgeCount,
                int tracedLoopCount,
                int selectedOuterLoopNumber,
                int selectedOuterLoopVertexCount,
                int selectedOuterLoopDistinctVertexCount,
                double selectedOuterLoopSignedArea,
                int repeatedVertexOccurrenceCount,
                Point firstRepeatedVertex,
                int firstRepeatedVertexFirstIndex,
                int firstRepeatedVertexRepeatedIndex,
                String firstRepeatedVertexClassification,
                String failureReason,
                List<Point> selectedOuterLoopVertices,
                DuplicateVertexDetail firstDuplicateDetail) {

            this.lumenId = lumenId;
            this.lumenPixelCount = lumenPixelCount;
            this.exposedDirectedEdgeCount = exposedDirectedEdgeCount;
            this.tracedLoopCount = tracedLoopCount;
            this.selectedOuterLoopNumber = selectedOuterLoopNumber;
            this.selectedOuterLoopVertexCount = selectedOuterLoopVertexCount;
            this.selectedOuterLoopDistinctVertexCount =
                    selectedOuterLoopDistinctVertexCount;
            this.selectedOuterLoopSignedArea = selectedOuterLoopSignedArea;
            this.repeatedVertexOccurrenceCount = repeatedVertexOccurrenceCount;
            this.firstRepeatedVertex = firstRepeatedVertex;
            this.firstRepeatedVertexFirstIndex = firstRepeatedVertexFirstIndex;
            this.firstRepeatedVertexRepeatedIndex =
                    firstRepeatedVertexRepeatedIndex;
            this.firstRepeatedVertexClassification =
                    firstRepeatedVertexClassification;
            this.failureReason = failureReason;
            this.selectedOuterLoopVertices = selectedOuterLoopVertices;
            this.firstDuplicateDetail = firstDuplicateDetail;
        }

        public int getLumenId() {
            return lumenId;
        }

        public int getLumenPixelCount() {
            return lumenPixelCount;
        }

        public int getExposedDirectedEdgeCount() {
            return exposedDirectedEdgeCount;
        }

        public int getTracedLoopCount() {
            return tracedLoopCount;
        }

        public int getSelectedOuterLoopNumber() {
            return selectedOuterLoopNumber;
        }

        public int getSelectedOuterLoopVertexCount() {
            return selectedOuterLoopVertexCount;
        }

        public int getSelectedOuterLoopDistinctVertexCount() {
            return selectedOuterLoopDistinctVertexCount;
        }

        public double getSelectedOuterLoopSignedArea() {
            return selectedOuterLoopSignedArea;
        }

        public int getRepeatedVertexOccurrenceCount() {
            return repeatedVertexOccurrenceCount;
        }

        public Point getFirstRepeatedVertex() {
            return firstRepeatedVertex == null
                    ? null
                    : new Point(firstRepeatedVertex);
        }

        public int getFirstRepeatedVertexFirstIndex() {
            return firstRepeatedVertexFirstIndex;
        }

        public int getFirstRepeatedVertexRepeatedIndex() {
            return firstRepeatedVertexRepeatedIndex;
        }

        public String getFirstRepeatedVertexClassification() {
            return firstRepeatedVertexClassification;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public List<Point> getSelectedOuterLoopVertices() {
            return defensiveCopyList(selectedOuterLoopVertices);
        }

        public Point getFirstDuplicateFirstPrevious() {
            return copyNullablePoint(
                    firstDuplicateDetail == null
                            ? null
                            : firstDuplicateDetail.firstPrevious);
        }

        public Point getFirstDuplicateFirstNext() {
            return copyNullablePoint(
                    firstDuplicateDetail == null
                            ? null
                            : firstDuplicateDetail.firstNext);
        }

        public Point getFirstDuplicateRepeatedPrevious() {
            return copyNullablePoint(
                    firstDuplicateDetail == null
                            ? null
                            : firstDuplicateDetail.repeatedPrevious);
        }

        public Point getFirstDuplicateRepeatedNext() {
            return copyNullablePoint(
                    firstDuplicateDetail == null
                            ? null
                            : firstDuplicateDetail.repeatedNext);
        }

        public String getFirstDuplicateFirstIncomingDirection() {
            return firstDuplicateDetail == null
                    ? null
                    : formatDirection(firstDuplicateDetail.firstIncoming);
        }

        public String getFirstDuplicateFirstOutgoingDirection() {
            return firstDuplicateDetail == null
                    ? null
                    : formatDirection(firstDuplicateDetail.firstOutgoing);
        }

        public String getFirstDuplicateRepeatedIncomingDirection() {
            return firstDuplicateDetail == null
                    ? null
                    : formatDirection(firstDuplicateDetail.repeatedIncoming);
        }

        public String getFirstDuplicateRepeatedOutgoingDirection() {
            return firstDuplicateDetail == null
                    ? null
                    : formatDirection(firstDuplicateDetail.repeatedOutgoing);
        }

        public int getFirstDuplicateIncidentDirectedEdgeCount() {
            return firstDuplicateDetail == null
                    ? 0
                    : firstDuplicateDetail.incidentDirectedEdgeCount;
        }

        public int getFirstDuplicateUnusedOutgoingEdgeCount() {
            return firstDuplicateDetail == null
                    ? 0
                    : firstDuplicateDetail.unusedOutgoingEdgeCount;
        }

        @Override
        public boolean equals(Object other) {

            if (this == other) {
                return true;
            }

            if (!(other instanceof TraceDiagnostics)) {
                return false;
            }

            TraceDiagnostics that = (TraceDiagnostics) other;

            return lumenId == that.lumenId
                    && lumenPixelCount == that.lumenPixelCount
                    && exposedDirectedEdgeCount == that.exposedDirectedEdgeCount
                    && tracedLoopCount == that.tracedLoopCount
                    && selectedOuterLoopNumber == that.selectedOuterLoopNumber
                    && selectedOuterLoopVertexCount == that.selectedOuterLoopVertexCount
                    && selectedOuterLoopDistinctVertexCount
                            == that.selectedOuterLoopDistinctVertexCount
                    && Double.compare(
                            selectedOuterLoopSignedArea,
                            that.selectedOuterLoopSignedArea) == 0
                    && repeatedVertexOccurrenceCount == that.repeatedVertexOccurrenceCount
                    && firstRepeatedVertexFirstIndex == that.firstRepeatedVertexFirstIndex
                    && firstRepeatedVertexRepeatedIndex
                            == that.firstRepeatedVertexRepeatedIndex
                    && Objects.equals(firstRepeatedVertex, that.firstRepeatedVertex)
                    && Objects.equals(
                            firstRepeatedVertexClassification,
                            that.firstRepeatedVertexClassification)
                    && Objects.equals(failureReason, that.failureReason)
                    && Objects.equals(
                            selectedOuterLoopVertices,
                            that.selectedOuterLoopVertices);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    lumenId,
                    lumenPixelCount,
                    exposedDirectedEdgeCount,
                    tracedLoopCount,
                    failureReason);
        }

        DuplicateVertexDetail getFirstDuplicateDetail() {
            return firstDuplicateDetail;
        }

        private static Point copyNullablePoint(Point point) {
            return point == null ? null : new Point(point);
        }

        static TraceDiagnostics emptySuccess(int lumenId) {
            return new TraceDiagnostics(
                    lumenId,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0.0,
                    0,
                    null,
                    -1,
                    -1,
                    null,
                    null,
                    Collections.emptyList(),
                    null);
        }
    }

    /**
     * Immutable diagnostic facts about a single successor-edge candidate as it
     * was evaluated, in evaluation order, while selecting the next directed
     * edge to continue an outer-boundary traversal step.
     */
    public static final class SuccessorCandidateDiagnostic {

        private final int evaluationOrder;
        private final int turnOffset;
        private final String turnName;
        private final int preferredDirection;
        private final String preferredDirectionName;
        private final int candidateEdgeId;
        private final int candidateFromX;
        private final int candidateFromY;
        private final int candidateToX;
        private final int candidateToY;
        private final int candidateDirection;
        private final String candidateDirectionName;
        private final boolean unusedAtEvaluation;
        private final boolean directionMatchesPreferred;
        private final boolean selected;
        private final boolean edgeExists;
        private final boolean startsAtCurrentVertex;
        private final boolean exposed;
        private final boolean alreadyUsed;
        private final String rejectedReason;

        private SuccessorCandidateDiagnostic(
                int evaluationOrder,
                int turnOffset,
                String turnName,
                int preferredDirection,
                String preferredDirectionName,
                int candidateEdgeId,
                int candidateFromX,
                int candidateFromY,
                int candidateToX,
                int candidateToY,
                int candidateDirection,
                String candidateDirectionName,
                boolean unusedAtEvaluation,
                boolean directionMatchesPreferred,
                boolean selected,
                boolean edgeExists,
                boolean startsAtCurrentVertex,
                boolean exposed,
                boolean alreadyUsed,
                String rejectedReason) {

            this.evaluationOrder = evaluationOrder;
            this.turnOffset = turnOffset;
            this.turnName = turnName;
            this.preferredDirection = preferredDirection;
            this.preferredDirectionName = preferredDirectionName;
            this.candidateEdgeId = candidateEdgeId;
            this.candidateFromX = candidateFromX;
            this.candidateFromY = candidateFromY;
            this.candidateToX = candidateToX;
            this.candidateToY = candidateToY;
            this.candidateDirection = candidateDirection;
            this.candidateDirectionName = candidateDirectionName;
            this.unusedAtEvaluation = unusedAtEvaluation;
            this.directionMatchesPreferred = directionMatchesPreferred;
            this.selected = selected;
            this.edgeExists = edgeExists;
            this.startsAtCurrentVertex = startsAtCurrentVertex;
            this.exposed = exposed;
            this.alreadyUsed = alreadyUsed;
            this.rejectedReason = rejectedReason;
        }

        public int getEvaluationOrder() {
            return evaluationOrder;
        }

        public int getTurnOffset() {
            return turnOffset;
        }

        public String getTurnName() {
            return turnName;
        }

        public int getPreferredDirection() {
            return preferredDirection;
        }

        public String getPreferredDirectionName() {
            return preferredDirectionName;
        }

        public int getCandidateEdgeId() {
            return candidateEdgeId;
        }

        public int getCandidateFromX() {
            return candidateFromX;
        }

        public int getCandidateFromY() {
            return candidateFromY;
        }

        public int getCandidateToX() {
            return candidateToX;
        }

        public int getCandidateToY() {
            return candidateToY;
        }

        public int getCandidateDirection() {
            return candidateDirection;
        }

        public String getCandidateDirectionName() {
            return candidateDirectionName;
        }

        public boolean isUnusedAtEvaluation() {
            return unusedAtEvaluation;
        }

        public boolean isDirectionMatchesPreferred() {
            return directionMatchesPreferred;
        }

        public boolean isSelected() {
            return selected;
        }

        public int getRelativeTurn() {
            return turnOffset;
        }

        public boolean isEdgeExists() {
            return edgeExists;
        }

        public boolean isStartsAtCurrentVertex() {
            return startsAtCurrentVertex;
        }

        public boolean isExposed() {
            return exposed;
        }

        public boolean isAlreadyUsed() {
            return alreadyUsed;
        }

        public String getRejectedReason() {
            return rejectedReason;
        }

        public Point getStart() {
            return new Point(candidateFromX, candidateFromY);
        }

        public Point getEnd() {
            return new Point(candidateToX, candidateToY);
        }

        public String getDirection() {
            return candidateDirectionName;
        }
    }

    /**
     * Immutable diagnostic snapshot of a single traversal step taken while
     * tracing a boundary loop: the vertex/edge state entering the step, the
     * successor candidates evaluated (in evaluation order), the successor
     * chosen (if any), explicit closure-condition results when the step is a
     * closure candidate, and the first traversal invariant (if any) that was
     * found to have failed at this step.
     */
    public static final class TraversalStepDiagnostic {

        private final int stepIndex;
        private final int loopNumber;
        private final int previousVertexX;
        private final int previousVertexY;
        private final int currentVertexX;
        private final int currentVertexY;
        private final int currentEdgeId;
        private final int currentEdgeFromX;
        private final int currentEdgeFromY;
        private final int currentEdgeToX;
        private final int currentEdgeToY;
        private final String currentDirection;
        private final int incomingDirection;
        private final String incomingDirectionName;
        private final int loopUsedCountBeforeStep;
        private final int loopUsedCountAfterStep;
        private final int globalUsedCountBeforeStep;
        private final int globalUsedCountAfterStep;
        private final List<SuccessorCandidateDiagnostic> successorCandidates;
        private final boolean successorSelected;
        private final int selectedSuccessorEdgeId;
        private final String selectedSuccessorDirection;
        private final boolean closureCandidate;
        private final boolean closureSelected;
        private final List<String> closureConditionResults;
        private final boolean allClosureConditionsPassed;
        private final String firstFailedInvariant;
        private final String invariantFailureMessage;
        private final String stepOutcome;
        private final String vertexTopologyClassification;
        private final boolean diagonalJunctionRuleApplied;
        private final String diagonalMappedDirection;
        private final int diagonalMappedEdgeId;
        private final boolean diagonalMappingValid;
        private final String selectionReason;

        private TraversalStepDiagnostic(
                int stepIndex,
                int loopNumber,
                int previousVertexX,
                int previousVertexY,
                int currentVertexX,
                int currentVertexY,
                int currentEdgeId,
                int currentEdgeFromX,
                int currentEdgeFromY,
                int currentEdgeToX,
                int currentEdgeToY,
                String currentDirection,
                int incomingDirection,
                String incomingDirectionName,
                int loopUsedCountBeforeStep,
                int loopUsedCountAfterStep,
                int globalUsedCountBeforeStep,
                int globalUsedCountAfterStep,
                List<SuccessorCandidateDiagnostic> successorCandidates,
                boolean successorSelected,
                int selectedSuccessorEdgeId,
                String selectedSuccessorDirection,
                boolean closureCandidate,
                boolean closureSelected,
                List<String> closureConditionResults,
                boolean allClosureConditionsPassed,
                String firstFailedInvariant,
                String invariantFailureMessage,
                String stepOutcome,
                String vertexTopologyClassification,
                boolean diagonalJunctionRuleApplied,
                String diagonalMappedDirection,
                int diagonalMappedEdgeId,
                boolean diagonalMappingValid,
                String selectionReason) {

            this.stepIndex = stepIndex;
            this.loopNumber = loopNumber;
            this.previousVertexX = previousVertexX;
            this.previousVertexY = previousVertexY;
            this.currentVertexX = currentVertexX;
            this.currentVertexY = currentVertexY;
            this.currentEdgeId = currentEdgeId;
            this.currentEdgeFromX = currentEdgeFromX;
            this.currentEdgeFromY = currentEdgeFromY;
            this.currentEdgeToX = currentEdgeToX;
            this.currentEdgeToY = currentEdgeToY;
            this.currentDirection = currentDirection;
            this.incomingDirection = incomingDirection;
            this.incomingDirectionName = incomingDirectionName;
            this.loopUsedCountBeforeStep = loopUsedCountBeforeStep;
            this.loopUsedCountAfterStep = loopUsedCountAfterStep;
            this.globalUsedCountBeforeStep = globalUsedCountBeforeStep;
            this.globalUsedCountAfterStep = globalUsedCountAfterStep;
            this.successorCandidates = Collections.unmodifiableList(
                    new ArrayList<>(successorCandidates));
            this.successorSelected = successorSelected;
            this.selectedSuccessorEdgeId = selectedSuccessorEdgeId;
            this.selectedSuccessorDirection = selectedSuccessorDirection;
            this.closureCandidate = closureCandidate;
            this.closureSelected = closureSelected;
            this.closureConditionResults = Collections.unmodifiableList(
                    new ArrayList<>(closureConditionResults));
            this.allClosureConditionsPassed = allClosureConditionsPassed;
            this.firstFailedInvariant = firstFailedInvariant;
            this.invariantFailureMessage = invariantFailureMessage;
            this.stepOutcome = stepOutcome;
            this.vertexTopologyClassification = vertexTopologyClassification;
            this.diagonalJunctionRuleApplied = diagonalJunctionRuleApplied;
            this.diagonalMappedDirection = diagonalMappedDirection;
            this.diagonalMappedEdgeId = diagonalMappedEdgeId;
            this.diagonalMappingValid = diagonalMappingValid;
            this.selectionReason = selectionReason;
        }

        public int getStepIndex() {
            return stepIndex;
        }

        public int getStepNumber() {
            return stepIndex;
        }

        public int getLoopNumber() {
            return loopNumber;
        }

        public int getPreviousVertexX() {
            return previousVertexX;
        }

        public int getPreviousVertexY() {
            return previousVertexY;
        }

        public int getCurrentVertexX() {
            return currentVertexX;
        }

        public int getCurrentVertexY() {
            return currentVertexY;
        }

        public int getCurrentEdgeId() {
            return currentEdgeId;
        }

        public int getCurrentEdgeFromX() {
            return currentEdgeFromX;
        }

        public int getCurrentEdgeFromY() {
            return currentEdgeFromY;
        }

        public int getCurrentEdgeToX() {
            return currentEdgeToX;
        }

        public int getCurrentEdgeToY() {
            return currentEdgeToY;
        }

        public String getCurrentDirection() {
            return currentDirection;
        }

        public Point getCurrentStart() {
            return new Point(currentEdgeFromX, currentEdgeFromY);
        }

        public Point getCurrentEnd() {
            return new Point(currentEdgeToX, currentEdgeToY);
        }

        public Point getCurrentVertex() {
            return new Point(currentVertexX, currentVertexY);
        }

        public int getLoopUsedCountBefore() {
            return loopUsedCountBeforeStep;
        }

        public int getLoopUsedCountAfter() {
            return loopUsedCountAfterStep;
        }

        public int getGlobalUsedCountBefore() {
            return globalUsedCountBeforeStep;
        }

        public int getGlobalUsedCountAfter() {
            return globalUsedCountAfterStep;
        }

        public int getUsedEdgeCountBeforeSelection() {
            return globalUsedCountBeforeStep;
        }

        public int getUsedEdgeCountAfterTraversal() {
            return globalUsedCountAfterStep;
        }

        public String getInvariantFailureCode() {
            return firstFailedInvariant;
        }

        public String getInvariantFailureMessage() {
            return invariantFailureMessage;
        }

        public boolean isClosureSelected() {
            return closureSelected;
        }

        public String getSelectedSuccessorDirection() {
            return selectedSuccessorDirection;
        }

        public int getIncomingDirection() {
            return incomingDirection;
        }

        public String getIncomingDirectionName() {
            return incomingDirectionName;
        }

        public int getUsedEdgeCountBeforeStep() {
            return globalUsedCountBeforeStep;
        }

        public int getUsedEdgeCountAfterStep() {
            return globalUsedCountAfterStep;
        }

        public List<SuccessorCandidateDiagnostic> getSuccessorCandidates() {
            return successorCandidates;
        }

        public boolean isSuccessorSelected() {
            return successorSelected;
        }

        public int getSelectedSuccessorEdgeId() {
            return selectedSuccessorEdgeId;
        }

        public int getSelectedSuccessorFromX() {
            return -1;
        }

        public int getSelectedSuccessorFromY() {
            return -1;
        }

        public int getSelectedSuccessorToX() {
            return -1;
        }

        public int getSelectedSuccessorToY() {
            return -1;
        }

        public boolean isClosureCandidate() {
            return closureCandidate;
        }

        public List<String> getClosureConditionResults() {
            return closureConditionResults;
        }

        public boolean isAllClosureConditionsPassed() {
            return allClosureConditionsPassed;
        }

        public String getFirstFailedInvariant() {
            return firstFailedInvariant;
        }

        public String getStepOutcome() {
            return stepOutcome;
        }

        public String getVertexTopologyClassification() {
            return vertexTopologyClassification;
        }

        public boolean isDiagonalJunctionRuleApplied() {
            return diagonalJunctionRuleApplied;
        }

        public String getDiagonalMappedDirection() {
            return diagonalMappedDirection;
        }

        public int getDiagonalMappedEdgeId() {
            return diagonalMappedEdgeId;
        }

        public boolean isDiagonalMappingValid() {
            return diagonalMappingValid;
        }

        public String getSelectionReason() {
            return selectionReason;
        }
    }

    /**
     * Immutable re-classification of a repeated-vertex duplicate using stable,
     * catalog-assigned edge IDs rather than {@link Point}/edge object identity.
     */
    public static final class RepeatedVertexEdgeAnalysis {

        private final int lumenId;
        private final int loopNumber;
        private final int x;
        private final int y;
        private final int firstOccurrenceIndex;
        private final int repeatedOccurrenceIndex;
        private final int firstIncomingEdgeId;
        private final int firstOutgoingEdgeId;
        private final int repeatedIncomingEdgeId;
        private final int repeatedOutgoingEdgeId;
        private final boolean equalIncomingEdgeIds;
        private final boolean equalOutgoingEdgeIds;
        private final boolean anyEqualEdgeIds;
        private final boolean equalIncomingCoordinatePairs;
        private final boolean equalOutgoingCoordinatePairs;
        private final boolean identicalIncomingEdgeObjects;
        private final boolean identicalOutgoingEdgeObjects;
        private final int inDegree;
        private final int outDegree;
        private final List<Integer> unusedOutgoingEdgeIds;
        private final Point firstForegroundFacePixel;
        private final Point repeatedForegroundFacePixel;
        private final boolean sameForegroundFace;
        private final boolean sameTopologicalState;
        private final String legacyClassification;
        private final String edgeIdClassification;
        private final String message;

        private RepeatedVertexEdgeAnalysis(
                int lumenId,
                int loopNumber,
                int x,
                int y,
                int firstOccurrenceIndex,
                int repeatedOccurrenceIndex,
                int firstIncomingEdgeId,
                int firstOutgoingEdgeId,
                int repeatedIncomingEdgeId,
                int repeatedOutgoingEdgeId,
                boolean equalIncomingEdgeIds,
                boolean equalOutgoingEdgeIds,
                boolean anyEqualEdgeIds,
                boolean equalIncomingCoordinatePairs,
                boolean equalOutgoingCoordinatePairs,
                boolean identicalIncomingEdgeObjects,
                boolean identicalOutgoingEdgeObjects,
                int inDegree,
                int outDegree,
                List<Integer> unusedOutgoingEdgeIds,
                Point firstForegroundFacePixel,
                Point repeatedForegroundFacePixel,
                boolean sameForegroundFace,
                boolean sameTopologicalState,
                String legacyClassification,
                String edgeIdClassification,
                String message) {

            this.lumenId = lumenId;
            this.loopNumber = loopNumber;
            this.x = x;
            this.y = y;
            this.firstOccurrenceIndex = firstOccurrenceIndex;
            this.repeatedOccurrenceIndex = repeatedOccurrenceIndex;
            this.firstIncomingEdgeId = firstIncomingEdgeId;
            this.firstOutgoingEdgeId = firstOutgoingEdgeId;
            this.repeatedIncomingEdgeId = repeatedIncomingEdgeId;
            this.repeatedOutgoingEdgeId = repeatedOutgoingEdgeId;
            this.equalIncomingEdgeIds = equalIncomingEdgeIds;
            this.equalOutgoingEdgeIds = equalOutgoingEdgeIds;
            this.anyEqualEdgeIds = anyEqualEdgeIds;
            this.equalIncomingCoordinatePairs = equalIncomingCoordinatePairs;
            this.equalOutgoingCoordinatePairs = equalOutgoingCoordinatePairs;
            this.identicalIncomingEdgeObjects = identicalIncomingEdgeObjects;
            this.identicalOutgoingEdgeObjects = identicalOutgoingEdgeObjects;
            this.inDegree = inDegree;
            this.outDegree = outDegree;
            this.unusedOutgoingEdgeIds = Collections.unmodifiableList(
                    new ArrayList<>(unusedOutgoingEdgeIds));
            this.firstForegroundFacePixel = firstForegroundFacePixel == null
                    ? null
                    : new Point(firstForegroundFacePixel);
            this.repeatedForegroundFacePixel = repeatedForegroundFacePixel == null
                    ? null
                    : new Point(repeatedForegroundFacePixel);
            this.sameForegroundFace = sameForegroundFace;
            this.sameTopologicalState = sameTopologicalState;
            this.legacyClassification = legacyClassification;
            this.edgeIdClassification = edgeIdClassification;
            this.message = message;
        }

        public int getLumenId() {
            return lumenId;
        }

        public int getLoopNumber() {
            return loopNumber;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getFirstOccurrenceIndex() {
            return firstOccurrenceIndex;
        }

        public int getRepeatedOccurrenceIndex() {
            return repeatedOccurrenceIndex;
        }

        public int getFirstIndex() {
            return firstOccurrenceIndex;
        }

        public int getRepeatedIndex() {
            return repeatedOccurrenceIndex;
        }

        public int getFirstIncomingEdgeId() {
            return firstIncomingEdgeId;
        }

        public int getFirstOutgoingEdgeId() {
            return firstOutgoingEdgeId;
        }

        public int getRepeatedIncomingEdgeId() {
            return repeatedIncomingEdgeId;
        }

        public int getRepeatedOutgoingEdgeId() {
            return repeatedOutgoingEdgeId;
        }

        public boolean isEqualIncomingEdgeIds() {
            return equalIncomingEdgeIds;
        }

        public boolean isEqualOutgoingEdgeIds() {
            return equalOutgoingEdgeIds;
        }

        public boolean isAnyEqualEdgeIds() {
            return anyEqualEdgeIds;
        }

        public boolean isEqualIncomingCoordinatePairs() {
            return equalIncomingCoordinatePairs;
        }

        public boolean isEqualOutgoingCoordinatePairs() {
            return equalOutgoingCoordinatePairs;
        }

        public boolean isIdenticalIncomingEdgeObjects() {
            return identicalIncomingEdgeObjects;
        }

        public boolean isIdenticalOutgoingEdgeObjects() {
            return identicalOutgoingEdgeObjects;
        }

        public int getInDegree() {
            return inDegree;
        }

        public int getOutDegree() {
            return outDegree;
        }

        public int getIncidentDirectedEdgeCount() {
            return inDegree + outDegree;
        }

        public int getUnusedOutgoingEdgeCount() {
            return unusedOutgoingEdgeIds.size();
        }

        public List<Integer> getUnusedOutgoingEdgeIds() {
            return unusedOutgoingEdgeIds;
        }

        public Point getFirstForegroundFacePixel() {
            return firstForegroundFacePixel == null ? null : new Point(firstForegroundFacePixel);
        }

        public Point getRepeatedForegroundFacePixel() {
            return repeatedForegroundFacePixel == null
                    ? null
                    : new Point(repeatedForegroundFacePixel);
        }

        public boolean isSameForegroundFace() {
            return sameForegroundFace;
        }

        public boolean isSameTopologicalState() {
            return sameTopologicalState;
        }

        public String getLegacyClassification() {
            return legacyClassification;
        }

        public String getEdgeIdClassification() {
            return edgeIdClassification;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Immutable description of one pixel quadrant around a lattice vertex.
     */
    public static final class QuadrantPixelDiagnostic {

        private final String quadrantName;
        private final int x;
        private final int y;
        private final boolean withinBounds;
        private final boolean belongsToLumen;
        private final boolean contributesIncidentEdge;

        private QuadrantPixelDiagnostic(
                String quadrantName,
                int x,
                int y,
                boolean withinBounds,
                boolean belongsToLumen,
                boolean contributesIncidentEdge) {

            this.quadrantName = quadrantName;
            this.x = x;
            this.y = y;
            this.withinBounds = withinBounds;
            this.belongsToLumen = belongsToLumen;
            this.contributesIncidentEdge = contributesIncidentEdge;
        }

        public String getQuadrantName() {
            return quadrantName;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public boolean isWithinBounds() {
            return withinBounds;
        }

        public boolean isBelongsToLumen() {
            return belongsToLumen;
        }

        public boolean isContributesIncidentEdge() {
            return contributesIncidentEdge;
        }
    }

    /**
     * Immutable lumen-side face diagnostic for one canonical directed edge.
     */
    public static final class EdgeFaceDiagnostic {

        private final int edgeId;
        private final String direction;
        private final int fromX;
        private final int fromY;
        private final int toX;
        private final int toY;
        private final Point leftAdjacentPixel;
        private final Point rightAdjacentPixel;
        private final boolean leftPixelBelongsToLumen;
        private final boolean rightPixelBelongsToLumen;
        private final String expectedLumenSide;
        private final boolean orientationInvariantSatisfied;

        private EdgeFaceDiagnostic(
                int edgeId,
                String direction,
                int fromX,
                int fromY,
                int toX,
                int toY,
                Point leftAdjacentPixel,
                Point rightAdjacentPixel,
                boolean leftPixelBelongsToLumen,
                boolean rightPixelBelongsToLumen,
                String expectedLumenSide,
                boolean orientationInvariantSatisfied) {

            this.edgeId = edgeId;
            this.direction = direction;
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
            this.leftAdjacentPixel = leftAdjacentPixel == null ? null : new Point(leftAdjacentPixel);
            this.rightAdjacentPixel =
                    rightAdjacentPixel == null ? null : new Point(rightAdjacentPixel);
            this.leftPixelBelongsToLumen = leftPixelBelongsToLumen;
            this.rightPixelBelongsToLumen = rightPixelBelongsToLumen;
            this.expectedLumenSide = expectedLumenSide;
            this.orientationInvariantSatisfied = orientationInvariantSatisfied;
        }

        public int getEdgeId() {
            return edgeId;
        }

        public int getFromX() {
            return fromX;
        }

        public int getFromY() {
            return fromY;
        }

        public int getToX() {
            return toX;
        }

        public int getToY() {
            return toY;
        }

        public String getDirection() {
            return direction;
        }

        public Point getLeftAdjacentPixel() {
            return leftAdjacentPixel == null ? null : new Point(leftAdjacentPixel);
        }

        public Point getRightAdjacentPixel() {
            return rightAdjacentPixel == null ? null : new Point(rightAdjacentPixel);
        }

        public boolean isLeftPixelBelongsToLumen() {
            return leftPixelBelongsToLumen;
        }

        public boolean isRightPixelBelongsToLumen() {
            return rightPixelBelongsToLumen;
        }

        public String getExpectedLumenSide() {
            return expectedLumenSide;
        }

        public boolean isOrientationInvariantSatisfied() {
            return orientationInvariantSatisfied;
        }
    }

    /**
     * Immutable diagnostic for one incoming-to-outgoing branch pairing at a vertex.
     */
    public static final class BranchPairingDiagnostic {

        private final int incomingEdgeId;
        private final String incomingDirection;
        private final int outgoingEdgeId;
        private final String outgoingDirection;
        private final int relativeTurn;
        private final boolean outgoingAlreadyUsedAtFailure;
        private final int successorPriorityRank;
        private final boolean selectedByCurrentRule;
        private final boolean preservesSameForegroundFace;
        private final boolean wouldReturnToPreviouslyVisitedCoordinate;
        private final boolean wouldReuseDirectedEdge;
        private final String pairingClassification;

        private BranchPairingDiagnostic(
                int incomingEdgeId,
                String incomingDirection,
                int outgoingEdgeId,
                String outgoingDirection,
                int relativeTurn,
                boolean outgoingAlreadyUsedAtFailure,
                int successorPriorityRank,
                boolean selectedByCurrentRule,
                boolean preservesSameForegroundFace,
                boolean wouldReturnToPreviouslyVisitedCoordinate,
                boolean wouldReuseDirectedEdge,
                String pairingClassification) {

            this.incomingEdgeId = incomingEdgeId;
            this.incomingDirection = incomingDirection;
            this.outgoingEdgeId = outgoingEdgeId;
            this.outgoingDirection = outgoingDirection;
            this.relativeTurn = relativeTurn;
            this.outgoingAlreadyUsedAtFailure = outgoingAlreadyUsedAtFailure;
            this.successorPriorityRank = successorPriorityRank;
            this.selectedByCurrentRule = selectedByCurrentRule;
            this.preservesSameForegroundFace = preservesSameForegroundFace;
            this.wouldReturnToPreviouslyVisitedCoordinate =
                    wouldReturnToPreviouslyVisitedCoordinate;
            this.wouldReuseDirectedEdge = wouldReuseDirectedEdge;
            this.pairingClassification = pairingClassification;
        }

        public int getIncomingEdgeId() {
            return incomingEdgeId;
        }

        public String getIncomingDirection() {
            return incomingDirection;
        }

        public int getOutgoingEdgeId() {
            return outgoingEdgeId;
        }

        public String getOutgoingDirection() {
            return outgoingDirection;
        }

        public int getRelativeTurn() {
            return relativeTurn;
        }

        public boolean isOutgoingAlreadyUsedAtFailure() {
            return outgoingAlreadyUsedAtFailure;
        }

        public int getSuccessorPriorityRank() {
            return successorPriorityRank;
        }

        public boolean isSelectedByCurrentRule() {
            return selectedByCurrentRule;
        }

        public boolean isPreservesSameForegroundFace() {
            return preservesSameForegroundFace;
        }

        public boolean isWouldReturnToPreviouslyVisitedCoordinate() {
            return wouldReturnToPreviouslyVisitedCoordinate;
        }

        public boolean isWouldReuseDirectedEdge() {
            return wouldReuseDirectedEdge;
        }

        public String getPairingClassification() {
            return pairingClassification;
        }
    }

    /**
     * Immutable local topology diagnostic for a repeated or high-degree lattice vertex.
     */
    public static final class VertexTopologyDiagnostic {

        private final Point vertex;
        private final boolean northWestForeground;
        private final boolean northEastForeground;
        private final boolean southWestForeground;
        private final boolean southEastForeground;
        private final int foregroundCount;
        private final String topologyClassification;
        private final List<Integer> incidentIncomingEdgeIds;
        private final List<Integer> incidentOutgoingEdgeIds;
        private final List<Integer> unusedOutgoingEdgeIds;
        private final List<QuadrantPixelDiagnostic> quadrantPixelDiagnostics;
        private final List<EdgeFaceDiagnostic> edgeFaceDiagnostics;
        private final List<BranchPairingDiagnostic> branchPairingDiagnostics;

        private VertexTopologyDiagnostic(
                Point vertex,
                boolean northWestForeground,
                boolean northEastForeground,
                boolean southWestForeground,
                boolean southEastForeground,
                int foregroundCount,
                String topologyClassification,
                List<Integer> incidentIncomingEdgeIds,
                List<Integer> incidentOutgoingEdgeIds,
                List<Integer> unusedOutgoingEdgeIds,
                List<QuadrantPixelDiagnostic> quadrantPixelDiagnostics,
                List<EdgeFaceDiagnostic> edgeFaceDiagnostics,
                List<BranchPairingDiagnostic> branchPairingDiagnostics) {

            this.vertex = new Point(vertex);
            this.northWestForeground = northWestForeground;
            this.northEastForeground = northEastForeground;
            this.southWestForeground = southWestForeground;
            this.southEastForeground = southEastForeground;
            this.foregroundCount = foregroundCount;
            this.topologyClassification = topologyClassification;
            this.incidentIncomingEdgeIds = Collections.unmodifiableList(
                    new ArrayList<>(incidentIncomingEdgeIds));
            this.incidentOutgoingEdgeIds = Collections.unmodifiableList(
                    new ArrayList<>(incidentOutgoingEdgeIds));
            this.unusedOutgoingEdgeIds = Collections.unmodifiableList(
                    new ArrayList<>(unusedOutgoingEdgeIds));
            this.quadrantPixelDiagnostics = Collections.unmodifiableList(
                    new ArrayList<>(quadrantPixelDiagnostics));
            this.edgeFaceDiagnostics = Collections.unmodifiableList(
                    new ArrayList<>(edgeFaceDiagnostics));
            this.branchPairingDiagnostics = Collections.unmodifiableList(
                    new ArrayList<>(branchPairingDiagnostics));
        }

        public Point getVertex() {
            return new Point(vertex);
        }

        public boolean isNorthWestForeground() {
            return northWestForeground;
        }

        public boolean isNorthEastForeground() {
            return northEastForeground;
        }

        public boolean isSouthWestForeground() {
            return southWestForeground;
        }

        public boolean isSouthEastForeground() {
            return southEastForeground;
        }

        public int getForegroundCount() {
            return foregroundCount;
        }

        public String getTopologyClassification() {
            return topologyClassification;
        }

        public List<Integer> getIncidentIncomingEdgeIds() {
            return incidentIncomingEdgeIds;
        }

        public List<Integer> getIncidentOutgoingEdgeIds() {
            return incidentOutgoingEdgeIds;
        }

        public List<Integer> getUnusedOutgoingEdgeIds() {
            return unusedOutgoingEdgeIds;
        }

        public List<QuadrantPixelDiagnostic> getQuadrantPixelDiagnostics() {
            return quadrantPixelDiagnostics;
        }

        public List<EdgeFaceDiagnostic> getEdgeFaceDiagnostics() {
            return edgeFaceDiagnostics;
        }

        public List<BranchPairingDiagnostic> getBranchPairingDiagnostics() {
            return branchPairingDiagnostics;
        }
    }

    /**
     * Immutable, bounded debug report produced by
     * {@link #debugOuterBoundary(Lumen, int)}: traversal steps from the first
     * failing loop when any loop fails, otherwise from the selected outer loop.
     */
    public static final class TraceDebugReport {

        private final TraceDiagnostics traceDiagnostics;
        private final int lumenId;
        private final int exposedDirectedEdgeCount;
        private final int failingLoopNumber;
        private final int selectedOuterLoopNumber;
        private final int diagnosticLoopNumber;
        private final int diagnosticLoopTotalStepCount;
        private final int maximumRecordedSteps;
        private final boolean stepsTruncated;
        private final List<TraversalStepDiagnostic> traversalSteps;
        private final String firstFailureCode;
        private final int firstFailureStep;
        private final String firstFailureMessage;
        private final int diagnosticStartingEdgeId;
        private final Point diagnosticStartingEdgeStart;
        private final Point diagnosticStartingEdgeEnd;
        private final int diagnosticLoopUsedEdgeCountBeforeStart;
        private final int diagnosticLoopUsedEdgeCountAtFailure;
        private final int totalGloballyUsedEdgeCountAtFailure;
        private final RepeatedVertexEdgeAnalysis repeatedVertexEdgeAnalysis;
        private final VertexTopologyDiagnostic vertexTopologyDiagnostic;
        private final List<Point> partialVertexPath;
        private final List<Integer> partialDirectedEdgePath;
        private final String failureReason;

        private TraceDebugReport(
                TraceDiagnostics traceDiagnostics,
                int lumenId,
                int exposedDirectedEdgeCount,
                int failingLoopNumber,
                int selectedOuterLoopNumber,
                int diagnosticLoopNumber,
                int diagnosticLoopTotalStepCount,
                int maximumRecordedSteps,
                boolean stepsTruncated,
                List<TraversalStepDiagnostic> traversalSteps,
                String firstFailureCode,
                int firstFailureStep,
                String firstFailureMessage,
                int diagnosticStartingEdgeId,
                Point diagnosticStartingEdgeStart,
                Point diagnosticStartingEdgeEnd,
                int diagnosticLoopUsedEdgeCountBeforeStart,
                int diagnosticLoopUsedEdgeCountAtFailure,
                int totalGloballyUsedEdgeCountAtFailure,
                RepeatedVertexEdgeAnalysis repeatedVertexEdgeAnalysis,
                VertexTopologyDiagnostic vertexTopologyDiagnostic,
                List<Point> partialVertexPath,
                List<Integer> partialDirectedEdgePath,
                String failureReason) {

            this.traceDiagnostics = traceDiagnostics;
            this.lumenId = lumenId;
            this.exposedDirectedEdgeCount = exposedDirectedEdgeCount;
            this.failingLoopNumber = failingLoopNumber;
            this.selectedOuterLoopNumber = selectedOuterLoopNumber;
            this.diagnosticLoopNumber = diagnosticLoopNumber;
            this.diagnosticLoopTotalStepCount = diagnosticLoopTotalStepCount;
            this.maximumRecordedSteps = maximumRecordedSteps;
            this.stepsTruncated = stepsTruncated;
            this.traversalSteps = Collections.unmodifiableList(new ArrayList<>(traversalSteps));
            this.firstFailureCode = firstFailureCode;
            this.firstFailureStep = firstFailureStep;
            this.firstFailureMessage = firstFailureMessage;
            this.diagnosticStartingEdgeId = diagnosticStartingEdgeId;
            this.diagnosticStartingEdgeStart = diagnosticStartingEdgeStart == null
                    ? null
                    : new Point(diagnosticStartingEdgeStart);
            this.diagnosticStartingEdgeEnd = diagnosticStartingEdgeEnd == null
                    ? null
                    : new Point(diagnosticStartingEdgeEnd);
            this.diagnosticLoopUsedEdgeCountBeforeStart =
                    diagnosticLoopUsedEdgeCountBeforeStart;
            this.diagnosticLoopUsedEdgeCountAtFailure =
                    diagnosticLoopUsedEdgeCountAtFailure;
            this.totalGloballyUsedEdgeCountAtFailure =
                    totalGloballyUsedEdgeCountAtFailure;
            this.repeatedVertexEdgeAnalysis = repeatedVertexEdgeAnalysis;
            this.vertexTopologyDiagnostic = vertexTopologyDiagnostic;
            this.partialVertexPath = defensiveCopyList(partialVertexPath);
            this.partialDirectedEdgePath = Collections.unmodifiableList(
                    new ArrayList<>(partialDirectedEdgePath));
            this.failureReason = failureReason;
        }

        public TraceDiagnostics getTraceDiagnostics() {
            return traceDiagnostics;
        }

        public int getLumenId() {
            return lumenId;
        }

        public int getExposedDirectedEdgeCount() {
            return exposedDirectedEdgeCount;
        }

        public int getFailingLoopNumber() {
            return failingLoopNumber;
        }

        public int getSelectedOuterLoopNumber() {
            return selectedOuterLoopNumber;
        }

        public int getDiagnosticLoopNumber() {
            return diagnosticLoopNumber;
        }

        public int getDiagnosticLoopTotalStepCount() {
            return diagnosticLoopTotalStepCount;
        }

        public int getSelectedLoopNumber() {
            return selectedOuterLoopNumber;
        }

        public int getSelectedLoopTotalStepCount() {
            return diagnosticLoopTotalStepCount;
        }

        public int getMaximumRecordedSteps() {
            return maximumRecordedSteps;
        }

        public boolean isStepsTruncated() {
            return stepsTruncated;
        }

        public List<TraversalStepDiagnostic> getTraversalSteps() {
            return traversalSteps;
        }

        public List<TraversalStepDiagnostic> getSteps() {
            return traversalSteps;
        }

        public String getFirstFailureCode() {
            return firstFailureCode;
        }

        public int getFirstFailureStep() {
            return firstFailureStep;
        }

        public String getFirstFailureMessage() {
            return firstFailureMessage;
        }

        public String getFirstInvariantFailureCode() {
            return firstFailureCode;
        }

        public int getFirstInvariantFailureStep() {
            return firstFailureStep;
        }

        public String getFirstInvariantFailureMessage() {
            return firstFailureMessage;
        }

        public int getDiagnosticStartingEdgeId() {
            return diagnosticStartingEdgeId;
        }

        public int getStartingEdgeId() {
            return diagnosticStartingEdgeId;
        }

        public Point getDiagnosticStartingEdgeStart() {
            return diagnosticStartingEdgeStart == null
                    ? null
                    : new Point(diagnosticStartingEdgeStart);
        }

        public Point getDiagnosticStartingEdgeEnd() {
            return diagnosticStartingEdgeEnd == null
                    ? null
                    : new Point(diagnosticStartingEdgeEnd);
        }

        public Point getStartingEdgeStart() {
            return getDiagnosticStartingEdgeStart();
        }

        public Point getStartingEdgeEnd() {
            return getDiagnosticStartingEdgeEnd();
        }

        public int getDiagnosticLoopUsedEdgeCountBeforeStart() {
            return diagnosticLoopUsedEdgeCountBeforeStart;
        }

        public int getDiagnosticLoopUsedEdgeCountAtFailure() {
            return diagnosticLoopUsedEdgeCountAtFailure;
        }

        public int getTotalGloballyUsedEdgeCountAtFailure() {
            return totalGloballyUsedEdgeCountAtFailure;
        }

        public int getTotalUsedEdgeCountAtFailure() {
            return totalGloballyUsedEdgeCountAtFailure;
        }

        public RepeatedVertexEdgeAnalysis getRepeatedVertexEdgeAnalysis() {
            return repeatedVertexEdgeAnalysis;
        }

        public VertexTopologyDiagnostic getVertexTopologyDiagnostic() {
            return vertexTopologyDiagnostic;
        }

        public String getExposedEdgeLumenSideConvention() {
            return EXPOSED_EDGE_LUMEN_SIDE_CONVENTION;
        }

        public List<Point> getPartialVertexPath() {
            return defensiveCopyList(partialVertexPath);
        }

        public List<Integer> getPartialDirectedEdgePath() {
            return partialDirectedEdgePath;
        }

        public String getFailureReason() {
            return failureReason;
        }
    }

    /**
     * Traces the outer exposed edge loop of the supplied lumen.
     */
    public List<Point> traceOuterBoundary(Lumen lumen) {

        BoundaryTraceOutcome outcome = traceBoundaryInternal(lumen, TraceOptions.strict());

        if (outcome.duplicateDetail != null) {
            throw new IllegalStateException(
                    buildDuplicateTracedVertexMessage(outcome.duplicateDetail));
        }

        if (outcome.failureReason != null) {
            throw new IllegalStateException(outcome.failureReason);
        }

        return defensiveCopyList(outcome.selectedOuterLoop);
    }

    /**
     * Runs the same boundary trace as {@link #traceOuterBoundary(Lumen)} and
     * returns diagnostic facts without throwing for duplicate-vertex failure.
     */
    public TraceDiagnostics diagnoseOuterBoundary(Lumen lumen) {
        return traceBoundaryInternal(lumen, TraceOptions.lenient()).diagnostics;
    }

    /**
     * Runs the same boundary trace as {@link #traceOuterBoundary(Lumen)} and
     * {@link #diagnoseOuterBoundary(Lumen)}, without throwing for
     * duplicate-vertex failure, but additionally records up to
     * {@code maximumRecordedSteps} individual traversal steps from the first
     * failing loop when any loop fails, otherwise from the selected outer loop.
     * Internal traversal continues until the first failure or all loops are
     * traced; {@code maximumRecordedSteps} only bounds retained diagnostic rows.
     */
    public TraceDebugReport debugOuterBoundary(Lumen lumen, int maximumRecordedSteps) {

        if (maximumRecordedSteps < 0) {
            throw new IllegalArgumentException(
                    "maximumRecordedSteps must not be negative.");
        }

        BoundaryTraceOutcome outcome = traceBoundaryInternal(
                lumen,
                TraceOptions.debug(maximumRecordedSteps));

        return outcome.buildDebugReport(maximumRecordedSteps);
    }

    /**
     * Builds local vertex-topology diagnostics at a lattice vertex using the same
     * lumen membership and exposed-edge construction as boundary tracing.
     */
    public VertexTopologyDiagnostic analyzeLocalVertexTopology(
            Lumen lumen,
            int vertexX,
            int vertexY) {

        Objects.requireNonNull(lumen, "lumen must not be null.");

        Set<Long> lumenCoordinates = buildLumenCoordinateSet(lumen.getPixels());
        List<DirectedEdge> exposedEdges = buildExposedEdges(lumenCoordinates);
        EdgeCatalog edgeCatalog = EdgeCatalog.build(exposedEdges);
        Map<Long, List<DirectedEdge>> incidentEdgesByVertex =
                groupIncidentEdges(exposedEdges);
        Map<Long, List<DirectedEdge>> outgoingEdgesByStartVertex =
                groupOutgoingEdges(exposedEdges);

        return buildVertexTopologyDiagnostic(
                vertexX,
                vertexY,
                lumenCoordinates,
                edgeCatalog,
                incidentEdgesByVertex,
                outgoingEdgesByStartVertex,
                null,
                Collections.emptySet(),
                Collections.emptySet());
    }

    /**
     * Returns the mapped outgoing direction name for a diagonal-junction topology,
     * or {@code null} when the topology is not a diagonal junction.
     */
    public static String mapDiagonalOutgoingDirectionName(
            String topologyClassification,
            String incomingDirectionName) {

        int incomingDirection = parseDirectionName(incomingDirectionName);
        Integer mapped = mappedDiagonalOutgoingDirection(
                topologyClassification,
                incomingDirection);
        return mapped == null ? null : formatDirection(mapped);
    }

    /**
     * Resolves the diagonal-junction successor edge id at a vertex for testing.
     * When {@code treatAllOutgoingAsUsed} is true, the mapped edge is treated as
     * already used (unless it is not found), which must fail strictly.
     */
    public int resolveDiagonalJunctionSuccessorEdgeId(
            Lumen lumen,
            int vertexX,
            int vertexY,
            String incomingDirectionName,
            boolean treatAllOutgoingAsUsed) {

        Objects.requireNonNull(lumen, "lumen must not be null.");

        Set<Long> lumenCoordinates = buildLumenCoordinateSet(lumen.getPixels());
        List<DirectedEdge> exposedEdges = buildExposedEdges(lumenCoordinates);
        EdgeCatalog edgeCatalog = EdgeCatalog.build(exposedEdges);
        Map<Long, List<DirectedEdge>> outgoingEdgesByStartVertex =
                groupOutgoingEdges(exposedEdges);
        String topologyClassification = classifyVertexTopology(
                vertexX,
                vertexY,
                lumenCoordinates);
        int incomingDirection = parseDirectionName(incomingDirectionName);
        Integer mappedDirection = mappedDiagonalOutgoingDirection(
                topologyClassification,
                incomingDirection);

        if (mappedDirection == null) {
            throw new IllegalArgumentException(
                    "Vertex is not a diagonal junction: " + topologyClassification);
        }

        List<DirectedEdge> availableOutgoing = outgoingEdgesByStartVertex.getOrDefault(
                encodeCoordinate(vertexX, vertexY),
                Collections.emptyList());
        Set<DirectedEdge> unusedEdges = new HashSet<>();

        if (!treatAllOutgoingAsUsed) {
            unusedEdges.addAll(availableOutgoing);
        }

        DiagonalMappedSuccessor mappedSuccessor = resolveDiagonalMappedSuccessor(
                new Point(vertexX, vertexY),
                mappedDirection,
                availableOutgoing,
                unusedEdges,
                null,
                edgeCatalog);

        if (!mappedSuccessor.valid || mappedSuccessor.edge == null) {
            throw new IllegalStateException(
                    DIAGONAL_JUNCTION_SUCCESSOR_INVALID
                            + ":topology="
                            + topologyClassification
                            + ",incoming="
                            + incomingDirectionName
                            + ",mappedDirection="
                            + formatDirection(mappedDirection)
                            + ",vertex=("
                            + vertexX
                            + ","
                            + vertexY
                            + ")");
        }

        return mappedSuccessor.edge.getEdgeId();
    }

    private static int parseDirectionName(String directionName) {

        if ("EAST".equals(directionName)) {
            return DIRECTION_EAST;
        }

        if ("SOUTH".equals(directionName)) {
            return DIRECTION_SOUTH;
        }

        if ("WEST".equals(directionName)) {
            return DIRECTION_WEST;
        }

        if ("NORTH".equals(directionName)) {
            return DIRECTION_NORTH;
        }

        throw new IllegalArgumentException("Unknown direction: " + directionName);
    }

    private static BoundaryTraceOutcome traceBoundaryInternal(
            Lumen lumen,
            TraceOptions options) {

        Objects.requireNonNull(lumen, "lumen must not be null.");

        int lumenId = lumen.getId();
        List<Point> lumenPixels = lumen.getPixels();
        int lumenPixelCount = lumenPixels.size();

        if (lumenPixelCount == 0) {
            return BoundaryTraceOutcome.success(
                    TraceDiagnostics.emptySuccess(lumenId),
                    Collections.emptyList(),
                    lumenId,
                    0,
                    null,
                    null,
                    null,
                    null,
                    Collections.emptySet());
        }

        Set<Long> lumenCoordinates = buildLumenCoordinateSet(lumenPixels);
        List<DirectedEdge> exposedEdges = buildExposedEdges(lumenCoordinates);
        EdgeCatalog edgeCatalog = EdgeCatalog.build(exposedEdges);

        if (exposedEdges.isEmpty()) {
            String failureReason =
                    "No exposed boundary edges were found for the lumen.";
            TraceDiagnostics diagnostics = buildDiagnostics(
                    lumenId,
                    lumenPixelCount,
                    exposedEdges,
                    Collections.emptyList(),
                    null,
                    failureReason);
            return BoundaryTraceOutcome.failure(
                    diagnostics,
                    failureReason,
                    null,
                    lumenId,
                    0,
                    null,
                    null,
                    null,
                    null,
                    lumenCoordinates);
        }

        Map<Long, List<DirectedEdge>> outgoingEdgesByStartVertex =
                groupOutgoingEdges(exposedEdges);
        Map<Long, List<DirectedEdge>> incidentEdgesByVertex =
                groupIncidentEdges(exposedEdges);
        Set<DirectedEdge> unusedEdges = new HashSet<>(exposedEdges);
        List<LoopTraceRecord> loopRecords = new ArrayList<>();
        DuplicateVertexDetail firstDuplicate = null;
        LoopTraceRecord firstFailingLoopRecord = null;
        int loopNumber = 0;

        while (!unusedEdges.isEmpty()) {
            loopNumber++;
            DirectedEdge startEdge =
                    selectLexicographicallySmallestStartEdge(unusedEdges);
            int usedEdgeCountBefore = exposedEdges.size() - unusedEdges.size();

            LoopTraceRecord loopRecord = traceLoop(
                    startEdge,
                    outgoingEdgesByStartVertex,
                    incidentEdgesByVertex,
                    unusedEdges,
                    loopNumber,
                    exposedEdges.size(),
                    usedEdgeCountBefore,
                    lumenId,
                    edgeCatalog,
                    lumenCoordinates,
                    options);

            loopRecords.add(loopRecord);

            if (firstDuplicate == null && loopRecord.duplicateDetail != null) {
                firstDuplicate = loopRecord.duplicateDetail;
                firstFailingLoopRecord = loopRecord;
            } else if (firstFailingLoopRecord == null
                    && loopRecord.invariantFailureTracker.getFirstFailureCode() != null) {
                firstFailingLoopRecord = loopRecord;
            }

            if (DIAGONAL_JUNCTION_SUCCESSOR_INVALID.equals(
                    loopRecord.invariantFailureTracker.getFirstFailureCode())) {
                break;
            }
        }

        if (loopRecords.isEmpty()) {
            String failureReason =
                    "No closed boundary loops were traced for the lumen.";
            TraceDiagnostics diagnostics = buildDiagnostics(
                    lumenId,
                    lumenPixelCount,
                    exposedEdges,
                    loopRecords,
                    null,
                    failureReason);
            return BoundaryTraceOutcome.failure(
                    diagnostics,
                    failureReason,
                    null,
                    lumenId,
                    edgeCatalog.size(),
                    null,
                    null,
                    null,
                    edgeCatalog,
                    lumenCoordinates);
        }

        LoopTraceRecord selectedLoopRecord = selectOuterLoopRecord(loopRecords);
        LoopTraceRecord diagnosticLoopRecord = firstFailingLoopRecord != null
                ? firstFailingLoopRecord
                : selectedLoopRecord;
        DuplicateVertexDetail duplicateDetail =
                firstDuplicate != null ? firstDuplicate : selectedLoopRecord.duplicateDetail;

        String diagonalJunctionFailure = findDiagonalJunctionFailure(loopRecords);
        String failureReason = duplicateDetail == null
                ? diagonalJunctionFailure
                : buildDuplicateTracedVertexMessage(duplicateDetail);

        TraceDiagnostics diagnostics = buildDiagnostics(
                lumenId,
                lumenPixelCount,
                exposedEdges,
                loopRecords,
                selectedLoopRecord,
                failureReason);

        if (duplicateDetail != null) {
            if (options.isStrict()) {
                return BoundaryTraceOutcome.failure(
                        diagnostics,
                        failureReason,
                        duplicateDetail,
                        lumenId,
                        edgeCatalog.size(),
                        selectedLoopRecord,
                        firstFailingLoopRecord,
                        diagnosticLoopRecord,
                        edgeCatalog,
                        lumenCoordinates);
            }

            return BoundaryTraceOutcome.failure(
                    diagnostics,
                    failureReason,
                    duplicateDetail,
                    lumenId,
                    edgeCatalog.size(),
                    selectedLoopRecord,
                    firstFailingLoopRecord,
                    diagnosticLoopRecord,
                    edgeCatalog,
                    lumenCoordinates);
        }

        if (diagonalJunctionFailure != null) {
            return BoundaryTraceOutcome.failure(
                    diagnostics,
                    diagonalJunctionFailure,
                    null,
                    lumenId,
                    edgeCatalog.size(),
                    selectedLoopRecord,
                    firstFailingLoopRecord,
                    diagnosticLoopRecord,
                    edgeCatalog,
                    lumenCoordinates);
        }

        return BoundaryTraceOutcome.success(
                diagnostics,
                selectedLoopRecord.vertices,
                lumenId,
                edgeCatalog.size(),
                selectedLoopRecord,
                firstFailingLoopRecord,
                diagnosticLoopRecord,
                edgeCatalog,
                lumenCoordinates);
    }

    private static String findDiagonalJunctionFailure(List<LoopTraceRecord> loopRecords) {

        for (LoopTraceRecord loopRecord : loopRecords) {
            String failureCode = loopRecord.invariantFailureTracker.getFirstFailureCode();

            if (DIAGONAL_JUNCTION_SUCCESSOR_INVALID.equals(failureCode)) {
                String failureMessage =
                        loopRecord.invariantFailureTracker.getFirstFailureMessage();
                return failureMessage == null
                        ? DIAGONAL_JUNCTION_SUCCESSOR_INVALID
                        : failureMessage;
            }
        }

        return null;
    }

    private static TraceDiagnostics buildDiagnostics(
            int lumenId,
            int lumenPixelCount,
            List<DirectedEdge> exposedEdges,
            List<LoopTraceRecord> loopRecords,
            LoopTraceRecord selectedLoopRecord,
            String failureReason) {

        int selectedOuterLoopNumber = selectedLoopRecord == null
                ? 0
                : selectedLoopRecord.loopNumber;
        List<Point> selectedVertices = selectedLoopRecord == null
                ? Collections.emptyList()
                : defensiveCopyList(selectedLoopRecord.vertices);
        int selectedVertexCount = selectedVertices.size();
        int distinctVertexCount = countDistinctVertices(selectedVertices);
        double signedArea = selectedVertices.isEmpty()
                ? 0.0
                : signedPolygonArea(selectedVertices);

        DuplicateVertexDetail duplicateDetail = null;
        int repeatedOccurrenceCount = 0;

        for (LoopTraceRecord loopRecord : loopRecords) {

            if (loopRecord.duplicateDetail != null) {
                repeatedOccurrenceCount++;

                if (duplicateDetail == null) {
                    duplicateDetail = loopRecord.duplicateDetail;
                }
            }
        }

        if (duplicateDetail != null && repeatedOccurrenceCount > 1) {
            repeatedOccurrenceCount = 1;
        }

        Point firstRepeatedVertex = duplicateDetail == null
                ? null
                : new Point(duplicateDetail.x, duplicateDetail.y);

        return new TraceDiagnostics(
                lumenId,
                lumenPixelCount,
                exposedEdges.size(),
                loopRecords.size(),
                selectedOuterLoopNumber,
                selectedVertexCount,
                distinctVertexCount,
                signedArea,
                repeatedOccurrenceCount,
                firstRepeatedVertex,
                duplicateDetail == null ? -1 : duplicateDetail.firstIndex,
                duplicateDetail == null ? -1 : duplicateDetail.repeatedIndex,
                duplicateDetail == null ? null : duplicateDetail.edgeIdClassification,
                failureReason,
                selectedVertices,
                duplicateDetail);
    }

    private static int countDistinctVertices(List<Point> vertices) {

        Set<Long> uniqueKeys = new HashSet<>();

        for (Point vertex : vertices) {
            uniqueKeys.add(encodeCoordinate(vertex.x, vertex.y));
        }

        return uniqueKeys.size();
    }

    private static LoopTraceRecord selectOuterLoopRecord(
            List<LoopTraceRecord> loopRecords) {

        return loopRecords.stream()
                .max(LOOP_RECORD_ORDER)
                .orElseThrow(() -> new IllegalStateException(
                        "No closed boundary loop was available for selection."));
    }

    private static final Comparator<LoopTraceRecord> LOOP_RECORD_ORDER =
            Comparator
                    .comparingDouble(
                            (LoopTraceRecord record) ->
                                    Math.abs(signedPolygonArea(record.vertices)))
                    .reversed()
                    .thenComparingInt(record -> record.vertices.size())
                    .reversed()
                    .thenComparingInt(record -> record.vertices.get(0).y)
                    .thenComparingInt(record -> record.vertices.get(0).x)
                    .thenComparing(record -> lexicographicLoopKey(record.vertices));

    private static Set<Long> buildLumenCoordinateSet(List<Point> lumenPixels) {

        Set<Long> lumenCoordinates = new HashSet<>();
        Set<Long> seenCoordinates = new HashSet<>();

        for (Point lumenPixel : lumenPixels) {

            if (lumenPixel == null) {
                throw new IllegalArgumentException(
                        "lumen pixel list must not contain null points.");
            }

            long coordinateKey = encodeCoordinate(lumenPixel.x, lumenPixel.y);

            if (!seenCoordinates.add(coordinateKey)) {
                throw new IllegalArgumentException(
                        "lumen pixel list must not contain duplicate coordinates.");
            }

            lumenCoordinates.add(coordinateKey);
        }

        return lumenCoordinates;
    }

    private static List<DirectedEdge> buildExposedEdges(Set<Long> lumenCoordinates) {

        List<DirectedEdge> exposedEdges = new ArrayList<>();

        for (long coordinateKey : lumenCoordinates) {
            int pixelX = decodeX(coordinateKey);
            int pixelY = decodeY(coordinateKey);

            if (!containsPixel(lumenCoordinates, pixelX, pixelY - 1)) {
                exposedEdges.add(new DirectedEdge(pixelX, pixelY, pixelX + 1, pixelY));
            }

            if (!containsPixel(lumenCoordinates, pixelX + 1, pixelY)) {
                exposedEdges.add(new DirectedEdge(pixelX + 1, pixelY, pixelX + 1, pixelY + 1));
            }

            if (!containsPixel(lumenCoordinates, pixelX, pixelY + 1)) {
                exposedEdges.add(new DirectedEdge(pixelX + 1, pixelY + 1, pixelX, pixelY + 1));
            }

            if (!containsPixel(lumenCoordinates, pixelX - 1, pixelY)) {
                exposedEdges.add(new DirectedEdge(pixelX, pixelY + 1, pixelX, pixelY));
            }
        }

        return exposedEdges;
    }

    private static Map<Long, List<DirectedEdge>> groupOutgoingEdges(
            List<DirectedEdge> exposedEdges) {

        Map<Long, List<DirectedEdge>> outgoingEdgesByStartVertex = new HashMap<>();

        for (DirectedEdge exposedEdge : exposedEdges) {
            outgoingEdgesByStartVertex
                    .computeIfAbsent(exposedEdge.startKey(), key -> new ArrayList<>())
                    .add(exposedEdge);
        }

        for (List<DirectedEdge> outgoingEdges : outgoingEdgesByStartVertex.values()) {
            outgoingEdges.sort(DirectedEdge.END_VERTEX_ORDER);
        }

        return outgoingEdgesByStartVertex;
    }

    private static Map<Long, List<DirectedEdge>> groupIncidentEdges(
            List<DirectedEdge> exposedEdges) {

        Map<Long, List<DirectedEdge>> incidentEdgesByVertex = new HashMap<>();

        for (DirectedEdge exposedEdge : exposedEdges) {
            incidentEdgesByVertex
                    .computeIfAbsent(exposedEdge.startKey(), key -> new ArrayList<>())
                    .add(exposedEdge);
            incidentEdgesByVertex
                    .computeIfAbsent(exposedEdge.endKey(), key -> new ArrayList<>())
                    .add(exposedEdge);
        }

        return incidentEdgesByVertex;
    }

    private static DirectedEdge selectLexicographicallySmallestStartEdge(
            Set<DirectedEdge> unusedEdges) {

        DirectedEdge selectedEdge = null;

        for (DirectedEdge unusedEdge : unusedEdges) {

            if (selectedEdge == null) {
                selectedEdge = unusedEdge;
                continue;
            }

            int startComparison = compareVertices(
                    unusedEdge.fromY,
                    unusedEdge.fromX,
                    selectedEdge.fromY,
                    selectedEdge.fromX);

            if (startComparison < 0) {
                selectedEdge = unusedEdge;
                continue;
            }

            if (startComparison == 0) {
                int endComparison = compareVertices(
                        unusedEdge.toY,
                        unusedEdge.toX,
                        selectedEdge.toY,
                        selectedEdge.toX);

                if (endComparison < 0) {
                    selectedEdge = unusedEdge;
                }
            }
        }

        return selectedEdge;
    }

    private static LoopTraceRecord traceLoop(
            DirectedEdge startEdge,
            Map<Long, List<DirectedEdge>> outgoingEdgesByStartVertex,
            Map<Long, List<DirectedEdge>> incidentEdgesByVertex,
            Set<DirectedEdge> unusedEdges,
            int loopNumber,
            int totalExposedEdges,
            int usedEdgeCountBefore,
            int lumenId,
            EdgeCatalog edgeCatalog,
            Set<Long> lumenCoordinates,
            TraceOptions options) {

        List<TraversalStepDiagnostic> recordedSteps = new ArrayList<>();
        InvariantFailureTracker invariantFailureTracker = new InvariantFailureTracker();
        List<Integer> partialEdgePath = new ArrayList<>();
        List<Point> partialVertexPath = new ArrayList<>();
        int totalStepCount = 0;
        boolean recordSteps = options.shouldRecordSteps();

        if (!unusedEdges.remove(startEdge)) {
            DuplicateVertexDetail duplicateDetail = buildSameDirectedEdgeDuplicate(
                    startEdge.fromX,
                    startEdge.fromY,
                    0,
                    0,
                    startEdge,
                    startEdge,
                    loopNumber,
                    1,
                    totalExposedEdges,
                    usedEdgeCountBefore,
                    incidentEdgesByVertex,
                    outgoingEdgesByStartVertex,
                    unusedEdges,
                    lumenId,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    edgeCatalog);
            return new LoopTraceRecord(
                    loopNumber,
                    Collections.singletonList(new Point(startEdge.fromX, startEdge.fromY)),
                    Collections.singletonList(startEdge),
                    duplicateDetail,
                    recordedSteps,
                    totalStepCount);
        }

        partialEdgePath.add(startEdge.getEdgeId());

        List<Point> loopVertices = new ArrayList<>();
        List<DirectedEdge> usedDirectedEdges = new ArrayList<>();
        List<VertexVisitRecord> vertexVisitRecords = new ArrayList<>();
        Map<Long, Integer> firstIndexByCoordinate = new HashMap<>();
        Set<Integer> usedEdgeIdsInLoop = new HashSet<>();

        Point startVertex = new Point(startEdge.fromX, startEdge.fromY);
        loopVertices.add(new Point(startVertex));
        partialVertexPath.add(new Point(startVertex));
        usedDirectedEdges.add(startEdge);
        usedEdgeIdsInLoop.add(startEdge.getEdgeId());
        firstIndexByCoordinate.put(
                encodeCoordinate(startVertex.x, startVertex.y),
                0);

        VertexVisitRecord startVisit = new VertexVisitRecord(
                new Point(startVertex),
                -1,
                directionIndex(
                        startEdge.fromX,
                        startEdge.fromY,
                        startEdge.toX,
                        startEdge.toY),
                null,
                startEdge,
                -1,
                startEdge.getEdgeId());
        vertexVisitRecords.add(startVisit);
        startVisit.outgoingDirection = directionIndex(
                startEdge.fromX,
                startEdge.fromY,
                startEdge.toX,
                startEdge.toY);
        startVisit.outgoingEdge = startEdge;
        startVisit.outgoingEdgeId = startEdge.getEdgeId();

        Point previousVertex = new Point(startEdge.fromX, startEdge.fromY);
        Point currentVertex = new Point(startEdge.toX, startEdge.toY);
        DirectedEdge currentEdge = startEdge;
        int safeIterationLimit = unusedEdges.size() + loopVertices.size() + 8;
        int iterationCount = 0;
        DuplicateVertexDetail duplicateDetail = null;
        int duplicateFailureStep = -1;
        final int loopUsedCountBeforeStart = 0;

        while (true) {

            if (currentVertex.x == startEdge.fromX
                    && currentVertex.y == startEdge.fromY) {
                break;
            }

            long currentCoordinateKey =
                    encodeCoordinate(currentVertex.x, currentVertex.y);

            if (firstIndexByCoordinate.containsKey(currentCoordinateKey)) {
                int firstIndex = firstIndexByCoordinate.get(currentCoordinateKey);
                DirectedEdge incomingEdge = edgeCatalog.resolve(
                        previousVertex.x,
                        previousVertex.y,
                        currentVertex.x,
                        currentVertex.y);
                int incomingDirection = directionIndex(
                        previousVertex.x,
                        previousVertex.y,
                        currentVertex.x,
                        currentVertex.y);
                duplicateFailureStep = totalStepCount > 0 ? totalStepCount - 1 : 0;

                SuccessorEvaluationResult duplicateEvaluationResult =
                        evaluateSuccessorCandidates(
                                previousVertex,
                                currentVertex,
                                currentEdge,
                                startEdge,
                                outgoingEdgesByStartVertex,
                                unusedEdges,
                                edgeCatalog,
                                lumenCoordinates);

                duplicateDetail = buildDuplicateDetail(
                        currentVertex.x,
                        currentVertex.y,
                        firstIndex,
                        loopVertices.size(),
                        loopVertices,
                        vertexVisitRecords,
                        incomingEdge,
                        previousVertex,
                        loopNumber,
                        loopVertices.size(),
                        totalExposedEdges,
                        usedEdgeCountBefore + usedDirectedEdges.size(),
                        incidentEdgesByVertex,
                        outgoingEdgesByStartVertex,
                        unusedEdges,
                        lumenId,
                        usedDirectedEdges,
                        edgeCatalog);
                String duplicateFailureMessage =
                        buildDuplicateTracedVertexMessage(duplicateDetail);

                invariantFailureTracker.recordIfFirst(
                        duplicateFailureStep,
                        INVARIANT_DUPLICATE_TRACED_VERTEX,
                        duplicateFailureMessage);

                int loopUsedBeforeDuplicate =
                        loopUsedEdgeCountExcludingStart(usedDirectedEdges);
                int globalUsedBeforeDuplicate =
                        totalExposedEdges - unusedEdges.size();

                if (recordSteps && recordedSteps.size() < options.getMaximumRecordedSteps()) {
                    recordedSteps.add(buildTraversalStepDiagnostic(
                            duplicateFailureStep,
                            loopNumber,
                            previousVertex,
                            currentVertex,
                            currentEdge,
                            incomingDirection,
                            loopUsedBeforeDuplicate,
                            loopUsedBeforeDuplicate,
                            globalUsedBeforeDuplicate,
                            globalUsedBeforeDuplicate,
                            duplicateEvaluationResult.candidates,
                            incomingEdge != null,
                            incomingEdge == null ? -1 : incomingEdge.getEdgeId(),
                            incomingEdge == null
                                    ? null
                                    : formatDirection(directionIndex(
                                            incomingEdge.fromX,
                                            incomingEdge.fromY,
                                            incomingEdge.toX,
                                            incomingEdge.toY)),
                            false,
                            false,
                            Collections.emptyList(),
                            false,
                            INVARIANT_DUPLICATE_TRACED_VERTEX,
                            duplicateFailureMessage,
                            "DUPLICATE_VERTEX",
                            duplicateEvaluationResult));
                }

                loopVertices.add(new Point(currentVertex));
                break;
            }

            loopVertices.add(new Point(currentVertex));
            partialVertexPath.add(new Point(currentVertex));
            firstIndexByCoordinate.put(currentCoordinateKey, loopVertices.size() - 1);

            int incomingDirection = directionIndex(
                    previousVertex.x,
                    previousVertex.y,
                    currentVertex.x,
                    currentVertex.y);
            DirectedEdge incomingEdge = edgeCatalog.resolve(
                    previousVertex.x,
                    previousVertex.y,
                    currentVertex.x,
                    currentVertex.y);

            VertexVisitRecord visitRecord = new VertexVisitRecord(
                    new Point(currentVertex),
                    incomingDirection,
                    -1,
                    incomingEdge,
                    null,
                    incomingEdge == null ? -1 : incomingEdge.getEdgeId(),
                    -1);
            vertexVisitRecords.add(visitRecord);

            iterationCount++;

            if (iterationCount > safeIterationLimit) {
                throw new IllegalStateException(
                        "Boundary tracing exceeded the safe deterministic iteration limit.");
            }

            int loopUsedBefore = loopUsedEdgeCountExcludingStart(usedDirectedEdges);
            int globalUsedBefore = totalExposedEdges - unusedEdges.size();
            SuccessorEvaluationResult evaluationResult = evaluateSuccessorCandidates(
                    previousVertex,
                    currentVertex,
                    currentEdge,
                    startEdge,
                    outgoingEdgesByStartVertex,
                    unusedEdges,
                    edgeCatalog,
                    lumenCoordinates);
            DirectedEdge nextEdge = evaluationResult.selectedEdge;

            String stepInvariantFailure = checkPreSelectionInvariants(
                    currentEdge,
                    currentVertex,
                    edgeCatalog,
                    unusedEdges,
                    usedEdgeIdsInLoop,
                    startEdge,
                    false);
            invariantFailureTracker.recordIfFirst(
                    totalStepCount,
                    stepInvariantFailure,
                    stepInvariantFailure);

            boolean closureCandidate = nextEdge != null && nextEdge.equals(startEdge);
            List<String> closureConditionResults = Collections.emptyList();
            boolean allClosureConditionsPassed = false;

            if (closureCandidate) {
                ClosureValidation closureValidation = validateClosureConditions(
                        currentVertex,
                        currentEdge,
                        startEdge,
                        nextEdge,
                        loopVertices,
                        outgoingEdgesByStartVertex,
                        unusedEdges,
                        edgeCatalog,
                        usedDirectedEdges,
                        evaluationResult.candidates);
                closureConditionResults = closureValidation.conditionResults;
                allClosureConditionsPassed = closureValidation.allPassed;

                if (!allClosureConditionsPassed) {
                    invariantFailureTracker.recordIfFirst(
                            totalStepCount,
                            INVARIANT_INVALID_LOOP_CLOSURE,
                            String.join(";", closureConditionResults));
                }
            }

            if (nextEdge == null) {
                String noSuccessorFailure = evaluationResult.diagonalJunctionRuleApplied
                        && !evaluationResult.diagonalMappingValid
                        ? DIAGONAL_JUNCTION_SUCCESSOR_INVALID
                                + ":topology="
                                + evaluationResult.topologyClassification
                                + ",incoming="
                                + formatDirection(incomingDirection)
                                + ",mappedDirection="
                                + evaluationResult.diagonalMappedDirection
                                + ",vertex=("
                                + currentVertex.x
                                + ","
                                + currentVertex.y
                                + ")"
                        : INVARIANT_NO_VALID_UNUSED_SUCCESSOR;
                String noSuccessorCode = evaluationResult.diagonalJunctionRuleApplied
                        && !evaluationResult.diagonalMappingValid
                        ? DIAGONAL_JUNCTION_SUCCESSOR_INVALID
                        : INVARIANT_NO_VALID_UNUSED_SUCCESSOR;

                invariantFailureTracker.recordIfFirst(
                        totalStepCount,
                        noSuccessorCode,
                        noSuccessorFailure);

                if (evaluationResult.selectionFailed
                        && !DIAGONAL_JUNCTION_SUCCESSOR_INVALID.equals(noSuccessorCode)) {
                    duplicateDetail = buildHighDegreeOrUnknownDuplicate(
                            currentVertex.x,
                            currentVertex.y,
                            loopVertices.size() - 1,
                            loopVertices.size() - 1,
                            loopVertices,
                            vertexVisitRecords,
                            incomingEdge,
                            previousVertex,
                            loopNumber,
                            loopVertices.size(),
                            totalExposedEdges,
                            usedEdgeCountBefore + usedDirectedEdges.size(),
                            incidentEdgesByVertex,
                            outgoingEdgesByStartVertex,
                            unusedEdges,
                            lumenId,
                            usedDirectedEdges,
                            edgeCatalog,
                            DUPLICATE_CLASSIFICATION_UNKNOWN);
                }

                if (recordSteps && recordedSteps.size() < options.getMaximumRecordedSteps()) {
                    recordedSteps.add(buildTraversalStepDiagnostic(
                            totalStepCount,
                            loopNumber,
                            previousVertex,
                            currentVertex,
                            currentEdge,
                            incomingDirection,
                            loopUsedBefore,
                            loopUsedBefore,
                            globalUsedBefore,
                            globalUsedBefore,
                            evaluationResult.candidates,
                            false,
                            -1,
                            null,
                            closureCandidate,
                            false,
                            closureConditionResults,
                            allClosureConditionsPassed,
                            stepInvariantFailure != null
                                    ? stepInvariantFailure
                                    : noSuccessorCode,
                            stepInvariantFailure != null
                                    ? stepInvariantFailure
                                    : noSuccessorFailure,
                            "NO_SUCCESSOR",
                            evaluationResult));
                }

                totalStepCount++;
                break;
            }

            String postSelectionInvariant = checkPostSelectionInvariants(
                    currentVertex,
                    nextEdge,
                    edgeCatalog,
                    unusedEdges,
                    usedEdgeIdsInLoop,
                    startEdge,
                    closureCandidate,
                    globalUsedBefore);
            invariantFailureTracker.recordIfFirst(
                    totalStepCount,
                    postSelectionInvariant,
                    postSelectionInvariant);

            String combinedInvariantFailure = stepInvariantFailure != null
                    ? stepInvariantFailure
                    : postSelectionInvariant;
            int loopUsedAfter = closureCandidate ? loopUsedBefore : loopUsedBefore + 1;
            int globalUsedAfter = closureCandidate ? globalUsedBefore : globalUsedBefore + 1;
            boolean closureSelected = closureCandidate && allClosureConditionsPassed;

            if (recordSteps && recordedSteps.size() < options.getMaximumRecordedSteps()) {
                recordedSteps.add(buildTraversalStepDiagnostic(
                        totalStepCount,
                        loopNumber,
                        previousVertex,
                        currentVertex,
                        currentEdge,
                        incomingDirection,
                        loopUsedBefore,
                        loopUsedAfter,
                        globalUsedBefore,
                        globalUsedAfter,
                        evaluationResult.candidates,
                        true,
                        nextEdge.getEdgeId(),
                        formatDirection(directionIndex(
                                currentVertex.x,
                                currentVertex.y,
                                nextEdge.toX,
                                nextEdge.toY)),
                        closureCandidate,
                        closureSelected,
                        closureConditionResults,
                        allClosureConditionsPassed,
                        combinedInvariantFailure,
                        combinedInvariantFailure,
                        closureCandidate ? "CLOSURE" : "TRAVERSE",
                        evaluationResult));
            }

            totalStepCount++;

            if (closureCandidate) {
                visitRecord.outgoingDirection = directionIndex(
                        currentVertex.x,
                        currentVertex.y,
                        startEdge.fromX,
                        startEdge.fromY);
                visitRecord.outgoingEdge = startEdge;
                visitRecord.outgoingEdgeId = startEdge.getEdgeId();
                break;
            }

            if (!unusedEdges.remove(nextEdge)) {
                duplicateDetail = buildSameDirectedEdgeDuplicate(
                        nextEdge.fromX,
                        nextEdge.fromY,
                        firstIndexByCoordinate.getOrDefault(
                                encodeCoordinate(nextEdge.fromX, nextEdge.fromY),
                                loopVertices.size() - 1),
                        loopVertices.size() - 1,
                        nextEdge,
                        nextEdge,
                        loopNumber,
                        loopVertices.size(),
                        totalExposedEdges,
                        usedEdgeCountBefore + usedDirectedEdges.size(),
                        incidentEdgesByVertex,
                        outgoingEdgesByStartVertex,
                        unusedEdges,
                        lumenId,
                        loopVertices,
                        usedDirectedEdges,
                        edgeCatalog);
                duplicateFailureStep = totalStepCount > 0 ? totalStepCount - 1 : 0;
                invariantFailureTracker.recordIfFirst(
                        duplicateFailureStep,
                        INVARIANT_DUPLICATE_TRACED_VERTEX,
                        buildDuplicateTracedVertexMessage(duplicateDetail));
                break;
            }

            visitRecord.outgoingDirection = directionIndex(
                    currentVertex.x,
                    currentVertex.y,
                    nextEdge.toX,
                    nextEdge.toY);
            visitRecord.outgoingEdge = nextEdge;
            visitRecord.outgoingEdgeId = nextEdge.getEdgeId();
            usedDirectedEdges.add(nextEdge);
            usedEdgeIdsInLoop.add(nextEdge.getEdgeId());
            partialEdgePath.add(nextEdge.getEdgeId());

            int loopUsedAfterActual = loopUsedEdgeCountExcludingStart(usedDirectedEdges);
            int globalUsedAfterActual = totalExposedEdges - unusedEdges.size();
            if (loopUsedAfterActual != loopUsedBefore + 1) {
                invariantFailureTracker.recordIfFirst(
                        totalStepCount - 1,
                        INVARIANT_USED_EDGE_COUNT_DID_NOT_INCREMENT_BY_ONE,
                        "loopUsedBefore="
                                + loopUsedBefore
                                + ",loopUsedAfter="
                                + loopUsedAfterActual);
            }
            if (globalUsedAfterActual != globalUsedBefore + 1) {
                invariantFailureTracker.recordIfFirst(
                        totalStepCount - 1,
                        INVARIANT_USED_EDGE_COUNT_DID_NOT_INCREMENT_BY_ONE,
                        "globalUsedBefore="
                                + globalUsedBefore
                                + ",globalUsedAfter="
                                + globalUsedAfterActual);
            }

            previousVertex = currentVertex;
            Point nextVertex = new Point(nextEdge.toX, nextEdge.toY);
            currentVertex = nextVertex;
            currentEdge = nextEdge;

            if (currentVertex.x != nextEdge.toX || currentVertex.y != nextEdge.toY) {
                invariantFailureTracker.recordIfFirst(
                        totalStepCount - 1,
                        INVARIANT_CURRENT_VERTEX_NOT_UPDATED_TO_SUCCESSOR_END,
                        INVARIANT_CURRENT_VERTEX_NOT_UPDATED_TO_SUCCESSOR_END);
            }
        }

        if (duplicateDetail == null) {
            duplicateDetail = validateLoopVerticesForDuplicate(
                    loopVertices,
                    loopNumber,
                    totalExposedEdges,
                    usedEdgeCountBefore + usedDirectedEdges.size(),
                    incidentEdgesByVertex,
                    outgoingEdgesByStartVertex,
                    unusedEdges,
                    lumenId,
                    usedDirectedEdges,
                    edgeCatalog);
        }

        return new LoopTraceRecord(
                loopNumber,
                loopVertices,
                usedDirectedEdges,
                duplicateDetail,
                recordedSteps,
                totalStepCount,
                partialVertexPath,
                partialEdgePath,
                invariantFailureTracker,
                duplicateFailureStep,
                loopUsedCountBeforeStart,
                loopUsedEdgeCountExcludingStart(usedDirectedEdges),
                totalExposedEdges - unusedEdges.size());
    }

    private static SuccessorEvaluationResult evaluateSuccessorCandidates(
            Point previousVertex,
            Point currentVertex,
            DirectedEdge currentEdge,
            DirectedEdge startEdge,
            Map<Long, List<DirectedEdge>> outgoingEdgesByStartVertex,
            Set<DirectedEdge> unusedEdges,
            EdgeCatalog edgeCatalog,
            Set<Long> lumenCoordinates) {

        List<DirectedEdge> availableOutgoingEdges =
                outgoingEdgesByStartVertex.getOrDefault(
                        encodeCoordinate(currentVertex.x, currentVertex.y),
                        Collections.emptyList());
        int incomingDirection = directionIndex(
                previousVertex.x,
                previousVertex.y,
                currentVertex.x,
                currentVertex.y);
        String topologyClassification = classifyVertexTopology(
                currentVertex.x,
                currentVertex.y,
                lumenCoordinates);

        List<SuccessorCandidateDiagnostic> candidates = new ArrayList<>();
        DirectedEdge selectedEdge = null;
        int evaluationOrder = 0;
        boolean selectionFailed = false;
        String selectionReason = null;
        boolean diagonalJunctionRuleApplied = false;
        String diagonalMappedDirection = null;
        int diagonalMappedEdgeId = -1;
        boolean diagonalMappingValid = false;

        Integer mappedDirection = mappedDiagonalOutgoingDirection(
                topologyClassification,
                incomingDirection);

        if (mappedDirection != null) {
            diagonalJunctionRuleApplied = true;
            diagonalMappedDirection = formatDirection(mappedDirection);
            DiagonalMappedSuccessor mappedSuccessor = resolveDiagonalMappedSuccessor(
                    currentVertex,
                    mappedDirection,
                    availableOutgoingEdges,
                    unusedEdges,
                    startEdge,
                    edgeCatalog);
            diagonalMappedEdgeId = mappedSuccessor.edge == null
                    ? -1
                    : mappedSuccessor.edge.getEdgeId();
            diagonalMappingValid = mappedSuccessor.valid;

            for (int turnOffset : new int[] {1, 0, 3, 2}) {
                int preferredDirection = (incomingDirection + turnOffset) % 4;
                DirectedEdge candidateForDirection = findOutgoingEdgeInDirection(
                        currentVertex,
                        preferredDirection,
                        availableOutgoingEdges,
                        edgeCatalog);
                boolean edgeExists = candidateForDirection != null;
                boolean startsAtCurrentVertex = candidateForDirection != null
                        && candidateForDirection.fromX == currentVertex.x
                        && candidateForDirection.fromY == currentVertex.y;
                boolean exposed = candidateForDirection != null
                        && edgeCatalog.exposedEdgeSet().contains(candidateForDirection);
                boolean alreadyUsed = candidateForDirection != null
                        && !unusedEdges.contains(candidateForDirection)
                        && !candidateForDirection.equals(startEdge);
                boolean selected = mappedSuccessor.valid
                        && mappedSuccessor.edge != null
                        && preferredDirection == mappedDirection
                        && candidateForDirection != null
                        && candidateForDirection.getEdgeId()
                                == mappedSuccessor.edge.getEdgeId();
                String rejectedReason;

                if (selected) {
                    rejectedReason = "NONE";
                } else if (preferredDirection == mappedDirection && !mappedSuccessor.valid) {
                    rejectedReason = DIAGONAL_JUNCTION_SUCCESSOR_INVALID;
                } else if (preferredDirection == mappedDirection) {
                    rejectedReason = "NONE";
                } else {
                    rejectedReason = "DIAGONAL_JUNCTION_OVERRIDDEN";
                }

                candidates.add(new SuccessorCandidateDiagnostic(
                        evaluationOrder,
                        turnOffset,
                        turnNameForOffset(turnOffset),
                        preferredDirection,
                        formatDirection(preferredDirection),
                        candidateForDirection == null ? -1 : candidateForDirection.getEdgeId(),
                        candidateForDirection == null
                                ? currentVertex.x
                                : candidateForDirection.fromX,
                        candidateForDirection == null
                                ? currentVertex.y
                                : candidateForDirection.fromY,
                        candidateForDirection == null
                                ? currentVertex.x
                                : candidateForDirection.toX,
                        candidateForDirection == null
                                ? currentVertex.y
                                : candidateForDirection.toY,
                        candidateForDirection == null
                                ? preferredDirection
                                : directionIndex(
                                        candidateForDirection.fromX,
                                        candidateForDirection.fromY,
                                        candidateForDirection.toX,
                                        candidateForDirection.toY),
                        candidateForDirection == null
                                ? formatDirection(preferredDirection)
                                : formatDirection(directionIndex(
                                        candidateForDirection.fromX,
                                        candidateForDirection.fromY,
                                        candidateForDirection.toX,
                                        candidateForDirection.toY)),
                        !alreadyUsed,
                        candidateForDirection != null
                                && directionIndex(
                                        candidateForDirection.fromX,
                                        candidateForDirection.fromY,
                                        candidateForDirection.toX,
                                        candidateForDirection.toY) == preferredDirection,
                        selected,
                        edgeExists,
                        startsAtCurrentVertex,
                        exposed,
                        alreadyUsed,
                        rejectedReason));
                evaluationOrder++;
            }

            if (mappedSuccessor.valid) {
                selectedEdge = mappedSuccessor.edge;
                selectionReason = selectedEdge != null && selectedEdge.equals(startEdge)
                        ? SELECTION_REASON_EXACT_START_EDGE_CLOSURE
                        : SELECTION_REASON_DIAGONAL_JUNCTION_MAPPING;
            } else {
                selectionFailed = true;
            }

            return new SuccessorEvaluationResult(
                    selectedEdge,
                    candidates,
                    selectionFailed,
                    topologyClassification,
                    diagonalJunctionRuleApplied,
                    diagonalMappedDirection,
                    diagonalMappedEdgeId,
                    diagonalMappingValid,
                    selectionReason);
        }

        for (int turnOffset : new int[] {1, 0, 3, 2}) {
            int preferredDirection = (incomingDirection + turnOffset) % 4;

            DirectedEdge candidateForDirection = findOutgoingEdgeInDirection(
                    currentVertex,
                    preferredDirection,
                    availableOutgoingEdges,
                    edgeCatalog);

            boolean edgeExists = candidateForDirection != null;
            boolean startsAtCurrentVertex = candidateForDirection != null
                    && candidateForDirection.fromX == currentVertex.x
                    && candidateForDirection.fromY == currentVertex.y;
            boolean exposed = candidateForDirection != null
                    && edgeCatalog.exposedEdgeSet().contains(candidateForDirection);
            boolean alreadyUsed = candidateForDirection != null
                    && !unusedEdges.contains(candidateForDirection);
            boolean allowStartEdgeClosure = candidateForDirection != null
                    && candidateForDirection.equals(startEdge);
            String rejectedReason = determineRejectedReason(
                    edgeExists,
                    startsAtCurrentVertex,
                    exposed,
                    alreadyUsed && !allowStartEdgeClosure,
                    false);
            boolean selected = false;

            if (selectedEdge == null
                    && edgeExists
                    && startsAtCurrentVertex
                    && exposed
                    && (!alreadyUsed || allowStartEdgeClosure)) {
                selectedEdge = candidateForDirection;
                selected = true;
                selectionReason = allowStartEdgeClosure && alreadyUsed
                        ? SELECTION_REASON_EXACT_START_EDGE_CLOSURE
                        : SELECTION_REASON_GENERIC_TURN_PRIORITY;
            } else if (selectedEdge != null && edgeExists && !alreadyUsed && exposed) {
                rejectedReason = "LOWER_PRIORITY";
            }

            candidates.add(new SuccessorCandidateDiagnostic(
                    evaluationOrder,
                    turnOffset,
                    turnNameForOffset(turnOffset),
                    preferredDirection,
                    formatDirection(preferredDirection),
                    candidateForDirection == null ? -1 : candidateForDirection.getEdgeId(),
                    candidateForDirection == null ? currentVertex.x : candidateForDirection.fromX,
                    candidateForDirection == null ? currentVertex.y : candidateForDirection.fromY,
                    candidateForDirection == null ? currentVertex.x : candidateForDirection.toX,
                    candidateForDirection == null ? currentVertex.y : candidateForDirection.toY,
                    candidateForDirection == null
                            ? preferredDirection
                            : directionIndex(
                                    candidateForDirection.fromX,
                                    candidateForDirection.fromY,
                                    candidateForDirection.toX,
                                    candidateForDirection.toY),
                    candidateForDirection == null
                            ? formatDirection(preferredDirection)
                            : formatDirection(directionIndex(
                                    candidateForDirection.fromX,
                                    candidateForDirection.fromY,
                                    candidateForDirection.toX,
                                    candidateForDirection.toY)),
                    !alreadyUsed || allowStartEdgeClosure,
                    candidateForDirection != null
                            && directionIndex(
                                    candidateForDirection.fromX,
                                    candidateForDirection.fromY,
                                    candidateForDirection.toX,
                                    candidateForDirection.toY) == preferredDirection,
                    selected,
                    edgeExists,
                    startsAtCurrentVertex,
                    exposed,
                    alreadyUsed,
                    rejectedReason));

            evaluationOrder++;
        }

        if (selectedEdge == null) {
            try {
                selectedEdge = selectSuccessorEdge(
                        previousVertex,
                        currentVertex,
                        startEdge,
                        outgoingEdgesByStartVertex,
                        unusedEdges,
                        lumenCoordinates);
                selectionReason = SELECTION_REASON_GENERIC_TURN_PRIORITY;
            } catch (IllegalStateException expectedFailure) {
                selectionFailed = true;

                if (expectedFailure.getMessage() != null
                        && expectedFailure.getMessage().startsWith(
                                DIAGONAL_JUNCTION_SUCCESSOR_INVALID)) {
                    diagonalJunctionRuleApplied = true;
                }
            }
        }

        if (selectionReason == null && selectedEdge != null) {
            selectionReason = SELECTION_REASON_GENERIC_TURN_PRIORITY;
        }

        return new SuccessorEvaluationResult(
                selectedEdge,
                candidates,
                selectionFailed,
                topologyClassification,
                diagonalJunctionRuleApplied,
                diagonalMappedDirection,
                diagonalMappedEdgeId,
                diagonalMappingValid,
                selectionReason);
    }

    private static DirectedEdge findOutgoingEdgeInDirection(
            Point currentVertex,
            int preferredDirection,
            List<DirectedEdge> availableOutgoingEdges,
            EdgeCatalog edgeCatalog) {

        for (DirectedEdge candidateEdge : availableOutgoingEdges) {

            if (directionIndex(
                    currentVertex.x,
                    currentVertex.y,
                    candidateEdge.toX,
                    candidateEdge.toY) == preferredDirection) {
                return edgeCatalog.resolve(
                        candidateEdge.fromX,
                        candidateEdge.fromY,
                        candidateEdge.toX,
                        candidateEdge.toY);
            }
        }

        return null;
    }

    private static String determineRejectedReason(
            boolean edgeExists,
            boolean startsAtCurrentVertex,
            boolean exposed,
            boolean alreadyUsed,
            boolean lowerPriority) {

        if (lowerPriority) {
            return "LOWER_PRIORITY";
        }

        if (!edgeExists) {
            return "EDGE_DOES_NOT_EXIST";
        }

        if (!startsAtCurrentVertex) {
            return "WRONG_START_VERTEX";
        }

        if (!exposed) {
            return "NOT_EXPOSED";
        }

        if (alreadyUsed) {
            return "ALREADY_USED";
        }

        return "NONE";
    }

    private static String turnNameForOffset(int turnOffset) {

        switch (turnOffset) {
            case 1:
                return "RIGHT";
            case 0:
                return "STRAIGHT";
            case 3:
                return "LEFT";
            case 2:
                return "REVERSE";
            default:
                return "UNKNOWN";
        }
    }

    private static String checkPreSelectionInvariants(
            DirectedEdge currentEdge,
            Point currentVertex,
            EdgeCatalog edgeCatalog,
            Set<DirectedEdge> unusedEdges,
            Set<Integer> usedEdgeIdsInLoop,
            DirectedEdge startEdge,
            boolean closureCheck) {

        if (!edgeCatalog.exposedEdgeSet().contains(currentEdge)) {
            return INVARIANT_CURRENT_EDGE_NOT_EXPOSED;
        }

        if (!closureCheck
                && usedEdgeIdsInLoop.contains(currentEdge.getEdgeId())
                && usedEdgeCountBeforeCurrentEdge(currentEdge, usedEdgeIdsInLoop) > 0) {
            int count = 0;

            for (int usedEdgeId : usedEdgeIdsInLoop) {

                if (usedEdgeId == currentEdge.getEdgeId()) {
                    count++;
                }
            }

            if (count > 1) {
                return INVARIANT_CURRENT_EDGE_ALREADY_USED_BEFORE_TRAVERSAL;
            }
        }

        if (currentVertex.x != currentEdge.toX || currentVertex.y != currentEdge.toY) {
            return INVARIANT_CURRENT_VERTEX_NOT_CURRENT_EDGE_END;
        }

        return null;
    }

    private static int usedEdgeCountBeforeCurrentEdge(
            DirectedEdge currentEdge,
            Set<Integer> usedEdgeIdsInLoop) {

        int count = 0;

        for (int usedEdgeId : usedEdgeIdsInLoop) {

            if (usedEdgeId == currentEdge.getEdgeId()) {
                count++;
            }
        }

        return count;
    }

    private static String checkPostSelectionInvariants(
            Point currentVertex,
            DirectedEdge selectedSuccessor,
            EdgeCatalog edgeCatalog,
            Set<DirectedEdge> unusedEdges,
            Set<Integer> usedEdgeIdsInLoop,
            DirectedEdge startEdge,
            boolean closureCandidate,
            int usedBeforeStep) {

        if (selectedSuccessor.fromX != currentVertex.x
                || selectedSuccessor.fromY != currentVertex.y) {
            return INVARIANT_SUCCESSOR_DOES_NOT_START_AT_CURRENT_VERTEX;
        }

        if (!edgeCatalog.exposedEdgeSet().contains(selectedSuccessor)) {
            return INVARIANT_SUCCESSOR_NOT_EXPOSED;
        }

        DirectedEdge canonicalSuccessor = edgeCatalog.resolve(
                selectedSuccessor.fromX,
                selectedSuccessor.fromY,
                selectedSuccessor.toX,
                selectedSuccessor.toY);

        if (canonicalSuccessor == null) {
            return INVARIANT_SUCCESSOR_NOT_IN_EXPOSED_EDGE_SET;
        }

        if (canonicalSuccessor != selectedSuccessor) {
            return INVARIANT_SUCCESSOR_NOT_IN_EXPOSED_EDGE_SET;
        }

        if (!isUnitLatticeStep(
                selectedSuccessor.fromX,
                selectedSuccessor.fromY,
                selectedSuccessor.toX,
                selectedSuccessor.toY)) {
            return INVARIANT_SUCCESSOR_BREAKS_UNIT_LATTICE_STEP;
        }

        if (!closureCandidate && !unusedEdges.contains(selectedSuccessor)) {
            return INVARIANT_SUCCESSOR_ALREADY_USED;
        }

        if (!closureCandidate
                && usedEdgeIdsInLoop.contains(selectedSuccessor.getEdgeId())) {
            return INVARIANT_SAME_DIRECTED_EDGE_SELECTED_AGAIN;
        }

        return null;
    }

    private static boolean isUnitLatticeStep(int fromX, int fromY, int toX, int toY) {

        int deltaX = Math.abs(toX - fromX);
        int deltaY = Math.abs(toY - fromY);

        return (deltaX == 1 && deltaY == 0) || (deltaX == 0 && deltaY == 1);
    }

    private static final class ClosureValidation {

        private final List<String> conditionResults;
        private final boolean allPassed;

        private ClosureValidation(List<String> conditionResults, boolean allPassed) {
            this.conditionResults = conditionResults;
            this.allPassed = allPassed;
        }
    }

    private static ClosureValidation validateClosureConditions(
            Point currentVertex,
            DirectedEdge currentEdge,
            DirectedEdge startEdge,
            DirectedEdge selectedSuccessor,
            List<Point> loopVertices,
            Map<Long, List<DirectedEdge>> outgoingEdgesByStartVertex,
            Set<DirectedEdge> unusedEdges,
            EdgeCatalog edgeCatalog,
            List<DirectedEdge> usedDirectedEdges,
            List<SuccessorCandidateDiagnostic> candidates) {

        List<String> results = new ArrayList<>();
        boolean condition1 = currentVertex.x == startEdge.fromX
                && currentVertex.y == startEdge.fromY;
        results.add("1_currentAtStart=" + condition1);

        boolean condition2 = selectedSuccessor.equals(startEdge);
        results.add("2_successorIsStartEdge=" + condition2);

        int startEdgeUseCount = 0;

        for (DirectedEdge usedEdge : usedDirectedEdges) {

            if (usedEdge.equals(startEdge)) {
                startEdgeUseCount++;
            }
        }

        boolean condition3 = startEdgeUseCount == 1;
        results.add("3_startEdgeUsedOnce=" + condition3);

        boolean condition4 = !hasHigherPriorityUnusedSuccessorThanStartEdge(
                currentVertex,
                currentEdge,
                startEdge,
                outgoingEdgesByStartVertex,
                unusedEdges);
        results.add("4_noHigherPriorityUnusedSuccessor=" + condition4);

        boolean condition5 = countDistinctVertices(loopVertices) >= 3;
        results.add("5_atLeastThreeDistinctVertices=" + condition5);

        boolean condition6 = loopVertices.isEmpty()
                || !(loopVertices.get(loopVertices.size() - 1).x == startEdge.fromX
                        && loopVertices.get(loopVertices.size() - 1).y == startEdge.fromY);
        results.add("6_closingVertexNotAppended=" + condition6);

        boolean allPassed = condition1
                && condition2
                && condition3
                && condition4
                && condition5
                && condition6;

        return new ClosureValidation(results, allPassed);
    }

    private static boolean hasHigherPriorityUnusedSuccessorThanStartEdge(
            Point currentVertex,
            DirectedEdge currentEdge,
            DirectedEdge startEdge,
            Map<Long, List<DirectedEdge>> outgoingEdgesByStartVertex,
            Set<DirectedEdge> unusedEdges) {

        int incomingDirection = directionIndex(
                currentEdge.fromX,
                currentEdge.fromY,
                currentVertex.x,
                currentVertex.y);
        List<DirectedEdge> availableOutgoingEdges =
                outgoingEdgesByStartVertex.getOrDefault(
                        encodeCoordinate(currentVertex.x, currentVertex.y),
                        Collections.emptyList());

        for (int turnOffset : new int[] {1, 0, 3, 2}) {
            int preferredDirection = (incomingDirection + turnOffset) % 4;

            for (DirectedEdge candidateEdge : availableOutgoingEdges) {

                if (!unusedEdges.contains(candidateEdge)) {
                    continue;
                }

                if (directionIndex(
                        currentVertex.x,
                        currentVertex.y,
                        candidateEdge.toX,
                        candidateEdge.toY) == preferredDirection) {
                    return !candidateEdge.equals(startEdge);
                }
            }
        }

        return false;
    }

    private static int loopUsedEdgeCountExcludingStart(List<DirectedEdge> usedDirectedEdges) {
        return Math.max(0, usedDirectedEdges.size() - 1);
    }

    private static TraversalStepDiagnostic buildTraversalStepDiagnostic(
            int stepIndex,
            int loopNumber,
            Point previousVertex,
            Point currentVertex,
            DirectedEdge currentEdge,
            int incomingDirection,
            int loopUsedCountBeforeStep,
            int loopUsedCountAfterStep,
            int globalUsedCountBeforeStep,
            int globalUsedCountAfterStep,
            List<SuccessorCandidateDiagnostic> successorCandidates,
            boolean successorSelected,
            int selectedSuccessorEdgeId,
            String selectedSuccessorDirection,
            boolean closureCandidate,
            boolean closureSelected,
            List<String> closureConditionResults,
            boolean allClosureConditionsPassed,
            String firstFailedInvariant,
            String invariantFailureMessage,
            String stepOutcome,
            SuccessorEvaluationResult evaluationResult) {

        String topologyClassification = evaluationResult == null
                ? null
                : evaluationResult.topologyClassification;
        boolean diagonalApplied = evaluationResult != null
                && evaluationResult.diagonalJunctionRuleApplied;
        String mappedDirection = evaluationResult == null
                ? null
                : evaluationResult.diagonalMappedDirection;
        int mappedEdgeId = evaluationResult == null
                ? -1
                : evaluationResult.diagonalMappedEdgeId;
        boolean mappingValid = evaluationResult != null
                && evaluationResult.diagonalMappingValid;
        String selectionReason = evaluationResult == null
                ? null
                : evaluationResult.selectionReason;

        if (closureSelected) {
            selectionReason = SELECTION_REASON_EXACT_START_EDGE_CLOSURE;
        }

        return new TraversalStepDiagnostic(
                stepIndex,
                loopNumber,
                previousVertex.x,
                previousVertex.y,
                currentVertex.x,
                currentVertex.y,
                currentEdge.getEdgeId(),
                currentEdge.fromX,
                currentEdge.fromY,
                currentEdge.toX,
                currentEdge.toY,
                formatDirection(directionIndex(
                        currentEdge.fromX,
                        currentEdge.fromY,
                        currentEdge.toX,
                        currentEdge.toY)),
                incomingDirection,
                formatDirection(incomingDirection),
                loopUsedCountBeforeStep,
                loopUsedCountAfterStep,
                globalUsedCountBeforeStep,
                globalUsedCountAfterStep,
                successorCandidates,
                successorSelected,
                selectedSuccessorEdgeId,
                selectedSuccessorDirection,
                closureCandidate,
                closureSelected,
                closureConditionResults,
                allClosureConditionsPassed,
                firstFailedInvariant,
                invariantFailureMessage,
                stepOutcome,
                topologyClassification,
                diagonalApplied,
                mappedDirection,
                mappedEdgeId,
                mappingValid,
                selectionReason);
    }

    private static DuplicateVertexDetail validateLoopVerticesForDuplicate(
            List<Point> loopVertices,
            int loopNumber,
            int totalExposedEdges,
            int usedExposedEdges,
            Map<Long, List<DirectedEdge>> incidentEdgesByVertex,
            Map<Long, List<DirectedEdge>> outgoingEdgesByStartVertex,
            Set<DirectedEdge> unusedEdges,
            int lumenId,
            List<DirectedEdge> usedDirectedEdges,
            EdgeCatalog edgeCatalog) {

        if (loopVertices.size() < 3) {
            return null;
        }

        Map<Long, Integer> firstIndexByCoordinate = new HashMap<>();

        for (int vertexIndex = 0; vertexIndex < loopVertices.size(); vertexIndex++) {
            Point loopVertex = loopVertices.get(vertexIndex);
            long coordinateKey = encodeCoordinate(loopVertex.x, loopVertex.y);

            if (firstIndexByCoordinate.containsKey(coordinateKey)) {
                int firstIndex = firstIndexByCoordinate.get(coordinateKey);
                Point repeatedPrevious = vertexIndex > 0
                        ? loopVertices.get(vertexIndex - 1)
                        : null;
                DirectedEdge repeatedIncomingEdge = repeatedPrevious == null
                        ? null
                        : edgeCatalog.resolve(
                                repeatedPrevious.x,
                                repeatedPrevious.y,
                                loopVertex.x,
                                loopVertex.y);
                return buildDuplicateDetailFromIndices(
                        loopVertex.x,
                        loopVertex.y,
                        firstIndex,
                        vertexIndex,
                        loopVertices,
                        Collections.emptyList(),
                        repeatedIncomingEdge,
                        loopNumber,
                        loopVertices.size(),
                        totalExposedEdges,
                        usedExposedEdges,
                        incidentEdgesByVertex,
                        outgoingEdgesByStartVertex,
                        unusedEdges,
                        lumenId,
                        usedDirectedEdges,
                        edgeCatalog,
                        DUPLICATE_CLASSIFICATION_CONVENTIONAL_CLOSURE);
            }

            firstIndexByCoordinate.put(coordinateKey, vertexIndex);
        }

        return null;
    }

    private static DuplicateVertexDetail buildDuplicateDetail(
            int x,
            int y,
            int firstIndex,
            int repeatedIndex,
            List<Point> loopVertices,
            List<VertexVisitRecord> vertexVisitRecords,
            DirectedEdge repeatedIncomingEdge,
            Point repeatedPreviousVertex,
            int loopNumber,
            int loopVertexCount,
            int totalExposedEdges,
            int usedExposedEdges,
            Map<Long, List<DirectedEdge>> incidentEdgesByVertex,
            Map<Long, List<DirectedEdge>> outgoingEdgesByStartVertex,
            Set<DirectedEdge> unusedEdges,
            int lumenId,
            List<DirectedEdge> usedDirectedEdges,
            EdgeCatalog edgeCatalog) {

        String classification = classifyDuplicate(
                x,
                y,
                firstIndex,
                repeatedIndex,
                loopVertices,
                repeatedIncomingEdge,
                usedDirectedEdges,
                incidentEdgesByVertex,
                outgoingEdgesByStartVertex,
                unusedEdges,
                edgeCatalog,
                vertexVisitRecords);

        return buildDuplicateDetailFromIndices(
                x,
                y,
                firstIndex,
                repeatedIndex,
                loopVertices,
                vertexVisitRecords,
                repeatedIncomingEdge,
                loopNumber,
                loopVertexCount,
                totalExposedEdges,
                usedExposedEdges,
                incidentEdgesByVertex,
                outgoingEdgesByStartVertex,
                unusedEdges,
                lumenId,
                usedDirectedEdges,
                edgeCatalog,
                classification);
    }

    private static DuplicateVertexDetail buildDuplicateDetailFromIndices(
            int x,
            int y,
            int firstIndex,
            int repeatedIndex,
            List<Point> loopVertices,
            List<VertexVisitRecord> vertexVisitRecords,
            DirectedEdge repeatedIncomingEdge,
            int loopNumber,
            int loopVertexCount,
            int totalExposedEdges,
            int usedExposedEdges,
            Map<Long, List<DirectedEdge>> incidentEdgesByVertex,
            Map<Long, List<DirectedEdge>> outgoingEdgesByStartVertex,
            Set<DirectedEdge> unusedEdges,
            int lumenId,
            List<DirectedEdge> usedDirectedEdges,
            EdgeCatalog edgeCatalog,
            String classification) {

        Point firstPrevious = firstIndex > 0
                ? loopVertices.get(firstIndex - 1)
                : null;
        Point firstNext = firstIndex + 1 < loopVertices.size()
                ? loopVertices.get(firstIndex + 1)
                : null;
        Point repeatedVertex = repeatedIndex < loopVertices.size()
                ? loopVertices.get(repeatedIndex)
                : new Point(x, y);
        Point repeatedPrevious = repeatedIndex > 0
                && repeatedIndex <= loopVertices.size()
                ? loopVertices.get(repeatedIndex - 1)
                : null;
        Point repeatedNext = repeatedIndex + 1 < loopVertices.size()
                ? loopVertices.get(repeatedIndex + 1)
                : null;

        int firstIncoming = firstPrevious == null
                ? -1
                : directionIndex(
                        firstPrevious.x,
                        firstPrevious.y,
                        loopVertices.get(firstIndex).x,
                        loopVertices.get(firstIndex).y);
        int firstOutgoing = firstNext == null
                ? -1
                : directionIndex(
                        loopVertices.get(firstIndex).x,
                        loopVertices.get(firstIndex).y,
                        firstNext.x,
                        firstNext.y);
        int repeatedIncoming = repeatedPrevious == null
                ? -1
                : directionIndex(
                        repeatedPrevious.x,
                        repeatedPrevious.y,
                        repeatedVertex.x,
                        repeatedVertex.y);
        int repeatedOutgoing = repeatedNext == null
                ? -1
                : directionIndex(
                        repeatedVertex.x,
                        repeatedVertex.y,
                        repeatedNext.x,
                        repeatedNext.y);

        int incidentDirectedEdgeCount = countIncidentDirectedEdges(
                x,
                y,
                incidentEdgesByVertex);
        int unusedOutgoingEdgeCount = countUnusedOutgoingEdges(
                x,
                y,
                outgoingEdgesByStartVertex,
                unusedEdges);
        int inDegree = edgeCatalog.inDegree(x, y);
        int outDegree = edgeCatalog.outDegree(x, y);
        List<Integer> unusedOutgoingEdgeIds =
                edgeCatalog.unusedOutgoingEdgeIds(x, y, unusedEdges);

        int firstIncomingEdgeId = resolveIncomingEdgeId(
                firstPrevious,
                loopVertices.get(firstIndex),
                edgeCatalog);
        int firstOutgoingEdgeId = resolveOutgoingEdgeId(
                loopVertices.get(firstIndex),
                firstNext,
                edgeCatalog,
                firstIndex < vertexVisitRecords.size()
                        ? vertexVisitRecords.get(firstIndex).outgoingEdgeId
                        : -1);
        int repeatedIncomingEdgeId = repeatedIncomingEdge == null
                ? resolveIncomingEdgeId(repeatedPrevious, repeatedVertex, edgeCatalog)
                : repeatedIncomingEdge.getEdgeId();
        int repeatedOutgoingEdgeId = repeatedIndex < vertexVisitRecords.size()
                ? vertexVisitRecords.get(repeatedIndex).outgoingEdgeId
                : -1;

        DirectedEdge firstIncomingEdgeObject = firstPrevious == null
                ? null
                : edgeCatalog.resolve(
                        firstPrevious.x,
                        firstPrevious.y,
                        loopVertices.get(firstIndex).x,
                        loopVertices.get(firstIndex).y);
        DirectedEdge repeatedIncomingEdgeObject = repeatedIncomingEdge == null
                ? (repeatedPrevious == null
                        ? null
                        : edgeCatalog.resolve(
                                repeatedPrevious.x,
                                repeatedPrevious.y,
                                repeatedVertex.x,
                                repeatedVertex.y))
                : repeatedIncomingEdge;
        DirectedEdge firstOutgoingEdgeObject = firstNext == null
                ? null
                : edgeCatalog.resolve(
                        loopVertices.get(firstIndex).x,
                        loopVertices.get(firstIndex).y,
                        firstNext.x,
                        firstNext.y);
        DirectedEdge repeatedOutgoingEdgeObject = repeatedNext == null
                ? null
                : edgeCatalog.resolve(
                        repeatedVertex.x,
                        repeatedVertex.y,
                        repeatedNext.x,
                        repeatedNext.y);

        boolean identicalIncomingEdgeObjects = firstIncomingEdgeObject != null
                && repeatedIncomingEdgeObject != null
                && firstIncomingEdgeObject == repeatedIncomingEdgeObject;
        boolean identicalOutgoingEdgeObjects = firstOutgoingEdgeObject != null
                && repeatedOutgoingEdgeObject != null
                && firstOutgoingEdgeObject == repeatedOutgoingEdgeObject;

        String edgeIdClassification = classifyDuplicateByEdgeIds(
                x,
                y,
                firstIndex,
                repeatedIndex,
                loopVertices,
                firstIncomingEdgeId,
                firstOutgoingEdgeId,
                repeatedIncomingEdgeId,
                repeatedOutgoingEdgeId,
                incidentDirectedEdgeCount,
                edgeCatalog,
                usedDirectedEdges);

        Point firstForegroundFacePixel =
                foregroundFacePixelForIncomingEdge(edgeCatalog, firstIncomingEdgeId);
        Point repeatedForegroundFacePixel =
                foregroundFacePixelForIncomingEdge(edgeCatalog, repeatedIncomingEdgeId);
        List<Long> visitedCoordinateKeysAtFailure = new ArrayList<>();

        for (Point loopVertex : loopVertices) {
            visitedCoordinateKeysAtFailure.add(encodeCoordinate(loopVertex.x, loopVertex.y));
        }

        List<Integer> usedEdgeIdsAtFailure = new ArrayList<>();

        for (DirectedEdge usedEdge : usedDirectedEdges) {
            usedEdgeIdsAtFailure.add(usedEdge.getEdgeId());
        }

        return new DuplicateVertexDetail(
                lumenId,
                x,
                y,
                firstIndex,
                repeatedIndex,
                firstPrevious,
                firstNext,
                repeatedPrevious,
                repeatedNext,
                firstIncoming,
                firstOutgoing,
                repeatedIncoming,
                repeatedOutgoing,
                incidentDirectedEdgeCount,
                unusedOutgoingEdgeCount,
                inDegree,
                outDegree,
                unusedOutgoingEdgeIds,
                identicalIncomingEdgeObjects,
                identicalOutgoingEdgeObjects,
                loopNumber,
                loopVertexCount,
                totalExposedEdges,
                usedExposedEdges,
                classification,
                firstIncomingEdgeId,
                firstOutgoingEdgeId,
                repeatedIncomingEdgeId,
                repeatedOutgoingEdgeId,
                edgeIdClassification,
                firstForegroundFacePixel,
                repeatedForegroundFacePixel,
                visitedCoordinateKeysAtFailure,
                usedEdgeIdsAtFailure);
    }

    private static int resolveIncomingEdgeId(
            Point fromVertex,
            Point toVertex,
            EdgeCatalog edgeCatalog) {

        if (fromVertex == null || toVertex == null) {
            return -1;
        }

        DirectedEdge edge = edgeCatalog.resolve(
                fromVertex.x,
                fromVertex.y,
                toVertex.x,
                toVertex.y);

        return edge == null ? -1 : edge.getEdgeId();
    }

    private static int resolveOutgoingEdgeId(
            Point fromVertex,
            Point toVertex,
            EdgeCatalog edgeCatalog,
            int recordedOutgoingEdgeId) {

        if (recordedOutgoingEdgeId >= 0) {
            return recordedOutgoingEdgeId;
        }

        if (fromVertex == null || toVertex == null) {
            return -1;
        }

        DirectedEdge edge = edgeCatalog.resolve(
                fromVertex.x,
                fromVertex.y,
                toVertex.x,
                toVertex.y);

        return edge == null ? -1 : edge.getEdgeId();
    }

    private static String classifyDuplicateByEdgeIds(
            int x,
            int y,
            int firstIndex,
            int repeatedIndex,
            List<Point> loopVertices,
            int firstIncomingEdgeId,
            int firstOutgoingEdgeId,
            int repeatedIncomingEdgeId,
            int repeatedOutgoingEdgeId,
            int incidentDirectedEdgeCount,
            EdgeCatalog edgeCatalog,
            List<DirectedEdge> usedDirectedEdges) {

        if (repeatedIndex == loopVertices.size()
                && firstIndex == 0
                && loopVertices.get(0).x == x
                && loopVertices.get(0).y == y) {
            return EDGE_ID_CLASSIFICATION_CONVENTIONAL_CLOSURE;
        }

        if (repeatedIncomingEdgeId >= 0) {

            for (DirectedEdge usedEdge : usedDirectedEdges) {

                if (usedEdge.getEdgeId() == repeatedIncomingEdgeId
                        && usedDirectedEdges.indexOf(usedEdge) < usedDirectedEdges.size() - 1) {
                    return EDGE_ID_CLASSIFICATION_SAME_EDGE_ID_REVISITED;
                }
            }
        }

        if (repeatedIncomingEdgeId >= 0 && firstIncomingEdgeId >= 0) {

            if (repeatedIncomingEdgeId == firstIncomingEdgeId) {
                return EDGE_ID_CLASSIFICATION_SAME_EDGE_ID_REVISITED;
            }
        }

        if (incidentDirectedEdgeCount >= 4) {
            return EDGE_ID_CLASSIFICATION_HIGH_DEGREE_VERTEX;
        }

        if (repeatedIncomingEdgeId >= 0 && firstIncomingEdgeId >= 0
                && repeatedIncomingEdgeId != firstIncomingEdgeId) {
            return EDGE_ID_CLASSIFICATION_DIFFERENT_EDGE_IDS_SHARED_VERTEX;
        }

        return EDGE_ID_CLASSIFICATION_UNKNOWN;
    }

    private static DuplicateVertexDetail buildSameDirectedEdgeDuplicate(
            int x,
            int y,
            int firstIndex,
            int repeatedIndex,
            DirectedEdge edge,
            DirectedEdge repeatedEdge,
            int loopNumber,
            int loopVertexCount,
            int totalExposedEdges,
            int usedExposedEdges,
            Map<Long, List<DirectedEdge>> incidentEdgesByVertex,
            Map<Long, List<DirectedEdge>> outgoingEdgesByStartVertex,
            Set<DirectedEdge> unusedEdges,
            int lumenId,
            List<Point> loopVertices,
            List<DirectedEdge> usedDirectedEdges,
            EdgeCatalog edgeCatalog) {

        Point firstPrevious = loopVertices.isEmpty() || firstIndex <= 0
                ? null
                : loopVertices.get(firstIndex - 1);
        Point firstNext = loopVertices.isEmpty() || firstIndex + 1 >= loopVertices.size()
                ? new Point(edge.toX, edge.toY)
                : loopVertices.get(firstIndex + 1);
        Point repeatedPrevious = loopVertices.isEmpty() || repeatedIndex <= 0
                ? null
                : loopVertices.get(repeatedIndex - 1);

        return buildDuplicateDetailFromIndices(
                x,
                y,
                firstIndex,
                repeatedIndex,
                loopVertices,
                Collections.emptyList(),
                repeatedEdge,
                loopNumber,
                loopVertexCount,
                totalExposedEdges,
                usedExposedEdges,
                incidentEdgesByVertex,
                outgoingEdgesByStartVertex,
                unusedEdges,
                lumenId,
                usedDirectedEdges,
                edgeCatalog,
                DUPLICATE_CLASSIFICATION_SAME_DIRECTED_EDGE);
    }

    private static DuplicateVertexDetail buildHighDegreeOrUnknownDuplicate(
            int x,
            int y,
            int firstIndex,
            int repeatedIndex,
            List<Point> loopVertices,
            List<VertexVisitRecord> vertexVisitRecords,
            DirectedEdge repeatedIncomingEdge,
            Point repeatedPreviousVertex,
            int loopNumber,
            int loopVertexCount,
            int totalExposedEdges,
            int usedExposedEdges,
            Map<Long, List<DirectedEdge>> incidentEdgesByVertex,
            Map<Long, List<DirectedEdge>> outgoingEdgesByStartVertex,
            Set<DirectedEdge> unusedEdges,
            int lumenId,
            List<DirectedEdge> usedDirectedEdges,
            EdgeCatalog edgeCatalog,
            String classification) {

        return buildDuplicateDetailFromIndices(
                x,
                y,
                firstIndex,
                repeatedIndex,
                loopVertices,
                vertexVisitRecords,
                repeatedIncomingEdge,
                loopNumber,
                loopVertexCount,
                totalExposedEdges,
                usedExposedEdges,
                incidentEdgesByVertex,
                outgoingEdgesByStartVertex,
                unusedEdges,
                lumenId,
                usedDirectedEdges,
                edgeCatalog,
                classification);
    }

    private static String classifyDuplicate(
            int x,
            int y,
            int firstIndex,
            int repeatedIndex,
            List<Point> loopVertices,
            DirectedEdge repeatedIncomingEdge,
            List<DirectedEdge> usedDirectedEdges,
            Map<Long, List<DirectedEdge>> incidentEdgesByVertex,
            Map<Long, List<DirectedEdge>> outgoingEdgesByStartVertex,
            Set<DirectedEdge> unusedEdges,
            EdgeCatalog edgeCatalog,
            List<VertexVisitRecord> vertexVisitRecords) {

        if (repeatedIndex == loopVertices.size()
                && firstIndex == 0
                && loopVertices.get(0).x == x
                && loopVertices.get(0).y == y) {
            return DUPLICATE_CLASSIFICATION_CONVENTIONAL_CLOSURE;
        }

        if (repeatedIncomingEdge != null) {

            for (DirectedEdge usedEdge : usedDirectedEdges) {

                if (usedEdge.getEdgeId() == repeatedIncomingEdge.getEdgeId()) {
                    return DUPLICATE_CLASSIFICATION_SAME_DIRECTED_EDGE;
                }
            }
        }

        int incidentDirectedEdgeCount = countIncidentDirectedEdges(
                x,
                y,
                incidentEdgesByVertex);

        if (incidentDirectedEdgeCount >= 4) {
            return DUPLICATE_CLASSIFICATION_HIGH_DEGREE;
        }

        return DUPLICATE_CLASSIFICATION_DIFFERENT_EDGES;
    }

    private static int countIncidentDirectedEdges(
            int x,
            int y,
            Map<Long, List<DirectedEdge>> incidentEdgesByVertex) {

        List<DirectedEdge> incidentEdges = incidentEdgesByVertex.getOrDefault(
                encodeCoordinate(x, y),
                Collections.emptyList());

        return incidentEdges.size();
    }

    private static int countUnusedOutgoingEdges(
            int x,
            int y,
            Map<Long, List<DirectedEdge>> outgoingEdgesByStartVertex,
            Set<DirectedEdge> unusedEdges) {

        List<DirectedEdge> outgoingEdges = outgoingEdgesByStartVertex.getOrDefault(
                encodeCoordinate(x, y),
                Collections.emptyList());
        int unusedCount = 0;

        for (DirectedEdge outgoingEdge : outgoingEdges) {

            if (unusedEdges.contains(outgoingEdge)) {
                unusedCount++;
            }
        }

        return unusedCount;
    }

    private static String buildDuplicateTracedVertexMessage(
            DuplicateVertexDetail detail) {

        return "DUPLICATE_TRACED_VERTEX: "
                + "lumenId="
                + detail.lumenId
                + ", "
                + "x="
                + detail.x
                + ", "
                + "y="
                + detail.y
                + ", "
                + "firstIndex="
                + detail.firstIndex
                + ", "
                + "repeatedIndex="
                + detail.repeatedIndex
                + ", "
                + "firstPrevious="
                + formatVertex(detail.firstPrevious)
                + ", "
                + "firstNext="
                + formatVertex(detail.firstNext)
                + ", "
                + "repeatedPrevious="
                + formatVertex(detail.repeatedPrevious)
                + ", "
                + "repeatedNext="
                + formatVertex(detail.repeatedNext)
                + ", "
                + "firstIncoming="
                + formatDirection(detail.firstIncoming)
                + ", "
                + "firstOutgoing="
                + formatDirection(detail.firstOutgoing)
                + ", "
                + "repeatedIncoming="
                + formatDirection(detail.repeatedIncoming)
                + ", "
                + "repeatedOutgoing="
                + formatDirection(detail.repeatedOutgoing)
                + ", "
                + "incidentDirectedEdgeCount="
                + detail.incidentDirectedEdgeCount
                + ", "
                + "unusedOutgoingEdgeCount="
                + detail.unusedOutgoingEdgeCount
                + ", "
                + "loopNumber="
                + detail.loopNumber
                + ", "
                + "loopVertexCount="
                + detail.loopVertexCount
                + ", "
                + "totalExposedEdges="
                + detail.totalExposedEdges
                + ", "
                + "usedExposedEdges="
                + detail.usedExposedEdges
                + ", "
                + "duplicateClassification="
                + detail.edgeIdClassification
                + ", "
                + "legacyDuplicateClassification="
                + detail.classification
                + ", "
                + "firstIncomingEdgeId="
                + detail.firstIncomingEdgeId
                + ", "
                + "firstOutgoingEdgeId="
                + detail.firstOutgoingEdgeId
                + ", "
                + "repeatedIncomingEdgeId="
                + detail.repeatedIncomingEdgeId
                + ", "
                + "repeatedOutgoingEdgeId="
                + detail.repeatedOutgoingEdgeId;
    }

    private static String formatVertex(Point vertex) {
        return vertex == null ? "NONE" : "(" + vertex.x + "," + vertex.y + ")";
    }

    private static String formatDirection(int directionIndex) {

        if (directionIndex < 0) {
            return "NONE";
        }

        switch (directionIndex) {
            case DIRECTION_EAST:
                return "EAST";
            case DIRECTION_SOUTH:
                return "SOUTH";
            case DIRECTION_WEST:
                return "WEST";
            case DIRECTION_NORTH:
                return "NORTH";
            default:
                return "NONE";
        }
    }

    private static DirectedEdge selectSuccessorEdge(
            Point previousVertex,
            Point currentVertex,
            DirectedEdge startEdge,
            Map<Long, List<DirectedEdge>> outgoingEdgesByStartVertex,
            Set<DirectedEdge> unusedEdges,
            Set<Long> lumenCoordinates) {

        List<DirectedEdge> availableOutgoingEdges =
                outgoingEdgesByStartVertex.getOrDefault(
                        encodeCoordinate(currentVertex.x, currentVertex.y),
                        Collections.emptyList());
        int incomingDirection = directionIndex(
                previousVertex.x,
                previousVertex.y,
                currentVertex.x,
                currentVertex.y);
        String topologyClassification = classifyVertexTopology(
                currentVertex.x,
                currentVertex.y,
                lumenCoordinates);
        Integer mappedDirection = mappedDiagonalOutgoingDirection(
                topologyClassification,
                incomingDirection);

        if (mappedDirection != null) {
            DirectedEdge mappedEdge = null;

            for (DirectedEdge candidateEdge : availableOutgoingEdges) {

                if (directionIndex(
                        currentVertex.x,
                        currentVertex.y,
                        candidateEdge.toX,
                        candidateEdge.toY) != mappedDirection) {
                    continue;
                }

                mappedEdge = candidateEdge;
                break;
            }

            boolean allowStartEdgeClosure = mappedEdge != null
                && startEdge != null
                && mappedEdge.equals(startEdge);
            boolean unusedOk = mappedEdge != null
                    && (unusedEdges.contains(mappedEdge) || allowStartEdgeClosure);
            boolean unitOk = mappedEdge != null
                    && isUnitLatticeStep(
                            mappedEdge.fromX,
                            mappedEdge.fromY,
                            mappedEdge.toX,
                            mappedEdge.toY);
            boolean startsOk = mappedEdge != null
                    && mappedEdge.fromX == currentVertex.x
                    && mappedEdge.fromY == currentVertex.y;

            if (mappedEdge == null || !startsOk || !unitOk || !unusedOk) {
                throw new IllegalStateException(
                        DIAGONAL_JUNCTION_SUCCESSOR_INVALID
                                + ":topology="
                                + topologyClassification
                                + ",incoming="
                                + formatDirection(incomingDirection)
                                + ",mappedDirection="
                                + formatDirection(mappedDirection)
                                + ",vertex=("
                                + currentVertex.x
                                + ","
                                + currentVertex.y
                                + ")");
            }

            return mappedEdge;
        }

        for (int turnOffset : new int[] {1, 0, 3, 2}) {
            int preferredDirection = (incomingDirection + turnOffset) % 4;

            for (DirectedEdge candidateEdge : availableOutgoingEdges) {

                if (!unusedEdges.contains(candidateEdge)
                        && !candidateEdge.equals(startEdge)) {
                    continue;
                }

                if (directionIndex(
                        currentVertex.x,
                        currentVertex.y,
                        candidateEdge.toX,
                        candidateEdge.toY) == preferredDirection) {
                    return candidateEdge;
                }
            }
        }

        throw new IllegalStateException(
                "Boundary edge cannot be continued at vertex ("
                        + currentVertex.x
                        + ","
                        + currentVertex.y
                        + ").");
    }

    static String classifyVertexTopology(
            int vertexX,
            int vertexY,
            Set<Long> lumenCoordinates) {

        int northWestX = vertexX - 1;
        int northWestY = vertexY - 1;
        int northEastX = vertexX;
        int northEastY = vertexY - 1;
        int southWestX = vertexX - 1;
        int southWestY = vertexY;
        int southEastX = vertexX;
        int southEastY = vertexY;

        boolean northWestInBounds = northWestX >= 0 && northWestY >= 0;
        boolean northEastInBounds = northEastX >= 0 && northEastY >= 0;
        boolean southWestInBounds = southWestX >= 0 && southWestY >= 0;
        boolean southEastInBounds = southEastX >= 0 && southEastY >= 0;
        boolean anyOutOfBounds = !northWestInBounds
                || !northEastInBounds
                || !southWestInBounds
                || !southEastInBounds;

        boolean northWestForeground = northWestInBounds
                && containsPixel(lumenCoordinates, northWestX, northWestY);
        boolean northEastForeground = northEastInBounds
                && containsPixel(lumenCoordinates, northEastX, northEastY);
        boolean southWestForeground = southWestInBounds
                && containsPixel(lumenCoordinates, southWestX, southWestY);
        boolean southEastForeground = southEastInBounds
                && containsPixel(lumenCoordinates, southEastX, southEastY);

        return classifyQuadrantTopology(
                northWestForeground,
                northEastForeground,
                southWestForeground,
                southEastForeground,
                anyOutOfBounds);
    }

    static Integer mappedDiagonalOutgoingDirection(
            String topologyClassification,
            int incomingDirection) {

        if (TOPOLOGY_TWO_DIAGONAL_FOREGROUND_NW_SE.equals(topologyClassification)) {

            switch (incomingDirection) {
                case DIRECTION_SOUTH:
                    return DIRECTION_EAST;
                case DIRECTION_NORTH:
                    return DIRECTION_WEST;
                case DIRECTION_EAST:
                    return DIRECTION_SOUTH;
                case DIRECTION_WEST:
                    return DIRECTION_NORTH;
                default:
                    return null;
            }
        }

        if (TOPOLOGY_TWO_DIAGONAL_FOREGROUND_NE_SW.equals(topologyClassification)) {

            switch (incomingDirection) {
                case DIRECTION_SOUTH:
                    return DIRECTION_WEST;
                case DIRECTION_NORTH:
                    return DIRECTION_EAST;
                case DIRECTION_EAST:
                    return DIRECTION_NORTH;
                case DIRECTION_WEST:
                    return DIRECTION_SOUTH;
                default:
                    return null;
            }
        }

        return null;
    }

    private static DiagonalMappedSuccessor resolveDiagonalMappedSuccessor(
            Point currentVertex,
            int mappedDirection,
            List<DirectedEdge> availableOutgoingEdges,
            Set<DirectedEdge> unusedEdges,
            DirectedEdge startEdge,
            EdgeCatalog edgeCatalog) {

        DirectedEdge mappedEdge = findOutgoingEdgeInDirection(
                currentVertex,
                mappedDirection,
                availableOutgoingEdges,
                edgeCatalog);

        if (mappedEdge == null) {
            mappedEdge = null;

            for (DirectedEdge candidateEdge : availableOutgoingEdges) {

                if (directionIndex(
                        currentVertex.x,
                        currentVertex.y,
                        candidateEdge.toX,
                        candidateEdge.toY) == mappedDirection) {
                    mappedEdge = candidateEdge;
                    break;
                }
            }
        }

        boolean startsAtCurrentVertex = mappedEdge != null
                && mappedEdge.fromX == currentVertex.x
                && mappedEdge.fromY == currentVertex.y;
        boolean exposed = mappedEdge != null
                && (edgeCatalog.exposedEdgeSet().contains(mappedEdge)
                        || availableOutgoingEdges.contains(mappedEdge));
        boolean unitOk = mappedEdge != null
                && isUnitLatticeStep(
                        mappedEdge.fromX,
                        mappedEdge.fromY,
                        mappedEdge.toX,
                        mappedEdge.toY);
        boolean allowStartEdgeClosure = mappedEdge != null
                && startEdge != null
                && mappedEdge.equals(startEdge);
        boolean unusedOk = mappedEdge != null
                && (unusedEdges.contains(mappedEdge) || allowStartEdgeClosure);
        boolean valid = startsAtCurrentVertex && exposed && unitOk && unusedOk;

        return new DiagonalMappedSuccessor(mappedEdge, valid);
    }

    private static final class DiagonalMappedSuccessor {

        private final DirectedEdge edge;
        private final boolean valid;

        private DiagonalMappedSuccessor(DirectedEdge edge, boolean valid) {
            this.edge = edge;
            this.valid = valid;
        }
    }

    public static double signedPolygonArea(List<Point> loopVertices) {

        double twiceSignedArea = 0.0;
        int vertexCount = loopVertices.size();

        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            Point currentVertex = loopVertices.get(vertexIndex);
            Point nextVertex = loopVertices.get((vertexIndex + 1) % vertexCount);
            twiceSignedArea +=
                    (long) currentVertex.x * nextVertex.y
                            - (long) nextVertex.x * currentVertex.y;
        }

        return twiceSignedArea / 2.0;
    }

    private static String lexicographicLoopKey(List<Point> loopVertices) {

        StringBuilder builder = new StringBuilder();

        for (Point loopVertex : loopVertices) {
            builder.append(loopVertex.y)
                    .append(',')
                    .append(loopVertex.x)
                    .append(';');
        }

        return builder.toString();
    }

    private static int directionIndex(int fromX, int fromY, int toX, int toY) {

        int deltaX = toX - fromX;
        int deltaY = toY - fromY;

        if (deltaX == 1 && deltaY == 0) {
            return DIRECTION_EAST;
        }

        if (deltaX == 0 && deltaY == 1) {
            return DIRECTION_SOUTH;
        }

        if (deltaX == -1 && deltaY == 0) {
            return DIRECTION_WEST;
        }

        if (deltaX == 0 && deltaY == -1) {
            return DIRECTION_NORTH;
        }

        throw new IllegalStateException(
                "Boundary edge must be axis-aligned with unit length.");
    }

    static Point lumenPixelOnRightSideOfEdge(DirectedEdge edge) {

        int deltaX = edge.toX - edge.fromX;
        int deltaY = edge.toY - edge.fromY;

        if (deltaX == 1 && deltaY == 0) {
            return new Point(edge.fromX, edge.fromY);
        }

        if (deltaX == 0 && deltaY == 1) {
            return new Point(edge.fromX - 1, edge.fromY);
        }

        if (deltaX == -1 && deltaY == 0) {
            return new Point(edge.fromX - 1, edge.fromY - 1);
        }

        if (deltaX == 0 && deltaY == -1) {
            return new Point(edge.fromX, edge.toY);
        }

        return null;
    }

    static Point leftAdjacentPixelOfEdge(DirectedEdge edge) {

        int deltaX = edge.toX - edge.fromX;
        int deltaY = edge.toY - edge.fromY;

        if (deltaX == 1 && deltaY == 0) {
            return new Point(edge.fromX, edge.fromY - 1);
        }

        if (deltaX == 0 && deltaY == 1) {
            return new Point(edge.fromX, edge.fromY);
        }

        if (deltaX == -1 && deltaY == 0) {
            return new Point(edge.fromX - 1, edge.fromY);
        }

        if (deltaX == 0 && deltaY == -1) {
            return new Point(edge.fromX - 1, edge.toY);
        }

        return null;
    }

    static Point rightAdjacentPixelOfEdge(DirectedEdge edge) {
        return lumenPixelOnRightSideOfEdge(edge);
    }

    static boolean isOrientationInvariantSatisfied(
            DirectedEdge edge,
            Set<Long> lumenCoordinates) {

        Point rightPixel = rightAdjacentPixelOfEdge(edge);
        Point leftPixel = leftAdjacentPixelOfEdge(edge);

        if (rightPixel == null || leftPixel == null) {
            return false;
        }

        return containsPixel(lumenCoordinates, rightPixel.x, rightPixel.y)
                && !containsPixel(lumenCoordinates, leftPixel.x, leftPixel.y);
    }

    static String classifyQuadrantTopology(
            boolean northWestForeground,
            boolean northEastForeground,
            boolean southWestForeground,
            boolean southEastForeground,
            boolean anyOutOfBounds) {

        if (anyOutOfBounds) {
            return TOPOLOGY_OUT_OF_BOUNDS;
        }

        int foregroundCount = 0;

        if (northWestForeground) {
            foregroundCount++;
        }

        if (northEastForeground) {
            foregroundCount++;
        }

        if (southWestForeground) {
            foregroundCount++;
        }

        if (southEastForeground) {
            foregroundCount++;
        }

        switch (foregroundCount) {
            case 0:
                return TOPOLOGY_ALL_BACKGROUND;
            case 1:
                return TOPOLOGY_SINGLE_FOREGROUND_QUADRANT;
            case 2:
                if (northWestForeground && northEastForeground) {
                    return TOPOLOGY_TWO_ADJACENT_FOREGROUND;
                }

                if (northEastForeground && southEastForeground) {
                    return TOPOLOGY_TWO_ADJACENT_FOREGROUND;
                }

                if (southEastForeground && southWestForeground) {
                    return TOPOLOGY_TWO_ADJACENT_FOREGROUND;
                }

                if (southWestForeground && northWestForeground) {
                    return TOPOLOGY_TWO_ADJACENT_FOREGROUND;
                }

                if (northWestForeground && southEastForeground) {
                    return TOPOLOGY_TWO_DIAGONAL_FOREGROUND_NW_SE;
                }

                if (northEastForeground && southWestForeground) {
                    return TOPOLOGY_TWO_DIAGONAL_FOREGROUND_NE_SW;
                }

                return TOPOLOGY_UNKNOWN;
            case 3:
                return TOPOLOGY_THREE_FOREGROUND_QUADRANTS;
            case 4:
                return TOPOLOGY_ALL_FOREGROUND;
            default:
                return TOPOLOGY_UNKNOWN;
        }
    }

    static VertexTopologyDiagnostic buildVertexTopologyDiagnostic(
            int vertexX,
            int vertexY,
            Set<Long> lumenCoordinates,
            EdgeCatalog edgeCatalog,
            Map<Long, List<DirectedEdge>> incidentEdgesByVertex,
            Map<Long, List<DirectedEdge>> outgoingEdgesByStartVertex,
            DuplicateVertexDetail failureDetail,
            Set<Long> visitedCoordinateKeys,
            Set<Integer> usedEdgeIdsAtFailure) {

        long vertexKey = encodeCoordinate(vertexX, vertexY);
        List<QuadrantPixelDiagnostic> quadrantDiagnostics = new ArrayList<>();
        boolean anyOutOfBounds = false;
        int[][] quadrantCoords = new int[][] {
                {-1, -1},
                {0, -1},
                {-1, 0},
                {0, 0}
        };
        String[] quadrantNames = new String[] {"NW", "NE", "SW", "SE"};
        boolean[] quadrantForeground = new boolean[4];

        for (int quadrantIndex = 0; quadrantIndex < 4; quadrantIndex++) {
            int pixelX = vertexX + quadrantCoords[quadrantIndex][0];
            int pixelY = vertexY + quadrantCoords[quadrantIndex][1];
            boolean withinBounds = pixelX >= 0 && pixelY >= 0;
            boolean belongsToLumen = withinBounds
                    && containsPixel(lumenCoordinates, pixelX, pixelY);
            boolean contributesIncidentEdge = edgeContributesAtQuadrant(
                    vertexX,
                    vertexY,
                    pixelX,
                    pixelY,
                    incidentEdgesByVertex.getOrDefault(vertexKey, Collections.emptyList()));

            if (!withinBounds) {
                anyOutOfBounds = true;
            }

            quadrantForeground[quadrantIndex] = belongsToLumen;
            quadrantDiagnostics.add(new QuadrantPixelDiagnostic(
                    quadrantNames[quadrantIndex],
                    pixelX,
                    pixelY,
                    withinBounds,
                    belongsToLumen,
                    contributesIncidentEdge));
        }

        String topologyClassification = classifyQuadrantTopology(
                quadrantForeground[0],
                quadrantForeground[1],
                quadrantForeground[2],
                quadrantForeground[3],
                anyOutOfBounds);

        List<DirectedEdge> incidentEdges =
                incidentEdgesByVertex.getOrDefault(vertexKey, Collections.emptyList());
        List<Integer> incomingEdgeIds = new ArrayList<>();
        List<Integer> outgoingEdgeIds = new ArrayList<>();
        List<EdgeFaceDiagnostic> edgeFaceDiagnostics = new ArrayList<>();

        for (DirectedEdge incidentEdge : incidentEdges) {
            if (incidentEdge.endKey() == vertexKey) {
                incomingEdgeIds.add(incidentEdge.getEdgeId());
            }

            if (incidentEdge.startKey() == vertexKey) {
                outgoingEdgeIds.add(incidentEdge.getEdgeId());
            }

            edgeFaceDiagnostics.add(buildEdgeFaceDiagnostic(incidentEdge, lumenCoordinates));
        }

        Collections.sort(incomingEdgeIds);
        Collections.sort(outgoingEdgeIds);

        List<Integer> unusedOutgoingEdgeIds = failureDetail == null
                ? edgeCatalog.unusedOutgoingEdgeIds(
                        vertexX,
                        vertexY,
                        Collections.emptySet())
                : failureDetail.unusedOutgoingEdgeIds;

        List<BranchPairingDiagnostic> branchPairings = buildBranchPairingDiagnostics(
                vertexX,
                vertexY,
                incidentEdges,
                outgoingEdgesByStartVertex,
                lumenCoordinates,
                failureDetail,
                visitedCoordinateKeys,
                usedEdgeIdsAtFailure,
                unusedOutgoingEdgeIds);

        int foregroundCount = 0;

        for (boolean foreground : quadrantForeground) {

            if (foreground) {
                foregroundCount++;
            }
        }

        return new VertexTopologyDiagnostic(
                new Point(vertexX, vertexY),
                quadrantForeground[0],
                quadrantForeground[1],
                quadrantForeground[2],
                quadrantForeground[3],
                foregroundCount,
                topologyClassification,
                incomingEdgeIds,
                outgoingEdgeIds,
                unusedOutgoingEdgeIds,
                quadrantDiagnostics,
                edgeFaceDiagnostics,
                branchPairings);
    }

    private static boolean edgeContributesAtQuadrant(
            int vertexX,
            int vertexY,
            int pixelX,
            int pixelY,
            List<DirectedEdge> incidentEdges) {

        for (DirectedEdge edge : incidentEdges) {
            Point rightPixel = lumenPixelOnRightSideOfEdge(edge);

            if (rightPixel != null && rightPixel.x == pixelX && rightPixel.y == pixelY) {
                return true;
            }
        }

        return false;
    }

    private static EdgeFaceDiagnostic buildEdgeFaceDiagnostic(
            DirectedEdge edge,
            Set<Long> lumenCoordinates) {

        Point leftPixel = leftAdjacentPixelOfEdge(edge);
        Point rightPixel = rightAdjacentPixelOfEdge(edge);
        int direction = directionIndex(edge.fromX, edge.fromY, edge.toX, edge.toY);

        return new EdgeFaceDiagnostic(
                edge.getEdgeId(),
                formatDirection(direction),
                edge.fromX,
                edge.fromY,
                edge.toX,
                edge.toY,
                leftPixel,
                rightPixel,
                leftPixel != null
                        && containsPixel(lumenCoordinates, leftPixel.x, leftPixel.y),
                rightPixel != null
                        && containsPixel(lumenCoordinates, rightPixel.x, rightPixel.y),
                EXPOSED_EDGE_LUMEN_SIDE_CONVENTION,
                isOrientationInvariantSatisfied(edge, lumenCoordinates));
    }

    private static List<BranchPairingDiagnostic> buildBranchPairingDiagnostics(
            int vertexX,
            int vertexY,
            List<DirectedEdge> incidentEdges,
            Map<Long, List<DirectedEdge>> outgoingEdgesByStartVertex,
            Set<Long> lumenCoordinates,
            DuplicateVertexDetail failureDetail,
            Set<Long> visitedCoordinateKeys,
            Set<Integer> usedEdgeIdsAtFailure,
            List<Integer> unusedOutgoingEdgeIds) {

        List<DirectedEdge> incomingEdges = new ArrayList<>();
        List<DirectedEdge> outgoingEdges = new ArrayList<>();

        for (DirectedEdge edge : incidentEdges) {

            if (edge.toX == vertexX && edge.toY == vertexY) {
                incomingEdges.add(edge);
            }

            if (edge.fromX == vertexX && edge.fromY == vertexY) {
                outgoingEdges.add(edge);
            }
        }

        List<BranchPairingDiagnostic> pairings = new ArrayList<>();
        Set<Integer> unusedOutgoingSet = new HashSet<>(unusedOutgoingEdgeIds);

        for (DirectedEdge incomingEdge : incomingEdges) {
            int incomingDirection = directionIndex(
                    incomingEdge.fromX,
                    incomingEdge.fromY,
                    incomingEdge.toX,
                    incomingEdge.toY);
            Point incomingFace = lumenPixelOnRightSideOfEdge(incomingEdge);
            DirectedEdge ruleSelected = selectSuccessorByCurrentRule(
                    incomingEdge,
                    vertexX,
                    vertexY,
                    outgoingEdgesByStartVertex,
                    unusedOutgoingSet);

            for (DirectedEdge outgoingEdge : outgoingEdges) {
                int outgoingDirection = directionIndex(
                        outgoingEdge.fromX,
                        outgoingEdge.fromY,
                        outgoingEdge.toX,
                        outgoingEdge.toY);
                int relativeTurn = (outgoingDirection - incomingDirection + 4) % 4;
                int priorityRank = successorPriorityRank(relativeTurn);
                Point outgoingFace = lumenPixelOnRightSideOfEdge(outgoingEdge);
                boolean preservesFace = incomingFace != null
                        && outgoingFace != null
                        && incomingFace.x == outgoingFace.x
                        && incomingFace.y == outgoingFace.y;
                boolean alreadyUsed = !unusedOutgoingSet.contains(outgoingEdge.getEdgeId());
                boolean selectedByRule = ruleSelected != null
                        && ruleSelected.getEdgeId() == outgoingEdge.getEdgeId();
                boolean selectedByObserved = failureDetail != null
                        && ((incomingEdge.getEdgeId() == failureDetail.firstIncomingEdgeId
                                && outgoingEdge.getEdgeId()
                                        == failureDetail.firstOutgoingEdgeId)
                        || (incomingEdge.getEdgeId()
                                == failureDetail.repeatedIncomingEdgeId
                                && outgoingEdge.getEdgeId()
                                        == failureDetail.repeatedOutgoingEdgeId));
                boolean wouldReuse = usedEdgeIdsAtFailure != null
                        && usedEdgeIdsAtFailure.contains(outgoingEdge.getEdgeId());
                boolean wouldReturn = visitedCoordinateKeys != null
                        && visitedCoordinateKeys.contains(outgoingEdge.endKey())
                        && !(outgoingEdge.toX == vertexX && outgoingEdge.toY == vertexY);

                String pairingClassification = classifyBranchPairing(
                        preservesFace,
                        wouldReuse,
                        wouldReturn,
                        incomingEdge,
                        outgoingEdge);

                pairings.add(new BranchPairingDiagnostic(
                        incomingEdge.getEdgeId(),
                        formatDirection(incomingDirection),
                        outgoingEdge.getEdgeId(),
                        formatDirection(outgoingDirection),
                        relativeTurn,
                        alreadyUsed,
                        priorityRank,
                        selectedByRule || selectedByObserved,
                        preservesFace,
                        wouldReturn,
                        wouldReuse,
                        pairingClassification));
            }
        }

        pairings.sort(Comparator
                .comparingInt(BranchPairingDiagnostic::getIncomingEdgeId)
                .thenComparingInt(BranchPairingDiagnostic::getOutgoingEdgeId));

        return pairings;
    }

    private static DirectedEdge selectSuccessorByCurrentRule(
            DirectedEdge incomingEdge,
            int vertexX,
            int vertexY,
            Map<Long, List<DirectedEdge>> outgoingEdgesByStartVertex,
            Set<Integer> unusedOutgoingEdgeIds) {

        int incomingDirection = directionIndex(
                incomingEdge.fromX,
                incomingEdge.fromY,
                incomingEdge.toX,
                incomingEdge.toY);
        List<DirectedEdge> availableOutgoing = outgoingEdgesByStartVertex.getOrDefault(
                encodeCoordinate(vertexX, vertexY),
                Collections.emptyList());

        for (int turnOffset : new int[] {1, 0, 3, 2}) {
            int preferredDirection = (incomingDirection + turnOffset) % 4;

            for (DirectedEdge candidate : availableOutgoing) {

                if (!unusedOutgoingEdgeIds.contains(candidate.getEdgeId())) {
                    continue;
                }

                int candidateDirection = directionIndex(
                        candidate.fromX,
                        candidate.fromY,
                        candidate.toX,
                        candidate.toY);

                if (candidateDirection == preferredDirection) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private static int successorPriorityRank(int relativeTurn) {

        switch (relativeTurn) {
            case 1:
                return 0;
            case 0:
                return 1;
            case 3:
                return 2;
            case 2:
                return 3;
            default:
                return -1;
        }
    }

    private static String classifyBranchPairing(
            boolean preservesFace,
            boolean wouldReuse,
            boolean wouldReturn,
            DirectedEdge incomingEdge,
            DirectedEdge outgoingEdge) {

        if (incomingEdge.toX != outgoingEdge.fromX
                || incomingEdge.toY != outgoingEdge.fromY
                || !isUnitLatticeStep(
                        outgoingEdge.fromX,
                        outgoingEdge.fromY,
                        outgoingEdge.toX,
                        outgoingEdge.toY)) {
            return PAIRING_INVALID_GEOMETRY;
        }

        if (wouldReuse) {
            return PAIRING_REUSES_DIRECTED_EDGE;
        }

        if (wouldReturn) {
            return PAIRING_RETURNS_TO_SHARED_LATTICE_VERTEX;
        }

        if (preservesFace) {
            return PAIRING_VALID_FACE_CONTINUATION;
        }

        return PAIRING_CHANGES_FOREGROUND_FACE;
    }

    static Point foregroundFacePixelForIncomingEdge(
            EdgeCatalog edgeCatalog,
            int incomingEdgeId) {

        if (incomingEdgeId < 0) {
            return null;
        }

        DirectedEdge edge = edgeCatalog.edgeById(incomingEdgeId);

        if (edge == null) {
            return null;
        }

        return lumenPixelOnRightSideOfEdge(edge);
    }

    static boolean sameTopologicalState(
            int vertexX,
            int vertexY,
            int firstIncomingEdgeId,
            int repeatedIncomingEdgeId,
            Point firstForegroundFacePixel,
            Point repeatedForegroundFacePixel) {

        if (firstIncomingEdgeId != repeatedIncomingEdgeId) {
            return false;
        }

        if (firstForegroundFacePixel == null || repeatedForegroundFacePixel == null) {
            return false;
        }

        return firstForegroundFacePixel.x == repeatedForegroundFacePixel.x
                && firstForegroundFacePixel.y == repeatedForegroundFacePixel.y;
    }

    private static boolean containsPixel(Set<Long> lumenCoordinates, int x, int y) {
        return lumenCoordinates.contains(encodeCoordinate(x, y));
    }

    private static int compareVertices(int leftY, int leftX, int rightY, int rightX) {

        if (leftY != rightY) {
            return Integer.compare(leftY, rightY);
        }

        return Integer.compare(leftX, rightX);
    }

    private static List<Point> defensiveCopyList(List<Point> points) {

        List<Point> copiedPoints = new ArrayList<>(points.size());

        for (Point point : points) {
            copiedPoints.add(new Point(point));
        }

        return Collections.unmodifiableList(copiedPoints);
    }

    private static long encodeCoordinate(int x, int y) {
        return (((long) y) << 32) | (x & 0xFFFFFFFFL);
    }

    private static int decodeX(long coordinateKey) {
        return (int) coordinateKey;
    }

    private static int decodeY(long coordinateKey) {
        return (int) (coordinateKey >> 32);
    }

    private static long encodeEdgeKey(int fromX, int fromY, int toX, int toY) {
        return ((fromY & 0xFFFFL) << 48)
                | ((fromX & 0xFFFFL) << 32)
                | ((toY & 0xFFFFL) << 16)
                | (toX & 0xFFFFL);
    }

    private static final class TraceOptions {

        private final boolean strict;
        private final int maximumRecordedSteps;

        private TraceOptions(boolean strict, int maximumRecordedSteps) {
            this.strict = strict;
            this.maximumRecordedSteps = maximumRecordedSteps;
        }

        static TraceOptions strict() {
            return new TraceOptions(true, 0);
        }

        static TraceOptions lenient() {
            return new TraceOptions(false, 0);
        }

        static TraceOptions debug(int maximumRecordedSteps) {
            return new TraceOptions(false, maximumRecordedSteps);
        }

        boolean isStrict() {
            return strict;
        }

        boolean shouldRecordSteps() {
            return maximumRecordedSteps > 0;
        }

        int getMaximumRecordedSteps() {
            return maximumRecordedSteps;
        }
    }

    private static final class EdgeCatalog {

        private static final Comparator<DirectedEdge> EDGE_ID_ORDER =
                Comparator
                        .comparingInt((DirectedEdge edge) -> edge.fromY)
                        .thenComparingInt(edge -> edge.fromX)
                        .thenComparingInt(edge -> edge.toY)
                        .thenComparingInt(edge -> edge.toX);

        private final List<DirectedEdge> edges;
        private final Map<Long, DirectedEdge> canonicalEdgeByKey;
        private final Set<DirectedEdge> exposedEdgeSet;

        private EdgeCatalog(
                List<DirectedEdge> edges,
                Map<Long, DirectedEdge> canonicalEdgeByKey,
                Set<DirectedEdge> exposedEdgeSet) {

            this.edges = edges;
            this.canonicalEdgeByKey = canonicalEdgeByKey;
            this.exposedEdgeSet = exposedEdgeSet;
        }

        static EdgeCatalog build(List<DirectedEdge> exposedEdges) {

            List<DirectedEdge> sortedEdges = new ArrayList<>(exposedEdges);
            sortedEdges.sort(EDGE_ID_ORDER);

            for (int edgeIndex = 0; edgeIndex < sortedEdges.size(); edgeIndex++) {
                sortedEdges.get(edgeIndex).edgeId = edgeIndex;
            }

            Map<Long, DirectedEdge> canonicalEdgeByKey = new HashMap<>();

            for (DirectedEdge edge : sortedEdges) {
                canonicalEdgeByKey.put(
                        encodeEdgeKey(edge.fromX, edge.fromY, edge.toX, edge.toY),
                        edge);
            }

            return new EdgeCatalog(
                    Collections.unmodifiableList(sortedEdges),
                    Collections.unmodifiableMap(canonicalEdgeByKey),
                    Collections.unmodifiableSet(new HashSet<>(sortedEdges)));
        }

        int size() {
            return edges.size();
        }

        Set<DirectedEdge> exposedEdgeSet() {
            return exposedEdgeSet;
        }

        DirectedEdge resolve(int fromX, int fromY, int toX, int toY) {
            return canonicalEdgeByKey.get(encodeEdgeKey(fromX, fromY, toX, toY));
        }

        DirectedEdge edgeById(int edgeId) {

            if (edgeId < 0 || edgeId >= edges.size()) {
                return null;
            }

            return edges.get(edgeId);
        }

        List<Integer> unusedOutgoingEdgeIds(
                int vertexX,
                int vertexY,
                Set<DirectedEdge> unusedEdges) {

            List<Integer> unusedOutgoingEdgeIds = new ArrayList<>();
            long vertexKey = encodeCoordinate(vertexX, vertexY);

            for (DirectedEdge edge : edges) {

                if (edge.startKey() == vertexKey && unusedEdges.contains(edge)) {
                    unusedOutgoingEdgeIds.add(edge.edgeId);
                }
            }

            return unusedOutgoingEdgeIds;
        }

        int inDegree(int vertexX, int vertexY) {

            int degree = 0;
            long vertexKey = encodeCoordinate(vertexX, vertexY);

            for (DirectedEdge edge : edges) {

                if (edge.endKey() == vertexKey) {
                    degree++;
                }
            }

            return degree;
        }

        int outDegree(int vertexX, int vertexY) {

            int degree = 0;
            long vertexKey = encodeCoordinate(vertexX, vertexY);

            for (DirectedEdge edge : edges) {

                if (edge.startKey() == vertexKey) {
                    degree++;
                }
            }

            return degree;
        }
    }

    private static final class SuccessorEvaluationResult {

        private final DirectedEdge selectedEdge;
        private final List<SuccessorCandidateDiagnostic> candidates;
        private final boolean selectionFailed;
        private final String topologyClassification;
        private final boolean diagonalJunctionRuleApplied;
        private final String diagonalMappedDirection;
        private final int diagonalMappedEdgeId;
        private final boolean diagonalMappingValid;
        private final String selectionReason;

        private SuccessorEvaluationResult(
                DirectedEdge selectedEdge,
                List<SuccessorCandidateDiagnostic> candidates,
                boolean selectionFailed,
                String topologyClassification,
                boolean diagonalJunctionRuleApplied,
                String diagonalMappedDirection,
                int diagonalMappedEdgeId,
                boolean diagonalMappingValid,
                String selectionReason) {

            this.selectedEdge = selectedEdge;
            this.candidates = candidates;
            this.selectionFailed = selectionFailed;
            this.topologyClassification = topologyClassification;
            this.diagonalJunctionRuleApplied = diagonalJunctionRuleApplied;
            this.diagonalMappedDirection = diagonalMappedDirection;
            this.diagonalMappedEdgeId = diagonalMappedEdgeId;
            this.diagonalMappingValid = diagonalMappingValid;
            this.selectionReason = selectionReason;
        }
    }

    private static final class InvariantFailureTracker {

        private String firstFailureCode;
        private int firstFailureStep = -1;
        private String firstFailureMessage;

        void recordIfFirst(int stepNumber, String failureCode, String failureMessage) {

            if (firstFailureCode == null && failureCode != null) {
                firstFailureCode = failureCode;
                firstFailureStep = stepNumber;
                firstFailureMessage = failureMessage;
            }
        }

        String getFirstFailureCode() {
            return firstFailureCode;
        }

        int getFirstFailureStep() {
            return firstFailureStep;
        }

        String getFirstFailureMessage() {
            return firstFailureMessage;
        }
    }

    private static final class BoundaryTraceOutcome {

        private final TraceDiagnostics diagnostics;
        private final List<Point> selectedOuterLoop;
        private final String failureReason;
        private final DuplicateVertexDetail duplicateDetail;
        private final int lumenId;
        private final int exposedDirectedEdgeCount;
        private final LoopTraceRecord selectedLoopRecord;
        private final LoopTraceRecord failingLoopRecord;
        private final LoopTraceRecord diagnosticLoopRecord;
        private final EdgeCatalog edgeCatalog;
        private final Set<Long> lumenCoordinates;

        private BoundaryTraceOutcome(
                TraceDiagnostics diagnostics,
                List<Point> selectedOuterLoop,
                String failureReason,
                DuplicateVertexDetail duplicateDetail,
                int lumenId,
                int exposedDirectedEdgeCount,
                LoopTraceRecord selectedLoopRecord,
                LoopTraceRecord failingLoopRecord,
                LoopTraceRecord diagnosticLoopRecord,
                EdgeCatalog edgeCatalog,
                Set<Long> lumenCoordinates) {

            this.diagnostics = diagnostics;
            this.selectedOuterLoop = selectedOuterLoop;
            this.failureReason = failureReason;
            this.duplicateDetail = duplicateDetail;
            this.lumenId = lumenId;
            this.exposedDirectedEdgeCount = exposedDirectedEdgeCount;
            this.selectedLoopRecord = selectedLoopRecord;
            this.failingLoopRecord = failingLoopRecord;
            this.diagnosticLoopRecord = diagnosticLoopRecord;
            this.edgeCatalog = edgeCatalog;
            this.lumenCoordinates = lumenCoordinates == null
                    ? Collections.emptySet()
                    : Collections.unmodifiableSet(new HashSet<>(lumenCoordinates));
        }

        static BoundaryTraceOutcome success(
                TraceDiagnostics diagnostics,
                List<Point> selectedOuterLoop,
                int lumenId,
                int exposedDirectedEdgeCount,
                LoopTraceRecord selectedLoopRecord,
                LoopTraceRecord failingLoopRecord,
                LoopTraceRecord diagnosticLoopRecord,
                EdgeCatalog edgeCatalog,
                Set<Long> lumenCoordinates) {

            return new BoundaryTraceOutcome(
                    diagnostics,
                    selectedOuterLoop,
                    null,
                    null,
                    lumenId,
                    exposedDirectedEdgeCount,
                    selectedLoopRecord,
                    failingLoopRecord,
                    diagnosticLoopRecord,
                    edgeCatalog,
                    lumenCoordinates);
        }

        static BoundaryTraceOutcome failure(
                TraceDiagnostics diagnostics,
                String failureReason,
                DuplicateVertexDetail duplicateDetail,
                int lumenId,
                int exposedDirectedEdgeCount,
                LoopTraceRecord selectedLoopRecord,
                LoopTraceRecord failingLoopRecord,
                LoopTraceRecord diagnosticLoopRecord,
                EdgeCatalog edgeCatalog,
                Set<Long> lumenCoordinates) {

            return new BoundaryTraceOutcome(
                    diagnostics,
                    diagnostics.getSelectedOuterLoopVertices(),
                    failureReason,
                    duplicateDetail,
                    lumenId,
                    exposedDirectedEdgeCount,
                    selectedLoopRecord,
                    failingLoopRecord,
                    diagnosticLoopRecord,
                    edgeCatalog,
                    lumenCoordinates);
        }

        TraceDebugReport buildDebugReport(int maximumRecordedSteps) {

            LoopTraceRecord diagnosticRecord = diagnosticLoopRecord;
            DuplicateVertexDetail failureDetail = duplicateDetail != null
                    ? duplicateDetail
                    : (diagnosticRecord == null ? null : diagnosticRecord.duplicateDetail);

            RepeatedVertexEdgeAnalysis repeatedVertexEdgeAnalysis = failureDetail == null
                    ? null
                    : buildRepeatedVertexEdgeAnalysis(failureDetail, edgeCatalog);

            int diagnosticStartingEdgeId = -1;
            Point diagnosticStartingEdgeStart = null;
            Point diagnosticStartingEdgeEnd = null;

            if (diagnosticRecord != null && !diagnosticRecord.usedDirectedEdges.isEmpty()) {
                DirectedEdge startingEdge = diagnosticRecord.usedDirectedEdges.get(0);
                diagnosticStartingEdgeId = startingEdge.getEdgeId();
                diagnosticStartingEdgeStart = new Point(startingEdge.fromX, startingEdge.fromY);
                diagnosticStartingEdgeEnd = new Point(startingEdge.toX, startingEdge.toY);
            }

            InvariantFailureTracker tracker = diagnosticRecord == null
                    ? new InvariantFailureTracker()
                    : diagnosticRecord.invariantFailureTracker;

            String firstFailureCode = tracker.getFirstFailureCode();
            int firstFailureStep = tracker.getFirstFailureStep();
            String firstFailureMessage = tracker.getFirstFailureMessage();

            if (firstFailureCode == null && failureDetail != null) {
                firstFailureCode = INVARIANT_DUPLICATE_TRACED_VERTEX;
                firstFailureStep = diagnosticRecord == null
                        ? -1
                        : diagnosticRecord.duplicateFailureStep;
                firstFailureMessage = buildDuplicateTracedVertexMessage(failureDetail);
            }

            boolean stepsTruncated = diagnosticRecord != null
                    && firstFailureStep >= 0
                    && firstFailureStep >= maximumRecordedSteps
                    && diagnosticRecord.recordedSteps.size() < maximumRecordedSteps;

            int failingLoopNumber = failingLoopRecord == null
                    ? -1
                    : failingLoopRecord.loopNumber;
            int selectedOuterLoopNumber = selectedLoopRecord == null
                    ? -1
                    : selectedLoopRecord.loopNumber;
            int diagnosticLoopNumber = diagnosticRecord == null
                    ? -1
                    : diagnosticRecord.loopNumber;

            VertexTopologyDiagnostic vertexTopologyDiagnostic = null;

            if (failureDetail != null && edgeCatalog != null && !lumenCoordinates.isEmpty()) {
                List<DirectedEdge> exposedEdgeList =
                        new ArrayList<>(edgeCatalog.exposedEdgeSet());
                Map<Long, List<DirectedEdge>> incidentEdgesByVertex =
                        groupIncidentEdges(exposedEdgeList);
                Map<Long, List<DirectedEdge>> outgoingEdgesByStartVertex =
                        groupOutgoingEdges(exposedEdgeList);
                Set<Long> visitedCoordinateKeys = new HashSet<>(
                        failureDetail.visitedCoordinateKeysAtFailure);
                Set<Integer> usedEdgeIdsAtFailure = new HashSet<>(
                        failureDetail.usedEdgeIdsAtFailure);

                vertexTopologyDiagnostic = buildVertexTopologyDiagnostic(
                        failureDetail.x,
                        failureDetail.y,
                        lumenCoordinates,
                        edgeCatalog,
                        incidentEdgesByVertex,
                        outgoingEdgesByStartVertex,
                        failureDetail,
                        visitedCoordinateKeys,
                        usedEdgeIdsAtFailure);
            }

            return new TraceDebugReport(
                    diagnostics,
                    lumenId,
                    exposedDirectedEdgeCount,
                    failingLoopNumber,
                    selectedOuterLoopNumber,
                    diagnosticLoopNumber,
                    diagnosticRecord == null ? 0 : diagnosticRecord.totalStepCount,
                    maximumRecordedSteps,
                    stepsTruncated,
                    diagnosticRecord == null
                            ? Collections.emptyList()
                            : diagnosticRecord.recordedSteps,
                    firstFailureCode,
                    firstFailureStep,
                    firstFailureMessage,
                    diagnosticStartingEdgeId,
                    diagnosticStartingEdgeStart,
                    diagnosticStartingEdgeEnd,
                    diagnosticRecord == null ? 0 : diagnosticRecord.loopUsedCountBeforeStart,
                    diagnosticRecord == null ? 0 : diagnosticRecord.loopUsedCountAtFailure,
                    diagnosticRecord == null
                            ? 0
                            : diagnosticRecord.globalUsedEdgeCountAtFailure,
                    repeatedVertexEdgeAnalysis,
                    vertexTopologyDiagnostic,
                    diagnosticRecord == null
                            ? Collections.emptyList()
                            : diagnosticRecord.partialVertexPath,
                    diagnosticRecord == null
                            ? Collections.emptyList()
                            : diagnosticRecord.partialEdgePath,
                    failureReason);
        }
    }

    private static RepeatedVertexEdgeAnalysis buildRepeatedVertexEdgeAnalysis(
            DuplicateVertexDetail detail,
            EdgeCatalog edgeCatalog) {

        boolean equalIncomingEdgeIds = detail.firstIncomingEdgeId >= 0
                && detail.repeatedIncomingEdgeId >= 0
                && detail.firstIncomingEdgeId == detail.repeatedIncomingEdgeId;
        boolean equalOutgoingEdgeIds = detail.firstOutgoingEdgeId >= 0
                && detail.repeatedOutgoingEdgeId >= 0
                && detail.firstOutgoingEdgeId == detail.repeatedOutgoingEdgeId;
        boolean anyEqualEdgeIds = equalIncomingEdgeIds || equalOutgoingEdgeIds;
        boolean equalIncomingCoordinatePairs = detail.firstIncomingEdgeId >= 0
                && detail.repeatedIncomingEdgeId >= 0
                && detail.firstIncomingEdgeId == detail.repeatedIncomingEdgeId;
        boolean equalOutgoingCoordinatePairs = detail.firstOutgoingEdgeId >= 0
                && detail.repeatedOutgoingEdgeId >= 0
                && detail.firstOutgoingEdgeId == detail.repeatedOutgoingEdgeId;
        boolean sameForegroundFace = detail.firstForegroundFacePixel != null
                && detail.repeatedForegroundFacePixel != null
                && detail.firstForegroundFacePixel.x == detail.repeatedForegroundFacePixel.x
                && detail.firstForegroundFacePixel.y == detail.repeatedForegroundFacePixel.y;
        boolean sameTopologicalState = sameTopologicalState(
                detail.x,
                detail.y,
                detail.firstIncomingEdgeId,
                detail.repeatedIncomingEdgeId,
                detail.firstForegroundFacePixel,
                detail.repeatedForegroundFacePixel);

        return new RepeatedVertexEdgeAnalysis(
                detail.lumenId,
                detail.loopNumber,
                detail.x,
                detail.y,
                detail.firstIndex,
                detail.repeatedIndex,
                detail.firstIncomingEdgeId,
                detail.firstOutgoingEdgeId,
                detail.repeatedIncomingEdgeId,
                detail.repeatedOutgoingEdgeId,
                equalIncomingEdgeIds,
                equalOutgoingEdgeIds,
                anyEqualEdgeIds,
                equalIncomingCoordinatePairs,
                equalOutgoingCoordinatePairs,
                detail.identicalIncomingEdgeObjects,
                detail.identicalOutgoingEdgeObjects,
                detail.inDegree,
                detail.outDegree,
                detail.unusedOutgoingEdgeIds,
                detail.firstForegroundFacePixel,
                detail.repeatedForegroundFacePixel,
                sameForegroundFace,
                sameTopologicalState,
                detail.classification,
                detail.edgeIdClassification,
                buildDuplicateTracedVertexMessage(detail));
    }

    private static final class LoopTraceRecord {

        private final int loopNumber;
        private final List<Point> vertices;
        private final List<DirectedEdge> usedDirectedEdges;
        private final DuplicateVertexDetail duplicateDetail;
        private final List<TraversalStepDiagnostic> recordedSteps;
        private final int totalStepCount;
        private final List<Point> partialVertexPath;
        private final List<Integer> partialEdgePath;
        private final InvariantFailureTracker invariantFailureTracker;
        private final int duplicateFailureStep;
        private final int loopUsedCountBeforeStart;
        private final int loopUsedCountAtFailure;
        private final int globalUsedEdgeCountAtFailure;

        private LoopTraceRecord(
                int loopNumber,
                List<Point> vertices,
                List<DirectedEdge> usedDirectedEdges,
                DuplicateVertexDetail duplicateDetail,
                List<TraversalStepDiagnostic> recordedSteps,
                int totalStepCount) {
            this(loopNumber, vertices, usedDirectedEdges, duplicateDetail, recordedSteps,
                    totalStepCount, Collections.emptyList(), Collections.emptyList(),
                    new InvariantFailureTracker(), -1, 0, 0, 0);
        }

        private LoopTraceRecord(
                int loopNumber,
                List<Point> vertices,
                List<DirectedEdge> usedDirectedEdges,
                DuplicateVertexDetail duplicateDetail,
                List<TraversalStepDiagnostic> recordedSteps,
                int totalStepCount,
                List<Point> partialVertexPath,
                List<Integer> partialEdgePath,
                InvariantFailureTracker invariantFailureTracker) {
            this(loopNumber, vertices, usedDirectedEdges, duplicateDetail, recordedSteps,
                    totalStepCount, partialVertexPath, partialEdgePath, invariantFailureTracker,
                    -1, 0, 0, 0);
        }

        private LoopTraceRecord(
                int loopNumber,
                List<Point> vertices,
                List<DirectedEdge> usedDirectedEdges,
                DuplicateVertexDetail duplicateDetail,
                List<TraversalStepDiagnostic> recordedSteps,
                int totalStepCount,
                List<Point> partialVertexPath,
                List<Integer> partialEdgePath,
                InvariantFailureTracker invariantFailureTracker,
                int duplicateFailureStep,
                int loopUsedCountBeforeStart,
                int loopUsedCountAtFailure,
                int globalUsedEdgeCountAtFailure) {

            this.loopNumber = loopNumber;
            this.vertices = vertices;
            this.usedDirectedEdges = usedDirectedEdges;
            this.duplicateDetail = duplicateDetail;
            this.recordedSteps = recordedSteps == null
                    ? Collections.emptyList()
                    : recordedSteps;
            this.totalStepCount = totalStepCount;
            this.partialVertexPath = partialVertexPath == null
                    ? Collections.emptyList()
                    : defensiveCopyList(partialVertexPath);
            this.partialEdgePath = partialEdgePath == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(partialEdgePath));
            this.invariantFailureTracker = invariantFailureTracker == null
                    ? new InvariantFailureTracker()
                    : invariantFailureTracker;
            this.duplicateFailureStep = duplicateFailureStep;
            this.loopUsedCountBeforeStart = loopUsedCountBeforeStart;
            this.loopUsedCountAtFailure = loopUsedCountAtFailure;
            this.globalUsedEdgeCountAtFailure = globalUsedEdgeCountAtFailure;
        }
    }

    private static final class VertexVisitRecord {

        private final Point vertex;
        private final int incomingDirection;
        private int outgoingDirection;
        private final DirectedEdge incomingEdge;
        private DirectedEdge outgoingEdge;
        private final int incomingEdgeId;
        private int outgoingEdgeId;

        private VertexVisitRecord(
                Point vertex,
                int incomingDirection,
                int outgoingDirection,
                DirectedEdge incomingEdge,
                DirectedEdge outgoingEdge,
                int incomingEdgeId,
                int outgoingEdgeId) {

            this.vertex = vertex;
            this.incomingDirection = incomingDirection;
            this.outgoingDirection = outgoingDirection;
            this.incomingEdge = incomingEdge;
            this.outgoingEdge = outgoingEdge;
            this.incomingEdgeId = incomingEdgeId;
            this.outgoingEdgeId = outgoingEdgeId;
        }
    }

    static final class DuplicateVertexDetail {

        private final int lumenId;
        private final int x;
        private final int y;
        private final int firstIndex;
        private final int repeatedIndex;
        private final Point firstPrevious;
        private final Point firstNext;
        private final Point repeatedPrevious;
        private final Point repeatedNext;
        private final int firstIncoming;
        private final int firstOutgoing;
        private final int repeatedIncoming;
        private final int repeatedOutgoing;
        private final int incidentDirectedEdgeCount;
        private final int unusedOutgoingEdgeCount;
        private final int inDegree;
        private final int outDegree;
        private final List<Integer> unusedOutgoingEdgeIds;
        private final boolean identicalIncomingEdgeObjects;
        private final boolean identicalOutgoingEdgeObjects;
        private final int loopNumber;
        private final int loopVertexCount;
        private final int totalExposedEdges;
        private final int usedExposedEdges;
        private final String classification;
        private final int firstIncomingEdgeId;
        private final int firstOutgoingEdgeId;
        private final int repeatedIncomingEdgeId;
        private final int repeatedOutgoingEdgeId;
        private final String edgeIdClassification;
        private final Point firstForegroundFacePixel;
        private final Point repeatedForegroundFacePixel;
        private final List<Long> visitedCoordinateKeysAtFailure;
        private final List<Integer> usedEdgeIdsAtFailure;

        private DuplicateVertexDetail(
                int lumenId,
                int x,
                int y,
                int firstIndex,
                int repeatedIndex,
                Point firstPrevious,
                Point firstNext,
                Point repeatedPrevious,
                Point repeatedNext,
                int firstIncoming,
                int firstOutgoing,
                int repeatedIncoming,
                int repeatedOutgoing,
                int incidentDirectedEdgeCount,
                int unusedOutgoingEdgeCount,
                int inDegree,
                int outDegree,
                List<Integer> unusedOutgoingEdgeIds,
                boolean identicalIncomingEdgeObjects,
                boolean identicalOutgoingEdgeObjects,
                int loopNumber,
                int loopVertexCount,
                int totalExposedEdges,
                int usedExposedEdges,
                String classification,
                int firstIncomingEdgeId,
                int firstOutgoingEdgeId,
                int repeatedIncomingEdgeId,
                int repeatedOutgoingEdgeId,
                String edgeIdClassification,
                Point firstForegroundFacePixel,
                Point repeatedForegroundFacePixel,
                List<Long> visitedCoordinateKeysAtFailure,
                List<Integer> usedEdgeIdsAtFailure) {

            this.lumenId = lumenId;
            this.x = x;
            this.y = y;
            this.firstIndex = firstIndex;
            this.repeatedIndex = repeatedIndex;
            this.firstPrevious = firstPrevious == null ? null : new Point(firstPrevious);
            this.firstNext = firstNext == null ? null : new Point(firstNext);
            this.repeatedPrevious =
                    repeatedPrevious == null ? null : new Point(repeatedPrevious);
            this.repeatedNext = repeatedNext == null ? null : new Point(repeatedNext);
            this.firstIncoming = firstIncoming;
            this.firstOutgoing = firstOutgoing;
            this.repeatedIncoming = repeatedIncoming;
            this.repeatedOutgoing = repeatedOutgoing;
            this.incidentDirectedEdgeCount = incidentDirectedEdgeCount;
            this.unusedOutgoingEdgeCount = unusedOutgoingEdgeCount;
            this.inDegree = inDegree;
            this.outDegree = outDegree;
            this.unusedOutgoingEdgeIds = Collections.unmodifiableList(
                    new ArrayList<>(unusedOutgoingEdgeIds));
            this.identicalIncomingEdgeObjects = identicalIncomingEdgeObjects;
            this.identicalOutgoingEdgeObjects = identicalOutgoingEdgeObjects;
            this.loopNumber = loopNumber;
            this.loopVertexCount = loopVertexCount;
            this.totalExposedEdges = totalExposedEdges;
            this.usedExposedEdges = usedExposedEdges;
            this.classification = classification;
            this.firstIncomingEdgeId = firstIncomingEdgeId;
            this.firstOutgoingEdgeId = firstOutgoingEdgeId;
            this.repeatedIncomingEdgeId = repeatedIncomingEdgeId;
            this.repeatedOutgoingEdgeId = repeatedOutgoingEdgeId;
            this.edgeIdClassification = edgeIdClassification;
            this.firstForegroundFacePixel = firstForegroundFacePixel == null
                    ? null
                    : new Point(firstForegroundFacePixel);
            this.repeatedForegroundFacePixel = repeatedForegroundFacePixel == null
                    ? null
                    : new Point(repeatedForegroundFacePixel);
            this.visitedCoordinateKeysAtFailure = visitedCoordinateKeysAtFailure == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(visitedCoordinateKeysAtFailure));
            this.usedEdgeIdsAtFailure = usedEdgeIdsAtFailure == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(usedEdgeIdsAtFailure));
        }
    }

    private static final class DirectedEdge {

        private static final Comparator<DirectedEdge> END_VERTEX_ORDER =
                Comparator
                        .comparingInt((DirectedEdge edge) -> edge.toY)
                        .thenComparingInt(edge -> edge.toX);

        private final int fromX;
        private final int fromY;
        private final int toX;
        private final int toY;
        private int edgeId = -1;

        private DirectedEdge(int fromX, int fromY, int toX, int toY) {
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }

        private long startKey() {
            return encodeCoordinate(fromX, fromY);
        }

        private long endKey() {
            return encodeCoordinate(toX, toY);
        }

        private int getEdgeId() {
            return edgeId;
        }

        @Override
        public boolean equals(Object other) {

            if (this == other) {
                return true;
            }

            if (!(other instanceof DirectedEdge)) {
                return false;
            }

            DirectedEdge that = (DirectedEdge) other;

            return fromX == that.fromX
                    && fromY == that.fromY
                    && toX == that.toX
                    && toY == that.toY;
        }

        @Override
        public int hashCode() {
            return Objects.hash(fromX, fromY, toX, toY);
        }
    }
}
