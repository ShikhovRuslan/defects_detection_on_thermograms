package main;

import com.grum.geocalc.EarthCalc;
import com.grum.geocalc.Point;

import java.util.*;

import static java.lang.Math.*;


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
     * Земные координаты места съёмки.
     */
    private final Point groundNadir;

    Thermogram(double yaw, double height, Point groundNadir) {
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
     * Вычисляет земные координаты углов текущей термограммы.
     */
    Point[] getCorners() {
        Point[] corners = new Point[4];
        double[] angles = {
                NewClass.Corners.C0.angle(NewClass.PRINCIPAL_POINT) - yaw - 180,
                -NewClass.Corners.C1.angle(NewClass.PRINCIPAL_POINT) - yaw,
                NewClass.Corners.C2.angle(NewClass.PRINCIPAL_POINT) - yaw,
                -NewClass.Corners.C3.angle(NewClass.PRINCIPAL_POINT) - yaw + 180};
        for (int i = 0; i < 4; i++)
            corners[i] = EarthCalc.pointAt(groundNadir, angles[i], NewClass.earthDistance(NewClass.PRINCIPAL_POINT, NewClass.Corners.values()[i].toPixel(), height));
        return corners;
    }

    /**
     * Возвращает пиксельные координаты точки {@code point}.
     */
    Pixel toPixel(Point point) {
        Point centre = getCorners()[3];
        double earthDistance = EarthCalc.harvesineDistance(point, centre);
        double omega = (PI / 180) * (360 - yaw - EarthCalc.bearing(centre, point));
        double pixelDistance = earthDistance / NewClass.reverseScale(height) / NewClass.PIXEL_SIZE;
        return new Pixel(pixelDistance * cos(omega), pixelDistance * sin(omega));
    }

    /**
     * Определяет принадлежность точки {@code point} текущей термограмме.
     */
    private boolean contains(Point point) {
        Pixel pixel = toPixel(point);
        return (0 <= pixel.getI() && pixel.getI() < NewClass.RES_X) && (0 <= pixel.getJ() && pixel.getJ() < NewClass.RES_Y);
    }

    /**
     * Возвращает список пиксельных координат углов термограммы {@code second}, которые принадлежат термограмме
     * {@code first}, в системе пиксельных координат, связанных с текущей термограммой.
     */
    private List<Pixel> cornersFromOther(Thermogram first, Thermogram second) {
        List<Pixel> vertices = new ArrayList<>();
        for (Point vertex : second.getCorners())
            if (first.contains(vertex))
                vertices.add(toPixel(vertex));
        return vertices;
    }

    /**
     * Упорядочивает пиксели из списка {@code pixels} против часовой стрелки.
     */
    static List<Pixel> order(List<Pixel> pixels) {
        OptionalDouble newI0Optional = pixels
                .stream()
                .mapToDouble(Pixel::getI)
                .average();
        OptionalDouble newJ0Optional = pixels
                .stream()
                .mapToDouble(Pixel::getJ)
                .average();
        double newJ0 = newJ0Optional.orElse(0);
        double newI0 = newI0Optional.orElse(0);
        pixels.sort((p1, p2) -> (int) Math.signum(Math.atan2(p1.getJ() - newJ0, p1.getI() - newI0) - Math.atan2(p2.getJ() - newJ0, p2.getI() - newI0)));
        return pixels;
    }

    Polygon<Pixel> getOverlap(Thermogram previousThermogram) {
        List<Pixel> vertices = new ArrayList<>();
        vertices.addAll(cornersFromOther(this, previousThermogram));
        vertices.addAll(cornersFromOther(previousThermogram, this));
        Pixel intersection;
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++) {
                intersection = Pixel.findIntersection(NewClass.Corners.values()[i].toPixel(), NewClass.Corners.values()[i + 1 < 4 ? i + 1 : 0].toPixel(),
                        toPixel(previousThermogram.getCorners()[j]), toPixel(previousThermogram.getCorners()[j + 1 < 4 ? j + 1 : 0]));
                if (intersection.getI() != -1)
                    vertices.add(intersection);
            }
        return new Polygon<>(order(vertices));
    }
}