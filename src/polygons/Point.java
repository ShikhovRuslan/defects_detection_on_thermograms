package polygons;

import main.*;

import java.util.List;

import static java.lang.Math.abs;


/**
 * Используется для хранения координат точек в системе координат Oxy.
 */
public class Point extends AbstractPoint {
    public Point(int i, int j) {
        super(i, j);
    }

    @Override
    public Point create(int i, int j) {
        return new Point(i, j);
    }

    /**
     * Конвертирует текущую точку из системы координат Oxy в систему координат c'x'y'.
     */
    public Pixel toPixel(int resY) {
        return new Pixel(getJ(), resY - 1 - getI());
    }

    /**
     * Возвращает расстояние от текущей точки до прямой, содержащей отрезок {@code segment}.
     *
     * @throws IllegalArgumentException если отрезок не является ни горизонтальным, ни вертикальным
     */
    public int distanceTo(Segment segment) {
        if (segment.isHorizontal())
            return abs(getI() - segment.getA().getI());
        if (segment.isVertical())
            return abs(getJ() - segment.getA().getJ());

        throw new IllegalArgumentException("Отрезок не является ни горизонтальным, ни вертикальным.");
    }

    /**
     * Конвертирует точку {@code point} из системы координат c'x'y' в систему координат Oxy.
     */
    public static Point toPoint(Pixel point, int resY) {
        return new Point(resY - 1 - point.getJ(), point.getI());
    }

    /**
     * Определяет возможность проектирования текущей точки на внутренность отрезка {@code segment}, если
     * {@code inclusive[0]} равен {@code false}, или на весь отрезок, в противном случае. Отсутствие {@code inclusive}
     * равносильно значению {@code false}.
     *
     * @throws IllegalArgumentException если отрезок не является ни горизонтальным, ни вертикальным
     */
    public boolean projectableTo(Segment segment, boolean... inclusive) {
        int minJ = Math.min(segment.getA().getJ(), segment.getB().getJ());
        int maxJ = Math.max(segment.getA().getJ(), segment.getB().getJ());
        int minI = Math.min(segment.getA().getI(), segment.getB().getI());
        int maxI = Math.max(segment.getA().getI(), segment.getB().getI());

        if (segment.isHorizontal()) return (inclusive.length > 0 && inclusive[0]) ?
                getJ() >= minJ && getJ() <= maxJ : getJ() > minJ && getJ() < maxJ;
        if (segment.isVertical()) return (inclusive.length > 0 && inclusive[0]) ?
                getI() >= minI && getI() <= maxI : getI() > minI && getI() < maxI;

        throw new IllegalArgumentException("Отрезок не является ни горизонтальным, ни вертикальным.");
    }

    /**
     * Возвращает проекцию текущей точки на отрезок {@code segment}.
     *
     * @throws IllegalArgumentException если текущую точку нельзя спроектировать на отрезок или отрезок не является ни
     *                                  горизонтальным, ни вертикальным
     */
    public Point project(Segment segment) {
        if (segment.isHorizontal() && (projectableTo(segment) || getJ() == segment.getA().getJ() || getJ() == segment.getB().getJ()))
            return new Point(segment.getA().getI(), getJ());
        if (segment.isVertical() && (projectableTo(segment) || getI() == segment.getA().getI() || getI() == segment.getB().getI()))
            return new Point(getI(), segment.getA().getJ());
        throw new IllegalArgumentException("Текущую точку нельзя спроектировать на отрезок, или отрезок не является " +
                "ни горизонтальным, ни вертикальным.");
    }

    /**
     * Определяет, принадлежит ли текущая точка какому-нибудь прямоугольнику из списка {@code rectangles}.
     */
    public boolean isInRectangles(List<Rectangle<Point>> rectangles, double focalLength) {
        for (Rectangle<Point> rectangle : rectangles)
            if (rectangle.contains(this, focalLength))
                return true;
        return false;
    }
}