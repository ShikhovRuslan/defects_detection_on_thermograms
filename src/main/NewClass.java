package main;

import com.grum.geocalc.Coordinate;
import com.grum.geocalc.DMSCoordinate;
import com.grum.geocalc.Point;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

/*

https://github.com/grumlimited/geocalc

 */

public class NewClass {
    /**
     * Разрешение матрицы по горизонтали.
     */
    public final static int RES_X = 640;
    /**
     * Разрешение матрицы по вертикали.
     */
    public final static int RES_Y = 512;
    /**
     * Шаг пикселя, м.
     */
    final static double PIXEL_SIZE = 17. / 1000_000;
    /**
     * Фокусное расстояние, м.
     */
    private final static double FOCAL_LENGTH = 25. / 1000;
    /**
     * Средний радиус Земли, м.
     */
    private final static double EARTH_RADIUS = 6371.01 * 1000;

    private final static int PRINCIPAL_POINT_X = 310;

    private final static int PRINCIPAL_POINT_Y = 182;

    final static Pixel PRINCIPAL_POINT = new Pixel(PRINCIPAL_POINT_X, PRINCIPAL_POINT_Y);

    /**
     * Возвращает величину, обратную к масштабу матрицы, т. е. отношение длины отрезка на местности к длине
     * соответствующего отрезка на матрице камеры.
     */
    static double reverseScale(double height) {
        return height / FOCAL_LENGTH;
    }

    /**
     * Вычисляет расстояние между пикселями с пиксельными координатами {@code a} и {@code b}.
     */
    private static double matrixDistance(Pixel a, Pixel b) {
        return PIXEL_SIZE * Math.sqrt(Math.pow(a.getI() - b.getI(), 2) + Math.pow(a.getJ() - b.getJ(), 2));
    }

    /**
     * Вычисляет расстояние между точками Земли, чьи проекции на матрице имеют пиксельные координаты {@code a} и
     * {@code b}.
     */
    static double earthDistance(Pixel a, Pixel b, double height) {
        return reverseScale(height) * matrixDistance(a, b);
    }

    /**
     * Пиксельные координаты углов термограммы, начиная с верхнего левого угла и заканчивая нижним левым.
     */
    enum Corners {
        /**
         * Верхний левый угол термограммы.
         */
        C0(0, RES_Y - 1),
        /**
         * Верхний правый угол термограммы.
         */
        C1(RES_X - 1, RES_Y - 1),
        /**
         * Нижний правый угол термограммы.
         */
        C2(RES_X - 1, 0),
        /**
         * Нижний левый угол термограммы.
         */
        C3(0, 0);

        private final int i;
        private final int j;

        Corners(int i, int j) {
            this.i = i;
            this.j = j;
        }

        Pixel toPixel() {
            return new Pixel(i, j);
        }

        /**
         * Вычисляет острый угол (в градусах) между отрезком, соединяющим точку {@code p} и текущий угол термограммы, и
         * прямой, проходящей через точку {@code p} и параллельной оси c'x'.
         */
        double angle(Pixel p) {
            return (180 / Math.PI) * Math.atan(Math.abs(j - p.getJ()) / Math.abs(i - p.getI() + 0.));
        }
    }

    /**
     * Конвертирует угол {@code decimalDegrees}, выраженный в градусах, в формат Г-М-С.
     */
    private static DMSCoordinate toDMSCoordinate(double decimalDegrees) {
        double degrees = (int) decimalDegrees;
        double remaining = Math.abs(decimalDegrees - degrees);
        double minutes = (int) (remaining * 60);
        remaining = remaining * 60 - minutes;
        double seconds = new BigDecimal(remaining * 60).setScale(2, RoundingMode.HALF_UP).doubleValue();
        return Coordinate.fromDMS(degrees, minutes, seconds);
    }

    public static void main(String[] args) {

        System.out.println(Pixel.findIntersection(new Pixel(0, 0), new Pixel(3, 3), new Pixel(3, 3), new Pixel(5, 5)));

        double s1 = earthDistance(new Pixel(0, 0), new Pixel(RES_X - 1, 0), 152);
        double s2 = earthDistance(new Pixel(0, 0), new Pixel(0, RES_Y - 1), 152);
        System.out.println(Corners.C2.angle(new Pixel(484, 490)) + " " + s2);

        Point mE = Point.at(Coordinate.fromDMS(53, 46, 45.70), Coordinate.fromDMS(87, 15, 44.59));

        Pixel m = new Pixel(484, 490);


        Rectangle<Pixel> rectangle = new Rectangle<>(new Pixel(10, 10), new Pixel(100, 100));
        Polygon<Pixel> polygon = new Polygon<>(Arrays.asList(new Pixel(1, 1), new Pixel(1, 104), new Pixel(107, 108), new Pixel(101, 2)));
        System.out.println(Rectangle.getIntersection(rectangle, polygon));

        System.out.println(new Triangle<>(Arrays.asList(new Pixel(401, 85), new Pixel(403, 85), new Pixel(403, 102))).square());
        System.out.println(new Polygon<>(Arrays.asList(new Pixel(401, 85), new Pixel(403, 102))).square());
    }
}