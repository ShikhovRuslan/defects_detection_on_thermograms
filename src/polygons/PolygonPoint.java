package polygons;

import main.Pixel;
import main.PolygonPixel;

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
 */
public class PolygonPoint {
    /**
     * Список вершин многоугольника.
     */
    private final List<Point> vertices;
    private double squarePixels;

    public PolygonPoint(List<Point> vertices) {
        this.vertices = vertices;
    }

    public PolygonPoint(List<Point> vertices, double squarePixels) {
        this.vertices = vertices;
        this.squarePixels = squarePixels;
    }

    /**
     * Возвращает список сторон текущего многоугольника, соединяя последовательно его вершины.
     */
    private Line[] getSides() {
        Line[] sides = new Line[vertices.size()];
        for (int i = 0; i < vertices.size(); i++)
            sides[i] = new Line(vertices.get(i), vertices.get(i < vertices.size() - 1 ? i + 1 : 0));
        return sides;
    }

    /**
     * Определяет, находится ли текущий многоугольник на расстоянии, не превышающим {@code distance}, от многоугольника
     * {@code polygon}.
     * Один многоугольник находится на расстоянии, не превышающим {@code distance}, от второго многоугольника, если
     * расстояние от какой-либо вершины первого многоугольника до внутренности какой-либо стороны второго многоугольника
     * не превышает величины {@code distance}.
     */
    private boolean isCloseTo(PolygonPoint polygon, int distance) {
        Line[] sides = polygon.getSides();
        for (Point vertex : vertices)
            for (Line side : sides)
                // Специально используется сокращённый оператор AND.
                if (vertex.projectableTo(side) && vertex.distance(side) <= distance)
                    return true;
        return false;
    }

    /**
     * Возвращает сторону текущего многоугольника, которая входит в данную точку {@code vertex}.
     *
     * @throws IllegalArgumentException если эта точка не является вершиной многоугольника
     */
    private Line incomingSide(Point vertex) {
        Line[] sides = getSides();
        for (Line side : sides)
            if (side.getB().equals(vertex))
                return side;
        throw new IllegalArgumentException("Точка не является вершиной многоугольника.");
    }

    /**
     * Возвращает сторону текущего многоугольника, которая исходит из данной точки {@code vertex}.
     *
     * @throws IllegalArgumentException если эта точка не является вершиной многоугольника
     */
    private Line outgoingSide(Point vertex) {
        Line[] sides = getSides();
        for (Line side : sides)
            if (side.getA().equals(vertex))
                return side;
        throw new IllegalArgumentException("Точка не является вершиной многоугольника.");
    }

    /**
     * Удаляет петли текущего многоугольника, а также его вершины, которые являются лишними (т. е. такие вершины,
     * которые являются вершинами развёрнутого угла).
     */
    private void removeRedundantVertices() {
        removeLoops(); // чтобы удаление лишних вершин было корректным
        vertices.removeIf(vertex -> incomingSide(vertex).getA().getX() == outgoingSide(vertex).getB().getX() ||
                incomingSide(vertex).getA().getY() == outgoingSide(vertex).getB().getY());
    }

    /**
     * Возвращает индекс первого вхождения минимального числа в списке {@code list}.
     */
    private static int findIndexOfMin(List<Integer> list) {
        int index = 0;
        int min = list.get(index);
        for (int i = 1; i < list.size(); i++)
            if (list.get(i) < min) {
                index = i;
                min = list.get(index);
            }
        return index;
    }

    /**
     * Возвращает перпендикуляр минимальной длины, опущенный из какой-либо вершины текущего многоугольника на
     * внутренность какой-либо стороны многоугольника {@code polygon}, и эту сторону, если длина перпендикуляра не
     * превышает величины {@code distance}.
     * Надо вызывать этот метод, только если {@link #isCloseTo(PolygonPoint, int)} выдаёт {@code true}.
     */
    private Line[] perpendicular(PolygonPoint polygon, int distance) {
        List<Integer> tmpDistances = new ArrayList<>();
        List<Point> tmpVertices = new ArrayList<>();
        List<Line> tmpSides = new ArrayList<>();
        Line[] sides = polygon.getSides();
        for (Point vertex : vertices)
            for (Line side : sides)
                if (vertex.projectableTo(side) && vertex.distance(side) <= distance) {
                    tmpDistances.add(vertex.distance(side));
                    tmpVertices.add(vertex);
                    tmpSides.add(side);
                }
        int index = findIndexOfMin(tmpDistances);
        Point vertex0 = tmpVertices.get(index);
        Line side1 = tmpSides.get(index);
        return new Line[]{new Line(vertex0, vertex0.project(side1)), side1};
    }

    /**
     * Возвращает индекс стороны текущего многоугольника, которая имеет своим концом точку {@code vertex} и имеет
     * противоположную значению {@code isPerpendicularHorizontal} ориентацию, или {@code -1}, в противном случае.
     * Если точка {@code vertex} не является вершиной текущего многоугольника, то выдаётся значение {@code -1}. Также
     * это значение может быть выдано, если эта точка является вершиной развёрнутого угла.
     */
    private int indexOfSideToShorten(Point vertex, boolean isPerpendicularHorizontal) {
        Line[] sides = getSides();
        for (int i = 0; i < sides.length; i++)
            if ((vertex.equals(sides[i].getA()) || vertex.equals(sides[i].getB())) &&
                    (isPerpendicularHorizontal && sides[i].isVertical() ||
                            !isPerpendicularHorizontal && sides[i].isHorizontal()))
                return i;
        return -1;
    }

    /**
     * Определяет номер стороны текущего многоугольника, внутренность которой содержит точку {@code point}.
     *
     * @throws IllegalArgumentException если указанная точка не принадлежит внутренности ни одной из сторон
     *                                  многоугольника
     */
    private int indexOfSideWithPoint(Point point) {
        Line[] sides = getSides();
        for (int i = 0; i < sides.length; i++)
            if (sides[i].contains(point))
                return i;
        throw new IllegalArgumentException("Указанная точка не принадлежит внутренности ни одной из сторон " +
                "многоугольника.");
    }

    /**
     * Возвращает массив, состоящий из массива {@code array} с удалённым элементом с индексом {@code index} и со
     * сдвинутыми влево элементами.
     */
    private static Line[] deleteWithShift(Line[] array, int index) {
        Line[] result = new Line[array.length - 1];
        System.arraycopy(array, 0, result, 0, index);
        System.arraycopy(array, index + 1, result, index, array.length - index - 1);
        return result;
    }

    /**
     * Определяет принадлежность значения {@code val0} списку {@code list}.
     */
    private static boolean isIn(List<Integer> list, int val0) {
        for (Integer val : list)
            if (val == val0)
                return true;
        return false;
    }

    /**
     * Рисует текущий многоугольник.
     */
    private void draw(BufferedImage image, Color color) {
        Line[] sides = getSides();
        for (Line side : sides)
            side.draw(image, color);
    }

    /**
     * Рисует многоугольники из списка {@code polygons}.
     */
    public static void drawPolygons(List<PolygonPoint> polygons, Color color, String pictureName, String newPictureName) {
        try {
            BufferedImage image = ImageIO.read(new File(pictureName));
            for (PolygonPoint polygon : polygons)
                polygon.draw(image, color);
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
        Iterator iter = vertices.iterator();
        Point curr;
        Point prev = vertices.get(vertices.size() - 1);
        while (iter.hasNext()) {
            curr = (Point) iter.next();
            if (curr.equals(prev))
                iter.remove();
            prev = curr;
        }
    }

    /**
     * Возвращает многоугольник, построенный на основе прямоугольника {@code range}.
     */
    private static PolygonPoint convertRange(RectanglePoint range, PolygonPixel overlap) {
        List<Point> vertices = new ArrayList<>();
        vertices.add(new Point(range.getUpperLeft().getX(), range.getUpperLeft().getY()));
        vertices.add(new Point(range.getUpperLeft().getX(), range.getLowerRight().getY()));
        vertices.add(new Point(range.getLowerRight().getX(), range.getLowerRight().getY()));
        vertices.add(new Point(range.getLowerRight().getX(), range.getUpperLeft().getY()));
        return new PolygonPoint(vertices, Pixel.squareRectangleWithoutOverlap(range.toRectangle(), overlap));
    }

    public static void showSquaresPixels(List<PolygonPoint> polygons) {
        List<Double> squaresPixels = new ArrayList<>();
        for (PolygonPoint polygon : polygons)
            squaresPixels.add(polygon.squarePixels);
        int totalSquarePixels = 0;
        for (Double num : squaresPixels)
            totalSquarePixels += num;
        System.out.printf("%d pi^2  -  %s%n", totalSquarePixels, "суммарная площадь дефектов");
        System.out.printf("%.2f %%  -  %s%n", (totalSquarePixels + 0.) / (RectanglePoint.RES_I * RectanglePoint.RES_J) * 100,
                "доля суммарной площади дефектов от общей площади");
        System.out.printf("%s%n%s", "Площади дефектов (в кв. пикселях):", Arrays.toString(squaresPixels.toArray()));
    }

    /**
     * Возвращает список многоугольников, построенных на основе прямоугольников из списка {@code ranges}.
     */
    public static List<PolygonPoint> convertRanges(List<RectanglePoint> ranges, PolygonPixel overlap) {
        List<PolygonPoint> polygons = new ArrayList<>();
        for (RectanglePoint range : ranges)
            polygons.add(convertRange(range, overlap));
        return polygons;
    }

//    private boolean isInPolygon(int i0, int j0) {
//
//    }
//
//    private static boolean isInPolygons(int i0, int j0, List<Polygon> polygons) {
//        for (Polygon polygon : polygons)
//            if (polygon.isInPolygon(i0, j0))
//                return true;
//        return false;
//    }
//
//    public static int squarePixels(List<Polygon> polygons, int xMax, int yMax) {
//        int count = 0;
//        for (int i = 0; i < xMax; i++)
//            for (int j = 0; j < yMax; j++)
//                if (isInPolygons(i, j, polygons))
//                    count++;
//        return count;
//    }

    /**
     * Возвращает упорядоченный массив линий из массива {@code lines}.
     */
    private static Line[] order(Line[] lines) throws NullPointerException {
        Line[] newLines = new Line[lines.length];
        List<Integer> processed = new ArrayList<>();
        newLines[0] = lines[0];
        for (int i = 1; i < lines.length; i++)
            for (int j = 1; j < lines.length; j++)
                if (!isIn(processed, j)) {
                    if (newLines[i - 1].getB().equals(lines[j].getA())) {
                        newLines[i] = lines[j];
                        processed.add(j);
                        break;
                    }
                    if (newLines[i - 1].getB().equals(lines[j].getB())) {
                        newLines[i] = new Line(lines[j].getB(), lines[j].getA());
                        processed.add(j);
                        break;
                    }
                }
        return newLines;
    }

    /**
     * Возвращает многоугольник, построенный на точках, являющихся концами линий из массива {@code lines}.
     */
    private static PolygonPoint createPolygon(Line[] lines, double squarePixels) throws NullPointerException {
        Line[] sides = order(lines);
        List<Point> points = new ArrayList<>();
        for (Line side : sides) {
            if (!points.contains(side.getA()))
                points.add(side.getA());
            if (!points.contains(side.getB()))
                points.add(side.getB());
        }
        PolygonPoint polygon = new PolygonPoint(points, squarePixels);
        polygon.removeRedundantVertices();
        return polygon;
    }

    /**
     * Возвращает подкорректированные массивы сторон многоугольников {@code polygon0}, {@code polygon1} и линию,
     * соединяющую эти многоугольники, отличную от перпендикуляра.
     *
     * @param polygon0      многоугольник
     * @param polygon1      многоугольник
     * @param vertex0       вершина многоугольника {@code polygon0}
     * @param perpendicular перпендикуляр, опущенный из вершины {@code vertex0} на внутренность стороны многоугольника
     *                      {@code polygon1}
     * @param end1          конец {@code perpendicular}, принадлежащий многоугольнику {@code polygon1}
     * @param side0Index    индекс стороны многоугольника {@code polygon0}, которая имеет своим концом вершину
     *                      {@code vertex0} и имеет ориентацию, противоположную ориентации {@code perpendicular}
     * @param side1Index    индекс стороны многоугольника {@code polygon1}, внутренность которой содержит точку
     *                      {@code end1}
     */
    private static Line[][] getPolygonalChains(PolygonPoint polygon0, PolygonPoint polygon1, Point vertex0, Line perpendicular,
                                               Point end1, int side0Index, int side1Index) {
        Line[] sides0 = polygon0.getSides();
        Line[] sides1 = polygon1.getSides();
        Line side0ToShorten = sides0[side0Index];
        Line side1ToShorten = sides1[side1Index];
        Line newSide0 = null;
        Line newSide1 = null;
        Line newSide11;
        Point end0 = side0ToShorten.getOtherEnd(vertex0);
        Point otherBorder0, otherBorder1;
        if (perpendicular.isHorizontal())
            if (end0.getX() < vertex0.getX()) {
                if (end0.getX() < side1ToShorten.upperEnd().getX()) {
                    newSide0 = new Line(end0, new Point(side1ToShorten.upperEnd().getX(), vertex0.getY()));
                    otherBorder1 = side1ToShorten.upperEnd();
                    otherBorder0 = otherBorder1.project(side0ToShorten);
                } else {
                    newSide1 = new Line(side1ToShorten.upperEnd(), new Point(end0.getX(), end1.getY()));
                    otherBorder0 = end0;
                    otherBorder1 = otherBorder0.project(side1ToShorten);
                }
                newSide11 = new Line(end1, side1ToShorten.lowerEnd());
            } else {
                if (end0.getX() > side1ToShorten.lowerEnd().getX()) {
                    newSide0 = new Line(end0, new Point(side1ToShorten.lowerEnd().getX(), vertex0.getY()));
                    otherBorder1 = side1ToShorten.lowerEnd();
                    otherBorder0 = otherBorder1.project(side0ToShorten);
                } else {
                    newSide1 = new Line(side1ToShorten.lowerEnd(), new Point(end0.getX(), end1.getY()));
                    otherBorder0 = end0;
                    otherBorder1 = otherBorder0.project(side1ToShorten);
                }
                newSide11 = new Line(end1, side1ToShorten.upperEnd());
            }
        else if (end0.getY() < vertex0.getY()) {
            if (end0.getY() < side1ToShorten.leftEnd().getY()) {
                newSide0 = new Line(end0, new Point(vertex0.getX(), side1ToShorten.leftEnd().getY()));
                otherBorder1 = side1ToShorten.leftEnd();
                otherBorder0 = otherBorder1.project(side0ToShorten);
            } else {
                newSide1 = new Line(side1ToShorten.leftEnd(), new Point(end1.getX(), end0.getY()));
                otherBorder0 = end0;
                otherBorder1 = otherBorder0.project(side1ToShorten);
            }
            newSide11 = new Line(end1, side1ToShorten.rightEnd());
        } else {
            if (end0.getY() > side1ToShorten.rightEnd().getY()) {
                newSide0 = new Line(end0, new Point(vertex0.getX(), side1ToShorten.rightEnd().getY()));
                otherBorder1 = side1ToShorten.rightEnd();
                otherBorder0 = otherBorder1.project(side0ToShorten);
            } else {
                newSide1 = new Line(side1ToShorten.rightEnd(), new Point(end1.getX(), end0.getY()));
                otherBorder0 = end0;
                otherBorder1 = otherBorder0.project(side1ToShorten);
            }
            newSide11 = new Line(end1, side1ToShorten.leftEnd());
        }

        // newSide0 не является точкой
        if (newSide0 != null)
            sides0[side0Index] = newSide0;
        else
            sides0 = deleteWithShift(sides0, side0Index);

        // newSide11 всегда !=null и не является точкой
        sides1[side1Index] = newSide11;

        if (newSide1 != null && !newSide1.isPointNotLine()) {
            Line[] tmp = new Line[sides1.length + 1];
            System.arraycopy(sides1, 0, tmp, 0, sides1.length);
            tmp[sides1.length] = newSide1;
            sides1 = tmp;
        }

        return new Line[][]{sides0, sides1, new Line[]{new Line(otherBorder0, otherBorder1)}};
    }

    /**
     * Возвращает площадь (в кв. пикселях) прямоугольника, чьими противоположными вершинами являются точки {@code p1} и
     * {@code p2}.
     */
    private static double squarePixels(Point p1, Point p2, PolygonPixel overlap) {
        int i1 = Math.min(p1.getX(), p2.getX());
        int i2 = Math.max(p1.getX(), p2.getX());
        int j1 = Math.min(p1.getY(), p2.getY());
        int j2 = Math.max(p1.getY(), p2.getY());
        return Pixel.squareRectangleWithoutOverlap(new RectanglePoint(new Point(i1, j1), new Point(i2, j2)).toRectangle(), overlap);
    }

    /**
     * Возвращает многоугольник, являющийся объединением текущего многоугольника и многоугольника {@code polygon},
     * расстояние между которыми не превышает величины {@code distance}.
     * Надо вызывать этот метод, только если {@link #isCloseTo(PolygonPoint, int)} выдаёт {@code true}.
     *
     * @see #isCloseTo(PolygonPoint, int)
     */
    PolygonPoint uniteWith(PolygonPoint polygon, int distance, PolygonPixel overlap) throws NullPointerException {
        Line[] lines = perpendicular(polygon, distance);
        Line perpendicular = lines[0];
        Point vertex0 = perpendicular.getA();
        Point end1 = perpendicular.getB();
        int side0Index = indexOfSideToShorten(vertex0, perpendicular.isHorizontal());
        int side1Index = polygon.indexOfSideWithPoint(end1);
        Line[][] polygonalChains = getPolygonalChains(this, polygon, vertex0, perpendicular, end1, side0Index, side1Index);
        Line otherBoarder = polygonalChains[2][0];
        Line[] allLines = new Line[polygonalChains[0].length + polygonalChains[1].length + 2];
        System.arraycopy(polygonalChains[0], 0, allLines, 0, polygonalChains[0].length);
        System.arraycopy(polygonalChains[1], 0, allLines, polygonalChains[0].length, polygonalChains[1].length);
        System.arraycopy(new Line[]{perpendicular, otherBoarder}, 0, allLines, polygonalChains[0].length + polygonalChains[1].length, 2);
        double squarePixelsOfConnectingRectangle = squarePixels(vertex0, otherBoarder.getB(), overlap);
        return createPolygon(allLines, this.squarePixels + polygon.squarePixels + squarePixelsOfConnectingRectangle);
    }

    /**
     * Возвращает список многоугольников, полученный путём объединения лежащих на расстоянии, не превышающим
     * {@code distance}, многоугольников из списка {@code polygons}.
     */
    private static List<PolygonPoint> toBiggerPolygons(List<PolygonPoint> polygons, int distance, PolygonPixel overlap) {
        List<PolygonPoint> newPolygons = new ArrayList<>();
        List<Integer> processed = new ArrayList<>();
        try {
            for (int i = 0; i < polygons.size(); i++)
                if (!isIn(processed, i)) {
                    int j;
                    for (j = i + 1; j < polygons.size(); j++)
                        if (!isIn(processed, j)) {
                            if (polygons.get(i).isCloseTo(polygons.get(j), distance)) {
                                newPolygons.add(polygons.get(i).uniteWith(polygons.get(j), distance, overlap));
                                processed.add(j);
                                break;
                            }
                            if (polygons.get(j).isCloseTo(polygons.get(i), distance)) {
                                newPolygons.add(polygons.get(j).uniteWith(polygons.get(i), distance, overlap));
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
    public static List<PolygonPoint> enlargeIteratively(List<PolygonPoint> polygons, int distance, PolygonPixel overlap) {
        List<PolygonPoint> newPolygons = null;
        List<PolygonPoint> prevPolygons;
        int count = -1; // число итераций, приводящих к укрупнению
        List<Integer> sizes = new ArrayList<>(); // размеры первоначального и всех последующих списков многоугольников
        do {
            prevPolygons = count >= 0 ? newPolygons : polygons;
            newPolygons = PolygonPoint.toBiggerPolygons(prevPolygons, distance, overlap);
            count++;
            sizes.add(prevPolygons.size());
        } while (newPolygons.size() < prevPolygons.size());
        //System.out.println(count);
        //System.out.println(Arrays.toString(sizes.toArray()));
        return prevPolygons;
    }

    @Override
    public String toString() {
        return Arrays.toString(vertices.toArray());
    }
}