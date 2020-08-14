package main;

import com.grum.geocalc.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

/*
https://github.com/grumlimited/geocalc
 */

/**
 * Система c'x'y'z' пиксельных координат. Центр c' находится в нижней левой точке матрицы камеры. Ось c'x' направлена
 * вдоль нижней стороны матрицы, ось c'y' - вдоль левой боковой стороны матрицы, а ось c'z' - вверх.
 */
class Pixel {
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

    public int getI() {
        return i;
    }

    public int getJ() {
        return j;
    }
}

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

    private static double imageLength() {
        return RES_X * PIXEL_SIZE;
    }

    private static double imageWidth() {
        return RES_Y * PIXEL_SIZE;
    }

    private static double areaLength(double height) {
        return height * imageLength() / FOCAL_LENGTH;
    }

    private static double areaWidth(double height) {
        return height * imageWidth() / FOCAL_LENGTH;
    }

    private static double toAreaLength(double height, int pixel) {
        return areaLength(height) * pixel / RES_X;
    }

    private static double toAngle(double length) {
        return (length / EARTH_RADIUS) * (180 / Math.PI);
    }

    private static double anglePerPixel(double height, double angle) {
        return toAngle(areaLength(height) / RES_X);
    }

    private static double anglePerPixel2(double height, double angle) {
        return toAngle(areaWidth(height) / RES_Y);
    }

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
    private enum MatrixCorners {
        MC0(0, RES_Y - 1),
        MC1(RES_X - 1, RES_Y - 1),
        MC2(RES_X - 1, 0),
        MC3(0, 0);

        private final int i;
        private final int j;

        MatrixCorners(int i, int j) {
            this.i = i;
            this.j = j;
        }

        private Pixel toPixel() {
            return new Pixel(i, j);
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

    private static Point[] getCorners(Pixel o, Point coords, double yaw, double height) {
        Point[] res = new Point[4];

        double[] angles = {
                -(90 + (180/Math.PI)*Math.atan(o.getI() / (RES_Y - o.getJ() + 0.)) + yaw),
                -((180/Math.PI)*Math.atan((RES_Y - o.getJ()) / (RES_X - o.getI() + 0.)) + yaw),
                -(-(180/Math.PI)*Math.atan(o.getJ() / (RES_X - o.getI() + 0.)) + yaw),
                -(-90 - (180/Math.PI)*Math.atan(o.getI() / (o.getJ() + 0.)) + yaw)};
        System.out.println("angles:");
        System.out.println(Arrays.toString(angles));

        for(int i = 0; i<angles.length; i++)
            res[i] = EarthCalc.pointAt(coords, angles[i], earthDistance(o, MatrixCorners.values()[i].toPixel(), height));
        return res;
    }

    public static void main(String[] args) {

        double s1 = earthDistance(new Pixel(0, 0), new Pixel(RES_X - 1, 0), 152);
        double s2 = earthDistance(new Pixel(0, 0), new Pixel(0, RES_Y - 1), 152);
        System.out.println(s1 + " " + s2);

        Point coords = Point.at(Coordinate.fromDMS(53, 46,45.70), Coordinate.fromDMS(87, 15,44.59));
        Point[] points = getCorners(new Pixel(484, 490), coords, 39.7, 152.2);
        for (Point point : points)
            try {
                System.out.println("lat=" + toDMSCoordinate(point.latitude) + "  lon=" + toDMSCoordinate(point.longitude));
            } catch (NumberFormatException e) {
                System.out.println("NumberFormatException");
            }

    }
}