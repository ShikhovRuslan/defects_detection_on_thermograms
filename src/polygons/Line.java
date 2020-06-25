package polygons;


import java.awt.*;
import java.awt.image.BufferedImage;

public class Line {
    private Point a;
    private Point b;

    public Line(Point a, Point b) {
        this.a = a;
        this.b = b;
    }

    public Point getA() {
        return a;
    }

    public Point getB() {
        return b;
    }

    public boolean isHorizontal() {
        return a.getX() == b.getX();
    }

    public boolean isVertical() {
        return a.getY() == b.getY();
    }

    public Point getOtherEnd(Point point) {
        if (a == point) return b;
        if (b == point) return a;
        return new Point(0, 0);
    }

    public Point upperEnd() {
        return a.getX() < b.getX() ? a : b;
    }

    public Point lowerEnd() {
        return a.getX() > b.getX() ? a : b;
    }

    public Point rightEnd() {
        return a.getY() > b.getY() ? a : b;
    }

    public Point leftEnd() {
        return a.getY() < b.getY() ? a : b;
    }

    // Вершины игнорируются.
    public boolean contains(Point point) {
        if (isHorizontal())
            return point.getX() == a.getX() && point.projectableTo(this);
        if (isVertical())
            return point.getY() == a.getY() && point.projectableTo(this);
        return false;
    }

    public void draw(BufferedImage image, Color color) {
        if (isHorizontal())
            for (int k = Math.min(a.getY(),b.getY()); k <= Math.max(a.getY(),b.getY()); k++)
                image.setRGB(k, a.getX(), color.getRGB());
        if (isVertical())
            for (int k = Math.min(a.getX(),b.getX()); k <= Math.max(a.getX(),b.getX()); k++)
                image.setRGB(a.getY(), k, color.getRGB());
    }

    @Override
    public String toString() {
        return a.toString() + b.toString();
    }
}