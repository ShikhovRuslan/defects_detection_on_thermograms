package main;

import org.apache.commons.lang3.ArrayUtils;
import polygons.Segment;
import polygons.Point;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.BiPredicate;
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
     * Возвращает площадь части многоугольника {@code polygon}, которая не принадлежит многоугольнику {@code overlap}.
     */
    public static double squarePolygonWithoutOverlap(Polygon<Pixel> polygon, Polygon<Pixel> overlap, double focalLength) {
        return polygon.square(focalLength) - getIntersection(polygon, overlap, focalLength).square(focalLength);
    }

    /**
     * Возвращает площадь той части многоугольника {@code polygon}, которая принадлежит {@code bigPolygon}, но не
     * принадлежит {@code overlap}. Многоугольник {@code bigPolygon} содержит многоугольник {@code overlap}.
     */
    public static double squarePolygon(Polygon<Pixel> polygon, Polygon<Pixel> overlap, Polygon<Pixel> bigPolygon,
                                       double thermogramHeight, double pixelSize, double focalLength) {

        double s1 = squarePolygonWithoutOverlap(polygon, overlap, focalLength);
        double s2 = squarePolygonWithoutOverlap(polygon, bigPolygon, focalLength);
        return Thermogram.toEarthSquare(s1 - s2, thermogramHeight, focalLength, pixelSize);
    }

    /**
     * Определяет, пересекаются ли многоугольники {@code polygon1} и {@code polygon2}.
     */
    public static boolean intersects(Polygon<Point> polygon1, Polygon<Point> polygon2, double focalLength,
                                     boolean isSloping) {
        List<Point> v1 = polygon1.getVertices();
        List<Point> v2 = polygon2.getVertices();
        Segment[] s1 = getSides(polygon1);
        Segment[] s2 = getSides(polygon2);

        for (Point v : v1)
            for (Point u : v2)
                if (v.equals(u)) return true;

        for (Point v : v1)
            for (Segment s : s2)
                if (isSloping ? new Pixel(v.getI(), v.getJ()).inSegment(
                        new Pixel(s.getA().getI(), s.getA().getJ()), new Pixel(s.getB().getI(), s.getB().getJ())) :
                        s.contains(v)) return true;
        for (Point v : v2)
            for (Segment s : s1)
                if (isSloping ? new Pixel(v.getI(), v.getJ()).inSegment(
                        new Pixel(s.getA().getI(), s.getA().getJ()), new Pixel(s.getB().getI(), s.getB().getJ())) :
                        s.contains(v)) return true;

        for (Segment side : getSides(polygon1))
            if (side.intersectsSideOf(polygon2)) return true;

        return !polygon1.verticesFrom(polygon2, focalLength).isEmpty() || !polygon2.verticesFrom(polygon1, focalLength).isEmpty();
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
                if (vertex.projectableTo(side, true) && vertex.distanceTo(side) <= distance)
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
     * которые являются вершинами развёрнутого или нулевого угла (т. е. такие вершины, у которых входящая и исходящая
     * стороны параллельны)).
     */
    private static void removeRedundantVertices(Polygon<Point> polygon) {
        polygon.removeLoops(); // чтобы удаление лишних вершин было корректным
        polygon.vertices.removeIf(vertex ->
                incomingSide(polygon, vertex).getA().getI() == outgoingSide(polygon, vertex).getB().getI() ||
                        incomingSide(polygon, vertex).getA().getJ() == outgoingSide(polygon, vertex).getB().getJ());
    }

    /**
     * Ищет перпендикуляры, опущенные из какой-либо вершины многоугольника {@code first} на сторону многоугольника
     * {@code second}, удовлетворяющие условиям:
     * <ul>
     *     <li> длина перпендикуляра не превышает {@code distance},</li>
     *     <li> перпендикуляр не пересекает никакую сторону многоугольника {@code first} по единственной точке, которая
     *     является внутренней для каждого из этих отрезков,</li>
     *     <li> внутренность перпендикуляра не содержит вершин многоугольников {@code first} и {@code second},</li>
     *     <li> если перпендикуляр имеет своим концом вершину многоугольника {@code second}, то стороны обоих
     *     многоугольников, которые перпендикулярны этому перпендикуляру и содержат его концы, должны лежать по одну
     *     сторону от перпендикуляра.</li>
     * </ul>
     * <p>
     * Возвращает массив, состоящий из найденных перпендикуляров и соответствующих им сторон многоугольника
     * {@code second}, которые перпендикулярны перпендикулярам и содержат их концы. Этот массив упорядочен по длинам
     * перпендикуляров (в порядке возрастания). (Этот массив может быть пустым.)
     */
    private static Segment[][] perpendicular(Polygon<Point> first, Polygon<Point> second, int distance) {
        var lengths = new ArrayList<Integer>();
        var perpendiculars = new ArrayList<Segment>();
        var sides = new ArrayList<Segment>();
        Segment[] sides2 = getSides(second);

        for (Point vertex : first.vertices)
            for (Segment side : sides2)
                if (vertex.projectableTo(side, true) && vertex.distanceTo(side) <= distance) {
                    boolean union = true;

                    // vertex проектируется на концы стороны side
                    if (!vertex.projectableTo(side)) {
                        Point end0 = getSides(first)[indexOfSide(first, vertex, side.isHorizontal())]
                                .getOtherEnd(vertex);
                        Point other = side.getOtherEnd(vertex.project(side));

                        union = (side.isVertical() && (min(end0.getI(), other.getI()) > vertex.getI() ||
                                max(end0.getI(), other.getI()) < vertex.getI())) ||
                                (side.isHorizontal() && (min(end0.getJ(), other.getJ()) > vertex.getJ() ||
                                        max(end0.getJ(), other.getJ()) < vertex.getJ()));
                    }

                    Segment perpendicular = new Segment(vertex, vertex.project(side));

                    union = union && !perpendicular.intersectsSideOf(first);

                    union = union &&
                            !perpendicular.containsVertexFrom(first) && !perpendicular.containsVertexFrom(second);

                    if (union) {
                        lengths.add(vertex.distanceTo(side));
                        perpendiculars.add(perpendicular);
                        sides.add(side);
                    }
                }

        Object[][] arr = new Object[lengths.size()][3];
        for (int i = 0; i < lengths.size(); i++) {
            arr[i][0] = lengths.get(i);
            arr[i][1] = perpendiculars.get(i);
            arr[i][2] = sides.get(i);
        }
        arr = Arrays.stream(arr).sorted(Comparator.comparingInt(o -> (Integer) o[0])).toArray(Object[][]::new);

        Segment[][] res = new Segment[lengths.size()][2];
        for (int i = 0; i < lengths.size(); i++) {
            res[i][0] = (Segment) arr[i][1];
            res[i][1] = (Segment) arr[i][2];
        }
        return res;
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
    public static Polygon<Point> toPolygonPoint(Polygon<Pixel> polygon, double focalLength, int resY) {
        var vertices = new ArrayList<Point>();
        for (Pixel vertex : polygon.vertices)
            vertices.add(vertex.toPoint(resY));
        return new Polygon<>(vertices, focalLength);
    }

    /**
     * Конвертирует многоугольник {@code polygon} из системы координат Oxy в систему координат c'x'y'.
     */
    public static Polygon<Pixel> toPolygonPixel(Polygon<Point> polygon, double focalLength, int resY) {
        var vertices = new ArrayList<Pixel>();
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
                    draw(toPolygonPoint(rectangle.toPolygon(0, 0, focalLength, 0), focalLength, resY), image, color);
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
            polygonsPoint.add(Polygon.toPolygonPoint(p, focalLength, resY));

        drawPolygons(polygonsPoint, overlap, forbiddenZones, color, pictureName, newPictureName, focalLength, resY);
    }

    /**
     * Выдаёт многоугольник, являющийся прямоугольником, который строится на основе текущего многоугольника путём
     * расширения/сужения сторон, перпендикулярных трубе, до длины {@code length}.
     * <p>
     * Предположения:
     * - текущий многоугольник является прямоугольником,
     * - вершины упорядочены против часовой стрелки,
     * - начальной является левая вершина в случае угла {@code pipeAngle}, отличного от {@code 0} и {@code 90}, и левая
     * верхняя вершина, в противном случае.
     *
     * @param pipeAngle угол (в град.), отсчитываемый от положительного направления оси абсцисс против часовой стрелки,
     *                  принадлежащий промежутку {@code [0,180)}
     */
    public Polygon<T> widen(double length, double pipeAngle) {
        pipeAngle *= PI / 180;
        T[] v = (T[]) new ArrayList<>(vertices).toArray(new AbstractPoint[0]);

        double sideToWiden = pipeAngle < PI / 2 ?
                sqrt(pow(v[0].getI() - v[1].getI(), 2) + pow(v[0].getJ() - v[1].getJ(), 2)) :
                sqrt(pow(v[1].getI() - v[2].getI(), 2) + pow(v[1].getJ() - v[2].getJ(), 2));

        double diff = (length - sideToWiden) / 2;

        if (pipeAngle < PI / 2) {
            v[0] = (T) v[0].create(v[0].getI() - diff * sin(pipeAngle), v[0].getJ() + diff * cos(pipeAngle));
            v[3] = (T) v[3].create(v[3].getI() - diff * sin(pipeAngle), v[3].getJ() + diff * cos(pipeAngle));
            v[1] = (T) v[1].create(v[1].getI() + diff * sin(pipeAngle), v[1].getJ() - diff * cos(pipeAngle));
            v[2] = (T) v[2].create(v[2].getI() + diff * sin(pipeAngle), v[2].getJ() - diff * cos(pipeAngle));
        } else {
            v[0] = (T) v[0].create(v[0].getI() - diff * sin(pipeAngle), v[0].getJ() + diff * cos(pipeAngle));
            v[3] = (T) v[3].create(v[3].getI() + diff * sin(pipeAngle), v[3].getJ() - diff * cos(pipeAngle));
            v[1] = (T) v[1].create(v[1].getI() - diff * sin(pipeAngle), v[1].getJ() + diff * cos(pipeAngle));
            v[2] = (T) v[2].create(v[2].getI() + diff * sin(pipeAngle), v[2].getJ() - diff * cos(pipeAngle));
        }

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
                    squarePolygonWithoutOverlap(Rectangle.toRectanglePixel(rectangle, resY)
                            .toPolygon(0, 0, 0, 0), overlap, focalLength),
                    height, focalLength, pixelSize
            ));
        return polygons;
    }

    /**
     * Возвращает многоугольник, построенный на точках, являющихся концами отрезков из массива {@code segments}.
     *
     * @throws IllegalArgumentException если не удалось создать многоугольник из-за того, что невозможно упорядочить
     *                                  отрезки
     * @see Segment#order(Segment[])
     */
    private static Polygon<Point> createPolygon(Segment[] segments, double squarePixels, double height,
                                                double focalLength, double pixelSize) {
        Segment[] sides = Segment.order(segments);
        var vertices = new ArrayList<Point>();

        for (Segment side : sides) {
            if (!vertices.contains(side.getA()))
                vertices.add(side.getA());
            if (!vertices.contains(side.getB()))
                vertices.add(side.getB());
        }

        var polygon = new Polygon<>(vertices, squarePixels, height, focalLength, pixelSize);
        removeRedundantVertices(polygon);
        return polygon;
    }

    /**
     * Возвращает подкорректированные массивы сторон многоугольников {@code first}, {@code second} и отрезок
     * {@code otherBorder}, соединяющий (наряду с {@code perpendicular}) эти многоугольники.
     * <p>
     * Отрезок {@code otherBorder} представляет собой {@code perpendicular}, сдвинутый по горизонтали (если
     * {@code perpendicular} вертикален) или по вертикали (если {@code perpendicular} горизонтален). Сдвиг
     * перпендикуляра происходит до тех пор, пока оба его конца принадлежат многоугольникам.
     * <p>
     * Отрезки {@code perpendicular} и {@code side1} вычисляются методом
     * <code>{@link #perpendicular perpendicular}(first, second, distance)</code> для некоторого значения
     * {@code distance}.
     *
     * @param first         многоугольник
     * @param second        многоугольник
     * @param perpendicular перпендикуляр
     * @param side1         сторона
     * @see Polygon#perpendicular(Polygon, Polygon, int)
     */
    private static Segment[][] getPolygonalChains(Polygon<Point> first, Polygon<Point> second,
                                                  Segment perpendicular, Segment side1) {
        Segment[] sides0 = getSides(first);
        Segment[] sides1 = getSides(second);

        Point vertex0 = perpendicular.getA();
        Point end1 = perpendicular.getB();
        int side0Index = indexOfSide(first, vertex0, side1.isHorizontal());
        int side1Index = ArrayUtils.indexOf(getSides(second), side1);

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

        // newSide0 всегда не является точкой
        if (newSide0 != null)
            sides0[side0Index] = newSide0;
        else
            sides0 = Helper.deleteWithShift(sides0, side0Index);

        // newSide11 всегда !=null
        if (!newSide11.isPointNotLine())
            sides1[side1Index] = newSide11;
        else
            sides1 = Helper.deleteWithShift(sides1, side1Index);

        if (newSide1 != null && !newSide1.isPointNotLine()) {
            Segment[] tmp = new Segment[sides1.length + 1];
            System.arraycopy(sides1, 0, tmp, 0, sides1.length);
            tmp[sides1.length] = newSide1;
            sides1 = tmp;
        }

        return new Segment[][]{sides0, sides1, new Segment[]{new Segment(otherBorder0, otherBorder1)}};
    }

    /**
     * Возвращает многоугольник, являющийся объединением многоугольников {@code first} и {@code second}, если
     * объединение возможно. В противном случае, возвращает многоугольник, являющийся точкой {@code (-1, -1)}.
     * <p>
     * Отрезки {@code perpendicular} и {@code side1} вычисляются методом
     * <code>{@link #perpendicular perpendicular}(first, second, distance)</code> для некоторого значения
     * {@code distance}.
     * <p>
     * Чтобы объединение произошло, должны быть выполнены следующие условия:
     * <ul>
     *     <li> прямоугольник, образованный отрезками {@code perpendicular} и {@code otherBoarder}, вычисляемым методом
     *     <code>{@link #getPolygonalChains getPolygonalChains}(first, second, perpendicular, side1)</code>, не должен
     *     пересекать ни один многоугольник из списка {@code polygons},</li>
     *     <li> {@code otherBoarder} не пересекает никакую сторону обоих многоугольников по единственной точке, которая
     *     является внутренней для каждого из этих отрезков,</li>
     *     <li> внутренность {@code otherBoarder} не содержит вершин обоих многоугольников.</li>
     * </ul>
     * <p>
     *
     * @param first         многоугольник
     * @param second        многоугольник
     * @param perpendicular перпендикуляр
     * @param side1         сторона
     * @param polygons      список многоугольников
     * @throws IllegalArgumentException если не удалось создать объединённый многоугольник
     * @see Polygon#perpendicular(Polygon, Polygon, int)
     * @see Polygon#getPolygonalChains(Polygon, Polygon, Segment, Segment)
     */
    private static Polygon<Point> unite(Polygon<Point> first, Polygon<Point> second, Segment perpendicular,
                                        Segment side1, List<Polygon<Point>> polygons, Polygon<Pixel> overlap,
                                        double height, double focalLength, double pixelSize, int resY) {

        Segment[][] polygonalChains = getPolygonalChains(first, second, perpendicular, side1);
        Segment otherBoarder = polygonalChains[2][0];
        Segment[] allSegments = new Segment[polygonalChains[0].length + polygonalChains[1].length + 2];
        System.arraycopy(polygonalChains[0], 0, allSegments, 0, polygonalChains[0].length);
        System.arraycopy(polygonalChains[1], 0, allSegments, polygonalChains[0].length,
                polygonalChains[1].length);
        System.arraycopy(new Segment[]{perpendicular, otherBoarder}, 0, allSegments,
                polygonalChains[0].length + polygonalChains[1].length, 2);

        Polygon<Pixel> connectingRectangle = Rectangle.toRectanglePixel(perpendicular.getA(), otherBoarder.getB(), resY)
                .toPolygon();
        Polygon<Point> connectingRectanglePoint = toPolygonPoint(connectingRectangle, focalLength, resY);

        Polygon<Point> no = new Rectangle<>(new Point(-1, -1), new Point(-1, -1)).toPolygon();

        for (Polygon<Point> p : polygons)
            // Т. к. p может быть невыпуклым, то возможен ошибочный результат true (если эти многоугольники не
            // пересекаются, но фиксируется ошибочная принадлежность вершин connectingRectanglePoint многоугольнику p).
            if (intersects(connectingRectanglePoint, p, focalLength, false)) return no;

        if (otherBoarder.containsVertexFrom(first) || otherBoarder.containsVertexFrom(second))
            return no;

        if (otherBoarder.intersectsSideOf(first) || otherBoarder.intersectsSideOf(second))
            return no;

        return createPolygon(allSegments, first.pixelSquare + second.pixelSquare +
                        squarePolygonWithoutOverlap(connectingRectangle, overlap, focalLength),
                height, focalLength, pixelSize);
    }

    /**
     * Возвращает многоугольник, являющийся объединением многоугольников first и second, если объединение возможно. В
     * противном случае, возвращает многоугольник, являющийся точкой (-1, -1).
     *
     * @see Polygon#perpendicular(Polygon, Polygon, int)
     * @see Polygon#unite(Polygon, Polygon, Segment, Segment, List, Polygon, double, double, double, int)
     */
    private static Polygon<Point> unite2(Polygon<Point> first, Polygon<Point> second, int distance,
                                         List<Polygon<Point>> polygons, Polygon<Pixel> overlap, double height,
                                         double focalLength, double pixelSize, int resY) {

        Segment[][] segments = perpendicular(first, second, distance);

        for (Segment[] segment : segments) {
            if (segment[0].isPointNotLine()) {
                System.out.println("Проблема в Polygon.unite2(): перпендикуляр имеет длину 0 (равен " + segment[0] +
                        ").\n" + "Сторона, вычисляемая методом Polygon.perpendicular(): " + segment[1] + ".\n" +
                        "Берём другой перпендикуляр, если он есть, чтобы попытаться объединить многоугольники.");
                continue;
            }

            Polygon<Point> unitedPolygon;
            try {
                unitedPolygon = unite(first, second, segment[0], segment[1], polygons, overlap, height,
                        focalLength, pixelSize, resY);
            } catch (Exception e) {
                System.out.println("Проблема в Polygon.unite2(): ошибка в Polygon.unite().\n" +
                        "Перпендикуляр: " + segment[0] + ",\n" +
                        "сторона, вычисляемая методом Polygon.perpendicular(): " + segment[1] + ".\n" +
                        "Берём другой перпендикуляр, если он есть, чтобы попытаться объединить многоугольники.");
                continue;
            }

            if (!unitedPolygon.vertices.get(0).equals(new Point(-1, -1)))
                return unitedPolygon;
        }

        return new Rectangle<>(new Point(-1, -1), new Point(-1, -1)).toPolygon();
    }

    /**
     * Возвращает список многоугольников, полученный путём объединения многоугольников из списка {@code polygons}.
     * Условия, при выполнении которых происходит объединение, перечислены в методах
     * {@link Polygon#perpendicular(Polygon, Polygon, int)} и
     * {@link Polygon#unite(Polygon, Polygon, Segment, Segment, List, Polygon, double, double, double, int)}.
     *
     * @see Polygon#perpendicular(Polygon, Polygon, int)
     * @see Polygon#unite(Polygon, Polygon, Segment, Segment, List, Polygon, double, double, double, int)
     */
    private static List<Polygon<Point>> toBiggerPolygons(List<Polygon<Point>> polygons, int distance,
                                                         Polygon<Pixel> overlap, String thermogramName, double height,
                                                         double focalLength, double pixelSize, int resY,
                                                         BiPredicate<Polygon<Point>, Polygon<Point>> condition) {
        var newPolygons = new ArrayList<Polygon<Point>>();
        var processed = new ArrayList<Integer>();

        for (int i = 0; i < polygons.size(); i++)
            if (!Helper.isIn(processed, i)) {
                int j;
                for (j = i + 1; j < polygons.size(); j++)
                    if (!Helper.isIn(processed, j)) {
                        if (!(condition == null || condition.test(polygons.get(i), polygons.get(j))))
                            continue;

                        int ii = i;
                        int jj = j;
                        List<Integer> indices = IntStream.rangeClosed(0, polygons.size() - 1)
                                .boxed().collect(Collectors.toList()).stream()
                                .filter(k -> k > ii && k != jj && !Helper.isIn(processed, k))
                                .collect(Collectors.toList());

                        var polygonsNotProcessed = new ArrayList<Polygon<Point>>();
                        for (int k : indices)
                            polygonsNotProcessed.add(polygons.get(k));

                        List<Polygon<Point>> polygonsTmp =
                                Stream.concat(newPolygons.stream(), polygonsNotProcessed.stream())
                                        .collect(Collectors.toList());

                        // Пытаемся объединить многоугольники i и j.
                        Polygon<Point> unitedPolygon = unite2(polygons.get(i), polygons.get(j), distance, polygonsTmp,
                                overlap, height, focalLength, pixelSize, resY);

                        // Если многоугольники i и j не объединились, то пытаемся объединить их в другом порядке.
                        if (unitedPolygon.vertices.get(0).equals(new Point(-1, -1)))
                            unitedPolygon = unite2(polygons.get(j), polygons.get(i), distance, polygonsTmp, overlap,
                                    height, focalLength, pixelSize, resY);

                        if (!unitedPolygon.vertices.get(0).equals(new Point(-1, -1))) {
                            newPolygons.add(unitedPolygon);
                            processed.add(j);
                            break;
                        }
                    }
                // Если не смогли найти пару i-му многоугольнику, то просто его добавляем.
                if (j == polygons.size())
                    newPolygons.add(polygons.get(i));
            }

        return newPolygons;
    }

    /**
     * Укрупняет итеративно список многоугольников {@code polygons} до тех пор, пока укрупнения возможны.
     *
     * @return список укрупнённых многоугольников
     */
    public static List<Polygon<Point>> enlargeIteratively(List<Polygon<Point>> polygons, int distance,
                                                          Polygon<Pixel> overlap, String thermogramName, double height,
                                                          double focalLength, double pixelSize, int resY,
                                                          BiPredicate<Polygon<Point>, Polygon<Point>> condition) {
        List<Polygon<Point>> newPolygons = polygons;
        List<Polygon<Point>> prevPolygons;
        int count = -1; // число итераций, приводящих к укрупнению
        var sizes = new ArrayList<Integer>(); // размеры первоначального и всех последующих списков многоугольников
        do {
            prevPolygons = newPolygons;
            try {
                newPolygons = toBiggerPolygons(prevPolygons, distance, overlap, thermogramName, height, focalLength,
                        pixelSize, resY, condition);
            } catch (Exception e) {
                System.out.println("Проблема на итерации " + (count + 2) + " в Polygon.enlargeIteratively(): " +
                        "ошибка в Polygon.toBiggerPolygons().\n" +
                        "Термограмма: " + thermogramName + ".");
                e.printStackTrace();
                System.out.println();
                break;
            }
            count++;
            sizes.add(prevPolygons.size());
        } while (newPolygons.size() < prevPolygons.size());
        //System.out.println(count);
        //System.out.println(Arrays.toString(sizes.toArray()));
        return prevPolygons;
    }

    /**
     * Возвращает массив длины 2 строк, каждая из которых содержит индексы двух вершин (в порядке возрастания),
     * образующих стороны прямоугольника, параллельные трубе.
     * Вершины прямоугольника упорядочены, как описано в методе {@link Polygon#widen(double, double)}.
     *
     * @see Polygon#widen
     */
    public static String[] sidesParallel(double pipeAngle) {
        return pipeAngle < 90 ? new String[]{"03", "12"} : new String[]{"01", "23"};
    }

    /**
     * Возвращает массив длины 2 строк, каждая из которых содержит индексы двух вершин (в порядке возрастания),
     * образующих стороны прямоугольника, перпендикулярные трубе.
     * Вершины прямоугольника упорядочены, как описано в методе {@link Polygon#widen(double, double)}.
     *
     * @see Polygon#widen
     */
    public static String[] sidesPerpendicular(double pipeAngle) {
        return pipeAngle < 90 ? new String[]{"01", "23"} : new String[]{"03", "12"};
    }

    /**
     * Определяет, являются ли точки {@code v1} и {@code v2} границами стороны прямоугольника {@code polygon},
     * параллельной трубе.
     * Вершины прямоугольника упорядочены, как описано в методе {@link Polygon#widen(double, double)}.
     *
     * @see Polygon#widen
     */
    public boolean sideParallelToPipe(T v1, T v2, double pipeAngle) {
        int v1Index = getVertices().indexOf(v1);
        int v2Index = getVertices().indexOf(v2);
        return Arrays.asList(sidesParallel(pipeAngle)).contains("" + min(v1Index, v2Index) + max(v1Index, v2Index));
    }

    /**
     * Определяет, являются ли точки {@code v1} и {@code v2} границами стороны прямоугольника {@code polygon},
     * перпендикулярной трубе.
     * Вершины прямоугольника упорядочены, как описано в методе {@link Polygon#widen(double, double)}.
     *
     * @see Polygon#widen
     */
    public boolean sidePerpendicularToPipe(T v1, T v2, double pipeAngle) {
        int v1Index = getVertices().indexOf(v1);
        int v2Index = getVertices().indexOf(v2);
        return Arrays.asList(sidesPerpendicular(pipeAngle)).contains("" + min(v1Index, v2Index) + max(v1Index, v2Index));
    }

    /**
     * Прямоугольник {@code p1} имеет хотя бы одну вершину, принадлежащую прямоугольнику {@code p2}. Точка
     * {@code vertex1} - одна из таких вершин. Находит сторону прямоугольника {@code p1}, которая перпендикулярна трубе
     * и имеет вершину {@code vertex1} свом концом.
     * <p>
     * Если площадь пересечения {@code <= minSquare}, то возвращается массив длины {@code 1}.
     * <p>
     * Если величина сдвига {@code >=} длине стороны прямоугольника {@code p1}, которая параллельна трубе, то
     * возвращается пустой массив.
     * <p>
     * В противном случае возвращаются величина сдвига найденной стороны (который позволяет ликвидировать пересечение
     * внутренностей прямоугольников) и сама эта сторона.
     * <p>
     * Величина {@code minSquare} должна быть {@code >=0}, чтобы при пересечении нулевой площади возвращать массив длины
     * {@code 1}.
     *
     * @param p1         прямоугольник
     * @param p2         прямоугольник
     * @param pipeAngle1 угол наклона трубы, соответствующий прямоугольнику {@code p1}
     * @param vertex1    одна из вершин прямоугольника {@code p1}, принадлежащая {@code p2}
     * @param minSquare  минимальная площадь пересечения прямоугольников
     */
    public static Object[] findShift(Polygon<Pixel> p1, Polygon<Pixel> p2, double pipeAngle1,
                                     Pixel vertex1, double minSquare) {

        Polygon<Pixel> overlap = getIntersection(p1, p2, -1);

        if (overlap.square(-1) <= minSquare)
            return new Object[1];

        List<Pixel> v1 = p1.getVertices();
        String sideToShift = Arrays.stream(sidesPerpendicular(pipeAngle1))
                .filter(s -> s.contains(v1.indexOf(vertex1) + ""))
                .findFirst().orElseThrow();

        double shift = overlap.getVertices().stream()
                .mapToDouble(p -> p.distanceToLine(v1.get(sideToShift.charAt(0) - '0'), v1.get(sideToShift.charAt(1) - '0')))
                .max().orElseThrow(NoSuchElementException::new);

        // shift >= высоте прямоугольника p1 (= длине его стороны, которая параллельна трубе).
        if (shift >= AbstractPoint.distance(v1.get(0), pipeAngle1 < 90 ? v1.get(3) : v1.get(1)))
            return new Object[0];

        return new Object[]{shift, sideToShift};
    }

    /**
     * Редактирует прямоугольник {@code polygon}, сдвигая сторону, перпендикулярную трубе и представленную строкой
     * {@code sideToShift}, на величину {@code shift} в сторону уменьшения площади прямоугольника. Эта строка содержит
     * индексы концов этой стороны (в порядке возрастания).
     * Вершины прямоугольника упорядочены, как описано в методе {@link Polygon#widen(double, double)}.
     *
     * @see Polygon#widen
     */
    public void shorten(double shift, String sideToShift, double pipeAngle) {
        pipeAngle *= PI / 180;
        int d = pipeAngle < PI / 2 ? (sideToShift.equals("01") ? 1 : -1) : (sideToShift.equals("12") ? 1 : -1);

        int ind1 = sideToShift.charAt(0) - '0';
        int ind2 = sideToShift.charAt(1) - '0';
        T v1 = getVertices().get(ind1);
        T v2 = getVertices().get(ind2);

        getVertices().set(ind1,
                (T) v1.create(v1.getI() + d * shift * cos(pipeAngle), v1.getJ() + d * shift * sin(pipeAngle)));
        getVertices().set(ind2,
                (T) v2.create(v2.getI() + d * shift * cos(pipeAngle), v2.getJ() + d * shift * sin(pipeAngle)));
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