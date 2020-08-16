package main;

import com.grum.geocalc.Coordinate;
import com.grum.geocalc.DMSCoordinate;
import com.grum.geocalc.EarthCalc;
import com.grum.geocalc.Point;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static java.lang.Math.*;

/*

https://github.com/grumlimited/geocalc

 */

public class NewClass {
    /**
     * Разрешение матрицы по горизонтали.
     */
    private final static int RES_X = 640;
    /**
     * Разрешение матрицы по вертикали.
     */
    private final static int RES_Y = 512;
    /**
     * Шаг пикселя, м.
     */
    private final static double PIXEL_SIZE = 17. / 1000_000;
    /**
     * Фокусное расстояние, м.
     */
    private final static double FOCAL_LENGTH = 25. / 1000;
    /**
     * Средний радиус Земли, м.
     */
    private final static double EARTH_RADIUS = 6371.01 * 1000;

    /**
     * Возвращает величину, обратную к масштабу матрицы, т. е. отношение длины отрезка на местности к длине
     * соответствующего отрезка на матрице камеры.
     */
    private static double reverseScale(double height) {
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
    private static double earthDistance(Pixel a, Pixel b, double height) {
        return reverseScale(height) * matrixDistance(a, b);
    }

    /**
     * Пиксельные координаты углов матрицы, начиная с верхнего левого угла и заканчивая нижним левым.
     */
    private enum Corners {
        /**
         * Верхний левый угол матрицы.
         */
        C0(0, RES_Y - 1),
        /**
         * Верхний правый угол матрицы.
         */
        C1(RES_X - 1, RES_Y - 1),
        /**
         * Нижний правый угол матрицы.
         */
        C2(RES_X - 1, 0),
        /**
         * Нижний левый угол матрицы.
         */
        C3(0, 0);

        private final int i;
        private final int j;

        Corners(int i, int j) {
            this.i = i;
            this.j = j;
        }

        private Pixel toPixel() {
            return new Pixel(i, j);
        }

        /**
         * Вычисляет острый угол (в градусах) между отрезком, соединяющим точку {@code p} и текущий угол матрицы, и
         * прямой, проходящей через точку {@code p} и параллельной оси c'x'.
         */
        private double angle(Pixel p) {
            return (180 / Math.PI) * Math.atan(Math.abs(j - p.getJ()) / Math.abs(i - p.getI() + 0.));
        }

        /**
         * Вычисляет земные координаты углов матрицы.
         *
         * @param p      Пиксельные координаты пикселя, который является изображением точки {@code point}
         * @param point  Земные координаты точки земной поверхности
         * @param yaw    Угол поворота оси c'x' относительно оси OX, отсчитываемый против часовой стрелки (ось OX
         *               направлена на север)
         * @param height Высота фотографирования
         */
        private static Point[] getCorners(Pixel p, Point point, double yaw, double height) {
            Point[] corners = new Point[4];
            double[] angles = {
                    Corners.C0.angle(p) - yaw - 180,
                    -Corners.C1.angle(p) - yaw,
                    Corners.C2.angle(p) - yaw,
                    -Corners.C3.angle(p) - yaw + 180};
            for (int i = 0; i < 4; i++)
                corners[i] = EarthCalc.pointAt(point, angles[i], earthDistance(p, Corners.values()[i].toPixel(), height));
            return corners;
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

    private static Pixel toPixel(Point point, Point centre, double yaw, double height) {
        double earthDistance = EarthCalc.harvesineDistance(point, centre);
        double omega = (PI / 180) * (360 - yaw - EarthCalc.bearing(centre, point));
        double pixelDistance = earthDistance / reverseScale(height) / PIXEL_SIZE;
        return new Pixel((int) round(pixelDistance * cos(omega)), (int) round(pixelDistance * sin(omega)));
    }

    public static void main(String[] args) {

        System.out.println(Pixel.findIntersection(new Pixel(0, 0), new Pixel(3, 3), new Pixel(3, 3), new Pixel(5, 5)));

        double s1 = earthDistance(new Pixel(0, 0), new Pixel(RES_X - 1, 0), 152);
        double s2 = earthDistance(new Pixel(0, 0), new Pixel(0, RES_Y - 1), 152);
        System.out.println(Corners.C2.angle(new Pixel(484, 490)) + " " + s2);

        Point mE = Point.at(Coordinate.fromDMS(53, 46, 45.70), Coordinate.fromDMS(87, 15, 44.59));
        double yaw = 39.7;
        double height = 152.2;
        Pixel m = new Pixel(484, 490);
        Point[] corners = Corners.getCorners(m, mE, yaw, height);

        System.out.println(toPixel(corners[2], corners[3], yaw, height));

        for (Point point : corners)
            try {
                System.out.println("lat=" + toDMSCoordinate(point.latitude) + "  lon=" + toDMSCoordinate(point.longitude));
            } catch (NumberFormatException e) {
                System.out.println("NumberFormatException");
            }

    }
}