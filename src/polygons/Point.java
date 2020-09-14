package polygons;

import main.*;

import java.util.List;


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
     * Возвращает расстояние от текущей точки до отрезка {@code segment}.
     *
     * @throws IllegalArgumentException если текущая точка не может быть спроектирована на внутренность отрезка или
     *                                  отрезок не является ни горизонтальным, ни вертикальным
     */
    public int distance(Segment segment) {
        if (projectableTo(segment)) {
            if (segment.isHorizontal())
                return Math.abs(getI() - segment.getA().getI());
            if (segment.isVertical())
                return Math.abs(getJ() - segment.getA().getJ());
        }
        throw new IllegalArgumentException("Текущая точка не может быть спроектирована на внутренность отрезка, или " +
                "отрезок не является ни горизонтальным, ни вертикальным.");
    }

    /**
     * Конвертирует точку {@code point} из системы координат c'x'y' в систему координат Oxy.
     */
    public static Point toPoint(Pixel point) {
        return new Point(Main.RES_Y - 1 - point.getJ(), point.getI());
    }

    /**
     * Определяет возможность проектирования текущей точки на внутренность отрезка {@code segment}.
     *
     * @throws IllegalArgumentException если отрезок не является ни горизонтальным, ни вертикальным
     */
    public boolean projectableTo(Segment segment) {
        if (segment.isHorizontal())
            return getJ() > Math.min(segment.getA().getJ(), segment.getB().getJ()) &&
                    getJ() < Math.max(segment.getA().getJ(), segment.getB().getJ());
        if (segment.isVertical())
            return getI() > Math.min(segment.getA().getI(), segment.getB().getI()) &&
                    getI() < Math.max(segment.getA().getI(), segment.getB().getI());
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
    public boolean isInRectangles(List<Rectangle<Point>> rectangles) {
        for (Rectangle<Point> rectangle : rectangles)
            if (rectangle.contains(this))
                return true;
        return false;
    }
}