package main;

import polygons.Point;

import java.awt.*;
import java.net.URISyntaxException;
import java.util.List;


/**
 * http://www.javenue.info/post/78 - чтение и запись CSV файлов в Java.
 * https://exiftool.org/forum/index.php/topic,4898.msg23972.html#msg23972.
 */
public class Main {
    //
    // Краткие имена файлов.
    //

    /**
     * Краткое имя файла с конфигурационными параметрами.
     */
    public final static String SHORT_FILENAME_CONFIG = "config.txt";
    /**
     * Краткое имя файла с общими для всех термограмм EXIF-параметрами.
     */
    private final static String SHORT_FILENAME_GLOBAL_PARAMS = "global_params.txt";
    /**
     * Краткое имя файла с геометрическими характеристиками съёмки.
     */
    private final static String SHORT_FILENAME_THERMOGRAMS_INFO = "thermograms_info.txt";


    //
    // Полные имена файлов.
    //

    /**
     * Полное имя файла {@code SHORT_FILENAME_GLOBAL_PARAMS}.
     */
    public final static String FILENAME_GLOBAL_PARAMS;
    /**
     * Полное имя файла {@code SHORT_FILENAME_THERMOGRAMS_INFO}.
     */
    private final static String FILENAME_THERMOGRAMS_INFO;


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
     * Минимальная площадь прямоугольника (в кв. пикселях).
     */
    public final static int MIN_PIXEL_SQUARE = 25;


    //
    // Константы, извлечённые с использованием информации из файла SHORT_FILENAME_GLOBAL_PARAMS.
    //

    /**
     * Фокусное расстояние, м.
     */
    public final static double FOCAL_LENGTH;
    /**
     * Разрешение матрицы по горизонтали (т. е. по оси c'x').
     */
    public final static int RES_X;
    /**
     * Разрешение матрицы по вертикали (т. е. по оси c'y').
     */
    public final static int RES_Y;


    //
    // Константы, извлечённые с использованием информации из конфигурационного файла SHORT_FILENAME_CONFIG и текущей
    // папки.
    //

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
    private final static String POSTFIX_RAW;
    /**
     * Постфикс (содержащий расширение {@code .jpg}), используемый для формирования имён термограмм с выделенными
     * дефектами.
     */
    private final static String POSTFIX_PROCESSED;
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

        FOCAL_LENGTH = ExifParam.FOCAL_LENGTH.getValue() / 1000;
        RES_X = (int) ExifParam.RAW_THERMAL_IMAGE_WIDTH.getValue();
        RES_Y = (int) ExifParam.RAW_THERMAL_IMAGE_HEIGHT.getValue();

        FILENAME_GLOBAL_PARAMS = DIR_CURRENT + "/" + Property.SUBDIR_OUTPUT.getValue() + "/" + SHORT_FILENAME_GLOBAL_PARAMS;
        FILENAME_THERMOGRAMS_INFO = DIR_CURRENT + "/" + Property.SUBDIR_OUTPUT.getValue() + "/" + SHORT_FILENAME_THERMOGRAMS_INFO;
        DIR_THERMOGRAMS = "/" + Property.DIR_THERMOGRAMS.getValue().replace('\\', '/');
        DIR_RAW = DIR_CURRENT + "/" + Property.SUBDIR_RAW.getValue();
        DIR_OUTPUT_PICTURES = DIR_CURRENT + "/" + Property.SUBDIR_OUTPUT_PICTURES.getValue();
        POSTFIX_RAW = Property.POSTFIX_RAW.getValue() + EXTENSION_RAW;
        POSTFIX_PROCESSED = Property.POSTFIX_PROCESSED.getValue() + EXTENSION;
        PIXEL_SIZE = Property.PIXEL_SIZE.getDoubleValue() / 1000_000;
        PRINCIPAL_POINT_X = Property.PRINCIPAL_POINT_X.getIntValue();
        PRINCIPAL_POINT_Y = Property.PRINCIPAL_POINT_Y.getIntValue();
        PRINCIPAL_POINT = new Pixel(PRINCIPAL_POINT_X, PRINCIPAL_POINT_Y);
        T_MIN = Property.T_MIN.getDoubleValue();
        SQUARE_MIN = Property.SQUARE_MIN.getDoubleValue();
    }


    private static void process(Thermogram thermogram, Thermogram previous) {
        String thermogramFilename = DIR_THERMOGRAMS + "/" + thermogram.getName() + EXTENSION;
        String rawFilename = DIR_RAW + "/" + thermogram.getName() + POSTFIX_RAW;
        String outputPictureFilename = DIR_OUTPUT_PICTURES + "/" + thermogram.getName() + EXTENSION;

        System.out.println("Файл с выделенными дефектами: " + outputPictureFilename + "\n");

        int[][] rawTable = Helper.extractRawTable(rawFilename,
                (int) ExifParam.RAW_THERMAL_IMAGE_HEIGHT.getValue(),
                (int) ExifParam.RAW_THERMAL_IMAGE_WIDTH.getValue());

        double[][] realTable = Helper.rawTableToReal(rawTable, ExifParam.readValues());

        int[][] binTable = Helper.findIf(realTable, num -> num > T_MIN);

        List<Rectangle<Point>> ranges = Rectangle.findRectangles(binTable);
        ranges.removeIf(range -> range.squarePixels() < MIN_PIXEL_SQUARE);

        Polygon<Pixel> overlap = thermogram.getOverlapWith(previous);
        //System.out.println(overlap);

        List<Polygon<Point>> polygons = Polygon.toPolygons(ranges, overlap, thermogram.getHeight());
        List<Polygon<Point>> enlargedPolygons = Polygon.enlargeIteratively(polygons, 5, overlap, thermogram.getHeight());

        Polygon.drawPolygons(enlargedPolygons, Polygon.toPointPolygon(overlap), Color.BLACK, thermogramFilename, outputPictureFilename);
        Polygon.showSquares(enlargedPolygons, thermogram.getHeight());

        System.out.println("\n");
    }

    public static void main(String[] args) {
        Thermogram[] thermograms = Thermogram.readThermograms(FILENAME_THERMOGRAMS_INFO);
        for (int i = 0; i < thermograms.length; i++)
            process(thermograms[i], thermograms[i - 1 >= 0 ? i - 1 : thermograms.length - 1]);
    }
}