package polygons;

import java.awt.*;
import java.awt.image.BufferedImage;


public class Line {
    private final Point a;
    private final Point b;

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
     * Возвращает конец текущей линии, отличный от точки {@code end}.
     *
     * @throws IllegalArgumentException если точка {@code end} не является концом текущей линии.
     */
    Point getOtherEnd(Point end) {
        if (a == end) return b;
        if (b == end) return a;
        throw new IllegalArgumentException("Аргумент не является концом текущей линии.");
    }

    /**
     * Возвращает верхний конец текущей линии.
     *
     * @throws IllegalArgumentException если текущая линия горизонтальна
     */
    Point upperEnd() {
        if (isHorizontal())
            throw new IllegalArgumentException("Текущая линия горизонтальна.");
        return a.getX() < b.getX() ? a : b;
    }

    /**
     * Возвращает нижний конец текущей линии.
     *
     * @throws IllegalArgumentException если текущая линия горизонтальна
     */
    Point lowerEnd() {
        if (isHorizontal())
            throw new IllegalArgumentException("Текущая линия горизонтальна.");
        return a.getX() > b.getX() ? a : b;
    }

    /**
     * Возвращает правый конец текущей линии.
     *
     * @throws IllegalArgumentException если текущая линия вертикальна
     */
    Point rightEnd() {
        if (isVertical())
            throw new IllegalArgumentException("Текущая линия вертикальна.");
        return a.getY() > b.getY() ? a : b;
    }

    /**
     * Возвращает левый конец текущей линии.
     *
     * @throws IllegalArgumentException если текущая линия вертикальна
     */
    Point leftEnd() {
        if (isVertical())
            throw new IllegalArgumentException("Текущая линия вертикальна.");
        return a.getY() < b.getY() ? a : b;
    }

    /**
     * Определяет принадлежность точки {@code point} внутренности текущей линии.
     *
     * @throws IllegalArgumentException если текущая линия ни горизонтальна, ни вертикальна
     */
    boolean contains(Point point) {
        if (isHorizontal())
            return point.getX() == a.getX() && point.projectableTo(this);
        if (isVertical())
            return point.getY() == a.getY() && point.projectableTo(this);
        throw new IllegalArgumentException("Текущая линия ни горизонтальна, ни вертикальна.");
    }

    /**
     * Рисует текущую линию.
     *
     * @throws IllegalArgumentException если текущая линия ни горизонтальна, ни вертикальна
     */
    void draw(BufferedImage image, Color color) {
        if (isHorizontal()) {
            for (int i = Math.min(a.getY(), b.getY()); i <= Math.max(a.getY(), b.getY()); i++)
                image.setRGB(i, a.getX(), color.getRGB());
            return;
        }
        if (isVertical()) {
            for (int i = Math.min(a.getX(), b.getX()); i <= Math.max(a.getX(), b.getX()); i++)
                image.setRGB(a.getY(), i, color.getRGB());
            return;
        }
        throw new IllegalArgumentException("Текущая линия ни горизонтальна, ни вертикальна.");
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