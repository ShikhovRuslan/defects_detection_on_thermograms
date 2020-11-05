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


    /**
     * Выдаёт пиксель, который расположен на отрезке, соединяющим пиксель {@code start} и пиксель, находящийся на
     * расстоянии, эквивалентном земному расстоянию {@code length}, и угловом расстоянии {@code angle} от пикселя
     * {@code start}, температура которого отличается от температуры предыдущего пикселя не менее, чем на
     * {@code tempDiff} гр. Ц. Если таких пикселей несколько, то выдаётся пиксель, который наиболее удалён от пикселя
     * {@code start}. Сам пиксель {@code start} отрезку не принадлежит.
     * <p>
     * Выдаёт средннюю температуру {@code n} пикселей, которые находятся в конце упомянутого выше отрезка. (Это
     * количество может быть уменьшено, если число пикселей в отрезке меньше {@code n}.)
     * <p>
     * Неинформативные случаи:
     * - длина отрезка равна 0, т. е. пиксель {@code start} совпадает с другим концом этого отрезка (в этом случае
     * выдаётся пиксель {@code (-2,-2)} и нулевая температура),
     * - длина отрезка >0, но на нём нет температурного контраста, т. е. пиксель, чьё вычисление описано в начале,
     * отсутствует (в этом случае выдаётся пиксель {@code (-1,-1)} и нулевая температура).
     *
     * @param angle    угол (в град.), отсчитываемый от положительного направления оси c'y' по часовой стрелке,
     *                 принадлежащий промежутку {@code (-180,180]}
     * @param length   земная длина (в м.) рассматриваемого отрезка
     * @param tempDiff разность температур (в гр. Ц.) между соседними пикселями, при превышении которой запоминается
     *                 пиксель
     * @param n        максимальное число пикселей, по которым рассчитывается средняя температура
     */
    public static Object[] findJump(Pixel start, double angle, double length, double tempDiff, int n, String filename,
                                    char separator, double height, double pixelSize, double focalLength,
                                    int resX, int resY) {

        double[][] realTable = Helper.extractTable(filename, separator);
        List<Pixel> jumps = new ArrayList<>();
        List<Double> temperatures = new ArrayList<>();
        int sI = start.getI();
        int sJ = start.getJ();

        int iInc = (int) Math.round(Thermogram.earthToDiscreteMatrix(length * sin(angle * PI / 180), height, pixelSize, focalLength));
        int jInc = (int) Math.round(Thermogram.earthToDiscreteMatrix(length * cos(angle * PI / 180), height, pixelSize, focalLength));

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
            if (i != sI + iIncSign && abs(currTemp - prevTemp) >= tempDiff)
                jumps.add(!inversion ? new Pixel(i, j) : new Pixel(j, i));
            jPrev = j;
        }

        double avEndTemp = 0;
        for (int i = 0; i < min(n, temperatures.size()); i++)
            avEndTemp += temperatures.get(temperatures.size() - 1 - i);
        avEndTemp = avEndTemp / min(n, temperatures.size());

        return new Object[]{jumps.size() > 0 ? jumps.get(jumps.size() - 1) : new Pixel(-1, -1), avEndTemp};
    }

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

    /**
     * Если сдвиг невозможен, то возвращает пиксель {@code (-10,-10)}.
     */
    private static Pixel shiftPixel(Pixel pixel, int[][] right, int[][] left, double[] avEndTemp,
                                    List<Integer> permittedAnglesIndicesWrtTemp, int half, int resX, int resY,
                                    String filenameOutput) {

        double[] diff = new double[half];
        double[] avR = new double[half];
        double[] avL = new double[half];
        int[] numberOfRightExcludedIndices = new int[half];
        int[] numberOfLeftExcludedIndices = new int[half];
        List<Integer> permittedDiameters = new ArrayList<>();

        for (int i = 0; i < half; i++) {
            for (int j = 0; j < half - 1; j++) {
                if (permittedAnglesIndicesWrtTemp.contains(right[i][j]))
                    avR[i] += avEndTemp[right[i][j]];
                else
                    numberOfRightExcludedIndices[i]++;
                if (permittedAnglesIndicesWrtTemp.contains(left[i][j]))
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

        Helper.write(filenameOutput, "---   indexMax=" + indexMax + "   avR[indexMax] > avL[indexMax] ?  " + (avR[indexMax] > avL[indexMax]));
        Helper.write(filenameOutput, pixel.toString());

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
                                        double diameter, double coef, double tempJump, int l, double dec, double eps,
                                        int maxIter, String filename, String outputPictureFilename,
                                        String filenameOutput, char separatorReal, double pixelSize, double focalLength,
                                        int resX, int resY) {

        Polygon<Pixel> polygon1 = Polygon.toPixelPolygon(polygon, focalLength, resY);
        double pixelDiameter = Thermogram.earthToDiscreteMatrix(diameter, thermogram.getHeight(), pixelSize, focalLength);
        Helper.write(filenameOutput, "       ===  " + thermogram.getName() + "  ===\n");

        int w = polygon1.width();
        int h = polygon1.height();
        double d = pixelDiameter;

        int eps1 = 2;
        int eps2 = 4;

        // Многоугольник polygon является отчётливо горизонтальным (относительно термограммы).
        if (w >= d + eps2 && ((d - eps1 <= h && h <= d) || (h > d && w > h))) {
            Helper.write(filenameOutput, "Многоугольник " + num + " " + polygon1 + " является длинным => 0");
            return 0;
        }

        // Многоугольник polygon является отчётливо вертикальным (относительно термограммы).
        if (h >= d + eps2 && ((d - eps1 <= w && w <= d) || (w > d && h > w))) {
            Helper.write(filenameOutput, "Многоугольник " + num + " " + polygon1 + " является длинным => 90");
            return 90;
        }

        double[] angles = new double[l];
        // 0, 45, 90, 135, 180, -135, -90, -45 (при l=8)
        for (int i = 0; i < l; i++)
            angles[i] = i * 360. / l - (i * 360 / l <= 180 ? 0 : 360);

        double pipeAngle = -1000;

        int n = 8; // число частей, на которые делим круг

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
        // Углы наклона отсчитываются от положительного направления оси абсцисс против часовой стрелки и принадлежат
        // промежутку (-90,90].
        int q = l / n; // число углов, приходящихся на 1/n часть круга
        int s = l / 2; // разность номеров диаметрально противоположных углов
        Point[] diameters = new Point[n / 2];
        double[] diameterAngles = new double[n / 2];
        for (int i = 0; i < n / 2; i++) {
            diameters[i] = new Point(i * q, i * q + s);
            diameterAngles[i] = 90 - 45 * i;
        }
        Point[] almostDiameters = new Point[n];
        double[] almostDiameterAngles = new double[n];
        for (int i = 0; i < n; i++) {
            almostDiameters[i] = new Point(i * q, (i + 1) * q + s - ((i + 1) * q + s < l ? 0 : l));
            double tmp = (90 + 45) / 2. - 45 * i;
            almostDiameterAngles[i] = tmp + (tmp <= -90 ? 180 : 0);
        }
        Helper.write(filenameOutput, Arrays.toString(diameterAngles));
        Helper.write(filenameOutput, Arrays.toString(almostDiameterAngles) + "\n");

        // Рассматривается half диаметров, чьи углы наклона, отсчитываемые относительно положительного направления оси
        // c'y' по часовой стрелке, изменяются от angles[0] до angles[half-1].
        //
        // Для 0-го диаметра right[0][] - множество индексов углов из массива angles, лежащих справа от этого диаметра,
        // а left[0][] - слева. При увеличении номера диаметра, диаметр поворачивается по часовой стрелке, и вслед за
        // ним также поворачиваются эти множества индексов.
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
            forbiddenDirections[i][1] = i + half;
        }

        Object[][] jumps = new Object[l][];
        Pixel[] jumpPixel = new Pixel[l];
        double[] avEndTemp = new double[l];

        List<Integer> range1Corr = new ArrayList<>();
        List<Integer> range2Corr = new ArrayList<>();

        int iter = 0;
        int i1 = -1, i2 = -1;
        while (iter < maxIter) {
            if (pixel.equals(new Pixel(-10, -10))) {
                Helper.write(filenameOutput, "--- сдвиг pixel невозможен ---");
                break;
            }
            iter++;

            double inclination1, inclination2;
            double inclination1Old, inclination2Old;
            double pipeAngleOld;

            double tMax1 = -1000;
            double tMax2 = -1000;
            double aMax1;
            double aMax2;

            List<Integer> range1 = new ArrayList<>();
            List<Integer> range2 = new ArrayList<>();

            List<Integer> excludedAnglesIndices = new ArrayList<>();
            List<Integer> permittedAnglesIndices = new ArrayList<>();
            List<Integer> excludedAnglesIndicesWrtTemp = new ArrayList<>();
            List<Integer> permittedAnglesIndicesWrtTemp = new ArrayList<>();

            range1Corr.clear();
            range2Corr.clear();

            for (int i = 0; i < l; i++) {
                jumps[i] = findJump(pixel, angles[i], coef * diameter, tempJump, 2, filename, separatorReal,
                        thermogram.getHeight(), pixelSize, focalLength, resX, resY);
                jumpPixel[i] = (Pixel) jumps[i][0];
                avEndTemp[i] = (double) jumps[i][1];
                if (jumpPixel[i].equals(new Pixel(-1, -1)) || jumpPixel[i].equals(new Pixel(-2, -2)))
                    excludedAnglesIndices.add(i);
                else
                    permittedAnglesIndices.add(i);
                if (jumpPixel[i].equals(new Pixel(-2, -2)))
                    excludedAnglesIndicesWrtTemp.add(i);
                else
                    permittedAnglesIndicesWrtTemp.add(i);
            }

            try {
                if (permittedAnglesIndices.size() >= 2) {
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

            // Упорядочиваем i1 и i2, чтобы стало i1<i2. Эти индексы могут быть соседними.
            int tmp = i1;
            i1 = min(i1, i2);
            i2 = max(tmp, i2);

            // Разделяем все индексы углов, за исключением индексов i1 и i2, на две равные части: range1 и range2.

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

            // inclination1 (inclination2) - угол наклона прямой, аппроксимирующей точки массива jumpPixel, чьи индексы
            // прнадлежат списку range1Corr (range2Corr). Сначала принадлежит интервалу (-90,90), потом добавляется
            // значение 90 для случая, когда точки из того или иного диапазона расположены примерно по вертикали.
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

            // pipeAngle - угол наклона биссектрисы острого угла между прямыми, чьи углы наклона равны inclination1 и
            // inclination2, относительно положительного направления оси c'x', отсчитываемый против часовой стрелки.
            // (Если эти прямые перпендикулярны, то берётся биссектриса, которая ближе всего к оси c'x'. Если же эти
            // биссектрисы равноудалены от оси c'x', то берётся биссектриса, чей угол наклона положителен.)
            //
            // Сначала принадлежит промежутку (-90,90].
            // Трактуется как угол наклона трубы.
            if (range1Corr.size() < 2 & range2Corr.size() > 1)
                pipeAngle = inclination2;
            else if (range2Corr.size() < 2 & range1Corr.size() > 1)
                pipeAngle = inclination1;
            else if (range1Corr.size() < 2 & range2Corr.size() < 2)
                pipeAngle = 90;
            else {
                double v = (inclination1 + inclination2) / 2;
                pipeAngle = v + (inclination1 * inclination2 < 0 && abs(inclination1) + abs(inclination2) > 90 ?
                        (v <= 0 ? 90 : -90) : 0);
            }
            pipeAngleOld = pipeAngle + (pipeAngle < 0 ? 180 : 0);

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

            int dInd = -1, aDInd = -1;
            for (int i = 0; i < n / 2; i++)
                if (new Point(i1, i2).equals(diameters[i])) {
                    dInd = i;
                    break;
                }
            if (dInd == -1)
                for (int i = 0; i < n; i++)
                    if (new Point(i1, i2).equals(almostDiameters[i]) || new Point(i2, i1).equals(almostDiameters[i])) {
                        aDInd = i;
                        break;
                    }

            boolean isCloseToDiameter = false, isCloseToAlmostDiameter = false;
            if (dInd != -1)
                // Определяет, отстоит ли прямая с углом наклона pipeAngle от прямой с углом наклона
                // diameterAngles[dInd] не более, чем на eps.
                isCloseToDiameter = abs(pipeAngle - diameterAngles[dInd]) <= eps ||
                        abs(pipeAngle - (diameterAngles[dInd] + 180)) <= eps;
            if (aDInd != -1)
                // Определяет, отстоит ли прямая с углом наклона pipeAngle от прямой с углом наклона
                // almostDiameterAngles[aDInd] не более, чем на eps.
                isCloseToAlmostDiameter = abs(pipeAngle - almostDiameterAngles[aDInd]) <= eps ||
                        abs(pipeAngle - (almostDiameterAngles[aDInd] + 180)) <= eps;
            // Значения i1, i2 соответствуют эталонному направлению (диаметру или почти-диаметру), но угол pipeAngle
            // сильно отличается от эталонного угла.
            if ((dInd != -1 && !isCloseToDiameter) || (aDInd != -1 && !isCloseToAlmostDiameter)) {
                if (iter < maxIter) {
                    coef *= dec;
                    if (dInd != -1)
                        System.out.println(dInd + "   d=" + diameters[dInd] + "  " + diameterAngles[dInd] + "  " + pipeAngle);
                    if (aDInd != -1)
                        System.out.println(aDInd + "   ad=" + almostDiameters[aDInd] + "  " + almostDiameterAngles[aDInd] + "  " + pipeAngle);
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
            }
            // Эталонное направление не найдено или, в противном случае, угол pipeAngle корректен.
            else
                // i1 и i2 - соседние или отличаются на 2.
                if ((abs(i1 - i2) == 1 || (i1 == 0 && i2 == l - 1)) ||
                        (abs(i1 - i2) == 2 || (i1 == 0 && i2 == l - 2) || (i1 == 1 && i2 == l - 1))) {

                    Helper.write(filenameOutput, "--- сдвиг и уменьшение coef ---\n");
                    coef *= dec;
                    pixel = shiftPixel(pixel, right, left, avEndTemp, permittedAnglesIndicesWrtTemp, half, resX, resY,
                            filenameOutput);
                } else {
                    Helper.write(filenameOutput, "--- индексы i1 и i2 хорошо различимы ---\n");
                    break;
                }
        }

        if (range1Corr.size() < 2 && range2Corr.size() < 2)
            Helper.write(filenameOutput, "Многоугольник " + num + " " + polygon1 + ": range1Corr.size(),range2Corr.size()<2");

        try {
            BufferedImage image = ImageIO.read(new File(outputPictureFilename));
            for (int i = 0; i < l; i++) {
                Helper.write(filenameOutput, String.format("%1$6s", angles[i]) + "  " +
                        String.format("%1$5s", round(avEndTemp[i] * 10) / 10.) + "  " + jumpPixel[i]);
                if (!jumpPixel[i].equals(new Pixel(-1, -1)) && !jumpPixel[i].equals(new Pixel(-2, -2)))
                    new Segment(pixel.toPoint(resY), jumpPixel[i].toPoint(resY)).draw(image, Color.BLACK);
            }
            ImageIO.write(image, "jpg", new File(outputPictureFilename));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Helper.write(filenameOutput, "\n");
        return pipeAngle;
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
        for (int i = 0; i < middles.size(); i++)
            aRes.add(findPipeAngle(middles.get(i), enlargedPolygons.get(i), i, thermogram, 0.7, 2,
                    2, 8, 0.9, 20, 10, filename, outputPictureFilename,
                    DIR_CURRENT + "/angles2.txt", separatorReal, pixelSize, focalLength, resX, resY));

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
                slopingDefects.add(Rectangle.slopeRectangle(bList.get(i), aRes.get(i) + (aRes.get(i) >= 90 ? -90 : 0), resY));

        int diam = (int) round(Base.realToMatrix(0.7, thermogram.getHeight(), pixelSize, focalLength));
        try {
            BufferedImage image = ImageIO.read(new File(thermogramFilename));
            for (int i = 0; i < slopingDefects.size(); i++) {
                Polygon<Pixel> p = slopingDefects.get(i).widen(diam, aRes.get(i));
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