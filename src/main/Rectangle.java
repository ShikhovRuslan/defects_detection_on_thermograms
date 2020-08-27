package main;

import polygons.Point;

import java.util.ArrayList;
import java.util.List;


/**
 * Содержит прямоугольник, который задан двумя противоположными вершинами, первая из которых - левая, а вторая - правая.
 *
 * @param <T> тип вершин
 */
public class Rectangle<T extends AbstractPoint> {
    /**
     * Левая вершина.
     */
    private final T left;
    /**
     * Правая вершина.
     */
    private final T right;

    public Rectangle(T left, T right) {
        this.left = left;
        this.right = right;
    }

    public T getLeft() {
        return left;
    }

    public T getRight() {
        return right;
    }

    /**
     * Возвращает площадь текущего прямоугольника.
     */
    public double square() {
        return (right.getI() - left.getI()) * (right.getJ() - left.getJ());
    }

    /**
     * Определяет принадлежность точки {@code point} текущему прямоугольнику.
     */
    public boolean contains(T point) {
        return (left.getI() <= point.getI() && point.getI() <= right.getI()) &&
                (left.getJ() <= point.getJ() && point.getJ() <= right.getJ());
    }

    public static Polygon<Pixel> getIntersection(Rectangle<Pixel> rectangle, Polygon<Pixel> polygon) {
        List<Pixel> vertices = new ArrayList<>();
        vertices.addAll(verticesFrom(rectangle, polygon));
        vertices.addAll(polygon.verticesFrom(Rectangle.toPolygon(rectangle)));
        Pixel intersection;
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < polygon.getVertices().size(); j++) {
                intersection = Pixel.findIntersection(Rectangle.toPolygon(rectangle).getVertices().get(i), Rectangle.toPolygon(rectangle).getVertices().get(i + 1 < 4 ? i + 1 : 0),
                        polygon.getVertices().get(j), polygon.getVertices().get(j + 1 < polygon.getVertices().size() ? j + 1 : 0));
                if (intersection.getI() != -1)
                    vertices.add(intersection);
            }
        return new Polygon<Pixel>(Thermogram.order(vertices));
    }

    /**
     * Возвращает площадь части прямоугольника {@code rectangle}, которая не принадлежит многоугольнику {@code overlap}.
     */
    public static double squareRectangleWithoutOverlap(Rectangle<Pixel> rectangle, Polygon<Pixel> overlap) {
        return rectangle.square() - getIntersection(rectangle, overlap).squarePolygon();
    }

    /**
     * Возвращает список вершин многоугольника {@code polygon}, которые принадлежат прямоугольнику {@code rectangle}.
     */
    public static List<Pixel> verticesFrom(Rectangle<Pixel> rectangle, Polygon<Pixel> polygon) {
        List<Pixel> res = new ArrayList<>();
        for (Pixel vertex : polygon.getVertices())
            if (rectangle.contains(vertex))
                res.add(vertex);
        return res;
    }


    /**
     * Определяет, является ли текущий прямоугольник горизонтальной или вертикальной линией.
     */
    private boolean isLine() {
        return left.getI() == right.getI() || left.getJ() == right.getJ();
    }

    /**
     * Возвращает число точек в текущем прямоугольнике.
     */
    public int squarePixels() {
        return (right.getI() - left.getI() + 1) * (right.getJ() - left.getJ() + 1);
    }

    /**
     * Конвертирует прямоугольник {@code rectangle} из системы координат Oxy в систему координат c'x'y'.
     */
    public static Rectangle<Pixel> toRectangle(Rectangle<Point> rectangle) {
        return new Rectangle<>(new Pixel(rectangle.left.getJ(), NewClass.RES_Y - rectangle.right.getI()), new Pixel(rectangle.right.getJ(), NewClass.RES_Y - rectangle.left.getI()));
    }

    /**
     * Определяет, пересекает ли вертикальная линия с концами ({@code i0}, {@code j}) и ({@code i1}, {@code j})
     * какой-нибудь прямоугольник из списка {@code ranges}.
     */
    private static boolean verticalLineIntersectsRanges(int i0, int i1, int j, List<Rectangle<Point>> ranges) {
        for (int k = Math.min(i0, i1); k <= Math.max(i0, i1); k++)
            if (new Point(k, j).isInRanges(ranges))
                return true;
        return false;
    }

    /**
     * Возвращает число единиц в прямоугольнике {@code rectangle} из таблицы {@code table}.
     */
    private static int amountOfOnes(Rectangle<Point> rectangle, int[][] table) {
        int count = 0;
        for (int i = rectangle.left.getI(); i <= rectangle.right.getI(); i++)
            for (int j = rectangle.left.getJ(); j <= rectangle.right.getJ(); j++)
                if (table[i][j] == 1)
                    count++;
        return count;
    }

    /**
     * Возвращает прямоугольник, чья верхняя левая вершина примерно совпадает с точкой {@code point}, на основании
     * таблицы {@code table} и списка уже построенных прямоугольников {@code ranges}.
     */
    private static Rectangle<Point> makeRange(int[][] table, Point point, List<Rectangle<Point>> ranges) {
        int x = point.getI(), y = point.getJ();
        boolean incrementX, incrementY;
        do {
            incrementX = false;
            incrementY = false;
            if (x + 1 < table.length &&
                    amountOfOnes(new Rectangle<>(point, new Point(x + 1, y)), table) - amountOfOnes(new Rectangle<>(point, new Point(x, y)), table) > (y - point.getJ() + 1) / 2) {
                x++;
                incrementX = true;
            }
            if (y + 1 < table[0].length &&
                    amountOfOnes(new Rectangle<>(point, new Point(x, y + 1)), table) - amountOfOnes(new Rectangle<>(point, new Point(x, y)), table) > (x - point.getI() + 1) / 2 &&
                    !Rectangle.verticalLineIntersectsRanges(point.getI(), x, y + 1, ranges)) {
                y++;
                incrementY = true;
            }
        } while (incrementX || incrementY);
        int newI = point.getI();
        int newJ = point.getJ();
        if (amountOfOnes(new Rectangle<>(point, new Point(point.getI(), y)), table) < (y - point.getJ() + 1) / 2 && point.getI() + 1 < table.length)
            newI++;
        if (amountOfOnes(new Rectangle<>(point, new Point(x, point.getJ())), table) < (x - point.getI() + 1) / 2 && point.getJ() + 1 < table[0].length)
            newJ++;
        return new Rectangle<>(new Point(newI, newJ), new Point(x, y));
    }

    /**
     * Возвращает список прямоугольников, созданных на основании таблицы {@code table}.
     */
    public static List<Rectangle<Point>> findRanges(int[][] table) {
        List<Rectangle<Point>> ranges = new ArrayList<>();
        Rectangle<Point> range;
        for (int i = 0; i < table.length; i++)
            for (int j = 0; j < table[0].length; j++)
                if (table[i][j] == 1 && !(new Point(i, j).isInRanges(ranges))) {
                    range = makeRange(table, new Point(i, j), ranges);
                    if (!range.isLine())
                        ranges.add(range);
                }
        return ranges;
    }

    /**
     * Преобразует прямоугольник {@code rectangle} в многоугольник, создавая список его вершин, которые упорядочены
     * против часовой стрелки.
     */
    public static Polygon<Pixel> toPolygon(Rectangle<Pixel> rectangle) {
        List<Pixel> vertices = new ArrayList<>();
        vertices.add(rectangle.getLeft());
        vertices.add(new Pixel(rectangle.getRight().getI(), rectangle.getLeft().getJ()));
        vertices.add(rectangle.getRight());
        vertices.add(new Pixel(rectangle.getLeft().getI(), rectangle.getRight().getJ()));
        return new Polygon<>(vertices);
    }
}