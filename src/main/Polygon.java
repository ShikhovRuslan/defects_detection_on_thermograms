package main;

import polygons.Segment;
import polygons.Point;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.*;


/**
 * Содержит многоугольник, который задаётся списком упорядоченных вершин.
 *
 * @param <T> тип вершин
 */
public class Polygon<T extends AbstractPoint> implements Figure<T> {
    /**
     * Список вершин.
     */
    private final List<T> vertices;
    /**
     * Площадь (в кв. пикселях).
     */
    private final double pixelSquare;
    /**
     * Площадь (в кв. метрах).
     */
    private final double earthSquare;

    public Polygon(List<T> vertices, double focalLength) {
        this(vertices, -1, 0, focalLength, -1);
    }

    public Polygon(List<T> vertices, double pixelSquare, double height, double focalLength, double pixelSize) {
        this.vertices = vertices;
        this.pixelSquare = pixelSquare;
        this.earthSquare = Thermogram.toEarthSquare(pixelSquare, height, focalLength, pixelSize);
    }

    public List<T> getVertices() {
        return vertices;
    }

    /**
     * Вычисляет высоту текущего многоугольника.
     */
    public int height() {
        int[] j = findMinAndMax(T::getJ);
        return j[1] - j[0] + 1;
    }

    /**
     * Вычисляет ширину текущего многоугольника.
     */
    public int width() {
        int[] i = findMinAndMax(T::getI);
        return i[1] - i[0] + 1;
    }

    /**
     * Создаёт окаймляющий прямоугольник для текущего многоугольника.
     */
    public Rectangle<T> boundingRectangle() {
        int[] i = findMinAndMax(T::getI);
        int[] j = findMinAndMax(T::getJ);
        return new Rectangle<>((T) vertices.get(0).create(i[0], j[0]), (T) vertices.get(0).create(i[1], j[1]));
    }

    /**
     * Вычисляет минимальный и максимальный числа среди чисел, полученных в результате применения функции {@code f} ко
     * всем вершинам текущего многоугольника.
     */
    public int[] findMinAndMax(Function<T, Integer> f) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i++)
            indices.add(i);
        return AbstractPoint.findMinAndMax((T[]) vertices.toArray(new AbstractPoint[0]), indices, f);
    }

    /**
     * Возвращает список треугольников, из которых состоит текущий многоугольник.
     */
    public List<Triangle<T>> toTriangles(double focalLength) {
        List<Triangle<T>> triangles = new ArrayList<>();
        for (int k = 1; k < vertices.size() - 1; k++)
            triangles.add(new Triangle<>(Arrays.asList(vertices.get(0), vertices.get(k), vertices.get(k + 1)), focalLength));
        return triangles;
    }

    @Override
    public double square(double focalLength) {
        double square = 0;
        for (Triangle<T> triangle : toTriangles(focalLength))
            square += triangle.square(focalLength);
        return square;
    }

    @Override
    public boolean contains(T point, double focalLength) {
        for (Triangle<T> triangle : toTriangles(focalLength))
            if (triangle.contains(point, focalLength))
                return true;
        return false;
    }

    public static boolean intersects(Polygon<Point> polygon1, Polygon<Point> polygon2, double focalLength, int resY) {
        List<?> v1 = polygon1.getVertices();
        List<?> v2 = polygon2.getVertices();
        Segment[] s1 = getSides(polygon1);
        Segment[] s2 = getSides(polygon2);

        for (Object v : v1)
            for (Object u : v2)
                if (v.equals(u)) return true;

        for (Object v : v1)
            for (Segment s : s2)
                if (s.contains((Point) v)) return true;
        for (Object v : v2)
            for (Segment s : s1)
                if (s.contains((Point) v)) return true;

        Polygon<Pixel> p1 = Polygon.toPixelPolygon(polygon1, focalLength, resY);
        Polygon<Pixel> p2 = Polygon.toPixelPolygon(polygon2, focalLength, resY);

        v1 = p1.getVertices();
        v2 = p2.getVertices();

        for (int i = 0; i < v1.size(); i++)
            for (int j = 0; j < v2.size(); j++)
                if (!Pixel.findIntersection(
                        (Pixel) v1.get(i),
                        (Pixel) v1.get(i + 1 < v1.size() ? i + 1 : 0),
                        (Pixel) v2.get(j),
                        (Pixel) v2.get(j + 1 < v2.size() ? j + 1 : 0))
                        .equals(new Pixel(Integer.MIN_VALUE, Integer.MIN_VALUE)))
                    return true;

        return false;
    }

    /**
     * Возвращает список сторон многоугольника {@code polygon}, соединяя последовательно его вершины.
     */
    public static Segment[] getSides(Polygon<Point> polygon) {
        Segment[] sides = new Segment[polygon.vertices.size()];
        for (int i = 0; i < polygon.vertices.size(); i++)
            sides[i] = new Segment(polygon.vertices.get(i),
                    polygon.vertices.get(i < polygon.vertices.size() - 1 ? i + 1 : 0));
        return sides;
    }

    /**
     * Определяет, находится ли многоугольник {@code first} на расстоянии, не превышающим {@code distance}, от
     * многоугольника {@code second}.
     * <p>
     * Один многоугольник находится на расстоянии, не превышающим {@code distance}, от второго многоугольника, если
     * расстояние от какой-либо вершины первого многоугольника до внутренности какой-либо стороны второго многоугольника
     * не превышает величины {@code distance}.
     */
    private static boolean areClose(Polygon<Point> first, Polygon<Point> second, int distance) {
        Segment[] sides = getSides(second);
        for (Point vertex : first.vertices)
            for (Segment side : sides)
                // Специально используется сокращённый оператор AND.
                if (vertex.projectableTo(side, true) && vertex.distance(side, true) <= distance)
                    return true;
        return false;
    }

    /**
     * Возвращает сторону многоугольника {@code polygon}, которая входит в данную точку {@code vertex}.
     *
     * @throws IllegalArgumentException если эта точка не является вершиной многоугольника
     */
    private static Segment incomingSide(Polygon<Point> polygon, Point vertex) {
        Segment[] sides = getSides(polygon);
        for (Segment side : sides)
            if (side.getB().equals(vertex))
                return side;
        throw new IllegalArgumentException("Точка не является вершиной многоугольника.");
    }

    /**
     * Возвращает сторону многоугольника {@code polygon}, которая исходит из данной точки {@code vertex}.
     *
     * @throws IllegalArgumentException если эта точка не является вершиной многоугольника
     */
    private static Segment outgoingSide(Polygon<Point> polygon, Point vertex) {
        Segment[] sides = getSides(polygon);
        for (Segment side : sides)
            if (side.getA().equals(vertex))
                return side;
        throw new IllegalArgumentException("Точка не является вершиной многоугольника.");
    }

    /**
     * Удаляет петли многоугольника {@code polygon}, а также его вершины, которые являются лишними (т. е. такие вершины,
     * которые являются вершинами развёрнутого угла).
     */
    private static void removeRedundantVertices(Polygon<Point> polygon) {
        polygon.removeLoops(); // чтобы удаление лишних вершин было корректным
        polygon.vertices.removeIf(vertex -> incomingSide(polygon, vertex).getA().getI() == outgoingSide(polygon, vertex).getB().getI() ||
                incomingSide(polygon, vertex).getA().getJ() == outgoingSide(polygon, vertex).getB().getJ());
    }

    /**
     * Возвращает перпендикуляр минимальной длины, опущенный из какой-либо вершины многоугольника {@code first} на
     * внутренность какой-либо стороны многоугольника {@code second}, и эту сторону, если длина перпендикуляра не
     * превышает величины {@code distance}.
     * Надо вызывать этот метод, только если {@link #areClose(Polygon, Polygon, int)} выдаёт {@code true}.
     */
    private static Segment[] perpendicular(Polygon<Point> first, Polygon<Point> second, int distance) {
        List<Integer> tmpDistances = new ArrayList<>();
        List<Point> tmpVertices = new ArrayList<>();
        List<Segment> tmpSides = new ArrayList<>();
        Segment[] sides = getSides(second);
        for (Point vertex : first.vertices)
            for (Segment side : sides)
                if (vertex.projectableTo(side, true) && vertex.distance(side, true) <= distance) {
                    int side0Index = indexOfSide(first, vertex, side.isHorizontal());
                    Segment side0ToShorten = getSides(first)[side0Index];
                    Point end0 = side0ToShorten.getOtherEnd(vertex);
                    Point other = side.getOtherEnd(vertex.project(side));
                    if((side.isVertical() && (end0.getI() > vertex.getI() && other.getI() > vertex.getI() ||
                            end0.getI() < vertex.getI() && other.getI() < vertex.getI())) ||
                    (side.isHorizontal() && (end0.getJ() > vertex.getJ() && other.getJ() > vertex.getJ() ||
                            end0.getJ() < vertex.getJ() && other.getJ() < vertex.getJ())))
                    tmpDistances.add(vertex.distance(side));
                    tmpVertices.add(vertex);
                    tmpSides.add(side);
                }
        int index = Helper.findIndexOfMin(tmpDistances);
        Point vertex0 = tmpVertices.get(index);
        Segment side1 = tmpSides.get(index);
        return new Segment[]{new Segment(vertex0, vertex0.project(side1)), side1};
    }

    /**
     * Возвращает номер стороны многоугольника {@code polygon}, которая имеет своим концом точку {@code vertex} и имеет
     * ориентацию {@code isHorizontal}, или {@code -1}, в противном случае.
     * (Ориентация стороны понимается относительно термограммы.)
     * Значение {@code -1} может быть выдано, если эта точка является вершиной развёрнутого угла.
     */
    private static int indexOfSide(Polygon<Point> polygon, Point vertex, boolean isHorizontal) {
        Segment[] sides = getSides(polygon);
        for (int i = 0; i < sides.length; i++)
            if ((vertex.equals(sides[i].getA()) || vertex.equals(sides[i].getB())) &&
                    (isHorizontal && sides[i].isHorizontal() ||
                            !isHorizontal && sides[i].isVertical()))
                return i;
        return -1;
    }

    /**
     * Определяет номер стороны многоугольника {@code polygon}, внутренность которой содержит точку {@code point}.
     *
     * @throws IllegalArgumentException если указанная точка не принадлежит внутренности ни одной из сторон
     *                                  многоугольника
     */
    private static int indexOfSideWithPoint(Polygon<Point> polygon, Point point) {
        Segment[] sides = getSides(polygon);
        for (int i = 0; i < sides.length; i++)
            if (sides[i].contains(point))
                return i;
        throw new IllegalArgumentException("Указанная точка не принадлежит внутренности ни одной из сторон " +
                "многоугольника.");
    }

    /**
     * Конвертирует многоугольник {@code polygon} из системы координат c'x'y' в систему координат Oxy.
     */
    public static Polygon<Point> toPointPolygon(Polygon<Pixel> polygon, double focalLength, int resY) {
        List<Point> vertices = new ArrayList<>();
        for (Pixel vertex : polygon.vertices)
            vertices.add(Point.toPoint(vertex, resY));
        return new Polygon<>(vertices, focalLength);
    }

    /**
     * Конвертирует многоугольник {@code polygon} из системы координат Oxy в систему координат c'x'y'.
     */
    public static Polygon<Pixel> toPixelPolygon(Polygon<Point> polygon, double focalLength, int resY) {
        List<Pixel> vertices = new ArrayList<>();
        for (Point vertex : polygon.vertices)
            vertices.add(vertex.toPixel(resY));
        return new Polygon<>(vertices, focalLength);
    }

    /**
     * Рисует многоугольник {@code polygon}.
     */
    public static void draw(Polygon<Point> polygon, BufferedImage image, Color color) {
        Segment[] sides = getSides(polygon);
        for (Segment side : sides)
            side.draw(image, color);
    }

    /**
     * Рисует многоугольники из списка {@code polygons}.
     */
    public static void drawPolygons(List<Polygon<Point>> polygons, Polygon<Point> overlap,
                                    List<Rectangle<Pixel>> forbiddenZones, Color color,
                                    String pictureName, String newPictureName, double focalLength, int resY) {
        try {
            BufferedImage image = ImageIO.read(new File(pictureName));
            draw(overlap, image, color);
            for (Polygon<Point> polygon : polygons)
                draw(polygon, image, color);
            if (forbiddenZones != null)
                for (Rectangle<Pixel> rectangle : forbiddenZones)
                    draw(toPointPolygon(rectangle.toPolygon(0, 0, focalLength, 0), focalLength, resY), image, color);
            ImageIO.write(image, "jpg", new File(newPictureName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void drawPolygons(List<Polygon<Pixel>> polygons, Polygon<Point> overlap,
                                    List<Rectangle<Pixel>> forbiddenZones, Color color,
                                    String pictureName, String newPictureName, int resY, double focalLength) {

        var polygonsPoint = new ArrayList<Polygon<Point>>();
        for (Polygon<Pixel> p : polygons)
            polygonsPoint.add(Polygon.toPointPolygon(p, focalLength, resY));

        drawPolygons(polygonsPoint, overlap, forbiddenZones, color, pictureName, newPictureName, focalLength, resY);
    }

    /**
     * Выдаёт многоугольник, являющийся прямоугольником, который строится на основе текущего многоугольника путём
     * расширения пары противоположных сторон (отличной от другой пары сторон, которые наклонены под углом
     * {@code angle}) до длины {@code length}. Если длина этих сторон не меньше {@code length}, то выдаёт многоугольник,
     * построенный на вершинах текущего многоугольника.
     * <p>
     * Предположения:
     * - текущий многоугольник является прямоугольником,
     * - вершины упорядочены против часовой стрелки,
     * - начальной является левая вершина в случае угла {@code angle}, отличного от {@code 0} и {@code 90}, и левая
     * верхняя вершина, в противном случае.
     *
     * @param angle угол (в град.), отсчитываемый от положительного направления оси c'x' против часовой стрелки,
     *              принадлежащий промежутку {@code [0,180)}
     */
    public Polygon<T> widen(double length, double angle) {
        angle *= PI / 180;
        T[] v = (T[]) new ArrayList<>(vertices).toArray(new AbstractPoint[0]);

        double sideToWiden = angle < PI / 2 ?
                sqrt(pow(v[0].getI() - v[1].getI(), 2) + pow(v[0].getJ() - v[1].getJ(), 2)) :
                sqrt(pow(v[1].getI() - v[2].getI(), 2) + pow(v[1].getJ() - v[2].getJ(), 2));

        double diff = (length - sideToWiden) / 2;

        //if (sideToWiden < length) {
        if (angle < PI / 2) {
            v[0] = (T) v[0].create(v[0].getI() - diff * sin(angle), v[0].getJ() + diff * cos(angle));
            v[3] = (T) v[3].create(v[3].getI() - diff * sin(angle), v[3].getJ() + diff * cos(angle));
            v[1] = (T) v[1].create(v[1].getI() + diff * sin(angle), v[1].getJ() - diff * cos(angle));
            v[2] = (T) v[2].create(v[2].getI() + diff * sin(angle), v[2].getJ() - diff * cos(angle));
        } else {
            v[0] = (T) v[0].create(v[0].getI() - diff * sin(angle), v[0].getJ() + diff * cos(angle));
            v[3] = (T) v[3].create(v[3].getI() + diff * sin(angle), v[3].getJ() - diff * cos(angle));
            v[1] = (T) v[1].create(v[1].getI() - diff * sin(angle), v[1].getJ() + diff * cos(angle));
            v[2] = (T) v[2].create(v[2].getI() + diff * sin(angle), v[2].getJ() - diff * cos(angle));
        }
        //}

        return new Polygon<>(Arrays.asList(v.clone()), 0, 0, 0, 0);
    }

    /**
     * Удаляет петли у текущего многоугольника. Это означает, что если в списке {@code vertices} вершин многоугольника
     * есть подряд идущие одинаковые вершины, то остаётся только одна вершина (например, список A,B,B,B,C превратится в
     * A,B,C).
     */
    private void removeLoops() {
        Iterator<T> iter = vertices.iterator();
        T curr;
        T prev = vertices.get(vertices.size() - 1);
        while (iter.hasNext()) {
            curr = (T) iter.next();
            if (curr.equals(prev))
                iter.remove();
            prev = curr;
        }
    }

    public static void showSquares(List<Polygon<Point>> polygons, double height, double focalLength, double pixelSize, int resX, int resY) {
        List<Double> pixelSquares = new ArrayList<>();
        List<Double> earthSquares = new ArrayList<>();
        for (Polygon<Point> polygon : polygons) {
            pixelSquares.add(polygon.pixelSquare);
            earthSquares.add(polygon.earthSquare);
        }
        double totalPixelSquare = 0;
        double totalEarthSquare = 0;
        for (Double num : pixelSquares)
            totalPixelSquare += num;
        for (Double num : earthSquares)
            totalEarthSquare += num;
        System.out.printf("%n%f  -  %s%n", totalPixelSquare, "суммарная площадь дефектов, кв. п.");
        System.out.printf("%.2f %%  -  %s%n", 100 * totalPixelSquare / (resX * resY),
                "доля суммарной площади дефектов от общей площади");
        System.out.printf("%s%n%s%n", "Площади дефектов, кв. п.:", Arrays.toString(pixelSquares.toArray()));

        System.out.printf("%n%f  -  %s%n", totalEarthSquare, "суммарная площадь дефектов, кв. м.");
        System.out.printf("%.2f %%  -  %s%n", 100 * totalEarthSquare / Thermogram.toEarthSquare(resX * resY, height, focalLength, pixelSize),
                "доля суммарной площади дефектов от общей площади");
        System.out.printf("%s%n%s%n", "Площади дефектов, кв. м.:", Arrays.toString(earthSquares.toArray()));
    }

    /**
     * Возвращает список многоугольников, построенных на основе прямоугольников из списка {@code rectangles}.
     */
    public static List<Polygon<Point>> toPolygons(List<Rectangle<Point>> rectangles, Polygon<Pixel> overlap, double height, double focalLength, double pixelSize, int resY) {
        List<Polygon<Point>> polygons = new ArrayList<>();
        for (Rectangle<Point> rectangle : rectangles)
            polygons.add(rectangle.toPolygon(
                    Rectangle.squarePolygonWithoutOverlap(Rectangle.toRectangle(rectangle, resY)
                            .toPolygon(0, 0, 0, 0), overlap, focalLength),
                    height, focalLength, pixelSize
            ));
        return polygons;
    }

    /**
     * Возвращает многоугольник, построенный на точках, являющихся концами отрезков из массива {@code segments}.
     */
    private static Polygon<Point> createPolygon(Segment[] segments, double squarePixels, double height, double focalLength, double pixelSize) throws NullPointerException {
        Segment[] sides = Segment.order(segments);
        List<Point> points = new ArrayList<>();
        for (Segment side : sides) {
            if (!points.contains(side.getA()))
                points.add(side.getA());
            if (!points.contains(side.getB()))
                points.add(side.getB());
        }
        Polygon<Point> polygon = new Polygon<>(points, squarePixels, height, focalLength, pixelSize);
        removeRedundantVertices(polygon);
        return polygon;
    }

    /**
     * Возвращает подкорректированные массивы сторон многоугольников {@code polygon0}, {@code polygon1} и отрезок,
     * соединяющий эти многоугольники, отличный от перпендикуляра. (Ориентация стороны понимается относительно
     * термограммы.)
     *
     * @param polygon0      многоугольник
     * @param polygon1      многоугольник
     * @param vertex0       вершина многоугольника {@code polygon0}
     * @param end1          конец {@code perpendicular}, принадлежащий многоугольнику {@code polygon1}
     * @param side0Index    номер стороны многоугольника {@code polygon0}, которая имеет своим концом вершину
     *                      {@code vertex0} и имеет ориентацию, противоположную ориентации {@code perpendicular}
     * @param side1Index    номер стороны многоугольника {@code polygon1}, внутренность которой содержит точку
     *                      {@code end1}
     */
    private static Segment[][] getPolygonalChains(Polygon<Point> polygon0, Polygon<Point> polygon1, Point vertex0,
                                                  Point end1, int side0Index, int side1Index) {
        Segment[] sides0 = getSides(polygon0);
        Segment[] sides1 = getSides(polygon1);
        Segment side0ToShorten = sides0[side0Index];
        Segment side1ToShorten = sides1[side1Index];
        Segment newSide0 = null;
        Segment newSide1 = null;
        Segment newSide11;
        Point end0 = side0ToShorten.getOtherEnd(vertex0);
        Point otherBorder0, otherBorder1;
        if (side1ToShorten.isVertical())
            if (end0.getI() < vertex0.getI()) {
                if (end0.getI() < side1ToShorten.upperEnd().getI()) {
                    newSide0 = new Segment(end0, new Point(side1ToShorten.upperEnd().getI(), vertex0.getJ()));
                    otherBorder1 = side1ToShorten.upperEnd();
                    otherBorder0 = otherBorder1.project(side0ToShorten);
                } else {
                    newSide1 = new Segment(side1ToShorten.upperEnd(), new Point(end0.getI(), end1.getJ()));
                    otherBorder0 = end0;
                    otherBorder1 = otherBorder0.project(side1ToShorten);
                }
                newSide11 = new Segment(end1, side1ToShorten.lowerEnd());
            } else {
                if (end0.getI() > side1ToShorten.lowerEnd().getI()) {
                    newSide0 = new Segment(end0, new Point(side1ToShorten.lowerEnd().getI(), vertex0.getJ()));
                    otherBorder1 = side1ToShorten.lowerEnd();
                    otherBorder0 = otherBorder1.project(side0ToShorten);
                } else {
                    newSide1 = new Segment(side1ToShorten.lowerEnd(), new Point(end0.getI(), end1.getJ()));
                    otherBorder0 = end0;
                    otherBorder1 = otherBorder0.project(side1ToShorten);
                }
                newSide11 = new Segment(end1, side1ToShorten.upperEnd());
            }
        else if (end0.getJ() < vertex0.getJ()) {
            if (end0.getJ() < side1ToShorten.leftEnd().getJ()) {
                newSide0 = new Segment(end0, new Point(vertex0.getI(), side1ToShorten.leftEnd().getJ()));
                otherBorder1 = side1ToShorten.leftEnd();
                otherBorder0 = otherBorder1.project(side0ToShorten);
            } else {
                newSide1 = new Segment(side1ToShorten.leftEnd(), new Point(end1.getI(), end0.getJ()));
                otherBorder0 = end0;
                otherBorder1 = otherBorder0.project(side1ToShorten);
            }
            newSide11 = new Segment(end1, side1ToShorten.rightEnd());
        } else {
            if (end0.getJ() > side1ToShorten.rightEnd().getJ()) {
                newSide0 = new Segment(end0, new Point(vertex0.getI(), side1ToShorten.rightEnd().getJ()));
                otherBorder1 = side1ToShorten.rightEnd();
                otherBorder0 = otherBorder1.project(side0ToShorten);
            } else {
                newSide1 = new Segment(side1ToShorten.rightEnd(), new Point(end1.getI(), end0.getJ()));
                otherBorder0 = end0;
                otherBorder1 = otherBorder0.project(side1ToShorten);
            }
            newSide11 = new Segment(end1, side1ToShorten.leftEnd());
        }

        // newSide0 не является точкой
        if (newSide0 != null)
            sides0[side0Index] = newSide0;
        else
            sides0 = Helper.deleteWithShift(sides0, side0Index);

        // newSide11 всегда !=null и не является точкой
        sides1[side1Index] = newSide11;

        if (newSide1 != null && !newSide1.isPointNotLine()) {
            Segment[] tmp = new Segment[sides1.length + 1];
            System.arraycopy(sides1, 0, tmp, 0, sides1.length);
            tmp[sides1.length] = newSide1;
            sides1 = tmp;
        }

        return new Segment[][]{sides0, sides1, new Segment[]{new Segment(otherBorder0, otherBorder1)}};
    }

    /**
     * Возвращает многоугольник, являющийся объединением многоугольников {@code first} и {@code second}, расстояние
     * между которыми не превышает величины {@code distance}.
     * <p>
     * Надо вызывать этот метод, только если {@link #areClose(Polygon, Polygon, int)} выдаёт {@code true}.
     *
     * @see #areClose(Polygon, Polygon, int)
     */
    private static Polygon<Point> unite(Polygon<Point> first, Polygon<Point> second, List<Polygon<Point>> polygons,
                                        int distance, Polygon<Pixel> overlap, double height, double focalLength,
                                        double pixelSize, int resY)
            throws NullPointerException {

        Segment[] segments = perpendicular(first, second, distance);
        Segment perpendicular = segments[0];
        Segment side1 = segments[1];
        Point vertex0 = perpendicular.getA();
        Point end1 = perpendicular.getB();
        int side0Index = indexOfSide(first, vertex0, side1.isHorizontal());
        int side1Index = indexOfSideWithPoint(second, end1);
        Segment[][] polygonalChains = getPolygonalChains(first, second, vertex0, end1, side0Index,
                side1Index);
        Segment otherBoarder = polygonalChains[2][0];
        Segment[] allSegments = new Segment[polygonalChains[0].length + polygonalChains[1].length + 2];
        System.arraycopy(polygonalChains[0], 0, allSegments, 0, polygonalChains[0].length);
        System.arraycopy(polygonalChains[1], 0, allSegments, polygonalChains[0].length,
                polygonalChains[1].length);
        System.arraycopy(new Segment[]{perpendicular, otherBoarder}, 0, allSegments,
                polygonalChains[0].length + polygonalChains[1].length, 2);

        Polygon<Pixel> connectingRectangle = Rectangle.toRectangle(vertex0, otherBoarder.getB(), resY).toPolygon();
        Polygon<Point> connRectPoint = toPointPolygon(connectingRectangle, focalLength, resY);

        Polygon<Point> no = new Rectangle<>(new Point(-1, -1), new Point(-1, -1)).toPolygon();

        for (Polygon<Point> p : polygons)
            if (intersects(connRectPoint, p, focalLength, resY)) return no;

        if (perpendicular.containsVerticesFrom(first) || perpendicular.containsVerticesFrom(second) ||
                otherBoarder.containsVerticesFrom(first) || otherBoarder.containsVerticesFrom(second))
            return no;

        if(perpendicular.intersectsSideOf(first)
                || otherBoarder.intersectsSideOf(first) || otherBoarder.intersectsSideOf(second))
            return no;

        return createPolygon(allSegments,
                first.pixelSquare + second.pixelSquare +
                        Rectangle.squarePolygonWithoutOverlap(connectingRectangle, overlap, focalLength),
                height, focalLength, pixelSize);
    }

    /**
     * Возвращает список многоугольников, полученный путём объединения лежащих на расстоянии, не превышающим
     * {@code distance}, многоугольников из списка {@code polygons}.
     */
    private static List<Polygon<Point>> toBiggerPolygons(List<Polygon<Point>> polygons, int distance,
                                                         Polygon<Pixel> overlap, double height, double focalLength,
                                                         double pixelSize, int resY) {

        var newPolygons = new ArrayList<Polygon<Point>>();
        var processed = new ArrayList<Integer>();

        try {
            for (int i = 0; i < polygons.size(); i++)
                if (!Helper.isIn(processed, i)) {
                    int j;
                    for (j = i + 1; j < polygons.size(); j++)
                        if (!Helper.isIn(processed, j)) {
                            int ii = i;
                            int jj = j;
                            List<Integer> indices = IntStream.rangeClosed(0, polygons.size() - 1)
                                    .boxed().collect(Collectors.toList()).stream()
                                    .filter(k -> k > ii && k != jj && !Helper.isIn(processed, k))
                                    .collect(Collectors.toList());

                            var polygonsNotProcessed = new ArrayList<Polygon<Point>>();
                            for (int k : indices)
                                polygonsNotProcessed.add(polygons.get(k));

                            if (areClose(polygons.get(i), polygons.get(j), distance)) {
                                Polygon<Point> unitedPolygon = unite(
                                        polygons.get(i), polygons.get(j),
                                        Stream.concat(newPolygons.stream(), polygonsNotProcessed.stream())
                                                .collect(Collectors.toList()),
                                        distance, overlap, height, focalLength, pixelSize, resY);
                                if (!unitedPolygon.vertices.get(0).equals(new Point(-1, -1))) {
                                    newPolygons.add(unitedPolygon);
                                    processed.add(j);
                                    break;
                                }
                            }
                            if (areClose(polygons.get(j), polygons.get(i), distance)) {
                                Polygon<Point> unitedPolygon = unite(
                                        polygons.get(j), polygons.get(i),
                                        Stream.concat(newPolygons.stream(), polygonsNotProcessed.stream())
                                                .collect(Collectors.toList()),
                                        distance, overlap, height, focalLength, pixelSize, resY);
                                if (!unitedPolygon.vertices.get(0).equals(new Point(-1, -1))) {
                                    newPolygons.add(unitedPolygon);
                                    processed.add(j);
                                    break;
                                }
                            }
                        }
                    // Если не смогли найти пару i-му многоугольнику, то просто его добавляем.
                    if (j == polygons.size())
                        newPolygons.add(polygons.get(i));
                }
        } catch (NullPointerException e) {
            System.out.println("NullPointerException in Polygon.toBiggerPolygons().");
            e.printStackTrace();
        }
        return newPolygons;
    }

    /**
     * Укрупняет итеративно список многоугольников {@code polygons} до тех пор, пока укрупнения возможны.
     *
     * @return список укрупнённых многоугольников
     */
    public static List<Polygon<Point>> enlargeIteratively(List<Polygon<Point>> polygons, int distance,
                                                          Polygon<Pixel> overlap, double height, double focalLength, double pixelSize, int resY) {
        List<Polygon<Point>> newPolygons = null;
        List<Polygon<Point>> prevPolygons;
        int count = -1; // число итераций, приводящих к укрупнению
        List<Integer> sizes = new ArrayList<>(); // размеры первоначального и всех последующих списков многоугольников
        do {
            prevPolygons = count >= 0 ? newPolygons : polygons;
            newPolygons = toBiggerPolygons(prevPolygons, distance, overlap, height, focalLength, pixelSize, resY);
            count++;
            sizes.add(prevPolygons.size());
        } while (newPolygons.size() < prevPolygons.size());
        //System.out.println(count);
        //System.out.println(Arrays.toString(sizes.toArray()));
        return prevPolygons;
    }

    @Override
    public String toString() {
        String str1 = "";
        String str2 = "<empty>";
        if (vertices.size() > 0) {
            str1 = vertices.get(0).getClass().getName();
            StringBuilder str = new StringBuilder();
            for (T vertex : vertices)
                str.append(vertex.toShortString()).append(", ");
            str2 = str.substring(0, str.toString().length() - 2);
        }
        return getClass().getName() + "<" + str1 + ">" + "[" + str2 + "]";
    }
}