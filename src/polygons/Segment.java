package polygons;

import main.Helper;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


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
        if (a == end) return b;
        if (b == end) return a;
        throw new IllegalArgumentException("Аргумент не является концом текущего отрезка.");
    }

    /**
     * Возвращает верхний конец текущего отрезка.
     *
     * @throws IllegalArgumentException если текущий отрезок горизонтален
     */
    public Point upperEnd() {
        if (isHorizontal())
            throw new IllegalArgumentException("Текущий отрезок горизонтален.");
        return a.getI() < b.getI() ? a : b;
    }

    /**
     * Возвращает нижний конец текущего отрезка.
     *
     * @throws IllegalArgumentException если текущий отрезок горизонтален
     */
    public Point lowerEnd() {
        if (isHorizontal())
            throw new IllegalArgumentException("Текущий отрезок горизонтален.");
        return a.getI() > b.getI() ? a : b;
    }

    /**
     * Возвращает правый конец текущего отрезка.
     *
     * @throws IllegalArgumentException если текущий отрезок вертикален
     */
    public Point rightEnd() {
        if (isVertical())
            throw new IllegalArgumentException("Текущий отрезок вертикален.");
        return a.getJ() > b.getJ() ? a : b;
    }

    /**
     * Возвращает левый конец текущего отрезка.
     *
     * @throws IllegalArgumentException если текущий отрезок вертикален
     */
    public Point leftEnd() {
        if (isVertical())
            throw new IllegalArgumentException("Текущий отрезок вертикален.");
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
     * Рисует текущий отрезок.
     */
    public void draw(BufferedImage image, Color color) {
        int maxI = Math.max(a.getI(), b.getI());
        int minI = Math.min(a.getI(), b.getI());
        int maxJ = Math.max(a.getJ(), b.getJ());
        int minJ = Math.min(a.getJ(), b.getJ());
        if (isHorizontal()) {
            for (int i = minJ; i <= maxJ; i++)
                image.setRGB(i, a.getI(), color.getRGB());
            return;
        }
        if (isVertical()) {
            for (int i = minI; i <= maxI; i++)
                image.setRGB(a.getJ(), i, color.getRGB());
            return;
        }
        // y=A*x+B - уравнение прямой, проходящей через концы текущего отрезка.
        double A = (a.getJ() - b.getJ()) / (a.getI() - b.getI() + 0.);
        double B = a.getJ() - A * a.getI();
        double p; // точка, пробегающая отрезок [minI, maxI]
        for (int i = 0; i <= Math.max(maxI - minI, maxJ - minJ); i++) {
            p = minI + i * (maxI - minI + 0.) / Math.max(maxI - minI, maxJ - minJ);
            image.setRGB((int) Math.round(A * p + B), (int) Math.round(p), color.getRGB());
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
    public String toString() {
        return a.getClass().getName() + "[" + a.toShortString() + ", " + b.toShortString() + "]";
    }
}