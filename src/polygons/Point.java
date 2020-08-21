package polygons;

import java.util.List;


/**
 * Используется для хранения пиксельных координат точек (т. е. координат точек в пиксельной системе координат Oxy,
 * которая связана с термограммой).
 */
public class Point {
    private final int x;
    private final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    /**
     * Возвращает расстояние от текущей точки до линии {@code line}.
     *
     * @throws IllegalArgumentException если текущая точка не может быть спроектирована на внутренность линии или линия
     *                                  не является ни горизонтальной, ни вертикальной
     */
    int distance(Line line) {
        if (projectableTo(line)) {
            if (line.isHorizontal())
                return Math.abs(x - line.getA().getX());
            if (line.isVertical())
                return Math.abs(y - line.getA().getY());
        }
        throw new IllegalArgumentException("Текущая точка не может быть спроектирована на внутренность линии, или " +
                "линия не является ни горизонтальной, ни вертикальной.");
    }

    /**
     * Определяет возможность проектирования текущей точки на внутренность линии {@code line}.
     *
     * @throws IllegalArgumentException если линия не является ни горизонтальной, ни вертикальной
     */
    boolean projectableTo(Line line) {
        if (line.isHorizontal())
            return y > Math.min(line.getA().getY(), line.getB().getY()) && y < Math.max(line.getA().getY(), line.getB().getY());
        if (line.isVertical())
            return x > Math.min(line.getA().getX(), line.getB().getX()) && x < Math.max(line.getA().getX(), line.getB().getX());
        throw new IllegalArgumentException("Линия не является ни горизонтальной, ни вертикальной.");
    }

    /**
     * Возвращает проекцию текущей точки на линию {@code line}.
     *
     * @throws IllegalArgumentException если текущую точку нельзя спроектировать на линию или линия не является ни
     *                                  горизонтальной, ни вертикальной
     */
    Point project(Line line) {
        if (line.isHorizontal() && (projectableTo(line) || y == line.getA().y || y == line.getB().y))
            return new Point(line.getA().getX(), y);
        if (line.isVertical() && (projectableTo(line) || x == line.getA().x || x == line.getB().x))
            return new Point(x, line.getA().getY());
        throw new IllegalArgumentException("Текущую точку нельзя спроектировать на линию, или линия не является ни " +
                "горизонтальной, ни вертикальной.");
    }

    /**
     * Определяет, принадлежит ли текущая точка какому-нибудь прямоугольнику из списка {@code ranges}.
     */
    public boolean isInRanges(List<RectanglePoint> ranges) {
        for (RectanglePoint range : ranges)
            if (range.containsPoint(this))
                return true;
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Point))
            return false;
        return x == ((Point) obj).getX() && y == ((Point) obj).getY();
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}