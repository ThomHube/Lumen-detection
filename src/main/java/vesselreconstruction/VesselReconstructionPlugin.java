package vesselreconstruction;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import vesselreconstruction.detection.ConnectedComponentLabeler;
import vesselreconstruction.geometry.ImaginaryRuler;
import vesselreconstruction.model.Lumen;

import java.awt.Color;
import java.awt.Point;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class VesselReconstructionPlugin implements PlugIn {

	private static final int IMAGINARY_RULER_NEIGHBOUR_COUNT = 10;
	private static final int DEVIATION_BACKGROUND_GRAY = 40;

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

		Overlay overlay = new Overlay();
		int contourRoiCount = 0;

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
			contourRoi.setStrokeColor(Color.BLACK);
			contourRoi.setStrokeWidth(2.0);

			overlay.add(contourRoi);
			contourRoiCount++;
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

		ImaginaryRuler imaginaryRuler = new ImaginaryRuler();
		int visualizedLumenCount = 0;
		int skippedShortBoundaryCount = 0;
		int minimumBoundaryLength =
				2 * IMAGINARY_RULER_NEIGHBOUR_COUNT + 1;

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

			visualizedLumenCount++;
		}

		ImagePlus deviationImage = new ImagePlus(
				"Imaginary Ruler Deviation",
				visualizationImageProcessor);
		deviationImage.show();

		IJ.log("Added " + contourRoiCount + " contour ROIs.");
		IJ.log("Visualized " + visualizedLumenCount + " lumen deviation maps.");
		IJ.log("Skipped " + skippedShortBoundaryCount
				+ " lumina with boundaries shorter than "
				+ minimumBoundaryLength + " points.");
		IJ.log("ImaginaryRuler neighbour count: "
				+ IMAGINARY_RULER_NEIGHBOUR_COUNT + ".");

		IJ.showMessage(
				"Vessel Reconstruction",
				"Detected " + lumina.size() + " lumen objects.\n"
						+ "Visualized " + visualizedLumenCount + " lumen deviation maps.\n"
						+ "ImaginaryRuler neighbour count: "
						+ IMAGINARY_RULER_NEIGHBOUR_COUNT + "."
		);
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
