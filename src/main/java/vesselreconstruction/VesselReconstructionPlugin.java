package vesselreconstruction;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import vesselreconstruction.detection.ConnectedComponentLabeler;
import vesselreconstruction.model.Lumen;

import java.awt.Color;
import java.awt.Point;
import java.util.List;

public class VesselReconstructionPlugin implements PlugIn {

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

		IJ.log("Added " + contourRoiCount + " contour ROIs.");

		IJ.showMessage(
				"Vessel Reconstruction",
				"Detected " + lumina.size() + " lumen objects."
		);
	}
}
