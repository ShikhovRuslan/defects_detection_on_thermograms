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
        return a.getI() == b.getI();
    }

    boolean isVertical() {
        return a.getJ() == b.getJ();
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
        return a.getI() < b.getI() ? a : b;
    }

    /**
     * Возвращает нижний конец текущей линии.
     *
     * @throws IllegalArgumentException если текущая линия горизонтальна
     */
    Point lowerEnd() {
        if (isHorizontal())
            throw new IllegalArgumentException("Текущая линия горизонтальна.");
        return a.getI() > b.getI() ? a : b;
    }

    /**
     * Возвращает правый конец текущей линии.
     *
     * @throws IllegalArgumentException если текущая линия вертикальна
     */
    Point rightEnd() {
        if (isVertical())
            throw new IllegalArgumentException("Текущая линия вертикальна.");
        return a.getJ() > b.getJ() ? a : b;
    }

    /**
     * Возвращает левый конец текущей линии.
     *
     * @throws IllegalArgumentException если текущая линия вертикальна
     */
    Point leftEnd() {
        if (isVertical())
            throw new IllegalArgumentException("Текущая линия вертикальна.");
        return a.getJ() < b.getJ() ? a : b;
    }

    /**
     * Определяет принадлежность точки {@code point} внутренности текущей линии.
     *
     * @throws IllegalArgumentException если текущая линия ни горизонтальна, ни вертикальна
     */
    boolean contains(Point point) {
        if (isHorizontal())
            return point.getI() == a.getI() && point.projectableTo(this);
        if (isVertical())
            return point.getJ() == a.getJ() && point.projectableTo(this);
        throw new IllegalArgumentException("Текущая линия ни горизонтальна, ни вертикальна.");
    }

    /**
     * Рисует текущую линию.
     *
     * @throws IllegalArgumentException если текущая линия ни горизонтальна, ни вертикальна
     */
    void draw(BufferedImage image, Color color) {
        if (isHorizontal()) {
            for (int i = Math.min(a.getJ(), b.getJ()); i <= Math.max(a.getJ(), b.getJ()); i++)
                image.setRGB(i, a.getI(), color.getRGB());
            return;
        }
        if (isVertical()) {
            for (int i = Math.min(a.getI(), b.getI()); i <= Math.max(a.getI(), b.getI()); i++)
                image.setRGB(a.getJ(), i, color.getRGB());
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