package main;

import polygons.Polygon;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;


/**
 * Используется для хранения пиксельных координат точек (т. е. координат точек в пиксельной системе координат c'x'y'z',
 * которая связана с термограммой).
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

    public Pixel(int i, int j) {
        this.i = i;
        this.j = j;
    }

    public Pixel(double iD, double jD) {
        this((int) Math.round(iD), (int) Math.round(jD));
    }

    int getI() {
        return i;
    }

    int getJ() {
        return j;
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
     * Определяет принадлежность текущего пикселя многоугольнику {@code polygon}.
     */
    private boolean isInPolygon(PolygonPixel polygon) {
        for (PolygonPixel triangle : polygon.toTriangles())
            if (isInTriangle(triangle))
                return true;
        return false;
    }

    /**
     * Преобразует прямоугольник {@code rectangle} в многоугольник, возвращая список его вершин, начиная с нижней левой
     * вершины и заканчивая верхней левой.
     */
    private static Pixel[] toPolygon(Rectangle rectangle) {
        Pixel[] vertices = new Pixel[4];
        vertices[0] = rectangle.getLowerLeft();
        vertices[1] = new Pixel(rectangle.getUpperRight().getI(), rectangle.getLowerLeft().getJ());
        vertices[2] = rectangle.getUpperRight();
        vertices[3] = new Pixel(rectangle.getLowerLeft().getI(), rectangle.getUpperRight().getJ());
        return vertices;
    }

    /**
     * Определяет принадлежность текущего пикселя треугольнику {@code triangle}.
     */
    public boolean isInTriangle(PolygonPixel triangle) {
        int[] sgn = new int[3];
        for (int k = 0; k < 3; k++) {
            if (isInLine(new Pixel[]{triangle.getVertices().get(k), triangle.getVertices().get(k + 1 < 3 ? k + 1 : 0)}))
                return true;
            sgn[k] = (triangle.getVertices().get(k).getI() - i) * (triangle.getVertices().get(k + 1 < 3 ? k + 1 : 0).getJ() - triangle.getVertices().get(k).getJ()) -
                    (triangle.getVertices().get(k).getJ() - j) * (triangle.getVertices().get(k + 1 < 3 ? k + 1 : 0).getI() - triangle.getVertices().get(k).getI());
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
     * Возвращает список вершин многоугольника {@code polygonFrom}, которые принадлежат многоугольнику {@code polygon}.
     */
    public static List<Pixel> inPolygon(Pixel[] polygonFrom, PolygonPixel polygon) {
        List<Pixel> res = new ArrayList<>();
        for (Pixel vertex : polygonFrom)
            if (vertex.isInPolygon(polygon))
                res.add(vertex);
        return res;
    }

    /**
     * Возвращает площадь треугольника {@code triangle}.
     */
    public static double squareTriangle(PolygonPixel triangle) {
        return 0.5 * Math.abs((triangle.getVertices().get(2).getI() - triangle.getVertices().get(0).getI()) * (triangle.getVertices().get(1).getJ() - triangle.getVertices().get(0).getJ()) -
                (triangle.getVertices().get(2).getJ() - triangle.getVertices().get(0).getJ()) * (triangle.getVertices().get(1).getI() - triangle.getVertices().get(0).getI()));
    }

    /**
     * Возвращает площадь многоугольника {@code polygon}.
     */
    public static double squarePolygon(PolygonPixel polygon) {
        double square = 0;
        for (PolygonPixel triangle : polygon.toTriangles())
            square += squareTriangle(triangle);
        return square;
    }

    /**
     * Возвращает площадь части прямоугольника {@code rectangle}, которая не принадлежит многоугольнику {@code overlap}.
     */
    public static double squareRectangleWithoutOverlap(Rectangle rectangle, PolygonPixel overlap) {
        double d1 = rectangle.squareRectangle();
        PolygonPixel inter = getIntersection(rectangle, overlap);
        double d2 = squarePolygon(inter);
        return d1 - d2;
    }

    public static PolygonPixel getIntersection(Rectangle rectangle, PolygonPixel polygon) {
        List<Pixel> vertices = new ArrayList<>();
        vertices.addAll(rectangle.inRectangle(polygon));
        vertices.addAll(inPolygon(toPolygon(rectangle), polygon));
        Pixel intersection;
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < polygon.getVertices().size(); j++) {
                intersection = Pixel.findIntersection(toPolygon(rectangle)[i], toPolygon(rectangle)[i + 1 < 4 ? i + 1 : 0],
                        polygon.getVertices().get(j), polygon.getVertices().get(j + 1 < polygon.getVertices().size() ? j + 1 : 0));
                if (intersection.getI() != -1)
                    vertices.add(intersection);
            }
        return new PolygonPixel(Thermogram.order(vertices));
    }

    @Override
    public String toString() {
        return "(" + i + ", " + j + ")";
    }
}