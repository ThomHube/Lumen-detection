package vesselreconstruction.model;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class Lumen {

    private final int id;

    private final List<Point> pixels = new ArrayList<>();
    private final List<Point> boundaryPixels = new ArrayList<>();

    private Point centroid;
    private Rectangle boundingBox;

    private int area;
    private boolean touchesBorder;

    public Lumen(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void addPixel(Point point) {
        pixels.add(point);
    }

    public List<Point> getPixels() {
        return pixels;
    }

    public List<Point> getBoundaryPixels() {
        return boundaryPixels;
    }

    public void setBoundaryPixels(List<Point> boundaryPixels) {
        this.boundaryPixels.clear();
        this.boundaryPixels.addAll(boundaryPixels);
    }

    public Point getCentroid() {
        return centroid;
    }

    public Rectangle getBoundingBox() {
        return boundingBox;
    }

    public int getArea() {
        return area;
    }

    public boolean touchesBorder() {
        return touchesBorder;
    }

    public void setTouchesBorder(boolean touchesBorder) {
        this.touchesBorder = touchesBorder;
    }

    public int getMinX() {
        return boundingBox.x;
    }

    public int getMaxX() {
        return boundingBox.x + boundingBox.width;
    }

    public int getMinY() {
        return boundingBox.y;
    }

    public int getMaxY() {
        return boundingBox.y + boundingBox.height;
    }

    public void finish() {

        area = pixels.size();

        if (pixels.isEmpty()) {
            centroid = new Point();
            boundingBox = new Rectangle();
            return;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        long sumX = 0;
        long sumY = 0;

        for (Point p : pixels) {

            sumX += p.x;
            sumY += p.y;

            if (p.x < minX) minX = p.x;
            if (p.x > maxX) maxX = p.x;

            if (p.y < minY) minY = p.y;
            if (p.y > maxY) maxY = p.y;
        }

        centroid = new Point(
                (int) Math.round((double) sumX / area),
                (int) Math.round((double) sumY / area)
        );

        boundingBox = new Rectangle(
                minX,
                minY,
                maxX - minX + 1,
                maxY - minY + 1
        );
    }
}