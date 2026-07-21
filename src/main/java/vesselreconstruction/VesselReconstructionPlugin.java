package vesselreconstruction;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import vesselreconstruction.detection.ConnectedComponentLabeler;
import vesselreconstruction.model.Lumen;

import java.util.List;

public class VesselReconstructionPlugin implements PlugIn {

	@Override
	public void run(String arg) {

		try {

			IJ.log("A - Plugin started");

			ImagePlus image = WindowManager.getCurrentImage();

			if (image == null) {
				IJ.log("No image open.");

				IJ.showMessage(
						"Vessel Reconstruction",
						"Please open a classified image first."
				);
				return;
			}

			IJ.log("B - Image acquired");

			ConnectedComponentLabeler labeler =
					new ConnectedComponentLabeler();

			IJ.log("C - Labeler created");

			List<Lumen> lumina = labeler.detect(image);

			IJ.log("D - detect() returned");

			IJ.log("Lumina detected = " + lumina.size());

			IJ.log("E - About to show dialog");

			IJ.showMessage(
					"Vessel Reconstruction",
					"Detected " + lumina.size() + " lumen objects."
			);

			IJ.log("F - Dialog closed");

			IJ.log("Plugin finished successfully.");

		}
		catch (Throwable t) {

			IJ.log("");
			IJ.log("===== EXCEPTION =====");

			t.printStackTrace();

			IJ.handleException(t);
		}
	}
}