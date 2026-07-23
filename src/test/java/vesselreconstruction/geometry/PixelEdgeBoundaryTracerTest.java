package vesselreconstruction.geometry;

import org.junit.Test;
import vesselreconstruction.model.Lumen;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PixelEdgeBoundaryTracerTest {

    private static final double AREA_TOLERANCE = 1.0e-9;

    private final PixelEdgeBoundaryTracer tracer = new PixelEdgeBoundaryTracer();

    @Test
    public void emptyLumenReturnsEmptyList() {

        assertTrue(tracer.traceOuterBoundary(lumenFromPoints(1)).isEmpty());
    }

    @Test
    public void onePixelReturnsFourClockwiseCorners() {

        List<Point> loop = tracer.traceOuterBoundary(lumenFromPoints(1, p(0, 0)));

        assertEquals(
                Arrays.asList(p(0, 0), p(1, 0), p(1, 1), p(0, 1)),
                loop);
    }

    @Test
    public void twoByTwoBlockReturnsRectangularOuterLoop() {

        List<Point> loop = tracer.traceOuterBoundary(lumenFromPoints(1,
                p(0, 0), p(1, 0),
                p(0, 1), p(1, 1)));

        assertEquals(8, loop.size());
        assertEquals(p(0, 0), loop.get(0));
        assertEquals(p(2, 0), loop.get(2));
        assertEquals(p(2, 2), loop.get(4));
        assertEquals(p(0, 2), loop.get(6));
        assertFalse(repeatsStartAtEnd(loop));
        assertEquals(loop.size(), uniqueVertexCount(loop));
    }

    @Test
    public void adjacentPixelsDoNotEmitSharedInternalEdge() {

        List<Point> loop = tracer.traceOuterBoundary(lumenFromPoints(1, p(0, 0), p(1, 0)));

        assertEquals(
                Arrays.asList(
                        p(0, 0), p(1, 0), p(2, 0), p(2, 1), p(1, 1), p(0, 1)),
                loop);
    }

    @Test
    public void lShapeReturnsOneOrderedSimpleOuterLoop() {

        List<Point> loop = tracer.traceOuterBoundary(lumenFromPoints(1,
                p(0, 0), p(0, 1), p(0, 2),
                p(1, 2), p(2, 2)));

        assertEquals(12, loop.size());
        assertEquals(p(0, 0), loop.get(0));
        assertEquals(loop.size(), uniqueVertexCount(loop));
        assertFalse(repeatsStartAtEnd(loop));
    }

    @Test
    public void concaveShapeIsTracedCorrectly() {

        List<Point> loop = tracer.traceOuterBoundary(lumenFromPoints(1,
                p(0, 0), p(1, 0), p(2, 0),
                p(2, 1), p(2, 2),
                p(1, 2), p(0, 2), p(0, 1)));

        assertEquals(12, loop.size());
        assertEquals(p(0, 0), loop.get(0));
        assertEquals(loop.size(), uniqueVertexCount(loop));
    }

    @Test
    public void shapeWithHoleReturnsOnlyOuterLoop() {

        List<Point> loop = tracer.traceOuterBoundary(lumenFromPoints(1,
                p(0, 0), p(1, 0), p(2, 0),
                p(0, 1), p(2, 1),
                p(0, 2), p(1, 2), p(2, 2)));

        assertEquals(12, loop.size());
        assertEquals(9.0, Math.abs(PixelEdgeBoundaryTracer.signedPolygonArea(loop)), AREA_TOLERANCE);
    }

    @Test
    public void outerLoopIsChosenByGreatestAbsoluteShoelaceArea() {

        List<Point> outerOnly = tracer.traceOuterBoundary(lumenFromPoints(1,
                p(0, 0), p(1, 0), p(2, 0),
                p(0, 1), p(2, 1),
                p(0, 2), p(1, 2), p(2, 2)));
        List<Point> filled = tracer.traceOuterBoundary(lumenFromPoints(1,
                p(0, 0), p(1, 0), p(2, 0),
                p(0, 1), p(1, 1), p(2, 1),
                p(0, 2), p(1, 2), p(2, 2)));

        assertTrue(
                Math.abs(PixelEdgeBoundaryTracer.signedPolygonArea(outerOnly))
                        > Math.abs(PixelEdgeBoundaryTracer.signedPolygonArea(
                                Arrays.asList(p(1, 1), p(2, 1), p(2, 2), p(1, 2)))));
        assertEquals(9.0, Math.abs(PixelEdgeBoundaryTracer.signedPolygonArea(filled)), AREA_TOLERANCE);
    }

    @Test
    public void diagonalPixelContactIsHandledDeterministically() {

        Lumen lumen = lumenFromPoints(1, p(0, 0), p(1, 1));
        PixelEdgeBoundaryTracer.TraceDiagnostics firstRun =
                tracer.diagnoseOuterBoundary(lumen);
        PixelEdgeBoundaryTracer.TraceDiagnostics secondRun =
                tracer.diagnoseOuterBoundary(lumen);

        assertNotNull(firstRun.getFailureReason());
        assertTrue(firstRun.getFailureReason().startsWith("DUPLICATE_TRACED_VERTEX"));
        assertEquals(firstRun, secondRun);

        PixelEdgeBoundaryTracer.TraceDebugReport report =
                tracer.debugOuterBoundary(lumen, 25);
        PixelEdgeBoundaryTracer.TraversalStepDiagnostic firstVisit =
                findJunctionStep(report, 1, 1);
        assertEquals(
                PixelEdgeBoundaryTracer.SELECTION_REASON_DIAGONAL_JUNCTION_MAPPING,
                firstVisit.getSelectionReason());
        assertEquals("EAST", firstVisit.getSelectedSuccessorDirection());
    }

    @Test
    public void diagonalContactDoesNotIncorrectlyJoinSeparateLoops() {

        // Three pixels that touch only through diagonal junctions. Strict
        // duplicate validation still rejects an incomplete join attempt rather
        // than inventing a single synthetic contour.
        Lumen lumen = lumenFromPoints(1, p(0, 0), p(2, 0), p(1, 1));
        PixelEdgeBoundaryTracer.TraceDiagnostics diagnostics =
                tracer.diagnoseOuterBoundary(lumen);

        assertNotNull(diagnostics.getFailureReason());
        assertTrue(diagnostics.getFailureReason().startsWith("DUPLICATE_TRACED_VERTEX"));
        assertTrue(diagnostics.getSelectedOuterLoopVertexCount() < 12);
    }

    @Test
    public void diagnoseOuterBoundaryReportsLoopCount() {

        // Layout: two pixels touching only diagonally.
        //   #.
        //   .#
        Lumen lumen = lumenFromPoints(1, p(0, 0), p(1, 1));
        PixelEdgeBoundaryTracer.TraceDiagnostics diagnostics =
                tracer.diagnoseOuterBoundary(lumen);

        assertTrue(diagnostics.getTracedLoopCount() >= 1);
        assertNotNull(diagnostics.getFailureReason());
    }

    @Test
    public void simplyConnectedShapeConsumesEveryExposedEdgeInOuterLoop() {

        List<Point> blockLoop = tracer.traceOuterBoundary(lumenFromPoints(1,
                p(0, 0), p(1, 0),
                p(0, 1), p(1, 1)));
        List<Point> lLoop = tracer.traceOuterBoundary(lumenFromPoints(1,
                p(0, 0), p(0, 1), p(0, 2),
                p(1, 2), p(2, 2)));

        assertEquals(exposedEdgeCount(4), blockLoop.size());
        assertEquals(exposedEdgeCount(5), lLoop.size());
    }

    @Test
    public void returnedFirstPointIsNotRepeatedAtEnd() {

        List<Point> loop = tracer.traceOuterBoundary(lumenFromPoints(1,
                p(0, 0), p(1, 0), p(0, 1), p(1, 1)));

        assertFalse(repeatsStartAtEnd(loop));
    }

    @Test
    public void returnedLoopHasNoDuplicateVertices() {

        List<Point> loop = tracer.traceOuterBoundary(lumenFromPoints(1,
                p(0, 0), p(1, 0), p(2, 0),
                p(0, 1), p(1, 1), p(2, 1)));

        assertEquals(loop.size(), uniqueVertexCount(loop));
    }

    @Test
    public void returnedListIsUnmodifiable() {

        List<Point> loop = tracer.traceOuterBoundary(lumenFromPoints(1, p(0, 0)));

        expectUnsupportedOperation(() -> loop.add(p(0, 0)));
    }

    @Test
    public void returnedPointsAreDefensiveCopies() {

        Lumen lumen = lumenFromPoints(1, p(0, 0));
        List<Point> loop = tracer.traceOuterBoundary(lumen);
        Point lumenPixel = lumen.getPixels().get(0);

        lumenPixel.translate(5, 5);

        assertEquals(p(0, 0), loop.get(0));
        assertNotSame(lumen.getPixels().get(0), loop.get(0));
    }

    @Test
    public void lumenInputRemainsUnchanged() {

        Lumen lumen = lumenFromPoints(1, p(0, 0), p(1, 0));
        List<Point> expectedPixels = copyPoints(lumen.getPixels());

        tracer.traceOuterBoundary(lumen);

        assertEquals(expectedPixels, lumen.getPixels());
    }

    @Test(expected = NullPointerException.class)
    public void nullLumenRejected() {
        tracer.traceOuterBoundary(null);
    }

    @Test
    public void nullLumenPixelRejected() {

        Lumen lumen = new Lumen(1);
        lumen.addPixel(new Point(0, 0));
        lumen.addPixel(null);

        expectIllegalArgument(() -> tracer.traceOuterBoundary(lumen));
    }

    @Test
    public void duplicateLumenPixelCoordinateRejected() {

        Lumen lumen = new Lumen(1);
        lumen.addPixel(new Point(0, 0));
        lumen.addPixel(new Point(0, 0));
        lumen.finish();

        expectIllegalArgument(() -> tracer.traceOuterBoundary(lumen));
    }

    @Test
    public void deterministicOutputAcrossRepeatedRuns() {

        Lumen lumen = lumenFromPoints(1,
                p(0, 0), p(1, 0), p(2, 0),
                p(0, 1), p(2, 1),
                p(0, 2), p(1, 2), p(2, 2));

        assertEquals(
                tracer.traceOuterBoundary(lumen),
                tracer.traceOuterBoundary(lumen));
    }

    @Test
    public void signedAreaMagnitudeMatchesKnownTestShape() {

        List<Point> loop = tracer.traceOuterBoundary(lumenFromPoints(1,
                p(0, 0), p(1, 0), p(2, 0),
                p(0, 1), p(1, 1), p(2, 1),
                p(0, 2), p(1, 2), p(2, 2)));

        assertEquals(9.0, PixelEdgeBoundaryTracer.signedPolygonArea(loop), AREA_TOLERANCE);
    }

    @Test
    public void diagnoseOuterBoundaryReportsExposedEdgeCount() {

        Lumen lumen = lumenFromPoints(1, p(0, 0), p(1, 0), p(0, 1), p(1, 1));
        PixelEdgeBoundaryTracer.TraceDiagnostics diagnostics =
                tracer.diagnoseOuterBoundary(lumen);

        assertEquals(8, diagnostics.getExposedDirectedEdgeCount());
    }

    @Test
    public void diagnoseOuterBoundaryReportsSelectedLoopCounts() {

        Lumen lumen = lumenFromPoints(1,
                p(0, 0), p(1, 0), p(0, 1), p(1, 1));
        PixelEdgeBoundaryTracer.TraceDiagnostics diagnostics =
                tracer.diagnoseOuterBoundary(lumen);

        assertEquals(1, diagnostics.getSelectedOuterLoopNumber());
        assertEquals(8, diagnostics.getSelectedOuterLoopVertexCount());
        assertEquals(8, diagnostics.getSelectedOuterLoopDistinctVertexCount());
    }

    @Test
    public void diagnoseOuterBoundaryAvailableForDuplicateVertexFailure() {

        // Pure two-pixel NW-SE diagonal still revisits the shared lattice vertex.
        Lumen lumen = northWestSouthEastDiagonalLumen();
        PixelEdgeBoundaryTracer.TraceDiagnostics diagnostics =
                tracer.diagnoseOuterBoundary(lumen);

        assertNotNull(diagnostics.getFailureReason());
        assertTrue(diagnostics.getFailureReason().startsWith("DUPLICATE_TRACED_VERTEX"));
        assertEquals(1, diagnostics.getRepeatedVertexOccurrenceCount());
        assertEquals(p(1, 1), diagnostics.getFirstRepeatedVertex());
        assertTrue(diagnostics.getFirstRepeatedVertexFirstIndex() >= 0);
        assertTrue(diagnostics.getFirstRepeatedVertexRepeatedIndex()
                > diagnostics.getFirstRepeatedVertexFirstIndex());
    }

    @Test
    public void traceOuterBoundaryRemainsStrictForDuplicateVertexFailure() {

        expectIllegalStateStartsWith(
                "DUPLICATE_TRACED_VERTEX:",
                () -> tracer.traceOuterBoundary(northWestSouthEastDiagonalLumen()));
    }

    @Test
    public void detailedDuplicateExceptionIncludesIndicesCoordinatesAndCounts() {

        try {
            tracer.traceOuterBoundary(northWestSouthEastDiagonalLumen());
        } catch (IllegalStateException exception) {
            String message = exception.getMessage();
            assertTrue(message.startsWith("DUPLICATE_TRACED_VERTEX:"));
            assertTrue(message.contains("x=1"));
            assertTrue(message.contains("y=1"));
            assertTrue(message.contains("firstIndex="));
            assertTrue(message.contains("repeatedIndex="));
            assertTrue(message.contains("firstPrevious="));
            assertTrue(message.contains("firstNext="));
            assertTrue(message.contains("repeatedPrevious="));
            assertTrue(message.contains("repeatedNext="));
            assertTrue(message.contains("incidentDirectedEdgeCount="));
            assertTrue(message.contains("unusedOutgoingEdgeCount="));
            assertTrue(message.contains("totalExposedEdges="));
            assertTrue(message.contains("usedExposedEdges="));
            assertTrue(message.contains("duplicateClassification="));
            return;
        }

        throw new AssertionError("Expected duplicate traced vertex failure.");
    }

    @Test
    public void detailedDuplicateExceptionIncludesIncomingAndOutgoingDirections() {

        try {
            tracer.traceOuterBoundary(northWestSouthEastDiagonalLumen());
        } catch (IllegalStateException exception) {
            String message = exception.getMessage();
            assertTrue(message.contains("firstIncoming="));
            assertTrue(message.contains("firstOutgoing="));
            assertTrue(message.contains("repeatedIncoming="));
            assertTrue(message.contains("repeatedOutgoing="));
            return;
        }

        throw new AssertionError("Expected duplicate traced vertex failure.");
    }

    @Test
    public void diagonalPinchPointIsDiagnosedDeterministically() {

        Lumen lumen = diagonalPinchLumen();
        PixelEdgeBoundaryTracer.TraceDiagnostics firstRun =
                tracer.diagnoseOuterBoundary(lumen);
        PixelEdgeBoundaryTracer.TraceDiagnostics secondRun =
                tracer.diagnoseOuterBoundary(lumen);

        assertNull(firstRun.getFailureReason());
        assertEquals(firstRun.getFailureReason(), secondRun.getFailureReason());
        assertEquals(
                firstRun.getSelectedOuterLoopVertexCount(),
                secondRun.getSelectedOuterLoopVertexCount());
        assertEquals(
                firstRun.getSelectedOuterLoopVertices(),
                secondRun.getSelectedOuterLoopVertices());
    }

    @Test
    public void repeatedVertexReachedThroughDifferentEdgesUsesDegreeBasedClassification() {

        PixelEdgeBoundaryTracer.TraceDiagnostics diagnostics =
                tracer.diagnoseOuterBoundary(northWestSouthEastDiagonalLumen());
        String classification = diagnostics.getFirstRepeatedVertexClassification();

        assertTrue(classification.equals(EDGE_ID_CLASSIFICATION_SAME_EDGE_ID_REVISITED)
                || classification.equals(EDGE_ID_CLASSIFICATION_DIFFERENT_EDGE_IDS_SHARED_VERTEX)
                || classification.equals(EDGE_ID_CLASSIFICATION_HIGH_DEGREE_VERTEX)
                || classification.equals(EDGE_ID_CLASSIFICATION_UNKNOWN));

        PixelEdgeBoundaryTracer.TraceDebugReport debugReport =
                tracer.debugOuterBoundary(northWestSouthEastDiagonalLumen(), 25);
        PixelEdgeBoundaryTracer.RepeatedVertexEdgeAnalysis edgeAnalysis =
                debugReport.getRepeatedVertexEdgeAnalysis();

        assertNotNull(edgeAnalysis);
        assertNotNull(edgeAnalysis.getEdgeIdClassification());

        if (diagnostics.getFirstDuplicateIncidentDirectedEdgeCount() >= 4
                && edgeAnalysis.getFirstIncomingEdgeId() != edgeAnalysis.getRepeatedIncomingEdgeId()) {
            assertEquals(
                    EDGE_ID_CLASSIFICATION_HIGH_DEGREE_VERTEX,
                    edgeAnalysis.getEdgeIdClassification());
        }
    }

    private static final String EDGE_ID_CLASSIFICATION_SAME_EDGE_ID_REVISITED =
            "SAME_EDGE_ID_REVISITED";
    private static final String EDGE_ID_CLASSIFICATION_DIFFERENT_EDGE_IDS_SHARED_VERTEX =
            "DIFFERENT_EDGE_IDS_SHARED_VERTEX";
    private static final String EDGE_ID_CLASSIFICATION_HIGH_DEGREE_VERTEX =
            "HIGH_DEGREE_VERTEX";
    private static final String EDGE_ID_CLASSIFICATION_UNKNOWN = "UNKNOWN";

    @Test
    public void simplyConnectedShapeConsumesEveryExposedDirectedEdge() {

        Lumen block = lumenFromPoints(1,
                p(0, 0), p(1, 0),
                p(0, 1), p(1, 1));
        Lumen lShape = lumenFromPoints(1,
                p(0, 0), p(0, 1), p(0, 2),
                p(1, 2), p(2, 2));

        assertEquals(
                tracer.diagnoseOuterBoundary(block).getExposedDirectedEdgeCount(),
                tracer.traceOuterBoundary(block).size());
        assertEquals(
                tracer.diagnoseOuterBoundary(lShape).getExposedDirectedEdgeCount(),
                tracer.traceOuterBoundary(lShape).size());
    }

    @Test
    public void diagnoseOuterBoundaryDoesNotMutateLumenInput() {

        Lumen lumen = diagonalPinchLumen();
        List<Point> expectedPixels = copyPoints(lumen.getPixels());

        tracer.diagnoseOuterBoundary(lumen);

        assertEquals(expectedPixels, lumen.getPixels());
    }

    @Test
    public void diagnoseOuterBoundaryReturnsDefensiveCopies() {

        Lumen validLumen = lumenFromPoints(1, p(0, 0));
        PixelEdgeBoundaryTracer.TraceDiagnostics validDiagnostics =
                tracer.diagnoseOuterBoundary(validLumen);
        List<Point> validLoopVertices =
                validDiagnostics.getSelectedOuterLoopVertices();

        validLoopVertices.get(0).translate(4, 4);
        assertEquals(p(0, 0), validDiagnostics.getSelectedOuterLoopVertices().get(0));

        Lumen pinchLumen = northWestSouthEastDiagonalLumen();
        PixelEdgeBoundaryTracer.TraceDiagnostics pinchDiagnostics =
                tracer.diagnoseOuterBoundary(pinchLumen);
        Point repeatedVertex = pinchDiagnostics.getFirstRepeatedVertex();

        assertNotNull(repeatedVertex);
        repeatedVertex.translate(3, 3);
        assertEquals(p(1, 1), pinchDiagnostics.getFirstRepeatedVertex());
    }

    @Test
    public void repeatedDiagnoseExecutionsProduceIdenticalDiagnostics() {

        Lumen lumen = northWestSouthEastDiagonalLumen();

        assertEquals(
                tracer.diagnoseOuterBoundary(lumen),
                tracer.diagnoseOuterBoundary(lumen));
    }

    @Test
    public void onePixelDebugReportRecordsFourCanonicalEdges() {

        PixelEdgeBoundaryTracer.TraceDebugReport report =
                tracer.debugOuterBoundary(lumenFromPoints(1, p(0, 0)), 25);

        assertEquals(4, report.getExposedDirectedEdgeCount());
        assertEquals(0, report.getFailureReason() == null ? 0 : 1);
    }

    @Test
    public void debugReportRecordsUnitEdgesForRectangle() {

        PixelEdgeBoundaryTracer.TraceDebugReport report =
                tracer.debugOuterBoundary(lumenFromPoints(1,
                        p(0, 0), p(1, 0), p(0, 1), p(1, 1)), 25);

        for (PixelEdgeBoundaryTracer.TraversalStepDiagnostic step : report.getTraversalSteps()) {
            assertTrue(step.getCurrentEdgeFromX() == step.getCurrentEdgeToX()
                    || step.getCurrentEdgeFromY() == step.getCurrentEdgeToY());
        }
    }

    @Test
    public void debugReportAvailableAfterTraversalFailure() {

        PixelEdgeBoundaryTracer.TraceDebugReport report =
                tracer.debugOuterBoundary(northWestSouthEastDiagonalLumen(), 25);

        assertNotNull(report.getFailureReason());
        assertNotNull(report.getRepeatedVertexEdgeAnalysis());
        assertFalse(report.getPartialDirectedEdgePath().isEmpty());
    }

    @Test
    public void debugAndStrictTraceShareSuccessfulOutput() {

        Lumen lumen = lumenFromPoints(1, p(0, 0), p(1, 0), p(0, 1), p(1, 1));

        assertEquals(
                tracer.traceOuterBoundary(lumen),
                tracer.debugOuterBoundary(lumen, 25).getTraceDiagnostics()
                        .getSelectedOuterLoopVertices());
    }

    @Test
    public void debugReportDeterministicAcrossRepeatedRuns() {

        Lumen lumen = diagonalPinchLumen();
        PixelEdgeBoundaryTracer.TraceDebugReport first =
                tracer.debugOuterBoundary(lumen, 10);
        PixelEdgeBoundaryTracer.TraceDebugReport second =
                tracer.debugOuterBoundary(lumen, 10);

        assertEquals(first.getStartingEdgeId(), second.getStartingEdgeId());
        assertEquals(first.getTraversalSteps().size(), second.getTraversalSteps().size());
        assertEquals(
                first.getFirstInvariantFailureCode(),
                second.getFirstInvariantFailureCode());
    }

    @Test
    public void coordinateEqualPointsResolveToSameCanonicalEdge() {

        Lumen lumen = lumenFromPoints(1, p(0, 0));
        PixelEdgeBoundaryTracer.TraceDebugReport report =
                tracer.debugOuterBoundary(lumen, 4);

        assertFalse(report.getPartialDirectedEdgePath().isEmpty());
        assertTrue(report.getPartialDirectedEdgePath().get(0) >= 0);
    }

    @Test
    public void debugOuterBoundaryDoesNotMutateLumenInput() {

        Lumen lumen = diagonalPinchLumen();
        List<Point> expected = copyPoints(lumen.getPixels());

        tracer.debugOuterBoundary(lumen, 25);

        assertEquals(expected, lumen.getPixels());
    }

    @Test
    public void candidateDiagnosticsPreserveSuccessorEvaluationOrder() {

        PixelEdgeBoundaryTracer.TraceDebugReport report =
                tracer.debugOuterBoundary(lumenFromPoints(1, p(0, 0), p(1, 0)), 5);

        if (!report.getTraversalSteps().isEmpty()) {
            List<PixelEdgeBoundaryTracer.SuccessorCandidateDiagnostic> candidates =
                    report.getTraversalSteps().get(0).getSuccessorCandidates();

            assertEquals(4, candidates.size());
            assertEquals(0, candidates.get(0).getEvaluationOrder());
            assertEquals(1, candidates.get(0).getRelativeTurn());
            assertEquals(0, candidates.get(1).getRelativeTurn());
            assertEquals("RIGHT", candidates.get(0).getTurnName());
            assertEquals("STRAIGHT", candidates.get(1).getTurnName());
        }
    }

    @Test
    public void diagnosticsCaptureFailureInEarlierLoopNotSelectedOuterLoop() {

        Lumen lumen = northWestSouthEastDiagonalLumen();
        PixelEdgeBoundaryTracer.TraceDebugReport report =
                tracer.debugOuterBoundary(lumen, 25);
        PixelEdgeBoundaryTracer.TraceDiagnostics diagnostics =
                tracer.diagnoseOuterBoundary(lumen);

        assertNotNull(report.getFailureReason());
        assertTrue(report.getFailingLoopNumber() >= 1);
        assertEquals(
                report.getFailingLoopNumber(),
                report.getDiagnosticLoopNumber());

        if (diagnostics.getTracedLoopCount() > 1
                && diagnostics.getSelectedOuterLoopNumber()
                        != report.getFailingLoopNumber()) {
            assertTrue(
                    report.getSelectedOuterLoopNumber()
                            != report.getDiagnosticLoopNumber());
        }

        for (PixelEdgeBoundaryTracer.TraversalStepDiagnostic step
                : report.getTraversalSteps()) {
            assertEquals(report.getDiagnosticLoopNumber(), step.getLoopNumber());
        }
    }

    @Test
    public void stepZeroOfNewLoopHasZeroLoopUsedCountBefore() {

        PixelEdgeBoundaryTracer.TraceDebugReport report =
                tracer.debugOuterBoundary(northWestSouthEastDiagonalLumen(), 25);

        assertFalse(report.getTraversalSteps().isEmpty());
        PixelEdgeBoundaryTracer.TraversalStepDiagnostic stepZero =
                report.getTraversalSteps().get(0);
        assertEquals(0, stepZero.getLoopUsedCountBefore());
        assertEquals(report.getDiagnosticLoopUsedEdgeCountBeforeStart(), 0);
    }

    @Test
    public void firstSuccessfulTraversalIncrementsLoopUsedCountToOne() {

        PixelEdgeBoundaryTracer.TraceDebugReport report =
                tracer.debugOuterBoundary(lumenFromPoints(1, p(0, 0), p(1, 0)), 25);

        assertFalse(report.getTraversalSteps().isEmpty());
        PixelEdgeBoundaryTracer.TraversalStepDiagnostic firstStep =
                report.getTraversalSteps().get(0);
        assertEquals(0, firstStep.getLoopUsedCountBefore());
        assertEquals(1, firstStep.getLoopUsedCountAfter());
    }

    @Test
    public void duplicateFailureReportsDuplicateTracedVertexCode() {

        PixelEdgeBoundaryTracer.TraceDebugReport report =
                tracer.debugOuterBoundary(northWestSouthEastDiagonalLumen(), 25);

        assertEquals("DUPLICATE_TRACED_VERTEX", report.getFirstFailureCode());
        assertTrue(report.getFirstFailureStep() >= 0);
        assertNotNull(report.getFirstFailureMessage());
    }

    @Test
    public void strictExceptionAndDebugReportAgreeOnDuplicateFacts() {

        Lumen lumen = northWestSouthEastDiagonalLumen();
        PixelEdgeBoundaryTracer.TraceDiagnostics diagnostics =
                tracer.diagnoseOuterBoundary(lumen);
        PixelEdgeBoundaryTracer.TraceDebugReport report =
                tracer.debugOuterBoundary(lumen, 25);
        PixelEdgeBoundaryTracer.RepeatedVertexEdgeAnalysis analysis =
                report.getRepeatedVertexEdgeAnalysis();

        assertNotNull(analysis);
        assertEquals(
                diagnostics.getFirstRepeatedVertexFirstIndex(),
                analysis.getFirstOccurrenceIndex());
        assertEquals(
                diagnostics.getFirstRepeatedVertexRepeatedIndex(),
                analysis.getRepeatedOccurrenceIndex());
        assertEquals(
                diagnostics.getFirstRepeatedVertex().x,
                analysis.getX());
        assertEquals(
                diagnostics.getFirstRepeatedVertex().y,
                analysis.getY());
        assertEquals(report.getFailingLoopNumber(), analysis.getLoopNumber());
    }

    @Test
    public void maximumRecordedStepsLimitsRetainedRowsButNotFailureReporting() {

        PixelEdgeBoundaryTracer.TraceDebugReport smallLimit =
                tracer.debugOuterBoundary(northWestSouthEastDiagonalLumen(), 3);
        PixelEdgeBoundaryTracer.TraceDebugReport largeLimit =
                tracer.debugOuterBoundary(northWestSouthEastDiagonalLumen(), 25);

        assertEquals("DUPLICATE_TRACED_VERTEX", smallLimit.getFirstFailureCode());
        assertEquals(
                largeLimit.getFirstFailureStep(),
                smallLimit.getFirstFailureStep());
        assertTrue(smallLimit.getTraversalSteps().size() <= 3);
        assertTrue(largeLimit.getTraversalSteps().size()
                > smallLimit.getTraversalSteps().size());
        assertNotNull(smallLimit.getRepeatedVertexEdgeAnalysis());
    }

    @Test
    public void partialEdgePathContainsOnlyDiagnosticLoopCanonicalEdges() {

        PixelEdgeBoundaryTracer.TraceDebugReport report =
                tracer.debugOuterBoundary(northWestSouthEastDiagonalLumen(), 25);

        assertFalse(report.getPartialDirectedEdgePath().isEmpty());
        for (Integer edgeId : report.getPartialDirectedEdgePath()) {
            assertTrue(edgeId >= 0);
        }
        for (PixelEdgeBoundaryTracer.TraversalStepDiagnostic step
                : report.getTraversalSteps()) {
            assertEquals(report.getDiagnosticLoopNumber(), step.getLoopNumber());
        }
    }

    @Test
    public void successfulShapeDiagnosticLoopMatchesSelectedOuterLoop() {

        Lumen lumen = lumenFromPoints(1, p(0, 0), p(1, 0), p(0, 1), p(1, 1));
        PixelEdgeBoundaryTracer.TraceDebugReport report =
                tracer.debugOuterBoundary(lumen, 25);

        assertNull(report.getFailureReason());
        assertEquals(-1, report.getFailingLoopNumber());
        assertEquals(
                report.getSelectedOuterLoopNumber(),
                report.getDiagnosticLoopNumber());
    }

    private static final String DUPLICATE_TRACED_VERTEX = "DUPLICATE_TRACED_VERTEX";
    private static final String TOPOLOGY_SINGLE_FOREGROUND_QUADRANT =
            "SINGLE_FOREGROUND_QUADRANT";
    private static final String TOPOLOGY_TWO_ADJACENT_FOREGROUND =
            "TWO_ADJACENT_FOREGROUND";
    private static final String TOPOLOGY_TWO_DIAGONAL_FOREGROUND_NW_SE =
            "TWO_DIAGONAL_FOREGROUND_NW_SE";
    private static final String TOPOLOGY_TWO_DIAGONAL_FOREGROUND_NE_SW =
            "TWO_DIAGONAL_FOREGROUND_NE_SW";
    private static final String TOPOLOGY_THREE_FOREGROUND_QUADRANTS =
            "THREE_FOREGROUND_QUADRANTS";
    private static final String TOPOLOGY_ALL_FOREGROUND = "ALL_FOREGROUND";
    private static final String TOPOLOGY_OUT_OF_BOUNDS = "OUT_OF_BOUNDS";

    @Test
    public void analyzeLocalVertexTopologyUsesFourQuadrantsAroundVertex() {

        PixelEdgeBoundaryTracer.VertexTopologyDiagnostic topology =
                tracer.analyzeLocalVertexTopology(lumenFromPoints(1, p(5, 5)), 5, 5);

        assertEquals(4, topology.getQuadrantPixelDiagnostics().size());
        assertTrue(topology.isSouthEastForeground());
        assertEquals(TOPOLOGY_SINGLE_FOREGROUND_QUADRANT, topology.getTopologyClassification());
    }

    @Test
    public void singleForegroundQuadrantClassification() {

        PixelEdgeBoundaryTracer.VertexTopologyDiagnostic topology =
                tracer.analyzeLocalVertexTopology(lumenFromPoints(1, p(2, 2)), 2, 2);

        assertEquals(TOPOLOGY_SINGLE_FOREGROUND_QUADRANT, topology.getTopologyClassification());
        assertEquals(1, topology.getForegroundCount());
    }

    @Test
    public void twoAdjacentForegroundClassification() {

        PixelEdgeBoundaryTracer.VertexTopologyDiagnostic topology =
                tracer.analyzeLocalVertexTopology(
                        lumenFromPoints(1, p(0, 0), p(1, 0)),
                        1,
                        1);

        assertEquals(TOPOLOGY_TWO_ADJACENT_FOREGROUND, topology.getTopologyClassification());
    }

    @Test
    public void northWestSouthEastDiagonalForegroundClassification() {

        PixelEdgeBoundaryTracer.VertexTopologyDiagnostic topology =
                tracer.analyzeLocalVertexTopology(
                        lumenFromPoints(1, p(0, 0), p(1, 1)),
                        1,
                        1);

        assertEquals(
                TOPOLOGY_TWO_DIAGONAL_FOREGROUND_NW_SE,
                topology.getTopologyClassification());
    }

    @Test
    public void northEastSouthWestDiagonalForegroundClassification() {

        PixelEdgeBoundaryTracer.VertexTopologyDiagnostic topology =
                tracer.analyzeLocalVertexTopology(
                        lumenFromPoints(1, p(1, 0), p(0, 1)),
                        1,
                        1);

        assertEquals(
                TOPOLOGY_TWO_DIAGONAL_FOREGROUND_NE_SW,
                topology.getTopologyClassification());
    }

    @Test
    public void threeForegroundQuadrantsClassification() {

        PixelEdgeBoundaryTracer.VertexTopologyDiagnostic topology =
                tracer.analyzeLocalVertexTopology(
                        lumenFromPoints(1, p(0, 0), p(1, 0), p(0, 1)),
                        1,
                        1);

        assertEquals(
                TOPOLOGY_THREE_FOREGROUND_QUADRANTS,
                topology.getTopologyClassification());
    }

    @Test
    public void allForegroundClassification() {

        PixelEdgeBoundaryTracer.VertexTopologyDiagnostic topology =
                tracer.analyzeLocalVertexTopology(
                        lumenFromPoints(1, p(0, 0), p(1, 0), p(0, 1), p(1, 1)),
                        1,
                        1);

        assertEquals(TOPOLOGY_ALL_FOREGROUND, topology.getTopologyClassification());
    }

    @Test
    public void outOfBoundsClassification() {

        PixelEdgeBoundaryTracer.VertexTopologyDiagnostic topology =
                tracer.analyzeLocalVertexTopology(lumenFromPoints(1, p(0, 0)), 0, 0);

        assertEquals(TOPOLOGY_OUT_OF_BOUNDS, topology.getTopologyClassification());
    }

    @Test
    public void leftAndRightAdjacentPixelsForCardinalDirections() {

        Lumen onePixel = lumenFromPoints(1, p(1, 1));
        Map<String, PixelEdgeBoundaryTracer.EdgeFaceDiagnostic> facesByDirection =
                new HashMap<>();

        for (Point corner : Arrays.asList(p(1, 1), p(2, 1), p(1, 2), p(2, 2))) {
            PixelEdgeBoundaryTracer.VertexTopologyDiagnostic topology =
                    tracer.analyzeLocalVertexTopology(onePixel, corner.x, corner.y);

            for (PixelEdgeBoundaryTracer.EdgeFaceDiagnostic face
                    : topology.getEdgeFaceDiagnostics()) {
                facesByDirection.put(face.getDirection(), face);
            }
        }

        PixelEdgeBoundaryTracer.EdgeFaceDiagnostic east = facesByDirection.get("EAST");
        PixelEdgeBoundaryTracer.EdgeFaceDiagnostic south = facesByDirection.get("SOUTH");
        PixelEdgeBoundaryTracer.EdgeFaceDiagnostic west = facesByDirection.get("WEST");
        PixelEdgeBoundaryTracer.EdgeFaceDiagnostic north = facesByDirection.get("NORTH");

        assertNotNull(east);
        assertNotNull(south);
        assertNotNull(west);
        assertNotNull(north);

        assertEquals(new Point(1, 0), east.getLeftAdjacentPixel());
        assertEquals(new Point(1, 1), east.getRightAdjacentPixel());
        assertEquals(new Point(2, 1), south.getLeftAdjacentPixel());
        assertEquals(new Point(1, 1), south.getRightAdjacentPixel());
        assertEquals(new Point(1, 2), west.getLeftAdjacentPixel());
        assertEquals(new Point(1, 1), west.getRightAdjacentPixel());
        assertEquals(new Point(0, 1), north.getLeftAdjacentPixel());
        assertEquals(new Point(1, 1), north.getRightAdjacentPixel());
    }

    @Test
    public void exposedEdgeOrientationInvariantMatchesLumenOnRightConvention() {

        Lumen onePixel = lumenFromPoints(1, p(1, 1));
        Set<Integer> seenEdgeIds = new HashSet<>();

        for (Point corner : Arrays.asList(p(1, 1), p(2, 1), p(1, 2), p(2, 2))) {
            PixelEdgeBoundaryTracer.VertexTopologyDiagnostic topology =
                    tracer.analyzeLocalVertexTopology(onePixel, corner.x, corner.y);

            for (PixelEdgeBoundaryTracer.EdgeFaceDiagnostic face
                    : topology.getEdgeFaceDiagnostics()) {

                if (!seenEdgeIds.add(face.getEdgeId())) {
                    continue;
                }

                assertTrue(face.isRightPixelBelongsToLumen());
                assertFalse(face.isLeftPixelBelongsToLumen());
                assertTrue(face.isOrientationInvariantSatisfied());
                assertEquals(
                        PixelEdgeBoundaryTracer.EXPOSED_EDGE_LUMEN_SIDE_CONVENTION,
                        face.getExpectedLumenSide());
            }
        }

        assertEquals(4, seenEdgeIds.size());
    }

    @Test
    public void diagonalPinchCreatesFourIncidentEdgesAtSharedVertex() {

        PixelEdgeBoundaryTracer.VertexTopologyDiagnostic topology =
                tracer.analyzeLocalVertexTopology(diagonalPinchLumen(), 2, 2);

        assertEquals(2, topology.getIncidentIncomingEdgeIds().size());
        assertEquals(2, topology.getIncidentOutgoingEdgeIds().size());
        assertEquals(4, topology.getEdgeFaceDiagnostics().size());
    }

    @Test
    public void repeatedVisitUsesDifferentIncomingEdgeIdsAndTopologicalStates() {

        PixelEdgeBoundaryTracer.TraceDebugReport report =
                tracer.debugOuterBoundary(northWestSouthEastDiagonalLumen(), 25);
        PixelEdgeBoundaryTracer.RepeatedVertexEdgeAnalysis analysis =
                report.getRepeatedVertexEdgeAnalysis();

        assertNotNull(analysis);
        assertTrue(
                analysis.getFirstIncomingEdgeId() != analysis.getRepeatedIncomingEdgeId());
        assertFalse(analysis.isSameTopologicalState());
    }

    @Test
    public void branchPairingReportsForegroundFacePreservation() {

        PixelEdgeBoundaryTracer.VertexTopologyDiagnostic topology =
                tracer.analyzeLocalVertexTopology(diagonalPinchLumen(), 2, 2);

        assertFalse(topology.getBranchPairingDiagnostics().isEmpty());

        boolean hasFaceContinuation = false;
        boolean hasFaceChange = false;

        for (PixelEdgeBoundaryTracer.BranchPairingDiagnostic pairing
                : topology.getBranchPairingDiagnostics()) {
            if ("VALID_FACE_CONTINUATION".equals(pairing.getPairingClassification())) {
                hasFaceContinuation = true;
            }
            if ("CHANGES_FOREGROUND_FACE".equals(pairing.getPairingClassification())) {
                hasFaceChange = true;
            }
        }

        assertTrue(hasFaceContinuation || hasFaceChange);
    }

    @Test
    public void branchPairingDiagnosticsDoNotChangeSuccessorSelection() {

        Lumen lumen = northWestSouthEastDiagonalLumen();
        PixelEdgeBoundaryTracer.TraceDiagnostics baseline =
                tracer.diagnoseOuterBoundary(lumen);
        PixelEdgeBoundaryTracer.TraceDebugReport report =
                tracer.debugOuterBoundary(lumen, 25);
        PixelEdgeBoundaryTracer.RepeatedVertexEdgeAnalysis analysis =
                report.getRepeatedVertexEdgeAnalysis();

        assertNotNull(analysis);
        assertEquals(baseline.getFailureReason(), report.getFailureReason());
        assertEquals(p(1, 1), baseline.getFirstRepeatedVertex());
        assertEquals(
                PixelEdgeBoundaryTracer.SELECTION_REASON_DIAGONAL_JUNCTION_MAPPING,
                findJunctionStep(report, 1, 1).getSelectionReason());
    }

    @Test
    public void vertexTopologyDiagnosticsUseImmutableCollectionsAndDefensivePoints() {

        PixelEdgeBoundaryTracer.VertexTopologyDiagnostic topology =
                tracer.analyzeLocalVertexTopology(lumenFromPoints(1, p(1, 1)), 2, 2);

        assertNotSame(topology.getVertex(), new Point(2, 2));
        topology.getVertex().translate(9, 9);
        assertEquals(new Point(2, 2), topology.getVertex());

        try {
            topology.getIncidentIncomingEdgeIds().add(99);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // expected immutable list
        }

        try {
            topology.getBranchPairingDiagnostics().clear();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // expected immutable list
        }
    }

    @Test
    public void topologyDiagnosticsAreDeterministicAndImmutable() {

        Lumen lumen = diagonalPinchLumen();
        PixelEdgeBoundaryTracer.VertexTopologyDiagnostic first =
                tracer.analyzeLocalVertexTopology(lumen, 2, 2);
        PixelEdgeBoundaryTracer.VertexTopologyDiagnostic second =
                tracer.analyzeLocalVertexTopology(lumen, 2, 2);

        assertEquals(
                first.getTopologyClassification(),
                second.getTopologyClassification());
        assertNotSame(first.getVertex(), second.getVertex());
    }

    @Test
    public void analyzeLocalVertexTopologyDoesNotMutateLumenInput() {

        Lumen lumen = diagonalPinchLumen();
        List<Point> expected = copyPoints(lumen.getPixels());

        tracer.analyzeLocalVertexTopology(lumen, 2, 2);

        assertEquals(expected, lumen.getPixels());
    }

    @Test
    public void northWestSouthEastDiagonalMappingsCoverAllIncomingDirections() {

        assertEquals(
                "EAST",
                PixelEdgeBoundaryTracer.mapDiagonalOutgoingDirectionName(
                        PixelEdgeBoundaryTracer.TOPOLOGY_CLASSIFICATION_TWO_DIAGONAL_FOREGROUND_NW_SE,
                        "SOUTH"));
        assertEquals(
                "WEST",
                PixelEdgeBoundaryTracer.mapDiagonalOutgoingDirectionName(
                        PixelEdgeBoundaryTracer.TOPOLOGY_CLASSIFICATION_TWO_DIAGONAL_FOREGROUND_NW_SE,
                        "NORTH"));
        assertEquals(
                "SOUTH",
                PixelEdgeBoundaryTracer.mapDiagonalOutgoingDirectionName(
                        PixelEdgeBoundaryTracer.TOPOLOGY_CLASSIFICATION_TWO_DIAGONAL_FOREGROUND_NW_SE,
                        "EAST"));
        assertEquals(
                "NORTH",
                PixelEdgeBoundaryTracer.mapDiagonalOutgoingDirectionName(
                        PixelEdgeBoundaryTracer.TOPOLOGY_CLASSIFICATION_TWO_DIAGONAL_FOREGROUND_NW_SE,
                        "WEST"));
    }

    @Test
    public void northEastSouthWestDiagonalMappingsCoverAllIncomingDirections() {

        assertEquals(
                "WEST",
                PixelEdgeBoundaryTracer.mapDiagonalOutgoingDirectionName(
                        PixelEdgeBoundaryTracer.TOPOLOGY_CLASSIFICATION_TWO_DIAGONAL_FOREGROUND_NE_SW,
                        "SOUTH"));
        assertEquals(
                "EAST",
                PixelEdgeBoundaryTracer.mapDiagonalOutgoingDirectionName(
                        PixelEdgeBoundaryTracer.TOPOLOGY_CLASSIFICATION_TWO_DIAGONAL_FOREGROUND_NE_SW,
                        "NORTH"));
        assertEquals(
                "NORTH",
                PixelEdgeBoundaryTracer.mapDiagonalOutgoingDirectionName(
                        PixelEdgeBoundaryTracer.TOPOLOGY_CLASSIFICATION_TWO_DIAGONAL_FOREGROUND_NE_SW,
                        "EAST"));
        assertEquals(
                "SOUTH",
                PixelEdgeBoundaryTracer.mapDiagonalOutgoingDirectionName(
                        PixelEdgeBoundaryTracer.TOPOLOGY_CLASSIFICATION_TWO_DIAGONAL_FOREGROUND_NE_SW,
                        "WEST"));
    }

    @Test
    public void northWestSouthEastDiagonalIncomingSouthSelectsEast() {

        assertDiagonalSuccessorDirection(
                northWestSouthEastDiagonalLumen(),
                1,
                1,
                "SOUTH",
                "EAST");
    }

    @Test
    public void northWestSouthEastDiagonalIncomingNorthSelectsWest() {

        assertDiagonalSuccessorDirection(
                northWestSouthEastDiagonalLumen(),
                1,
                1,
                "NORTH",
                "WEST");
    }

    @Test
    public void northEastSouthWestDiagonalIncomingSouthSelectsWest() {

        assertEquals(
                "WEST",
                PixelEdgeBoundaryTracer.mapDiagonalOutgoingDirectionName(
                        PixelEdgeBoundaryTracer.TOPOLOGY_CLASSIFICATION_TWO_DIAGONAL_FOREGROUND_NE_SW,
                        "SOUTH"));
    }

    @Test
    public void northEastSouthWestDiagonalIncomingNorthSelectsEast() {

        assertEquals(
                "EAST",
                PixelEdgeBoundaryTracer.mapDiagonalOutgoingDirectionName(
                        PixelEdgeBoundaryTracer.TOPOLOGY_CLASSIFICATION_TWO_DIAGONAL_FOREGROUND_NE_SW,
                        "NORTH"));
    }

    @Test
    public void northEastSouthWestDiagonalIncomingEastSelectsNorth() {

        assertDiagonalSuccessorDirection(
                northEastSouthWestDiagonalLumen(),
                1,
                1,
                "EAST",
                "NORTH");
    }

    @Test
    public void northEastSouthWestDiagonalIncomingWestSelectsSouth() {

        assertDiagonalSuccessorDirection(
                northEastSouthWestDiagonalLumen(),
                1,
                1,
                "WEST",
                "SOUTH");
    }

    @Test
    public void diagonalMappedSuccessorIsCanonicalExposedUnusedAndStartsAtVertex() {

        Lumen lumen = northWestSouthEastDiagonalLumen();
        int edgeId = tracer.resolveDiagonalJunctionSuccessorEdgeId(
                lumen,
                1,
                1,
                "SOUTH",
                false);
        PixelEdgeBoundaryTracer.VertexTopologyDiagnostic topology =
                tracer.analyzeLocalVertexTopology(lumen, 1, 1);

        assertTrue(topology.getIncidentOutgoingEdgeIds().contains(edgeId));
        assertTrue(edgeId >= 0);

        for (PixelEdgeBoundaryTracer.EdgeFaceDiagnostic face
                : topology.getEdgeFaceDiagnostics()) {

            if (face.getEdgeId() != edgeId) {
                continue;
            }

            assertEquals("EAST", face.getDirection());
            assertEquals(1, face.getFromX());
            assertEquals(1, face.getFromY());
            assertTrue(face.isOrientationInvariantSatisfied());
        }
    }

    @Test
    public void invalidMappedDiagonalSuccessorFailsStrictly() {

        try {
            tracer.resolveDiagonalJunctionSuccessorEdgeId(
                    northWestSouthEastDiagonalLumen(),
                    1,
                    1,
                    "SOUTH",
                    true);
            fail("Expected DIAGONAL_JUNCTION_SUCCESSOR_INVALID");
        } catch (IllegalStateException exception) {
            assertTrue(exception.getMessage().startsWith(
                    PixelEdgeBoundaryTracer.DIAGONAL_JUNCTION_SUCCESSOR_INVALID));
        }
    }

    @Test
    public void ordinaryVerticesRetainGenericSuccessorPriority() {

        PixelEdgeBoundaryTracer.TraceDebugReport report =
                tracer.debugOuterBoundary(
                        lumenFromPoints(1, p(0, 0), p(1, 0), p(0, 1), p(1, 1)),
                        25);

        assertNull(report.getFailureReason());
        boolean sawGeneric = false;

        for (PixelEdgeBoundaryTracer.TraversalStepDiagnostic step
                : report.getTraversalSteps()) {
            assertFalse(step.isDiagonalJunctionRuleApplied());
            if (PixelEdgeBoundaryTracer.SELECTION_REASON_GENERIC_TURN_PRIORITY
                    .equals(step.getSelectionReason())) {
                sawGeneric = true;
            }
        }

        assertTrue(sawGeneric);
    }

    @Test
    public void exactStartingEdgeClosureRemainsUnchangedAndStartEdgeNotUsedTwice() {

        Lumen lumen = lumenFromPoints(1, p(0, 0), p(1, 0), p(0, 1), p(1, 1));
        PixelEdgeBoundaryTracer.TraceDebugReport report =
                tracer.debugOuterBoundary(lumen, 25);
        List<Integer> edgePath = report.getPartialDirectedEdgePath();

        assertNull(report.getFailureReason());
        assertFalse(edgePath.isEmpty());
        assertEquals(1, Collections.frequency(edgePath, report.getStartingEdgeId()));
    }

    @Test
    public void duplicateCoordinateValidationRemainsStrictForPureDiagonal() {

        expectIllegalStateStartsWith(
                "DUPLICATE_TRACED_VERTEX:",
                () -> tracer.traceOuterBoundary(northWestSouthEastDiagonalLumen()));

        PixelEdgeBoundaryTracer.TraceDebugReport report =
                tracer.debugOuterBoundary(northWestSouthEastDiagonalLumen(), 25);
        PixelEdgeBoundaryTracer.TraversalStepDiagnostic firstVisit =
                findJunctionStep(report, 1, 1);

        assertEquals(
                PixelEdgeBoundaryTracer.SELECTION_REASON_DIAGONAL_JUNCTION_MAPPING,
                firstVisit.getSelectionReason());
        assertEquals("EAST", firstVisit.getDiagonalMappedDirection());
    }

    @Test
    public void diagonalPinchNoLongerProducesTinyLocalLoop() {

        List<Point> loop = tracer.traceOuterBoundary(diagonalPinchLumen());

        assertEquals(12, uniqueVertexCount(loop));
        assertEquals(12, loop.size());
        assertFalse(loop.contains(p(1, 1)) && loop.contains(p(2, 1))
                && Collections.frequency(loop, p(2, 2)) > 1);
    }

    @Test
    public void syntheticEightConnectedDiagonalShapeIsOneJoinedContour() {

        List<Point> first = tracer.traceOuterBoundary(diagonalPinchLumen());
        List<Point> second = tracer.traceOuterBoundary(diagonalPinchLumen());

        assertEquals(first, second);
        assertEquals(uniqueVertexCount(first), first.size());
        assertTrue(first.contains(p(2, 2)));
        assertEquals(1, Collections.frequency(first, p(2, 2)));
    }

    @Test
    public void everyTraversedEdgeIsCanonicalUnitEdge() {

        PixelEdgeBoundaryTracer.TraceDebugReport report =
                tracer.debugOuterBoundary(diagonalPinchLumen(), 40);

        assertNull(report.getFailureReason());

        for (PixelEdgeBoundaryTracer.TraversalStepDiagnostic step
                : report.getTraversalSteps()) {
            int deltaX = Math.abs(step.getCurrentEdgeToX() - step.getCurrentEdgeFromX());
            int deltaY = Math.abs(step.getCurrentEdgeToY() - step.getCurrentEdgeFromY());
            assertTrue((deltaX == 1 && deltaY == 0) || (deltaX == 0 && deltaY == 1));
            assertTrue(step.getCurrentEdgeId() >= 0);
        }
    }

    @Test
    public void diagonalJunctionSelectionReasonIsRecorded() {

        PixelEdgeBoundaryTracer.TraceDebugReport report =
                tracer.debugOuterBoundary(diagonalPinchLumen(), 40);
        boolean found = false;

        for (PixelEdgeBoundaryTracer.TraversalStepDiagnostic step
                : report.getTraversalSteps()) {

            if (step.getCurrentVertexX() == 2 && step.getCurrentVertexY() == 2) {
                assertTrue(step.isDiagonalJunctionRuleApplied());
                assertEquals(
                        PixelEdgeBoundaryTracer.SELECTION_REASON_DIAGONAL_JUNCTION_MAPPING,
                        step.getSelectionReason());
                assertEquals("SOUTH", step.getDiagonalMappedDirection());
                assertEquals(step.getDiagonalMappedEdgeId(), step.getSelectedSuccessorEdgeId());
                found = true;
            }
        }

        assertTrue(found);
    }

    @Test
    public void diagonalJoinOutputIsDeterministicAndDoesNotMutateInput() {

        Lumen lumen = diagonalPinchLumen();
        List<Point> expectedPixels = copyPoints(lumen.getPixels());
        List<Point> first = tracer.traceOuterBoundary(lumen);
        List<Point> second = tracer.traceOuterBoundary(lumen);

        assertEquals(first, second);
        assertEquals(expectedPixels, lumen.getPixels());
    }

    private void assertDiagonalSuccessorDirection(
            Lumen lumen,
            int vertexX,
            int vertexY,
            String incomingDirection,
            String expectedOutgoingDirection) {

        int edgeId = tracer.resolveDiagonalJunctionSuccessorEdgeId(
                lumen,
                vertexX,
                vertexY,
                incomingDirection,
                false);
        PixelEdgeBoundaryTracer.VertexTopologyDiagnostic topology =
                tracer.analyzeLocalVertexTopology(lumen, vertexX, vertexY);

        for (PixelEdgeBoundaryTracer.EdgeFaceDiagnostic face
                : topology.getEdgeFaceDiagnostics()) {

            if (face.getEdgeId() == edgeId) {
                assertEquals(expectedOutgoingDirection, face.getDirection());
                assertEquals(vertexX, face.getFromX());
                assertEquals(vertexY, face.getFromY());
                return;
            }
        }

        fail("Mapped edge " + edgeId + " not found among incident faces.");
    }

    private static PixelEdgeBoundaryTracer.TraversalStepDiagnostic findJunctionStep(
            PixelEdgeBoundaryTracer.TraceDebugReport report,
            int vertexX,
            int vertexY) {

        for (PixelEdgeBoundaryTracer.TraversalStepDiagnostic step
                : report.getTraversalSteps()) {

            if (step.getCurrentVertexX() == vertexX
                    && step.getCurrentVertexY() == vertexY
                    && step.isSuccessorSelected()) {
                return step;
            }
        }

        throw new AssertionError(
                "No successor step found at vertex (" + vertexX + "," + vertexY + ").");
    }

    private static Lumen diagonalPinchLumen() {

        //   ###
        //   # #
        //   ##.
        return lumenFromPoints(213,
                p(0, 0), p(1, 0), p(2, 0),
                p(0, 1), p(2, 1),
                p(0, 2), p(1, 2));
    }

    private static Lumen northWestSouthEastDiagonalLumen() {

        return lumenFromPoints(1, p(0, 0), p(1, 1));
    }

    private static Lumen northEastSouthWestDiagonalLumen() {

        return lumenFromPoints(1, p(1, 0), p(0, 1));
    }

    private static void expectIllegalStateStartsWith(
            String expectedPrefix,
            Runnable action) {

        try {
            action.run();
        } catch (IllegalStateException exception) {
            assertTrue(exception.getMessage().startsWith(expectedPrefix));
            return;
        }

        throw new AssertionError(
                "Expected IllegalStateException starting with: "
                        + expectedPrefix);
    }

    private static int exposedEdgeCount(int pixelCount) {

        switch (pixelCount) {
            case 4:
                return 8;
            case 5:
                return 12;
            default:
                throw new IllegalArgumentException("Unsupported test pixel count.");
        }
    }

    private static Lumen lumenFromPoints(int id, Point... points) {

        Lumen lumen = new Lumen(id);

        for (Point point : points) {
            lumen.addPixel(new Point(point.x, point.y));
        }

        lumen.finish();

        return lumen;
    }

    private static Point p(int x, int y) {
        return new Point(x, y);
    }

    private static boolean repeatsStartAtEnd(List<Point> loop) {

        if (loop.size() < 2) {
            return false;
        }

        Point first = loop.get(0);
        Point last = loop.get(loop.size() - 1);

        return first.x == last.x && first.y == last.y;
    }

    private static int uniqueVertexCount(List<Point> loop) {

        Set<String> uniqueVertices = new HashSet<>();

        for (Point vertex : loop) {
            uniqueVertices.add(vertex.x + "," + vertex.y);
        }

        return uniqueVertices.size();
    }

    private static List<Point> copyPoints(List<Point> points) {

        List<Point> copies = new ArrayList<>(points.size());

        for (Point point : points) {
            copies.add(new Point(point));
        }

        return copies;
    }

    private static void expectIllegalArgument(Runnable action) {

        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }

        throw new AssertionError("Expected IllegalArgumentException.");
    }

    private static void expectUnsupportedOperation(Runnable action) {

        try {
            action.run();
        } catch (UnsupportedOperationException expected) {
            return;
        }

        throw new AssertionError("Expected UnsupportedOperationException.");
    }
}
