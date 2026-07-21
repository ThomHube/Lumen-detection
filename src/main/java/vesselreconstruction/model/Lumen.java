package vesselreconstruction.model;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class Lumen {

    private final int id;

    private final List<Point> pixels = new ArrayList<>();
    private final List<Point> boundary = new ArrayList<>();

    private Point centroid;

    /*
     * Basic geometry
     */
    private int area;
    private double perimeter;

    /*
     * Shape descriptors
     */
    private double equivalentDiameter;
    private double circularity;
    private double solidity;
    private double aspectRatio;

    /*
     * Bounding box
     */
    private Rectangle boundingBox;

    /*
     * Miscellaneous
     */
    private boolean touchesBorder;

    public Lumen(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public List<Point> getPixels() {
        return pixels;
    }

    public List<Point> getBoundary() {
        return boundary;
    }

    public Point getCentroid() {
        return centroid;
    }

    public void setCentroid(Point centroid) {
        this.centroid = centroid;
    }

    public int getArea() {
        return area;
    }

    public void setArea(int area) {
        this.area = area;
    }

    public double getPerimeter() {
        return perimeter;
    }

    public void setPerimeter(double perimeter) {
        this.perimeter = perimeter;
    }

    public double getEquivalentDiameter() {
        return equivalentDiameter;
    }

    public void setEquivalentDiameter(double equivalentDiameter) {
        this.equivalentDiameter = equivalentDiameter;
    }

    public double getCircularity() {
        return circularity;
    }

    public void setCircularity(double circularity) {
        this.circularity = circularity;
    }

    public double getSolidity() {
        return solidity;
    }

    public void setSolidity(double solidity) {
        this.solidity = solidity;
    }

    public double getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(double aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public Rectangle getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(Rectangle boundingBox) {
        this.boundingBox = boundingBox;
    }

    public boolean touchesBorder() {
        return touchesBorder;
    }

    public void setTouchesBorder(boolean touchesBorder) {
        this.touchesBorder = touchesBorder;
    }
}