package polygons;

import main.Helper;
import main.Pixel;
import main.Polygon;
import main.Rectangle;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Используется для хранения концов отрезка в системе координат Oxy.
 */
public class Segment {
    /**
     * Конец отрезка.
     */
    private final Point a;
    /**
     * Конец отрезка.
     */
    private final Point b;

    public Segment(Point a, Point b) {
        this.a = a;
        this.b = b;
    }

    public Point getA() {
        return a;
    }

    public Point getB() {
        return b;
    }

    /**
     * Вычисляет длину текущего отрезка.
     *
     * @throws IllegalArgumentException если текущий отрезок ни горизонтален, ни вертикален
     */
    public int length() {
        if (isHorizontal()) return Math.abs(a.getJ() - b.getJ());
        if (isVertical()) return Math.abs(a.getI() - b.getI());
        throw new IllegalArgumentException("Текущий отрезок ни горизонтален, ни вертикален.");
    }

    /**
     * Определяет, является ли текущий отрезок горизонтальным.
     */
    public boolean isHorizontal() {
        return a.getI() == b.getI();
    }

    /**
     * Определяет, является ли текущий отрезок вертикальным.
     */
    public boolean isVertical() {
        return a.getJ() == b.getJ();
    }

    /**
     * Возвращает конец текущего отрезка, отличный от точки {@code end}.
     *
     * @throws IllegalArgumentException если точка {@code end} не является концом текущего отрезка.
     */
    public Point getOtherEnd(Point end) {
        if (a.equals(end)) return b;
        if (b.equals(end)) return a;
        throw new IllegalArgumentException("Аргумент не является концом текущего отрезка.");
    }

    /**
     * Возвращает верхний конец текущего отрезка.
     *
     * @throws IllegalArgumentException если текущий отрезок горизонтален и не является точкой
     */
    public Point upperEnd() {
        if (isHorizontal() && !isPointNotLine())
            throw new IllegalArgumentException("Текущий отрезок горизонтален и не является точкой.");
        return a.getI() < b.getI() ? a : b;
    }

    /**
     * Возвращает нижний конец текущего отрезка.
     *
     * @throws IllegalArgumentException если текущий отрезок горизонтален и не является точкой
     */
    public Point lowerEnd() {
        if (isHorizontal() && !isPointNotLine())
            throw new IllegalArgumentException("Текущий отрезок горизонтален и не является точкой.");
        return a.getI() > b.getI() ? a : b;
    }

    /**
     * Возвращает правый конец текущего отрезка.
     *
     * @throws IllegalArgumentException если текущий отрезок вертикален и не является точкой
     */
    public Point rightEnd() {
        if (isVertical() && !isPointNotLine())
            throw new IllegalArgumentException("Текущий отрезок вертикален и не является точкой.");
        return a.getJ() > b.getJ() ? a : b;
    }

    /**
     * Возвращает левый конец текущего отрезка.
     *
     * @throws IllegalArgumentException если текущий отрезок вертикален и не является точкой
     */
    public Point leftEnd() {
        if (isVertical() && !isPointNotLine())
            throw new IllegalArgumentException("Текущий отрезок вертикален и не является точкой.");
        return a.getJ() < b.getJ() ? a : b;
    }

    /**
     * Определяет принадлежность точки {@code point} внутренности текущего отрезка.
     *
     * @throws IllegalArgumentException если текущий отрезок ни горизонтален, ни вертикален
     */
    public boolean contains(Point point) {
        if (isHorizontal())
            return point.getI() == a.getI() && point.projectableTo(this);
        if (isVertical())
            return point.getJ() == a.getJ() && point.projectableTo(this);
        throw new IllegalArgumentException("Текущий отрезок ни горизонтален, ни вертикален.");
    }

    /**
     * Определяет, содержит ли внутренность текущего отрезка какую-либо вершину многоугольника {@code polygon}.
     */
    public boolean containsVertexFrom(Polygon<Point> polygon) {
        for (Point vertex : polygon.getVertices())
            if (contains(vertex)) return true;
        return false;
    }

    /**
     * Определяет, пересекается ли текущий отрезок с какой-либо стороной многоугольника {@code polygon} по единственной
     * точке, которая является внутренней для каждого из этих отрезков.
     */
    public boolean intersectsSideOf(Polygon<Point> polygon) {
        for (Segment side : Polygon.getSides(polygon))
            if (!Pixel.findIntersection(new Pixel(a.getI(), a.getJ()), new Pixel(b.getI(), b.getJ()),
                    new Pixel(side.a.getI(), side.a.getJ()), new Pixel(side.b.getI(), side.b.getJ()))
                    .equals(new Pixel(Integer.MIN_VALUE, Integer.MIN_VALUE)))
                return true;
        return false;
    }

    /**
     * Рисует текущий отрезок.
     */
    public void draw(BufferedImage image, Color color) {
        int maxI = Math.max(a.getI(), b.getI());
        int minI = Math.min(a.getI(), b.getI());
        int maxJ = Math.max(a.getJ(), b.getJ());
        int minJ = Math.min(a.getJ(), b.getJ());

        var rectangleImage = new Rectangle<>(new Point(0, 0), new Point(image.getWidth() - 1, image.getHeight() - 1));

        if (isHorizontal()) {
            for (int j = minJ; j <= maxJ; j++)
                if (rectangleImage.contains(new Point(j, a.getI()), 0))
                    image.setRGB(j, a.getI(), color.getRGB());
            return;
        }
        if (isVertical()) {
            for (int i = minI; i <= maxI; i++)
                if (rectangleImage.contains(new Point(a.getJ(), i), 0))
                    image.setRGB(a.getJ(), i, color.getRGB());
            return;
        }
        // y=A*x+B - уравнение прямой, проходящей через концы текущего отрезка.
        double A = (a.getJ() - b.getJ()) / (a.getI() - b.getI() + 0.);
        double B = a.getJ() - A * a.getI();
        double p; // точка, пробегающая отрезок [minI, maxI]
        for (int i = 0; i <= Math.max(maxI - minI, maxJ - minJ); i++) {
            p = minI + i * (maxI - minI + 0.) / Math.max(maxI - minI, maxJ - minJ);
            int x = (int) Math.round(A * p + B);
            int y = (int) Math.round(p);
            if (rectangleImage.contains(new Point(x, y), 0))
                image.setRGB(x, y, color.getRGB());
        }
    }

    /**
     * Определяет, является ли текущий отрезок точкой.
     */
    public boolean isPointNotLine() {
        return a.equals(b);
    }

    /**
     * Возвращает упорядоченный массив отрезков из массива {@code segments}.
     */
    public static Segment[] order(Segment[] segments) throws NullPointerException {
        Segment[] newSegments = new Segment[segments.length];
        List<Integer> processed = new ArrayList<>();
        newSegments[0] = segments[0];
        for (int i = 1; i < segments.length; i++)
            for (int j = 1; j < segments.length; j++)
                if (!Helper.isIn(processed, j)) {
                    if (newSegments[i - 1].getB().equals(segments[j].getA())) {
                        newSegments[i] = segments[j];
                        processed.add(j);
                        break;
                    }
                    if (newSegments[i - 1].getB().equals(segments[j].getB())) {
                        newSegments[i] = new Segment(segments[j].getB(), segments[j].getA());
                        processed.add(j);
                        break;
                    }
                }
        return newSegments;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Segment segment = (Segment) obj;

        if (!Objects.equals(a, segment.a) && !Objects.equals(a, segment.b)) return false;
        return Objects.equals(b, Objects.equals(a, segment.a) ? segment.b : segment.a);
    }

    @Override
    public String toString() {
        return a.getClass().getName() + "[" + a.toShortString() + ", " + b.toShortString() + "]";
    }
}