package vesselreconstruction;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.io.SaveDialog;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import vesselreconstruction.detection.ConnectedComponentLabeler;
import vesselreconstruction.geometry.ArtifactArcCandidate;
import vesselreconstruction.geometry.ArtifactArcCandidateFinder;
import vesselreconstruction.geometry.ArtifactBridgeTriplet;
import vesselreconstruction.geometry.BridgeCandidate;
import vesselreconstruction.geometry.BridgeCandidateFinder;
import vesselreconstruction.geometry.BridgeCandidateSelector;
import vesselreconstruction.geometry.BridgeInteriorSeedFinder;
import vesselreconstruction.geometry.DeviationPeakFinder;
import vesselreconstruction.reconstruction.BridgeEnclosedWoodFinder;
import vesselreconstruction.reconstruction.SinglePixelLumenReconstructor;
import vesselreconstruction.reconstruction.SinglePixelReconstructionResult;
import vesselreconstruction.geometry.ImaginaryRuler;
import vesselreconstruction.geometry.LumenFeatureCalculator;
import vesselreconstruction.geometry.LumenFeatures;
import vesselreconstruction.geometry.LumenFilter;
import vesselreconstruction.geometry.LumenFilterCriteria;
import vesselreconstruction.geometry.PixelEdgeBoundaryTracer;
import vesselreconstruction.geometry.LumenFilterResult;
import vesselreconstruction.model.Lumen;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class VesselReconstructionPlugin implements PlugIn {

	private static final Font LUMEN_ID_LABEL_FONT =
			new Font(Font.SANS_SERIF, Font.BOLD, 18);

	private static final int IMAGINARY_RULER_NEIGHBOUR_COUNT = 10;
	private static final double PEAK_MINIMUM_RELATIVE_SCORE = 0.60;
	private static final int PEAK_SUPPRESSION_RADIUS = 10;
	private static final int PEAK_RING_RADIUS = 5;
	private static final int DEVIATION_BACKGROUND_GRAY = 40;
	private static final int PEAK_RING_MAGENTA_RGB = (255 << 16) | 255;
	private static final int BRIDGE_MINIMUM_CIRCULAR_BOUNDARY_SEPARATION = 10;
	private static final double BRIDGE_MAXIMUM_LENGTH = 30.0;
	private static final int BRIDGE_LINE_CYAN_RGB = 0x00FFFF;
	private static final int BRIDGE_LINE_HALF_WIDTH = 1;
	private static final int CLASSIFIED_WOOD = 1;
	private static final int BRIDGE_OPENING_HALF_WIDTH = 1;
	private static final int GROWTH_SEARCH_MARGIN = 50;
	private static final int MAXIMUM_RECONSTRUCTION_STEPS = 100;
	private static final int NON_IMPROVING_STEP_PATIENCE = 10;
	private static final boolean DEBUG_SINGLE_LUMEN_MODE = true;
	private static final int DEBUG_LUMEN_ID = 213;
	// Diagnostic thresholds for artifact-bridge triplet exploration; not final values.
	private static final int ARTIFACT_MINIMUM_FLANK_ARC_SEPARATION = 10;
	private static final int ARTIFACT_MAXIMUM_ARC_POINT_COUNT = 200;
	private static final double ARTIFACT_MAXIMUM_CLOSING_LINE_LENGTH = 50.0;
	private static final double ARTIFACT_MINIMUM_PEAK_DISTANCE = 5.0;
	private static final double ARTIFACT_MINIMUM_ARC_TO_CHORD_RATIO = 1.20;
	// Diagnostic thresholds for artifact-arc endpoint-pair exploration; not final values.
	private static final int ARTIFACT_ARC_MINIMUM_ENDPOINT_SEPARATION = 12;
	private static final int ARTIFACT_ARC_MAXIMUM_POINT_COUNT = 220;
	private static final double ARTIFACT_ARC_MINIMUM_CLOSING_LENGTH = 3.0;
	private static final double ARTIFACT_ARC_MAXIMUM_CLOSING_LENGTH = 60.0;
	private static final double ARTIFACT_ARC_MINIMUM_PEAK_DISTANCE = 5.0;
	private static final double ARTIFACT_ARC_MINIMUM_PEAK_DOMINANCE = 1.10;
	private static final double ARTIFACT_ARC_MINIMUM_ARC_TO_CHORD_RATIO = 1.20;
	private static final int ARTIFACT_ARC_MINIMUM_PEAK_FLANK = 8;
	private static final double ARTIFACT_ARC_MAXIMUM_PROFILE_VIOLATION = 2.0;
	private static final int DEBUG_ARTIFACT_DUPLICATE_MARKER_RADIUS = 4;
	private static final int DEBUG_ARTIFACT_REPEATED_CLOSING_RGB = 0xFF8800;
	private static final int DEBUG_ARTIFACT_DUPLICATE_VERTEX_RGB = 0xFF0000;
	private static final int DEBUG_ARTIFACT_TRACE_FIRST_DUPLICATE_RGB = 0xFFFFFF;
	private static final int DEBUG_ARTIFACT_TRACE_REPEATED_DUPLICATE_RGB = 0xFF0000;
	private static final int DEBUG_ARTIFACT_TRACE_CANDIDATE_VALID_RGB = 0x00FF00;
	private static final int DEBUG_ARTIFACT_TRACE_CANDIDATE_USED_RGB = 0xFF0000;
	private static final int DEBUG_ARTIFACT_TRACE_CANDIDATE_OTHER_RGB = 0xFF8800;
	private static final int DEBUG_ARTIFACT_TRACE_CURRENT_EDGE_RGB = 0xFFFF00;
	private static final int DEBUG_ARTIFACT_TRACE_SELECTED_SUCCESSOR_RGB = 0xFF00FF;
	private static final int DEBUG_ARTIFACT_TRACE_FIRST_EDGE_RGB = 0xFFFF00;
	private static final int DEBUG_ARTIFACT_TRACE_REPEATED_EDGE_RGB = 0xFF00FF;
	private static final int DEBUG_TOPOLOGY_INSET_PIXEL_SCALE = 8;
	private static final int DEBUG_TOPOLOGY_INSET_RADIUS = 3;
	private static final int DEBUG_TOPOLOGY_INSET_LUMEN_RGB = 0xAAAAAA;
	private static final int DEBUG_TOPOLOGY_INSET_BACKGROUND_RGB = 0x404040;
	private static final int DEBUG_ARTIFACT_TRACE_MARKER_OFFSET = 2;
	private static final int DEBUG_ARTIFACT_DISTANCE_PROFILE_HEIGHT = 240;
	private static final int DEBUG_ARTIFACT_DISTANCE_PROFILE_WIDTH = 480;
	private static final int DEBUG_ARTIFACT_DISTANCE_PROFILE_MARGIN = 24;
	private static final int DEBUG_ARTIFACT_DISTANCE_PROFILE_LINE_RGB = 0xFFFFFF;
	private static final int DEBUG_ARTIFACT_DISTANCE_PROFILE_PEAK_RGB = 0xFF00FF;
	private static final int DEBUG_ARTIFACT_BACKGROUND_RGB = 0x282828;
	private static final int DEBUG_ARTIFACT_LUMEN_RGB = 0x646464;
	private static final int DEBUG_ARTIFACT_PIXEL_CENTER_BOUNDARY_RGB = 0x000088;
	private static final int DEBUG_ARTIFACT_BOUNDARY_CYAN_RGB = 0x00FFFF;
	private static final int DEBUG_ARTIFACT_PEAK_RING_RGB = 0xFF0000;
	private static final int DEBUG_ARTIFACT_CANDIDATE_LINE_RGB = 0xBBBBBB;
	private static final int DEBUG_ARTIFACT_SELECTED_LINE_RGB = 0xFFFF00;
	private static final int DEBUG_ARTIFACT_FIRST_BRIDGE_RGB = 0xFFFFFF;
	private static final int DEBUG_ARTIFACT_BRIDGE_PEAK_RGB = 0xFF00FF;
	private static final int DEBUG_ARTIFACT_SECOND_BRIDGE_RGB = 0xFF8800;
	private static final int DEBUG_ARTIFACT_ARTIFACT_ARC_RGB = 0x0088FF;
	private static final int DEBUG_ARTIFACT_PEAK_RING_RADIUS = 2;
	private static final int DEBUG_ARTIFACT_BRIDGE_PEAK_RING_RADIUS = 3;
	private static final int DEBUG_ARTIFACT_SQUARE_MARKER_RADIUS = 2;
	private static final int DEBUG_ARTIFACT_SELECTED_LINE_THICKNESS = 3;
	private static final Font DEBUG_ARTIFACT_LABEL_FONT =
			new Font(Font.SANS_SERIF, Font.BOLD, 12);
	private static final int DEBUG_RECONSTRUCTION_ENCLOSED_WOOD_RGB = 0x994400;
	private static final int DEBUG_RECONSTRUCTION_CROP_MARGIN = 30;
	private static final int DEBUG_RECONSTRUCTION_CROP_SCALE = 4;
	private static final int DEBUG_RECONSTRUCTION_LAST_PIXEL_MARKER_RADIUS = 2;
	private static final int[] DEBUG_BRIDGE_CANDIDATE_LINE_COLORS = {
			0xFF0000,
			0x00FF00,
			0x0000FF,
			0xFFFF00,
			0xFF00FF,
			0xFF8800,
			0xFFFFFF
	};
	private static final int DEBUG_BRIDGE_CANDIDATE_BOUNDARY_CYAN_RGB = 0x00FFFF;
	private static final Font DEBUG_BRIDGE_CANDIDATE_LABEL_FONT =
			new Font(Font.SANS_SERIF, Font.BOLD, 10);
	private static final int DEBUG_SELECTED_BRIDGE_ENDPOINT_RING_RADIUS = 4;
	private static final int GROWTH_PREVIEW_BACKGROUND_GRAY = 40;
	private static final int GROWTH_PREVIEW_ORIGINAL_GRAY_RGB = 0x646464;
	private static final int GROWTH_PREVIEW_BEST_CYAN_RGB = 0x00FFFF;
	private static final int GROWTH_PREVIEW_ADDED_WOOD_ORANGE_RGB = 0xFF8800;
	private static final int GROWTH_PREVIEW_REMOVED_BRIDGE_WHITE_RGB = 0xFFFFFF;
	private static final int GROWTH_PREVIEW_SEED_MAGENTA_RGB = 0xFF00FF;
	private static final int GROWTH_PREVIEW_ENDPOINT_WHITE_RGB = 0xFFFFFF;
	private static final int GROWTH_PREVIEW_SEED_MARKER_RADIUS = 2;
	private static final int GROWTH_PREVIEW_ENDPOINT_RING_RADIUS = 2;
	private static final String CALIBRATION_CSV_DEFAULT_FILENAME =
			"lumen_calibration.csv";
	private static final String CALIBRATION_CSV_HEADER =
			"lumenId,manualLabel,filterAccepted,area,perimeter,circularity,"
					+ "aspectRatio,extent,boundaryRoughness,touchesBorder,"
					+ "rejectionReasons";

	@Override
	public void run(String arg) {

		ImagePlus image = WindowManager.getCurrentImage();

		if (image == null) {
			IJ.showMessage(
					"Vessel Reconstruction",
					"Please open a classified image first."
			);
			return;
		}

		ConnectedComponentLabeler labeler = new ConnectedComponentLabeler();
		List<Lumen> lumina = labeler.detect(image);

		LumenFeatureCalculator featureCalculator = new LumenFeatureCalculator();
		LumenFilter lumenFilter = new LumenFilter();
		LumenFilterCriteria filterCriteria =
				LumenFilterCriteria.exploratoryDefaults();
		LumenFilterDiagnostics filterDiagnostics = evaluateLumenFilters(
				featureCalculator,
				lumenFilter,
				filterCriteria,
				lumina);
		List<LumenFeatures> lumenFeatures = filterDiagnostics.lumenFeatures;

		Overlay overlay = new Overlay();
		int contourRoiCount = 0;
		int lumenIdLabelCount = 0;

		for (Lumen lumen : lumina) {

			List<Point> boundaryPixels = lumen.getBoundaryPixels();

			if (boundaryPixels.size() < 2) {
				continue;
			}

			int[] x = new int[boundaryPixels.size()];
			int[] y = new int[boundaryPixels.size()];

			for (int index = 0; index < boundaryPixels.size(); index++) {
				Point point = boundaryPixels.get(index);
				x[index] = point.x;
				y[index] = point.y;
			}

			PolygonRoi contourRoi = new PolygonRoi(
					x, y, boundaryPixels.size(), Roi.POLYGON);
			LumenFilterResult filterResult =
					filterDiagnostics.filterResultsByLumenId.get(lumen.getId());
			Color diagnosticColor = diagnosticColorForFilterResult(filterResult);
			contourRoi.setStrokeColor(diagnosticColor);
			contourRoi.setStrokeWidth(
					contourStrokeWidthForFilterResult(filterResult));

			overlay.add(contourRoi);
			contourRoiCount++;

			TextRoi idLabel = createLumenIdLabel(lumen);
			overlay.add(idLabel);
			lumenIdLabelCount++;
		}

		image.setOverlay(overlay);
		image.updateAndDraw();

		ColorProcessor visualizationImageProcessor = new ColorProcessor(
				image.getWidth(),
				image.getHeight());
		visualizationImageProcessor.setColor(
				new Color(
						DEVIATION_BACKGROUND_GRAY,
						DEVIATION_BACKGROUND_GRAY,
						DEVIATION_BACKGROUND_GRAY));
		visualizationImageProcessor.fill();

		ColorProcessor growthPreviewProcessor = new ColorProcessor(
				image.getWidth(),
				image.getHeight());
		growthPreviewProcessor.setColor(
				new Color(
						GROWTH_PREVIEW_BACKGROUND_GRAY,
						GROWTH_PREVIEW_BACKGROUND_GRAY,
						GROWTH_PREVIEW_BACKGROUND_GRAY));
		growthPreviewProcessor.fill();

		ImageProcessor classifiedImageProcessor = image.getProcessor();
		ImaginaryRuler imaginaryRuler = new ImaginaryRuler();
		DeviationPeakFinder peakFinder = new DeviationPeakFinder();
		BridgeCandidateFinder bridgeCandidateFinder = new BridgeCandidateFinder();
		BridgeCandidateSelector bridgeCandidateSelector =
				new BridgeCandidateSelector();
		ArtifactArcCandidateFinder artifactArcCandidateFinder =
				new ArtifactArcCandidateFinder();
		PixelEdgeBoundaryTracer pixelEdgeBoundaryTracer =
				new PixelEdgeBoundaryTracer();
		BridgeInteriorSeedFinder bridgeInteriorSeedFinder =
				new BridgeInteriorSeedFinder();
		SinglePixelLumenReconstructor singlePixelLumenReconstructor =
				new SinglePixelLumenReconstructor();
		BridgeEnclosedWoodFinder bridgeEnclosedWoodFinder =
				new BridgeEnclosedWoodFinder();
		int visualizedLumenCount = 0;
		int skippedShortBoundaryCount = 0;
		int totalAcceptedPeakCount = 0;
		int luminaWithPeakCount = 0;
		int totalBridgeCandidateCount = 0;
		int luminaWithBridgeCandidateCount = 0;
		int selectedBridgeCount = 0;
		int growthPreviewLumenCount = 0;
		int growthPreviewSelectedBridgeCount = 0;
		int circularityImprovedLumenCount = 0;
		int circularityNotImprovedLumenCount = 0;
		int totalBridgeOpeningPixelsRemoved = 0;
		int totalWoodPixelsAdded = 0;
		double totalCircularityImprovement = 0.0;
		int circularityImprovementSampleCount = 0;
		List<Double> acceptedPeakScores = new ArrayList<>();
		List<Integer> acceptedPeaksPerLumen = new ArrayList<>();
		List<String> bridgeSelectionDiagnosticLines = new ArrayList<>();
		List<String> growthPreviewDiagnosticLines = new ArrayList<>();
		ImagePlus debugReconstructionCropImage = null;
		ImagePlus debugBridgeCandidatesImage = null;
		ImagePlus debugArtifactArcCandidatesImage = null;
		ImagePlus debugArtifactDistanceProfileImage = null;
		int minimumBoundaryLength = Math.max(
				2 * IMAGINARY_RULER_NEIGHBOUR_COUNT + 1,
				2 * PEAK_SUPPRESSION_RADIUS + 1);

		if (DEBUG_SINGLE_LUMEN_MODE) {
			IJ.log("Single-lumen reconstruction debug mode: enabled.");
			IJ.log("Debug lumen ID: " + DEBUG_LUMEN_ID + ".");
		}

		for (Lumen lumen : lumina) {

			List<Point> boundaryPixels = lumen.getBoundaryPixels();

			if (boundaryPixels.size() < minimumBoundaryLength) {
				skippedShortBoundaryCount++;
				continue;
			}

			double[] deviationScores = imaginaryRuler.calculateDeviationScores(
					boundaryPixels,
					IMAGINARY_RULER_NEIGHBOUR_COUNT);

			paintDeviationScores(
					visualizationImageProcessor,
					boundaryPixels,
					deviationScores);

			List<Integer> peakIndices = peakFinder.findPeakIndices(
					deviationScores,
					PEAK_MINIMUM_RELATIVE_SCORE,
					PEAK_SUPPRESSION_RADIUS);

			for (int peakIndex : peakIndices) {
				acceptedPeakScores.add(deviationScores[peakIndex]);
				Point peakPoint = boundaryPixels.get(peakIndex);
				paintPeakRing(
						visualizationImageProcessor,
						peakPoint.x,
						peakPoint.y);
			}

			if (!peakIndices.isEmpty()) {
				luminaWithPeakCount++;
			}

			totalAcceptedPeakCount += peakIndices.size();
			acceptedPeaksPerLumen.add(peakIndices.size());

			List<BridgeCandidate> bridgeCandidates =
					bridgeCandidateFinder.findCandidates(
							boundaryPixels,
							peakIndices,
							BRIDGE_MINIMUM_CIRCULAR_BOUNDARY_SEPARATION,
							BRIDGE_MAXIMUM_LENGTH);
			totalBridgeCandidateCount += bridgeCandidates.size();

			if (!bridgeCandidates.isEmpty()) {
				luminaWithBridgeCandidateCount++;
			}

			if (DEBUG_SINGLE_LUMEN_MODE && lumen.getId() == DEBUG_LUMEN_ID) {
				IJ.log("Legacy bridge candidate diagnostics for lumen "
						+ DEBUG_LUMEN_ID + ":");
				logDebugBridgeCandidates(bridgeCandidates);
			}

			Optional<BridgeCandidate> selectedBridge =
					bridgeCandidateSelector.selectBest(bridgeCandidates);

			if (DEBUG_SINGLE_LUMEN_MODE && lumen.getId() == DEBUG_LUMEN_ID) {
				debugBridgeCandidatesImage = createDebugBridgeCandidatesImage(
						lumen,
						boundaryPixels,
						bridgeCandidates,
						selectedBridge,
						image.getWidth(),
						image.getHeight());

				List<Point> edgeBoundaryPixels = Collections.emptyList();
				double[] edgeDeviationScores = new double[0];
				double edgeLoopSignedArea = 0.0;
				List<ArtifactArcCandidate> artifactArcCandidates =
						Collections.emptyList();
				Optional<ArtifactArcCandidate> selectedArtifactArcCandidate =
						Optional.empty();
				boolean artifactReconstructionAllowed = false;
				String artifactReconstructionReason =
						"NO_VALID_SINGLE_PEAKED_ARTIFACT_ARC";
				boolean artifactArcInputRejected = false;
				String artifactArcInputRejectionMessage = null;
				boolean artifactEdgeTraceFailed = false;
				PixelEdgeBoundaryTracer.TraceDiagnostics pixelEdgeTraceDiagnostics =
						null;
				PixelEdgeBoundaryTracer.TraceDebugReport pixelEdgeTraceDebugReport =
						null;
				ArtifactEdgeBoundaryInputDiagnostics edgeBoundaryInputDiagnostics =
						ArtifactEdgeBoundaryInputDiagnostics.empty();

				pixelEdgeTraceDebugReport =
						pixelEdgeBoundaryTracer.debugOuterBoundary(lumen, 25);
				pixelEdgeTraceDiagnostics =
						pixelEdgeTraceDebugReport.getTraceDiagnostics();
				logPixelEdgeTraceDiagnostics(pixelEdgeTraceDiagnostics);
				logPixelEdgeTraversalSummary(pixelEdgeTraceDebugReport);
				logPixelEdgeTraversalSteps(pixelEdgeTraceDebugReport);
				logPixelEdgeRepeatedVertexEdgeAnalysis(pixelEdgeTraceDebugReport);
				logPixelEdgeLocalVertexTopology(pixelEdgeTraceDebugReport);
				logPixelEdgeIncidentEdgeFaces(pixelEdgeTraceDebugReport);
				logPixelEdgeVertexBranchPairings(pixelEdgeTraceDebugReport);

				try {
					edgeBoundaryPixels = pixelEdgeBoundaryTracer.traceOuterBoundary(lumen);
					edgeLoopSignedArea =
							PixelEdgeBoundaryTracer.signedPolygonArea(edgeBoundaryPixels);

					if (edgeBoundaryPixels.size() >= minimumBoundaryLength) {
						edgeDeviationScores =
								imaginaryRuler.calculateDeviationScores(
										edgeBoundaryPixels,
										IMAGINARY_RULER_NEIGHBOUR_COUNT);
						edgeBoundaryInputDiagnostics =
								analyzeArtifactEdgeBoundaryInput(
										edgeBoundaryPixels,
										edgeDeviationScores);
						logArtifactArcInputDiagnostics(
								lumen.getId(),
								edgeBoundaryInputDiagnostics);

						try {
							artifactArcCandidates =
									artifactArcCandidateFinder.findCandidates(
											edgeBoundaryPixels,
											edgeDeviationScores,
											ARTIFACT_ARC_MINIMUM_ENDPOINT_SEPARATION,
											ARTIFACT_ARC_MAXIMUM_POINT_COUNT,
											ARTIFACT_ARC_MINIMUM_CLOSING_LENGTH,
											ARTIFACT_ARC_MAXIMUM_CLOSING_LENGTH,
											ARTIFACT_ARC_MINIMUM_PEAK_DISTANCE,
											ARTIFACT_ARC_MINIMUM_PEAK_DOMINANCE,
											ARTIFACT_ARC_MINIMUM_ARC_TO_CHORD_RATIO,
											ARTIFACT_ARC_MINIMUM_PEAK_FLANK,
											ARTIFACT_ARC_MAXIMUM_PROFILE_VIOLATION);
							selectedArtifactArcCandidate = artifactArcCandidates.isEmpty()
									? Optional.empty()
									: Optional.of(artifactArcCandidates.get(0));
							// Reconstruction remains disabled in single-lumen debug mode.
							artifactReconstructionAllowed = false;
							artifactReconstructionReason = selectedArtifactArcCandidate.isPresent()
									? "RECONSTRUCTION_DISABLED_DEBUG_MODE"
									: "NO_VALID_SINGLE_PEAKED_ARTIFACT_ARC";
						} catch (IllegalArgumentException validationException) {
							artifactArcInputRejected = true;
							artifactArcInputRejectionMessage =
									validationException.getMessage();
							artifactReconstructionReason = extractStableReasonCode(
									artifactArcInputRejectionMessage);
							logArtifactArcInputRejected(
									lumen.getId(),
									artifactArcInputRejectionMessage);
						}
					}

					logArtifactEdgeContourDiagnostics(
							lumen.getId(),
							lumen.getPixels().size(),
							boundaryPixels.size(),
							edgeBoundaryPixels,
							edgeLoopSignedArea,
							Collections.emptyList());
				} catch (IllegalStateException traceException) {
					artifactArcInputRejected = true;
					artifactEdgeTraceFailed = true;
					artifactArcInputRejectionMessage = traceException.getMessage();
					artifactReconstructionReason = "EDGE_TRACE_FAILED";
					logPixelEdgeTraceException(
							lumen.getId(),
							traceException.getMessage());
					logArtifactArcInputRejected(
							lumen.getId(),
							artifactArcInputRejectionMessage);
					if (edgeBoundaryPixels.isEmpty()
							&& pixelEdgeTraceDiagnostics != null) {
						edgeBoundaryPixels =
								pixelEdgeTraceDiagnostics.getSelectedOuterLoopVertices();
					}
				}

				logArtifactArcDiagnostics(
						lumen.getId(),
						artifactArcCandidates,
						artifactReconstructionAllowed,
						artifactReconstructionReason,
						selectedArtifactArcCandidate,
						edgeBoundaryPixels);
				debugArtifactArcCandidatesImage = createDebugArtifactArcCandidatesImage(
						lumen,
						edgeBoundaryPixels,
						artifactArcCandidates,
						selectedArtifactArcCandidate,
						artifactArcInputRejected,
						artifactEdgeTraceFailed,
						artifactReconstructionReason,
						edgeBoundaryInputDiagnostics,
						pixelEdgeTraceDiagnostics,
						pixelEdgeTraceDebugReport,
						image.getWidth(),
						image.getHeight());
				if (selectedArtifactArcCandidate.isPresent() && !artifactArcInputRejected) {
					debugArtifactDistanceProfileImage =
							createDebugArtifactDistanceProfileImage(
									selectedArtifactArcCandidate.get(),
									edgeBoundaryPixels);
				}
			}

			if (selectedBridge.isPresent()) {
				selectedBridgeCount++;
				paintBridgeLine(
						visualizationImageProcessor,
						selectedBridge.get());
			}

			bridgeSelectionDiagnosticLines.add(
					formatBridgeSelectionDiagnosticLine(
							lumen.getId(),
							bridgeCandidates,
							selectedBridge));

			if (!DEBUG_SINGLE_LUMEN_MODE) {
				List<Point> lumenForegroundPixels = copyPoints(lumen.getPixels());
				Optional<BridgeCandidate> reconstructionBridge = selectedBridge;

				ReconstructionPreviewOutcome reconstructionPreviewOutcome =
						paintReconstructionPreview(
								growthPreviewProcessor,
								bridgeInteriorSeedFinder,
								bridgeEnclosedWoodFinder,
								singlePixelLumenReconstructor,
								classifiedImageProcessor,
								lumen,
								lumenForegroundPixels,
								reconstructionBridge,
								circularityForLumen(
										filterDiagnostics,
										lumen.getId()));
				growthPreviewLumenCount++;
				growthPreviewDiagnosticLines.add(
						formatReconstructionPreviewDiagnosticLine(
								lumen.getId(),
								reconstructionPreviewOutcome));

				if (reconstructionPreviewOutcome.selected) {
					growthPreviewSelectedBridgeCount++;
					totalBridgeOpeningPixelsRemoved +=
							reconstructionPreviewOutcome.reconstructionResult
									.getDirectlyRemovedBridgePixels().size();
					totalWoodPixelsAdded +=
							reconstructionPreviewOutcome.reconstructionResult
									.getAddedWoodPixels().size();

					if (reconstructionPreviewOutcome.reconstructionResult
							.isCircularityImproved()) {
						circularityImprovedLumenCount++;
						totalCircularityImprovement +=
								reconstructionPreviewOutcome.reconstructionResult
										.getBestCircularity()
										- reconstructionPreviewOutcome
												.reconstructionResult
												.getOriginalCircularity();
						circularityImprovementSampleCount++;
					} else {
						circularityNotImprovedLumenCount++;
					}
				}
			}

			visualizedLumenCount++;
		}

		ImagePlus deviationImage = new ImagePlus(
				"Imaginary Ruler Deviation",
				visualizationImageProcessor);
		deviationImage.show();

		if (!DEBUG_SINGLE_LUMEN_MODE) {
			ImagePlus growthPreviewImage = new ImagePlus(
					"Single-Pixel Lumen Reconstruction Preview",
					growthPreviewProcessor);
			growthPreviewImage.show();
		}

		if (debugReconstructionCropImage != null) {
			debugReconstructionCropImage.show();
		}

		if (debugBridgeCandidatesImage != null) {
			debugBridgeCandidatesImage.show();
		}

		if (debugArtifactArcCandidatesImage != null) {
			debugArtifactArcCandidatesImage.show();
		}

		if (debugArtifactDistanceProfileImage != null) {
			debugArtifactDistanceProfileImage.show();
		}

		IJ.log("Added " + contourRoiCount + " contour ROIs.");
		IJ.log("Added " + lumenIdLabelCount + " lumen ID labels.");
		IJ.log("Visualized " + visualizedLumenCount + " lumen deviation maps.");
		IJ.log("Skipped " + skippedShortBoundaryCount
				+ " lumina with boundaries shorter than "
				+ minimumBoundaryLength + " points.");
		IJ.log("ImaginaryRuler neighbour count: "
				+ IMAGINARY_RULER_NEIGHBOUR_COUNT + ".");
		IJ.log("Peak minimum relative score: "
				+ PEAK_MINIMUM_RELATIVE_SCORE + ".");
		IJ.log("Peak suppression radius: "
				+ PEAK_SUPPRESSION_RADIUS + ".");
		IJ.log("Total accepted peak count: "
				+ totalAcceptedPeakCount + ".");
		IJ.log("Lumina containing peaks: "
				+ luminaWithPeakCount + ".");
		logAcceptedPeakScoreStatistics(acceptedPeakScores);
		logAcceptedPeaksPerLumenStatistics(acceptedPeaksPerLumen);
		logLumenFeatureStatistics(lumenFeatures);
		logExploratoryFilterDiagnostics(filterDiagnostics, filterCriteria);
		logLumenFilterPerLumenDiagnostics(filterDiagnostics);
		exportLumenCalibrationCsv(filterDiagnostics);
		logBridgeSelectionDiagnostics(
				bridgeSelectionDiagnosticLines,
				totalBridgeCandidateCount,
				luminaWithBridgeCandidateCount,
				selectedBridgeCount);
		logReconstructionPreviewDiagnostics(
				growthPreviewDiagnosticLines,
				growthPreviewLumenCount,
				growthPreviewSelectedBridgeCount,
				circularityImprovedLumenCount,
				circularityNotImprovedLumenCount,
				totalBridgeOpeningPixelsRemoved,
				totalWoodPixelsAdded,
				circularityImprovementSampleCount,
				totalCircularityImprovement);

		IJ.showMessage(
				"Vessel Reconstruction",
				"Detected " + lumina.size() + " lumen objects.\n"
						+ "Accepted candidates: "
						+ filterDiagnostics.acceptedCandidateCount + ".\n"
						+ "Rejected candidates: "
						+ filterDiagnostics.rejectedCandidateCount + ".\n"
						+ "Feature calculation failures: "
						+ filterDiagnostics.featureCalculationFailureCount + ".\n"
						+ "Selected bridge count: "
						+ selectedBridgeCount + ".\n"
						+ "Circularity-improved lumina: "
						+ circularityImprovedLumenCount + ".\n"
						+ (DEBUG_SINGLE_LUMEN_MODE
								? "Reconstruction debug lumen: "
										+ DEBUG_LUMEN_ID + ".\n"
								: "")
						+ "Visualized " + visualizedLumenCount + " lumen deviation maps.\n"
						+ "Total accepted peak count: "
						+ totalAcceptedPeakCount + ".\n"
						+ "ImaginaryRuler neighbour count: "
						+ IMAGINARY_RULER_NEIGHBOUR_COUNT + "."
		);
	}

	private static final class LumenFilterDiagnostics {

		private final List<LumenFeatures> lumenFeatures = new ArrayList<>();
		private final Map<Integer, LumenFilterResult> filterResultsByLumenId =
				new HashMap<>();
		private int acceptedCandidateCount;
		private int rejectedCandidateCount;
		private int areaRejectionCount;
		private int circularityRejectionCount;
		private int aspectRatioRejectionCount;
		private int extentRejectionCount;
		private int boundaryRoughnessRejectionCount;
		private int borderContactRejectionCount;
		private int featureCalculationFailureCount;
		private final List<String> filterDiagnosticLines = new ArrayList<>();
		private final List<LumenCalibrationRow> calibrationRows = new ArrayList<>();
	}

	private static final class LumenCalibrationRow {

		private final LumenFeatures features;
		private final LumenFilterResult filterResult;

		private LumenCalibrationRow(
				LumenFeatures features,
				LumenFilterResult filterResult) {

			this.features = features;
			this.filterResult = filterResult;
		}
	}

	private static LumenFilterDiagnostics evaluateLumenFilters(
			LumenFeatureCalculator featureCalculator,
			LumenFilter lumenFilter,
			LumenFilterCriteria filterCriteria,
			List<Lumen> lumina) {

		LumenFilterDiagnostics diagnostics = new LumenFilterDiagnostics();

		for (Lumen lumen : lumina) {

			try {
				LumenFeatures features = featureCalculator.calculate(lumen);
				LumenFilterResult filterResult = lumenFilter.evaluate(
						features,
						filterCriteria);

				diagnostics.lumenFeatures.add(features);
				diagnostics.filterResultsByLumenId.put(
						lumen.getId(),
						filterResult);

				if (filterResult.isAccepted()) {
					diagnostics.acceptedCandidateCount++;
				} else {
					diagnostics.rejectedCandidateCount++;
				}

				if (filterResult.isRejectedByArea()) {
					diagnostics.areaRejectionCount++;
				}

				if (filterResult.isRejectedByCircularity()) {
					diagnostics.circularityRejectionCount++;
				}

				if (filterResult.isRejectedByAspectRatio()) {
					diagnostics.aspectRatioRejectionCount++;
				}

				if (filterResult.isRejectedByExtent()) {
					diagnostics.extentRejectionCount++;
				}

				if (filterResult.isRejectedByBoundaryRoughness()) {
					diagnostics.boundaryRoughnessRejectionCount++;
				}

				if (filterResult.isRejectedByBorderContact()) {
					diagnostics.borderContactRejectionCount++;
				}

				diagnostics.filterDiagnosticLines.add(
						formatSuccessfulFilterDiagnosticLine(
								features,
								filterResult));
				diagnostics.calibrationRows.add(
						new LumenCalibrationRow(features, filterResult));
			} catch (RuntimeException exception) {
				diagnostics.featureCalculationFailureCount++;
				diagnostics.filterDiagnosticLines.add(
						formatFailedFilterDiagnosticLine(
								lumen.getId(),
								exception.getMessage()));
			}
		}

		return diagnostics;
	}

	private static void logBridgeSelectionDiagnostics(
			List<String> bridgeSelectionDiagnosticLines,
			int totalBridgeCandidateCount,
			int luminaWithBridgeCandidateCount,
			int selectedBridgeCount) {

		IJ.log("Bridge selection diagnostics:");
		IJ.log(
				"lumenId,candidateCount,selected,firstBoundaryIndex,"
						+ "secondBoundaryIndex,firstX,firstY,secondX,secondY,"
						+ "bridgeLength,circularBoundarySeparation");

		for (String diagnosticLine : bridgeSelectionDiagnosticLines) {
			IJ.log(diagnosticLine);
		}

		IJ.log("Total bridge candidate count: "
				+ totalBridgeCandidateCount + ".");
		IJ.log("Lumina containing at least one bridge candidate: "
				+ luminaWithBridgeCandidateCount + ".");
		IJ.log("Selected bridge count: " + selectedBridgeCount + ".");
		IJ.log("Bridge minimum circular boundary separation: "
				+ BRIDGE_MINIMUM_CIRCULAR_BOUNDARY_SEPARATION + ".");
		IJ.log("Bridge maximum length: "
				+ formatFeatureValue(BRIDGE_MAXIMUM_LENGTH) + ".");
	}

	private static String formatBridgeSelectionDiagnosticLine(
			int lumenId,
			List<BridgeCandidate> bridgeCandidates,
			Optional<BridgeCandidate> selectedBridge) {

		if (!selectedBridge.isPresent()) {
			return lumenId + ","
					+ bridgeCandidates.size()
					+ ",false,,,,,,,,";
		}

		BridgeCandidate bridge = selectedBridge.get();
		Point firstPoint = bridge.getFirstPoint();
		Point secondPoint = bridge.getSecondPoint();

		return lumenId
				+ ","
				+ bridgeCandidates.size()
				+ ",true,"
				+ bridge.getFirstBoundaryIndex()
				+ ","
				+ bridge.getSecondBoundaryIndex()
				+ ","
				+ firstPoint.x
				+ ","
				+ firstPoint.y
				+ ","
				+ secondPoint.x
				+ ","
				+ secondPoint.y
				+ ","
				+ formatFeatureValue(bridge.getEuclideanDistance())
				+ ","
				+ bridge.getCircularBoundarySeparation();
	}

	private static final class ReconstructionPreviewOutcome {

		private final boolean selected;
		private final SinglePixelReconstructionResult reconstructionResult;
		private final Point seedPoint;
		private final String stopReason;

		private ReconstructionPreviewOutcome(
				boolean selected,
				SinglePixelReconstructionResult reconstructionResult,
				Point seedPoint,
				String stopReason) {

			this.selected = selected;
			this.reconstructionResult = reconstructionResult;
			this.seedPoint = seedPoint;
			this.stopReason = stopReason;
		}
	}

	private static void logReconstructionPreviewDiagnostics(
			List<String> growthPreviewDiagnosticLines,
			int growthPreviewLumenCount,
			int growthPreviewSelectedBridgeCount,
			int circularityImprovedLumenCount,
			int circularityNotImprovedLumenCount,
			int totalBridgeOpeningPixelsRemoved,
			int totalWoodPixelsAdded,
			int circularityImprovementSampleCount,
			double totalCircularityImprovement) {

		IJ.log("Single-pixel reconstruction diagnostics:");
		IJ.log(
				"lumenId,selected,seedX,seedY,originalPixels,openedPixels,"
						+ "removedBridgePixels,enclosedWoodPixels,bestPixels,"
						+ "addedWoodPixels,originalCircularity,bestCircularity,"
						+ "bestStep,performedSteps,improved,stopReason");

		for (String diagnosticLine : growthPreviewDiagnosticLines) {
			IJ.log(diagnosticLine);
		}

		IJ.log("Previewed lumen count: " + growthPreviewLumenCount + ".");
		IJ.log("Lumina with selected bridge: "
				+ growthPreviewSelectedBridgeCount + ".");
		IJ.log("Lumina whose circularity improved: "
				+ circularityImprovedLumenCount + ".");
		IJ.log("Lumina whose circularity did not improve: "
				+ circularityNotImprovedLumenCount + ".");
		IJ.log("Total bridge-opening pixels removed: "
				+ totalBridgeOpeningPixelsRemoved + ".");
		IJ.log("Total wood pixels added in best states: "
				+ totalWoodPixelsAdded + ".");
		IJ.log("Mean circularity improvement among improved lumina: "
				+ formatMeanCircularityImprovement(
						circularityImprovementSampleCount,
						totalCircularityImprovement) + ".");
		IJ.log("Bridge opening half width: " + BRIDGE_OPENING_HALF_WIDTH + ".");
		IJ.log("Growth search margin: " + GROWTH_SEARCH_MARGIN + ".");
		IJ.log("Maximum reconstruction steps: "
				+ MAXIMUM_RECONSTRUCTION_STEPS + ".");
		IJ.log("Non-improving step patience: "
				+ NON_IMPROVING_STEP_PATIENCE + ".");
	}

	private static String formatMeanCircularityImprovement(
			int sampleCount,
			double totalCircularityImprovement) {

		if (sampleCount == 0) {
			return formatFeatureValue(0.0);
		}

		return formatFeatureValue(totalCircularityImprovement / sampleCount);
	}

	private static String formatReconstructionPreviewDiagnosticLine(
			int lumenId,
			ReconstructionPreviewOutcome previewOutcome) {

		if (!previewOutcome.selected) {
			SinglePixelReconstructionResult reconstructionResult =
					previewOutcome.reconstructionResult;

			return lumenId
					+ ",false,,,"
					+ reconstructionResult.getOriginalLumenPixels().size()
					+ ","
					+ reconstructionResult.getOriginalLumenPixels().size()
					+ ",0,0,"
					+ reconstructionResult.getOriginalLumenPixels().size()
					+ ",0,"
					+ formatFeatureValue(reconstructionResult.getOriginalCircularity())
					+ ","
					+ formatFeatureValue(reconstructionResult.getBestCircularity())
					+ ",0,0,false,"
					+ previewOutcome.stopReason;
		}

		SinglePixelReconstructionResult reconstructionResult =
				previewOutcome.reconstructionResult;
		Point seedPoint = previewOutcome.seedPoint;

		return lumenId
				+ ",true,"
				+ seedPoint.x
				+ ","
				+ seedPoint.y
				+ ","
				+ reconstructionResult.getOriginalLumenPixels().size()
				+ ","
				+ reconstructionResult.getOpenedLumenPixels().size()
				+ ","
				+ reconstructionResult.getDirectlyRemovedBridgePixels().size()
				+ ","
				+ reconstructionResult.getBridgeEnclosedWoodPixels().size()
				+ ","
				+ reconstructionResult.getBestLumenPixels().size()
				+ ","
				+ reconstructionResult.getAddedWoodPixels().size()
				+ ","
				+ formatFeatureValue(reconstructionResult.getOriginalCircularity())
				+ ","
				+ formatFeatureValue(reconstructionResult.getBestCircularity())
				+ ","
				+ reconstructionResult.getBestReconstructionStep()
				+ ","
				+ reconstructionResult.getPerformedReconstructionSteps()
				+ ","
				+ reconstructionResult.isCircularityImproved()
				+ ","
				+ previewOutcome.stopReason;
	}

	private static ReconstructionPreviewOutcome paintReconstructionPreview(
			ColorProcessor growthPreviewProcessor,
			BridgeInteriorSeedFinder bridgeInteriorSeedFinder,
			BridgeEnclosedWoodFinder bridgeEnclosedWoodFinder,
			SinglePixelLumenReconstructor singlePixelLumenReconstructor,
			ImageProcessor classifiedImageProcessor,
			Lumen lumen,
			List<Point> lumenForegroundPixels,
			Optional<BridgeCandidate> selectedBridge,
			double originalFeatureCircularity) {

		boolean useDebugReconstructionVisualization =
				DEBUG_SINGLE_LUMEN_MODE && lumen.getId() == DEBUG_LUMEN_ID;

		if (!useDebugReconstructionVisualization) {
			paintPreviewPixels(
					growthPreviewProcessor,
					lumenForegroundPixels,
					GROWTH_PREVIEW_ORIGINAL_GRAY_RGB);
		}

		if (!selectedBridge.isPresent()) {
			SinglePixelReconstructionResult unchangedResult =
					createUnchangedReconstructionResult(
							lumenForegroundPixels,
							originalFeatureCircularity);

			return new ReconstructionPreviewOutcome(
					false,
					unchangedResult,
					null,
					"NO_SELECTION");
		}

		BridgeCandidate bridge = selectedBridge.get();
		Point firstBridgePoint = bridge.getFirstPoint();
		Point secondBridgePoint = bridge.getSecondPoint();
		Point seedPoint = bridgeInteriorSeedFinder.findSeed(
				lumenForegroundPixels,
				firstBridgePoint,
				secondBridgePoint);
		Set<Point> allowedWoodPixels = collectAllowedWoodPixels(
				classifiedImageProcessor,
				lumen,
				GROWTH_SEARCH_MARGIN);
		Set<Point> enclosedWoodPixels = bridgeEnclosedWoodFinder.findEnclosedWood(
				lumenForegroundPixels,
				allowedWoodPixels,
				firstBridgePoint,
				secondBridgePoint,
				BRIDGE_OPENING_HALF_WIDTH);

		if (useDebugReconstructionVisualization) {
			IJ.log("Debug reconstruction input:");
			IJ.log(
					"lumenId,bridgeFirstX,bridgeFirstY,bridgeSecondX,"
							+ "bridgeSecondY,enclosedWoodPixels");
			IJ.log(
					lumen.getId()
							+ ","
							+ firstBridgePoint.x
							+ ","
							+ firstBridgePoint.y
							+ ","
							+ secondBridgePoint.x
							+ ","
							+ secondBridgePoint.y
							+ ","
							+ enclosedWoodPixels.size());
		}

		SinglePixelReconstructionResult reconstructionResult =
				singlePixelLumenReconstructor.reconstructToMaximumCircularity(
						lumenForegroundPixels,
						allowedWoodPixels,
						firstBridgePoint,
						secondBridgePoint,
						seedPoint,
						BRIDGE_OPENING_HALF_WIDTH,
						MAXIMUM_RECONSTRUCTION_STEPS,
						NON_IMPROVING_STEP_PATIENCE);

		if (useDebugReconstructionVisualization) {
			List<Point> enclosedWoodList =
					new ArrayList<>(enclosedWoodPixels);

			IJ.log("Debug reconstruction result:");
			IJ.log(
					"lumenId,originalPixels,enclosedWoodPixels,bestPixels,"
							+ "addedPixels,bestIteration,performedIterations,"
							+ "originalCircularity,bestCircularity");
			IJ.log(
					lumen.getId()
							+ ","
							+ reconstructionResult.getOriginalLumenPixels().size()
							+ ","
							+ enclosedWoodList.size()
							+ ","
							+ reconstructionResult.getBestLumenPixels().size()
							+ ","
							+ reconstructionResult.getAddedWoodPixels().size()
							+ ","
							+ reconstructionResult.getBestReconstructionStep()
							+ ","
							+ reconstructionResult.getPerformedReconstructionSteps()
							+ ","
							+ formatFeatureValue(
									reconstructionResult.getOriginalCircularity())
							+ ","
							+ formatFeatureValue(
									reconstructionResult.getBestCircularity()));
			IJ.log(
					"Enclosed wood bounds: "
							+ formatPixelCollectionBounds(enclosedWoodList));
			IJ.log(
					"Best added-pixel bounds: "
							+ formatPixelCollectionBounds(
									reconstructionResult.getAddedWoodPixels()));

			paintPreviewPixels(
					growthPreviewProcessor,
					lumenForegroundPixels,
					GROWTH_PREVIEW_BEST_CYAN_RGB);
			paintPreviewPixels(
					growthPreviewProcessor,
					enclosedWoodList,
					DEBUG_RECONSTRUCTION_ENCLOSED_WOOD_RGB);
			paintPreviewPixels(
					growthPreviewProcessor,
					reconstructionResult.getAddedWoodPixels(),
					GROWTH_PREVIEW_ADDED_WOOD_ORANGE_RGB);
			paintEndpointRing(
					growthPreviewProcessor,
					firstBridgePoint.x,
					firstBridgePoint.y);
			paintEndpointRing(
					growthPreviewProcessor,
					secondBridgePoint.x,
					secondBridgePoint.y);

			if (reconstructionResult.getBestReconstructionStep() > 0) {
				Point lastPixelAddedAtBestStep = findLastPixelAddedAtBestStep(
						reconstructionResult,
						enclosedWoodPixels);

				if (lastPixelAddedAtBestStep != null) {
					paintSeedMarker(
							growthPreviewProcessor,
							lastPixelAddedAtBestStep.x,
							lastPixelAddedAtBestStep.y);
				}
			}
		} else {
			paintPreviewPixels(
					growthPreviewProcessor,
					reconstructionResult.getBestLumenPixels(),
					GROWTH_PREVIEW_BEST_CYAN_RGB);
			paintPreviewPixels(
					growthPreviewProcessor,
					reconstructionResult.getAddedWoodPixels(),
					GROWTH_PREVIEW_ADDED_WOOD_ORANGE_RGB);
			paintPreviewPixels(
					growthPreviewProcessor,
					reconstructionResult.getDirectlyRemovedBridgePixels(),
					GROWTH_PREVIEW_REMOVED_BRIDGE_WHITE_RGB);
			paintEndpointRing(
					growthPreviewProcessor,
					firstBridgePoint.x,
					firstBridgePoint.y);
			paintEndpointRing(
					growthPreviewProcessor,
					secondBridgePoint.x,
					secondBridgePoint.y);
			paintSeedMarker(
					growthPreviewProcessor,
					seedPoint.x,
					seedPoint.y);
		}

		return new ReconstructionPreviewOutcome(
				true,
				reconstructionResult,
				seedPoint,
				determineReconstructionStopReason(reconstructionResult));
	}

	private static void logDebugBridgeCandidates(
			List<BridgeCandidate> bridgeCandidates) {

		IJ.log("Debug bridge candidates for lumen " + DEBUG_LUMEN_ID + ":");
		IJ.log(
				"candidateNumber,firstBoundaryIndex,secondBoundaryIndex,"
						+ "firstX,firstY,secondX,secondY,bridgeLength,"
						+ "circularBoundarySeparation,enclosedWoodPixels");

		for (int candidateIndex = 0;
				candidateIndex < bridgeCandidates.size();
				candidateIndex++) {

			BridgeCandidate candidate = bridgeCandidates.get(candidateIndex);
			int candidateNumber = candidateIndex + 1;
			Point firstPoint = candidate.getFirstPoint();
			Point secondPoint = candidate.getSecondPoint();

			IJ.log(
					candidateNumber
							+ ","
							+ candidate.getFirstBoundaryIndex()
							+ ","
							+ candidate.getSecondBoundaryIndex()
							+ ","
							+ firstPoint.x
							+ ","
							+ firstPoint.y
							+ ","
							+ secondPoint.x
							+ ","
							+ secondPoint.y
							+ ","
							+ formatFeatureValue(candidate.getEuclideanDistance())
							+ ","
							+ candidate.getCircularBoundarySeparation()
							+ ",SKIPPED");
		}
	}

	private static void logArtifactBridgeTripletDiagnostics(
			int lumenId,
			List<ArtifactBridgeTriplet> candidates,
			boolean reconstructionAllowed,
			String reason,
			Optional<ArtifactBridgeTriplet> selectedTriplet) {

		IJ.log("Artifact bridge triplet diagnostics:");
		IJ.log(
				"lumenId,candidateCount,reconstructionAllowed,reason,"
						+ "firstBoundaryIndex,peakBoundaryIndex,secondBoundaryIndex,"
						+ "firstX,firstY,peakX,peakY,secondX,secondY,"
						+ "closingLineLength,artifactArcLength,arcToClosingLineRatio,"
						+ "peakDistanceFromClosingLine,firstDeviationScore,"
						+ "peakDeviationScore,secondDeviationScore");

		if (selectedTriplet.isPresent()) {
			IJ.log(formatArtifactBridgeTripletSummaryLine(
					lumenId,
					candidates.size(),
					reconstructionAllowed,
					reason,
					selectedTriplet.get()));
		} else {
			IJ.log(
					lumenId
							+ ",0,false,"
							+ reason
							+ ",,,,,,,,,,,,,,,,");
		}

		if (candidates.isEmpty()) {
			return;
		}

		IJ.log("Artifact bridge triplet candidates for lumen " + lumenId + ":");
		IJ.log(
				"candidateNumber,firstBoundaryIndex,peakBoundaryIndex,"
						+ "secondBoundaryIndex,firstX,firstY,peakX,peakY,secondX,"
						+ "secondY,closingLineLength,artifactArcLength,"
						+ "arcToClosingLineRatio,peakDistanceFromClosingLine,"
						+ "firstDeviationScore,peakDeviationScore,"
						+ "secondDeviationScore");

		for (int candidateIndex = 0;
				candidateIndex < candidates.size();
				candidateIndex++) {
			IJ.log(formatArtifactBridgeTripletCandidateLine(
					candidateIndex + 1,
					candidates.get(candidateIndex)));
		}
	}

	private static void logArtifactEdgeContourDiagnostics(
			int lumenId,
			int lumenPixelCount,
			int pixelCenterBoundaryPointCount,
			List<Point> edgeBoundaryPixels,
			double edgeLoopSignedArea,
			List<Integer> acceptedEdgePeakIndices) {

		IJ.log("Artifact edge contour diagnostics:");
		IJ.log(
				"lumenId,lumenPixels,pixelCenterBoundaryPoints,edgeBoundaryVertices,"
						+ "uniqueEdgeBoundaryVertices,edgeLoopSignedArea,acceptedEdgePeaks");
		IJ.log(
				lumenId
						+ ","
						+ lumenPixelCount
						+ ","
						+ pixelCenterBoundaryPointCount
						+ ","
						+ edgeBoundaryPixels.size()
						+ ","
						+ uniqueVertexCount(edgeBoundaryPixels)
						+ ","
						+ formatFeatureValue(edgeLoopSignedArea)
						+ ","
						+ acceptedEdgePeakIndices.size());
	}

	private static int uniqueVertexCount(List<Point> vertices) {

		Set<Long> uniqueKeys = new HashSet<>();

		for (Point vertex : vertices) {
			uniqueKeys.add((((long) vertex.y) << 32) | (vertex.x & 0xFFFFFFFFL));
		}

		return uniqueKeys.size();
	}

	private static void logArtifactArcInputDiagnostics(
			int lumenId,
			ArtifactEdgeBoundaryInputDiagnostics diagnostics) {

		IJ.log("Artifact arc input diagnostics:");
		IJ.log(
				"lumenId,boundarySize,deviationCount,uniqueVertexCount,"
						+ "duplicateOccurrenceCount,firstEqualsLast,minX,minY,maxX,maxY");
		IJ.log(
				lumenId
						+ ","
						+ diagnostics.boundarySize
						+ ","
						+ diagnostics.deviationCount
						+ ","
						+ diagnostics.uniqueVertexCount
						+ ","
						+ diagnostics.duplicateOccurrences.size()
						+ ","
						+ diagnostics.firstEqualsLast
						+ ","
						+ diagnostics.minX
						+ ","
						+ diagnostics.minY
						+ ","
						+ diagnostics.maxX
						+ ","
						+ diagnostics.maxY);

		IJ.log("Artifact edge-boundary duplicate vertices for lumen " + lumenId + ":");
		IJ.log("duplicateNumber,firstIndex,repeatedIndex,x,y");

		if (diagnostics.duplicateOccurrences.isEmpty()) {
			IJ.log("NONE");
		} else {

			for (int duplicateIndex = 0;
					duplicateIndex < diagnostics.duplicateOccurrences.size();
					duplicateIndex++) {
				ArtifactEdgeBoundaryDuplicateOccurrence duplicateOccurrence =
						diagnostics.duplicateOccurrences.get(duplicateIndex);
				IJ.log(
						(duplicateIndex + 1)
								+ ","
								+ duplicateOccurrence.firstIndex
								+ ","
								+ duplicateOccurrence.repeatedIndex
								+ ","
								+ duplicateOccurrence.x
								+ ","
								+ duplicateOccurrence.y);
			}
		}

		IJ.log("Artifact edge-boundary first/last:");
		IJ.log(
				"firstIndex,firstX,firstY,lastIndex,lastX,lastY,equal");
		IJ.log(
				diagnostics.firstIndex
						+ ","
						+ diagnostics.firstX
						+ ","
						+ diagnostics.firstY
						+ ","
						+ diagnostics.lastIndex
						+ ","
						+ diagnostics.lastX
						+ ","
						+ diagnostics.lastY
						+ ","
						+ diagnostics.firstEqualsLast);
	}

	private static void logArtifactArcInputRejected(
			int lumenId,
			String rejectionMessage) {

		IJ.log("Artifact arc input rejected:");
		IJ.log("lumenId,reason");
		IJ.log(lumenId + "," + rejectionMessage);
	}

	private static void logPixelEdgeTraceDiagnostics(
			PixelEdgeBoundaryTracer.TraceDiagnostics diagnostics) {

		IJ.log("Pixel-edge trace diagnostics:");
		IJ.log(
				"lumenId,lumenPixels,exposedDirectedEdges,tracedLoops,"
						+ "selectedOuterLoop,selectedVertices,distinctVertices,"
						+ "signedArea,repeatedVertexOccurrences,firstDuplicateX,"
						+ "firstDuplicateY,firstIndex,repeatedIndex,"
						+ "duplicateClassification,failureReason");

		Point firstRepeatedVertex = diagnostics.getFirstRepeatedVertex();
		String failureReason = diagnostics.getFailureReason();
		String duplicateClassification =
				diagnostics.getFirstRepeatedVertexClassification();

		IJ.log(
				diagnostics.getLumenId()
						+ ","
						+ diagnostics.getLumenPixelCount()
						+ ","
						+ diagnostics.getExposedDirectedEdgeCount()
						+ ","
						+ diagnostics.getTracedLoopCount()
						+ ","
						+ diagnostics.getSelectedOuterLoopNumber()
						+ ","
						+ diagnostics.getSelectedOuterLoopVertexCount()
						+ ","
						+ diagnostics.getSelectedOuterLoopDistinctVertexCount()
						+ ","
						+ String.format(
								"%.4f",
								diagnostics.getSelectedOuterLoopSignedArea())
						+ ","
						+ diagnostics.getRepeatedVertexOccurrenceCount()
						+ ","
						+ (firstRepeatedVertex == null
								? ""
								: firstRepeatedVertex.x)
						+ ","
						+ (firstRepeatedVertex == null
								? ""
								: firstRepeatedVertex.y)
						+ ","
						+ diagnostics.getFirstRepeatedVertexFirstIndex()
						+ ","
						+ diagnostics.getFirstRepeatedVertexRepeatedIndex()
						+ ","
						+ (duplicateClassification == null
								? ""
								: duplicateClassification)
						+ ","
						+ (failureReason == null ? "" : failureReason));
	}

	private static void logPixelEdgeTraceException(
			int lumenId,
			String exceptionMessage) {

		IJ.log("Pixel-edge trace exception:");
		IJ.log("lumenId,exceptionMessage");
		IJ.log(lumenId + "," + exceptionMessage);
	}

	private static void logPixelEdgeTraversalSummary(
			PixelEdgeBoundaryTracer.TraceDebugReport report) {

		IJ.log("Pixel-edge traversal summary:");
		IJ.log(
				"lumenId,diagnosticLoop,failingLoop,selectedOuterLoop,startingEdgeId,"
						+ "recordedSteps,loopUsedBeforeStart,loopUsedAtFailure,"
						+ "globalUsedAtFailure,firstFailureStep,firstFailureCode,"
						+ "firstFailureMessage");
		IJ.log(
				report.getLumenId()
						+ ","
						+ report.getDiagnosticLoopNumber()
						+ ","
						+ report.getFailingLoopNumber()
						+ ","
						+ report.getSelectedOuterLoopNumber()
						+ ","
						+ report.getDiagnosticStartingEdgeId()
						+ ","
						+ report.getTraversalSteps().size()
						+ (report.isStepsTruncated() ? "(truncated)" : "")
						+ ","
						+ report.getDiagnosticLoopUsedEdgeCountBeforeStart()
						+ ","
						+ report.getDiagnosticLoopUsedEdgeCountAtFailure()
						+ ","
						+ report.getTotalGloballyUsedEdgeCountAtFailure()
						+ ","
						+ report.getFirstFailureStep()
						+ ","
						+ (report.getFirstFailureCode() == null
								? ""
								: report.getFirstFailureCode())
						+ ","
						+ (report.getFirstFailureMessage() == null
								? ""
								: report.getFirstFailureMessage()));
	}

	private static void logPixelEdgeTraversalSteps(
			PixelEdgeBoundaryTracer.TraceDebugReport report) {

		IJ.log("Pixel-edge traversal steps for lumen " + DEBUG_LUMEN_ID + ":");
		IJ.log(
				"step,loop,currentEdgeId,startX,startY,endX,endY,currentDirection,"
						+ "currentVertexX,currentVertexY,incomingDirection,loopUsedBefore,"
						+ "loopUsedAfter,globalUsedBefore,globalUsedAfter,"
						+ "selectedSuccessorEdgeId,selectedSuccessorDirection,closureSelected,"
						+ "vertexTopologyClassification,diagonalJunctionRuleApplied,"
						+ "diagonalMappedDirection,diagonalMappedEdgeId,diagonalMappingValid,"
						+ "selectionReason,invariantFailureCode,invariantFailureMessage");

		for (PixelEdgeBoundaryTracer.TraversalStepDiagnostic step
				: report.getTraversalSteps()) {
			IJ.log(
					step.getStepNumber()
							+ ","
							+ step.getLoopNumber()
							+ ","
							+ step.getCurrentEdgeId()
							+ ","
							+ step.getCurrentEdgeFromX()
							+ ","
							+ step.getCurrentEdgeFromY()
							+ ","
							+ step.getCurrentEdgeToX()
							+ ","
							+ step.getCurrentEdgeToY()
							+ ","
							+ step.getCurrentDirection()
							+ ","
							+ step.getCurrentVertexX()
							+ ","
							+ step.getCurrentVertexY()
							+ ","
							+ step.getIncomingDirectionName()
							+ ","
							+ step.getLoopUsedCountBefore()
							+ ","
							+ step.getLoopUsedCountAfter()
							+ ","
							+ step.getGlobalUsedCountBefore()
							+ ","
							+ step.getGlobalUsedCountAfter()
							+ ","
							+ step.getSelectedSuccessorEdgeId()
							+ ","
							+ (step.getSelectedSuccessorDirection() == null
									? ""
									: step.getSelectedSuccessorDirection())
							+ ","
							+ step.isClosureSelected()
							+ ","
							+ (step.getVertexTopologyClassification() == null
									? ""
									: step.getVertexTopologyClassification())
							+ ","
							+ step.isDiagonalJunctionRuleApplied()
							+ ","
							+ (step.getDiagonalMappedDirection() == null
									? ""
									: step.getDiagonalMappedDirection())
							+ ","
							+ step.getDiagonalMappedEdgeId()
							+ ","
							+ step.isDiagonalMappingValid()
							+ ","
							+ (step.getSelectionReason() == null
									? ""
									: step.getSelectionReason())
							+ ","
							+ (step.getInvariantFailureCode() == null
									? ""
									: step.getInvariantFailureCode())
							+ ","
							+ (step.getInvariantFailureMessage() == null
									? ""
									: step.getInvariantFailureMessage()));

			IJ.log("Pixel-edge successor candidates for lumen "
					+ DEBUG_LUMEN_ID
					+ " step "
					+ step.getStepNumber()
					+ ":");
			IJ.log(
					"evaluationOrder,relativeTurn,edgeId,startX,startY,endX,endY,"
							+ "direction,edgeExists,startsAtCurrentVertex,exposed,"
							+ "alreadyUsed,rejectedReason,selected");

			for (PixelEdgeBoundaryTracer.SuccessorCandidateDiagnostic candidate
					: step.getSuccessorCandidates()) {
				IJ.log(
						candidate.getEvaluationOrder()
								+ ","
								+ candidate.getRelativeTurn()
								+ ","
								+ candidate.getCandidateEdgeId()
								+ ","
								+ candidate.getCandidateFromX()
								+ ","
								+ candidate.getCandidateFromY()
								+ ","
								+ candidate.getCandidateToX()
								+ ","
								+ candidate.getCandidateToY()
								+ ","
								+ candidate.getDirection()
								+ ","
								+ candidate.isEdgeExists()
								+ ","
								+ candidate.isStartsAtCurrentVertex()
								+ ","
								+ candidate.isExposed()
								+ ","
								+ candidate.isAlreadyUsed()
								+ ","
								+ candidate.getRejectedReason()
								+ ","
								+ candidate.isSelected());
			}
		}
	}

	private static void logPixelEdgeRepeatedVertexEdgeAnalysis(
			PixelEdgeBoundaryTracer.TraceDebugReport report) {

		PixelEdgeBoundaryTracer.RepeatedVertexEdgeAnalysis analysis =
				report.getRepeatedVertexEdgeAnalysis();

		IJ.log("Pixel-edge repeated-vertex edge analysis:");
		IJ.log(
				"lumenId,loopNumber,x,y,firstOccurrenceIndex,repeatedOccurrenceIndex,"
						+ "firstIncomingEdgeId,firstOutgoingEdgeId,repeatedIncomingEdgeId,"
						+ "repeatedOutgoingEdgeId,equalIncomingEdgeIds,equalOutgoingEdgeIds,"
						+ "anyEqualEdgeIds,equalIncomingCoordinatePairs,"
						+ "equalOutgoingCoordinatePairs,identicalIncomingEdgeObjects,"
						+ "identicalOutgoingEdgeObjects,inDegree,outDegree,"
						+ "unusedOutgoingEdgeIds,classification,firstForegroundFaceX,"
						+ "firstForegroundFaceY,repeatedForegroundFaceX,repeatedForegroundFaceY,"
						+ "sameForegroundFace,sameTopologicalState");

		if (analysis == null) {
			IJ.log("NONE");
			return;
		}

		IJ.log(
				analysis.getLumenId()
						+ ","
						+ analysis.getLoopNumber()
						+ ","
						+ analysis.getX()
						+ ","
						+ analysis.getY()
						+ ","
						+ analysis.getFirstOccurrenceIndex()
						+ ","
						+ analysis.getRepeatedOccurrenceIndex()
						+ ","
						+ analysis.getFirstIncomingEdgeId()
						+ ","
						+ analysis.getFirstOutgoingEdgeId()
						+ ","
						+ analysis.getRepeatedIncomingEdgeId()
						+ ","
						+ analysis.getRepeatedOutgoingEdgeId()
						+ ","
						+ analysis.isEqualIncomingEdgeIds()
						+ ","
						+ analysis.isEqualOutgoingEdgeIds()
						+ ","
						+ analysis.isAnyEqualEdgeIds()
						+ ","
						+ analysis.isEqualIncomingCoordinatePairs()
						+ ","
						+ analysis.isEqualOutgoingCoordinatePairs()
						+ ","
						+ analysis.isIdenticalIncomingEdgeObjects()
						+ ","
						+ analysis.isIdenticalOutgoingEdgeObjects()
						+ ","
						+ analysis.getInDegree()
						+ ","
						+ analysis.getOutDegree()
						+ ","
						+ formatIntegerList(analysis.getUnusedOutgoingEdgeIds())
						+ ","
						+ analysis.getEdgeIdClassification()
						+ ","
						+ (analysis.getFirstForegroundFacePixel() == null
								? ""
								: analysis.getFirstForegroundFacePixel().x)
						+ ","
						+ (analysis.getFirstForegroundFacePixel() == null
								? ""
								: analysis.getFirstForegroundFacePixel().y)
						+ ","
						+ (analysis.getRepeatedForegroundFacePixel() == null
								? ""
								: analysis.getRepeatedForegroundFacePixel().x)
						+ ","
						+ (analysis.getRepeatedForegroundFacePixel() == null
								? ""
								: analysis.getRepeatedForegroundFacePixel().y)
						+ ","
						+ analysis.isSameForegroundFace()
						+ ","
						+ analysis.isSameTopologicalState());
	}

	private static void logPixelEdgeLocalVertexTopology(
			PixelEdgeBoundaryTracer.TraceDebugReport report) {

		PixelEdgeBoundaryTracer.VertexTopologyDiagnostic topology =
				report.getVertexTopologyDiagnostic();

		IJ.log("Pixel-edge local vertex topology:");
		IJ.log(
				"lumenId,loopNumber,vertexX,vertexY,northWestForeground,northEastForeground,"
						+ "southWestForeground,southEastForeground,foregroundCount,"
						+ "topologyClassification,incidentIncomingEdgeIds,"
						+ "incidentOutgoingEdgeIds,unusedOutgoingEdgeIds");

		if (topology == null) {
			IJ.log("NONE");
			return;
		}

		IJ.log(
				report.getLumenId()
						+ ","
						+ report.getDiagnosticLoopNumber()
						+ ","
						+ topology.getVertex().x
						+ ","
						+ topology.getVertex().y
						+ ","
						+ topology.isNorthWestForeground()
						+ ","
						+ topology.isNorthEastForeground()
						+ ","
						+ topology.isSouthWestForeground()
						+ ","
						+ topology.isSouthEastForeground()
						+ ","
						+ topology.getForegroundCount()
						+ ","
						+ topology.getTopologyClassification()
						+ ","
						+ formatIntegerList(topology.getIncidentIncomingEdgeIds())
						+ ","
						+ formatIntegerList(topology.getIncidentOutgoingEdgeIds())
						+ ","
						+ formatIntegerList(topology.getUnusedOutgoingEdgeIds()));
	}

	private static void logPixelEdgeIncidentEdgeFaces(
			PixelEdgeBoundaryTracer.TraceDebugReport report) {

		PixelEdgeBoundaryTracer.VertexTopologyDiagnostic topology =
				report.getVertexTopologyDiagnostic();

		IJ.log("Pixel-edge incident edge faces:");
		IJ.log(
				"edgeId,direction,leftPixelX,leftPixelY,rightPixelX,rightPixelY,"
						+ "leftBelongsToLumen,rightBelongsToLumen,expectedLumenSide,"
						+ "orientationInvariantSatisfied");

		if (topology == null) {
			IJ.log("NONE");
			return;
		}

		for (PixelEdgeBoundaryTracer.EdgeFaceDiagnostic face
				: topology.getEdgeFaceDiagnostics()) {
			Point leftPixel = face.getLeftAdjacentPixel();
			Point rightPixel = face.getRightAdjacentPixel();
			IJ.log(
					face.getEdgeId()
							+ ","
							+ face.getDirection()
							+ ","
							+ (leftPixel == null ? "" : leftPixel.x)
							+ ","
							+ (leftPixel == null ? "" : leftPixel.y)
							+ ","
							+ (rightPixel == null ? "" : rightPixel.x)
							+ ","
							+ (rightPixel == null ? "" : rightPixel.y)
							+ ","
							+ face.isLeftPixelBelongsToLumen()
							+ ","
							+ face.isRightPixelBelongsToLumen()
							+ ","
							+ face.getExpectedLumenSide()
							+ ","
							+ face.isOrientationInvariantSatisfied());
		}
	}

	private static void logPixelEdgeVertexBranchPairings(
			PixelEdgeBoundaryTracer.TraceDebugReport report) {

		PixelEdgeBoundaryTracer.VertexTopologyDiagnostic topology =
				report.getVertexTopologyDiagnostic();

		IJ.log("Pixel-edge vertex branch pairings:");
		IJ.log(
				"incomingEdgeId,incomingDirection,outgoingEdgeId,outgoingDirection,"
						+ "relativeTurn,alreadyUsed,priorityRank,selectedByCurrentRule,"
						+ "preservesSameForegroundFace,wouldReturnToPreviouslyVisitedCoordinate,"
						+ "wouldReuseDirectedEdge,pairingClassification");

		if (topology == null) {
			IJ.log("NONE");
			return;
		}

		for (PixelEdgeBoundaryTracer.BranchPairingDiagnostic pairing
				: topology.getBranchPairingDiagnostics()) {
			IJ.log(
					pairing.getIncomingEdgeId()
							+ ","
							+ pairing.getIncomingDirection()
							+ ","
							+ pairing.getOutgoingEdgeId()
							+ ","
							+ pairing.getOutgoingDirection()
							+ ","
							+ pairing.getRelativeTurn()
							+ ","
							+ pairing.isOutgoingAlreadyUsedAtFailure()
							+ ","
							+ pairing.getSuccessorPriorityRank()
							+ ","
							+ pairing.isSelectedByCurrentRule()
							+ ","
							+ pairing.isPreservesSameForegroundFace()
							+ ","
							+ pairing.isWouldReturnToPreviouslyVisitedCoordinate()
							+ ","
							+ pairing.isWouldReuseDirectedEdge()
							+ ","
							+ pairing.getPairingClassification());
		}
	}

	private static String formatIntegerList(List<Integer> values) {

		if (values == null || values.isEmpty()) {
			return "[]";
		}

		StringBuilder builder = new StringBuilder("[");
		for (int index = 0; index < values.size(); index++) {
			if (index > 0) {
				builder.append('|');
			}
			builder.append(values.get(index));
		}
		builder.append(']');
		return builder.toString();
	}

	private static String extractStableReasonCode(String rejectionMessage) {

		if (rejectionMessage == null || rejectionMessage.isEmpty()) {
			return "UNKNOWN";
		}

		int separatorIndex = rejectionMessage.indexOf(':');

		if (separatorIndex < 0) {
			return rejectionMessage;
		}

		return rejectionMessage.substring(0, separatorIndex);
	}

	private static ArtifactEdgeBoundaryInputDiagnostics analyzeArtifactEdgeBoundaryInput(
			List<Point> edgeBoundaryPixels,
			double[] edgeDeviationScores) {

		if (edgeBoundaryPixels == null || edgeBoundaryPixels.isEmpty()) {
			return new ArtifactEdgeBoundaryInputDiagnostics(
					edgeBoundaryPixels == null ? 0 : edgeBoundaryPixels.size(),
					edgeDeviationScores == null ? 0 : edgeDeviationScores.length,
					0,
					Collections.emptyList(),
					false,
					-1,
					0,
					0,
					-1,
					0,
					0,
					Integer.MAX_VALUE,
					Integer.MAX_VALUE,
					Integer.MIN_VALUE,
					Integer.MIN_VALUE);
		}

		Map<Long, Integer> firstIndexByCoordinate = new HashMap<>();
		List<ArtifactEdgeBoundaryDuplicateOccurrence> duplicateOccurrences =
				new ArrayList<>();
		Set<Long> uniqueCoordinateKeys = new HashSet<>();
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;

		for (int boundaryIndex = 0;
				boundaryIndex < edgeBoundaryPixels.size();
				boundaryIndex++) {

			Point boundaryVertex = edgeBoundaryPixels.get(boundaryIndex);

			if (boundaryVertex == null) {
				continue;
			}

			long coordinateKey = encodeBoundaryCoordinate(
					boundaryVertex.x,
					boundaryVertex.y);
			uniqueCoordinateKeys.add(coordinateKey);

			if (firstIndexByCoordinate.containsKey(coordinateKey)) {
				duplicateOccurrences.add(new ArtifactEdgeBoundaryDuplicateOccurrence(
						firstIndexByCoordinate.get(coordinateKey),
						boundaryIndex,
						boundaryVertex.x,
						boundaryVertex.y));
			} else {
				firstIndexByCoordinate.put(coordinateKey, boundaryIndex);
			}

			minX = Math.min(minX, boundaryVertex.x);
			minY = Math.min(minY, boundaryVertex.y);
			maxX = Math.max(maxX, boundaryVertex.x);
			maxY = Math.max(maxY, boundaryVertex.y);
		}

		Point firstVertex = edgeBoundaryPixels.get(0);
		Point lastVertex = edgeBoundaryPixels.get(edgeBoundaryPixels.size() - 1);
		boolean firstEqualsLast = firstVertex != null
				&& lastVertex != null
				&& firstVertex.x == lastVertex.x
				&& firstVertex.y == lastVertex.y;

		return new ArtifactEdgeBoundaryInputDiagnostics(
				edgeBoundaryPixels.size(),
				edgeDeviationScores == null ? 0 : edgeDeviationScores.length,
				uniqueCoordinateKeys.size(),
				duplicateOccurrences,
				firstEqualsLast,
				0,
				firstVertex == null ? 0 : firstVertex.x,
				firstVertex == null ? 0 : firstVertex.y,
				edgeBoundaryPixels.size() - 1,
				lastVertex == null ? 0 : lastVertex.x,
				lastVertex == null ? 0 : lastVertex.y,
				minX,
				minY,
				maxX,
				maxY);
	}

	private static long encodeBoundaryCoordinate(int x, int y) {
		return (((long) y) << 32) | (x & 0xFFFFFFFFL);
	}

	private static final class ArtifactEdgeBoundaryDuplicateOccurrence {

		private final int firstIndex;
		private final int repeatedIndex;
		private final int x;
		private final int y;

		private ArtifactEdgeBoundaryDuplicateOccurrence(
				int firstIndex,
				int repeatedIndex,
				int x,
				int y) {
			this.firstIndex = firstIndex;
			this.repeatedIndex = repeatedIndex;
			this.x = x;
			this.y = y;
		}
	}

	private static final class ArtifactEdgeBoundaryInputDiagnostics {

		private final int boundarySize;
		private final int deviationCount;
		private final int uniqueVertexCount;
		private final List<ArtifactEdgeBoundaryDuplicateOccurrence> duplicateOccurrences;
		private final boolean firstEqualsLast;
		private final int firstIndex;
		private final int firstX;
		private final int firstY;
		private final int lastIndex;
		private final int lastX;
		private final int lastY;
		private final int minX;
		private final int minY;
		private final int maxX;
		private final int maxY;

		private ArtifactEdgeBoundaryInputDiagnostics(
				int boundarySize,
				int deviationCount,
				int uniqueVertexCount,
				List<ArtifactEdgeBoundaryDuplicateOccurrence> duplicateOccurrences,
				boolean firstEqualsLast,
				int firstIndex,
				int firstX,
				int firstY,
				int lastIndex,
				int lastX,
				int lastY,
				int minX,
				int minY,
				int maxX,
				int maxY) {
			this.boundarySize = boundarySize;
			this.deviationCount = deviationCount;
			this.uniqueVertexCount = uniqueVertexCount;
			this.duplicateOccurrences = duplicateOccurrences;
			this.firstEqualsLast = firstEqualsLast;
			this.firstIndex = firstIndex;
			this.firstX = firstX;
			this.firstY = firstY;
			this.lastIndex = lastIndex;
			this.lastX = lastX;
			this.lastY = lastY;
			this.minX = minX;
			this.minY = minY;
			this.maxX = maxX;
			this.maxY = maxY;
		}

		private static ArtifactEdgeBoundaryInputDiagnostics empty() {
			return new ArtifactEdgeBoundaryInputDiagnostics(
					0,
					0,
					0,
					Collections.emptyList(),
					false,
					-1,
					0,
					0,
					-1,
					0,
					0,
					Integer.MAX_VALUE,
					Integer.MAX_VALUE,
					Integer.MIN_VALUE,
					Integer.MIN_VALUE);
		}
	}

	private static void logArtifactArcDiagnostics(
			int lumenId,
			List<ArtifactArcCandidate> candidates,
			boolean reconstructionAllowed,
			String reason,
			Optional<ArtifactArcCandidate> selectedCandidate,
			List<Point> edgeBoundaryPixels) {

		IJ.log("Artifact arc diagnostics:");
		IJ.log(
				"lumenId,candidateCount,reconstructionAllowed,reason,"
						+ "firstBoundaryIndex,peakBoundaryIndex,secondBoundaryIndex,"
						+ "firstX,firstY,peakX,peakY,secondX,secondY,"
						+ "closingLineLength,artifactArcLength,arcToClosingLineRatio,"
						+ "peakDistance,secondGreatestPeakDistance,peakDominanceRatio,"
						+ "firstFlankPoints,secondFlankPoints,riseViolation,fallViolation,"
						+ "firstDeviationScore,peakDeviationScore,secondDeviationScore");

		if (selectedCandidate.isPresent()) {
			IJ.log(formatArtifactArcSummaryLine(
					lumenId,
					candidates.size(),
					reconstructionAllowed,
					reason,
					selectedCandidate.get()));
		} else {
			IJ.log(
					lumenId
							+ ",0,false,"
							+ reason
							+ ",,,,,,,,,,,,,,,,,,,,,,,,,");
		}

		if (candidates.isEmpty()) {
			return;
		}

		IJ.log("Artifact arc candidates for lumen " + lumenId + ":");
		IJ.log(
				"candidateNumber,firstBoundaryIndex,peakBoundaryIndex,"
						+ "secondBoundaryIndex,firstX,firstY,peakX,peakY,secondX,"
						+ "secondY,closingLineLength,artifactArcLength,"
						+ "arcToClosingLineRatio,peakDistance,secondGreatestPeakDistance,"
						+ "peakDominanceRatio,firstFlankPoints,secondFlankPoints,"
						+ "riseViolation,fallViolation,firstDeviationScore,"
						+ "peakDeviationScore,secondDeviationScore");

		for (int candidateIndex = 0;
				candidateIndex < candidates.size();
				candidateIndex++) {
			IJ.log(formatArtifactArcCandidateLine(
					candidateIndex + 1,
					candidates.get(candidateIndex)));
		}

		if (lumenId == DEBUG_LUMEN_ID) {
			logArtifactArcTShapeDiagnostics(lumenId, candidates);
		}

		if (selectedCandidate.isPresent()) {
			logSelectedArtifactDistanceProfile(
					lumenId,
					selectedCandidate.get(),
					edgeBoundaryPixels);
		}
	}

	private static void logArtifactArcTShapeDiagnostics(
			int lumenId,
			List<ArtifactArcCandidate> candidates) {

		IJ.log("Artifact arc T-shape diagnostics for lumen " + lumenId + ":");
		IJ.log(
				"EXISTING_RANK,candidateId,A,B,P,mouthWidth,protrusionDepth,"
						+ "depthToMouthRatio,artifactArcLength,arcToMouthRatio,"
						+ "relativeContourSpan,firstAttachmentTurnDegrees,"
						+ "secondAttachmentTurnDegrees,minimumAttachmentTurnDegrees,"
						+ "meanAttachmentTurnDegrees,attachmentTurnBalance,"
						+ "outsideWallDirectionDifferenceDegrees,"
						+ "outsideWallContinuityScore,outsideWallLineDeviation,"
						+ "peakArcFraction,peakCentrality,peakTurnDegrees,"
						+ "peakDominanceRatio,EXPERIMENTAL_T_SHAPE_SCORE,"
						+ "tShapeDiagnosticScore");

		for (int candidateIndex = 0;
				candidateIndex < candidates.size();
				candidateIndex++) {
			ArtifactArcCandidate candidate = candidates.get(candidateIndex);
			Point firstPoint = candidate.getFirstPoint();
			Point peakPoint = candidate.getPeakPoint();
			Point secondPoint = candidate.getSecondPoint();
			IJ.log(
					(candidateIndex + 1)
							+ ","
							+ (candidateIndex + 1)
							+ ",("
							+ firstPoint.x
							+ ","
							+ firstPoint.y
							+ "),("
							+ secondPoint.x
							+ ","
							+ secondPoint.y
							+ "),("
							+ peakPoint.x
							+ ","
							+ peakPoint.y
							+ "),"
							+ formatFeatureValue(candidate.getMouthWidth())
							+ ","
							+ formatFeatureValue(candidate.getProtrusionDepth())
							+ ","
							+ formatFeatureValue(candidate.getDepthToMouthRatio())
							+ ","
							+ formatFeatureValue(candidate.getArtifactArcLength())
							+ ","
							+ formatFeatureValue(candidate.getArcToMouthRatio())
							+ ","
							+ formatFeatureValue(candidate.getRelativeContourSpan())
							+ ","
							+ formatFeatureValue(candidate.getFirstAttachmentTurnDegrees())
							+ ","
							+ formatFeatureValue(candidate.getSecondAttachmentTurnDegrees())
							+ ","
							+ formatFeatureValue(
									candidate.getMinimumAttachmentTurnDegrees())
							+ ","
							+ formatFeatureValue(candidate.getMeanAttachmentTurnDegrees())
							+ ","
							+ formatFeatureValue(candidate.getAttachmentTurnBalance())
							+ ","
							+ formatFeatureValue(
									candidate.getOutsideWallDirectionDifferenceDegrees())
							+ ","
							+ formatFeatureValue(
									candidate.getOutsideWallContinuityScore())
							+ ","
							+ formatFeatureValue(candidate.getOutsideWallLineDeviation())
							+ ","
							+ formatFeatureValue(candidate.getPeakArcFraction())
							+ ","
							+ formatFeatureValue(candidate.getPeakCentrality())
							+ ","
							+ formatFeatureValue(candidate.getPeakTurnDegrees())
							+ ","
							+ formatFeatureValue(candidate.getPeakDominanceRatio())
							+ ","
							+ formatFeatureValue(candidate.getTShapeDiagnosticScore())
							+ ","
							+ formatFeatureValue(candidate.getTShapeDiagnosticScore()));
		}
	}

	private static void logSelectedArtifactDistanceProfile(
			int lumenId,
			ArtifactArcCandidate selectedCandidate,
			List<Point> edgeBoundaryPixels) {

		IJ.log("Selected artifact distance profile for lumen " + lumenId + ":");
		IJ.log("arcOffset,boundaryIndex,x,y,distanceToClosingLine");

		List<DistanceProfileSample> profileSamples = buildForwardArcDistanceProfile(
				selectedCandidate,
				edgeBoundaryPixels);

		for (DistanceProfileSample profileSample : profileSamples) {
			IJ.log(
					profileSample.arcOffset
							+ ","
							+ profileSample.boundaryIndex
							+ ","
							+ profileSample.x
							+ ","
							+ profileSample.y
							+ ","
							+ formatFeatureValue(profileSample.distanceToClosingLine));
		}
	}

	private static String formatArtifactArcSummaryLine(
			int lumenId,
			int candidateCount,
			boolean reconstructionAllowed,
			String reason,
			ArtifactArcCandidate candidate) {

		Point firstPoint = candidate.getFirstPoint();
		Point peakPoint = candidate.getPeakPoint();
		Point secondPoint = candidate.getSecondPoint();

		return lumenId
				+ ","
				+ candidateCount
				+ ","
				+ reconstructionAllowed
				+ ","
				+ reason
				+ ","
				+ candidate.getFirstBoundaryIndex()
				+ ","
				+ candidate.getPeakBoundaryIndex()
				+ ","
				+ candidate.getSecondBoundaryIndex()
				+ ","
				+ firstPoint.x
				+ ","
				+ firstPoint.y
				+ ","
				+ peakPoint.x
				+ ","
				+ peakPoint.y
				+ ","
				+ secondPoint.x
				+ ","
				+ secondPoint.y
				+ ","
				+ formatFeatureValue(candidate.getClosingLineLength())
				+ ","
				+ formatFeatureValue(candidate.getArtifactArcLength())
				+ ","
				+ formatFeatureValue(candidate.getArcToClosingLineRatio())
				+ ","
				+ formatFeatureValue(candidate.getPeakDistanceFromClosingLine())
				+ ","
				+ formatFeatureValue(candidate.getSecondGreatestPeakDistance())
				+ ","
				+ formatFeatureValue(candidate.getPeakDominanceRatio())
				+ ","
				+ candidate.getFirstFlankArcPointCount()
				+ ","
				+ candidate.getSecondFlankArcPointCount()
				+ ","
				+ formatFeatureValue(candidate.getMaximumProfileRiseViolation())
				+ ","
				+ formatFeatureValue(candidate.getMaximumProfileFallViolation())
				+ ","
				+ formatFeatureValue(candidate.getFirstEndpointDeviationScore())
				+ ","
				+ formatFeatureValue(candidate.getPeakDeviationScore())
				+ ","
				+ formatFeatureValue(candidate.getSecondEndpointDeviationScore());
	}

	private static String formatArtifactArcCandidateLine(
			int candidateNumber,
			ArtifactArcCandidate candidate) {

		Point firstPoint = candidate.getFirstPoint();
		Point peakPoint = candidate.getPeakPoint();
		Point secondPoint = candidate.getSecondPoint();

		return candidateNumber
				+ ","
				+ candidate.getFirstBoundaryIndex()
				+ ","
				+ candidate.getPeakBoundaryIndex()
				+ ","
				+ candidate.getSecondBoundaryIndex()
				+ ","
				+ firstPoint.x
				+ ","
				+ firstPoint.y
				+ ","
				+ peakPoint.x
				+ ","
				+ peakPoint.y
				+ ","
				+ secondPoint.x
				+ ","
				+ secondPoint.y
				+ ","
				+ formatFeatureValue(candidate.getClosingLineLength())
				+ ","
				+ formatFeatureValue(candidate.getArtifactArcLength())
				+ ","
				+ formatFeatureValue(candidate.getArcToClosingLineRatio())
				+ ","
				+ formatFeatureValue(candidate.getPeakDistanceFromClosingLine())
				+ ","
				+ formatFeatureValue(candidate.getSecondGreatestPeakDistance())
				+ ","
				+ formatFeatureValue(candidate.getPeakDominanceRatio())
				+ ","
				+ candidate.getFirstFlankArcPointCount()
				+ ","
				+ candidate.getSecondFlankArcPointCount()
				+ ","
				+ formatFeatureValue(candidate.getMaximumProfileRiseViolation())
				+ ","
				+ formatFeatureValue(candidate.getMaximumProfileFallViolation())
				+ ","
				+ formatFeatureValue(candidate.getFirstEndpointDeviationScore())
				+ ","
				+ formatFeatureValue(candidate.getPeakDeviationScore())
				+ ","
				+ formatFeatureValue(candidate.getSecondEndpointDeviationScore());
	}

	private static ImagePlus createDebugArtifactArcCandidatesImage(
			Lumen lumen,
			List<Point> edgeBoundaryPixels,
			List<ArtifactArcCandidate> artifactArcCandidates,
			Optional<ArtifactArcCandidate> selectedCandidate,
			boolean inputRejected,
			boolean edgeTraceFailed,
			String inputRejectionReasonCode,
			ArtifactEdgeBoundaryInputDiagnostics inputDiagnostics,
			PixelEdgeBoundaryTracer.TraceDiagnostics traceDiagnostics,
			PixelEdgeBoundaryTracer.TraceDebugReport traceDebugReport,
			int imageWidth,
			int imageHeight) {

		Rectangle boundingBox = lumen.getBoundingBox();
		int cropMinX = Math.max(
				0,
				boundingBox.x - DEBUG_RECONSTRUCTION_CROP_MARGIN);
		int cropMinY = Math.max(
				0,
				boundingBox.y - DEBUG_RECONSTRUCTION_CROP_MARGIN);
		int cropMaxX = Math.min(
				imageWidth,
				boundingBox.x + boundingBox.width + DEBUG_RECONSTRUCTION_CROP_MARGIN);
		int cropMaxY = Math.min(
				imageHeight,
				boundingBox.y + boundingBox.height + DEBUG_RECONSTRUCTION_CROP_MARGIN);
		int cropWidth = cropMaxX - cropMinX;
		int cropHeight = cropMaxY - cropMinY;

		if (cropWidth <= 0 || cropHeight <= 0) {
			return null;
		}

		ColorProcessor cropProcessor = new ColorProcessor(cropWidth, cropHeight);
		cropProcessor.setColor(colorFromPackedRgb(DEBUG_ARTIFACT_BACKGROUND_RGB));
		cropProcessor.fill();

		paintPreviewPixelsInCrop(
				cropProcessor,
				lumen.getPixels(),
				DEBUG_ARTIFACT_LUMEN_RGB,
				cropMinX,
				cropMinY);

		if (edgeTraceFailed && traceDebugReport != null) {
			paintTraceDebugCanonicalEdgesInCrop(
					cropProcessor,
					traceDebugReport,
					cropMinX,
					cropMinY);
		} else {
			paintClosedEdgeLoopInCrop(
					cropProcessor,
					edgeBoundaryPixels,
					DEBUG_ARTIFACT_BOUNDARY_CYAN_RGB,
					cropMinX,
					cropMinY);
			if (traceDebugReport != null) {
				paintDiagonalJunctionTransitionsInCrop(
						cropProcessor,
						traceDebugReport,
						cropMinX,
						cropMinY);
			}
		}

		if (inputRejected) {

			if (edgeTraceFailed && traceDebugReport != null) {
				paintTraceDebugFailureMarkersInCrop(
						cropProcessor,
						traceDebugReport,
						cropMinX,
						cropMinY);
				paintVertexTopologyInsetInCrop(
						cropProcessor,
						lumen,
						traceDebugReport,
						cropMinX,
						cropMinY);
			} else if (edgeTraceFailed && traceDiagnostics != null) {
				Point firstRepeatedVertex = traceDiagnostics.getFirstRepeatedVertex();

				if (firstRepeatedVertex != null) {
					paintSquareMarkerInCrop(
							cropProcessor,
							firstRepeatedVertex.x
									- DEBUG_ARTIFACT_TRACE_MARKER_OFFSET,
							firstRepeatedVertex.y
									- DEBUG_ARTIFACT_TRACE_MARKER_OFFSET,
							DEBUG_ARTIFACT_DUPLICATE_MARKER_RADIUS,
							DEBUG_ARTIFACT_TRACE_FIRST_DUPLICATE_RGB,
							cropMinX,
							cropMinY);
					paintSquareMarkerInCrop(
							cropProcessor,
							firstRepeatedVertex.x
									+ DEBUG_ARTIFACT_TRACE_MARKER_OFFSET,
							firstRepeatedVertex.y
									+ DEBUG_ARTIFACT_TRACE_MARKER_OFFSET,
							DEBUG_ARTIFACT_DUPLICATE_MARKER_RADIUS,
							DEBUG_ARTIFACT_TRACE_REPEATED_DUPLICATE_RGB,
							cropMinX,
							cropMinY);

					Point firstPrevious =
							traceDiagnostics.getFirstDuplicateFirstPrevious();
					Point firstNext = traceDiagnostics.getFirstDuplicateFirstNext();
					Point repeatedPrevious =
							traceDiagnostics.getFirstDuplicateRepeatedPrevious();
					Point repeatedNext =
							traceDiagnostics.getFirstDuplicateRepeatedNext();

					if (firstPrevious != null) {
						paintOnePixelBresenhamLineInCrop(
								cropProcessor,
								firstPrevious.x,
								firstPrevious.y,
								firstRepeatedVertex.x,
								firstRepeatedVertex.y,
								DEBUG_ARTIFACT_TRACE_FIRST_EDGE_RGB,
								cropMinX,
								cropMinY);
					}

					if (firstNext != null) {
						paintOnePixelBresenhamLineInCrop(
								cropProcessor,
								firstRepeatedVertex.x,
								firstRepeatedVertex.y,
								firstNext.x,
								firstNext.y,
								DEBUG_ARTIFACT_TRACE_FIRST_EDGE_RGB,
								cropMinX,
								cropMinY);
					}

					if (repeatedPrevious != null) {
						paintOnePixelBresenhamLineInCrop(
								cropProcessor,
								repeatedPrevious.x,
								repeatedPrevious.y,
								firstRepeatedVertex.x,
								firstRepeatedVertex.y,
								DEBUG_ARTIFACT_TRACE_REPEATED_EDGE_RGB,
								cropMinX,
								cropMinY);
					}

					if (repeatedNext != null) {
						paintOnePixelBresenhamLineInCrop(
								cropProcessor,
								firstRepeatedVertex.x,
								firstRepeatedVertex.y,
								repeatedNext.x,
								repeatedNext.y,
								DEBUG_ARTIFACT_TRACE_REPEATED_EDGE_RGB,
								cropMinX,
								cropMinY);
					}
				}
			} else {

				for (ArtifactEdgeBoundaryDuplicateOccurrence duplicateOccurrence
						: inputDiagnostics.duplicateOccurrences) {
					Point repeatedVertex = edgeBoundaryPixels.get(
							duplicateOccurrence.repeatedIndex);

					if (repeatedVertex != null) {
						paintSquareMarkerInCrop(
								cropProcessor,
								repeatedVertex.x,
								repeatedVertex.y,
								DEBUG_ARTIFACT_DUPLICATE_MARKER_RADIUS,
								DEBUG_ARTIFACT_DUPLICATE_VERTEX_RGB,
								cropMinX,
								cropMinY);
					}
				}

				if (inputDiagnostics.firstEqualsLast
						&& !edgeBoundaryPixels.isEmpty()) {
					Point closingVertex = edgeBoundaryPixels.get(0);

					if (closingVertex != null) {
						paintSquareMarkerInCrop(
								cropProcessor,
								closingVertex.x,
								closingVertex.y,
								DEBUG_ARTIFACT_DUPLICATE_MARKER_RADIUS,
								DEBUG_ARTIFACT_REPEATED_CLOSING_RGB,
								cropMinX,
								cropMinY);
					}
				}
			}
		} else {

			for (ArtifactArcCandidate candidate : artifactArcCandidates) {
				Point firstPoint = candidate.getFirstPoint();
				Point secondPoint = candidate.getSecondPoint();
				paintOnePixelBresenhamLineInCrop(
						cropProcessor,
						firstPoint.x,
						firstPoint.y,
						secondPoint.x,
						secondPoint.y,
						DEBUG_ARTIFACT_CANDIDATE_LINE_RGB,
						cropMinX,
						cropMinY);
			}

			if (selectedCandidate.isPresent()) {
				ArtifactArcCandidate candidate = selectedCandidate.get();
				Point firstPoint = candidate.getFirstPoint();
				Point peakPoint = candidate.getPeakPoint();
				Point secondPoint = candidate.getSecondPoint();

				paintForwardBoundaryArcInCrop(
						cropProcessor,
						edgeBoundaryPixels,
						candidate.getFirstBoundaryIndex(),
						candidate.getSecondBoundaryIndex(),
						DEBUG_ARTIFACT_ARTIFACT_ARC_RGB,
						cropMinX,
						cropMinY);
				paintThickOnePixelBresenhamLineInCrop(
						cropProcessor,
						firstPoint.x,
						firstPoint.y,
						secondPoint.x,
						secondPoint.y,
						DEBUG_ARTIFACT_SELECTED_LINE_RGB,
						DEBUG_ARTIFACT_SELECTED_LINE_THICKNESS,
						cropMinX,
						cropMinY);
				paintSquareMarkerInCrop(
						cropProcessor,
						firstPoint.x,
						firstPoint.y,
						DEBUG_ARTIFACT_SQUARE_MARKER_RADIUS,
						DEBUG_ARTIFACT_FIRST_BRIDGE_RGB,
						cropMinX,
						cropMinY);
				paintEndpointRingInCrop(
						cropProcessor,
						peakPoint.x,
						peakPoint.y,
						DEBUG_ARTIFACT_BRIDGE_PEAK_RING_RADIUS,
						DEBUG_ARTIFACT_BRIDGE_PEAK_RGB,
						cropMinX,
						cropMinY);
				paintSquareMarkerInCrop(
						cropProcessor,
						secondPoint.x,
						secondPoint.y,
						DEBUG_ARTIFACT_SQUARE_MARKER_RADIUS,
						DEBUG_ARTIFACT_SECOND_BRIDGE_RGB,
						cropMinX,
						cropMinY);
			}
		}

		int scaledWidth = cropWidth * DEBUG_RECONSTRUCTION_CROP_SCALE;
		int scaledHeight = cropHeight * DEBUG_RECONSTRUCTION_CROP_SCALE;
		ColorProcessor scaledProcessor =
				new ColorProcessor(scaledWidth, scaledHeight);

		for (int scaledY = 0; scaledY < scaledHeight; scaledY++) {

			for (int scaledX = 0; scaledX < scaledWidth; scaledX++) {
				scaledProcessor.putPixel(
						scaledX,
						scaledY,
						cropProcessor.get(
								scaledX / DEBUG_RECONSTRUCTION_CROP_SCALE,
								scaledY / DEBUG_RECONSTRUCTION_CROP_SCALE));
			}
		}

		ImagePlus artifactArcCandidatesImage = new ImagePlus(
				"Artifact Arc Candidates - Lumen " + DEBUG_LUMEN_ID,
				scaledProcessor);
		Overlay artifactLabels = new Overlay();

		if (inputRejected) {
			TextRoi inputRejectedLabel = new TextRoi(
					8,
					8,
					"INPUT REJECTED");
			inputRejectedLabel.setFont(DEBUG_ARTIFACT_LABEL_FONT);
			inputRejectedLabel.setStrokeColor(Color.WHITE);
			inputRejectedLabel.setFillColor(Color.BLACK);
			artifactLabels.add(inputRejectedLabel);

			TextRoi rejectionReasonLabel = new TextRoi(
					8,
					24,
					inputRejectionReasonCode);
			rejectionReasonLabel.setFont(DEBUG_ARTIFACT_LABEL_FONT);
			rejectionReasonLabel.setStrokeColor(Color.WHITE);
			rejectionReasonLabel.setFillColor(Color.BLACK);
			artifactLabels.add(rejectionReasonLabel);

			if (edgeTraceFailed && traceDebugReport != null) {
				String failureCode = traceDebugReport.getFirstFailureCode();
				TextRoi failureCodeLabel = new TextRoi(
						8,
						40,
						failureCode == null || failureCode.isEmpty()
								? "UNKNOWN"
								: failureCode);
				failureCodeLabel.setFont(DEBUG_ARTIFACT_LABEL_FONT);
				failureCodeLabel.setStrokeColor(Color.WHITE);
				failureCodeLabel.setFillColor(Color.BLACK);
				artifactLabels.add(failureCodeLabel);

				TextRoi loopStepLabel = new TextRoi(
						8,
						56,
						"LOOP "
								+ traceDebugReport.getDiagnosticLoopNumber()
								+ " STEP "
								+ traceDebugReport.getFirstFailureStep());
				loopStepLabel.setFont(DEBUG_ARTIFACT_LABEL_FONT);
				loopStepLabel.setStrokeColor(Color.WHITE);
				loopStepLabel.setFillColor(Color.BLACK);
				artifactLabels.add(loopStepLabel);

				PixelEdgeBoundaryTracer.VertexTopologyDiagnostic topology =
						traceDebugReport.getVertexTopologyDiagnostic();
				PixelEdgeBoundaryTracer.RepeatedVertexEdgeAnalysis edgeAnalysis =
						traceDebugReport.getRepeatedVertexEdgeAnalysis();

				if (topology != null) {
					TextRoi topologyLabel = new TextRoi(
							8,
							72,
							topology.getTopologyClassification());
					topologyLabel.setFont(DEBUG_ARTIFACT_LABEL_FONT);
					topologyLabel.setStrokeColor(Color.WHITE);
					topologyLabel.setFillColor(Color.BLACK);
					artifactLabels.add(topologyLabel);
				}

				if (edgeAnalysis != null) {
					Point firstFace = edgeAnalysis.getFirstForegroundFacePixel();
					Point repeatedFace = edgeAnalysis.getRepeatedForegroundFacePixel();
					String faceSummary = "first face="
							+ (firstFace == null
									? "NONE"
									: "(" + firstFace.x + "," + firstFace.y + ")")
							+ " repeated face="
							+ (repeatedFace == null
									? "NONE"
									: "(" + repeatedFace.x + "," + repeatedFace.y + ")")
							+ " same face="
							+ edgeAnalysis.isSameForegroundFace();
					TextRoi faceLabel = new TextRoi(8, 88, faceSummary);
					faceLabel.setFont(DEBUG_ARTIFACT_LABEL_FONT);
					faceLabel.setStrokeColor(Color.WHITE);
					faceLabel.setFillColor(Color.BLACK);
					artifactLabels.add(faceLabel);
				}
			} else if (edgeTraceFailed && traceDiagnostics != null) {
				String duplicateClassification =
						traceDiagnostics.getFirstRepeatedVertexClassification();
				TextRoi duplicateClassificationLabel = new TextRoi(
						8,
						40,
						duplicateClassification == null
								? "UNKNOWN"
								: duplicateClassification);
				duplicateClassificationLabel.setFont(DEBUG_ARTIFACT_LABEL_FONT);
				duplicateClassificationLabel.setStrokeColor(Color.WHITE);
				duplicateClassificationLabel.setFillColor(Color.BLACK);
				artifactLabels.add(duplicateClassificationLabel);
			}
		} else if (selectedCandidate.isPresent()) {
			ArtifactArcCandidate candidate = selectedCandidate.get();
			addArtifactTripletLabel(
					artifactLabels,
					candidate.getFirstPoint(),
					"A",
					DEBUG_ARTIFACT_FIRST_BRIDGE_RGB,
					cropMinX,
					cropMinY);
			addArtifactTripletLabel(
					artifactLabels,
					candidate.getPeakPoint(),
					"P",
					DEBUG_ARTIFACT_BRIDGE_PEAK_RGB,
					cropMinX,
					cropMinY);
			addArtifactTripletLabel(
					artifactLabels,
					candidate.getSecondPoint(),
					"B",
					DEBUG_ARTIFACT_SECOND_BRIDGE_RGB,
					cropMinX,
					cropMinY);
		} else {
			TextRoi noValidArcLabel = new TextRoi(
					scaledWidth / 2 - 90,
					scaledHeight / 2 - 6,
					"NO VALID SINGLE-PEAKED ARC");
			noValidArcLabel.setFont(DEBUG_ARTIFACT_LABEL_FONT);
			noValidArcLabel.setStrokeColor(Color.WHITE);
			noValidArcLabel.setFillColor(Color.BLACK);
			artifactLabels.add(noValidArcLabel);
		}

		if (!inputRejected) {
			addArtifactTShapeOverlayLabels(
					artifactLabels,
					artifactArcCandidates,
					cropMinX,
					cropMinY);
		}

		if (!edgeTraceFailed && traceDebugReport != null) {
			addDiagonalJunctionOverlayLabels(
					artifactLabels,
					traceDebugReport,
					cropMinX,
					cropMinY);
		}

		artifactArcCandidatesImage.setOverlay(artifactLabels);

		return artifactArcCandidatesImage;
	}

	private static void addArtifactTShapeOverlayLabels(
			Overlay artifactLabels,
			List<ArtifactArcCandidate> artifactArcCandidates,
			int cropMinX,
			int cropMinY) {

		for (ArtifactArcCandidate candidate : artifactArcCandidates) {
			Point firstPoint = candidate.getFirstPoint();
			Point secondPoint = candidate.getSecondPoint();
			int midpointX = (firstPoint.x + secondPoint.x) / 2;
			int midpointY = (firstPoint.y + secondPoint.y) / 2;
			int scaledX =
					(midpointX - cropMinX) * DEBUG_RECONSTRUCTION_CROP_SCALE
							+ DEBUG_RECONSTRUCTION_CROP_SCALE;
			int scaledY =
					(midpointY - cropMinY) * DEBUG_RECONSTRUCTION_CROP_SCALE
							+ DEBUG_RECONSTRUCTION_CROP_SCALE;
			String labelText = "D/M="
					+ formatFeatureValue(candidate.getDepthToMouthRatio())
					+ "\nTurn="
					+ formatFeatureValue(candidate.getMinimumAttachmentTurnDegrees())
					+ "\nWall="
					+ formatFeatureValue(candidate.getOutsideWallContinuityScore())
					+ "\nT="
					+ formatFeatureValue(candidate.getTShapeDiagnosticScore());
			TextRoi tShapeLabel = new TextRoi(scaledX, scaledY, labelText);
			tShapeLabel.setFont(DEBUG_ARTIFACT_LABEL_FONT);
			tShapeLabel.setStrokeColor(Color.WHITE);
			tShapeLabel.setFillColor(Color.BLACK);
			artifactLabels.add(tShapeLabel);
		}
	}

	private static ImagePlus createDebugArtifactDistanceProfileImage(
			ArtifactArcCandidate selectedCandidate,
			List<Point> edgeBoundaryPixels) {

		List<DistanceProfileSample> profileSamples = buildForwardArcDistanceProfile(
				selectedCandidate,
				edgeBoundaryPixels);

		if (profileSamples.isEmpty()) {
			return null;
		}

		ColorProcessor profileProcessor = new ColorProcessor(
				DEBUG_ARTIFACT_DISTANCE_PROFILE_WIDTH,
				DEBUG_ARTIFACT_DISTANCE_PROFILE_HEIGHT);
		profileProcessor.setColor(colorFromPackedRgb(DEBUG_ARTIFACT_BACKGROUND_RGB));
		profileProcessor.fill();

		double maximumDistance = 0.0;

		for (DistanceProfileSample profileSample : profileSamples) {
			maximumDistance = Math.max(
					maximumDistance,
					profileSample.distanceToClosingLine);
		}

		if (maximumDistance <= 0.0) {
			maximumDistance = 1.0;
		}

		int plotWidth = DEBUG_ARTIFACT_DISTANCE_PROFILE_WIDTH
				- (2 * DEBUG_ARTIFACT_DISTANCE_PROFILE_MARGIN);
		int plotHeight = DEBUG_ARTIFACT_DISTANCE_PROFILE_HEIGHT
				- (2 * DEBUG_ARTIFACT_DISTANCE_PROFILE_MARGIN);
		int lastArcOffset = profileSamples.get(profileSamples.size() - 1).arcOffset;

		for (int segmentIndex = 1;
				segmentIndex < profileSamples.size();
				segmentIndex++) {

			DistanceProfileSample previousSample =
					profileSamples.get(segmentIndex - 1);
			DistanceProfileSample currentSample =
					profileSamples.get(segmentIndex);
			int previousPlotX = DEBUG_ARTIFACT_DISTANCE_PROFILE_MARGIN
					+ (lastArcOffset == 0
							? 0
							: previousSample.arcOffset * plotWidth / lastArcOffset);
			int currentPlotX = DEBUG_ARTIFACT_DISTANCE_PROFILE_MARGIN
					+ (lastArcOffset == 0
							? 0
							: currentSample.arcOffset * plotWidth / lastArcOffset);
			int previousPlotY = DEBUG_ARTIFACT_DISTANCE_PROFILE_HEIGHT
					- DEBUG_ARTIFACT_DISTANCE_PROFILE_MARGIN
					- (int) Math.round(
							previousSample.distanceToClosingLine * plotHeight
									/ maximumDistance);
			int currentPlotY = DEBUG_ARTIFACT_DISTANCE_PROFILE_HEIGHT
					- DEBUG_ARTIFACT_DISTANCE_PROFILE_MARGIN
					- (int) Math.round(
							currentSample.distanceToClosingLine * plotHeight
									/ maximumDistance);

			paintOnePixelBresenhamLineInCrop(
					profileProcessor,
					previousPlotX,
					previousPlotY,
					currentPlotX,
					currentPlotY,
					DEBUG_ARTIFACT_DISTANCE_PROFILE_LINE_RGB,
					0,
					0);
		}

		for (DistanceProfileSample profileSample : profileSamples) {

			if (profileSample.boundaryIndex
					!= selectedCandidate.getPeakBoundaryIndex()) {
				continue;
			}

			int peakPlotX = DEBUG_ARTIFACT_DISTANCE_PROFILE_MARGIN
					+ (lastArcOffset == 0
							? 0
							: profileSample.arcOffset * plotWidth / lastArcOffset);
			int peakPlotY = DEBUG_ARTIFACT_DISTANCE_PROFILE_HEIGHT
					- DEBUG_ARTIFACT_DISTANCE_PROFILE_MARGIN
					- (int) Math.round(
							profileSample.distanceToClosingLine * plotHeight
									/ maximumDistance);

			paintEndpointRingInCrop(
					profileProcessor,
					peakPlotX,
					peakPlotY,
					DEBUG_ARTIFACT_BRIDGE_PEAK_RING_RADIUS,
					DEBUG_ARTIFACT_DISTANCE_PROFILE_PEAK_RGB,
					0,
					0);
		}

		ImagePlus distanceProfileImage = new ImagePlus(
				"Artifact Distance Profile - Lumen " + DEBUG_LUMEN_ID,
				profileProcessor);
		Overlay profileLabels = new Overlay();
		addArtifactLegendLine(profileLabels, 8, 8, "A");
		addArtifactLegendLine(
				profileLabels,
				DEBUG_ARTIFACT_DISTANCE_PROFILE_WIDTH / 2 - 4,
				8,
				"P");
		addArtifactLegendLine(
				profileLabels,
				DEBUG_ARTIFACT_DISTANCE_PROFILE_WIDTH - 20,
				8,
				"B");
		distanceProfileImage.setOverlay(profileLabels);

		return distanceProfileImage;
	}

	private static List<DistanceProfileSample> buildForwardArcDistanceProfile(
			ArtifactArcCandidate selectedCandidate,
			List<Point> edgeBoundaryPixels) {

		List<DistanceProfileSample> profileSamples = new ArrayList<>();
		Point firstPoint = selectedCandidate.getFirstPoint();
		Point secondPoint = selectedCandidate.getSecondPoint();
		int boundarySize = edgeBoundaryPixels.size();
		int currentBoundaryIndex = selectedCandidate.getFirstBoundaryIndex();
		int arcOffset = 0;

		while (true) {
			Point currentPoint = edgeBoundaryPixels.get(currentBoundaryIndex);
			double distanceToClosingLine = distancePointToSegment(
					currentPoint,
					firstPoint,
					secondPoint);
			profileSamples.add(new DistanceProfileSample(
					arcOffset,
					currentBoundaryIndex,
					currentPoint.x,
					currentPoint.y,
					distanceToClosingLine));

			if (currentBoundaryIndex
					== selectedCandidate.getSecondBoundaryIndex()) {
				break;
			}

			currentBoundaryIndex =
					(currentBoundaryIndex + 1) % boundarySize;
			arcOffset++;
		}

		return profileSamples;
	}

	private static double distancePointToSegment(
			Point point,
			Point segmentStart,
			Point segmentEnd) {

		double deltaX = segmentEnd.x - segmentStart.x;
		double deltaY = segmentEnd.y - segmentStart.y;
		double segmentLengthSquared = deltaX * deltaX + deltaY * deltaY;

		if (segmentLengthSquared == 0.0) {
			return Math.hypot(
					point.x - segmentStart.x,
					point.y - segmentStart.y);
		}

		double projectionNumerator =
				(point.x - segmentStart.x) * deltaX
						+ (point.y - segmentStart.y) * deltaY;
		double projectionParameter = projectionNumerator / segmentLengthSquared;
		projectionParameter = Math.max(0.0, Math.min(1.0, projectionParameter));

		double projectedX = segmentStart.x + projectionParameter * deltaX;
		double projectedY = segmentStart.y + projectionParameter * deltaY;

		return Math.hypot(point.x - projectedX, point.y - projectedY);
	}

	private static final class DistanceProfileSample {

		private final int arcOffset;
		private final int boundaryIndex;
		private final int x;
		private final int y;
		private final double distanceToClosingLine;

		private DistanceProfileSample(
				int arcOffset,
				int boundaryIndex,
				int x,
				int y,
				double distanceToClosingLine) {
			this.arcOffset = arcOffset;
			this.boundaryIndex = boundaryIndex;
			this.x = x;
			this.y = y;
			this.distanceToClosingLine = distanceToClosingLine;
		}
	}

	private static String formatArtifactBridgeTripletSummaryLine(
			int lumenId,
			int candidateCount,
			boolean reconstructionAllowed,
			String reason,
			ArtifactBridgeTriplet triplet) {

		Point firstPoint = triplet.getFirstBridgePoint();
		Point peakPoint = triplet.getBridgePeakPoint();
		Point secondPoint = triplet.getSecondBridgePoint();

		return lumenId
				+ ","
				+ candidateCount
				+ ","
				+ reconstructionAllowed
				+ ","
				+ reason
				+ ","
				+ triplet.getFirstBridgeBoundaryIndex()
				+ ","
				+ triplet.getBridgePeakBoundaryIndex()
				+ ","
				+ triplet.getSecondBridgeBoundaryIndex()
				+ ","
				+ firstPoint.x
				+ ","
				+ firstPoint.y
				+ ","
				+ peakPoint.x
				+ ","
				+ peakPoint.y
				+ ","
				+ secondPoint.x
				+ ","
				+ secondPoint.y
				+ ","
				+ formatFeatureValue(triplet.getClosingLineLength())
				+ ","
				+ formatFeatureValue(triplet.getArtifactArcLength())
				+ ","
				+ formatFeatureValue(triplet.getArcToClosingLineRatio())
				+ ","
				+ formatFeatureValue(triplet.getPeakDistanceFromClosingLine())
				+ ","
				+ formatFeatureValue(triplet.getFirstBridgeDeviationScore())
				+ ","
				+ formatFeatureValue(triplet.getBridgePeakDeviationScore())
				+ ","
				+ formatFeatureValue(triplet.getSecondBridgeDeviationScore());
	}

	private static String formatArtifactBridgeTripletCandidateLine(
			int candidateNumber,
			ArtifactBridgeTriplet triplet) {

		Point firstPoint = triplet.getFirstBridgePoint();
		Point peakPoint = triplet.getBridgePeakPoint();
		Point secondPoint = triplet.getSecondBridgePoint();

		return candidateNumber
				+ ","
				+ triplet.getFirstBridgeBoundaryIndex()
				+ ","
				+ triplet.getBridgePeakBoundaryIndex()
				+ ","
				+ triplet.getSecondBridgeBoundaryIndex()
				+ ","
				+ firstPoint.x
				+ ","
				+ firstPoint.y
				+ ","
				+ peakPoint.x
				+ ","
				+ peakPoint.y
				+ ","
				+ secondPoint.x
				+ ","
				+ secondPoint.y
				+ ","
				+ formatFeatureValue(triplet.getClosingLineLength())
				+ ","
				+ formatFeatureValue(triplet.getArtifactArcLength())
				+ ","
				+ formatFeatureValue(triplet.getArcToClosingLineRatio())
				+ ","
				+ formatFeatureValue(triplet.getPeakDistanceFromClosingLine())
				+ ","
				+ formatFeatureValue(triplet.getFirstBridgeDeviationScore())
				+ ","
				+ formatFeatureValue(triplet.getBridgePeakDeviationScore())
				+ ","
				+ formatFeatureValue(triplet.getSecondBridgeDeviationScore());
	}

	private static ImagePlus createDebugArtifactBridgeTripletsImage(
			Lumen lumen,
			List<Point> pixelCenterBoundaryPixels,
			List<Point> edgeBoundaryPixels,
			List<Integer> acceptedEdgePeakIndices,
			List<ArtifactBridgeTriplet> artifactBridgeTriplets,
			Optional<ArtifactBridgeTriplet> selectedTriplet,
			int imageWidth,
			int imageHeight) {

		Rectangle boundingBox = lumen.getBoundingBox();
		int cropMinX = Math.max(
				0,
				boundingBox.x - DEBUG_RECONSTRUCTION_CROP_MARGIN);
		int cropMinY = Math.max(
				0,
				boundingBox.y - DEBUG_RECONSTRUCTION_CROP_MARGIN);
		int cropMaxX = Math.min(
				imageWidth,
				boundingBox.x + boundingBox.width + DEBUG_RECONSTRUCTION_CROP_MARGIN);
		int cropMaxY = Math.min(
				imageHeight,
				boundingBox.y + boundingBox.height + DEBUG_RECONSTRUCTION_CROP_MARGIN);
		int cropWidth = cropMaxX - cropMinX;
		int cropHeight = cropMaxY - cropMinY;

		if (cropWidth <= 0 || cropHeight <= 0) {
			return null;
		}

		ColorProcessor cropProcessor = new ColorProcessor(cropWidth, cropHeight);
		cropProcessor.setColor(colorFromPackedRgb(DEBUG_ARTIFACT_BACKGROUND_RGB));
		cropProcessor.fill();

		paintPreviewPixelsInCrop(
				cropProcessor,
				lumen.getPixels(),
				DEBUG_ARTIFACT_LUMEN_RGB,
				cropMinX,
				cropMinY);
		paintPreviewPixelsInCrop(
				cropProcessor,
				pixelCenterBoundaryPixels,
				DEBUG_ARTIFACT_PIXEL_CENTER_BOUNDARY_RGB,
				cropMinX,
				cropMinY);
		paintClosedEdgeLoopInCrop(
				cropProcessor,
				edgeBoundaryPixels,
				DEBUG_ARTIFACT_BOUNDARY_CYAN_RGB,
				cropMinX,
				cropMinY);

		for (int peakIndex : acceptedEdgePeakIndices) {
			Point peakPoint = edgeBoundaryPixels.get(peakIndex);
			paintEndpointRingInCrop(
					cropProcessor,
					peakPoint.x,
					peakPoint.y,
					DEBUG_ARTIFACT_PEAK_RING_RADIUS,
					DEBUG_ARTIFACT_PEAK_RING_RGB,
					cropMinX,
					cropMinY);
		}

		for (ArtifactBridgeTriplet triplet : artifactBridgeTriplets) {
			Point firstPoint = triplet.getFirstBridgePoint();
			Point secondPoint = triplet.getSecondBridgePoint();
			paintOnePixelBresenhamLineInCrop(
					cropProcessor,
					firstPoint.x,
					firstPoint.y,
					secondPoint.x,
					secondPoint.y,
					DEBUG_ARTIFACT_CANDIDATE_LINE_RGB,
					cropMinX,
					cropMinY);
		}

		if (selectedTriplet.isPresent()) {
			ArtifactBridgeTriplet triplet = selectedTriplet.get();
			Point firstPoint = triplet.getFirstBridgePoint();
			Point peakPoint = triplet.getBridgePeakPoint();
			Point secondPoint = triplet.getSecondBridgePoint();

			paintForwardBoundaryArcInCrop(
					cropProcessor,
					edgeBoundaryPixels,
					triplet.getFirstBridgeBoundaryIndex(),
					triplet.getSecondBridgeBoundaryIndex(),
					DEBUG_ARTIFACT_ARTIFACT_ARC_RGB,
					cropMinX,
					cropMinY);
			paintThickOnePixelBresenhamLineInCrop(
					cropProcessor,
					firstPoint.x,
					firstPoint.y,
					secondPoint.x,
					secondPoint.y,
					DEBUG_ARTIFACT_SELECTED_LINE_RGB,
					DEBUG_ARTIFACT_SELECTED_LINE_THICKNESS,
					cropMinX,
					cropMinY);
			paintSquareMarkerInCrop(
					cropProcessor,
					firstPoint.x,
					firstPoint.y,
					DEBUG_ARTIFACT_SQUARE_MARKER_RADIUS,
					DEBUG_ARTIFACT_FIRST_BRIDGE_RGB,
					cropMinX,
					cropMinY);
			paintEndpointRingInCrop(
					cropProcessor,
					peakPoint.x,
					peakPoint.y,
					DEBUG_ARTIFACT_BRIDGE_PEAK_RING_RADIUS,
					DEBUG_ARTIFACT_BRIDGE_PEAK_RGB,
					cropMinX,
					cropMinY);
			paintSquareMarkerInCrop(
					cropProcessor,
					secondPoint.x,
					secondPoint.y,
					DEBUG_ARTIFACT_SQUARE_MARKER_RADIUS,
					DEBUG_ARTIFACT_SECOND_BRIDGE_RGB,
					cropMinX,
					cropMinY);
		}

		int scaledWidth = cropWidth * DEBUG_RECONSTRUCTION_CROP_SCALE;
		int scaledHeight = cropHeight * DEBUG_RECONSTRUCTION_CROP_SCALE;
		ColorProcessor scaledProcessor =
				new ColorProcessor(scaledWidth, scaledHeight);

		for (int scaledY = 0; scaledY < scaledHeight; scaledY++) {

			for (int scaledX = 0; scaledX < scaledWidth; scaledX++) {
				scaledProcessor.putPixel(
						scaledX,
						scaledY,
						cropProcessor.get(
								scaledX / DEBUG_RECONSTRUCTION_CROP_SCALE,
								scaledY / DEBUG_RECONSTRUCTION_CROP_SCALE));
			}
		}

		ImagePlus artifactTripletsImage = new ImagePlus(
				"Artifact Bridge Triplets - Lumen " + DEBUG_LUMEN_ID,
				scaledProcessor);
		Overlay artifactLabels = new Overlay();

		if (selectedTriplet.isPresent()) {
			ArtifactBridgeTriplet triplet = selectedTriplet.get();
			addArtifactTripletLabel(
					artifactLabels,
					triplet.getFirstBridgePoint(),
					"A",
					DEBUG_ARTIFACT_FIRST_BRIDGE_RGB,
					cropMinX,
					cropMinY);
			addArtifactTripletLabel(
					artifactLabels,
					triplet.getBridgePeakPoint(),
					"P",
					DEBUG_ARTIFACT_BRIDGE_PEAK_RGB,
					cropMinX,
					cropMinY);
			addArtifactTripletLabel(
					artifactLabels,
					triplet.getSecondBridgePoint(),
					"B",
					DEBUG_ARTIFACT_SECOND_BRIDGE_RGB,
					cropMinX,
					cropMinY);
		} else {
			TextRoi noValidTripletLabel = new TextRoi(
					scaledWidth / 2 - 40,
					scaledHeight / 2 - 6,
					"NO VALID TRIPLET");
			noValidTripletLabel.setFont(DEBUG_ARTIFACT_LABEL_FONT);
			noValidTripletLabel.setStrokeColor(Color.WHITE);
			noValidTripletLabel.setFillColor(Color.BLACK);
			artifactLabels.add(noValidTripletLabel);
		}

		addArtifactLegendLine(
				artifactLabels,
				8,
				8,
				"cyan = pixel-edge contour");
		addArtifactLegendLine(
				artifactLabels,
				8,
				22,
				"red = accepted curvature peaks");
		addArtifactLegendLine(
				artifactLabels,
				8,
				36,
				"blue = selected A->P->B arc");
		addArtifactLegendLine(
				artifactLabels,
				8,
				50,
				"yellow = selected A-B closing line");

		artifactTripletsImage.setOverlay(artifactLabels);

		return artifactTripletsImage;
	}

	private static void paintClosedEdgeLoopInCrop(
			ColorProcessor cropProcessor,
			List<Point> edgeBoundaryPixels,
			int rgb,
			int cropMinX,
			int cropMinY) {

		if (edgeBoundaryPixels.isEmpty()) {
			return;
		}

		int vertexCount = edgeBoundaryPixels.size();

		for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
			Point currentVertex = edgeBoundaryPixels.get(vertexIndex);
			Point nextVertex = edgeBoundaryPixels.get(
					(vertexIndex + 1) % vertexCount);
			paintOnePixelBresenhamLineInCrop(
					cropProcessor,
					currentVertex.x,
					currentVertex.y,
					nextVertex.x,
					nextVertex.y,
					rgb,
					cropMinX,
					cropMinY);
		}
	}

	private static void paintTraceDebugCanonicalEdgesInCrop(
			ColorProcessor cropProcessor,
			PixelEdgeBoundaryTracer.TraceDebugReport traceDebugReport,
			int cropMinX,
			int cropMinY) {

		List<PixelEdgeBoundaryTracer.TraversalStepDiagnostic> steps =
				traceDebugReport.getTraversalSteps();
		int edgeLabelCount = 0;

		for (PixelEdgeBoundaryTracer.TraversalStepDiagnostic step : steps) {
			paintOnePixelBresenhamLineInCrop(
					cropProcessor,
					step.getCurrentEdgeFromX(),
					step.getCurrentEdgeFromY(),
					step.getCurrentEdgeToX(),
					step.getCurrentEdgeToY(),
					DEBUG_ARTIFACT_BOUNDARY_CYAN_RGB,
					cropMinX,
					cropMinY);

			if (edgeLabelCount < 25) {
				paintEdgeIdLabelInCrop(
						cropProcessor,
						step.getCurrentEdgeFromX(),
						step.getCurrentEdgeFromY(),
						step.getCurrentEdgeId(),
						cropMinX,
						cropMinY);
				edgeLabelCount++;
			}
		}

		if (!steps.isEmpty()) {
			PixelEdgeBoundaryTracer.TraversalStepDiagnostic failureStep =
					steps.get(steps.size() - 1);
			paintOnePixelBresenhamLineInCrop(
					cropProcessor,
					failureStep.getCurrentEdgeFromX(),
					failureStep.getCurrentEdgeFromY(),
					failureStep.getCurrentEdgeToX(),
					failureStep.getCurrentEdgeToY(),
					DEBUG_ARTIFACT_TRACE_CURRENT_EDGE_RGB,
					cropMinX,
					cropMinY);
		}
	}

	private static void paintTraceDebugFailureMarkersInCrop(
			ColorProcessor cropProcessor,
			PixelEdgeBoundaryTracer.TraceDebugReport traceDebugReport,
			int cropMinX,
			int cropMinY) {

		List<PixelEdgeBoundaryTracer.TraversalStepDiagnostic> steps =
				traceDebugReport.getTraversalSteps();

		if (steps.isEmpty()) {
			return;
		}

		PixelEdgeBoundaryTracer.TraversalStepDiagnostic failureStep =
				steps.get(steps.size() - 1);

		paintSquareMarkerInCrop(
				cropProcessor,
				failureStep.getCurrentVertexX(),
				failureStep.getCurrentVertexY(),
				DEBUG_ARTIFACT_DUPLICATE_MARKER_RADIUS,
				DEBUG_ARTIFACT_TRACE_FIRST_DUPLICATE_RGB,
				cropMinX,
				cropMinY);

		for (PixelEdgeBoundaryTracer.SuccessorCandidateDiagnostic candidate
				: failureStep.getSuccessorCandidates()) {

			if (!candidate.isEdgeExists()) {
				continue;
			}

			int candidateColor = DEBUG_ARTIFACT_TRACE_CANDIDATE_OTHER_RGB;

			if (candidate.isAlreadyUsed()) {
				candidateColor = DEBUG_ARTIFACT_TRACE_CANDIDATE_USED_RGB;
			} else if ("NONE".equals(candidate.getRejectedReason())
					&& candidate.isSelected()) {
				candidateColor = DEBUG_ARTIFACT_TRACE_SELECTED_SUCCESSOR_RGB;
			} else if ("NONE".equals(candidate.getRejectedReason())) {
				candidateColor = DEBUG_ARTIFACT_TRACE_CANDIDATE_VALID_RGB;
			}

			paintOnePixelBresenhamLineInCrop(
					cropProcessor,
					candidate.getCandidateFromX(),
					candidate.getCandidateFromY(),
					candidate.getCandidateToX(),
					candidate.getCandidateToY(),
					candidateColor,
					cropMinX,
					cropMinY);
		}
	}

	private static void paintDiagonalJunctionTransitionsInCrop(
			ColorProcessor cropProcessor,
			PixelEdgeBoundaryTracer.TraceDebugReport traceDebugReport,
			int cropMinX,
			int cropMinY) {

		for (PixelEdgeBoundaryTracer.TraversalStepDiagnostic step
				: traceDebugReport.getTraversalSteps()) {

			if (!step.isDiagonalJunctionRuleApplied()
					|| !PixelEdgeBoundaryTracer.SELECTION_REASON_DIAGONAL_JUNCTION_MAPPING
							.equals(step.getSelectionReason())) {
				continue;
			}

			paintSquareMarkerInCrop(
					cropProcessor,
					step.getCurrentVertexX(),
					step.getCurrentVertexY(),
					DEBUG_ARTIFACT_SQUARE_MARKER_RADIUS,
					DEBUG_ARTIFACT_TRACE_FIRST_DUPLICATE_RGB,
					cropMinX,
					cropMinY);
		}
	}

	private static void addDiagonalJunctionOverlayLabels(
			Overlay artifactLabels,
			PixelEdgeBoundaryTracer.TraceDebugReport traceDebugReport,
			int cropMinX,
			int cropMinY) {

		for (PixelEdgeBoundaryTracer.TraversalStepDiagnostic step
				: traceDebugReport.getTraversalSteps()) {

			if (!step.isDiagonalJunctionRuleApplied()
					|| !PixelEdgeBoundaryTracer.SELECTION_REASON_DIAGONAL_JUNCTION_MAPPING
							.equals(step.getSelectionReason())) {
				continue;
			}

			int scaledX = (step.getCurrentVertexX() - cropMinX)
					* DEBUG_RECONSTRUCTION_CROP_SCALE;
			int scaledY = (step.getCurrentVertexY() - cropMinY)
					* DEBUG_RECONSTRUCTION_CROP_SCALE;
			TextRoi joinLabel = new TextRoi(
					scaledX + 4,
					scaledY - 18,
					"DIAGONAL JOIN\n"
							+ step.getCurrentEdgeId()
							+ " -> "
							+ step.getSelectedSuccessorEdgeId());
			joinLabel.setFont(DEBUG_ARTIFACT_LABEL_FONT);
			joinLabel.setStrokeColor(Color.WHITE);
			joinLabel.setFillColor(Color.BLACK);
			artifactLabels.add(joinLabel);
		}
	}

	private static void paintVertexTopologyInsetInCrop(
			ColorProcessor cropProcessor,
			Lumen lumen,
			PixelEdgeBoundaryTracer.TraceDebugReport traceDebugReport,
			int cropMinX,
			int cropMinY) {

		PixelEdgeBoundaryTracer.VertexTopologyDiagnostic topology =
				traceDebugReport.getVertexTopologyDiagnostic();

		if (topology == null) {
			return;
		}

		Point vertex = topology.getVertex();
		int insetPixelSize = DEBUG_TOPOLOGY_INSET_RADIUS * 2 + 1;
		int insetWidth = insetPixelSize * DEBUG_TOPOLOGY_INSET_PIXEL_SCALE;
		int insetHeight = insetPixelSize * DEBUG_TOPOLOGY_INSET_PIXEL_SCALE;
		int insetOriginX = cropProcessor.getWidth() - insetWidth - 4;
		int insetOriginY = cropProcessor.getHeight() - insetHeight - 4;

		if (insetOriginX < 0 || insetOriginY < 0) {
			return;
		}

		Set<String> lumenPixelKeys = new HashSet<>();
		for (Point lumenPixel : lumen.getPixels()) {
			lumenPixelKeys.add(lumenPixel.x + "," + lumenPixel.y);
		}

		for (int insetY = 0; insetY < insetPixelSize; insetY++) {
			for (int insetX = 0; insetX < insetPixelSize; insetX++) {
				int imageX = vertex.x - DEBUG_TOPOLOGY_INSET_RADIUS + insetX;
				int imageY = vertex.y - DEBUG_TOPOLOGY_INSET_RADIUS + insetY;
				int pixelRgb = lumenPixelKeys.contains(imageX + "," + imageY)
						? DEBUG_TOPOLOGY_INSET_LUMEN_RGB
						: DEBUG_TOPOLOGY_INSET_BACKGROUND_RGB;

				for (int scaleY = 0; scaleY < DEBUG_TOPOLOGY_INSET_PIXEL_SCALE; scaleY++) {
					for (int scaleX = 0; scaleX < DEBUG_TOPOLOGY_INSET_PIXEL_SCALE; scaleX++) {
						cropProcessor.putPixel(
								insetOriginX
										+ insetX * DEBUG_TOPOLOGY_INSET_PIXEL_SCALE
										+ scaleX,
								insetOriginY
										+ insetY * DEBUG_TOPOLOGY_INSET_PIXEL_SCALE
										+ scaleY,
								pixelRgb);
					}
				}
			}
		}

		PixelEdgeBoundaryTracer.RepeatedVertexEdgeAnalysis analysis =
				traceDebugReport.getRepeatedVertexEdgeAnalysis();
		Map<Integer, Integer> edgeColorById = new HashMap<>();

		if (analysis != null) {
			edgeColorById.put(
					analysis.getFirstIncomingEdgeId(),
					DEBUG_ARTIFACT_BOUNDARY_CYAN_RGB);
			edgeColorById.put(
					analysis.getFirstOutgoingEdgeId(),
					DEBUG_ARTIFACT_TRACE_CURRENT_EDGE_RGB);
			edgeColorById.put(
					analysis.getRepeatedIncomingEdgeId(),
					DEBUG_ARTIFACT_TRACE_SELECTED_SUCCESSOR_RGB);

			for (Integer unusedEdgeId : analysis.getUnusedOutgoingEdgeIds()) {
				edgeColorById.put(unusedEdgeId, DEBUG_ARTIFACT_TRACE_CANDIDATE_VALID_RGB);
			}
		}

		for (PixelEdgeBoundaryTracer.EdgeFaceDiagnostic face
				: topology.getEdgeFaceDiagnostics()) {
			Integer edgeColor = edgeColorById.get(face.getEdgeId());

			if (edgeColor == null) {
				continue;
			}

			paintTopologyInsetEdgeLine(
					cropProcessor,
					face.getFromX(),
					face.getFromY(),
					face.getToX(),
					face.getToY(),
					vertex,
					insetOriginX,
					insetOriginY,
					edgeColor);
			paintEdgeIdLabelInCrop(
					cropProcessor,
					face.getFromX(),
					face.getFromY(),
					face.getEdgeId(),
					cropMinX,
					cropMinY);
		}

		paintTopologyInsetVertexMarker(
				cropProcessor,
				vertex,
				insetOriginX,
				insetOriginY);
	}

	private static void paintTopologyInsetEdgeLine(
			ColorProcessor cropProcessor,
			int fromX,
			int fromY,
			int toX,
			int toY,
			Point vertex,
			int insetOriginX,
			int insetOriginY,
			int rgb) {

		int startInsetX = (fromX - vertex.x + DEBUG_TOPOLOGY_INSET_RADIUS)
				* DEBUG_TOPOLOGY_INSET_PIXEL_SCALE
				+ DEBUG_TOPOLOGY_INSET_PIXEL_SCALE / 2;
		int startInsetY = (fromY - vertex.y + DEBUG_TOPOLOGY_INSET_RADIUS)
				* DEBUG_TOPOLOGY_INSET_PIXEL_SCALE
				+ DEBUG_TOPOLOGY_INSET_PIXEL_SCALE / 2;
		int endInsetX = (toX - vertex.x + DEBUG_TOPOLOGY_INSET_RADIUS)
				* DEBUG_TOPOLOGY_INSET_PIXEL_SCALE
				+ DEBUG_TOPOLOGY_INSET_PIXEL_SCALE / 2;
		int endInsetY = (toY - vertex.y + DEBUG_TOPOLOGY_INSET_RADIUS)
				* DEBUG_TOPOLOGY_INSET_PIXEL_SCALE
				+ DEBUG_TOPOLOGY_INSET_PIXEL_SCALE / 2;

		paintLineInInset(
				cropProcessor,
				insetOriginX + startInsetX,
				insetOriginY + startInsetY,
				insetOriginX + endInsetX,
				insetOriginY + endInsetY,
				rgb);
	}

	private static void paintTopologyInsetVertexMarker(
			ColorProcessor cropProcessor,
			Point vertex,
			int insetOriginX,
			int insetOriginY) {

		int centerX = insetOriginX
				+ DEBUG_TOPOLOGY_INSET_RADIUS * DEBUG_TOPOLOGY_INSET_PIXEL_SCALE
				+ DEBUG_TOPOLOGY_INSET_PIXEL_SCALE / 2;
		int centerY = insetOriginY
				+ DEBUG_TOPOLOGY_INSET_RADIUS * DEBUG_TOPOLOGY_INSET_PIXEL_SCALE
				+ DEBUG_TOPOLOGY_INSET_PIXEL_SCALE / 2;

		for (int deltaY = -1; deltaY <= 1; deltaY++) {
			for (int deltaX = -1; deltaX <= 1; deltaX++) {
				int x = centerX + deltaX;
				int y = centerY + deltaY;

				if (x >= 0
						&& y >= 0
						&& x < cropProcessor.getWidth()
						&& y < cropProcessor.getHeight()) {
					cropProcessor.putPixel(x, y, DEBUG_ARTIFACT_TRACE_FIRST_DUPLICATE_RGB);
				}
			}
		}
	}

	private static void paintLineInInset(
			ColorProcessor cropProcessor,
			int x0,
			int y0,
			int x1,
			int y1,
			int rgb) {

		int deltaX = Math.abs(x1 - x0);
		int deltaY = Math.abs(y1 - y0);
		int stepX = x0 < x1 ? 1 : -1;
		int stepY = y0 < y1 ? 1 : -1;
		int error = deltaX - deltaY;
		int x = x0;
		int y = y0;

		while (true) {
			if (x >= 0
					&& y >= 0
					&& x < cropProcessor.getWidth()
					&& y < cropProcessor.getHeight()) {
				cropProcessor.putPixel(x, y, rgb);
			}

			if (x == x1 && y == y1) {
				break;
			}

			int doubledError = 2 * error;

			if (doubledError > -deltaY) {
				error -= deltaY;
				x += stepX;
			}

			if (doubledError < deltaX) {
				error += deltaX;
				y += stepY;
			}
		}
	}

	private static void paintEdgeIdLabelInCrop(
			ColorProcessor cropProcessor,
			int x,
			int y,
			int edgeId,
			int cropMinX,
			int cropMinY) {

		int cropX = x - cropMinX;
		int cropY = y - cropMinY;

		if (cropX < 0 || cropY < 0
				|| cropX >= cropProcessor.getWidth()
				|| cropY >= cropProcessor.getHeight()) {
			return;
		}

		cropProcessor.drawString(String.valueOf(edgeId), cropX, cropY);
	}

	private static void addArtifactLegendLine(
			Overlay artifactLabels,
			int x,
			int y,
			String text) {

		TextRoi legendLine = new TextRoi(x, y, text);
		legendLine.setFont(DEBUG_ARTIFACT_LABEL_FONT);
		legendLine.setStrokeColor(Color.WHITE);
		legendLine.setFillColor(Color.BLACK);
		artifactLabels.add(legendLine);
	}

	private static void addArtifactTripletLabel(
			Overlay artifactLabels,
			Point markerPoint,
			String label,
			int labelColorRgb,
			int cropMinX,
			int cropMinY) {

		int scaledX =
				(markerPoint.x - cropMinX) * DEBUG_RECONSTRUCTION_CROP_SCALE
						+ DEBUG_RECONSTRUCTION_CROP_SCALE;
		int scaledY =
				(markerPoint.y - cropMinY) * DEBUG_RECONSTRUCTION_CROP_SCALE
						- DEBUG_RECONSTRUCTION_CROP_SCALE;
		TextRoi labelRoi = new TextRoi(scaledX, scaledY, label);
		labelRoi.setFont(DEBUG_ARTIFACT_LABEL_FONT);
		labelRoi.setStrokeColor(colorFromPackedRgb(labelColorRgb));
		labelRoi.setFillColor(Color.BLACK);
		artifactLabels.add(labelRoi);
	}

	private static void paintForwardBoundaryArcInCrop(
			ColorProcessor cropProcessor,
			List<Point> boundaryPixels,
			int fromBoundaryIndex,
			int toBoundaryIndex,
			int rgb,
			int cropMinX,
			int cropMinY) {

		int boundarySize = boundaryPixels.size();
		int currentBoundaryIndex = fromBoundaryIndex;

		while (true) {
			Point boundaryPoint = boundaryPixels.get(currentBoundaryIndex);
			int cropX = boundaryPoint.x - cropMinX;
			int cropY = boundaryPoint.y - cropMinY;

			if (cropX >= 0
					&& cropY >= 0
					&& cropX < cropProcessor.getWidth()
					&& cropY < cropProcessor.getHeight()) {
				cropProcessor.putPixel(cropX, cropY, rgb);
			}

			if (currentBoundaryIndex == toBoundaryIndex) {
				break;
			}

			currentBoundaryIndex = (currentBoundaryIndex + 1) % boundarySize;
		}
	}

	private static void paintThickOnePixelBresenhamLineInCrop(
			ColorProcessor cropProcessor,
			int startX,
			int startY,
			int endX,
			int endY,
			int rgb,
			int thickness,
			int cropMinX,
			int cropMinY) {

		for (int offsetY = -(thickness - 1) / 2;
				offsetY <= (thickness - 1) / 2;
				offsetY++) {

			for (int offsetX = -(thickness - 1) / 2;
					offsetX <= (thickness - 1) / 2;
					offsetX++) {
				paintOnePixelBresenhamLineInCrop(
						cropProcessor,
						startX + offsetX,
						startY + offsetY,
						endX + offsetX,
						endY + offsetY,
						rgb,
						cropMinX,
						cropMinY);
			}
		}
	}

	private static void paintSquareMarkerInCrop(
			ColorProcessor cropProcessor,
			int centerX,
			int centerY,
			int markerRadius,
			int rgb,
			int cropMinX,
			int cropMinY) {

		int cropWidth = cropProcessor.getWidth();
		int cropHeight = cropProcessor.getHeight();

		for (int deltaY = -markerRadius; deltaY <= markerRadius; deltaY++) {

			for (int deltaX = -markerRadius; deltaX <= markerRadius; deltaX++) {
				int cropX = centerX + deltaX - cropMinX;
				int cropY = centerY + deltaY - cropMinY;

				if (cropX < 0
						|| cropY < 0
						|| cropX >= cropWidth
						|| cropY >= cropHeight) {
					continue;
				}

				cropProcessor.putPixel(cropX, cropY, rgb);
			}
		}
	}

	private static ImagePlus createDebugBridgeCandidatesImage(
			Lumen lumen,
			List<Point> boundaryPixels,
			List<BridgeCandidate> bridgeCandidates,
			Optional<BridgeCandidate> selectedBridge,
			int imageWidth,
			int imageHeight) {

		Rectangle boundingBox = lumen.getBoundingBox();
		int cropMinX = Math.max(
				0,
				boundingBox.x - DEBUG_RECONSTRUCTION_CROP_MARGIN);
		int cropMinY = Math.max(
				0,
				boundingBox.y - DEBUG_RECONSTRUCTION_CROP_MARGIN);
		int cropMaxX = Math.min(
				imageWidth,
				boundingBox.x + boundingBox.width + DEBUG_RECONSTRUCTION_CROP_MARGIN);
		int cropMaxY = Math.min(
				imageHeight,
				boundingBox.y + boundingBox.height + DEBUG_RECONSTRUCTION_CROP_MARGIN);
		int cropWidth = cropMaxX - cropMinX;
		int cropHeight = cropMaxY - cropMinY;

		if (cropWidth <= 0 || cropHeight <= 0) {
			return null;
		}

		ColorProcessor cropProcessor = new ColorProcessor(cropWidth, cropHeight);
		cropProcessor.setColor(
				new Color(
						GROWTH_PREVIEW_BACKGROUND_GRAY,
						GROWTH_PREVIEW_BACKGROUND_GRAY,
						GROWTH_PREVIEW_BACKGROUND_GRAY));
		cropProcessor.fill();

		paintPreviewPixelsInCrop(
				cropProcessor,
				lumen.getPixels(),
				GROWTH_PREVIEW_ORIGINAL_GRAY_RGB,
				cropMinX,
				cropMinY);
		paintPreviewPixelsInCrop(
				cropProcessor,
				boundaryPixels,
				DEBUG_BRIDGE_CANDIDATE_BOUNDARY_CYAN_RGB,
				cropMinX,
				cropMinY);

		for (int candidateIndex = 0;
				candidateIndex < bridgeCandidates.size();
				candidateIndex++) {

			BridgeCandidate candidate = bridgeCandidates.get(candidateIndex);
			Point firstPoint = candidate.getFirstPoint();
			Point secondPoint = candidate.getSecondPoint();
			int lineColor = DEBUG_BRIDGE_CANDIDATE_LINE_COLORS[
					candidateIndex % DEBUG_BRIDGE_CANDIDATE_LINE_COLORS.length];

			paintOnePixelBresenhamLineInCrop(
					cropProcessor,
					firstPoint.x,
					firstPoint.y,
					secondPoint.x,
					secondPoint.y,
					lineColor,
					cropMinX,
					cropMinY);
		}

		if (selectedBridge.isPresent()) {
			BridgeCandidate bridge = selectedBridge.get();
			Point firstPoint = bridge.getFirstPoint();
			Point secondPoint = bridge.getSecondPoint();

			paintEndpointRingInCrop(
					cropProcessor,
					firstPoint.x,
					firstPoint.y,
					DEBUG_SELECTED_BRIDGE_ENDPOINT_RING_RADIUS,
					GROWTH_PREVIEW_ENDPOINT_WHITE_RGB,
					cropMinX,
					cropMinY);
			paintEndpointRingInCrop(
					cropProcessor,
					secondPoint.x,
					secondPoint.y,
					DEBUG_SELECTED_BRIDGE_ENDPOINT_RING_RADIUS,
					GROWTH_PREVIEW_ENDPOINT_WHITE_RGB,
					cropMinX,
					cropMinY);
		}

		int scaledWidth = cropWidth * DEBUG_RECONSTRUCTION_CROP_SCALE;
		int scaledHeight = cropHeight * DEBUG_RECONSTRUCTION_CROP_SCALE;
		ColorProcessor scaledProcessor =
				new ColorProcessor(scaledWidth, scaledHeight);

		for (int scaledY = 0; scaledY < scaledHeight; scaledY++) {

			for (int scaledX = 0; scaledX < scaledWidth; scaledX++) {
				scaledProcessor.putPixel(
						scaledX,
						scaledY,
						cropProcessor.get(
								scaledX / DEBUG_RECONSTRUCTION_CROP_SCALE,
								scaledY / DEBUG_RECONSTRUCTION_CROP_SCALE));
			}
		}

		ImagePlus bridgeCandidatesImage = new ImagePlus(
				"Legacy Bridge Candidates - Lumen " + DEBUG_LUMEN_ID,
				scaledProcessor);
		Overlay candidateLabels = new Overlay();

		TextRoi legacyOnlyLabel = new TextRoi(
				8,
				8,
				"LEGACY DIAGNOSTIC ONLY");
		legacyOnlyLabel.setFont(DEBUG_BRIDGE_CANDIDATE_LABEL_FONT);
		legacyOnlyLabel.setStrokeColor(Color.WHITE);
		legacyOnlyLabel.setFillColor(Color.BLACK);
		candidateLabels.add(legacyOnlyLabel);

		TextRoi legacyNotUsedLabel = new TextRoi(
				8,
				24,
				"NOT USED FOR RECONSTRUCTION");
		legacyNotUsedLabel.setFont(DEBUG_BRIDGE_CANDIDATE_LABEL_FONT);
		legacyNotUsedLabel.setStrokeColor(Color.WHITE);
		legacyNotUsedLabel.setFillColor(Color.BLACK);
		candidateLabels.add(legacyNotUsedLabel);

		for (int candidateIndex = 0;
				candidateIndex < bridgeCandidates.size();
				candidateIndex++) {

			BridgeCandidate candidate = bridgeCandidates.get(candidateIndex);
			Point firstPoint = candidate.getFirstPoint();
			Point secondPoint = candidate.getSecondPoint();
			int midpointX = (firstPoint.x + secondPoint.x) / 2;
			int midpointY = (firstPoint.y + secondPoint.y) / 2;
			int scaledMidpointX =
					(midpointX - cropMinX) * DEBUG_RECONSTRUCTION_CROP_SCALE;
			int scaledMidpointY =
					(midpointY - cropMinY) * DEBUG_RECONSTRUCTION_CROP_SCALE;
			int lineColor = DEBUG_BRIDGE_CANDIDATE_LINE_COLORS[
					candidateIndex % DEBUG_BRIDGE_CANDIDATE_LINE_COLORS.length];
			TextRoi candidateLabel = new TextRoi(
					scaledMidpointX,
					scaledMidpointY,
					String.valueOf(candidateIndex + 1));
			candidateLabel.setFont(DEBUG_BRIDGE_CANDIDATE_LABEL_FONT);
			candidateLabel.setStrokeColor(colorFromPackedRgb(lineColor));
			candidateLabel.setFillColor(Color.BLACK);
			candidateLabels.add(candidateLabel);
		}

		bridgeCandidatesImage.setOverlay(candidateLabels);

		return bridgeCandidatesImage;
	}

	private static void paintPreviewPixelsInCrop(
			ColorProcessor cropProcessor,
			List<Point> pixels,
			int rgb,
			int cropMinX,
			int cropMinY) {

		int cropWidth = cropProcessor.getWidth();
		int cropHeight = cropProcessor.getHeight();

		for (Point pixel : pixels) {
			int cropX = pixel.x - cropMinX;
			int cropY = pixel.y - cropMinY;

			if (cropX < 0
					|| cropY < 0
					|| cropX >= cropWidth
					|| cropY >= cropHeight) {
				continue;
			}

			cropProcessor.putPixel(cropX, cropY, rgb);
		}
	}

	private static void paintOnePixelBresenhamLineInCrop(
			ColorProcessor cropProcessor,
			int startX,
			int startY,
			int endX,
			int endY,
			int rgb,
			int cropMinX,
			int cropMinY) {

		int deltaX = Math.abs(endX - startX);
		int deltaY = Math.abs(endY - startY);
		int stepX = startX < endX ? 1 : -1;
		int stepY = startY < endY ? 1 : -1;
		int error = deltaX - deltaY;
		int currentX = startX;
		int currentY = startY;
		int cropWidth = cropProcessor.getWidth();
		int cropHeight = cropProcessor.getHeight();

		while (true) {
			int cropX = currentX - cropMinX;
			int cropY = currentY - cropMinY;

			if (cropX >= 0
					&& cropY >= 0
					&& cropX < cropWidth
					&& cropY < cropHeight) {
				cropProcessor.putPixel(cropX, cropY, rgb);
			}

			if (currentX == endX && currentY == endY) {
				break;
			}

			int doubledError = 2 * error;

			if (doubledError > -deltaY) {
				error -= deltaY;
				currentX += stepX;
			}

			if (doubledError < deltaX) {
				error += deltaX;
				currentY += stepY;
			}
		}
	}

	private static void paintEndpointRingInCrop(
			ColorProcessor cropProcessor,
			int centerX,
			int centerY,
			int ringRadius,
			int rgb,
			int cropMinX,
			int cropMinY) {

		int cropWidth = cropProcessor.getWidth();
		int cropHeight = cropProcessor.getHeight();
		int innerRadiusSquared = (ringRadius - 1) * (ringRadius - 1);
		int outerRadiusSquared = (ringRadius + 1) * (ringRadius + 1);

		for (int deltaY = -ringRadius - 1; deltaY <= ringRadius + 1; deltaY++) {

			for (int deltaX = -ringRadius - 1; deltaX <= ringRadius + 1; deltaX++) {
				int distanceSquared = deltaX * deltaX + deltaY * deltaY;

				if (distanceSquared < innerRadiusSquared
						|| distanceSquared > outerRadiusSquared) {
					continue;
				}

				int cropX = centerX + deltaX - cropMinX;
				int cropY = centerY + deltaY - cropMinY;

				if (cropX < 0
						|| cropY < 0
						|| cropX >= cropWidth
						|| cropY >= cropHeight) {
					continue;
				}

				cropProcessor.putPixel(cropX, cropY, rgb);
			}
		}
	}

	private static Color colorFromPackedRgb(int rgb) {

		return new Color(
				(rgb >> 16) & 0xFF,
				(rgb >> 8) & 0xFF,
				rgb & 0xFF);
	}

	private static ImagePlus createDebugReconstructionCrop(
			ColorProcessor growthPreviewProcessor,
			Lumen lumen,
			int imageWidth,
			int imageHeight) {

		Rectangle boundingBox = lumen.getBoundingBox();
		int cropMinX = Math.max(0, boundingBox.x - DEBUG_RECONSTRUCTION_CROP_MARGIN);
		int cropMinY = Math.max(0, boundingBox.y - DEBUG_RECONSTRUCTION_CROP_MARGIN);
		int cropMaxX = Math.min(
				imageWidth,
				boundingBox.x + boundingBox.width + DEBUG_RECONSTRUCTION_CROP_MARGIN);
		int cropMaxY = Math.min(
				imageHeight,
				boundingBox.y + boundingBox.height + DEBUG_RECONSTRUCTION_CROP_MARGIN);
		int cropWidth = cropMaxX - cropMinX;
		int cropHeight = cropMaxY - cropMinY;

		if (cropWidth <= 0 || cropHeight <= 0) {
			return null;
		}

		ColorProcessor cropProcessor = new ColorProcessor(cropWidth, cropHeight);
		cropProcessor.setColor(
				new Color(
						GROWTH_PREVIEW_BACKGROUND_GRAY,
						GROWTH_PREVIEW_BACKGROUND_GRAY,
						GROWTH_PREVIEW_BACKGROUND_GRAY));
		cropProcessor.fill();

		for (int y = 0; y < cropHeight; y++) {

			for (int x = 0; x < cropWidth; x++) {
				cropProcessor.putPixel(
						x,
						y,
						growthPreviewProcessor.get(cropMinX + x, cropMinY + y));
			}
		}

		int scaledWidth = cropWidth * DEBUG_RECONSTRUCTION_CROP_SCALE;
		int scaledHeight = cropHeight * DEBUG_RECONSTRUCTION_CROP_SCALE;
		ColorProcessor scaledProcessor =
				new ColorProcessor(scaledWidth, scaledHeight);

		for (int scaledY = 0; scaledY < scaledHeight; scaledY++) {

			for (int scaledX = 0; scaledX < scaledWidth; scaledX++) {
				scaledProcessor.putPixel(
						scaledX,
						scaledY,
						cropProcessor.get(
								scaledX / DEBUG_RECONSTRUCTION_CROP_SCALE,
								scaledY / DEBUG_RECONSTRUCTION_CROP_SCALE));
			}
		}

		return new ImagePlus(
				"Single-Pixel Reconstruction Debug Crop",
				scaledProcessor);
	}

	private static String formatPixelCollectionBounds(List<Point> pixels) {

		if (pixels == null || pixels.isEmpty()) {
			return "NONE";
		}

		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;

		for (Point pixel : pixels) {
			minX = Math.min(minX, pixel.x);
			minY = Math.min(minY, pixel.y);
			maxX = Math.max(maxX, pixel.x);
			maxY = Math.max(maxY, pixel.y);
		}

		return "x="
				+ minX
				+ ".."
				+ maxX
				+ ", y="
				+ minY
				+ ".."
				+ maxY;
	}

	private static Point findLastPixelAddedAtBestStep(
			SinglePixelReconstructionResult reconstructionResult,
			Set<Point> enclosedWoodPixels) {

		int bestStep = reconstructionResult.getBestReconstructionStep();

		if (bestStep <= 0) {
			return null;
		}

		Set<Long> currentLumenKeys = toCoordinateKeySet(
				reconstructionResult.getOpenedLumenPixels());
		Set<Long> removedBridgeKeys = toCoordinateKeySet(
				reconstructionResult.getDirectlyRemovedBridgePixels());
		Set<Long> enclosedWoodKeys = toCoordinateKeySet(enclosedWoodPixels);
		Set<Long> addedWoodKeys = new HashSet<>();
		Point lastPixelAddedAtBestStep = null;

		for (int step = 1; step <= bestStep; step++) {
			Long candidateKey = findDebugReconstructionCandidate(
					currentLumenKeys,
					removedBridgeKeys,
					enclosedWoodKeys,
					addedWoodKeys);

			if (candidateKey == null) {
				break;
			}

			currentLumenKeys.add(candidateKey);
			addedWoodKeys.add(candidateKey);

			if (step == bestStep) {
				lastPixelAddedAtBestStep = decodeDebugCoordinate(candidateKey);
			}
		}

		return lastPixelAddedAtBestStep;
	}

	private static Long findDebugReconstructionCandidate(
			Set<Long> currentLumenKeys,
			Set<Long> removedBridgeKeys,
			Set<Long> enclosedWoodKeys,
			Set<Long> addedWoodKeys) {

		Long bestCandidateKey = null;
		Point bestCandidatePoint = null;

		for (long woodKey : enclosedWoodKeys) {

			if (addedWoodKeys.contains(woodKey)) {
				continue;
			}

			Point woodPoint = decodeDebugCoordinate(woodKey);

			if (!isDebugReconstructionCandidateAdjacent(
					woodPoint,
					currentLumenKeys,
					removedBridgeKeys,
					addedWoodKeys)) {
				continue;
			}

			if (bestCandidatePoint == null
					|| compareDebugPoints(woodPoint, bestCandidatePoint) < 0) {
				bestCandidatePoint = woodPoint;
				bestCandidateKey = woodKey;
			}
		}

		return bestCandidateKey;
	}

	private static boolean isDebugReconstructionCandidateAdjacent(
			Point woodPoint,
			Set<Long> currentLumenKeys,
			Set<Long> removedBridgeKeys,
			Set<Long> addedWoodKeys) {

		if (isEightAdjacentToCoordinateSet(woodPoint, currentLumenKeys)) {
			return true;
		}

		if (addedWoodKeys.isEmpty()
				&& isEightAdjacentToCoordinateSet(woodPoint, removedBridgeKeys)) {
			return true;
		}

		return false;
	}

	private static boolean isEightAdjacentToCoordinateSet(
			Point point,
			Set<Long> targetKeys) {

		for (long targetKey : targetKeys) {
			Point targetPoint = decodeDebugCoordinate(targetKey);
			int deltaX = Math.abs(point.x - targetPoint.x);
			int deltaY = Math.abs(point.y - targetPoint.y);

			if (deltaX <= 1 && deltaY <= 1 && (deltaX != 0 || deltaY != 0)) {
				return true;
			}
		}

		return false;
	}

	private static int compareDebugPoints(Point left, Point right) {

		int byY = Integer.compare(left.y, right.y);

		if (byY != 0) {
			return byY;
		}

		return Integer.compare(left.x, right.x);
	}

	private static Set<Long> toCoordinateKeySet(Iterable<Point> pixels) {

		Set<Long> keys = new HashSet<>();

		for (Point pixel : pixels) {
			keys.add(encodeDebugCoordinate(pixel.x, pixel.y));
		}

		return keys;
	}

	private static long encodeDebugCoordinate(int x, int y) {
		return (((long) y) << 32) | (x & 0xFFFFFFFFL);
	}

	private static Point decodeDebugCoordinate(long key) {
		int x = (int) key;
		int y = (int) (key >> 32);
		return new Point(x, y);
	}

	private static SinglePixelReconstructionResult createUnchangedReconstructionResult(
			List<Point> lumenForegroundPixels,
			double originalFeatureCircularity) {

		return new SinglePixelReconstructionResult(
				new Point(0, 0),
				new Point(0, 0),
				new Point(0, 0),
				lumenForegroundPixels,
				lumenForegroundPixels,
				lumenForegroundPixels,
				Collections.emptyList(),
				Collections.emptyList(),
				Collections.emptyList(),
				originalFeatureCircularity,
				originalFeatureCircularity,
				0,
				0,
				false,
				false);
	}

	private static String determineReconstructionStopReason(
			SinglePixelReconstructionResult reconstructionResult) {

		if (reconstructionResult.isStoppedBecauseNoCandidate()) {
			return "NO_CANDIDATE";
		}

		if (reconstructionResult.isStoppedBecausePatienceExhausted()) {
			return "PATIENCE";
		}

		if (reconstructionResult.getPerformedReconstructionSteps()
				>= MAXIMUM_RECONSTRUCTION_STEPS) {
			return "MAXIMUM_STEPS";
		}

		return "NO_CANDIDATE";
	}

	private static double circularityForLumen(
			LumenFilterDiagnostics filterDiagnostics,
			int lumenId) {

		for (LumenFeatures features : filterDiagnostics.lumenFeatures) {

			if (features.getLumenId() == lumenId) {
				return features.getCircularity();
			}
		}

		return 0.0;
	}

	private static Set<Point> collectAllowedWoodPixels(
			ImageProcessor classifiedImageProcessor,
			Lumen lumen,
			int searchMargin) {

		Rectangle boundingBox = lumen.getBoundingBox();
		int minX = Math.max(
				0,
				boundingBox.x - searchMargin);
		int minY = Math.max(
				0,
				boundingBox.y - searchMargin);
		int maxX = Math.min(
				classifiedImageProcessor.getWidth(),
				boundingBox.x + boundingBox.width + searchMargin);
		int maxY = Math.min(
				classifiedImageProcessor.getHeight(),
				boundingBox.y + boundingBox.height + searchMargin);
		Set<Point> allowedWoodPixels = new HashSet<>();

		for (int y = minY; y < maxY; y++) {

			for (int x = minX; x < maxX; x++) {

				if (classifiedImageProcessor.get(x, y) == CLASSIFIED_WOOD) {
					allowedWoodPixels.add(new Point(x, y));
				}
			}
		}

		return allowedWoodPixels;
	}

	private static void paintSeedMarker(
			ColorProcessor growthPreviewProcessor,
			int centerX,
			int centerY) {

		int imageWidth = growthPreviewProcessor.getWidth();
		int imageHeight = growthPreviewProcessor.getHeight();

		for (int deltaY = -GROWTH_PREVIEW_SEED_MARKER_RADIUS;
				deltaY <= GROWTH_PREVIEW_SEED_MARKER_RADIUS;
				deltaY++) {

			int pixelY = centerY + deltaY;

			if (pixelY < 0 || pixelY >= imageHeight) {
				continue;
			}

			for (int deltaX = -GROWTH_PREVIEW_SEED_MARKER_RADIUS;
					deltaX <= GROWTH_PREVIEW_SEED_MARKER_RADIUS;
					deltaX++) {

				int pixelX = centerX + deltaX;

				if (pixelX < 0 || pixelX >= imageWidth) {
					continue;
				}

				growthPreviewProcessor.putPixel(
						pixelX,
						pixelY,
						GROWTH_PREVIEW_SEED_MAGENTA_RGB);
			}
		}
	}

	private static void paintEndpointRing(
			ColorProcessor growthPreviewProcessor,
			int centerX,
			int centerY) {

		int imageWidth = growthPreviewProcessor.getWidth();
		int imageHeight = growthPreviewProcessor.getHeight();
		int innerRadiusSquared =
				(GROWTH_PREVIEW_ENDPOINT_RING_RADIUS - 1)
						* (GROWTH_PREVIEW_ENDPOINT_RING_RADIUS - 1);
		int outerRadiusSquared =
				(GROWTH_PREVIEW_ENDPOINT_RING_RADIUS + 1)
						* (GROWTH_PREVIEW_ENDPOINT_RING_RADIUS + 1);

		for (int deltaY = -GROWTH_PREVIEW_ENDPOINT_RING_RADIUS - 1;
				deltaY <= GROWTH_PREVIEW_ENDPOINT_RING_RADIUS + 1;
				deltaY++) {

			int pixelY = centerY + deltaY;

			if (pixelY < 0 || pixelY >= imageHeight) {
				continue;
			}

			for (int deltaX = -GROWTH_PREVIEW_ENDPOINT_RING_RADIUS - 1;
					deltaX <= GROWTH_PREVIEW_ENDPOINT_RING_RADIUS + 1;
					deltaX++) {

				int pixelX = centerX + deltaX;

				if (pixelX < 0 || pixelX >= imageWidth) {
					continue;
				}

				int distanceSquared = deltaX * deltaX + deltaY * deltaY;

				if (distanceSquared >= innerRadiusSquared
						&& distanceSquared <= outerRadiusSquared) {
					growthPreviewProcessor.putPixel(
							pixelX,
							pixelY,
							GROWTH_PREVIEW_ENDPOINT_WHITE_RGB);
				}
			}
		}
	}

	private static void paintPreviewPixels(
			ColorProcessor growthPreviewProcessor,
			List<Point> pixels,
			int rgb) {

		int imageWidth = growthPreviewProcessor.getWidth();
		int imageHeight = growthPreviewProcessor.getHeight();

		for (Point pixel : pixels) {

			if (pixel.x < 0
					|| pixel.y < 0
					|| pixel.x >= imageWidth
					|| pixel.y >= imageHeight) {
				continue;
			}

				growthPreviewProcessor.putPixel(pixel.x, pixel.y, rgb);
		}
	}

	private static List<Point> copyPoints(List<Point> points) {

		List<Point> copies = new ArrayList<>(points.size());

		for (Point point : points) {
			copies.add(new Point(point.x, point.y));
		}

		return copies;
	}

	private static void paintBridgeLine(
			ColorProcessor visualizationImageProcessor,
			BridgeCandidate bridge) {

		Point firstPoint = bridge.getFirstPoint();
		Point secondPoint = bridge.getSecondPoint();

		paintBresenhamLine(
				visualizationImageProcessor,
				firstPoint.x,
				firstPoint.y,
				secondPoint.x,
				secondPoint.y);
	}

	private static void paintBresenhamLine(
			ColorProcessor visualizationImageProcessor,
			int startX,
			int startY,
			int endX,
			int endY) {

		int deltaX = Math.abs(endX - startX);
		int deltaY = Math.abs(endY - startY);
		int stepX = startX < endX ? 1 : -1;
		int stepY = startY < endY ? 1 : -1;
		int error = deltaX - deltaY;
		int currentX = startX;
		int currentY = startY;

		while (true) {
			paintBridgeLinePixel(
					visualizationImageProcessor,
					currentX,
					currentY);

			if (currentX == endX && currentY == endY) {
				break;
			}

			int doubledError = 2 * error;

			if (doubledError > -deltaY) {
				error -= deltaY;
				currentX += stepX;
			}

			if (doubledError < deltaX) {
				error += deltaX;
				currentY += stepY;
			}
		}
	}

	private static void paintBridgeLinePixel(
			ColorProcessor visualizationImageProcessor,
			int centerX,
			int centerY) {

		int imageWidth = visualizationImageProcessor.getWidth();
		int imageHeight = visualizationImageProcessor.getHeight();

		for (int deltaY = -BRIDGE_LINE_HALF_WIDTH;
				deltaY <= BRIDGE_LINE_HALF_WIDTH;
				deltaY++) {

			int pixelY = centerY + deltaY;

			if (pixelY < 0 || pixelY >= imageHeight) {
				continue;
			}

			for (int deltaX = -BRIDGE_LINE_HALF_WIDTH;
					deltaX <= BRIDGE_LINE_HALF_WIDTH;
					deltaX++) {

				int pixelX = centerX + deltaX;

				if (pixelX < 0 || pixelX >= imageWidth) {
					continue;
				}

				visualizationImageProcessor.putPixel(
						pixelX,
						pixelY,
						BRIDGE_LINE_CYAN_RGB);
			}
		}
	}

	private static void exportLumenCalibrationCsv(
			LumenFilterDiagnostics diagnostics) {

		SaveDialog saveDialog = new SaveDialog(
				"Export Lumen Calibration CSV",
				CALIBRATION_CSV_DEFAULT_FILENAME,
				".csv");
		String directory = saveDialog.getDirectory();
		String fileName = saveDialog.getFileName();

		if (directory == null || fileName == null) {
			IJ.log("Lumen calibration CSV export was cancelled.");
			return;
		}

		File outputFile = new File(directory, fileName);

		try (PrintWriter writer = new PrintWriter(
				new OutputStreamWriter(
						new FileOutputStream(outputFile),
						StandardCharsets.UTF_8))) {

			writer.println(CALIBRATION_CSV_HEADER);

			for (LumenCalibrationRow calibrationRow : diagnostics.calibrationRows) {
				writer.println(formatCalibrationCsvRow(
						calibrationRow.features,
						calibrationRow.filterResult));
			}

			IJ.log("Exported " + diagnostics.calibrationRows.size()
					+ " lumen calibration rows to "
					+ outputFile.getAbsolutePath()
					+ ".");
		} catch (IOException exception) {
			IJ.error(
					"Vessel Reconstruction",
					"Failed to write lumen calibration CSV: "
							+ exception.getMessage());
			IJ.log("Lumen calibration CSV export failed: "
					+ exception.getMessage());
		}
	}

	private static String formatCalibrationCsvRow(
			LumenFeatures features,
			LumenFilterResult filterResult) {

		return features.getLumenId()
				+ ","
				+ ","
				+ filterResult.isAccepted()
				+ ","
				+ features.getArea()
				+ ","
				+ formatCalibrationDouble(features.getPerimeter())
				+ ","
				+ formatCalibrationDouble(features.getCircularity())
				+ ","
				+ formatCalibrationDouble(features.getAspectRatio())
				+ ","
				+ formatCalibrationDouble(features.getExtent())
				+ ","
				+ formatCalibrationDouble(features.getBoundaryRoughness())
				+ ","
				+ features.isTouchesBorder()
				+ ","
				+ formatFilterRejectionReasons(filterResult);
	}

	private static String formatCalibrationDouble(double value) {

		return String.format(Locale.US, "%.4f", value);
	}

	private static TextRoi createLumenIdLabel(Lumen lumen) {

		Point centroid = lumen.getCentroid();
		int labelX = (int) Math.round(centroid.x);
		int labelY = (int) Math.round(centroid.y);
		TextRoi idLabel = new TextRoi(
				labelX,
				labelY,
				String.valueOf(lumen.getId()));
		idLabel.setStrokeColor(Color.WHITE);
		idLabel.setFillColor(Color.BLACK);
		idLabel.setFont(LUMEN_ID_LABEL_FONT);

		return idLabel;
	}

	private static Color diagnosticColorForFilterResult(
			LumenFilterResult filterResult) {

		if (filterResult != null && filterResult.isAccepted()) {
			return new Color(0, 255, 255);
		}

		return new Color(255, 0, 255);
	}

	private static double contourStrokeWidthForFilterResult(
			LumenFilterResult filterResult) {

		if (filterResult != null && filterResult.isAccepted()) {
			return 3.0;
		}

		return 1.5;
	}

	private static void logExploratoryFilterDiagnostics(
			LumenFilterDiagnostics diagnostics,
			LumenFilterCriteria filterCriteria) {

		IJ.log("Exploratory lumen filter enabled in diagnostic mode.");
		IJ.log("Accepted candidate count: "
				+ diagnostics.acceptedCandidateCount + ".");
		IJ.log("Rejected candidate count: "
				+ diagnostics.rejectedCandidateCount + ".");
		IJ.log("Rejection count for AREA: "
				+ diagnostics.areaRejectionCount + ".");
		IJ.log("Rejection count for CIRCULARITY: "
				+ diagnostics.circularityRejectionCount + ".");
		IJ.log("Rejection count for ASPECT_RATIO: "
				+ diagnostics.aspectRatioRejectionCount + ".");
		IJ.log("Rejection count for EXTENT: "
				+ diagnostics.extentRejectionCount + ".");
		IJ.log("Rejection count for BOUNDARY_ROUGHNESS: "
				+ diagnostics.boundaryRoughnessRejectionCount + ".");
		IJ.log("Rejection count for BORDER_CONTACT: "
				+ diagnostics.borderContactRejectionCount + ".");
		IJ.log("Exploratory minimumArea: "
				+ filterCriteria.getMinimumArea() + ".");
		IJ.log("Exploratory minimumCircularity: "
				+ formatFeatureValue(filterCriteria.getMinimumCircularity())
				+ ".");
		IJ.log("Exploratory maximumAspectRatio: "
				+ formatFeatureValue(filterCriteria.getMaximumAspectRatio())
				+ ".");
		IJ.log("Exploratory minimumExtent: "
				+ formatFeatureValue(filterCriteria.getMinimumExtent()) + ".");
		IJ.log("Exploratory maximumBoundaryRoughness: "
				+ formatFeatureValue(
						filterCriteria.getMaximumBoundaryRoughness())
				+ ".");
		IJ.log("Exploratory rejectBorderTouching: "
				+ filterCriteria.isRejectBorderTouching() + ".");
		IJ.log("Feature calculation failure count: "
				+ diagnostics.featureCalculationFailureCount + ".");
	}

	private static void logLumenFilterPerLumenDiagnostics(
			LumenFilterDiagnostics diagnostics) {

		IJ.log("Lumen filter diagnostics:");
		IJ.log(
				"lumenId,accepted,area,circularity,aspectRatio,extent,"
						+ "boundaryRoughness,rejectionReasons");

		for (String diagnosticLine : diagnostics.filterDiagnosticLines) {
			IJ.log(diagnosticLine);
		}
	}

	private static String formatSuccessfulFilterDiagnosticLine(
			LumenFeatures features,
			LumenFilterResult filterResult) {

		return features.getLumenId()
				+ ","
				+ filterResult.isAccepted()
				+ ","
				+ features.getArea()
				+ ","
				+ formatFeatureValue(features.getCircularity())
				+ ","
				+ formatFeatureValue(features.getAspectRatio())
				+ ","
				+ formatFeatureValue(features.getExtent())
				+ ","
				+ formatFeatureValue(features.getBoundaryRoughness())
				+ ","
				+ formatFilterRejectionReasons(filterResult);
	}

	private static String formatFailedFilterDiagnosticLine(
			int lumenId,
			String errorMessage) {

		return lumenId
				+ ",ERROR,,,,,,"
				+ sanitizeCsvField(errorMessage);
	}

	private static String formatFilterRejectionReasons(
			LumenFilterResult filterResult) {

		if (filterResult.isAccepted()) {
			return "NONE";
		}

		return String.join("|", filterResult.getRejectionReasons());
	}

	private static String sanitizeCsvField(String value) {

		if (value == null) {
			return "";
		}

		return value
				.replace(",", ";")
				.replace("\r", " ")
				.replace("\n", " ");
	}

	private static void logLumenFeatureStatistics(List<LumenFeatures> lumenFeatures) {

		if (lumenFeatures.isEmpty()) {
			IJ.log("Lumen feature statistics: none (no calculated features).");
			return;
		}

		int[] areas = new int[lumenFeatures.size()];
		double[] perimeters = new double[lumenFeatures.size()];
		double[] circularities = new double[lumenFeatures.size()];
		double[] aspectRatios = new double[lumenFeatures.size()];
		double[] extents = new double[lumenFeatures.size()];
		double[] boundaryRoughnessValues = new double[lumenFeatures.size()];
		int touchesBorderCount = 0;

		for (int index = 0; index < lumenFeatures.size(); index++) {

			LumenFeatures features = lumenFeatures.get(index);

			areas[index] = features.getArea();
			perimeters[index] = features.getPerimeter();
			circularities[index] = features.getCircularity();
			aspectRatios[index] = features.getAspectRatio();
			extents[index] = features.getExtent();
			boundaryRoughnessValues[index] = features.getBoundaryRoughness();

			if (features.isTouchesBorder()) {
				touchesBorderCount++;
			}
		}

		Arrays.sort(areas);
		Arrays.sort(perimeters);
		Arrays.sort(circularities);
		Arrays.sort(aspectRatios);
		Arrays.sort(extents);
		Arrays.sort(boundaryRoughnessValues);

		logIntegerFeatureDistribution("area", areas);
		logDoubleFeatureDistribution("perimeter", perimeters);
		logDoubleFeatureDistribution("circularity", circularities);
		logDoubleFeatureDistribution("aspectRatio", aspectRatios);
		logDoubleFeatureDistribution("extent", extents);
		logDoubleFeatureDistribution(
				"boundaryRoughness",
				boundaryRoughnessValues);

		IJ.log("Lumina with touchesBorder == true: " + touchesBorderCount + ".");

		IJ.log("Lumina with circularity >= 0.20: "
				+ countDoublesAtOrAbove(circularities, 0.20) + ".");
		IJ.log("Lumina with circularity >= 0.40: "
				+ countDoublesAtOrAbove(circularities, 0.40) + ".");
		IJ.log("Lumina with circularity >= 0.60: "
				+ countDoublesAtOrAbove(circularities, 0.60) + ".");
		IJ.log("Lumina with circularity >= 0.80: "
				+ countDoublesAtOrAbove(circularities, 0.80) + ".");

		IJ.log("Lumina with aspectRatio <= 1.25: "
				+ countDoublesAtOrBelow(aspectRatios, 1.25) + ".");
		IJ.log("Lumina with aspectRatio <= 1.50: "
				+ countDoublesAtOrBelow(aspectRatios, 1.50) + ".");
		IJ.log("Lumina with aspectRatio <= 2.00: "
				+ countDoublesAtOrBelow(aspectRatios, 2.00) + ".");
		IJ.log("Lumina with aspectRatio <= 3.00: "
				+ countDoublesAtOrBelow(aspectRatios, 3.00) + ".");

		IJ.log("Lumina with extent >= 0.30: "
				+ countDoublesAtOrAbove(extents, 0.30) + ".");
		IJ.log("Lumina with extent >= 0.50: "
				+ countDoublesAtOrAbove(extents, 0.50) + ".");
		IJ.log("Lumina with extent >= 0.70: "
				+ countDoublesAtOrAbove(extents, 0.70) + ".");
		IJ.log("Lumina with extent >= 0.85: "
				+ countDoublesAtOrAbove(extents, 0.85) + ".");

		IJ.log("Lumina with boundaryRoughness <= 1.25: "
				+ countDoublesAtOrBelow(boundaryRoughnessValues, 1.25) + ".");
		IJ.log("Lumina with boundaryRoughness <= 1.50: "
				+ countDoublesAtOrBelow(boundaryRoughnessValues, 1.50) + ".");
		IJ.log("Lumina with boundaryRoughness <= 2.00: "
				+ countDoublesAtOrBelow(boundaryRoughnessValues, 2.00) + ".");
		IJ.log("Lumina with boundaryRoughness <= 3.00: "
				+ countDoublesAtOrBelow(boundaryRoughnessValues, 3.00) + ".");

		IJ.log("Lumina with area >= 100: "
				+ countIntegersAtOrAbove(areas, 100) + ".");
		IJ.log("Lumina with area >= 250: "
				+ countIntegersAtOrAbove(areas, 250) + ".");
		IJ.log("Lumina with area >= 500: "
				+ countIntegersAtOrAbove(areas, 500) + ".");
		IJ.log("Lumina with area >= 1000: "
				+ countIntegersAtOrAbove(areas, 1000) + ".");
		IJ.log("Lumina with area >= 5000: "
				+ countIntegersAtOrAbove(areas, 5000) + ".");

		IJ.log("Lumen feature CSV:");
		IJ.log(
				"lumenId,area,perimeter,circularity,aspectRatio,extent,"
						+ "boundaryRoughness,touchesBorder");

		for (LumenFeatures features : lumenFeatures) {
			IJ.log(formatLumenFeatureCsvLine(features));
		}
	}

	private static void logIntegerFeatureDistribution(
			String featureName,
			int[] sortedValues) {

		IJ.log("Lumen feature " + featureName + " minimum: "
				+ sortedValues[0] + ".");
		IJ.log("Lumen feature " + featureName + " 10th percentile: "
				+ formatIntegerPercentile(sortedValues, 0.10) + ".");
		IJ.log("Lumen feature " + featureName + " 25th percentile: "
				+ formatIntegerPercentile(sortedValues, 0.25) + ".");
		IJ.log("Lumen feature " + featureName + " median: "
				+ formatIntegerPercentile(sortedValues, 0.50) + ".");
		IJ.log("Lumen feature " + featureName + " 75th percentile: "
				+ formatIntegerPercentile(sortedValues, 0.75) + ".");
		IJ.log("Lumen feature " + featureName + " 90th percentile: "
				+ formatIntegerPercentile(sortedValues, 0.90) + ".");
		IJ.log("Lumen feature " + featureName + " maximum: "
				+ sortedValues[sortedValues.length - 1] + ".");
	}

	private static void logDoubleFeatureDistribution(
			String featureName,
			double[] sortedValues) {

		IJ.log("Lumen feature " + featureName + " minimum: "
				+ formatFeatureValue(sortedValues[0]) + ".");
		IJ.log("Lumen feature " + featureName + " 10th percentile: "
				+ formatFeatureValue(percentile(sortedValues, 0.10)) + ".");
		IJ.log("Lumen feature " + featureName + " 25th percentile: "
				+ formatFeatureValue(percentile(sortedValues, 0.25)) + ".");
		IJ.log("Lumen feature " + featureName + " median: "
				+ formatFeatureValue(percentile(sortedValues, 0.50)) + ".");
		IJ.log("Lumen feature " + featureName + " 75th percentile: "
				+ formatFeatureValue(percentile(sortedValues, 0.75)) + ".");
		IJ.log("Lumen feature " + featureName + " 90th percentile: "
				+ formatFeatureValue(percentile(sortedValues, 0.90)) + ".");
		IJ.log("Lumen feature " + featureName + " maximum: "
				+ formatFeatureValue(
						sortedValues[sortedValues.length - 1])
				+ ".");
	}

	private static String formatLumenFeatureCsvLine(LumenFeatures features) {

		return features.getLumenId()
				+ ","
				+ features.getArea()
				+ ","
				+ formatFeatureValue(features.getPerimeter())
				+ ","
				+ formatFeatureValue(features.getCircularity())
				+ ","
				+ formatFeatureValue(features.getAspectRatio())
				+ ","
				+ formatFeatureValue(features.getExtent())
				+ ","
				+ formatFeatureValue(features.getBoundaryRoughness())
				+ ","
				+ features.isTouchesBorder();
	}

	private static int countDoublesAtOrAbove(
			double[] sortedValues,
			double threshold) {

		int count = 0;

		for (double value : sortedValues) {

			if (value >= threshold) {
				count++;
			}
		}

		return count;
	}

	private static int countDoublesAtOrBelow(
			double[] sortedValues,
			double threshold) {

		int count = 0;

		for (double value : sortedValues) {

			if (value <= threshold) {
				count++;
			}
		}

		return count;
	}

	private static int countIntegersAtOrAbove(int[] sortedValues, int threshold) {

		int count = 0;

		for (int value : sortedValues) {

			if (value >= threshold) {
				count++;
			}
		}

		return count;
	}

	private static String formatFeatureValue(double value) {

		return String.format("%.4f", value);
	}

	private static void logAcceptedPeakScoreStatistics(
			List<Double> acceptedPeakScores) {

		if (acceptedPeakScores.isEmpty()) {
			IJ.log("Accepted peak raw score statistics: none (no accepted peaks).");
			IJ.log("Accepted peaks with raw score >= 1.0 pixels: 0.");
			IJ.log("Accepted peaks with raw score >= 2.0 pixels: 0.");
			IJ.log("Accepted peaks with raw score >= 3.0 pixels: 0.");
			IJ.log("Accepted peaks with raw score >= 5.0 pixels: 0.");
			IJ.log("Accepted peaks with raw score >= 10.0 pixels: 0.");
			return;
		}

		double[] sortedPeakScores = new double[acceptedPeakScores.size()];

		for (int index = 0; index < acceptedPeakScores.size(); index++) {
			sortedPeakScores[index] = acceptedPeakScores.get(index);
		}

		Arrays.sort(sortedPeakScores);

		IJ.log("Accepted peak raw score minimum: "
				+ formatPixelScore(sortedPeakScores[0]) + " pixels.");
		IJ.log("Accepted peak raw score median: "
				+ formatPixelScore(percentile(sortedPeakScores, 0.5))
				+ " pixels.");
		IJ.log("Accepted peak raw score 75th percentile: "
				+ formatPixelScore(percentile(sortedPeakScores, 0.75))
				+ " pixels.");
		IJ.log("Accepted peak raw score 90th percentile: "
				+ formatPixelScore(percentile(sortedPeakScores, 0.90))
				+ " pixels.");
		IJ.log("Accepted peak raw score 95th percentile: "
				+ formatPixelScore(percentile(sortedPeakScores, 0.95))
				+ " pixels.");
		IJ.log("Accepted peak raw score maximum: "
				+ formatPixelScore(
						sortedPeakScores[sortedPeakScores.length - 1])
				+ " pixels.");
		IJ.log("Accepted peaks with raw score >= 1.0 pixels: "
				+ countScoresAtOrAbove(sortedPeakScores, 1.0) + ".");
		IJ.log("Accepted peaks with raw score >= 2.0 pixels: "
				+ countScoresAtOrAbove(sortedPeakScores, 2.0) + ".");
		IJ.log("Accepted peaks with raw score >= 3.0 pixels: "
				+ countScoresAtOrAbove(sortedPeakScores, 3.0) + ".");
		IJ.log("Accepted peaks with raw score >= 5.0 pixels: "
				+ countScoresAtOrAbove(sortedPeakScores, 5.0) + ".");
		IJ.log("Accepted peaks with raw score >= 10.0 pixels: "
				+ countScoresAtOrAbove(sortedPeakScores, 10.0) + ".");
	}

	private static void logAcceptedPeaksPerLumenStatistics(
			List<Integer> acceptedPeaksPerLumen) {

		if (acceptedPeaksPerLumen.isEmpty()) {
			IJ.log("Accepted peaks per lumen statistics: none (no processed lumina).");
			return;
		}

		int[] sortedPeaksPerLumen = new int[acceptedPeaksPerLumen.size()];

		for (int index = 0; index < acceptedPeaksPerLumen.size(); index++) {
			sortedPeaksPerLumen[index] = acceptedPeaksPerLumen.get(index);
		}

		Arrays.sort(sortedPeaksPerLumen);

		IJ.log("Accepted peaks per lumen minimum: "
				+ sortedPeaksPerLumen[0] + ".");
		IJ.log("Accepted peaks per lumen median: "
				+ formatIntegerPercentile(sortedPeaksPerLumen, 0.5) + ".");
		IJ.log("Accepted peaks per lumen maximum: "
				+ sortedPeaksPerLumen[sortedPeaksPerLumen.length - 1] + ".");
	}

	private static double percentile(double[] sortedValues, double percentile) {

		int sortedIndex = (int) Math.round(
				percentile * (sortedValues.length - 1));

		return sortedValues[sortedIndex];
	}

	private static int formatIntegerPercentile(
			int[] sortedValues,
			double percentile) {

		int sortedIndex = (int) Math.round(
				percentile * (sortedValues.length - 1));

		return sortedValues[sortedIndex];
	}

	private static int countScoresAtOrAbove(
			double[] sortedScores,
			double threshold) {

		int count = 0;

		for (double score : sortedScores) {

			if (score >= threshold) {
				count++;
			}
		}

		return count;
	}

	private static String formatPixelScore(double score) {

		return String.format("%.3f", score);
	}

	private static void paintDeviationScores(
			ColorProcessor visualizationImageProcessor,
			List<Point> boundaryPixels,
			double[] deviationScores) {

		double maximumScore = 0.0;

		for (double deviationScore : deviationScores) {

			if (deviationScore > maximumScore) {
				maximumScore = deviationScore;
			}
		}

		int boundarySize = boundaryPixels.size();
		double[] normalizedScores = new double[boundarySize];

		for (int index = 0; index < boundarySize; index++) {

			normalizedScores[index] = 0.0;

			if (maximumScore > 0.0) {
				normalizedScores[index] = deviationScores[index] / maximumScore;
			}
		}

		Integer[] paintOrder = new Integer[boundarySize];

		for (int index = 0; index < boundarySize; index++) {
			paintOrder[index] = index;
		}

		Arrays.sort(
				paintOrder,
				Comparator.comparingDouble(index -> normalizedScores[index]));

		for (int paintIndex = 0; paintIndex < boundarySize; paintIndex++) {

			int boundaryIndex = paintOrder[paintIndex];
			Point boundaryPoint = boundaryPixels.get(boundaryIndex);

			paintMarker(
					visualizationImageProcessor,
					boundaryPoint.x,
					boundaryPoint.y,
					heatMapColor(normalizedScores[boundaryIndex]));
		}
	}

	private static void paintPeakRing(
			ColorProcessor visualizationImageProcessor,
			int centerX,
			int centerY) {

		int imageWidth = visualizationImageProcessor.getWidth();
		int imageHeight = visualizationImageProcessor.getHeight();
		int minimumDistanceSquared =
				(PEAK_RING_RADIUS - 1) * (PEAK_RING_RADIUS - 1)
						+ PEAK_RING_RADIUS;
		int maximumDistanceSquared =
				PEAK_RING_RADIUS * PEAK_RING_RADIUS + PEAK_RING_RADIUS;

		for (int deltaY = -PEAK_RING_RADIUS; deltaY <= PEAK_RING_RADIUS; deltaY++) {

			int pixelY = centerY + deltaY;

			if (pixelY < 0 || pixelY >= imageHeight) {
				continue;
			}

			for (int deltaX = -PEAK_RING_RADIUS; deltaX <= PEAK_RING_RADIUS; deltaX++) {

				int distanceSquared = deltaX * deltaX + deltaY * deltaY;

				if (distanceSquared < minimumDistanceSquared
						|| distanceSquared > maximumDistanceSquared) {
					continue;
				}

				int pixelX = centerX + deltaX;

				if (pixelX < 0 || pixelX >= imageWidth) {
					continue;
				}

				visualizationImageProcessor.putPixel(
						pixelX,
						pixelY,
						PEAK_RING_MAGENTA_RGB);
			}
		}
	}

	private static void paintMarker(
			ColorProcessor visualizationImageProcessor,
			int centerX,
			int centerY,
			int rgb) {

		int imageWidth = visualizationImageProcessor.getWidth();
		int imageHeight = visualizationImageProcessor.getHeight();

		for (int deltaY = -2; deltaY <= 2; deltaY++) {

			int pixelY = centerY + deltaY;

			if (pixelY < 0 || pixelY >= imageHeight) {
				continue;
			}

			for (int deltaX = -2; deltaX <= 2; deltaX++) {

				int pixelX = centerX + deltaX;

				if (pixelX < 0 || pixelX >= imageWidth) {
					continue;
				}

				visualizationImageProcessor.putPixel(pixelX, pixelY, rgb);
			}
		}
	}

	/**
	 * Maps a normalized score in {@code [0, 1]} to a continuous blue-yellow-red
	 * heat-map color.
	 */
	private static int heatMapColor(double normalizedScore) {

		double clampedScore = Math.max(0.0, Math.min(1.0, normalizedScore));

		int red;
		int green;
		int blue;

		if (clampedScore <= 0.5) {
			double blend = clampedScore / 0.5;
			red = (int) Math.round(255.0 * blend);
			green = (int) Math.round(255.0 * blend);
			blue = (int) Math.round(255.0 * (1.0 - blend));
		} else {
			double blend = (clampedScore - 0.5) / 0.5;
			red = 255;
			green = (int) Math.round(255.0 * (1.0 - blend));
			blue = 0;
		}

		return (red << 16) | (green << 8) | blue;
	}
}
