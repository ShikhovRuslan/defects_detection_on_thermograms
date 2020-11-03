package main;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import polygons.Point;
import polygons.Segment;
import tmp.Base;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.*;
import static java.lang.Math.abs;


/**
 * http://www.javenue.info/post/78 - чтение и запись CSV файлов в Java.
 * https://exiftool.org/forum/index.php/topic,4898.msg23972.html#msg23972.
 * https://exiftool.org/forum/index.php?topic=4898.90.
 */
public class Main {
    //
    // Краткие имена файлов.
    //

    /**
     *
     */
    private final static String SCRIPT_GLOBAL_PARAMS = "global_params";
    /**
     *
     */
    private final static String SCRIPT_THERMOGRAMS_INFO = "thermograms_info";
    /**
     *
     */
    private final static String SCRIPT_THERMOGRAMS_RAW_TEMPERATURES = "thermograms_raw_temperatures";
    /**
     *
     */
    private final static String SCRIPT_RAW = "raw";
    /**
     * Краткое имя файла с конфигурационными параметрами.
     */
    final static String SHORT_FILENAME_CONFIG = "config.txt";
    /**
     * Краткое имя файла с общими для всех термограмм EXIF-параметрами.
     */
    final static String SHORT_FILENAME_GLOBAL_PARAMS = "global_params.txt";
    /**
     * Краткое имя файла с геометрическими характеристиками съёмки.
     */
    private final static String SHORT_FILENAME_THERMOGRAMS_INFO = "thermograms_info.txt";
    /**
     * Краткое имя файла с запрещёнными зонами.
     */
    private final static String SHORT_FILENAME_FORBIDDEN_ZONES = "forbidden_zones.txt";


    //
    // Вспомогательные константы.
    //

    /**
     * Операционная система.
     */
    private final static String OS;
    /**
     * Расширение скриптов (для Windows - .bat, для Linux - .sh).
     */
    private final static String SCRIPT_EXTENSION;
    /**
     * Папка, содержащая данную программу.
     */
    public final static String DIR_CURRENT;
    /**
     * Расширение файлов с необработанными температурными данными термограмм.
     */
    private final static String EXTENSION_RAW = ".pgm";
    /**
     * Расширение термограмм.
     */
    private final static String EXTENSION = ".jpg";
    /**
     * Расширение файлов с температурными данными термограмм в формате CSV.
     */
    public final static String EXTENSION_REAL = ".csv";
    /**
     * Разделитель значений в файле с необработанными температурами в формате CSV.
     */
    private final static char SEPARATOR_RAW = ' ';
    /**
     * Разделитель значений в файле с температурами в формате CSV.
     */
    public final static char SEPARATOR_REAL = ';';
    /**
     * Минимальная площадь прямоугольника (в кв. пикселях).
     */
    public final static int MIN_PIXEL_SQUARE = 25;
    /**
     *
     */
    private final static String SHORT_FILENAME_HELP = "help.txt";


    //
    // Константы, извлечённые с использованием информации из конфигурационного файла SHORT_FILENAME_CONFIG.
    //

    /**
     * Шаг пикселя, м.
     */
    public final static double PIXEL_SIZE;
    /**
     * Абсцисса главной точки снимка в системе координат c'x'y'.
     */
    public final static int PRINCIPAL_POINT_X;
    /**
     * Ордината главной точки снимка в системе координат c'x'y'.
     */
    public final static int PRINCIPAL_POINT_Y;
    /**
     * Главная точка снимка в системе координат c'x'y'.
     */
    public final static Pixel PRINCIPAL_POINT;
    /**
     * Минимальная температура, гр. Ц.
     */
    public final static double T_MIN;
    /**
     * Минимальная площадь прямоугольника (в кв. м).
     */
    public final static double SQUARE_MIN;


    static {
        if (System.getProperty("os.name").contains(Helper.Os.WINDOWS.getName())) {
            OS = Helper.Os.WINDOWS.getName();
            SCRIPT_EXTENSION = ".bat";
        } else if (System.getProperty("os.name").contains(Helper.Os.LINUX.getName())) {
            OS = Helper.Os.LINUX.getName();
            SCRIPT_EXTENSION = ".sh";
        } else {
            OS = "";
            SCRIPT_EXTENSION = "";
        }

        String dirCurrent = "";
        try {
            dirCurrent = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        DIR_CURRENT = dirCurrent.substring(OS.equals(Helper.Os.WINDOWS.getName()) ? 1 : 0, dirCurrent.lastIndexOf('/'));

        PIXEL_SIZE = Property.PIXEL_SIZE.getDoubleValue() / 1000_000;
        PRINCIPAL_POINT_X = Property.PRINCIPAL_POINT_X.getIntValue();
        PRINCIPAL_POINT_Y = Property.PRINCIPAL_POINT_Y.getIntValue();
        PRINCIPAL_POINT = new Pixel(PRINCIPAL_POINT_X, PRINCIPAL_POINT_Y);
        T_MIN = Property.T_MIN.getDoubleValue();
        SQUARE_MIN = Property.SQUARE_MIN.getDoubleValue();
    }


    private enum Option {
        GLOBAL_PARAMS,
        THERMOGRAMS_INFO,
        THERMOGRAMS_RAW_TEMPERATURES,
        CSV,
        DEFECTS,
        HELP;

        private static void help() {
            try {
                BufferedReader reader = Files.newBufferedReader(Paths.get(DIR_CURRENT + "/" + SHORT_FILENAME_HELP));
                String line;
                while ((line = reader.readLine()) != null)
                    System.out.println(line);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private static Option getByAlias(String alias) {
            switch (alias) {
                case "-gp":
                    return GLOBAL_PARAMS;
                case "-ti":
                    return THERMOGRAMS_INFO;
                case "-trt":
                    return THERMOGRAMS_RAW_TEMPERATURES;
                case "-csv":
                    return CSV;
                case "-d":
                    return DEFECTS;
            }
            return HELP;
        }
    }


    private static Polygon<Pixel> slopingDefect(Rectangle<Pixel> rectangle, double angle) {
        int[] rangeI = Base.findMaxAndMin(Figure.toPolygon(rectangle, 0, 0, 0), Pixel::getI);
        int[] rangeJ = Base.findMaxAndMin(Figure.toPolygon(rectangle, 0, 0, 0), Pixel::getJ);
        Pixel v1 = new Pixel(rangeI[1], rangeJ[1]);
        Pixel v2 = new Pixel(rangeI[0], rangeJ[1]);
        Pixel v3 = new Pixel(rangeI[0], rangeJ[0]);
        Pixel v4 = new Pixel(rangeI[1], rangeJ[0]);

        int side14 = v4.getJ() - v1.getJ();
        int side12 = v2.getI() - v1.getI();
        int side23 = v3.getJ() - v2.getJ();
        int side34 = v3.getI() - v4.getI();

        double a = angle * PI / 180;

        Pixel v1_ = new Pixel(v1.getI() - side14 * cos(a) * sin(a), v1.getJ() + side14 * cos(a) * cos(a));
        Pixel v2_ = new Pixel(v2.getI() - side12 * cos(a) * cos(a), v2.getJ() - side12 * cos(a) * sin(a));
        Pixel v3_ = new Pixel(v3.getI() + side23 * cos(a) * sin(a), v3.getJ() - side23 * cos(a) * cos(a));
        Pixel v4_ = new Pixel(v4.getI() + side34 * cos(a) * cos(a), v4.getJ() + side34 * cos(a) * sin(a));
        List<Pixel> vertices = new ArrayList<>();
        vertices.add(v1_);
        vertices.add(v2_);
        vertices.add(v3_);
        vertices.add(v4_);

        return new Polygon<>(vertices, 0);
    }

    /*private static List<Pixel> findMiddlesOfBigs(Thermogram thermogram, double[][] realTable, int resY,
                                                 double focalLength, Polygon<Pixel> overlap,
                                                 List<Polygon<Point>> enlargedPolygons) {
        // Определение укрупнённых псевдодефектов
        int[][] binTable2 = Helper.findIf(realTable, num -> num > 0);

        Helper.nullifyRectangles(binTable2, thermogram.getForbiddenZones(), resY);

        List<Rectangle<Point>> ranges2 = Rectangle.findRectangles(binTable2, focalLength);
        ranges2.removeIf(range -> range.squarePixels() < MIN_PIXEL_SQUARE);

        List<Polygon<Point>> polygons2 = Polygon.toPolygons(ranges2, overlap, thermogram.getHeight(), focalLength, resY);
        List<Polygon<Point>> enlargedPolygons2 = new ArrayList<>();
        try {
            enlargedPolygons2 = Polygon.enlargeIteratively(polygons2, 5, overlap,
                    thermogram.getHeight(), focalLength, resY);
        } catch (IllegalArgumentException e) {
            System.out.println("Проблема с upperEnd()");
        }

        List<Rectangle<Pixel>> rp2 = new ArrayList<>();
        for (Polygon<Point> polygon : enlargedPolygons2)
            rp2.add(Base.boundingRectangle(Base.toPixelPolygon(polygon, focalLength, resY)));

        List<Rectangle<Pixel>> rp = new ArrayList<>();
        for (Polygon<Point> polygon : enlargedPolygons)
            rp.add(Base.boundingRectangle(Base.toPixelPolygon(polygon, focalLength, resY)));

        //Rectangle<Pixel> rectangle = rp.get(rp.size() - 1);
        List<Rectangle<Pixel>> bigs = new ArrayList<>();
        for (Rectangle<Pixel> initial : rp)
            for (Rectangle<Pixel> big : rp2)
                if (Base.include(initial, big)) {
                    if (big.squarePixels() > 5 * initial.squarePixels())
                        bigs.add(big);
                    else
                        bigs.add(initial);
                    break;
                }
        List<Pixel> middlesOfBigs = new ArrayList<>();
        for (Rectangle<Pixel> big : bigs)
            middlesOfBigs.add(Base.middle(big));
        return middlesOfBigs;
    }*/

    /**
     * Конвертирует таблицу с реальными температурами {@code realTable} в список укрупнённых многоугольников.
     */
    private static List<Polygon<Point>> realTableToEnlargedPolygons(Thermogram thermogram, double[][] realTable,
                                                                    double tMin, int minPixelSquare, int distance,
                                                                    Polygon<Pixel> overlap, double focalLength,
                                                                    int resY) {

        int[][] binTable = Helper.findIf(realTable, num -> num > tMin);
        Helper.nullifyRectangles(binTable, thermogram.getForbiddenZones(), resY);

        List<Rectangle<Point>> ranges = Rectangle.findRectangles(binTable, focalLength);
        ranges.removeIf(range -> range.squarePixels() < minPixelSquare);

        List<Polygon<Point>> polygons = Polygon.toPolygons(ranges, overlap, thermogram.getHeight(), focalLength, resY);
        return Polygon.enlargeIteratively(polygons, distance, overlap, thermogram.getHeight(), focalLength, resY);
    }

    private static List<Pixel> findMiddlesOfPseudoDefects(Thermogram thermogram, double[][] realTable, int resY,
                                                          double tMin, int minPixelSquare, int distance,
                                                          double focalLength, Polygon<Pixel> overlap,
                                                          List<Polygon<Point>> enlargedPolygons, int maxDiff, double k) {

        List<Polygon<Point>> enlargedPolygons2 = realTableToEnlargedPolygons(thermogram, realTable, tMin,
                minPixelSquare, distance, overlap, focalLength, resY);

        List<Rectangle<Pixel>> boundingRectangles2 = new ArrayList<>();
        for (Polygon<Point> polygon : enlargedPolygons2)
            boundingRectangles2.add(Polygon.toPixelPolygon(polygon, focalLength, resY).boundingRectangle());

        List<Rectangle<Pixel>> boundingRectangles = new ArrayList<>();
        for (Polygon<Point> polygon : enlargedPolygons)
            boundingRectangles.add(Polygon.toPixelPolygon(polygon, focalLength, resY).boundingRectangle());

        List<Pixel> middlesOfPseudoDefects = new ArrayList<>();
        for (Rectangle<Pixel> br : boundingRectangles)
            for (Rectangle<Pixel> br2 : boundingRectangles2)
                if (br.isIn(br2, maxDiff)) {
                    middlesOfPseudoDefects.add((br2.squarePixels() > k * br.squarePixels() ? br2 : br).middle());
                    break;
                }

        if (enlargedPolygons.size() != middlesOfPseudoDefects.size())
            throw new IllegalArgumentException("Для какого-то дефекта отсутствует точка, по которой вычисляется " +
                    "направление трубы.");

        return middlesOfPseudoDefects;
    }

    private static Pixel shiftPixel(Pixel pixel, int[][] right, int[][] left, double[] avEndTemp,
                                    List<Integer> permittedAnglesIndices, int half, int resX, int resY,
                                    String filenameOutput) {

        double[] diff = new double[half];
        double[] avL = new double[half];
        double[] avR = new double[half];
        int[] numberOfRightExcludedIndices = new int[half];
        int[] numberOfLeftExcludedIndices = new int[half];

        for (int i = 0; i < half; i++) {
            for (int j = 0; j < half - 1; j++) {
                if (permittedAnglesIndices.contains(left[i][j]))
                    avL[i] += avEndTemp[left[i][j]];
                else
                    numberOfLeftExcludedIndices[i]++;
                if (permittedAnglesIndices.contains(right[i][j]))
                    avR[i] += avEndTemp[right[i][j]];
                else
                    numberOfRightExcludedIndices[i]++;
            }

            try {
                if (numberOfLeftExcludedIndices[i] == 3)
                    Helper.write(filenameOutput, "numberOfLeftExcludedIndices[" + i + "] = " + (half - 1));
                else
                    avL[i] /= 3 - numberOfLeftExcludedIndices[i];
                if (numberOfRightExcludedIndices[i] == 3)
                    Helper.write(filenameOutput, "numberOfRightExcludedIndices[" + i + "] = " + (half - 1));
                else
                    avR[i] /= 3 - numberOfRightExcludedIndices[i];
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            diff[i] = abs(avL[i] - avR[i]);
        }

        double max = -1;
        int indexMax = -1;
        for (int k = 0; k < half; k++)
            if (diff[k] > max) {
                indexMax = k;
                max = diff[indexMax];
            }
        int iIncrement = 2, jIncrement = 2;

        Helper.write(filenameOutput, "---   indexMax=" + indexMax + "   avR[indexMax] > avL[indexMax] ?  " + (avR[indexMax] > avL[indexMax]));
        Helper.write(filenameOutput, pixel.toString());

        int group = 0;
        if (indexMax >= half / 4)
            group++;
        if (indexMax >= half / 2)
            group++;
        if (indexMax >= 3 * half / 4)
            group++;

        //int shiftToRight = avR[indexMax] > avL[indexMax] ? 1 : -1;
        int shiftToRight = avR[indexMax] > avL[indexMax] ? -1 : 1;
        switch (group) {
            case 0:
                iIncrement = shiftToRight;
                jIncrement = 0;
                break;
            case 1:
                iIncrement = shiftToRight;
                jIncrement = -shiftToRight;
                break;
            case 2:
                iIncrement = 0;
                jIncrement = -shiftToRight;
                break;
            case 3:
                iIncrement = -shiftToRight;
                jIncrement = -shiftToRight;
                break;
        }

        if (pixel.getI() + iIncrement >= 0 && pixel.getI() + iIncrement < resX &&
                pixel.getJ() + jIncrement >= 0 && pixel.getJ() + jIncrement < resY) {
            return new Pixel(pixel.getI() + iIncrement, pixel.getJ() + jIncrement);
        } else return new Pixel(-10, -10);
    }

    private static double processMiddle(Pixel pixel, Polygon<Point> polygon, int num, Thermogram thermogram,
                                        String filename,
                                        String outputPictureFilename, String filenameOutput, char separatorReal,
                                        double pixelSize, double focalLength, int resX, int resY) {

        double diameter = 0.7; // 0.7

        Polygon<Pixel> polygon1 = Base.toPixelPolygon(polygon, focalLength, resY);
        double pd = Base.realToMatrix(diameter, thermogram.getHeight(), pixelSize, focalLength);
        if (pd - 2 <= Base.width(polygon1) & Base.width(polygon1) <= 2 * pd + 2 &
                Base.height(polygon1) > Base.width(polygon1) * 2) {
            Helper.write(filenameOutput, "Многоугольник " + num + " " + polygon1 + " является длинным => 90");
            return 90;
        }
        if (pd - 2 <= Base.height(polygon1) & Base.height(polygon1) <= 2 * pd + 2 &
                Base.width(polygon1) > Base.height(polygon1) * 2) {
            Helper.write(filenameOutput, "Многоугольник " + num + " " + polygon1 + " является длинным => 0");
            return 0;
        }

        double coef = 2.0; // 2.0
        double tempJump = 2.0; // 2.0
        int l = 8;
        double[] angles = new double[l];
        // 0, 45, 90, 135, 180, -135, -90, -45
        for (int i = 0; i < l; i++)
            angles[i] = i * 360. / l - (i * 360 / l <= 180 ? 0 : 360);


        double inclination1 = -1000, inclination2 = -1000, pipeAngle = -1000;
        double inclination1Old = -1000, inclination2Old = -1000, pipeAngleOld = -1000;

        int n = 8; // число частей, на которые делим круг
        Point[] diameters = new Point[n / 2];
        double[] diameterAngles = new double[n / 2];
        for (int i = 0; i < n / 2; i++) {
            diameters[i] = new Point(i * l / n, i * l / n + l / 2);
            //diameterAngles[i] = 90 - 45 * i + (90 - 45 * i < 0 ? 180 : 0);
            diameterAngles[i] = 90 - 45 * i;
        }
        Point[] almostDiameters = new Point[n];
        double[] almostDiameterAngles = new double[n];
        double tmp0;
        for (int i = 0; i < n; i++) {
            almostDiameters[i] = new Point(i * l / n, i * l / n + l / 2 + l / n - (i * l / n + l / 2 + l / n < l ? 0 : l));
            tmp0 = (90 + 45) / 2. - 45 * i;
            //almostDiameterAngles[i] = tmp0 + ((0 > tmp0 & tmp0 >= -180) ? 180 : (tmp0 < -180 ? 360 : 0));
            almostDiameterAngles[i] = tmp0 + (tmp0 <= -90 ? 180 : 0);
        }
        Helper.write(filenameOutput, Arrays.toString(diameterAngles));
        Helper.write(filenameOutput, Arrays.toString(almostDiameterAngles) + "\n");

        int half = l / 2;
        int[][] right = new int[half][half - 1];
        int[][] left = new int[half][half - 1];
        int[][] forbiddenDirections = new int[half][2];
        for (int i = 0; i < half; i++) {
            for (int j = 0; j < half - 1; j++) {
                right[i][j] = i + j + 1;
                left[i][j] = right[i][j] + half - (right[i][j] + half < l ? 0 : l);
            }
            forbiddenDirections[i][0] = i;
            forbiddenDirections[i][1] = half + i;
        }

        double tMax1 = -1000;
        double tMax2 = -1000;
        double aMax1 = 1000;
        double aMax2 = 1000;
        int i1 = -1;
        int i2 = -1;

        Object[][] jumps = new Object[l][];
        Pixel[] jumpPixel = new Pixel[l];
        double[] avEndTemp = new double[l];

        List<Integer> range1 = new ArrayList<>();
        List<Integer> range2 = new ArrayList<>();

        List<Integer> range1Corr = new ArrayList<>();
        List<Integer> range2Corr = new ArrayList<>();

        List<Integer> excludedAnglesIndices = new ArrayList<>();
        List<Integer> permittedAnglesIndices = new ArrayList<>();

        int iter = 0;
        int maxIter = 10;
        while (iter < maxIter) {
            if (pixel.equals(new Pixel(-10, -10))) {
                Helper.write(filenameOutput, "--- сдвиг pixel невозможен ---");
                break;
            }
            iter++;

            range1.clear();
            range2.clear();
            range1Corr.clear();
            range2Corr.clear();

            for (int i = 0; i < l; i++) {
                jumps[i] = Base.findJump(pixel, angles[i], coef * diameter, tempJump, filename, separatorReal,
                        thermogram.getHeight(), pixelSize, focalLength, resX, resY);
                jumpPixel[i] = (Pixel) jumps[i][0];
                avEndTemp[i] = (double) jumps[i][1];
                if (jumpPixel[i].equals(new Pixel(-1, -1)) || jumpPixel[i].equals(new Pixel(-2, -2)))
                    excludedAnglesIndices.add(i);
                else
                    permittedAnglesIndices.add(i);
            }

            try {
                if (permittedAnglesIndices.size() >= 2) {
                    tMax1 = -1000;
                    tMax2 = -1000;
                    for (int i : permittedAnglesIndices)
                        if (avEndTemp[i] > tMax1) {
                            i1 = i;
                            tMax1 = avEndTemp[i1];
                        }
                    for (int i : permittedAnglesIndices)
                        if (avEndTemp[i] > tMax2 && i != i1) {
                            i2 = i;
                            tMax2 = avEndTemp[i2];
                        }
                } else
                    throw new Exception("permittedAnglesIndices.size()<2");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            aMax1 = angles[i1];
            aMax2 = angles[i2];

            //
            // Упорядочиваем i1 и i2, чтобы стало i1<i2. Эти индексы могут быть соседними.
            int tmp = i1;
            i1 = min(i1, i2);
            i2 = max(tmp, i2);

            if (i2 - i1 > 1 && !(i1 == 0 && i2 == l - 1)) {
                for (int k = i1 + 1; k < i2; k++)
                    range1.add(k);
                for (int k = i2 + 1; k < i1 + l; k++)
                    range2.add(k - (k < l ? 0 : l));
            } else if (i2 - i1 == 1) {
                for (int k = i2 + 1; k < i2 + l / 2; k++)
                    range1.add(k - (k < l ? 0 : l));
                for (int k = i2 + l / 2; k < l + i2 - 1; k++)
                    range2.add(k - (k < l ? 0 : l));
            } else {
                for (int k = 1; k < l / 2; k++)
                    range1.add(k);
                for (int k = l / 2; k < l - 1; k++)
                    range2.add(k);
            }

            Helper.write(filenameOutput, "      === iter:  " + iter + " ===\n");
            Helper.write(filenameOutput, "   aMaxs:   " + aMax1 + "   " + aMax2 + "\n");
            Helper.write(filenameOutput, "ranges:   " + Arrays.toString(range1.toArray()) + "   " +
                    Arrays.toString(range2.toArray()) + "\n");

            var sr1 = new SimpleRegression();
            var sr2 = new SimpleRegression();

            range1Corr = new ArrayList<>(range1);
            range2Corr = new ArrayList<>(range2);
            boolean range1Changed = range1Corr.removeAll(excludedAnglesIndices);
            boolean range2Changed = range2Corr.removeAll(excludedAnglesIndices);

            if (range1Changed)
                Helper.write(filenameOutput, "range1Corr:   " + range1Corr);
            if (range2Changed)
                Helper.write(filenameOutput, "range2Corr:   " + range2Corr);

            try {
                if (range1Corr.size() == 0)
                    throw new Exception("range1Corr.size()=0");
                if (range2Corr.size() == 0)
                    throw new Exception("range2Corr.size()=0");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            for (int i : range1Corr)
                sr1.addData(jumpPixel[i].getI(), jumpPixel[i].getJ());
            for (int i : range2Corr)
                sr2.addData(jumpPixel[i].getI(), jumpPixel[i].getJ());

            Helper.write(filenameOutput, "slopes:   " + sr1.getSlope() + "   " + sr2.getSlope() + "\n");

            inclination1 = atan(sr1.getSlope()) * 180 / PI;
            inclination2 = atan(sr2.getSlope()) * 180 / PI;
            inclination1Old = inclination1;
            inclination2Old = inclination2;

            int[] tmp1 = Base.getMinAndMax(jumpPixel, range1Corr, Pixel::getI);
            int[] tmp2 = Base.getMinAndMax(jumpPixel, range2Corr, Pixel::getI);
            int minI1 = tmp1[0];
            int minI2 = tmp2[0];
            int maxI1 = tmp1[1];
            int maxI2 = tmp2[1];

            if (range1Corr.size() > 1)
                inclination1 = (maxI1 - minI1 <= 2 &&
                        jumpPixel[range1Corr.get(0)].getI() == jumpPixel[range1Corr.get(range1Corr.size() - 1)].getI() ?
                        90 : inclination1);
            if (range2Corr.size() > 1)
                inclination2 = (maxI2 - minI2 <= 2 &&
                        jumpPixel[range2Corr.get(0)].getI() == jumpPixel[range2Corr.get(range2Corr.size() - 1)].getI() ?
                        90 : inclination2);


            if (range1Corr.size() < 2 & range2Corr.size() > 1) {
                pipeAngle = inclination2;
                pipeAngleOld = pipeAngle + (pipeAngle < 0 ? 180 : 0);
            } else if (range2Corr.size() < 2 & range1Corr.size() > 1) {
                pipeAngle = inclination1;
                pipeAngleOld = pipeAngle + (pipeAngle < 0 ? 180 : 0);
            } else if (range1Corr.size() < 2 & range2Corr.size() < 2) {
                pipeAngle = 90;
                pipeAngleOld = pipeAngle + (pipeAngle < 0 ? 180 : 0);
            } else {
                // pipeAngle - угол наклона трубы относительно положительного направления оси c'x', отсчитываемый против часовой
                // стрелки.
                // Сначала принадлежит промежутку (-90,90].
                double w = (inclination1 + inclination2) / 2;
                pipeAngle = w +
                        (inclination1 * inclination2 <= 0 && abs(inclination1) + abs(inclination2) > 90 ? (w <= 0 ? 90 : -90) : 0);
                pipeAngleOld = pipeAngle + (pipeAngle < 0 ? 180 : 0);
            }

            //if ((aMax1 == 0 && aMax2 == 180 || aMax2 == 0 && aMax1 == 180) && pipeAngle <= 45 && pipeAngle >= -45)
            //    pipeAngle += 90;

            if (inclination1 == 90 || inclination2 == 90)
                pipeAngle = 90;

            // Теперь pipeAngle принадлежит промежутку [0,180).
            pipeAngle = pipeAngle + (pipeAngle < 0 ? 180 : 0);

            if (inclination1 == inclination1Old & inclination2 == inclination2Old)
                Helper.write(filenameOutput, "inclinations:   " + inclination1 + "   " + inclination2 + "\n");
            else {
                Helper.write(filenameOutput, "inclinations (initial):   " + inclination1Old + "   " + inclination2Old);
                Helper.write(filenameOutput, "inclinations (after change):   " + inclination1 + "   " + inclination2 + "\n");
            }

            if (pipeAngle == pipeAngleOld) {
                Helper.write(filenameOutput, "============================");
                Helper.write(filenameOutput, "=   " + (round(pipeAngle * 100) / 100.) + "   (pipeAngle)");
            } else {
                Helper.write(filenameOutput, (round(pipeAngleOld * 100) / 100.) + "   (pipeAngle (initial))");
                Helper.write(filenameOutput, "============================");
                Helper.write(filenameOutput, "=   " + (round(pipeAngle * 100) / 100.) + "   (pipeAngle (after change))");
            }
            Helper.write(filenameOutput, "============================\n");

            int dInd = -1;
            int aDInd = -1;
            for (int i = 0; i < n / 2; i++)
                if (i1 == diameters[i].getI() & i2 == diameters[i].getJ()) {
                    dInd = i;
                    break;
                }
            if (dInd == -1)
                for (int i = 0; i < n; i++)
                    if (i1 == almostDiameters[i].getI() & i2 == almostDiameters[i].getJ() |
                            i2 == almostDiameters[i].getI() & i1 == almostDiameters[i].getJ()) {
                        aDInd = i;
                        break;
                    }


            double eps = 20;
            //if ((pipeAngle < 90 - 20 | pipeAngle > 90 + 20) & (i1 == 0 & i2 == 4 | i2 == 0 & i1 == 4)) {
            if ((dInd != -1 && !((pipeAngle <= diameterAngles[dInd] + eps &&
                    pipeAngle >= diameterAngles[dInd] - eps) | (pipeAngle - 180 <= diameterAngles[dInd] + eps &&
                    pipeAngle - 180 >= diameterAngles[dInd] - eps))) |
                    (aDInd != -1 && !((pipeAngle <= almostDiameterAngles[aDInd] + eps
                            && pipeAngle >= almostDiameterAngles[aDInd] - eps) |
                            (pipeAngle - 180 <= almostDiameterAngles[aDInd] + eps
                                    && pipeAngle - 180 >= almostDiameterAngles[aDInd] - eps)))) {
                if (iter < maxIter) {
                    coef *= 90 / 100.;
                    if (dInd != -1)
                        Helper.write(filenameOutput, dInd + "   d=" + diameters[dInd] + "  " + diameterAngles[dInd] + "  " + pipeAngle);
                    if (aDInd != -1)
                        Helper.write(filenameOutput, aDInd + "   ad=" + almostDiameters[aDInd] + "  " + almostDiameterAngles[aDInd] + "  " + pipeAngle);
                    Helper.write(filenameOutput, "--- уменьшение coef ---\n");
                } else {
                    if (dInd != -1) {
                        Helper.write(filenameOutput, "--- итерации исчерпаны, берём эталонный угол d[" + dInd + "]=" + diameterAngles[dInd]);
                        pipeAngle = diameterAngles[dInd] + (diameterAngles[dInd] < 0 ? 180 : 0);
                    }
                    if (aDInd != -1) {
                        Helper.write(filenameOutput, "--- итерации исчерпаны, берём эталонный угол ad[" + aDInd + "]=" + almostDiameterAngles[aDInd]);
                        pipeAngle = almostDiameterAngles[aDInd] + (almostDiameterAngles[aDInd] < 0 ? 180 : 0);
                    }
                }
            } else {
                // i1 и i2 - соседние или отличаются на 2
                if (
                        (abs(i1 - i2) == 1 || (min(i1, i2) == 0 && max(i1, i2) == l - 1)) ||
                                (abs(i1 - i2) == 2 || (min(i1, i2) == 0 && max(i1, i2) == l - 2) ||
                                        (min(i1, i2) == 1 && max(i1, i2) == l - 1))) {

                    Helper.write(filenameOutput, "--- сдвиг и уменьшение coef ---\n");
                    coef *= 90 / 100.;
                    pixel = shiftPixel(pixel, right, left, avEndTemp, permittedAnglesIndices, half, resX, resY, filenameOutput);
                } else {
                    Helper.write(filenameOutput, "--- индексы i1 и i2 хорошо различимы ---\n");
                    break;
                }
            }
        }

        if (range1Corr.size() < 2 & range2Corr.size() < 2)
            Helper.write(filenameOutput, "Многоугольник " + num + " " + polygon1 + ": range1Corr.size(),range2Corr.size()<2");

        try {
            BufferedImage image = ImageIO.read(new File(outputPictureFilename));
            for (int i = 0; i < l; i++) {
                Helper.write(filenameOutput, String.format("%1$6s", angles[i]) + "  " +
                        String.format("%1$5s", round(avEndTemp[i] * 10) / 10.) + "  " + jumpPixel[i]);
                if (!jumpPixel[i].equals(new Pixel(-1, -1)) && !jumpPixel[i].equals(new Pixel(-2, -2)))
                    new Segment(Point.toPoint(pixel, resY), Point.toPoint(jumpPixel[i], resY)).draw(image, Color.BLACK);
            }
            ImageIO.write(image, "jpg", new File(outputPictureFilename));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Helper.write(filenameOutput, "\n");
        return pipeAngle;
    }

    private static Polygon<Pixel> widen(Polygon<Pixel> polygon, int diam, double angle) {
        angle *= PI / 180;
        Pixel v1 = polygon.getVertices().get(0);
        Pixel v2 = polygon.getVertices().get(1);
        Pixel v3 = polygon.getVertices().get(2);
        Pixel v4 = polygon.getVertices().get(3);
        Pixel v1_ = null, v2_ = null, v3_ = null, v4_ = null;

        if (angle < PI / 2 & pow(v1.getI() - v2.getI(), 2) + pow(v1.getJ() - v2.getJ(), 2) < diam * diam) {
            int diff = (int) round((diam - sqrt(pow(v1.getI() - v2.getI(), 2) + pow(v1.getJ() - v2.getJ(), 2))) / 2);
            v1_ = new Pixel(v1.getI() - diff * sin(angle), v1.getJ() + diff * cos(angle));
            v4_ = new Pixel(v4.getI() - diff * sin(angle), v4.getJ() + diff * cos(angle));
            v2_ = new Pixel(v2.getI() + diff * sin(angle), v2.getJ() - diff * cos(angle));
            v3_ = new Pixel(v3.getI() + diff * sin(angle), v3.getJ() - diff * cos(angle));

            List<Pixel> vert = new ArrayList<>();
            vert.add(v1_);
            vert.add(v2_);
            vert.add(v3_);
            vert.add(v4_);
            return new Polygon<>(vert, 0, 0, 0);
        } else if (angle >= PI / 2 & pow(v2.getI() - v3.getI(), 2) + pow(v2.getJ() - v3.getJ(), 2) < diam * diam) {
            int diff = (int) round((diam - sqrt(pow(v2.getI() - v3.getI(), 2) + pow(v2.getJ() - v3.getJ(), 2))) / 2);
            v1_ = new Pixel(v2.getI() - diff * sin(angle), v2.getJ() + diff * cos(angle));
            v4_ = new Pixel(v1.getI() - diff * sin(angle), v1.getJ() + diff * cos(angle));
            v2_ = new Pixel(v3.getI() + diff * sin(angle), v3.getJ() - diff * cos(angle));
            v3_ = new Pixel(v4.getI() + diff * sin(angle), v4.getJ() - diff * cos(angle));

            List<Pixel> vert = new ArrayList<>();
            vert.add(v1_);
            vert.add(v2_);
            vert.add(v3_);
            vert.add(v4_);
            return new Polygon<>(vert, 0, 0, 0);
        }
        return polygon;
    }

    private static void defects(Thermogram thermogram, Polygon<Pixel> overlap,
                                String thermogramFilename, String outputPictureFilename, String realFilename,
                                char separatorReal, double pixelSize, double focalLength, int resX, int resY) throws IOException {
        System.out.println("=== Thermogram: " + thermogram.getName() + " ===\n");

        double[][] realTable = Helper.extractTable(realFilename, SEPARATOR_REAL);
        int[][] binTable = Helper.findIf(realTable, num -> num > T_MIN);

        Helper.nullifyRectangles(binTable, thermogram.getForbiddenZones(), resY);

        List<Rectangle<Point>> ranges = Rectangle.findRectangles(binTable, focalLength);
        ranges.removeIf(range -> range.squarePixels() < MIN_PIXEL_SQUARE);

        System.out.println("Overlap: " + overlap);

        List<Polygon<Point>> polygons = Polygon.toPolygons(ranges, overlap, thermogram.getHeight(), focalLength, resY);
        List<Polygon<Point>> enlargedPolygons = Polygon.enlargeIteratively(polygons, 5, overlap,
                thermogram.getHeight(), focalLength, resY);

        Polygon.drawPolygons(enlargedPolygons, Polygon.toPointPolygon(overlap, focalLength, resY), thermogram.getForbiddenZones(),
                Color.BLACK, thermogramFilename, outputPictureFilename, focalLength, resY);
        Polygon.showSquares(enlargedPolygons, thermogram.getHeight(), focalLength, resX, resY);

        String filename = DIR_CURRENT + "/" + Property.SUBDIR_OUTPUT_REAL.getValue() +
                "/" + thermogram.getName() + Property.POSTFIX_REAL.getValue() + EXTENSION_REAL;
        Pixel[] pixels;
        for (Polygon<Point> polygon : enlargedPolygons) {
            System.out.println(Base.toPixelPolygon(polygon, focalLength, resY));
            /*System.out.println(Base.procedureNotNeeded((int) Math.round(Base.realToMatrix(0.7, thermogram.getHeight(),
                    Main.PIXEL_SIZE)), Base.toPixelPolygon(polygon)) + "  " +
                    Base.toPixelPolygon(polygon) + "  " +
                    (int) Math.round(Base.realToMatrix(0.7, thermogram.getHeight(), Main.PIXEL_SIZE)) + " | " +
                    Base.width(Base.toPixelPolygon(polygon)) + " X " +
                    Base.height(Base.toPixelPolygon(polygon)));*/
            pixels = Base.toPixelPolygon(polygon, focalLength, resY).getVertices().toArray(new Pixel[0]);
//            System.out.println(Base.sss(Base.realToMatrix(0.7, thermogram.getHeight(),
//                    Main.PIXEL_SIZE), 48, 4, Main.SEPARATOR_REAL, filename, pixels) + "\n" +
//                    Base.toPixelPolygon(polygon));
            /*double a = Base.vert(Base.boundingRectangle(Base.toPixelPolygon(polygon)),
                    Base.realToMatrix(0.7, thermogram.getHeight(),
                    Main.PIXEL_SIZE), 48, 3, Main.SEPARATOR_REAL, filename);
            System.out.println("a = " + a);*/
            //System.out.println(Base.toPixelPolygon(polygon));
        }
        Pixel[] innerPixels = new Pixel[3];
        //innerPixels[0] = new Pixel(228 - 10, 129 - 5);
        //innerPixels[1] = new Pixel(231, 129);
        //innerPixels[2] = new Pixel(233 + 10, 130 + 5);
        //Pixel p = new Pixel(378, 321);
        //Pixel p = new Pixel(375,281);
        //Pixel p = new Pixel(364, 188);
        //Pixel p = new Pixel(230, 125);


        //Pixel p = new Pixel(209 + 2, 70 + 10);
        List<Pixel> middles = findMiddlesOfPseudoDefects(thermogram, realTable, resY, 0, MIN_PIXEL_SQUARE, 5, focalLength, overlap, enlargedPolygons, 3, 5);
        //Pixel p = middles.get(middles.size() - 1);

        List<Double> aRes = new ArrayList<>();
        for (int i = 0; i < middles.size(); i++) {
            //middle = new Pixel(middle.getI()+2, middle.getJ());
            aRes.add(processMiddle(middles.get(i), enlargedPolygons.get(i), i, thermogram, filename, outputPictureFilename, DIR_CURRENT + "/angles2.txt", separatorReal, pixelSize, focalLength, resX, resY));
        }

        //System.out.println("\nУглы наклона трубы:\n" + aRes + "\n");
        List<Double> res = new ArrayList<>();
        for (double v : aRes)
            res.add(round(v * 100) / 100.);
        System.out.println("\nУглы наклона трубы:\n" + res + "\n");

        try {
            String filename0 = DIR_CURRENT + "/angles.txt";
            FileWriter fw = new FileWriter(filename0, true);
            fw.write(thermogram.getName() + "   " + res + "\n\n");
            fw.close();
        } catch (IOException ioe) {
            System.err.println("IOException: " + ioe.getMessage());
        }

        List<Rectangle<Pixel>> bList = new ArrayList<>();
        for (Polygon<Point> p0 : enlargedPolygons)
            bList.add(Base.boundingRectangle(Base.toPixelPolygon(p0, focalLength, resY)));
        List<Polygon<Pixel>> slopingDefects = new ArrayList<>();
        if (aRes.size() > 0)
            for (int i = 0; i < bList.size(); i++)
                slopingDefects.add(slopingDefect(bList.get(i), aRes.get(i) + (aRes.get(i) >= 90 ? -90 : 0)));

        int diam = (int) round(Base.realToMatrix(0.7, thermogram.getHeight(), pixelSize, focalLength));
        try {
            BufferedImage image = ImageIO.read(new File(thermogramFilename));
            for (int i = 0; i < slopingDefects.size(); i++) {
                Polygon<Pixel> p = widen(slopingDefects.get(i), diam, aRes.get(i));
                System.out.println("\nbefore widen():\n" + slopingDefects.get(i));
                System.out.println("after widen():\n" + p + "\n");
                int[] a = Base.findMaxAndMin(p, Pixel::getI);
                int[] b = Base.findMaxAndMin(p, Pixel::getJ);
                if (a[1] >= 0 && b[1] >= 0 && a[0] < 640 && b[0] < 512)
                    Polygon.draw(Polygon.toPointPolygon(p, focalLength, resY), image, Color.BLACK);
            }
            ImageIO.write(image, "jpg", new File(DIR_CURRENT + "/out_sloping/" + thermogram.getName() + "_sl.jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
        List<Rectangle<Pixel>> bList2 = new ArrayList<>();
        bList2.add(new Rectangle<>(new Pixel(50, 50), new Pixel(70, 90)));
        bList2.add(new Rectangle<>(new Pixel(50 + 200, 50), new Pixel(70 + 200, 90)));
        List<Double> aRes2 = new ArrayList<>();
        aRes2.add(30.);
        aRes2.add(170.);
        List<Polygon<Pixel>> slopingDefects2 = new ArrayList<>();
        for (int i = 0; i < bList2.size(); i++)
            slopingDefects2.add(slopingDefect(bList2.get(i), aRes2.get(i) < 90 ? aRes2.get(i) : aRes2.get(i) - 90));
        try {
            BufferedImage image = ImageIO.read(new File(thermogramFilename));
            for (Rectangle<Pixel> slopingDefect : bList2)
                Polygon.draw(Polygon.toPointPolygon(Figure.toPolygon(slopingDefect, 0, 0, 0), focalLength, resY), image, Color.BLACK);
            for (Polygon<Pixel> slopingDefect : slopingDefects2)
                Polygon.draw(Polygon.toPointPolygon(slopingDefect, focalLength, resY), image, Color.BLACK);
            ImageIO.write(image, "jpg", new File(DIR_CURRENT + "/out_sloping/" + thermogram.getName() + "_sl2.jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    public static void main(String[] args) throws IOException {
        Option option = args.length == 1 ? Option.getByAlias(args[0]) : Option.HELP;

        switch (option) {
            case GLOBAL_PARAMS:
                Helper.run(DIR_CURRENT, SCRIPT_GLOBAL_PARAMS + SCRIPT_EXTENSION, OS);
                break;

            case THERMOGRAMS_INFO:
                Helper.run(DIR_CURRENT, SCRIPT_THERMOGRAMS_INFO + SCRIPT_EXTENSION, OS);
                break;

            case THERMOGRAMS_RAW_TEMPERATURES:
                Helper.run(DIR_CURRENT, SCRIPT_THERMOGRAMS_RAW_TEMPERATURES + SCRIPT_EXTENSION, OS);
                break;

            case CSV:
                File[] files = new File(Property.DIR_THERMOGRAMS.getValue()).listFiles();
                String[] thermogramsNames = new String[files.length];
                for (int i = 0; i < files.length; i++)
                    thermogramsNames[i] = files[i].getName().substring(0, files[i].getName().indexOf('.'));
                for (String thermogramName : thermogramsNames)
                    Helper.rawFileToRealFile(DIR_CURRENT + "/" + Property.SUBDIR_RAW.getValue() +
                                    "/" + thermogramName + Property.POSTFIX_RAW.getValue() + EXTENSION_RAW,
                            DIR_CURRENT + "/" + Property.SUBDIR_OUTPUT_REAL.getValue() +
                                    "/" + thermogramName + Property.POSTFIX_REAL.getValue() + EXTENSION_REAL,
                            ExifParam.RES_Y.getIntValue(), ExifParam.RES_X.getIntValue(), SEPARATOR_RAW, SEPARATOR_REAL,
                            Arrays.copyOfRange(ExifParam.readValues(), 1, ExifParam.readValues().length));
                break;

            case DEFECTS:
                Thermogram[] thermograms = Thermogram.readThermograms(
                        DIR_CURRENT + "/" + Property.SUBDIR_OUTPUT.getValue() + "/" + SHORT_FILENAME_THERMOGRAMS_INFO,
                        DIR_CURRENT + "/" + SHORT_FILENAME_FORBIDDEN_ZONES);
                for (int i = 0; i < thermograms.length; i++)
                    defects(thermograms[i],
                            thermograms[i].getOverlapWith(thermograms[i - 1 >= 0 ? i - 1 : thermograms.length - 1],
                                    ExifParam.FOCAL_LENGTH.getValue(),
                                    ExifParam.RES_X.getIntValue(),
                                    ExifParam.RES_Y.getIntValue()),
                            Property.DIR_THERMOGRAMS.getValue().replace('\\', '/') +
                                    "/" + thermograms[i].getName() + EXTENSION,
                            DIR_CURRENT + "/" + Property.SUBDIR_OUTPUT_PICTURES.getValue() +
                                    "/" + thermograms[i].getName() + Property.POSTFIX_PROCESSED.getValue() + EXTENSION,
                            DIR_CURRENT + "/" + Property.SUBDIR_OUTPUT_REAL.getValue() +
                                    "/" + thermograms[i].getName() + Property.POSTFIX_REAL.getValue() + EXTENSION_REAL,
                            SEPARATOR_REAL, PIXEL_SIZE, ExifParam.FOCAL_LENGTH.getValue(),
                            ExifParam.RES_X.getIntValue(), ExifParam.RES_Y.getIntValue());
                break;

            case HELP:
                Option.help();
        }
    }
}