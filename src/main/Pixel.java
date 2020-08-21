package main;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Используется для хранения пиксельных координат точек (т. е. координат точек в пиксельной системе координат c'x'y'z',
 * которая связана с термограммой).
 * <p>
 * Под прямоугольником понимается прямоугольник, стороны которого параллельны координатным осям и который задаётся двумя
 * противоположными пикселями, нижним левым и верхним правым.
 * <p>
 * Под многоугольником понимается произвольный многоугольник, а также прямоугольник, не удовлетворяющий определению
 * выше. Многоугольник задаётся массивом упорядоченных вершин.
 */
public class Pixel {
    /**
     * Номер пикселя по оси c'x'.
     */
    private final int i;
    /**
     * Номер пикселя по оси c'y'.
     */
    private final int j;

    Pixel(int i, int j) {
        this.i = i;
        this.j = j;
    }

    Pixel(double iD, double jD) {
        this((int) Math.round(iD), (int) Math.round(jD));
    }

    int getI() {
        return i;
    }

    int getJ() {
        return j;
    }

    @Override
    public String toString() {
        return "Pixel{i=" + i + ", j=" + j + "}";
    }

    /**
     * Возвращает точку пересечения отрезков [p1, p2] и [p3, p4] в случае, если они пересекаются по единственной точке и
     * эта точка является внутренней для каждого из этих отрезков. В противном случае возвращается точка (-1,-1).
     * <p>
     * (https://vscode.ru/prog-lessons/nayti-tochku-peresecheniya-otrezkov.html)
     */
    static Pixel findIntersection(Pixel p1, Pixel p2, Pixel p3, Pixel p4) {
        Pixel no = new Pixel(-1, -1);

        // Добиваемся, чтобы было p1.i <= p2.i и p3.i <= p4.i.
        if (p2.getI() < p1.getI()) {
            Pixel tmp = p1;
            p1 = p2;
            p2 = tmp;
        }
        if (p4.getI() < p3.getI()) {
            Pixel tmp = p3;
            p3 = p4;
            p4 = tmp;
        }

        // У отрезков нет общей абсциссы или ординаты.
        if ((p2.getI() < p3.getI() || p4.getI() < p1.getI()) ||
                (max(p1.getJ(), p2.getJ()) < min(p3.getJ(), p4.getJ()) || max(p3.getJ(), p4.getJ()) < min(p1.getJ(), p2.getJ())))
            return no;

        // Отрезки либо оба вертикальные, либо оба горизонтальные.
        if (p1.getI() == p2.getI() && p3.getI() == p4.getI() || p1.getJ() == p2.getJ() && p3.getJ() == p4.getJ())
            return no;

        // Если второй отрезок вертикальный, то его делаем первым.
        if (p3.getI() == p4.getI()) {
            Pixel tmp1 = p1;
            Pixel tmp2 = p2;
            p1 = p3;
            p2 = p4;
            p3 = tmp1;
            p4 = tmp2;
        }

        //
        // Здесь второй отрезок невертикальный.
        //

        // Прямые, содержащие отрезки (в случае невертикальных отрезков):
        // y = a1*x + b1,
        // y = a2*x + b2.

        double x0, y0; // (x0, y0) - точка пересечения прямых, содержащих отрезки.
        double a2 = (p3.getJ() - p4.getJ()) / (p3.getI() - p4.getI() + 0.);
        double b2 = p3.getJ() - a2 * p3.getI();

        if (p1.getI() == p2.getI()) { // Первый отрезок вертикальный.
            x0 = p1.getI();
            y0 = a2 * x0 + b2;
            if (p3.getI() < x0 && x0 < p4.getI() &&
                    Math.min(p1.getJ(), p2.getJ()) < y0 && y0 < Math.max(p1.getJ(), p2.getJ()))
                return new Pixel(x0, y0);
        } else { // Первый отрезок невертикальный.
            double a1 = (p1.getJ() - p2.getJ()) / (p1.getI() - p2.getI() + 0.);
            double b1 = p1.getJ() - a1 * p1.getI();

            // Прямые параллельны (возможно совпадение прямых).
            if (a1 == a2)
                return no;

            x0 = (b2 - b1) / (a1 - a2);

            // Точка x0 находится на пересечении проекций внутренностей отрезков на ось абсцисс.
            if (Math.max(p1.getI(), p3.getI()) < x0 && x0 < Math.min(p2.getI(), p4.getI()))
                return new Pixel(x0, a1 * x0 + b1);
        }
        return no;
    }

    /**
     * Определяет принадлежность текущего пикселя многоугольнику или прямоугольнику {@code polygon}. (Вид фигуры
     * определяется при помощи флага {@code isRectangle}.)
     */
    private boolean isInPolygon(Pixel[] polygon, boolean isRectangle) {
        if (isRectangle)
            return (polygon[0].getI() <= i && i <= polygon[1].getI()) &&
                    (polygon[0].getJ() <= j && j <= polygon[1].getJ());
        for (Pixel[] triangle : toTriangles(polygon))
            if (isInTriangle(triangle))
                return true;
        return false;
    }

    /**
     * Преобразует прямоугольник {@code rectangle} в многоугольник, возвращая список его вершин, начиная с нижней левой
     * вершины и заканчивая верхней левой.
     */
    private static Pixel[] toPolygon(Pixel[] rectangle) {
        Pixel[] vertices = new Pixel[4];
        vertices[0] = rectangle[0];
        vertices[1] = new Pixel(rectangle[1].getI(), rectangle[0].getJ());
        vertices[2] = rectangle[1];
        vertices[3] = new Pixel(rectangle[0].getI(), rectangle[1].getJ());
        return vertices;
    }

    /**
     * Определяет принадлежность текущего пикселя треугольнику {@code triangle}.
     */
    public boolean isInTriangle(Pixel[] triangle) {
        int[] sgn = new int[3];
        for (int k = 0; k < 3; k++) {
            if (isInLine(new Pixel[]{triangle[k], triangle[k + 1 < 3 ? k + 1 : 0]}))
                return true;
            sgn[k] = (triangle[k].getI() - i) * (triangle[k + 1 < 3 ? k + 1 : 0].getJ() - triangle[k].getJ()) -
                    (triangle[k].getJ() - j) * (triangle[k + 1 < 3 ? k + 1 : 0].getI() - triangle[k].getI());
        }
        return (sgn[0] > 0 && sgn[1] > 0 && sgn[2] > 0) || (sgn[0] < 0 && sgn[1] < 0 && sgn[2] < 0);
    }

    /**
     * Определяет принадлежность текущего пикселя отрезку {@code line}.
     */
    public boolean isInLine(Pixel[] line) {
        if (line[0].getI() == line[1].getI())
            return i == line[0].getI() && min(line[0].getJ(), line[1].getJ()) <= j && j <= max(line[0].getJ(), line[1].getJ());
        // Прямая, содержащая отрезок (в случае невертикального отрезка): y = a*x + b.
        double a = (line[0].getJ() - line[1].getJ()) / (line[0].getI() - line[1].getI() + 0.);
        double b = line[0].getJ() - a * line[0].getI();
        return j == a * i + b && min(line[0].getI(), line[1].getI()) <= i && i <= max(line[0].getI(), line[1].getI());
    }

    /**
     * Возвращает список треугольников, из которых состоит многоугольник {@code polygon}.
     */
    public static List<Pixel[]> toTriangles(Pixel[] polygon) {
        List<Pixel[]> triangles = new ArrayList<>();
        for (int k = 1; k < polygon.length - 1; k++)
            triangles.add(new Pixel[]{polygon[0], polygon[k], polygon[k + 1]});
        return triangles;
    }

    /**
     * Возвращает список вершин многоугольника {@code polygonFrom}, которые принадлежат многоугольнику или
     * прямоугольнику {@code polygon}. (Вид второй фигуры определяется при помощи флага {@code isRectangle}.)
     */
    public static List<Pixel> inPolygon(Pixel[] polygonFrom, Pixel[] polygon, boolean isRectangle) {
        List<Pixel> res = new ArrayList<>();
        for (Pixel vertex : polygonFrom)
            if (vertex.isInPolygon(polygon, isRectangle))
                res.add(vertex);
        return res;
    }

    /**
     * Возвращает площадь треугольника {@code triangle}.
     */
    public static double squareTriangle(Pixel[] triangle) {
        return 0.5 * Math.abs((triangle[2].getI() - triangle[0].getI()) * (triangle[1].getJ() - triangle[0].getJ()) -
                (triangle[2].getJ() - triangle[0].getJ()) * (triangle[1].getI() - triangle[0].getI()));
    }

    /**
     * Возвращает площадь многоугольника или прямоугольника {@code polygon}. (Вид фигуры определяется при помощи флага
     * {@code isRectangle}.)
     */
    public static double squarePolygon(Pixel[] polygon, boolean isRectangle) {
        if(isRectangle)
            return (polygon[1].getI() - polygon[0].getI()) *(polygon[1].getJ() - polygon[0].getJ());
        double square = 0;
        for(Pixel[] triangle : toTriangles(polygon))
            square += squareTriangle(triangle);
        return square;
    }

    /**
     * Возвращает площадь части прямоугольника {@code rectangle}, которая не принадлежит многоугольнику {@code overlap}.
     */
    public static double squareRectangleWithoutOverlap(Pixel[] rectangle, Pixel[] overlap) {
        double d1 = squarePolygon(rectangle, true);
        Pixel[] inter = getIntersection(rectangle,overlap);
        double d2 = squarePolygon(inter, false);
        return d1 - d2;
    }

    public static Pixel[] getIntersection(Pixel[] rectangle, Pixel[] polygon) {
        List<Pixel> vertices = new ArrayList<>();
        vertices.addAll(inPolygon(polygon, rectangle, true));
        vertices.addAll(inPolygon(toPolygon(rectangle), polygon, false));
        Pixel intersection;
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < polygon.length; j++) {
                intersection = Pixel.findIntersection(toPolygon(rectangle)[i], toPolygon(rectangle)[i + 1 < 4 ? i + 1 : 0],
                        polygon[j], polygon[j + 1 < polygon.length ? j + 1 : 0]);
                if (intersection.getI() != -1)
                    vertices.add(intersection);
            }
        return Thermogram.order(vertices).toArray(new Pixel[vertices.size()]);
    }
}