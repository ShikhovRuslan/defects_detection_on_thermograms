package main;

import polygons.Point;
import polygons.Segment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.*;


/**
 * Содержит прямоугольник, который задан двумя противоположными вершинами, первая из которых - нижняя левая, а вторая -
 * верхняя правая. (Понятия "нижний" и "верхний" обозначают расположение относительно обычного положения двумерной
 * системы координат.)
 *
 * @param <T> тип вершин
 */
public class Rectangle<T extends AbstractPoint> implements Figure<T> {
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

    /**
     * Преобразует текущий прямоугольник в многоугольник, создавая список его вершин, которые упорядочены против часовой
     * стрелки, начиная с нижней левой вершины.
     */
    public Polygon<T> toPolygon(double squareRectangleWithoutOverlap, double height, double focalLength, double pixelSize) {
        List<T> vertices = new ArrayList<>();
        vertices.add(left);
        vertices.add((T) left.create(right.getI(), left.getJ()));
        vertices.add(right);
        vertices.add((T) right.create(left.getI(), right.getJ()));
        return new Polygon<>(vertices, squareRectangleWithoutOverlap, height, focalLength, pixelSize);
    }

    public Polygon<T> toPolygon() {
        return toPolygon(-1, -1, -1, -1);
    }

    public T getLeft() {
        return left;
    }

    public T getRight() {
        return right;
    }

    /**
     * Возвращает многоугольник, являющийся прямоугольником, описанным около прямоугольника {@code rectangle}, со
     * сторонами, наклонёнными под углами {@code angle} и {@code angle+90}.
     * <p>
     * Вершины в возвращаемом многоугольнике упорядочены против часовой стрелки, начиная с:
     * - левой вершины (при {@code angle} из {@code (0,90)}),
     * - левой верхней вершины (при {@code angle = 0}),
     * - левой нижней вершины (при {@code angle = 90}).
     *
     * @param angle угол (в град.), отсчитываемый от положительного направления оси c'x' против часовой стрелки,
     *              принадлежащий промежутку {@code [0,90]}
     */
    public static Polygon<Pixel> slopeRectangle(Rectangle<Pixel> rectangle, double angle, int resY) {
        Polygon<Pixel> polygon = rectangle.toPolygon(0, 0, 0, 0);
        List<Pixel> vertices = polygon.getVertices();
        Segment[] sides = Polygon.getSides(Polygon.toPolygonPoint(polygon, 0, resY));
        double a = angle * PI / 180;

        Pixel v0 = new Pixel(vertices.get(0).getI() - sides[3].length() * cos(a) * sin(a),
                vertices.get(0).getJ() + sides[3].length() * cos(a) * cos(a));
        Pixel v1 = new Pixel(vertices.get(1).getI() - sides[0].length() * cos(a) * cos(a),
                vertices.get(1).getJ() - sides[0].length() * cos(a) * sin(a));
        Pixel v2 = new Pixel(vertices.get(2).getI() + sides[1].length() * cos(a) * sin(a),
                vertices.get(2).getJ() - sides[1].length() * cos(a) * cos(a));
        Pixel v3 = new Pixel(vertices.get(3).getI() + sides[2].length() * cos(a) * cos(a),
                vertices.get(3).getJ() + sides[2].length() * cos(a) * sin(a));

        return new Polygon<>(Arrays.asList(v0, v1, v2, v3), 0);
    }

    /**
     * Определяет "почти" принадлежность текущего прямоугольника прямоугольнику {@code big}. Конкретнее, выдаёт
     * {@code true}, если первый принадлежит второму или если первый слегка выходит за рамки второго, но при этом
     * проекции векторов, соединяющих left-вершину прямоугольника {@code big} с аналогичной вершиной текущего
     * прямоугольника и right-вершину текущего прямоугольника с аналогичной вершиной прямоугольника {@code big} на
     * координатные оси не должны быть меньше отрицательной величины {@code -maxDiff}.
     */
    public boolean isIn(Rectangle<T> big, int maxDiff) {
        int leftIDiff = left.getI() - big.left.getI();
        int leftJDiff = left.getJ() - big.left.getJ();
        int rightIDiff = -right.getI() + big.right.getI();
        int rightJDiff = -right.getJ() + big.right.getJ();

        return leftIDiff >= -maxDiff & leftJDiff >= -maxDiff & rightIDiff >= -maxDiff & rightJDiff >= -maxDiff;
    }

    @Override
    public double square(double focalLength) {
        return (right.getI() - left.getI()) * (right.getJ() - left.getJ());
    }

    /**
     * Вычисляет середину текущего прямоугольника.
     */
    public T middle() {
        //return (T) left.create((left.getI() + right.getI()) / 2., (left.getJ() + right.getJ()) / 2);
        return (T) left.create((left.getI() + right.getI()) / 2, (left.getJ() + right.getJ()) / 2);
    }

    /**
     * Возвращает число точек в текущем прямоугольнике.
     */
    public int squarePixels() {
        return (right.getI() - left.getI() + 1) * (right.getJ() - left.getJ() + 1);
    }

    /**
     * Возвращает площадь части многоугольника {@code polygon}, которая не принадлежит многоугольнику {@code overlap}.
     */
    public static double squarePolygonWithoutOverlap(Polygon<Pixel> polygon, Polygon<Pixel> overlap, double focalLength) {
        return polygon.square(focalLength) - getIntersection(polygon, overlap, focalLength).square(focalLength);
    }

    @Override
    public boolean contains(T point, double focalLength) {
        return (left.getI() <= point.getI() && point.getI() <= right.getI()) &&
                (left.getJ() <= point.getJ() && point.getJ() <= right.getJ());
    }

    /**
     * Возвращает многоугольник, который является пересечением многоугольников {@code polygon1} и {@code polygon2}.
     */
    public static Polygon<Pixel> getIntersection(Polygon<Pixel> polygon1, Polygon<Pixel> polygon2, double focalLength) {
        List<Pixel> v1 = polygon1.getVertices();
        List<Pixel> v2 = polygon2.getVertices();

        List<Pixel> vertices = new ArrayList<>();

        vertices.addAll(polygon1.verticesFrom(polygon2, focalLength));
        vertices.addAll(polygon2.verticesFrom(polygon1, focalLength));

        Pixel intersection;
        for (int i = 0; i < v1.size(); i++)
            for (int j = 0; j < v2.size(); j++) {
                intersection = Pixel.findIntersection(
                        v1.get(i),
                        v1.get(i + 1 < v1.size() ? i + 1 : 0),
                        v2.get(j),
                        v2.get(j + 1 < v2.size() ? j + 1 : 0));
                if (!intersection.equals(new Pixel(Integer.MIN_VALUE, Integer.MIN_VALUE)))
                    vertices.add(intersection);
            }
        return new Polygon<>(AbstractPoint.order(vertices), focalLength);
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
    public static Rectangle<Pixel> toRectanglePixel(Rectangle<Point> rectangle, int resY) {
        return new Rectangle<>(new Point(rectangle.right.getI(), rectangle.left.getJ()).toPixel(resY),
                new Point(rectangle.left.getI(), rectangle.right.getJ()).toPixel(resY));
    }

    /**
     * Возвращает прямоугольник в системе координат c'x'y', чьими противоположными вершинами являются точки {@code p1} и
     * {@code p2} в системе координат Oxy.
     */
    public static Rectangle<Pixel> toRectanglePixel(Point p1, Point p2, int resY) {
        int[] ii = AbstractPoint.findMinAndMax(new Point[]{p1, p2}, Point::getI);
        int[] jj = AbstractPoint.findMinAndMax(new Point[]{p1, p2}, Point::getJ);

        return toRectanglePixel(new Rectangle<>(new Point(ii[0], jj[0]), new Point(ii[1], jj[1])), resY);
    }

    /**
     * Конвертирует прямоугольник {@code rectangle} из системы координат c'x'y' в систему координат Oxy.
     */
    public static Rectangle<Point> toRectangle2(Rectangle<Pixel> rectangle, int resY) {
        return new Rectangle<>(new Point(resY - rectangle.right.getJ(), rectangle.left.getI()),
                new Point(resY - rectangle.left.getJ(), rectangle.right.getI()));
    }

    /**
     * Определяет, пересекает ли вертикальный (относительно термограммы и горизонтальный относительно системы координат
     * Oxy) отрезок [({@code i1}, {@code j}), ({@code i2}, {@code j})] какой-нибудь прямоугольник из списка
     * {@code rectangles}.
     */
    private static boolean verticalSegmentIntersectsRectangles(int i1, int i2, int j,
                                                               List<Rectangle<Point>> rectangles, double focalLength) {
        for (int k = min(i1, i2); k <= max(i1, i2); k++)
            if (new Point(k, j).isInRectangles(rectangles, focalLength))
                return true;
        return false;
    }

    private static boolean horizontalSegmentIntersectsRectangles(int i, int j1, int j2,
                                                                 List<Rectangle<Point>> rectangles, double focalLength) {
        for (int k = min(j1, j2); k <= max(j1, j2); k++)
            if (new Point(i, k).isInRectangles(rectangles, focalLength))
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
    private static Rectangle<Point> makeRectangle(int[][] table, Point point, List<Rectangle<Point>> rectangles,
                                                  double focalLength) {
        int x = point.getI(), y = point.getJ();
        boolean incrementX, incrementY;
        do {
            incrementX = false;
            incrementY = false;
            if (x + 1 < table.length &&
                    amountOfOnes(new Rectangle<>(new Point(x + 1, point.getJ()), new Point(x + 1, y)), table)
                            > (y - point.getJ() + 1) / 2 &&
                    !Rectangle.horizontalSegmentIntersectsRectangles(x + 1, point.getJ(), y, rectangles, focalLength)) {
                x++;
                incrementX = true;
            }
            if (y + 1 < table[0].length &&
                    amountOfOnes(new Rectangle<>(new Point(point.getI(), y + 1), new Point(x, y + 1)), table)
                            > (x - point.getI() + 1) / 2 &&
                    !Rectangle.verticalSegmentIntersectsRectangles(point.getI(), x, y + 1, rectangles, focalLength)) {
                y++;
                incrementY = true;
            }
        } while (incrementX || incrementY);

        int newI = point.getI();
        int newJ = point.getJ();
        if (amountOfOnes(new Rectangle<>(new Point(newI, newJ), new Point(newI, y)), table) < (y - newJ + 1) / 2 &&
                newI + 1 < table.length)
            newI++;
        if (amountOfOnes(new Rectangle<>(new Point(newI, newJ), new Point(x, newJ)), table) < (x - newI + 1) / 2 &&
                newJ + 1 < table[0].length)
            newJ++;
        return new Rectangle<>(new Point(newI, newJ), new Point(x, y));
    }

    /**
     * Возвращает список прямоугольников, созданных на основании таблицы {@code table}.
     */
    public static List<Rectangle<Point>> findRectangles(int[][] table, double focalLength) {
        List<Rectangle<Point>> rectangles = new ArrayList<>();
        Rectangle<Point> rectangle;
        for (int i = 0; i < table.length; i++)
            for (int j = 0; j < table[0].length; j++)
                if (table[i][j] == 1 && !(new Point(i, j).isInRectangles(rectangles, focalLength))) {
                    rectangle = makeRectangle(table, new Point(i, j), rectangles, focalLength);
                    if (!rectangle.isSegment())
                        rectangles.add(rectangle);
                }
        return rectangles;
    }
}