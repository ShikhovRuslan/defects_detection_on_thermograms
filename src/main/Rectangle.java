package main;

import polygons.Point;

import java.util.ArrayList;
import java.util.List;


/**
 * Содержит прямоугольник, который задан двумя противоположными вершинами, первая из которых - нижняя левая, а вторая -
 * верхняя правая. (Понятия "нижний" и "верхний" обозначают расположение относительно обычного положения двумерной
 * системы координат.)
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
     * Возвращает число точек в текущем прямоугольнике.
     */
    public int squarePixels() {
        return (right.getI() - left.getI() + 1) * (right.getJ() - left.getJ() + 1);
    }

    /**
     * Возвращает площадь части прямоугольника {@code rectangle}, которая не принадлежит многоугольнику {@code overlap}.
     */
    public static double squareRectangleWithoutOverlap(Rectangle<Pixel> rectangle, Polygon<Pixel> overlap) {
        return rectangle.square() - getIntersection(rectangle, overlap).squarePolygon();
    }

    /**
     * Определяет принадлежность точки {@code point} текущему прямоугольнику.
     */
    public boolean contains(T point) {
        return (left.getI() <= point.getI() && point.getI() <= right.getI()) &&
                (left.getJ() <= point.getJ() && point.getJ() <= right.getJ());
    }

    /**
     * Возвращает многоугольник, который является пересечением прямоугольника {@code rectangle} и многоугольника
     * {@code polygon}.
     */
    public static Polygon<Pixel> getIntersection(Rectangle<Pixel> rectangle, Polygon<Pixel> polygon) {
        List<Pixel> vertices = new ArrayList<>();
        vertices.addAll(rectangle.verticesFrom(polygon));
        vertices.addAll(polygon.verticesFrom(Rectangle.toPolygon(rectangle)));
        Pixel intersection;
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < polygon.getVertices().size(); j++) {
                intersection = Pixel.findIntersection(Rectangle.toPolygon(rectangle).getVertices().get(i),
                        Rectangle.toPolygon(rectangle).getVertices().get(i + 1 < 4 ? i + 1 : 0),
                        polygon.getVertices().get(j),
                        polygon.getVertices().get(j + 1 < polygon.getVertices().size() ? j + 1 : 0));
                if (intersection.getI() != -1)
                    vertices.add(intersection);
            }
        return new Polygon<>(AbstractPoint.order(vertices));
    }

    /**
     * Возвращает список вершин многоугольника {@code polygon}, которые принадлежат текущему прямоугольнику.
     */
    public List<T> verticesFrom(Polygon<T> polygon) {
        List<T> res = new ArrayList<>();
        for (T vertex : polygon.getVertices())
            if (contains(vertex))
                res.add(vertex);
        return res;
    }

    /**
     * Определяет, является ли текущий прямоугольник горизонтальным или вертикальным отрезком.
     */
    private boolean isSegment() {
        return left.getI() == right.getI() || left.getJ() == right.getJ();
    }

    /**
     * Конвертирует прямоугольник {@code rectangle} из системы координат Oxy в систему координат c'x'y'.
     */
    public static Rectangle<Pixel> toRectangle(Rectangle<Point> rectangle) {
        return new Rectangle<>(new Pixel(rectangle.left.getJ(), NewClass.RES_Y - rectangle.right.getI()),
                new Pixel(rectangle.right.getJ(), NewClass.RES_Y - rectangle.left.getI()));
    }

    /**
     * Определяет, пересекает ли вертикальный (относительно термограммы и горизонтальный относительно системы координат
     * Oxy) отрезок [({@code i1}, {@code j}), ({@code i2}, {@code j})] какой-нибудь прямоугольник из списка
     * {@code rectangles}.
     */
    private static boolean verticalSegmentIntersectsRectangles(int i1, int i2, int j,
                                                               List<Rectangle<Point>> rectangles) {
        for (int k = Math.min(i1, i2); k <= Math.max(i1, i2); k++)
            if (new Point(k, j).isInRectangles(rectangles))
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
     * Возвращает прямоугольник, чья верхняя (относительно термограммы) левая вершина примерно совпадает с точкой
     * {@code point}, на основании таблицы {@code table} и списка уже построенных прямоугольников {@code rectangles}.
     */
    private static Rectangle<Point> makeRectangle(int[][] table, Point point, List<Rectangle<Point>> rectangles) {
        int x = point.getI(), y = point.getJ();
        boolean incrementX, incrementY;
        do {
            incrementX = false;
            incrementY = false;
            if (x + 1 < table.length &&
                    amountOfOnes(new Rectangle<>(point, new Point(x + 1, y)), table) -
                            amountOfOnes(new Rectangle<>(point, new Point(x, y)), table) > (y - point.getJ() + 1) / 2) {
                x++;
                incrementX = true;
            }
            if (y + 1 < table[0].length &&
                    amountOfOnes(new Rectangle<>(point, new Point(x, y + 1)), table) -
                            amountOfOnes(new Rectangle<>(point, new Point(x, y)), table) > (x - point.getI() + 1) / 2 &&
                    !Rectangle.verticalSegmentIntersectsRectangles(point.getI(), x, y + 1, rectangles)) {
                y++;
                incrementY = true;
            }
        } while (incrementX || incrementY);
        int newI = point.getI();
        int newJ = point.getJ();
        if (amountOfOnes(new Rectangle<>(point, new Point(point.getI(), y)), table) < (y - point.getJ() + 1) / 2 &&
                point.getI() + 1 < table.length)
            newI++;
        if (amountOfOnes(new Rectangle<>(point, new Point(x, point.getJ())), table) < (x - point.getI() + 1) / 2 &&
                point.getJ() + 1 < table[0].length)
            newJ++;
        return new Rectangle<>(new Point(newI, newJ), new Point(x, y));
    }

    /**
     * Возвращает список прямоугольников, созданных на основании таблицы {@code table}.
     */
    public static List<Rectangle<Point>> findRectangles(int[][] table) {
        List<Rectangle<Point>> rectangles = new ArrayList<>();
        Rectangle<Point> rectangle;
        for (int i = 0; i < table.length; i++)
            for (int j = 0; j < table[0].length; j++)
                if (table[i][j] == 1 && !(new Point(i, j).isInRectangles(rectangles))) {
                    rectangle = makeRectangle(table, new Point(i, j), rectangles);
                    if (!rectangle.isSegment())
                        rectangles.add(rectangle);
                }
        return rectangles;
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