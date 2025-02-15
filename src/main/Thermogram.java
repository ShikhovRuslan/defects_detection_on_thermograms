package main;

import com.google.gson.*;
import com.grum.geocalc.Coordinate;
import com.grum.geocalc.DMSCoordinate;
import com.grum.geocalc.EarthCalc;
import com.grum.geocalc.Point;
import figures.AbstractPoint;
import figures.Pixel;
import figures.Polygon;
import figures.Rectangle;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static double matrixDistance(Pixel a, Pixel b, double pixelSize) {
        return pixelSize * sqrt(pow(a.getI() - b.getI(), 2) + pow(a.getJ() - b.getJ(), 2));
    }

    /**
     * Вычисляет расстояние в метрах между точками Земли, которые проектируются в пиксели {@code a} и {@code b}.
     */
    public static double earthDistance(Pixel a, Pixel b, double height, double focalLength, double pixelSize) {
        return reverseScale(height, focalLength) * matrixDistance(a, b, pixelSize);
    }

    /**
     * Конвертирует площадь в кв. пикселях участка матрицы в площадь в кв. метрах участка Земли, который проектируется
     * на этот участок матрицы.
     */
    public static double toEarthSquare(double pixelSquare, double height, double focalLength, double pixelSize) {
        return pixelSquare * pow(pixelSize * reverseScale(height, focalLength), 2);
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
    Point[] getCorners(double focalLength, double pixelSize, Pixel principalPoint) {
        Point[] corners = new Point[4];
        double[] angles = {
                Corners.C0.angle(principalPoint) - yaw - 180,
                -Corners.C1.angle(principalPoint) - yaw,
                Corners.C2.angle(principalPoint) - yaw,
                -Corners.C3.angle(principalPoint) - yaw + 180};
        for (int i = 0; i < 4; i++)
            corners[i] = EarthCalc.pointAt(groundNadir, angles[i],
                    earthDistance(principalPoint, Corners.values()[i].toPixel(), height, focalLength, pixelSize));
        return corners;
    }

    /**
     * Возвращает пиксельные координаты точки {@code point}, заданной географическими координатами.
     */
    Pixel toPixel(Point point, double focalLength, Pixel principalPoint, double pixelSize) {
        Point centre = getCorners(focalLength, pixelSize, principalPoint)[3];
        double earthDistance = EarthCalc.harvesineDistance(point, centre);
        double omega = (PI / 180) * (360 - yaw - EarthCalc.bearing(centre, point));
        double pixelDistance = earthDistance / reverseScale(height, focalLength) / pixelSize;
        return new Pixel(pixelDistance * cos(omega), pixelDistance * sin(omega));
    }

    /**
     * Определяет принадлежность текущей термограмме точки {@code point}, заданной географическими координатами.
     */
    private boolean contains(Point point, double focalLength, Pixel principalPoint, double pixelSize, int resX, int resY) {
        Pixel pixel = toPixel(point, focalLength, principalPoint, pixelSize);
        return (0 <= pixel.getI() && pixel.getI() < resX) && (0 <= pixel.getJ() && pixel.getJ() < resY);
    }

    /**
     * Возвращает список координат углов термограммы {@code second}, которые принадлежат термограмме {@code first}, в
     * системе пиксельных координат, связанных с текущей термограммой.
     */
    private List<Pixel> cornersFromOther(Thermogram first, Thermogram second, double focalLength, double pixelSize,
                                         Pixel principalPoint, int resX, int resY) {
        List<Pixel> vertices = new ArrayList<>();
        for (Point vertex : second.getCorners(focalLength, pixelSize, principalPoint))
            if (first.contains(vertex, focalLength, principalPoint, pixelSize, resX, resY))
                vertices.add(toPixel(vertex, focalLength, principalPoint, pixelSize));
        return vertices;
    }

    /**
     * Возвращает многоугольник (в системе пиксельных координат, связанных с текущей термограммой), который является
     * пересечением текущей термограммы и термограммы {@code previous}.
     */
    public Polygon<Pixel> getOverlapWith(Thermogram previous, double focalLength, double pixelSize, Pixel principalPoint,
                                         int resX, int resY) {
        List<Pixel> vertices = new ArrayList<>();
        vertices.addAll(cornersFromOther(this, previous, focalLength, pixelSize, principalPoint, resX, resY));
        vertices.addAll(cornersFromOther(previous, this, focalLength, pixelSize, principalPoint, resX, resY));
        Pixel intersection;
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++) {
                intersection = Pixel.findIntersection(Corners.values()[i].toPixel(),
                        Corners.values()[i + 1 < 4 ? i + 1 : 0].toPixel(),
                        toPixel(previous.getCorners(focalLength, pixelSize, principalPoint)[j],
                                focalLength, principalPoint, pixelSize),
                        toPixel(previous.getCorners(focalLength, pixelSize, principalPoint)[j + 1 < 4 ? j + 1 : 0],
                                focalLength, principalPoint, pixelSize));
                if (!intersection.equals(new Pixel(Integer.MIN_VALUE, Integer.MIN_VALUE)))
                    vertices.add(intersection);
            }
        return new Polygon<>(AbstractPoint.order(vertices), focalLength);
    }

    /**
     * Возвращает массив термограмм, прочитанных из файла {@code filename1}, содержащего массив в формате JSON.
     * Все поля, кроме поля {@code forbiddenZones}, прочитываются из файла {@code filename1}, а поле
     * {@code forbiddenZones} - из файла {@code filename2}, содержащего массив в формате JSON.
     */
    public static Thermogram[] readThermograms(String filename1, String filename2) {
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

        Map<String, List<Rectangle<Pixel>>> map = Helper.mapFromFileWithJsonArray(filename2, o -> {
            JsonObject jRectangle = (JsonObject) o;
            JsonObject jLeft = (JsonObject) jRectangle.get("Left");
            JsonObject jRight = (JsonObject) jRectangle.get("Right");
            return new Rectangle<>(
                    new Pixel(jLeft.get("I").getAsInt(), jLeft.get("J").getAsInt()),
                    new Pixel(jRight.get("I").getAsInt(), jRight.get("J").getAsInt()));
        }, "Name", "ForbiddenZones");

        Thermogram[] thermograms = new Thermogram[tmpThermograms.length];
        for (int i = 0; i < tmpThermograms.length; i++)
            thermograms[i] = new Thermogram(tmpThermograms[i].name, tmpThermograms[i].yaw, tmpThermograms[i].height,
                    tmpThermograms[i].groundNadir, map != null ? map.get(tmpThermograms[i].name) : null);
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

    /**
     * Углы термограммы в системе координат c'x'y', начиная с верхнего левого угла и заканчивая нижним левым.
     */
    private enum Corners {
        /**
         * Верхний левый угол термограммы.
         */
        C0(0, ExifParam.RES_Y.intValue() - 1),
        /**
         * Верхний правый угол термограммы.
         */
        C1(ExifParam.RES_X.intValue() - 1, ExifParam.RES_Y.intValue() - 1),
        /**
         * Нижний правый угол термограммы.
         */
        C2(ExifParam.RES_X.intValue() - 1, 0),
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
        private Pixel toPixel() {
            return new Pixel(i, j);
        }

        /**
         * Вычисляет острый угол (в градусах) между отрезком, соединяющим точку {@code point} и текущий угол
         * термограммы, и прямой, проходящей через точку {@code point} и параллельной оси c'x'.
         */
        private double angle(Pixel point) {
            return (180 / PI) * atan(abs(j - point.getJ()) / abs(i - point.getI() + 0.));
        }
    }

    /**
     * Используется для десериализации термограммы. Заполняются все поля, кроме поля {@code forbiddenZones}.
     */
    private static class ThermogramDeserializer implements JsonDeserializer<Thermogram> {
        @Override
        public Thermogram deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            JsonObject jsonObject = json.getAsJsonObject();
            String name = jsonObject.get("SourceFile").getAsString().substring(
                    jsonObject.get("SourceFile").getAsString().lastIndexOf('/') + 1)
                    .substring(0, jsonObject.get("SourceFile").getAsString().substring(
                            jsonObject.get("SourceFile").getAsString().lastIndexOf('/') + 1).indexOf('.'));
            double yaw = jsonObject.get("GimbalYawDegree").getAsDouble();
            double height = jsonObject.get("RelativeAltitude").getAsDouble();
            double latitude = jsonObject.get("GPSLatitude").getAsDouble();
            double longitude = jsonObject.get("GPSLongitude").getAsDouble();
            return new Thermogram(name, -yaw - 90, height, Point.at(Coordinate.fromDegrees(latitude), Coordinate.fromDegrees(longitude)));
        }
    }
}