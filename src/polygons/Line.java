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

    /**
     * Возвращает конец текущей линии, отличный от точки {@param end}, которая предполагается концом этой линии.
     */
    Point getOtherEnd(Point end) {
        if (a == end) return b;
        if (b == end) return a;
        return new Point(-1, -1);
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

    /**
     * Определяет, принадлежит ли точка {@param point} внутренности текущей линии. Линия предполагается горизонтальной
     * или вертикальной.
     */
    boolean contains(Point point) {
        if (isHorizontal())
            return point.getX() == a.getX() && point.projectableTo(this);
        if (isVertical())
            return point.getY() == a.getY() && point.projectableTo(this);
        return false;
    }

    /**
     * Рисует линию. Линия предполагается горизонтальной или вертикальной.
     */
    void draw(BufferedImage image, Color color) {
        if (isHorizontal())
            for (int i = Math.min(a.getY(), b.getY()); i <= Math.max(a.getY(), b.getY()); i++)
                image.setRGB(i, a.getX(), color.getRGB());
        if (isVertical())
            for (int i = Math.min(a.getX(), b.getX()); i <= Math.max(a.getX(), b.getX()); i++)
                image.setRGB(a.getY(), i, color.getRGB());
    }

    /**
     * Определяет, является ли текущая линия точкой.
     */
    boolean isPointNotLine() {
        return a.equals(b);
    }

    @Override
    public String toString() {
        return a.toString() + b.toString();
    }
}