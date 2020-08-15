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
        C0(0, RES_Y - 1),
        C1(RES_X - 1, RES_Y - 1),
        C2(RES_X - 1, 0),
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

    /*
    // https://www.igismap.com/formula-to-find-bearing-or-heading-angle-between-two-points-latitude-longitude/
    private static double bearing(Point point, Point centre) {
        double X = cos((PI / 180) * point.latitude) * sin((PI / 180) * (point.longitude - centre.longitude));
        double Y = cos((PI / 180) * centre.latitude) * sin((PI / 180) * point.latitude) - sin((PI / 180) * centre.latitude) * cos((PI / 180) * point.latitude) * cos((PI / 180) * (point.longitude - centre.longitude));
        return (180 / PI) * (atan2(X, Y) > 0 ? atan2(X, Y) : 2 * PI + atan2(X, Y));
    }*/

    private static Pixel toPixel(Point point, Point centre, double yaw, double height) {
        double earthDistance = EarthCalc.harvesineDistance(point, centre);
        double omega = (PI / 180) * (360 - yaw - EarthCalc.bearing(centre, point));
        double pixelDistance = earthDistance / reverseScale(height) / PIXEL_SIZE;
        return new Pixel((int) round(pixelDistance * cos(omega)), (int) round(pixelDistance * sin(omega)));
    }

    private static class Point2 {
        double x, y;

        public Point2(double newX, double newY) {
            x = newX;
            y = newY;
        }
    }

    //https://vscode.ru/prog-lessons/nayti-tochku-peresecheniya-otrezkov.html
    //метод, проверяющий пересекаются ли 2 отрезка [p1, p2] и [p3, p4]
    private boolean checkIntersectionOfTwoLineSegments(Point2 p1, Point2 p2, Point2 p3, Point2 p4) {
        //сначала расставим точки по порядку, т.е. чтобы было p1.x <= p2.x
        if (p2.x < p1.x) {
            Point2 tmp = p1;
            p1 = p2;
            p2 = tmp;
        }
        //и p3.x <= p4.x
        if (p4.x < p3.x) {
            Point2 tmp = p3;
            p3 = p4;
            p4 = tmp;
        }

        //проверим существование потенциального интервала для точки пересечения отрезков
        if (p2.x < p3.x) {
            return false; //ибо у отрезков нету взаимной абсциссы
        }

        //если оба отрезка вертикальные
        if ((p1.x - p2.x == 0) && (p3.x - p4.x == 0)) {
            //если они лежат на одном X
            if (p1.x == p3.x)
                //проверим пересекаются ли они, т.е. есть ли у них общий Y
                //для этого возьмём отрицание от случая, когда они НЕ пересекаются
                if (!((Math.max(p1.y, p2.y) < Math.min(p3.y, p4.y)) ||
                        (Math.min(p1.y, p2.y) > Math.max(p3.y, p4.y))))
                    return true;

            return false;
        }

        //найдём коэффициенты уравнений, содержащих отрезки
        //f1(x) = A1*x + b1 = y
        //f2(x) = A2*x + b2 = y

        //если первый отрезок вертикальный
        if (p1.x - p2.x == 0) {

            //найдём Xa, Ya - точки пересечения двух прямых
            double Xa = p1.x;
            double A2 = (p3.y - p4.y) / (p3.x - p4.x);
            double b2 = p3.y - A2 * p3.x;
            double Ya = A2 * Xa + b2;

            if (p3.x <= Xa && p4.x >= Xa && Math.min(p1.y, p2.y) <= Ya &&
                    Math.max(p1.y, p2.y) >= Ya) {

                return true;
            }

            return false;
        }

        //если второй отрезок вертикальный
        if (p3.x - p4.x == 0) {

            //найдём Xa, Ya - точки пересечения двух прямых
            double Xa = p3.x;
            double A1 = (p1.y - p2.y) / (p1.x - p2.x);
            double b1 = p1.y - A1 * p1.x;
            double Ya = A1 * Xa + b1;

            if (p1.x <= Xa && p2.x >= Xa && Math.min(p3.y, p4.y) <= Ya &&
                    Math.max(p3.y, p4.y) >= Ya) {

                return true;
            }

            return false;
        }

        //оба отрезка невертикальные
        double A1 = (p1.y - p2.y) / (p1.x - p2.x);
        double A2 = (p3.y - p4.y) / (p3.x - p4.x);
        double b1 = p1.y - A1 * p1.x;
        double b2 = p3.y - A2 * p3.x;

        if (A1 == A2) {
            return false; //отрезки параллельны
        }

        //Xa - абсцисса точки пересечения двух прямых
        double Xa = (b2 - b1) / (A1 - A2);

        if ((Xa < Math.max(p1.x, p3.x)) || (Xa > Math.min(p2.x, p4.x))) {
            return false; //точка Xa находится вне пересечения проекций отрезков на ось X
        } else {
            return true;
        }
    }

    public static void main(String[] args) {

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