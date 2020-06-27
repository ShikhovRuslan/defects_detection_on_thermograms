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

    Point getA() {
        return a;
    }

    Point getB() {
        return b;
    }

    boolean isHorizontal() {
        return a.getX() == b.getX();
    }

    boolean isVertical() {
        return a.getY() == b.getY();
    }

    Point getOtherEnd(Point point) {
        if (a == point) return b;
        if (b == point) return a;
        return new Point(0, 0);
    }

    Point upperEnd() {
        return a.getX() < b.getX() ? a : b;
    }

    Point lowerEnd() {
        return a.getX() > b.getX() ? a : b;
    }

    Point rightEnd() {
        return a.getY() > b.getY() ? a : b;
    }

    Point leftEnd() {
        return a.getY() < b.getY() ? a : b;
    }

    // Вершины игнорируются.
    boolean contains(Point point) {
        if (isHorizontal())
            return point.getX() == a.getX() && point.projectableTo(this);
        if (isVertical())
            return point.getY() == a.getY() && point.projectableTo(this);
        return false;
    }

    void draw(BufferedImage image, Color color) {
        if (isHorizontal())
            for (int k = Math.min(a.getY(), b.getY()); k <= Math.max(a.getY(), b.getY()); k++)
                image.setRGB(k, a.getX(), color.getRGB());
        if (isVertical())
            for (int k = Math.min(a.getX(), b.getX()); k <= Math.max(a.getX(), b.getX()); k++)
                image.setRGB(a.getY(), k, color.getRGB());
    }

    @Override
    public String toString() {
        return a.toString() + b.toString();
    }
}