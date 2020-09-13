package main;

import polygons.Point;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Scanner;


/**
 * http://www.javenue.info/post/78 - чтение и запись CSV файлов в Java.
 * https://exiftool.org/forum/index.php/topic,4898.msg23972.html#msg23972.
 */
public class Main {
    /**
     * Краткое имя файла с конфигурационными параметрами.
     */
    private final static String SHORT_FILENAME_CONFIG = "config.txt";
    /**
     * Краткое имя файла с общими для всех термограмм параметрами.
     */
    private final static String SHORT_FILENAME_GLOBAL_PARAMS = "global_params.txt";
    /**
     * Краткое имя файла с геометрическими характеристиками съёмки.
     */
    private final static String SHORT_FILENAME_THERMOGRAMS_INFO = "thermograms_info.txt";

    /**
     * Полное имя файла {@code SHORT_FILENAME_GLOBAL_PARAMS}.
     */
    private final static String FILENAME_GLOBAL_PARAMS;
    /**
     * Полное имя файла {@code SHORT_FILENAME_THERMOGRAMS_INFO}.
     */
    private final static String FILENAME_THERMOGRAMS_INFO;

    /**
     * Папка с термограммами.
     */
    private final static String DIR_THERMOGRAMS;
    /**
     * Папка с файлами, содержащими необработанные температурные данные термограмм.
     */
    private final static String DIR_RAW;
    /**
     * Папка с термограммами с выделенными дефектами.
     */
    private final static String DIR_OUTPUT_PICTURES;

    /**
     * Постфикс (содержащий расширение {@code EXTENSION}), используемый для формирования имён файлов с необработанными
     * температурными данными термограмм.
     */
    private final static String POSTFIX;
    /**
     * Расширение файлов с необработанными температурными данными термограмм.
     */
    private final static String EXTENSION = ".pgm";

    /**
     * Содержит названия параметров в конфигурационном файле {@code SHORT_FILENAME_CONFIG}.
     */
    private enum Property {
        DIR_THERMOGRAMS("THERMOGRAMS_DIR"),
        SUBDIR_OUTPUT("OUTPUT_SUBDIR"),
        SUBDIR_RAW("RAW_SUBDIR"),
        SUBDIR_OUTPUT_PICTURES("OUTPUT_PICTURES_SUBDIR"),
        POSTFIX("POSTFIX");

        private final String name;

        Property(String name) {
            this.name = name;
        }

        private String getName() {
            return name;
        }
    }

    /**
     * Содержит общие для всех термограмм EXIF-параметры, находящиеся в файле {@code SHORT_FILENAME_GLOBAL_PARAMS}.
     */
    private enum ExifParam {
        PLANCK_R1("PlanckR1"),
        PLANCK_R2("PlanckR2"),
        PLANCK_O("PlanckO"),
        PLANCK_B("PlanckB"),
        PLANCK_F("PlanckF"),
        EMISSIVITY("Emissivity"),
        REFLECTED_APPARENT_TEMPERATURE("ReflectedApparentTemperature"),
        RAW_THERMAL_IMAGE_HEIGHT("RawThermalImageHeight"),
        RAW_THERMAL_IMAGE_WIDTH("RawThermalImageWidth");

        private final String rawName;

        ExifParam(String rawName) {
            this.rawName = rawName;
        }

        private String getRawName() {
            return rawName;
        }

        /**
         * Извлекает значение ключа {@code exifParam} из файла {@code filename} в формате JSON.
         */
        private static double getExifParam(ExifParam exifParam, String filename) {
            return Helper.getJsonObject(filename).get(exifParam.getRawName()).getAsDouble();
        }

        /**
         * Извлекает массив значений всех ключей из файла {@code filename} в формате JSON.
         */
        private static double[] getExifParams(String filename) {
            double[] exifParams = new double[ExifParam.values().length];
            for (int i = 0; i < ExifParam.values().length; i++)
                exifParams[i] = getExifParam(ExifParam.values()[i], filename);
            return exifParams;
        }
    }

    static {
        String currentDir = "";
        try {
            currentDir = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        currentDir = currentDir.substring(0, currentDir.lastIndexOf('/'));

        File configFile = new File(currentDir + "/" + SHORT_FILENAME_CONFIG);

        Scanner sc = null;
        try {
            sc = new Scanner(configFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String nextLine;
        String[] values = new String[Property.values().length];
        while (sc.hasNextLine()) {
            nextLine = sc.nextLine();
            for (int i = 0; i < Property.values().length; i++)
                if (nextLine.matches(Property.values()[i].getName() + ".*"))
                    values[i] = nextLine.substring(nextLine.indexOf('=') + 2);
        }
        String dirThermograms = values[0];
        String subdirOutput = values[1];
        String subdirRaw = values[2];
        String subdirOutputPictures = values[3];
        String postfix = values[4];

        FILENAME_GLOBAL_PARAMS = currentDir + "/" + subdirOutput + "/" + SHORT_FILENAME_GLOBAL_PARAMS;
        FILENAME_THERMOGRAMS_INFO = currentDir + "/" + subdirOutput + "/" + SHORT_FILENAME_THERMOGRAMS_INFO;
        DIR_THERMOGRAMS = "/" + dirThermograms.replace('\\', '/');
        DIR_RAW = currentDir + "/" + subdirRaw;
        DIR_OUTPUT_PICTURES = currentDir + "/" + subdirOutputPictures;
        POSTFIX = postfix + EXTENSION;
    }

    private static void process(Thermogram thermogram, Thermogram previous) {
        String thermogramFilename = DIR_THERMOGRAMS + "/" + thermogram.getName() + ".jpg";
        String rawFilename = DIR_RAW + "/" + thermogram.getName() + POSTFIX;
        String outputPictureFilename = DIR_OUTPUT_PICTURES + "/" + thermogram.getName() + ".jpg";

        System.out.println("Файл с выделенными дефектами: " + outputPictureFilename + "\n");

        int[][] rawTable = Helper.extractRawTable(rawFilename,
                (int) ExifParam.getExifParam(ExifParam.RAW_THERMAL_IMAGE_HEIGHT, FILENAME_GLOBAL_PARAMS),
                (int) ExifParam.getExifParam(ExifParam.RAW_THERMAL_IMAGE_WIDTH, FILENAME_GLOBAL_PARAMS));
        double[][] temperatureTable = Helper.rawTableToReal(rawTable, ExifParam.getExifParams(FILENAME_GLOBAL_PARAMS));

        int[][] binTable = Helper.findIf(temperatureTable, num -> num > Thermogram.T_MIN);
        List<Rectangle<Point>> ranges = Rectangle.findRectangles(binTable);
        ranges.removeIf(range -> range.squarePixels() < Thermogram.MIN_PIXEL_SQUARE);

        Polygon<Pixel> overlap = thermogram.getOverlapWith(previous);
        System.out.println(overlap);

        List<Polygon<Point>> polygons = Polygon.toPolygons(ranges, overlap, thermogram.getHeight());
        List<Polygon<Point>> enlargedPolygons = Polygon.enlargeIteratively(polygons, 5, overlap, thermogram.getHeight());

        Polygon.drawPolygons(enlargedPolygons, Polygon.toPointPolygon(overlap), Color.BLACK, thermogramFilename, outputPictureFilename);
        Polygon.showSquares(enlargedPolygons, thermogram.getHeight());
    }

    public static void main(String[] args) {
        Thermogram[] thermograms = Thermogram.readThermograms(FILENAME_THERMOGRAMS_INFO);
        process(thermograms[4], thermograms[3]);
    }
}