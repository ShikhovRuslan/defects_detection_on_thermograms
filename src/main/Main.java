package main;

import polygons.Point;
import tmp.Base;

import java.awt.*;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.*;
import static java.lang.Math.abs;


/**
 * http://www.javenue.info/post/78 - чтение и запись CSV файлов в Java.
 * https://exiftool.org/forum/index.php/topic,4898.msg23972.html#msg23972.
 */
public class Main {
    //
    // Краткие имена файлов.
    //

    /**
     *
     */
    private final static String BAT_GLOBAL_PARAMS = "global_params.bat";
    /**
     *
     */
    private final static String BAT_THERMOGRAMS_INFO = "thermograms_info.bat";
    /**
     *
     */
    private final static String BAT_THERMOGRAMS_RAW_TEMPERATURES = "thermograms_raw_temperatures.bat";
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
        String dirCurrent = "";
        try {
            dirCurrent = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        DIR_CURRENT = dirCurrent.substring(0, dirCurrent.lastIndexOf('/'));

        PIXEL_SIZE = Property.PIXEL_SIZE.getDoubleValue() / 1000_000;
        PRINCIPAL_POINT_X = Property.PRINCIPAL_POINT_X.getIntValue();
        PRINCIPAL_POINT_Y = Property.PRINCIPAL_POINT_Y.getIntValue();
        PRINCIPAL_POINT = new Pixel(PRINCIPAL_POINT_X, PRINCIPAL_POINT_Y);
        T_MIN = Property.T_MIN.getDoubleValue();
        SQUARE_MIN = Property.SQUARE_MIN.getDoubleValue();
    }


    /**
     * Углы термограммы в системе координат c'x'y', начиная с верхнего левого угла и заканчивая нижним левым.
     */
    public enum Corners {
        /**
         * Верхний левый угол термограммы.
         */
        C0(0, ExifParam.RES_Y.getIntValue() - 1),
        /**
         * Верхний правый угол термограммы.
         */
        C1(ExifParam.RES_X.getIntValue() - 1, ExifParam.RES_Y.getIntValue() - 1),
        /**
         * Нижний правый угол термограммы.
         */
        C2(ExifParam.RES_X.getIntValue() - 1, 0),
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

    private enum Option {
        GLOBAL_PARAMS,
        THERMOGRAMS_INFO,
        THERMOGRAMS_RAW_TEMPERATURES,
        CSV,
        DEFECTS,
        HELP;

        private static void help() {
            try {
                BufferedReader reader = Files.newBufferedReader(Paths.get(DIR_CURRENT.substring(1) + "/" +
                        SHORT_FILENAME_HELP));
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

    private static void defects(Thermogram thermogram, Polygon<Pixel> overlap,
                                String thermogramFilename, String outputPictureFilename, String realFilename,
                                double focalLength, int resX, int resY) throws IOException {
        System.out.println("=== Thermogram: " + thermogram.getName() + " ===\n");

        double[][] realTable = Helper.extractTable(realFilename, SEPARATOR_REAL);
        int[][] binTable = Helper.findIf(realTable, num -> num > T_MIN);

        BufferedWriter outputWriter = new BufferedWriter(
                new FileWriter(DIR_CURRENT + "/" + thermogram.getName() + "_bin.txt"));
        for (int[] ints : binTable) {
            for (int j = 0; j < binTable[0].length; j++)
                outputWriter.write(ints[j] + "");
            outputWriter.newLine();
        }
        outputWriter.flush();
        outputWriter.close();

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
        innerPixels[0] = new Pixel(228 - 10, 129 - 5);
        innerPixels[1] = new Pixel(231, 129);
        innerPixels[2] = new Pixel(233 + 10, 130 + 5);
        System.out.println(Base._aaa(innerPixels[0], filename, SEPARATOR_REAL, 0.7, thermogram.getHeight(), focalLength, resY));
        System.out.println(Base._aaa(innerPixels[1], filename, SEPARATOR_REAL, 0.7, thermogram.getHeight(), focalLength, resY));
        System.out.println(Base._aaa(innerPixels[2], filename, SEPARATOR_REAL, 0.7, thermogram.getHeight(), focalLength, resY));

        System.out.println("\n");
    }

    public static void main(String[] args) throws IOException {
        Option option = args.length == 1 ? Option.getByAlias(args[0]) : Option.HELP;

        switch (option) {
            case GLOBAL_PARAMS:
                Helper.run(DIR_CURRENT.substring(1), BAT_GLOBAL_PARAMS);
                break;

            case THERMOGRAMS_INFO:
                Helper.run(DIR_CURRENT.substring(1), BAT_THERMOGRAMS_INFO);
                break;

            case THERMOGRAMS_RAW_TEMPERATURES:
                Helper.run(DIR_CURRENT.substring(1), BAT_THERMOGRAMS_RAW_TEMPERATURES);
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
                            "/" + Property.DIR_THERMOGRAMS.getValue().replace('\\', '/') +
                                    "/" + thermograms[i].getName() + EXTENSION,
                            DIR_CURRENT + "/" + Property.SUBDIR_OUTPUT_PICTURES.getValue() +
                                    "/" + thermograms[i].getName() + Property.POSTFIX_PROCESSED.getValue() + EXTENSION,
                            DIR_CURRENT + "/" + Property.SUBDIR_OUTPUT_REAL.getValue() +
                                    "/" + thermograms[i].getName() + Property.POSTFIX_REAL.getValue() + EXTENSION_REAL,
                            ExifParam.FOCAL_LENGTH.getValue(), ExifParam.RES_X.getIntValue(), ExifParam.RES_Y.getIntValue());
                break;

            case HELP:
                Option.help();
        }
    }
}