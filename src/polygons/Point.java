package polygons;

import main.AbstractPoint;
import main.Rectangle;

import java.util.List;


/**
 * Используется для хранения пиксельных координат точек (т. е. координат точек в пиксельной системе координат Oxy,
 * которая связана с термограммой).
 */
public class Point extends AbstractPoint {
    public Point(int i, int j) {
        super(i, j);
    }

    /**
     * Возвращает расстояние от текущей точки до линии {@code line}.
     *
     * @throws IllegalArgumentException если текущая точка не может быть спроектирована на внутренность линии или линия
     *                                  не является ни горизонтальной, ни вертикальной
     */
    public int distance(Line line) {
        if (projectableTo(line)) {
            if (line.isHorizontal())
                return Math.abs(i - line.getA().getI());
            if (line.isVertical())
                return Math.abs(j - line.getA().getJ());
        }
        throw new IllegalArgumentException("Текущая точка не может быть спроектирована на внутренность линии, или " +
                "линия не является ни горизонтальной, ни вертикальной.");
    }

    /**
     * Определяет возможность проектирования текущей точки на внутренность линии {@code line}.
     *
     * @throws IllegalArgumentException если линия не является ни горизонтальной, ни вертикальной
     */
    public boolean projectableTo(Line line) {
        if (line.isHorizontal())
            return j > Math.min(line.getA().getJ(), line.getB().getJ()) && j < Math.max(line.getA().getJ(), line.getB().getJ());
        if (line.isVertical())
            return i > Math.min(line.getA().getI(), line.getB().getI()) && i < Math.max(line.getA().getI(), line.getB().getI());
        throw new IllegalArgumentException("Линия не является ни горизонтальной, ни вертикальной.");
    }

    /**
     * Возвращает проекцию текущей точки на линию {@code line}.
     *
     * @throws IllegalArgumentException если текущую точку нельзя спроектировать на линию или линия не является ни
     *                                  горизонтальной, ни вертикальной
     */
    public Point project(Line line) {
        if (line.isHorizontal() && (projectableTo(line) || j == line.getA().j || j == line.getB().j))
            return new Point(line.getA().getI(), j);
        if (line.isVertical() && (projectableTo(line) || i == line.getA().i || i == line.getB().i))
            return new Point(i, line.getA().getJ());
        throw new IllegalArgumentException("Текущую точку нельзя спроектировать на линию, или линия не является ни " +
                "горизонтальной, ни вертикальной.");
    }

    /**
     * Определяет, принадлежит ли текущая точка какому-нибудь прямоугольнику из списка {@code ranges}.
     */
    public boolean isInRanges(List<Rectangle<Point>> ranges) {
        for (Rectangle<Point> range : ranges)
            if (range.contains(this))
                return true;
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Point))
            return false;
        return i == ((Point) obj).getI() && j == ((Point) obj).getJ();
    }
}