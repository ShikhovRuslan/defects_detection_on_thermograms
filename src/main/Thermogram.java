package main;

import com.grum.geocalc.EarthCalc;
import com.grum.geocalc.Point;

import java.util.*;

import static java.lang.Math.*;


/**
 * С Землёй связана неподвижная система OXYZ координат. Ось OX направлена на север, ось OY - на запад, а ось OZ - вверх.
 * Положение центра O произвольно.
 */
public class Thermogram {
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

    public Thermogram(double yaw, double height, Point groundNadir) {
        this.yaw = yaw;
        this.height = height;
        this.groundNadir = groundNadir;
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
     * Вычисляет географические координаты углов текущей термограммы.
     */
    Point[] getCorners() {
        Point[] corners = new Point[4];
        double[] angles = {
                NewClass.Corners.C0.angle(NewClass.PRINCIPAL_POINT) - yaw - 180,
                -NewClass.Corners.C1.angle(NewClass.PRINCIPAL_POINT) - yaw,
                NewClass.Corners.C2.angle(NewClass.PRINCIPAL_POINT) - yaw,
                -NewClass.Corners.C3.angle(NewClass.PRINCIPAL_POINT) - yaw + 180};
        for (int i = 0; i < 4; i++)
            corners[i] = EarthCalc.pointAt(groundNadir, angles[i],
                    NewClass.earthDistance(NewClass.PRINCIPAL_POINT, NewClass.Corners.values()[i].toPixel(), height));
        return corners;
    }

    /**
     * Возвращает пиксельные координаты точки {@code point}, заданной географическими координатами.
     */
    Pixel toPixel(Point point) {
        Point centre = getCorners()[3];
        double earthDistance = EarthCalc.harvesineDistance(point, centre);
        double omega = (PI / 180) * (360 - yaw - EarthCalc.bearing(centre, point));
        double pixelDistance = earthDistance / NewClass.reverseScale(height) / NewClass.PIXEL_SIZE;
        return new Pixel(pixelDistance * cos(omega), pixelDistance * sin(omega));
    }

    /**
     * Определяет принадлежность текущей термограмме точки {@code point}, заданной географическими координатами.
     */
    private boolean contains(Point point) {
        Pixel pixel = toPixel(point);
        return (0 <= pixel.getI() && pixel.getI() < NewClass.RES_X) &&
                (0 <= pixel.getJ() && pixel.getJ() < NewClass.RES_Y);
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
                intersection = Pixel.findIntersection(NewClass.Corners.values()[i].toPixel(),
                        NewClass.Corners.values()[i + 1 < 4 ? i + 1 : 0].toPixel(),
                        toPixel(previous.getCorners()[j]), toPixel(previous.getCorners()[j + 1 < 4 ? j + 1 : 0]));
                if (intersection.getI() != -1)
                    vertices.add(intersection);
            }
        return new Polygon<>(AbstractPoint.order(vertices));
    }
}