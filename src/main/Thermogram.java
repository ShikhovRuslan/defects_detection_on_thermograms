package main;

import com.google.gson.*;
import com.grum.geocalc.Coordinate;
import com.grum.geocalc.DMSCoordinate;
import com.grum.geocalc.EarthCalc;
import com.grum.geocalc.Point;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
     * Список прямоугольников, где не нужно искать дефекты.
     */
    private final List<Rectangle<Pixel>> forbiddenZones;

    public Thermogram(String name, double yaw, double height, Point groundNadir, List<Rectangle<Pixel>> forbiddenZones) {
        this.name = name;
        this.yaw = yaw;
        this.height = height;
        this.groundNadir = groundNadir;
        this.forbiddenZones = forbiddenZones;
    }

    public Thermogram(String name, double yaw, double height, Point groundNadir) {
        this(name, yaw, height, groundNadir, null);
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

    public List<Rectangle<Pixel>> getForbiddenZones() {
        return forbiddenZones;
    }

    /**
     * Конвертирует земное расстояние в метрах {@code earthDistance} в соответствующее ему расстояние на матрице,
     * выраженное числом пикселей.
     */
    public static double earthToDiscreteMatrix(double earthDistance, double height, double pixelSize, double focalLength) {
        return earthDistance / reverseScale(height, focalLength) / pixelSize;
    }

    /**
     * Возвращает величину, обратную к масштабу матрицы, т. е. отношение длины отрезка на местности к длине
     * соответствующего отрезка на матрице камеры.
     */
    public static double reverseScale(double height, double focalLength) {
        return height / focalLength;
    }

    /**
     * Вычисляет расстояние в метрах между пикселями {@code a} и {@code b}.
     */
    private static double matrixDistance(Pixel a, Pixel b) {
        return Main.PIXEL_SIZE * sqrt(pow(a.getI() - b.getI(), 2) + pow(a.getJ() - b.getJ(), 2));
    }

    /**
     * Вычисляет расстояние в метрах между точками Земли, которые проектируются в пиксели {@code a} и {@code b}.
     */
    static double earthDistance(Pixel a, Pixel b, double height, double focalLength) {
        return reverseScale(height, focalLength) * matrixDistance(a, b);
    }

    /**
     * Конвертирует площадь в кв. пикселях участка матрицы в площадь в кв. метрах участка Земли, который проектируется
     * на этот участок матрицы.
     */
    public static double toEarthSquare(double pixelSquare, double height, double focalLength) {
        return pixelSquare * pow(Main.PIXEL_SIZE * reverseScale(height, focalLength), 2);
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
    Point[] getCorners(double focalLength) {
        Point[] corners = new Point[4];
        double[] angles = {
                Corners.C0.angle(Main.PRINCIPAL_POINT) - yaw - 180,
                -Corners.C1.angle(Main.PRINCIPAL_POINT) - yaw,
                Corners.C2.angle(Main.PRINCIPAL_POINT) - yaw,
                -Corners.C3.angle(Main.PRINCIPAL_POINT) - yaw + 180};
        for (int i = 0; i < 4; i++)
            corners[i] = EarthCalc.pointAt(groundNadir, angles[i],
                    earthDistance(Main.PRINCIPAL_POINT, Corners.values()[i].toPixel(), height, focalLength));
        return corners;
    }

    /**
     * Возвращает пиксельные координаты точки {@code point}, заданной географическими координатами.
     */
    Pixel toPixel(Point point, double focalLenght) {
        Point centre = getCorners(focalLenght)[3];
        double earthDistance = EarthCalc.harvesineDistance(point, centre);
        double omega = (PI / 180) * (360 - yaw - EarthCalc.bearing(centre, point));
        double pixelDistance = earthDistance / reverseScale(height, focalLenght) / Main.PIXEL_SIZE;
        return new Pixel(pixelDistance * cos(omega), pixelDistance * sin(omega));
    }

    /**
     * Определяет принадлежность текущей термограмме точки {@code point}, заданной географическими координатами.
     */
    private boolean contains(Point point, double focalLength, int resX, int resY) {
        Pixel pixel = toPixel(point, focalLength);
        return (0 <= pixel.getI() && pixel.getI() < resX) && (0 <= pixel.getJ() && pixel.getJ() < resY);
    }

    /**
     * Возвращает список координат углов термограммы {@code second}, которые принадлежат термограмме {@code first}, в
     * системе пиксельных координат, связанных с текущей термограммой.
     */
    private List<Pixel> cornersFromOther(Thermogram first, Thermogram second, double focalLength, int resX, int resY) {
        List<Pixel> vertices = new ArrayList<>();
        for (Point vertex : second.getCorners(focalLength))
            if (first.contains(vertex, focalLength, resX, resY))
                vertices.add(toPixel(vertex, focalLength));
        return vertices;
    }

    /**
     * Возвращает многоугольник (в системе пиксельных координат, связанных с текущей термограммой), который является
     * пересечением текущей термограммы и термограммы {@code previous}.
     */
    Polygon<Pixel> getOverlapWith(Thermogram previous, double focalLength, int resX, int resY) {
        List<Pixel> vertices = new ArrayList<>();
        vertices.addAll(cornersFromOther(this, previous, focalLength, resX, resY));
        vertices.addAll(cornersFromOther(previous, this, focalLength, resX, resY));
        Pixel intersection;
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++) {
                intersection = Pixel.findIntersection(Corners.values()[i].toPixel(),
                        Corners.values()[i + 1 < 4 ? i + 1 : 0].toPixel(),
                        toPixel(previous.getCorners(focalLength)[j], focalLength), toPixel(previous.getCorners(focalLength)[j + 1 < 4 ? j + 1 : 0], focalLength));
                if (intersection.getI() != -1)
                    vertices.add(intersection);
            }
        return new Polygon<>(AbstractPoint.order(vertices), focalLength);
    }

    /**
     * Возвращает массив термограмм, прочитанных из файла {@code filename1}, содержащего массив в формате JSON.
     * Все поля, кроме поля {@code forbiddenZones}, прочитываются из файла {@code filename1}, а поле
     * {@code forbiddenZones} - из файла {@code filename2}, содержащего массив в формате JSON.
     */
    static Thermogram[] readThermograms(String filename1, String filename2) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Thermogram.class, new ThermogramDeserializer())
                .create();
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(filename1));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Thermogram[] tmpThermograms = gson.fromJson(bufferedReader, Thermogram[].class);
        Map<String, List<Rectangle<Pixel>>> map = readForbiddenZones(filename2);
        Thermogram[] thermograms = new Thermogram[tmpThermograms.length];
        for (int i = 0; i < tmpThermograms.length; i++)
            thermograms[i] = new Thermogram(tmpThermograms[i].name, tmpThermograms[i].yaw, tmpThermograms[i].height,
                    tmpThermograms[i].groundNadir, map.get(tmpThermograms[i].name));
        return thermograms;
    }

    /**
     * Возвращает термограмму из массива {@code thermograms}, имя которой совпадает с именем {@code name}.
     *
     * @throws IllegalArgumentException если термограмма с указанным именем в указанном массиве отсутствует
     */
    static Thermogram getByName(String name, Thermogram[] thermograms) {
        for (Thermogram thermogram : thermograms)
            if (thermogram.name.equals(name))
                return thermogram;
        throw new IllegalArgumentException("Термограмма с указанным именем в указанном массиве отсутствует.");
    }

    /**
     * Возвращает соответствие между значениями полей {@link #name} и {@link #forbiddenZones}, считанными из файла
     * {@code filename}, содержащего массив в формате JSON.
     */
    static Map<String, List<Rectangle<Pixel>>> readForbiddenZones(String filename) {
        JsonArray arrEntries = null, arrRectangles;
        JsonObject jEntry, jRectangle, jLeft, jRight;

        List<String> names = new ArrayList<>();
        List<List<Rectangle<Pixel>>> rectangleLists = new ArrayList<>();
        List<Rectangle<Pixel>> rectangles = new ArrayList<>();

        Pixel left, right;
        try {
            arrEntries = (JsonArray) new JsonParser().parse(new FileReader(filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        for (Object objEntry : arrEntries) {
            jEntry = (JsonObject) objEntry;
            names.add(jEntry.get("Name").getAsString());
            arrRectangles = (JsonArray) jEntry.get("ForbiddenZones");
            for (Object objRectangle : arrRectangles) {
                jRectangle = (JsonObject) objRectangle;
                jLeft = (JsonObject) jRectangle.get("Left");
                jRight = (JsonObject) jRectangle.get("Right");
                left = new Pixel(jLeft.get("I").getAsInt(), jLeft.get("J").getAsInt());
                right = new Pixel(jRight.get("I").getAsInt(), jRight.get("J").getAsInt());
                rectangles.add(new Rectangle<>(left, right));
            }
            rectangleLists.add(new ArrayList<>(rectangles));
            rectangles.clear();
        }
        Map<String, List<Rectangle<Pixel>>> map = new HashMap<>();
        for (int i = 0; i < names.size(); i++)
            map.put(names.get(i), rectangleLists.get(i));
        return map;
    }

    @Override
    public String toString() {
        return getClass().getName() + new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}