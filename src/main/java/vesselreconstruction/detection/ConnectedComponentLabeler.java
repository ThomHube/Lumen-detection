package vesselreconstruction.detection;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import vesselreconstruction.geometry.BoundaryTracer;
import vesselreconstruction.model.Lumen;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class ConnectedComponentLabeler {

    private static final int VESSEL = 3;
    private static final int MIN_LUMEN_SIZE = 100;

    private final BoundaryTracer boundaryTracer = new BoundaryTracer();

    public List<Lumen> detect(ImagePlus image) {

        IJ.log("ConnectedComponentLabeler started.");

        ImageProcessor ip = image.getProcessor();

        int width = image.getWidth();
        int height = image.getHeight();

        boolean[][] visited = new boolean[height][width];

        List<Lumen> lumenList = new ArrayList<>();

        int id = 1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                if (ip.get(x, y) == VESSEL && !visited[y][x]) {

                    Lumen lumen = floodFill(ip, visited, x, y, id);

                    lumen.finish();

                    if (lumen.getArea() >= MIN_LUMEN_SIZE) {

                        lumen.setBoundaryPixels(
                                boundaryTracer.traceBoundary(lumen)
                        );

                        lumenList.add(lumen);

                        IJ.log(
                                "Lumen " + id +
                                        " | area=" + lumen.getArea() +
                                        " | boundary=" + lumen.getBoundaryPixels().size()
                        );

                        id++;
                    }
                }
            }
        }

        IJ.log("");
        IJ.log("Detected " + lumenList.size() + " lumen objects.");

        return lumenList;
    }

    private Lumen floodFill(ImageProcessor ip,
                            boolean[][] visited,
                            int startX,
                            int startY,
                            int id) {

        Lumen lumen = new Lumen(id);

        ArrayDeque<Point> queue = new ArrayDeque<>();
        queue.add(new Point(startX, startY));

        int width = ip.getWidth();
        int height = ip.getHeight();

        while (!queue.isEmpty()) {

            Point p = queue.removeFirst();

            int x = p.x;
            int y = p.y;

            if (x < 0 || x >= width || y < 0 || y >= height)
                continue;

            if (visited[y][x])
                continue;

            if (ip.get(x, y) != VESSEL)
                continue;

            visited[y][x] = true;

            lumen.addPixel(new Point(x, y));

            queue.add(new Point(x + 1, y));
            queue.add(new Point(x - 1, y));
            queue.add(new Point(x, y + 1));
            queue.add(new Point(x, y - 1));
        }

        return lumen;
    }
}