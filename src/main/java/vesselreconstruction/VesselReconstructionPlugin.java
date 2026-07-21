package vesselreconstruction;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Overlay;
import ij.gui.PointRoi;
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

		for (Lumen lumen : lumina) {

			for (Point p : lumen.getBoundaryPixels()) {

				PointRoi point = new PointRoi(p.x, p.y);
				point.setStrokeColor(Color.RED);

				overlay.add(point);
			}
		}

		image.setOverlay(overlay);
		image.updateAndDraw();

		IJ.showMessage(
				"Vessel Reconstruction",
				"Detected " + lumina.size() + " lumen objects."
		);
	}
}