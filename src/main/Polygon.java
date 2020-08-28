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
    private double squarePixels;

    public Polygon(List<T> vertices) {
        this.vertices = vertices;
    }

    public Polygon(List<T> vertices, double squarePixels) {
        this.vertices = vertices;
        this.squarePixels = squarePixels;
    }

    public List<T> getVertices() {
        return vertices;
    }

    /**
     * Возвращает список треугольников, из которых состоит текущий многоугольник.
     */
    public List<Triangle<T>> toTriangles() {
        List<Triangle<T>> triangles = new ArrayList<>();
        for (int k = 1; k < vertices.size() - 1; k++)
            triangles.add(new Triangle<>(Arrays.asList(vertices.get(0), vertices.get(k), vertices.get(k + 1))));
        return triangles;
    }

    @Override
    public double square() {
        double square = 0;
        for (Triangle<T> triangle : toTriangles())
            square += triangle.square();
        return square;
    }

    @Override
    public boolean contains(T point) {
        for (Triangle<T> triangle : toTriangles())
            if (triangle.contains(point))
                return true;
        return false;
    }

    /**
     * Возвращает список сторон многоугольника {@code polygon}, соединяя последовательно его вершины.
     */
    private static Segment[] getSides(Polygon<Point> polygon) {
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
                if (vertex.projectableTo(side) && vertex.distance(side) <= distance)
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
                if (vertex.projectableTo(side) && vertex.distance(side) <= distance) {
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
     * противоположную значению {@code isPerpendicularHorizontal} ориентацию, или {@code -1}, в противном случае.
     * (Ориентация стороны понимается относительно термограммы.)
     * Значение {@code -1} может быть выдано, если эта точка является вершиной развёрнутого угла.
     */
    private static int indexOfSideToShorten(Polygon<Point> polygon, Point vertex, boolean isPerpendicularHorizontal) {
        Segment[] sides = getSides(polygon);
        for (int i = 0; i < sides.length; i++)
            if ((vertex.equals(sides[i].getA()) || vertex.equals(sides[i].getB())) &&
                    (isPerpendicularHorizontal && sides[i].isVertical() ||
                            !isPerpendicularHorizontal && sides[i].isHorizontal()))
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
     * Рисует многоугольник {@code polygon}.
     */
    private static void draw(Polygon<Point> polygon, BufferedImage image, Color color) {
        Segment[] sides = getSides(polygon);
        for (Segment side : sides)
            side.draw(image, color);
    }

    /**
     * Рисует многоугольники из списка {@code polygons}.
     */
    public static void drawPolygons(List<Polygon<Point>> polygons, Color color, String pictureName, String newPictureName) {
        try {
            BufferedImage image = ImageIO.read(new File(pictureName));
            for (Polygon<Point> polygon : polygons)
                draw(polygon, image, color);
            ImageIO.write(image, "jpg", new File(newPictureName));
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public static void showSquaresPixels(List<Polygon<Point>> polygons) {
        List<Double> squaresPixels = new ArrayList<>();
        for (Polygon<Point> polygon : polygons)
            squaresPixels.add(polygon.squarePixels);
        int totalSquarePixels = 0;
        for (Double num : squaresPixels)
            totalSquarePixels += num;
        System.out.printf("%d pi^2  -  %s%n", totalSquarePixels, "суммарная площадь дефектов");
        System.out.printf("%.2f %%  -  %s%n", (totalSquarePixels + 0.) / (Helper.RES_I * Helper.RES_J) * 100,
                "доля суммарной площади дефектов от общей площади");
        System.out.printf("%s%n%s", "Площади дефектов (в кв. пикселях):", Arrays.toString(squaresPixels.toArray()));
    }

    /**
     * Возвращает список многоугольников, построенных на основе прямоугольников из списка {@code rectangles}.
     */
    public static List<Polygon<Point>> toPolygons(List<Rectangle<Point>> rectangles, Polygon<Pixel> overlap) {
        List<Polygon<Point>> polygons = new ArrayList<>();
        for (Rectangle<Point> rectangle : rectangles)
            polygons.add(toPolygon(rectangle, overlap));
        return polygons;
    }

    /**
     * Возвращает многоугольник, построенный на основе прямоугольника {@code rectangle}.
     */
    private static Polygon<Point> toPolygon(Rectangle<Point> rectangle, Polygon<Pixel> overlap) {
        List<Point> vertices = new ArrayList<>();
        vertices.add(rectangle.getLeft());
        vertices.add(new Point(rectangle.getLeft().getI(), rectangle.getRight().getJ()));
        vertices.add(rectangle.getRight());
        vertices.add(new Point(rectangle.getRight().getI(), rectangle.getLeft().getJ()));
        return new Polygon<>(vertices, Rectangle.squareRectangleWithoutOverlap(Rectangle.toRectangle(rectangle), overlap));
    }

    /**
     * Возвращает многоугольник, построенный на точках, являющихся концами отрезков из массива {@code segments}.
     */
    private static Polygon<Point> createPolygon(Segment[] segments, double squarePixels) throws NullPointerException {
        Segment[] sides = Segment.order(segments);
        List<Point> points = new ArrayList<>();
        for (Segment side : sides) {
            if (!points.contains(side.getA()))
                points.add(side.getA());
            if (!points.contains(side.getB()))
                points.add(side.getB());
        }
        Polygon<Point> polygon = new Polygon<>(points, squarePixels);
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
     * @param perpendicular перпендикуляр, опущенный из вершины {@code vertex0} на внутренность стороны многоугольника
     *                      {@code polygon1}
     * @param end1          конец {@code perpendicular}, принадлежащий многоугольнику {@code polygon1}
     * @param side0Index    номер стороны многоугольника {@code polygon0}, которая имеет своим концом вершину
     *                      {@code vertex0} и имеет ориентацию, противоположную ориентации {@code perpendicular}
     * @param side1Index    номер стороны многоугольника {@code polygon1}, внутренность которой содержит точку
     *                      {@code end1}
     */
    private static Segment[][] getPolygonalChains(Polygon<Point> polygon0, Polygon<Point> polygon1, Point vertex0,
                                                  Segment perpendicular, Point end1, int side0Index, int side1Index) {
        Segment[] sides0 = getSides(polygon0);
        Segment[] sides1 = getSides(polygon1);
        Segment side0ToShorten = sides0[side0Index];
        Segment side1ToShorten = sides1[side1Index];
        Segment newSide0 = null;
        Segment newSide1 = null;
        Segment newSide11;
        Point end0 = side0ToShorten.getOtherEnd(vertex0);
        Point otherBorder0, otherBorder1;
        if (perpendicular.isHorizontal())
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
    static Polygon<Point> unite(Polygon<Point> first, Polygon<Point> second, int distance, Polygon<Pixel> overlap)
            throws NullPointerException {
        Segment[] segments = perpendicular(first, second, distance);
        Segment perpendicular = segments[0];
        Point vertex0 = perpendicular.getA();
        Point end1 = perpendicular.getB();
        int side0Index = indexOfSideToShorten(first, vertex0, perpendicular.isHorizontal());
        int side1Index = indexOfSideWithPoint(second, end1);
        Segment[][] polygonalChains = getPolygonalChains(first, second, vertex0, perpendicular, end1, side0Index,
                side1Index);
        Segment otherBoarder = polygonalChains[2][0];
        Segment[] allSegments = new Segment[polygonalChains[0].length + polygonalChains[1].length + 2];
        System.arraycopy(polygonalChains[0], 0, allSegments, 0, polygonalChains[0].length);
        System.arraycopy(polygonalChains[1], 0, allSegments, polygonalChains[0].length,
                polygonalChains[1].length);
        System.arraycopy(new Segment[]{perpendicular, otherBoarder}, 0, allSegments,
                polygonalChains[0].length + polygonalChains[1].length, 2);
        double squarePixelsOfConnectingRectangle = Rectangle.squareRectangleWithoutOverlap(Rectangle.toRectangle(vertex0, otherBoarder.getB()), overlap);
        return createPolygon(allSegments,
                first.squarePixels + second.squarePixels + squarePixelsOfConnectingRectangle);
    }

    /**
     * Возвращает список многоугольников, полученный путём объединения лежащих на расстоянии, не превышающим
     * {@code distance}, многоугольников из списка {@code polygons}.
     */
    private static List<Polygon<Point>> toBiggerPolygons(List<Polygon<Point>> polygons, int distance,
                                                         Polygon<Pixel> overlap) {
        List<Polygon<Point>> newPolygons = new ArrayList<>();
        List<Integer> processed = new ArrayList<>();
        try {
            for (int i = 0; i < polygons.size(); i++)
                if (!Helper.isIn(processed, i)) {
                    int j;
                    for (j = i + 1; j < polygons.size(); j++)
                        if (!Helper.isIn(processed, j)) {
                            if (areClose(polygons.get(i), polygons.get(j), distance)) {
                                newPolygons.add(unite(polygons.get(i), polygons.get(j), distance, overlap));
                                processed.add(j);
                                break;
                            }
                            if (areClose(polygons.get(j), polygons.get(i), distance)) {
                                newPolygons.add(unite(polygons.get(j), polygons.get(i), distance, overlap));
                                processed.add(j);
                                break;
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
                                                          Polygon<Pixel> overlap) {
        List<Polygon<Point>> newPolygons = null;
        List<Polygon<Point>> prevPolygons;
        int count = -1; // число итераций, приводящих к укрупнению
        List<Integer> sizes = new ArrayList<>(); // размеры первоначального и всех последующих списков многоугольников
        do {
            prevPolygons = count >= 0 ? newPolygons : polygons;
            newPolygons = toBiggerPolygons(prevPolygons, distance, overlap);
            count++;
            sizes.add(prevPolygons.size());
        } while (newPolygons.size() < prevPolygons.size());
        //System.out.println(count);
        //System.out.println(Arrays.toString(sizes.toArray()));
        return prevPolygons;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(getClass().getName() + "<" + vertices.get(0).getClass().getName() + ">[");
        for (T vertex : vertices)
            str.append(vertex.toShortString()).append(", ");
        return str.substring(0, str.toString().length() - 2) + "]";
    }
}