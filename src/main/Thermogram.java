package main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.grum.geocalc.Coordinate;
import com.grum.geocalc.DMSCoordinate;
import com.grum.geocalc.EarthCalc;
import com.grum.geocalc.Point;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static java.lang.Math.*;


/**
 * С Землёй связана неподвижная система OXYZ координат. Ось OX направлена на север, ось OY - на запад, а ось OZ - вверх.
 * Положение центра O произвольно.
 * <p>
 * https://github.com/grumlimited/geocalc
 */
public class Thermogram {
    /**
     * Название термограммы.
     */
    private final String name;
    /**
     * Угол поворота оси c'x' относительно оси OX, отсчитываемый против часовой стрелки.
     */
    private final double yaw;
    /**
     * Высота фотографирования.
     */
    private final double height;
    /**
     * Географические координаты места съёмки.
     */
    private final Point groundNadir;

    /**
     * Разрешение матрицы по горизонтали (т. е. по оси c'x').
     */
    public final static int RES_X = 640;
    /**
     * Разрешение матрицы по вертикали (т. е. по оси c'y').
     */
    public final static int RES_Y = 512;
    /**
     * Шаг пикселя, м.
     */
    public final static double PIXEL_SIZE = 17. / 1000_000;
    /**
     * Фокусное расстояние, м.
     */
    public final static double FOCAL_LENGTH = 25. / 1000;
    /**
     * Средний радиус Земли, м.
     */
    public final static double EARTH_RADIUS = 6371.01 * 1000;
    /**
     * Абсцисса главной точки снимка в системе координат c'x'y'.
     */
    public final static int PRINCIPAL_POINT_X = 310;
    /**
     * Ордината главной точки снимка в системе координат c'x'y'.
     */
    public final static int PRINCIPAL_POINT_Y = 182;
    /**
     * Главная точка снимка в системе координат c'x'y'.
     */
    public final static Pixel PRINCIPAL_POINT = new Pixel(PRINCIPAL_POINT_X, PRINCIPAL_POINT_Y);
    /**
     * Минимальная температура.
     */
    public final static double T_MIN = 30;
    /**
     * Минимальная площадь прямоугольника (в кв. пикселях).
     */
    public final static int MIN_PIXEL_SQUARE = 25;

    public Thermogram(String name, double yaw, double height, Point groundNadir) {
        this.name = name;
        this.yaw = yaw;
        this.height = height;
        this.groundNadir = groundNadir;
    }

    public String getName() {
        return name;
    }

    public double getYaw() {
        return yaw;
    }

    public double getHeight() {
        return height;
    }

    public Point getGroundNadir() {
        return groundNadir;
    }

    /**
     * Углы термограммы в системе координат c'x'y', начиная с верхнего левого угла и заканчивая нижним левым.
     */
    public enum Corners {
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

        /**
         * Абсцисса угла термограммы.
         */
        private final int i;
        /**
         * Ордината угла термограммы.
         */
        private final int j;

        Corners(int i, int j) {
            this.i = i;
            this.j = j;
        }

        /**
         * Конвертирует текущий угол термограммы в точку.
         */
        Pixel toPixel() {
            return new Pixel(i, j);
        }

        /**
         * Вычисляет острый угол (в градусах) между отрезком, соединяющим точку {@code point} и текущий угол
         * термограммы, и прямой, проходящей через точку {@code point} и параллельной оси c'x'.
         */
        double angle(Pixel point) {
            return (180 / PI) * atan(abs(j - point.getJ()) / abs(i - point.getI() + 0.));
        }
    }

    /**
     * Возвращает величину, обратную к масштабу матрицы, т. е. отношение длины отрезка на местности к длине
     * соответствующего отрезка на матрице камеры.
     */
    private static double reverseScale(double height) {
        return height / FOCAL_LENGTH;
    }

    /**
     * Вычисляет расстояние в метрах между пикселями {@code a} и {@code b}.
     */
    private static double matrixDistance(Pixel a, Pixel b) {
        return PIXEL_SIZE * sqrt(pow(a.getI() - b.getI(), 2) + pow(a.getJ() - b.getJ(), 2));
    }

    /**
     * Вычисляет расстояние в метрах между точками Земли, которые проектируются в пиксели {@code a} и {@code b}.
     */
    static double earthDistance(Pixel a, Pixel b, double height) {
        return reverseScale(height) * matrixDistance(a, b);
    }

    /**
     * Конвертирует площадь в кв. пикселях участка матрицы в площадь в кв. метрах участка Земли, который проектируется
     * на этот участок матрицы.
     */
    public static double toEarthSquare(double pixelSquare, double height) {
        return pixelSquare * pow(PIXEL_SIZE * reverseScale(height), 2);
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

    /**
     * Вычисляет географические координаты углов текущей термограммы.
     */
    Point[] getCorners() {
        Point[] corners = new Point[4];
        double[] angles = {
                Corners.C0.angle(PRINCIPAL_POINT) - yaw - 180,
                -Corners.C1.angle(PRINCIPAL_POINT) - yaw,
                Corners.C2.angle(PRINCIPAL_POINT) - yaw,
                -Corners.C3.angle(PRINCIPAL_POINT) - yaw + 180};
        for (int i = 0; i < 4; i++)
            corners[i] = EarthCalc.pointAt(groundNadir, angles[i],
                    earthDistance(PRINCIPAL_POINT, Corners.values()[i].toPixel(), height));
        return corners;
    }

    /**
     * Возвращает пиксельные координаты точки {@code point}, заданной географическими координатами.
     */
    Pixel toPixel(Point point) {
        Point centre = getCorners()[3];
        double earthDistance = EarthCalc.harvesineDistance(point, centre);
        double omega = (PI / 180) * (360 - yaw - EarthCalc.bearing(centre, point));
        double pixelDistance = earthDistance / reverseScale(height) / PIXEL_SIZE;
        return new Pixel(pixelDistance * cos(omega), pixelDistance * sin(omega));
    }

    /**
     * Определяет принадлежность текущей термограмме точки {@code point}, заданной географическими координатами.
     */
    private boolean contains(Point point) {
        Pixel pixel = toPixel(point);
        return (0 <= pixel.getI() && pixel.getI() < RES_X) && (0 <= pixel.getJ() && pixel.getJ() < RES_Y);
    }

    /**
     * Возвращает список координат углов термограммы {@code second}, которые принадлежат термограмме {@code first}, в
     * системе пиксельных координат, связанных с текущей термограммой.
     */
    private List<Pixel> cornersFromOther(Thermogram first, Thermogram second) {
        List<Pixel> vertices = new ArrayList<>();
        for (Point vertex : second.getCorners())
            if (first.contains(vertex))
                vertices.add(toPixel(vertex));
        return vertices;
    }

    /**
     * Возвращает многоугольник (в системе пиксельных координат, связанных с текущей термограммой), который является
     * пересечением текущей термограммы и термограммы {@code previous}.
     */
    Polygon<Pixel> getOverlapWith(Thermogram previous) {
        List<Pixel> vertices = new ArrayList<>();
        vertices.addAll(cornersFromOther(this, previous));
        vertices.addAll(cornersFromOther(previous, this));
        Pixel intersection;
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++) {
                intersection = Pixel.findIntersection(Corners.values()[i].toPixel(),
                        Corners.values()[i + 1 < 4 ? i + 1 : 0].toPixel(),
                        toPixel(previous.getCorners()[j]), toPixel(previous.getCorners()[j + 1 < 4 ? j + 1 : 0]));
                if (intersection.getI() != -1)
                    vertices.add(intersection);
            }
        return new Polygon<>(AbstractPoint.order(vertices));
    }

    @Override
    public String toString() {
        return getClass().getName() + new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}