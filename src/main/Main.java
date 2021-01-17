package main;

import com.google.gson.JsonElement;
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
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Math.*;
import static java.lang.Math.abs;


/**
 * http://www.javenue.info/post/78 - чтение и запись CSV файлов в Java.
 * https://exiftool.org/forum/index.php/topic,4898.msg23972.html#msg23972.
 * https://exiftool.org/forum/index.php?topic=4898.90.
 */
public class Main {
    private static int n = 0;
    //
    // Краткие имена скриптов.
    //

    /**
     * Скрипт, извлекающий общие для всех термограмм EXIF-параметры.
     */
    private final static String SCRIPT_GLOBAL_PARAMS = "global_params";
    /**
     * Скрипт, извлекающий из всех термограмм геометрические характеристики съёмки.
     */
    private final static String SCRIPT_THERMOGRAMS_INFO = "thermograms_info";
    /**
     * Скрипт, извлекающий из всех термограмм необработанные температурные данные.
     */
    private final static String SCRIPT_THERMOGRAMS_RAW_TEMPERATURES = "thermograms_raw_temperatures";
    /**
     * Скрипт, копирующий GPS-координаты из термограмм в картинки с дефектами.
     */
    private final static String SCRIPT_COPY_GPS = "copy_gps";


    //
    // Краткие имена файлов, содержащих исходную информацию.
    //

    /**
     * Файл с конфигурационными параметрами.
     */
    final static String CONFIG = "config.txt";
    /**
     * Файл с запрещёнными зонами.
     */
    private final static String FORBIDDEN_ZONES = "forbidden_zones.txt";
    /**
     * Файл с углами наклона трубы, переопределяющими углы по умолчанию.
     */
    private final static String CUSTOM_PIPE_ANGLES = "custom_pipe_angles.txt";
    /**
     * Файл со справкой.
     */
    private final static String HELP = "help.txt";


    //
    // Краткие имена файлов, которые редактируются скриптами.
    //

    /**
     * Файл с общими для всех термограмм EXIF-параметрами.
     */
    final static String GLOBAL_PARAMS = "global_params.txt";
    /**
     * Файл с геометрическими характеристиками съёмки.
     */
    private final static String THERMOGRAMS_INFO = "thermograms_info.txt";


    //
    // Краткие имена файлов, которые редактируются этой программой.
    //

    /**
     * Файл с площадями дефектов.
     */
    private final static String PIPE_SQUARES = "pipe_squares.txt";
    /**
     * Файл с площадями проекций дефектов.
     */
    private final static String SQUARES = "squares.txt";
    /**
     * Файл с углами наклона трубы.
     */
    private final static String PIPE_ANGLES = "angles.txt";
    /**
     * Файл с этапами вычисления углов наклона трубы.
     */
    private final static String PIPE_ANGLES_LOG = "pipe_angles_log.txt";


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
     * Расширение термограмм.
     */
    private final static String EXTENSION = ".jpg";
    /**
     * Расширение файлов с необработанными температурами (в формате CSV).
     */
    private final static String EXTENSION_RAW = ".pgm";
    /**
     * Расширение файлов с температурами (в формате CSV).
     */
    public final static String EXTENSION_REAL = ".csv";
    /**
     * Разделитель значений в файле с необработанными температурами.
     */
    private final static char SEPARATOR_RAW = ' ';
    /**
     * Разделитель значений в файле с температурами.
     */
    public final static char SEPARATOR_REAL = ';';


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
                BufferedReader reader = Files.newBufferedReader(Paths.get(Helper.filename(DIR_CURRENT, Main.HELP)));
                String line;
                while ((line = reader.readLine()) != null)
                    System.out.println(line);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private static Option getByAlias(String alias) {
            return switch (alias) {
                case "-gp" -> GLOBAL_PARAMS;
                case "-ti" -> THERMOGRAMS_INFO;
                case "-trt" -> THERMOGRAMS_RAW_TEMPERATURES;
                case "-csv" -> CSV;
                case "-d" -> DEFECTS;
                default -> HELP;
            };
        }
    }


    /**
     * Выдаёт пиксель, который расположен на отрезке, соединяющим пиксель {@code start} и пиксель, находящийся на
     * расстоянии, эквивалентном земному расстоянию {@code length}, и угловом расстоянии {@code angle} от пикселя
     * {@code start}, температура которого отличается от температуры предыдущего пикселя не менее, чем на
     * {@code tempJump} гр. Ц. Если таких пикселей несколько, то выдаётся пиксель, который наиболее удалён от пикселя
     * {@code start}. Сам пиксель {@code start} отрезку не принадлежит.
     * <p>
     * Выдаёт средннюю температуру {@code numberEndPixels} пикселей, которые находятся в конце упомянутого выше отрезка. (Это
     * количество может быть уменьшено, если число пикселей в отрезке меньше {@code numberEndPixels}.)
     * <p>
     * Неинформативные случаи:
     * - длина отрезка равна 0, т. е. пиксель {@code start} совпадает с другим концом этого отрезка (в этом случае
     * выдаётся пиксель {@code (-2,-2)} и нулевая температура),
     * - длина отрезка {@code >0}, но на нём нет температурного контраста, т. е. пиксель, чьё вычисление описано в
     * начале, отсутствует (в этом случае выдаётся пиксель {@code (-1,-1)}).
     *
     * @param angle           угол (в град.), отсчитываемый от положительного направления оси c'y' по часовой стрелке,
     *                        принадлежащий промежутку {@code (-180,180]}
     * @param length          земная длина (в м.) рассматриваемого отрезка
     * @param tempJump        разность температур (в гр. Ц.) между соседними пикселями, при превышении которой запоминается
     *                        пиксель
     * @param numberEndPixels максимальное число пикселей, по которым рассчитывается средняя температура
     */
    public static Object[] findJump(Pixel start, double angle, double length, double tempJump, int numberEndPixels,
                                    double[][] realTable, double height, double pixelSize, double focalLength,
                                    int resX, int resY) {

        List<Pixel> jumps = new ArrayList<>();
        List<Double> temperatures = new ArrayList<>();
        int sI = start.getI();
        int sJ = start.getJ();

        int iInc = (int) Math.round(Thermogram.earthToDiscreteMatrix(length * sin(angle * PI / 180), height,
                pixelSize, focalLength));
        int jInc = (int) Math.round(Thermogram.earthToDiscreteMatrix(length * cos(angle * PI / 180), height,
                pixelSize, focalLength));

        int iIncSign = (int) signum(iInc);
        int jIncSign = (int) signum(jInc);

        Pixel end = new Pixel(
                sI + iInc >= 0 && sI + iInc < resX ? sI + iInc : (sI + iInc < 0 ? 0 : resX - 1),
                sJ + jInc >= 0 && sJ + jInc < resY ? sJ + jInc : (sJ + jInc < 0 ? 0 : resY - 1));

        int eI = end.getI();
        int eJ = end.getJ();

        int jPrev = -1;

        if (start.equals(end)) return new Object[]{new Pixel(-2, -2), 0.};

        // Случай start=end рассмотрен выше, поэтому здесь start!=end и, следовательно, хотя бы один из инкрементов
        // отличен от 0.

        // Если проекция отрезка, соединяющего точки start и end, на ось c'x' меньше проекции на ось c'y', то
        // рассматриваем отрезок, симметричный упомянутому отрезку, относительно прямой j=i.
        boolean inversion = false;
        if (abs(eI - sI) < abs(eJ - sJ)) {
            inversion = true;
            int t;

            t = sI;
            sI = sJ;
            sJ = t;

            t = eI;
            eI = eJ;
            eJ = t;

            t = iIncSign;
            iIncSign = jIncSign;
            jIncSign = t;
        }

        // Здесь |eI-sI|>=|eJ-sJ|. Следовательно, iIncSign!=0 и sI!=eI (иначе start=end). Значит, хотя бы одна итерация
        // состоится (и тем самым, список temperatures окажется непустым).
        for (int i = sI + iIncSign; iIncSign > 0 ? i <= eI : i >= eI; i = i + iIncSign) {
            int j = (int) round(AbstractPoint.linearFunction(i, new Pixel(sI, sJ), new Pixel(eI, eJ)));

            double currTemp, prevTemp = -1000;
            if (!inversion) {
                currTemp = realTable[resY - 1 - j][i];
                if (i != sI + iIncSign)
                    prevTemp = realTable[resY - 1 - jPrev][i - iIncSign];
            } else {
                currTemp = realTable[resY - 1 - i][j];
                if (i != sI + iIncSign)
                    prevTemp = realTable[resY - 1 - (i - iIncSign)][jPrev];
            }

            temperatures.add(currTemp);
            if (i != sI + iIncSign && abs(currTemp - prevTemp) >= tempJump)
                jumps.add(!inversion ? new Pixel(i, j) : new Pixel(j, i));
            jPrev = j;
        }

        double avEndTemp = 0;
        for (int i = 0; i < min(numberEndPixels, temperatures.size()); i++)
            avEndTemp += temperatures.get(temperatures.size() - 1 - i);
        avEndTemp = avEndTemp / min(numberEndPixels, temperatures.size());

        return new Object[]{jumps.size() > 0 ? jumps.get(jumps.size() - 1) : new Pixel(-1, -1), avEndTemp};
    }

    /**
     * Конвертирует таблицу с реальными температурами {@code realTable} в список укрупнённых многоугольников.
     */
    private static List<Polygon<Point>> realTableToEnlargedPolygons(Thermogram thermogram, double[][] realTable,
                                                                    double tMin, double tMax, int minPixelSquare,
                                                                    int distance, Polygon<Pixel> overlap,
                                                                    int maxLength, double focalLength, double pixelSize, int resY,
                                                                    BiPredicate<Polygon<Point>, Polygon<Point>> condition) {

        int[][] binTable = Helper.findIf(realTable, num -> num > tMin && num < tMax);
        Helper.nullifyRectangles(binTable, thermogram.getForbiddenZones(), resY);

        List<Rectangle<Point>> ranges = Rectangle.findRectangles(binTable, maxLength, focalLength);
        ranges.removeIf(range -> range.squarePixels() < minPixelSquare);

        List<Polygon<Point>> polygons = Polygon.toPolygons(ranges, overlap, thermogram.getHeight(), focalLength,
                pixelSize, resY);

        try {
            return Polygon.enlargeIteratively(polygons, distance, overlap, thermogram.getName(), thermogram.getHeight(),
                    focalLength, pixelSize, resY, condition);
        } catch (Exception e) {
            System.out.println("Проблема в Main.realTableToEnlargedPolygons(): ошибка в Polygon.enlargeIteratively().\n" +
                    "Берём изначальные дефекты.\n" +
                    "Термограмма: " + thermogram.getName() + ".");
            e.printStackTrace();
            System.out.println();
            return polygons;
        }
    }

    private static List<Pixel> findMiddlesOfPseudoDefects(Thermogram thermogram, double[][] realTable, double pixelSize,
                                                          int resY, double tMinPseudo, int minPixelSquare, int distance,
                                                          int maxLength, double focalLength, Polygon<Pixel> overlap,
                                                          List<Polygon<Point>> enlargedPolygons, int maxDiff, double k,
                                                          BiPredicate<Polygon<Point>, Polygon<Point>> condition) {

        var boundingRectangles = new ArrayList<Rectangle<Pixel>>();
        for (Polygon<Point> p : enlargedPolygons)
            boundingRectangles.add(Polygon.toPolygonPixel(p, focalLength, resY).boundingRectangle());

        List<Polygon<Point>> enlargedPolygons2;
        try {
            enlargedPolygons2 = realTableToEnlargedPolygons(thermogram, realTable, tMinPseudo, 100, minPixelSquare,
                    distance, overlap, maxLength, focalLength, pixelSize, resY, condition);
        } catch (Exception e) {
            System.out.println("Проблема в Main.findMiddlesOfPseudoDefects(): " +
                    "ошибка в Main.realTableToEnlargedPolygons() (т. е. псевдодефекты не вычисляются).\n" +
                    "Берём середины окаймляющих прямоугольников настоящих дефектов, а не псевдодефектов.");
            e.printStackTrace();
            System.out.println();
            var middlesOfInitialDefects = new ArrayList<Pixel>();
            for (Rectangle<Pixel> br : boundingRectangles)
                middlesOfInitialDefects.add(br.middle());
            return middlesOfInitialDefects;
        }

        var boundingRectangles2 = new ArrayList<Rectangle<Pixel>>();
        for (Polygon<Point> p : enlargedPolygons2)
            boundingRectangles2.add(Polygon.toPolygonPixel(p, focalLength, resY).boundingRectangle());

        var middlesOfPseudoDefects = new ArrayList<Pixel>();
        for (Rectangle<Pixel> br : boundingRectangles) {
            boolean added = false;
            for (Rectangle<Pixel> br2 : boundingRectangles2)
                if (br.isIn(br2, maxDiff)) {
                    middlesOfPseudoDefects.add((br2.squarePixels() > k * br.squarePixels() ? br2 : br).middle());
                    added = true;
                    break;
                }
            if (!added) middlesOfPseudoDefects.add(br.middle());
        }

        return middlesOfPseudoDefects;
    }

    /**
     * Если сдвиг невозможен, то возвращает пиксель {@code (-10,-10)}.
     */
    private static Pixel shiftPixel(Pixel pixel, int[][] right, int[][] left, double[] avEndTemp,
                                    List<Integer> anglesWithNoAvEndTemp, int resX, int resY) {

        int half = right.length;
        double[] diff = new double[half];
        double[] avR = new double[half];
        double[] avL = new double[half];
        int[] numberOfRightExcludedIndices = new int[half];
        int[] numberOfLeftExcludedIndices = new int[half];
        var permittedDiameters = new ArrayList<Integer>();

        for (int i = 0; i < half; i++) {
            for (int j = 0; j < half - 1; j++) {
                if (!anglesWithNoAvEndTemp.contains(right[i][j]))
                    avR[i] += avEndTemp[right[i][j]];
                else
                    numberOfRightExcludedIndices[i]++;
                if (!anglesWithNoAvEndTemp.contains(left[i][j]))
                    avL[i] += avEndTemp[left[i][j]];
                else
                    numberOfLeftExcludedIndices[i]++;
            }

            if (numberOfRightExcludedIndices[i] < half - 1 && numberOfLeftExcludedIndices[i] < half - 1) {
                permittedDiameters.add(i);
                avR[i] /= (half - 1) - numberOfRightExcludedIndices[i];
                avL[i] /= (half - 1) - numberOfLeftExcludedIndices[i];
            }

            diff[i] = abs(avL[i] - avR[i]);
        }

        if (permittedDiameters.size() == 0) return new Pixel(-10, -10);

        int indexMax = Helper.findIndexOfMax(diff, permittedDiameters);

        // group - номер полуинтеравала (начиная с 0), содержащего indexMax, среди всех 4-x полуинтервалов, включающих
        // левую границу, длины half/4, образующих промежуток [0,half).
        int group = 0;
        if (indexMax >= half / 4)
            group++;
        if (indexMax >= half / 2)
            group++;
        if (indexMax >= 3 * half / 4)
            group++;

        // Положительный shift означает сдвиг в область правых индексов, а отрицательный - в область левых.
        // Сдвигаемся в область тёплой температуры.
        int shift = avR[indexMax] > avL[indexMax] ? 1 : -1;
        int iIncrement = 2, jIncrement = 2;
        switch (group) {
            case 0:
                iIncrement = shift;
                jIncrement = 0;
                break;
            case 1:
                iIncrement = shift;
                jIncrement = -shift;
                break;
            case 2:
                iIncrement = 0;
                jIncrement = -shift;
                break;
            case 3:
                iIncrement = -shift;
                jIncrement = -shift;
                break;
        }

        Pixel shiftedPixel = new Pixel(pixel.getI() + iIncrement, pixel.getJ() + jIncrement);
        if (shiftedPixel.getI() >= 0 && shiftedPixel.getI() < resX &&
                shiftedPixel.getJ() >= 0 && shiftedPixel.getJ() < resY) {

            return shiftedPixel;
        } else return new Pixel(-10, -10);
    }

    private static double findPipeAngle(Pixel pixel, Polygon<Point> polygon, int num, Thermogram thermogram,
                                        double diameter, double coef, double tempJump, int numberEndPixels, double dec,
                                        double eps, int maxIter, double[][] realTable, String rawDefectsFilename,
                                        ReentrantReadWriteLock lock, String pipeAnglesLogFilename, double pixelSize,
                                        double focalLength, int resX, int resY) {

        Polygon<Pixel> polygon1 = Polygon.toPolygonPixel(polygon, focalLength, resY);
        double d = Thermogram.earthToDiscreteMatrix(diameter, thermogram.getHeight(), pixelSize, focalLength);
        int w = polygon1.width();
        int h = polygon1.height();
        Helper.log(pipeAnglesLogFilename, "       ===  " + thermogram.getName() + ",  polygon " + num + "  ===\n");
        Helper.log(pipeAnglesLogFilename, "Polygon (" + w + "x" + h + "): " + polygon1 + ".\n");

        int eps1 = 2;
        int eps2 = 4;

        /*// Многоугольник polygon является отчётливо горизонтальным (относительно термограммы).
        if (w >= d + eps2 && ((d - eps1 <= h && h <= d) || (h > d && w > h))) {
            Helper.log(pipeAnglesLogFilename, "Многоугольник является отчётливо горизонтальным => pipeAngle=0.\n\n\n");
            return 0;
        }

        // Многоугольник polygon является отчётливо вертикальным (относительно термограммы).
        if (h >= d + eps2 && ((d - eps1 <= w && w <= d) || (w > d && h > w))) {
            Helper.log(pipeAnglesLogFilename, "Многоугольник является отчётливо вертикальным => pipeAngle=90.\n\n\n");
            return 90;
        }*/

        // При другом l алгоритм работает некорректно, но можно адаптировать его для произвольного l, кратного n(=8).
        int l = 8;
        double[] angles = new double[l];
        // 0, 45, 90, 135, 180, -135, -90, -45 (при l=8)
        for (int i = 0; i < l; i++)
            angles[i] = i * 360. / l - (i * 360 / l <= 180 ? 0 : 360);

        // Круг разделён на 8 частей координатными осями и биссектрисами координатных четвертей. Полученные точки
        // пересечения нумеруются от 0, начиная с верхней точки, по часовой стрелке. Круг также разделён на l углов;
        // точке с номером i соответствует угол с номером i*l/8.
        //
        // Рассматриваются 4 диаметра, которые соединяют точки 0 и 4,... , 3 и 7.
        // С этими диаметрами связаны углы их наклона.
        //
        // Рассматриваются 8 почти-диаметров (т. е. ломаных, состоящих из двух радиусов, заканчивающихся в вышеописанных
        // точках, угол между которыми равен 135).
        // С этими почти-диаметрами связаны углы наклона прямых, образующих углы 22.5 с составляющими их радиусами.
        //
        // Углы наклона отсчитываются от положительного направления оси c'x' против часовой стрелки и принадлежат
        // промежутку (-90,90].
        int n = 8;        // число частей, на которые делим круг
        int q = l / n;    // число углов, приходящихся на 1/n часть круга
        int half = l / 2; // разность номеров диаметрально противоположных углов
        Point[] diameters = new Point[n / 2];
        double[] diameterAngles = new double[n / 2];
        for (int i = 0; i < n / 2; i++) {
            diameters[i] = new Point(i * q, i * q + half);
            diameterAngles[i] = 90 - 45 * i;
        }
        Point[] almostDiameters = new Point[n];
        double[] almostDiameterAngles = new double[n];
        for (int i = 0; i < n; i++) {
            almostDiameters[i] = new Point(i * q, (i + 1) * q + half - ((i + 1) * q + half < l ? 0 : l));
            double tmp = (90 + 45) / 2. - 45 * i;
            almostDiameterAngles[i] = tmp + (tmp <= -90 ? 180 : 0);
        }

        // Рассматривается half диаметров, чьи углы наклона, отсчитываемые относительно положительного направления оси
        // c'y' по часовой стрелке, изменяются от angles[0] до angles[half-1].
        //
        // Для 0-го диаметра right[0][] - множество индексов углов из массива angles, лежащих справа от этого диаметра,
        // а left[0][] - слева. При увеличении номера диаметра, диаметр поворачивается по часовой стрелке, и вслед за
        // ним также поворачиваются эти множества индексов.
        int[][] right = new int[half][half - 1];
        int[][] left = new int[half][half - 1];
        for (int i = 0; i < half; i++)
            for (int j = 0; j < half - 1; j++) {
                right[i][j] = i + j + 1;
                left[i][j] = right[i][j] + half - (right[i][j] + half < l ? 0 : l);
            }

        Object[][] jumps = new Object[l][];
        Pixel[] jumpPixel = new Pixel[l];
        double[] avEndTemp = new double[l];
        var anglesWithNoJumpPixel = new ArrayList<Integer>();

        int iter = 0;
        int i1 = -1, i2 = -1;
        double pipeAngle = -1000;

        Helper.log(pipeAnglesLogFilename, "");

        while (iter < maxIter) {
            if (pixel.equals(new Pixel(-10, -10))) {
                Helper.log(pipeAnglesLogFilename, "--- Сдвиг pixel невозможен. ---\n");
                break;
            }
            iter++;
            Helper.log(pipeAnglesLogFilename, "            === iter:  " + iter + " ===\n");

            double inclination1, inclination2;
            double inclination1Old, inclination2Old;
            double pipeAngleOld;

            double tMax1 = -1000;
            double tMax2 = -1000;

            var range1 = new ArrayList<Integer>();
            var range2 = new ArrayList<Integer>();

            anglesWithNoJumpPixel.clear();
            var anglesWithNoAvEndTemp = new ArrayList<Integer>();

            for (int i = 0; i < l; i++) {
                jumps[i] = findJump(pixel, angles[i], coef * diameter, tempJump, numberEndPixels,
                        realTable, thermogram.getHeight(), pixelSize, focalLength, resX, resY);
                jumpPixel[i] = (Pixel) jumps[i][0];
                avEndTemp[i] = (double) jumps[i][1];
                if (jumpPixel[i].equals(new Pixel(-1, -1)) || jumpPixel[i].equals(new Pixel(-2, -2)))
                    anglesWithNoJumpPixel.add(i);
                if (jumpPixel[i].equals(new Pixel(-2, -2)))
                    anglesWithNoAvEndTemp.add(i);
            }

            if (anglesWithNoAvEndTemp.size() <= l - 2) {
                for (int i = 0; i < l; i++)
                    if (!anglesWithNoAvEndTemp.contains(i) && avEndTemp[i] > tMax1) {
                        i1 = i;
                        tMax1 = avEndTemp[i1];
                    }
                for (int i = 0; i < l; i++)
                    if (!anglesWithNoAvEndTemp.contains(i) && avEndTemp[i] > tMax2 && i != i1) {
                        i2 = i;
                        tMax2 = avEndTemp[i2];
                    }
                // Упорядочиваем i1 и i2, чтобы стало i1<i2. Эти индексы могут быть соседними.
                int tmp = i1;
                i1 = min(i1, i2);
                i2 = max(tmp, i2);
            } else {
                if (iter > 1) {
                    Helper.log(pipeAnglesLogFilename, "Значения i1, i2 остаются с предыдущей итерации, т. к. не " +
                            "могут быть корректно вычислены из-за того, что anglesWithNoAvEndTemp.size>l-2." + "\n");
                } else {
                    i1 = 0;
                    i2 = half;
                    Helper.log(pipeAnglesLogFilename, "В качестве значений i1, i2 берутся " + i1 + ", " + i2 + ", " +
                            "т. к. не могут быть корректно вычислены из-за того, что anglesWithNoAvEndTemp.size>l-2, " +
                            "и итерация 1-я." + "\n");
                }
            }

            Helper.log(pipeAnglesLogFilename, "aMaxs:   " + angles[i1] + "   " + angles[i2] + "\n");

            // Разделяем все индексы углов, за исключением индексов i1 и i2, на две (непустые) части: range1 и range2.

            // i1 и i2 - не соседние.
            // range1 - индексы между i1 и i2, range2 - индексы между i2 и i1.
            if (i2 - i1 > 1 && !(i1 == 0 && i2 == l - 1)) {
                for (int k = i1 + 1; k < i2; k++)
                    range1.add(k);
                for (int k = i2 + 1; k < i1 + l; k++)
                    range2.add(k - (k < l ? 0 : l));
            } else
                // i1 и i2 - соседние, кроме случая i1=0, i2=l-1.
                // range1 - 1-я половина индексов между i2 и i1, range2 - 2-я половина.
                if (i2 - i1 == 1) {
                    for (int k = i2 + 1; k < i2 + half; k++)
                        range1.add(k - (k < l ? 0 : l));
                    for (int k = i2 + half; k < l + i1; k++)
                        range2.add(k - (k < l ? 0 : l));
                }
                // Случай i1=0, i2=l-1.
                // range1 - 1-я половина индексов между i1 и i2, range2 - 2-я половина.
                else {
                    for (int k = 1; k < half; k++)
                        range1.add(k);
                    for (int k = half; k < l - 1; k++)
                        range2.add(k);
                }

            Helper.log(pipeAnglesLogFilename, "ranges:   " + Arrays.toString(range1.toArray()) + "   " +
                    Arrays.toString(range2.toArray()) + "\n");

            var range1Corr = new ArrayList<>(range1);
            var range2Corr = new ArrayList<>(range2);

            if (range1Corr.removeAll(anglesWithNoJumpPixel))
                Helper.log(pipeAnglesLogFilename, "range1Corr:   " + range1Corr);
            if (range2Corr.removeAll(anglesWithNoJumpPixel))
                Helper.log(pipeAnglesLogFilename, "range2Corr:   " + range2Corr);

            var sr1 = new SimpleRegression();
            var sr2 = new SimpleRegression();

            for (int i : range1Corr)
                sr1.addData(jumpPixel[i].getI(), jumpPixel[i].getJ());
            for (int i : range2Corr)
                sr2.addData(jumpPixel[i].getI(), jumpPixel[i].getJ());

            Helper.log(pipeAnglesLogFilename, "slopes:   " + sr1.getSlope() + "   " + sr2.getSlope() + "\n");

            // inclination1 (inclination2) - угол наклона прямой, аппроксимирующей точки массива jumpPixel, чьи индексы
            // прнадлежат списку range1Corr (range2Corr). Сначала принадлежит интервалу (-90,90) или равен NaN, потом
            // добавляется значение 90 для случая, когда точки из того или иного диапазона расположены примерно по
            // вертикали.
            inclination1 = atan(sr1.getSlope()) * 180 / PI;
            inclination2 = atan(sr2.getSlope()) * 180 / PI;

            inclination1Old = inclination1;
            inclination2Old = inclination2;

            int[] tmp1 = AbstractPoint.findMinAndMax(jumpPixel, range1Corr, Pixel::getI);
            int[] tmp2 = AbstractPoint.findMinAndMax(jumpPixel, range2Corr, Pixel::getI);
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

            if (Helper.compare(inclination1, inclination1Old) && Helper.compare(inclination2, inclination2Old))
                Helper.log(pipeAnglesLogFilename, "inclinations:   " + inclination1 + "   " + inclination2 + "\n");
            else {
                Helper.log(pipeAnglesLogFilename, "inclinations (initial):   " + inclination1Old + "   " +
                        inclination2Old);
                Helper.log(pipeAnglesLogFilename, "inclinations (after change):   " + inclination1 + "   " +
                        inclination2 + "\n");
            }

            // pipeAngle - угол наклона биссектрисы острого угла между прямыми, чьи углы наклона равны inclination1 и
            // inclination2, относительно положительного направления оси c'x', отсчитываемый против часовой стрелки.
            // (Если эти прямые перпендикулярны, то берётся биссектриса, которая ближе всего к оси c'x'. Если же эти
            // биссектрисы равноудалены от оси c'x', то берётся биссектриса, чей угол наклона положителен.)
            //
            // Сначала принадлежит промежутку (-90,90].
            // Трактуется как угол наклона трубы.
            if (range1Corr.size() < 2 && range2Corr.size() > 1)
                pipeAngle = inclination2;
            else if (range2Corr.size() < 2 && range1Corr.size() > 1)
                pipeAngle = inclination1;
            else if (range1Corr.size() < 2) {
                pipeAngle = 90; // Эту ситуацию можно обрабатывать более точно, например, сдвигом или изм. длины.
                Helper.log(pipeAnglesLogFilename, "В качестве pipeAngle берётся 90, т. к. range1Corr.size," +
                        "range2Corr.size<2.\n");
            } else {
                double v = (inclination1 + inclination2) / 2;
                pipeAngle = v + (inclination1 * inclination2 < 0 && abs(inclination1) + abs(inclination2) > 90 ?
                        (v <= 0 ? 90 : -90) : 0);
            }
            pipeAngleOld = pipeAngle + (pipeAngle < 0 ? 180 : 0);

            if (inclination1 == 90 || inclination2 == 90)
                pipeAngle = 90;

            // Теперь pipeAngle принадлежит промежутку [0,180).
            pipeAngle = pipeAngle + (pipeAngle < 0 ? 180 : 0);

            if (pipeAngle == pipeAngleOld) {
                Helper.log(pipeAnglesLogFilename, "============================");
                Helper.log(pipeAnglesLogFilename, "=   " + (round(pipeAngle * 100) / 100.) + "   (pipeAngle)");
            } else {
                Helper.log(pipeAnglesLogFilename, "    " + (round(pipeAngleOld * 100) / 100.) +
                        "   (pipeAngle (initial))");
                Helper.log(pipeAnglesLogFilename, "============================");
                Helper.log(pipeAnglesLogFilename, "=   " + (round(pipeAngle * 100) / 100.) +
                        "   (pipeAngle (after change))");
            }
            Helper.log(pipeAnglesLogFilename, "============================\n");

            String standardDirection = "";
            double standardAngle = -1000;
            String standardDirectionName = "";

            int dInd = -1, aDInd = -1;
            for (int i = 0; i < n / 2; i++)
                if (new Point(i1, i2).equals(diameters[i])) {
                    dInd = i;
                    standardDirection = diameters[dInd].getI() + "-" + diameters[dInd].getJ();
                    standardAngle = diameterAngles[dInd];
                    standardDirectionName = "diameter";
                    break;
                }
            if (dInd == -1)
                for (int i = 0; i < n; i++)
                    if (new Point(i1, i2).equals(almostDiameters[i]) || new Point(i2, i1).equals(almostDiameters[i])) {
                        aDInd = i;
                        standardDirection = almostDiameters[aDInd].getI() + "-" + almostDiameters[aDInd].getJ();
                        standardAngle = almostDiameterAngles[aDInd];
                        standardDirectionName = "almostDiameter";
                        break;
                    }

            // Определяет, отстоит ли прямая с углом наклона pipeAngle от прямой с углом наклона standardAngle не более,
            // чем на eps.
            boolean isCloseToStandardDirection = Helper.close(pipeAngle,
                    standardAngle + (standardAngle < 0 ? 180 : 0), eps);

            // Значения i1, i2 соответствуют эталонному направлению (диаметру или почти-диаметру), но угол pipeAngle
            // сильно отличается от эталонного угла.
            if ((dInd != -1 || aDInd != -1) && !isCloseToStandardDirection) {
                if (iter < maxIter) {
                    coef *= dec;
                    Helper.log(pipeAnglesLogFilename, "Эталонное направление:   " + standardDirection +
                            " (" + standardDirectionName + "),   эталонный угол:   " + standardAngle + ",   " +
                            "pipeAngle:   " + (round(pipeAngle * 100) / 100.) + ".");
                    Helper.log(pipeAnglesLogFilename, "--- Уменьшение coef, т. к. pipeAngle не соответствует " +
                            "эталонному углу. ---\n\n");
                } else {
                    pipeAngle = standardAngle + (standardAngle < 0 ? 180 : 0);
                    Helper.log(pipeAnglesLogFilename, "--- Итерации исчерпаны, в качестве pipeAngle берём угол, " +
                            "соответствующий эталонному направлению " + standardDirection +
                            " (" + standardDirectionName + "):   " + standardAngle + ".\n\n");
                }
            }
            // Эталонное направление не найдено или, в противном случае, угол pipeAngle корректен.
            else {
                // i1 и i2 - соседние или отличаются на 2 (это при l=8 равносильно отсутствию эталонного направления).
                if ((abs(i1 - i2) == 1 || (i1 == 0 && i2 == l - 1)) ||
                        (abs(i1 - i2) == 2 || (i1 == 0 && i2 == l - 2) || (i1 == 1 && i2 == l - 1))) {

                    Helper.log(pipeAnglesLogFilename, "Эталонное направление отсутствует.");
                    Helper.log(pipeAnglesLogFilename, "--- Cдвиг и уменьшение coef. ---");
                    coef *= dec;
                    Pixel shiftedPixel = shiftPixel(pixel, right, left, avEndTemp, anglesWithNoAvEndTemp, resX, resY);
                    Helper.log(pipeAnglesLogFilename, pixel + "  ->  " + shiftedPixel + "\n\n");
                    pixel = shiftedPixel;
                }
                // i1 и i2 - отличаются более чем на 2 (это при l=8 равносильно наличию эталонного направления).
                else {
                    Helper.log(pipeAnglesLogFilename, "Эталонное направление:   " + standardDirection +
                            " (" + standardDirectionName + "),   эталонный угол:   " + standardAngle + ",   " +
                            "pipeAngle:   " + (round(pipeAngle * 100) / 100.) + ".");
                    Helper.log(pipeAnglesLogFilename, "--- Прекращение итераций, т. к. pipeAngle соответствует " +
                            "эталонному углу. ---\n\n");
                    break;
                }
            }
        }

        synchronized (Main.class) {
            try {
                BufferedImage image = ImageIO.read(new File(rawDefectsFilename));

                for (int i = 0; i < l; i++) {
                    Helper.log(pipeAnglesLogFilename, String.format("%1$6s", angles[i]) + "  " +
                            String.format("%1$5s", round(avEndTemp[i] * 10) / 10.) + "  " + jumpPixel[i]);
                    if (!anglesWithNoJumpPixel.contains(i))
                        new Segment(pixel.toPoint(resY), jumpPixel[i].toPoint(resY)).draw(image, Color.BLACK);
                }

                ImageIO.write(image, "jpg", new File(rawDefectsFilename));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Helper.log(pipeAnglesLogFilename, "\n\n");
        return pipeAngle;
    }

    private static Object[] defects(Thermogram thermogram, Polygon<Pixel> overlap, double tMin, double tMax,
                                    int minPixelSquare, double diameter, double[] params, String thermogramFilename,
                                    String rawDefectsFilename, String realTempsFilename, char separatorReal,
                                    double pixelSize, int maxLength, double focalLength, int resX, int resY,
                                    String squaresFilename,
                                    String pipeAnglesFilename, String pipeAnglesLogFilename,
                                    double minIntersectionSquare, BiPredicate<Polygon<Point>, Polygon<Point>> condition,
                                    BiFunction<Double, List<Double>, Double> function, List<Double> customPipeAngles,
                                    ExecutorService executor)
            throws IOException {

        int distance = (int) params[0];
        double tMinPseudo = params[1];
        int maxDiff = (int) params[2];
        double k = params[3];
        double coef = params[4];
        double tempJump = params[5];
        int numberEndPixels = (int) params[6];
        double dec = params[7];
        double eps = params[8];
        int maxIter = (int) params[9];

        double[][] realTable = Helper.extractTable(realTempsFilename, separatorReal);

        List<Polygon<Point>> enlargedPolygons = realTableToEnlargedPolygons(thermogram, realTable, tMin, tMax,
                minPixelSquare, distance, overlap, maxLength, focalLength, pixelSize, resY, condition);

        Polygon.drawPolygons(enlargedPolygons, Polygon.toPolygonPoint(overlap, focalLength, resY),
                thermogram.getForbiddenZones(), Color.BLACK, thermogramFilename, rawDefectsFilename, focalLength, resY);

        int diameterPixel = (int) round(Thermogram.earthToDiscreteMatrix(diameter, thermogram.getHeight(), pixelSize,
                focalLength));
        Polygon<Pixel> thermogramPolygon = new Rectangle<>(new Pixel(0, 0), new Pixel(resX - 1, resY - 1))
                .toPolygon();
        double thermogramSquare = Thermogram.toEarthSquare(thermogramPolygon.square(focalLength),
                thermogram.getHeight(), focalLength, pixelSize);

        List<Pixel> middles = findMiddlesOfPseudoDefects(thermogram, realTable, pixelSize, resY, tMinPseudo,
                minPixelSquare, distance, maxLength, focalLength, overlap, enlargedPolygons, maxDiff, k, condition);

        String[][] tmpFiles = Helper.createTmpFiles(
                IntStream.range(0, enlargedPolygons.size())
                        .mapToObj(i -> "__" + String.format("%0" + (enlargedPolygons.size() + "").length() + "d", i + 1))
                        .toArray(String[]::new),
                new StringBuilder(pipeAnglesLogFilename));

        var pipeAngles = new ArrayList<>(Arrays.asList(new Double[enlargedPolygons.size()]));
        var lock = new ReentrantReadWriteLock();
        Future[] futures = new Future[enlargedPolygons.size()];
        for (int i = 0; i < enlargedPolygons.size(); i++) {
            int ii = i;
            futures[i] = executor.submit(() -> {
                Thread.currentThread().setName("Processing defect " + (ii + 1) + ": " + enlargedPolygons.get(ii) + ", " +
                        "thermogram: " + thermogram.getName());
                double pipeAngle = findPipeAngle(middles.get(ii), enlargedPolygons.get(ii), ii + 1, thermogram,
                        diameter, coef, tempJump, numberEndPixels, dec, eps, maxIter, realTable, rawDefectsFilename,
                        lock, tmpFiles[0][ii + 1], pixelSize, focalLength, resX, resY);
                pipeAngles.set(ii, function != null ? function.apply(pipeAngle, customPipeAngles) : pipeAngle);
            });
        }
        for (Future f : futures) {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        Helper.concatenateAndDelete(new StringBuilder[]{new StringBuilder(pipeAnglesLogFilename)},
                new String[]{tmpFiles[0][0]});

        var boundingDefects = new ArrayList<Rectangle<Pixel>>();
        var slopingDefects = new ArrayList<Polygon<Pixel>>();
        var defects = new ArrayList<Polygon<Pixel>>();
        var squares = new ArrayList<Double>();
        var pipeSquares = new ArrayList<Double>();

        for (int i = 0; i < enlargedPolygons.size(); i++) {
            Rectangle<Pixel> bd = Polygon.toPolygonPixel(enlargedPolygons.get(i), focalLength, resY).boundingRectangle();
            Polygon<Pixel> sd = Rectangle.slopeRectangle(bd,
                    pipeAngles.get(i) + (pipeAngles.get(i) >= 90 ? -90 : 0), resY);
            Polygon<Pixel> d = sd.widen(diameterPixel, pipeAngles.get(i));

            boundingDefects.add(bd);
            slopingDefects.add(sd);
            defects.add(d);

            double s = Polygon.squarePolygon(d, overlap, thermogramPolygon, thermogram.getHeight(), pixelSize,
                    focalLength);
            squares.add(s);
            pipeSquares.add(PI * s);
        }

        // Корректировка.
        var defectsChanged = new HashSet<Integer>();
        List<Integer> indices = IntStream.rangeClosed(0, defects.size() - 1).boxed().collect(Collectors.toList());
        indices.stream()
                .flatMap(i -> indices.stream()
                        .filter(j -> !j.equals(i))
                        .peek(j -> {
                            Polygon<Pixel> p1 = defects.get(i);
                            Polygon<Pixel> p2 = defects.get(j);

                            double pipeAngle1 = pipeAngles.get(i);
                            double pipeAngle2 = pipeAngles.get(j);

                            if (p1.getVertices().get(0) == null || p2.getVertices().get(0) == null)
                                return;

                            boolean p1Changed = false;
                            boolean p2Changed = false;

                            Intersection[] intersections = Intersection.values();

                            // Действия, т. е. функции action, в перечислении Intersection упорядочены таким образом,
                            // что сначала идут функции, не проверяющие величину площади пересечения (т. е. не выдающие
                            // 1), а затем идут функции, проверяющие это. Сделано для того, чтобы функции из 1-й группы
                            // применялись даже и в случае малости пересечения.
                            for (Intersection intersection : intersections) {
                                if (intersection.getCondition().test(p1, p2, pipeAngle1, minIntersectionSquare)) {
                                    int res = intersection.getAction().apply(p1, p2, pipeAngle1,
                                            minIntersectionSquare);
                                    if (res == 1) return;
                                    if (res == 0) {
                                        p1Changed = true;
                                        break;
                                    }
                                }
                            }

                            if (!p1Changed) {
                                for (Intersection intersection : intersections) {
                                    if (intersection.getCondition().test(p2, p1, pipeAngle2, minIntersectionSquare)) {
                                        int res = intersection.getAction().apply(p2, p1, pipeAngle2,
                                                minIntersectionSquare);
                                        if (res == 0) {
                                            p2Changed = true;
                                            break;
                                        }
                                    }
                                }
                            }

                            if (p1Changed) defectsChanged.add(i);
                            if (p2Changed) defectsChanged.add(j);

                            if (p1.getVertices().get(0) != null && p2.getVertices().get(0) != null &&
                                    Polygon.getIntersection(p1, p2, -1).square(-1) > minIntersectionSquare)

                                System.out.println("Дефекты " + (i + 1) + " и " + (j + 1) + " на термограмме " +
                                        thermogram.getName() + " по-прежнему пересекаются существенным образом " +
                                        "(т. е. площадь пересечения >" + minIntersectionSquare + " п.):\n" +
                                        "1. " + p1 + ",\n2. " + p2 + ".");
                        }))
                .collect(Collectors.toList());

        for (int i : defectsChanged) {
            Polygon<Pixel> d = defects.get(i);
            if (d.getVertices().get(0) != null) {
                double s = Polygon.squarePolygon(d, overlap, thermogramPolygon, thermogram.getHeight(), pixelSize,
                        focalLength);
                squares.set(i, s);
                pipeSquares.set(i, PI * s);
            }
        }

        for (int i = defects.size() - 1; i >= 0; i--) {
            if (defects.get(i).getVertices().get(0) == null) {
                defects.remove(i);
                pipeAngles.remove(i);
                squares.remove(i);
                pipeSquares.remove(i);
            }
        }

        double totalSquare = squares.stream().mapToDouble(Double::doubleValue).sum();

        Helper.log(squaresFilename, thermogram.getName() + "   " + Helper.roundAndAppend(totalSquare, 2, 2) + "   " +
                Helper.roundAndAppend(totalSquare * 100 / thermogramSquare, 2, 2) + "   " +
                Helper.roundAndAppend(squares, 2, 2) + "\n");

        Helper.log(pipeAnglesFilename, thermogram.getName() + "   " + Helper.roundAndAppend(pipeAngles, 2, 3) + "\n");

        Object[] o = new Object[2];
        o[0] = defects;
        o[1] = pipeSquares;
        return o;
    }

    public static void main(String[] args) throws IOException {
        Option option = args.length == 1 ? Option.getByAlias(args[0]) : Option.HELP;

        switch (option) {
            case GLOBAL_PARAMS -> Helper.run(DIR_CURRENT, SCRIPT_GLOBAL_PARAMS + SCRIPT_EXTENSION, OS);

            case THERMOGRAMS_INFO -> Helper.run(DIR_CURRENT, SCRIPT_THERMOGRAMS_INFO + SCRIPT_EXTENSION, OS);

            case THERMOGRAMS_RAW_TEMPERATURES -> Helper.run(DIR_CURRENT, SCRIPT_THERMOGRAMS_RAW_TEMPERATURES + SCRIPT_EXTENSION, OS);

            case CSV -> {
                File[] files = new File(Property.DIR_THERMOGRAMS.value()).listFiles();
                String[] thermogramsNames = new String[files.length];
                for (int i = 0; i < files.length; i++)
                    thermogramsNames[i] = files[i].getName().substring(0, files[i].getName().indexOf('.'));
                for (String thermogramName : thermogramsNames)
                    Helper.rawFileToRealFile(DIR_CURRENT + "/" + Property.SUBDIR_RAW_TEMPS.value() +
                                    "/" + thermogramName + Property.POSTFIX_RAW_TEMPS.value() + EXTENSION_RAW,
                            DIR_CURRENT + "/" + Property.SUBDIR_REAL_TEMPS.value() +
                                    "/" + thermogramName + Property.POSTFIX_REAL_TEMPS.value() + EXTENSION_REAL,
                            ExifParam.RES_Y.intValue(), ExifParam.RES_X.intValue(), SEPARATOR_RAW, SEPARATOR_REAL,
                            Arrays.copyOfRange(ExifParam.readValues(), 1, ExifParam.readValues().length));
            }

            case DEFECTS -> {
                Thermogram[] thermograms = Thermogram.readThermograms(
                        Helper.filename(DIR_CURRENT, Property.SUBDIR_OUTPUT.value(), THERMOGRAMS_INFO),
                        Helper.filename(DIR_CURRENT, FORBIDDEN_ZONES));

                var customPipeAnglesLists = new ArrayList<List<Double>>();
                List<Double> defaultPipeAngles = Arrays.asList(Property.DEFAULT_PIPE_ANGLES.doubleArrayValue());

                //Map<String, List<Double>> map = Base.readStandardPipeAngles(
                //        Helper.filename(DIR_CURRENT, CUSTOM_PIPE_ANGLES));
                Map<String, List<Double>> map = Helper.mapFromFileWithJsonArray(
                        Helper.filename(DIR_CURRENT, CUSTOM_PIPE_ANGLES),
                        JsonElement::getAsDouble, "Name", "CustomPipeAngles");

                for (Thermogram thermogram : thermograms) {
                    List<Double> angles = map.get(thermogram.getName());
                    customPipeAnglesLists.add(angles == null ? defaultPipeAngles : angles);
                }

                BiFunction<Double, List<Double>, Double> function = (pipeAngle, customPipeAngles) -> {
                    List<Double> angleDistances = customPipeAngles.stream()
                            .map(customPipeAngle -> min(abs(pipeAngle - customPipeAngle),
                                    180 - abs(pipeAngle - customPipeAngle)))
                            .collect(Collectors.toList());
                    return customPipeAngles.get(angleDistances.indexOf(Collections.min(angleDistances)));
                };

                // Сохранение 10-ти последних параметров из конфиг. файла в массив. На этих параметрах основывается
                // метод defects().
                double[] params = new double[10];
                for (int i = 0; i < 10; i++)
                    params[i] = Property.values()[Property.values().length - 10 + i].doubleValue();

                var outputFiles = new StringBuilder[4];
                for (int i = 0; i < outputFiles.length; i++)
                    outputFiles[i] = new StringBuilder().insert(0, Helper.filename(DIR_CURRENT,
                            new Property[]{Property.SUBDIR_OUTPUT, Property.SUBDIR_AUXILIARY, Property.SUBDIR_AUXILIARY,
                                    Property.SUBDIR_AUXILIARY}[i].value(),
                            new String[]{PIPE_SQUARES, SQUARES, PIPE_ANGLES, PIPE_ANGLES_LOG}[i]));

                String[][] tmpFiles = Helper.createTmpFiles(
                        IntStream.range(0, thermograms.length)
                                .mapToObj(i -> "__" + String.format("%0" + (thermograms.length + "").length() + "d", i + 1) +
                                        "__" + thermograms[i].getName())
                                .toArray(String[]::new),
                        outputFiles);

                int threadPoolSize = (int) Math.ceil(Runtime.getRuntime().availableProcessors() / 2.);
                ExecutorService executorThermograms = Executors.newFixedThreadPool(threadPoolSize);
                ExecutorService executorDefects = Executors.newFixedThreadPool(threadPoolSize);

                class Processing implements Runnable {
                    final int i; // номер термограммы, подлежащей обработке
                    final String pipeSquaresTmpFilename;
                    final String squaresTmpFilename;
                    final String pipeAnglesTmpFilename;
                    final String pipeAnglesLogTmpFilename;

                    public Processing(int i, String pipeSquaresTmpFilename, String squaresTmpFilename,
                                      String pipeAnglesTmpFilename, String pipeAnglesLogTmpFilename) {
                        this.i = i;
                        this.pipeSquaresTmpFilename = pipeSquaresTmpFilename;
                        this.squaresTmpFilename = squaresTmpFilename;
                        this.pipeAnglesTmpFilename = pipeAnglesTmpFilename;
                        this.pipeAnglesLogTmpFilename = pipeAnglesLogTmpFilename;
                    }

                    @Override
                    public void run() {
                        Thermogram thermogram = thermograms[i];
                        String thermogramName = thermogram.getName();
                        Thread.currentThread().setName("thread-" + thermogramName);
                        System.out.println("start " + thermogramName);

                        Thermogram previous = thermograms[i - 1 >= 0 ? i - 1 : thermograms.length - 1];

                        Polygon<Pixel> overlap = thermogram.getOverlapWith(previous, ExifParam.FOCAL_LENGTH.value(),
                                Property.PIXEL_SIZE.doubleValue() / 1000_000,
                                new Pixel(Property.PRINCIPAL_POINT_X.intValue(), Property.PRINCIPAL_POINT_Y.intValue()),
                                ExifParam.RES_X.intValue(), ExifParam.RES_Y.intValue());

                        var thermogramFilename = new StringBuilder();
                        var rawDefectsFilename = new StringBuilder();
                        var defectsFilename = new StringBuilder();
                        var realTempsFilename = new StringBuilder();

                        var files = new StringBuilder[]{thermogramFilename, rawDefectsFilename, defectsFilename,
                                realTempsFilename};

                        for (int i = 0; i < files.length; i++)
                            files[i].insert(0, Helper.filename(
                                    new String[]{null, DIR_CURRENT, DIR_CURRENT, DIR_CURRENT}[i],
                                    new Property[]{Property.DIR_THERMOGRAMS, Property.SUBDIR_RAW_DEFECTS,
                                            Property.SUBDIR_DEFECTS, Property.SUBDIR_REAL_TEMPS}[i].value(),
                                    thermogramName + new String[]{EXTENSION, Property.POSTFIX_RAW_DEFECTS.value() +
                                            EXTENSION, Property.POSTFIX_DEFECTS.value() + EXTENSION,
                                            Property.POSTFIX_REAL_TEMPS.value() + EXTENSION_REAL
                                    }[i]));

                        if (!new File(String.valueOf(thermogramFilename)).isFile())
                            thermogramFilename.replace(0, thermogramFilename.length(),
                                    thermogramFilename.substring(0, thermogramFilename.length() - EXTENSION.length()) +
                                            EXTENSION.toUpperCase());

                        int diameterPixel = (int) round(Thermogram.earthToDiscreteMatrix(
                                Property.DIAMETER.doubleValue(), thermogram.getHeight(),
                                Property.PIXEL_SIZE.doubleValue() / 1000_000, ExifParam.FOCAL_LENGTH.value()));

                        BiPredicate<Polygon<Point>, Polygon<Point>> condition = (p1, p2) -> {
                            int[] ii1 = p1.findMinAndMax(Point::getI);
                            int[] jj1 = p1.findMinAndMax(Point::getJ);
                            int[] ii2 = p2.findMinAndMax(Point::getI);
                            int[] jj2 = p2.findMinAndMax(Point::getJ);
                            int[] ii = new int[]{min(ii1[0], ii2[0]), max(ii1[1], ii2[1])};
                            int[] jj = new int[]{min(jj1[0], jj2[0]), max(jj1[1], jj2[1])};

                            int iLength = ii[1] - ii[0];
                            int jLength = jj[1] - jj[0];

                            return (iLength <= round(Property.K1.doubleValue() * diameterPixel) ||
                                    jLength <= round(Property.K1.doubleValue() * diameterPixel)) &&
                                    (iLength <= round(Property.K2.doubleValue() * diameterPixel) &&
                                            jLength <= round(Property.K2.doubleValue() * diameterPixel));
                        };

                        Object[] o;
                        try {
                            o = defects(thermogram, overlap, Property.T_MIN.doubleValue(), Property.T_MAX.doubleValue(),
                                    Property.MIN_PIXEL_SQUARE.intValue(), Property.DIAMETER.doubleValue(), params,
                                    thermogramFilename.toString(), rawDefectsFilename.toString(),
                                    realTempsFilename.toString(), SEPARATOR_REAL,
                                    Property.PIXEL_SIZE.doubleValue() / 1000_000,
                                    (int) round(Property.K3.doubleValue() * diameterPixel), ExifParam.FOCAL_LENGTH.value(),
                                    ExifParam.RES_X.intValue(), ExifParam.RES_Y.intValue(), squaresTmpFilename,
                                    pipeAnglesTmpFilename, pipeAnglesLogTmpFilename,
                                    Property.MIN_INTERSECTION_SQUARE.doubleValue(),
                                    Property.K1.doubleValue() != -1 ?
                                            (Property.K1.doubleValue() != -2 ? condition :
                                                    (p1, p2) -> false) : null,
                                    Property.DEFAULT_PIPE_ANGLES.doubleArrayValue()[0] != -1 ? function : null,
                                    customPipeAnglesLists.get(i), executorDefects);
                        } catch (Throwable e) {
                            System.out.println("Термограмма " + thermogramName + " не обработана.");
                            e.printStackTrace();
                            System.out.println();
                            return;
                        }

                        var defects = (ArrayList<Polygon<Pixel>>) o[0];
                        var pipeSquares = (ArrayList<Double>) o[1];

                        Helper.log(pipeSquaresTmpFilename, thermogramName + "   " +
                                Helper.roundAndAppend(pipeSquares.stream().mapToDouble(Double::doubleValue).sum(), 2, 2) +
                                "   " + Helper.roundAndAppend(pipeSquares, 2, 2) + "\n");

                        Polygon.drawPolygons(defects, Polygon.toPolygonPoint(overlap, ExifParam.FOCAL_LENGTH.value(),
                                ExifParam.RES_Y.intValue()), thermogram.getForbiddenZones(), Color.BLACK,
                                thermogramFilename.toString(), defectsFilename.toString(), ExifParam.RES_Y.intValue(),
                                ExifParam.FOCAL_LENGTH.value());

                        synchronized (Main.class) {
                            n++;
                            System.out.println("finish " + thermogramName + "   [ " +
                                    Helper.roundAndAppend(Math.floor(100 * (100. * n / thermograms.length)) / 100, 2, 3) +
                                    " % обработано,  осталось " + String.format("%" + (thermograms.length + "").length() + "d",
                                    thermograms.length - n) + " ]");
                        }
                    }
                }

                Future[] futures = new Future[thermograms.length];
                for (int i = 0; i < thermograms.length; i++) {
                    futures[i] = executorThermograms.submit(new Processing(i,
                                    tmpFiles[0][i + 1], tmpFiles[1][i + 1], tmpFiles[2][i + 1], tmpFiles[3][i + 1]),
                            "Thermogram: " + thermograms[i].getName());
                }
                for (Future f : futures) {
                    try {
                        f.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }

                executorThermograms.shutdown();
                try {
                    executorThermograms.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                executorDefects.shutdown();
                try {
                    executorDefects.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                System.out.println("Обработка термограмм завершена.");

                Helper.concatenateAndDelete(outputFiles, IntStream.range(0, outputFiles.length)
                        .mapToObj(i -> tmpFiles[i][0])
                        .toArray(String[]::new));

                Helper.run(DIR_CURRENT, SCRIPT_COPY_GPS + SCRIPT_EXTENSION, OS);
            }

            case HELP -> Option.help();
        }
    }
}